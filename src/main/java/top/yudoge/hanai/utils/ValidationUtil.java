package top.yudoge.hanai.utils;

import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.chat.ModelUsage;
import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolDefinition;

import java.util.Collection;

public class ValidationUtil {

    public static boolean usageNotBlank(ModelUsage usage) {
        return usage != null && usage.getInputTokens() != null && usage.getOutputTokens() != null;
    }

    public static boolean toolCallNotBlank(ToolCall toolCall) {
        return toolCall != null && toolCall.getToolIdentifier() != null;
    }

    public static <E> boolean iterable(Collection<E> coll) {
        return coll != null && !coll.isEmpty();
    }


    public static boolean messageNotBlank(Message message) {
        return message != null &&
                stringNotEmpty(message.getContent()) &&
                stringNotEmpty(message.getRole());
    }

    public static boolean notNull(Object object) {
        return object != null;
    }

    public static boolean stringNotBlank(String string) {
        return string != null && !string.trim().isEmpty();
    }

    public static boolean stringNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public static boolean toolNotBlank(Tool tool) {
        return tool != null && toolDescriptionNotBlank(tool.definition());
    }

    public static boolean toolDescriptionNotBlank(ToolDefinition toolDefinition) {
        return (toolDefinition != null && toolDefinition.getIdentifier() != null);
    }

}
