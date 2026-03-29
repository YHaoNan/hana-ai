package top.yudoge.hanai.core.tool;

public interface Tool {
    ToolDefinition definition();
    ToolCallResult invoke(ToolCall toolCall);
}
