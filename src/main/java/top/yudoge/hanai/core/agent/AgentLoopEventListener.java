package top.yudoge.hanai.core.agent;

import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;

public interface AgentLoopEventListener {

    void onThinkingStart(String turnId);

    void onThinking(String turnId, String content);

    void onThinkingEnd(String turnId, Long timeConsumed);



    void onToolCallingStart(String turnId, ToolDefinition definition, ToolCall toolCall);

    void onToolCallingEnd(String turnId, ToolCall toolCall, ToolCallResult toolCallResult, Long timeConsumed);


    void onError(String reason, Exception e, Long timeConsumed);
    void onComplete(Long timeConsumed);
    void onFinish(String turnId, AgentUsage usage, Long timeConsumed);

}
