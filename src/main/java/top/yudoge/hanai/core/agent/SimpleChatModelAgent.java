package top.yudoge.hanai.core.agent;

import top.yudoge.hanai.core.chat.ChatModel;
import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.chat.ModelUsage;
import top.yudoge.hanai.core.chat.stream.ChatStreamListener;
import top.yudoge.hanai.core.memory.Memory;
import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.utils.AppLogger;
import top.yudoge.hanai.utils.ValidationUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleChatModelAgent implements ChatModelAgent {

    private static final AppLogger log = AppLogger.get(SimpleChatModelAgent.class);

    private ChatModel chatModel;

    private AtomicBoolean started = new AtomicBoolean(false);

    private final Memory memory;

    private final ExecutorService agentLoopExecutor;

    private final ExecutorService toolCallExecutor;

    private final AgentUsage agentUsage;

    private final Map<String, Tool> toolMap;

    public SimpleChatModelAgent(Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory must not be null");
        }

        this.memory = memory;

        this.agentLoopExecutor = Executors.newSingleThreadExecutor();
        this.toolCallExecutor = Executors.newCachedThreadPool();
        this.agentUsage = new AgentUsage();
        this.toolMap = new ConcurrentHashMap<>();
        log.info("SimpleChatModelAgent initialized");
    }

    @Override
    public void registerChatModel(ChatModel chatModel) {
        if (!chatModel.supportToolCalling()) {
            log.error("ChatModel does not support tool calling: {}", chatModel.getClass().getName());
            throw new IllegalArgumentException("The chat model does not support tool calling: " + chatModel.getClass().getName());
        }

        this.chatModel = chatModel;
        log.info("ChatModel registered: {}", chatModel.getClass().getSimpleName());
    }

    @Override
    public void registerTool(Tool tool) {
        if (!ValidationUtil.toolNotBlank(tool)) {
            log.error("Invalid tool: {}", tool.getClass().getName());
            throw new IllegalArgumentException("The tool must not be blank: " + tool.getClass().getName());
        }
        if (toolMap.containsKey(tool.definition().getIdentifier())) {
            log.warn("Tool already exists: {}", tool.definition().getIdentifier());
            throw new IllegalArgumentException("The tool already exists: " + tool.definition().getIdentifier());
        }

        this.toolMap.put(tool.definition().getIdentifier(), tool);
        chatModel.registerTool(tool);
        log.debug("Tool registered: {}", tool.definition().getIdentifier());
    }

    @Override
    public void start(Message initialMessage, AgentLoopEventListener listener) {
        if (!ValidationUtil.messageNotBlank(initialMessage)) {
            log.error("Invalid initial message");
            throw new IllegalArgumentException("Message must not be blank");
        }

        if (!started.compareAndSet(false, true)) {
            log.warn("Agent is already started");
            throw new IllegalStateException("Agent is already started");
        }

        log.info("Starting agent with message: {}", initialMessage.getContent());
        Future f = this.agentLoopExecutor.submit(() -> {
            memory.add(initialMessage);
            agentLoop(listener);
        });

        try {
            f.get();
        } catch (InterruptedException e) {
            log.error("Agent interrupted", e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Agent execution failed", e);
            throw new RuntimeException(e);
        }
    }

    private void agentLoop(AgentLoopEventListener listener) {
        int turnCount = 0;
        while (true) {
            turnCount++;
            CountDownLatch turnLatch = new CountDownLatch(1);
            String turnId = UUID.randomUUID().toString();
            long turnStartTime = System.currentTimeMillis();

            List<ToolCall> toolCallList = new ArrayList<>();
            List<CompletableFuture<ToolCallResult>> toolCallFutures = new ArrayList<>();
            Map<String, ToolCall> toolCallsById = new HashMap<>();

            log.debug("Turn {} started", turnCount);
            listener.onThinkingStart(turnId);

            StringBuilder assistantResponseBuffer = new StringBuilder();

            chatModel.streamChat(memory.messages(), new ChatStreamListener() {

                @Override
                public void onTextDelta(String content) {
                    assistantResponseBuffer.append(content);
                    listener.onThinking(turnId, content);
                }

                @Override
                public void onTextComplete(String content) {
                    listener.onThinkingEnd(turnId, System.currentTimeMillis() - turnStartTime);
                }

                @Override
                public void onToolCall(ToolCall toolCall) {
                    if (ValidationUtil.toolCallNotBlank(toolCall) && toolCall.getId() != null) {
                        if (!toolCallsById.containsKey(toolCall.getId())) {
                            toolCallsById.put(toolCall.getId(), toolCall);
                            toolCallList.add(toolCall);
                            log.debug("Received tool call: {} with params {}", toolCall.getToolIdentifier(), toolCall.getParams().getValues());

                            CompletableFuture<ToolCallResult> future = CompletableFuture.supplyAsync(() -> {
                                return executeToolCall(turnId, toolCall, listener);
                            }, toolCallExecutor);

                            toolCallFutures.add(future);
                        }
                    }
                }

                @Override
                public void onError(String reason, Exception e) {
                    log.error("Stream error: {}", reason, e);
                    listener.onError(reason, e, System.currentTimeMillis() - turnStartTime);
                }

                @Override
                public void onFinish(ModelUsage usage) {
                    turnLatch.countDown();
                }
            });

            try {
                turnLatch.await();
            } catch (InterruptedException e) {
                log.error("Turn interrupted", e);
                throw new RuntimeException(e);
            }

            if (!toolCallList.isEmpty()) {
                memory.add(Message.assistantWithToolCalls(toolCallList));
                log.debug("Added {} tool calls to memory", toolCallList.size());
            } else if (assistantResponseBuffer.length() > 0) {
                memory.add(Message.assistant(assistantResponseBuffer.toString()));
                log.debug("Added assistant response to memory: {} chars", assistantResponseBuffer.length());
            }

            if (!toolCallFutures.isEmpty()) {
                try {
                    for (int i = 0; i < toolCallFutures.size(); i++) {
                        ToolCallResult toolCallResult = toolCallFutures.get(i).get();
                        ToolCall toolCall = toolCallList.get(i);
                        if (toolCallResult != null && toolCallResult.getValue() != null) {
                            memory.add(Message.toolResult(toolCall.getId(), toolCallResult.getValue().toString()));
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process tool call results", e);
                    listener.onError("Failed to wait for tool calls", e, System.currentTimeMillis() - turnStartTime);
                }
            }

            listener.onFinish(turnId, null, System.currentTimeMillis() - turnStartTime);
            log.debug("Turn {} finished, duration: {}ms", turnCount, System.currentTimeMillis() - turnStartTime);

            if (toolCallList.isEmpty()) {
                started.set(false);
                log.info("Agent finished after {} turns", turnCount);
                break;
            }
        }
    }

    private ToolCallResult executeToolCall(String turnId, ToolCall toolCall, AgentLoopEventListener listener) {
        long startTime = System.currentTimeMillis();
        Tool tool = toolMap.get(toolCall.getToolIdentifier());

        if (tool == null) {
            log.error("Tool not found: {}", toolCall.getToolIdentifier());
            Exception e = new IllegalArgumentException("Tool not found: " + toolCall.getToolIdentifier());
            listener.onError("Tool not found", e, System.currentTimeMillis() - startTime);
            return null;
        }

        try {
            log.info("Executing tool: {} with params {}", toolCall.getToolIdentifier(), toolCall.getParams().getValues());
            listener.onToolCallingStart(turnId, tool.definition(), toolCall);
            ToolCallResult result = tool.invoke(toolCall);
            listener.onToolCallingEnd(turnId, toolCall, result, System.currentTimeMillis() - startTime);
            log.debug("Tool executed successfully in {}ms", System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolCall.getToolIdentifier(), e);
            listener.onToolCallingEnd(turnId, toolCall, null, System.currentTimeMillis() - startTime);
            listener.onError("Tool execution failed: " + toolCall.getToolIdentifier(), e, System.currentTimeMillis() - startTime);
            return null;
        }
    }

    public void shutdown() {
        log.info("Shutting down agent");
        agentLoopExecutor.shutdown();
        toolCallExecutor.shutdown();
    }
}
