package top.yudoge.hanaai.core.tool;

public interface Tool {
    ToolDefinition definition();
    ToolCallResult invoke(ToolCall toolCall);
}
