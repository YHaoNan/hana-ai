package top.yudoge.hanaai.core.chat;

import lombok.Data;
import top.yudoge.hanaai.core.tool.ToolCall;

import java.util.List;

import static top.yudoge.hanaai.utils.ValidationUtil.*;

@Data
public class ChatResponse {

    private final ModelUsage usage;

    private final Message message;

    private final List<ToolCall> toolCall;

    private final boolean isConversation;

    private final boolean isToolCalling;

    private final boolean hasUsage;

    private ChatResponse(
            ModelUsage usage,
            Message message,
            List<ToolCall> toolCall,
            boolean isConversation,
            boolean isToolCalling,
            boolean hasUsage
    ) {
        this.usage = usage;
        this.message = message;
        this.toolCall = toolCall;
        this.isConversation = isConversation;
        this.isToolCalling = isToolCalling;
        this.hasUsage = hasUsage;
    }


    public static ChatResponse conversation(Message message) {
        if (!messageNotBlank(message)) {
            throw new IllegalArgumentException("The message cannot be null or empty.");
        }
        return new ChatResponse(
                null, message, null, true, false, false
        );
    }

    public static ChatResponse conversation(Message message, ModelUsage usage) {
        if (!usageNotBlank(usage)) {
            throw new IllegalArgumentException("The usage cannot be null or blank. Please try conversation(Message).");
        }
        return new ChatResponse(usage, message, null, true, false, usageNotBlank(usage));
    }

    public static ChatResponse toolCalling(List<ToolCall> toolCall) {
        if (!iterable(toolCall)) {
            throw new IllegalArgumentException("Tool call list can not be empty.");
        }

        for (ToolCall call : toolCall) {
            if (!toolCallNotBlank(call)) {
                throw new IllegalArgumentException("The tool call cannot be null or blank. ");
            }
        }

        return new ChatResponse(null, null, toolCall, false, true, false);
    }

    public static ChatResponse toolCalling(List<ToolCall> toolCall, ModelUsage usage) {
        if (!iterable(toolCall)) {
            throw new IllegalArgumentException("Tool call list can not be empty.");
        }

        for (ToolCall call : toolCall) {
            if (!toolCallNotBlank(call)) {
                throw new  IllegalArgumentException("The tool call cannot be null or blank. ");
            }
        }

        if (!usageNotBlank(usage)) {
            throw new IllegalArgumentException("The tool call cannot be null or blank. ");
        }
        return new ChatResponse(usage, null, toolCall, false, true, usageNotBlank(usage));
    }
}
