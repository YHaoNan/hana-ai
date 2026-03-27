package top.yudoge.hanaai.core;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okio.BufferedSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenAILLM implements LLM {

    private final String baseUrl;
    private final String modelName;
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OpenAILLM(String baseUrl, String modelName, String apiKey) {
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
    public LLMResponse chat(List<Message> messages) {
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
    public LLMResponse chat(Message message) {
        return chat(Collections.singletonList(message));
    }

    @Override
    public void streamChat(List<Message> messages, LLMStreamListener listener) {
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
                LLMUsage usage = null;
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
                                        listener.onResponse(content);
                                    }
                                }
                            }
                            JsonNode finishReason = choice.get("finish_reason");
                            if (finishReason != null && "stop".equals(finishReason.asText())) {
                                JsonNode usageNode = node.get("usage");
                                if (usageNode != null && usageNode.isObject()) {
                                    usage = parseUsage((ObjectNode) usageNode);
                                }
                            }
                        }
                    }
                    listener.onComplete();
                } catch (Exception e) {
                    listener.onError(e.getMessage(), e);
                } finally {
                    listener.onFinish(usage);
                }
            }
        });
    }

    private RequestBody buildRequestBody(List<Message> messages, boolean stream) {
        ObjectNode json = mapper.createObjectNode();
        json.put("model", modelName);
        json.put("stream", stream);

        ArrayNode messageArray = json.putArray("messages");
        if (messages != null) {
            for (Message msg : messages) {
                ObjectNode msgNode = messageArray.addObject();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getMessage());
            }
        }

        return RequestBody.create(JSON, json.toString());
    }

    private Request buildRequest(RequestBody body) {
        String url = baseUrl;
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }

    private LLMResponse parseResponse(String responseBody) {
        try {
            ObjectNode node = mapper.readValue(responseBody, ObjectNode.class);
            ArrayNode choices = (ArrayNode) node.get("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("No choices in response");
            }

            ObjectNode choice = (ObjectNode) choices.get(0);
            ObjectNode messageNode = (ObjectNode) choice.get("message");
            Message message = new Message();
            message.setRole(messageNode.has("role") ? messageNode.get("role").asText() : "assistant");
            message.setMessage(messageNode.has("content") ? messageNode.get("content").asText() : "");

            LLMResponse response = new LLMResponse();
            response.setMessage(message);

            ObjectNode usageNode = (ObjectNode) node.get("usage");
            if (usageNode != null) {
                response.setUsage(parseUsage(usageNode));
            }

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private LLMUsage parseUsage(ObjectNode usageNode) {
        LLMUsage usage = new LLMUsage();
        usage.setHasUsageStatics(true);
        usage.setInputTokens(usageNode.has("prompt_tokens") ? (long) usageNode.get("prompt_tokens").asInt() : 0L);
        usage.setOutputTokens(usageNode.has("completion_tokens") ? (long) usageNode.get("completion_tokens").asInt() : 0L);
        return usage;
    }
}