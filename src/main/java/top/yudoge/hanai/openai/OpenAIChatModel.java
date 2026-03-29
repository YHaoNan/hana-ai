package top.yudoge.hanai.openai;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okio.BufferedSource;
import top.yudoge.hanai.core.chat.*;
import top.yudoge.hanai.core.chat.stream.ChatStreamListener;
import top.yudoge.hanai.core.chat.stream.StreamEvent;
import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.utils.AppLogger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OpenAIChatModel implements ChatModel {

    private static final AppLogger log = AppLogger.get(OpenAIChatModel.class);
    
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
        log.info("OpenAIChatModel initialized with model: {}", modelName);
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
        if (tool != null && tool.definition() != null) {
            tools.add(tool);
            log.debug("Registered tool: {}", tool.definition().getIdentifier());
        }
    }

    @Override
    public ChatResponse chat(List<Message> messages) {
        log.debug("Sending chat request with {} messages", messages.size());
        RequestBody body = buildRequestBody(messages, false);
        Request request = buildRequest(body);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String bodyStr = response.body() != null ? response.body().string() : "";
                log.error("Chat request failed: {} - {}", response.code(), bodyStr);
                throw new IOException("Unexpected response: " + response);
            }
            ChatResponse chatResponse = parseResponse(response.body().string());
            log.debug("Chat response received");
            return chatResponse;
        } catch (IOException e) {
            log.error("Failed to execute chat request", e);
            throw new RuntimeException("LLM chat failed", e);
        }
    }

    @Override
    public ChatResponse chat(Message message) {
        return chat(Collections.singletonList(message));
    }

    @Override
    public void streamChat(List<Message> messages, ChatStreamListener listener) {
        log.debug("Starting streaming chat with {} messages", messages.size());
        RequestBody body = buildRequestBody(messages, true);
        Request request = buildRequest(body);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Stream chat request failed", e);
                listener.onError(e.getMessage(), e);
                listener.onFinish(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String bodyStr = response.body() != null ? response.body().string() : "";
                    log.error("Stream chat failed: {} - {}", response.code(), bodyStr);
                    listener.onError("Unexpected response: " + response + ", body: " + bodyStr, null);
                    listener.onFinish(null);
                    return;
                }

                MediaType contentType = response.body().contentType();
                if (contentType == null || !contentType.subtype().contains("event-stream")) {
                    String bodyStr = response.body().string();
                    log.error("Invalid content type: {}", contentType);
                    listener.onError("Invalid content type: " + contentType + ", body: " + bodyStr, null);
                    listener.onFinish(null);
                    return;
                }

                log.debug("Starting to read streaming response");
                BufferedSource source = response.body().source();
                ModelUsage usage = null;
                StringBuilder textContent = new StringBuilder();
                Map<String, ToolCallBuilder> toolCallBuilders = new LinkedHashMap<>();
                String currentToolCallId = null;

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
                                        String id = tc.has("id") ? tc.get("id").asText() : null;
                                        JsonNode funcNode = tc.get("function");

                                        if (id != null && !id.isEmpty()) {
                                            currentToolCallId = id;
                                        }

                                        if (currentToolCallId == null) continue;

                                        ToolCallBuilder builder = toolCallBuilders.computeIfAbsent(currentToolCallId, k -> new ToolCallBuilder(k));

                                        if (funcNode != null) {
                                            if (funcNode.has("name")) {
                                                builder.name.append(funcNode.get("name").asText());
                                            }
                                            if (funcNode.has("arguments")) {
                                                builder.arguments.append(funcNode.get("arguments").asText());
                                            }
                                        }
                                    }
                                }
                            }

                            JsonNode finishReason = choice.get("finish_reason");
                            if (finishReason != null) {
                                String reason = finishReason.asText();
                                if ("stop".equals(reason) || "tool_calls".equals(reason)) {
                                    if (textContent.length() > 0) {
                                        listener.onEvent(StreamEvent.textComplete(textContent.toString()));
                                    }
                                    for (ToolCallBuilder builder : toolCallBuilders.values()) {
                                        ToolCall toolCall = builder.build(mapper);
                                        log.debug("Emitting tool call: id={}, tool={}, params={}", 
                                                toolCall.getId(), toolCall.getToolIdentifier(), toolCall.getParams().getValues());
                                        if (toolCall != null) {
                                            listener.onEvent(StreamEvent.toolCall(toolCall));
                                        }
                                    }
                                    JsonNode usageNode = node.get("usage");
                                    if (usageNode != null && usageNode.isObject()) {
                                        usage = parseUsage((ObjectNode) usageNode);
                                        listener.onEvent(StreamEvent.usageReceived(usage));
                                    }
                                    log.debug("Stream finished with reason: {}", reason);
                                }
                            }
                        }
                    }
                    listener.onEvent(StreamEvent.streamComplete());
                    log.debug("Stream completed successfully");
                } catch (Exception e) {
                    log.error("Error during stream processing", e);
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
                
                if ("tool".equals(msg.getRole())) {
                    msgNode.put("tool_call_id", msg.getToolCallId());
                    msgNode.put("content", msg.getContent() != null ? msg.getContent() : "");
                } else if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    if (msg.getContent() != null) {
                        msgNode.put("content", msg.getContent());
                    }
                    ArrayNode toolCallsArray = msgNode.putArray("tool_calls");
                    for (ToolCall tc : msg.getToolCalls()) {
                        ObjectNode tcNode = toolCallsArray.addObject();
                        tcNode.put("id", tc.getId());
                        tcNode.put("type", "function");
                        ObjectNode funcNode = tcNode.putObject("function");
                        funcNode.put("name", tc.getToolIdentifier());
                        if (tc.getParams() != null && tc.getParams().getValues() != null && !tc.getParams().getValues().isEmpty()) {
                            try {
                                funcNode.put("arguments", mapper.writeValueAsString(tc.getParams().getValues()));
                            } catch (Exception e) {
                                funcNode.put("arguments", "{}");
                            }
                        } else {
                            funcNode.put("arguments", "{}");
                        }
                    }
                } else {
                    msgNode.put("content", msg.getContent() != null ? msg.getContent() : "");
                }
            }
        }

        if (!tools.isEmpty()) {
            ArrayNode toolsArray = json.putArray("tools");
            for (Tool tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode functionNode = toolNode.putObject("function");
                ToolDefinition desc = tool.definition();
                functionNode.put("name", desc.getIdentifier());
                functionNode.put("description", desc.getDescription());
                if (desc.getParams() != null && !desc.getParams().isEmpty()) {
                    ObjectNode parametersNode = functionNode.putObject("parameters");
                    parametersNode.put("type", "object");
                    ObjectNode propertiesNode = parametersNode.putObject("properties");
                    for (ToolParamDefinition param : desc.getParams()) {
                        ObjectNode paramNode = propertiesNode.putObject(param.getName());
                        paramNode.put("type", param.getType().name().toLowerCase());
                        paramNode.put("description", param.getDescription());
                        if (param.isRequired()) {
                            parametersNode.putArray("required").add(param.getName());
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
                    log.debug("Response contains {} tool calls", toolCalls.size());
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
            log.error("Failed to parse response", e);
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

        ToolCall toolCall = new ToolCall(toolId, funcName);

        try {
            ObjectNode argsNode = mapper.readValue(argumentsJson, ObjectNode.class);
            Iterator<Map.Entry<String, JsonNode>> fields = argsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                toolCall.addParam(field.getKey(), field.getValue().asText());
            }
        } catch (IOException e) {
            log.warn("Failed to parse tool call arguments: {}", argumentsJson, e);
        }

        return toolCall;
    }

    private ModelUsage parseUsage(ObjectNode usageNode) {
        ModelUsage usage = new ModelUsage();
        usage.setInputTokens(usageNode.has("prompt_tokens") ? (long) usageNode.get("prompt_tokens").asInt() : 0L);
        usage.setOutputTokens(usageNode.has("completion_tokens") ? (long) usageNode.get("completion_tokens").asInt() : 0L);
        return usage;
    }

    private static class ToolCallBuilder {
        private final String id;
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        ToolCallBuilder(String id) {
            this.id = id;
        }

        ToolCall build(ObjectMapper mapper) {
            String funcName = name.toString();
            String argsJson = arguments.toString();
            
            ToolCall toolCall = new ToolCall(id, funcName);
            
            if (argsJson != null && !argsJson.isEmpty()) {
                try {
                    ObjectNode argsNode = mapper.readValue(argsJson, ObjectNode.class);
                    Iterator<Map.Entry<String, JsonNode>> fields = argsNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        toolCall.addParam(field.getKey(), field.getValue().asText());
                    }
                } catch (IOException e) {
                    log.warn("Failed to parse tool call arguments: {}", argsJson, e);
                }
            }
            
            return toolCall;
        }
    }
}
