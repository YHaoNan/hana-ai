package top.yudoge.hanaai.openai;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okio.BufferedSource;
import top.yudoge.hanaai.core.chat.*;
import top.yudoge.hanaai.core.chat.stream.ChatStreamListener;
import top.yudoge.hanaai.core.chat.stream.StreamEvent;
import top.yudoge.hanaai.core.tool.Tool;
import top.yudoge.hanaai.core.tool.ToolCall;
import top.yudoge.hanaai.core.tool.ToolDescription;
import top.yudoge.hanaai.core.tool.ToolParamDescription;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OpenAIChatModel implements ChatModel {

    private final String baseUrl;
    private final String modelName;
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final List<Tool> tools = new ArrayList<>();
    private ChatOptions options = new ChatOptions();

    public OpenAIChatModel(String baseUrl, String modelName, String apiKey) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public ChatOptions options() {
        return options;
    }

    @Override
    public void options(ChatOptions options) {
        this.options = options != null ? options : new ChatOptions();
    }

    @Override
    public void registerTool(Tool tool) {
        if (tool != null && tool.description() != null) {
            tools.add(tool);
        }
    }

    @Override
    public ChatResponse chat(List<Message> messages) {
        RequestBody body = buildRequestBody(messages, false);
        Request request = buildRequest(body);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            return parseResponse(response.body().string());
        } catch (IOException e) {
            throw new RuntimeException("LLM chat failed", e);
        }
    }

    @Override
    public ChatResponse chat(Message message) {
        return chat(Collections.singletonList(message));
    }

    @Override
    public void streamChat(List<Message> messages, ChatStreamListener listener) {
        RequestBody body = buildRequestBody(messages, true);
        Request request = buildRequest(body);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onError(e.getMessage(), e);
                listener.onFinish(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    listener.onError("Unexpected response: " + response + ", body: " + bodyStr, null);
                    listener.onFinish(null);
                    return;
                }

                MediaType contentType = response.body().contentType();
                if (contentType == null || !contentType.subtype().contains("event-stream")) {
                    String bodyStr = response.body().string();
                    listener.onError("Invalid content type: " + contentType + ", body: " + bodyStr, null);
                    listener.onFinish(null);
                    return;
                }

                BufferedSource source = response.body().source();
                ModelUsage usage = null;
                StringBuilder textContent = new StringBuilder();
                List<ToolCall> toolCalls = new ArrayList<>();

                try {
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || line.isEmpty()) continue;
                        if (!line.startsWith("data: ")) continue;
                        String data = line.substring(6);
                        if (data.equals("[DONE]")) break;

                        ObjectNode node = mapper.readValue(data, ObjectNode.class);
                        ArrayNode choices = (ArrayNode) node.get("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonNode choice = choices.get(0);
                            JsonNode delta = choice.get("delta");

                            if (delta != null && delta.isObject()) {
                                ObjectNode deltaObj = (ObjectNode) delta;
                                JsonNode contentNode = deltaObj.get("content");
                                if (contentNode != null && !contentNode.isNull()) {
                                    String content = contentNode.asText("");
                                    if (!content.isEmpty()) {
                                        textContent.append(content);
                                        listener.onEvent(StreamEvent.textDelta(content));
                                    }
                                }

                                JsonNode toolCallsNode = deltaObj.get("tool_calls");
                                if (toolCallsNode != null && toolCallsNode.isArray()) {
                                    for (JsonNode tc : toolCallsNode) {
                                        ToolCall toolCall = parseToolCall((ObjectNode) tc);
                                        if (toolCall != null) {
                                            toolCalls.add(toolCall);
                                            listener.onEvent(StreamEvent.toolCall(toolCall));
                                        }
                                    }
                                }
                            }

                            JsonNode finishReason = choice.get("finish_reason");
                            if (finishReason != null) {
                                String reason = finishReason.asText();
                                if ("stop".equals(reason)) {
                                    if (textContent.length() > 0) {
                                        listener.onEvent(StreamEvent.textComplete(textContent.toString()));
                                    }
                                    JsonNode usageNode = node.get("usage");
                                    if (usageNode != null && usageNode.isObject()) {
                                        usage = parseUsage((ObjectNode) usageNode);
                                        listener.onEvent(StreamEvent.usageReceived(usage));
                                    }
                                } else if ("tool_calls".equals(reason)) {
                                    for (ToolCall tc : toolCalls) {
                                        listener.onEvent(StreamEvent.toolCall(tc));
                                    }
                                }
                            }
                        }
                    }
                    listener.onEvent(StreamEvent.streamComplete());
                } catch (Exception e) {
                    listener.onError(e.getMessage(), e);
                } finally {
                    listener.onFinish(usage);
                }
            }
        });
    }

    @Override
    public void streamChat(Message message, ChatStreamListener listener) {
        streamChat(Collections.singletonList(message), listener);
    }

    @Override
    public boolean supportToolCalling() {
        return true;
    }

    @Override
    public boolean supportStructuredOutput() {
        return true;
    }

    @Override
    public boolean supportParallelToolCalling() {
        return true;
    }

    private RequestBody buildRequestBody(List<Message> messages, boolean stream) {
        ObjectNode json = mapper.createObjectNode();
        json.put("model", modelName);
        json.put("stream", stream);

        if (options.getTemperature() != null) {
            json.put("temperature", options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            json.put("max_tokens", options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            json.put("top_p", options.getTopP());
        }
        if (options.getFrequencyPenalty() != null) {
            json.put("frequency_penalty", options.getFrequencyPenalty());
        }
        if (options.getPresencePenalty() != null) {
            json.put("presence_penalty", options.getPresencePenalty());
        }
        if (options.getStop() != null && !options.getStop().isEmpty()) {
            ArrayNode stopArray = json.putArray("stop");
            for (String stop : options.getStop()) {
                stopArray.add(stop);
            }
        }

        ArrayNode messageArray = json.putArray("messages");
        if (messages != null) {
            for (Message msg : messages) {
                ObjectNode msgNode = messageArray.addObject();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
            }
        }

        if (!tools.isEmpty()) {
            ArrayNode toolsArray = json.putArray("tools");
            for (Tool tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode functionNode = toolNode.putObject("function");
                ToolDescription desc = tool.description();
                functionNode.put("name", desc.getId());
                functionNode.put("description", desc.getDescription());
                if (desc.getParams() != null && !desc.getParams().isEmpty()) {
                    ObjectNode parametersNode = functionNode.putObject("parameters");
                    parametersNode.put("type", "object");
                    ObjectNode propertiesNode = parametersNode.putObject("properties");
                    for (ToolParamDescription param : desc.getParams()) {
                        ObjectNode paramNode = propertiesNode.putObject(param.getIdentifier());
                        paramNode.put("type", param.getType().name().toLowerCase());
                        paramNode.put("description", param.getDescription());
                        if (Boolean.TRUE.equals(param.getIsRequired())) {
                            parametersNode.putArray("required").add(param.getIdentifier());
                        }
                    }
                }
            }
        }

        return RequestBody.create(JSON, json.toString());
    }

    private Request buildRequest(RequestBody body) {
        return new Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }

    private ChatResponse parseResponse(String responseBody) {
        try {
            ObjectNode node = mapper.readValue(responseBody, ObjectNode.class);
            ArrayNode choices = (ArrayNode) node.get("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("No choices in response");
            }

            ObjectNode choice = (ObjectNode) choices.get(0);
            ObjectNode messageNode = (ObjectNode) choice.get("message");

            String finishReason = choice.has("finish_reason") ? choice.get("finish_reason").asText() : null;

            if ("tool_calls".equals(finishReason)) {
                ArrayNode toolCallsNode = (ArrayNode) messageNode.get("tool_calls");
                if (toolCallsNode != null && toolCallsNode.size() > 0) {
                    List<ToolCall> toolCalls = new ArrayList<>();
                    for (JsonNode tc : toolCallsNode) {
                        ToolCall toolCall = parseToolCall((ObjectNode) tc);
                        if (toolCall != null) {
                            toolCalls.add(toolCall);
                        }
                    }
                    ModelUsage usage = null;
                    ObjectNode usageNode = (ObjectNode) node.get("usage");
                    if (usageNode != null) {
                        usage = parseUsage(usageNode);
                    }
                    return ChatResponse.toolCalling(toolCalls, usage);
                }
            }

            Message message = new Message();
            message.setRole(messageNode.has("role") ? messageNode.get("role").asText() : "assistant");
            message.setContent(messageNode.has("content") ? messageNode.get("content").asText() : "");

            ModelUsage usage = null;
            ObjectNode usageNode = (ObjectNode) node.get("usage");
            if (usageNode != null) {
                usage = parseUsage(usageNode);
            }

            return ChatResponse.conversation(message, usage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private ToolCall parseToolCall(ObjectNode toolCallNode) {
        JsonNode idNode = toolCallNode.get("id");
        JsonNode functionNode = toolCallNode.get("function");
        if (functionNode == null) return null;

        String toolId = idNode != null ? idNode.asText() : null;
        String funcName = functionNode.has("name") ? functionNode.get("name").asText() : null;
        String argumentsJson = functionNode.has("arguments") ? functionNode.get("arguments").asText() : "{}";

        if (funcName == null) return null;

        ToolCall toolCall = new ToolCall(toolId);
        toolCall.setToolIdentifier(funcName);

        try {
            ObjectNode argsNode = mapper.readValue(argumentsJson, ObjectNode.class);
            Iterator<Map.Entry<String, JsonNode>> fields = argsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                toolCall.addArgument(field.getKey(), field.getValue().asText());
            }
        } catch (IOException e) {
        }

        return toolCall;
    }

    private ModelUsage parseUsage(ObjectNode usageNode) {
        ModelUsage usage = new ModelUsage();
        usage.setHasUsageStatics(true);
        usage.setInputTokens(usageNode.has("prompt_tokens") ? (long) usageNode.get("prompt_tokens").asInt() : 0L);
        usage.setOutputTokens(usageNode.has("completion_tokens") ? (long) usageNode.get("completion_tokens").asInt() : 0L);
        return usage;
    }
}
