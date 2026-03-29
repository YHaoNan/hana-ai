package top.yudoge.hanai.core.chat.stream;

import top.yudoge.hanai.core.chat.ModelUsage;
import top.yudoge.hanai.core.tool.ToolCall;

public interface ChatStreamListener {

    default void onEvent(StreamEvent event) {
        if (event.isTextDelta()) {
            onTextDelta(event.getTextDelta());
        } else if (event.isTextComplete()) {
            onTextComplete(event.getTextDelta());
        } else if (event.isToolCall()) {
            onToolCall(event.getToolCall());
        } else if (event.isModelUsage()) {
            onUsage(event.getModelUsage());
        } else if (event.isStreamComplete()) {
            onComplete();
        } else if (event.isError()) {
            onError(event.getException().getMessage(), event.getException());
        }
    }

    default void onTextDelta(String content) {}

    default void onTextComplete(String content) {}

    default void onToolCall(ToolCall toolCall) {}

    default void onUsage(ModelUsage usage) {}

    default void onError(String reason, Exception e) {}

    default void onComplete() {}

    default void onFinish(ModelUsage usage) {}

}
