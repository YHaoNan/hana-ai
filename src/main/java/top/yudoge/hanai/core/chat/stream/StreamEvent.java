package top.yudoge.hanai.core.chat.stream;

import top.yudoge.hanai.core.chat.ModelUsage;
import top.yudoge.hanai.core.tool.ToolCall;

import static top.yudoge.hanai.utils.ValidationUtil.*;


public class StreamEvent {


    private final StreamEventType eventType;

    private final String textDelta;

    private final ToolCall toolCall;

    private final ModelUsage modelUsage;

    private final Exception exception;

    private StreamEvent(
            StreamEventType eventType,
            String textDelta,
            ToolCall toolCall,
            ModelUsage modelUsage,
            Exception exception
    ) {
        this.eventType = eventType;
        this.textDelta = textDelta;
        this.toolCall = toolCall;
        this.modelUsage = modelUsage;
        this.exception = exception;
    }

    public boolean isTextDelta() {
        return StreamEventType.TextDelta == eventType;
    }

    public boolean isTextComplete() {
        return StreamEventType.TextComplete == eventType;
    }

    public boolean isStreamComplete() {
        return StreamEventType.StreamComplete == eventType;
    }

    public boolean isToolCall() {
        return StreamEventType.ToolCallReceived == eventType;
    }

    public boolean isModelUsage() {
        return StreamEventType.UsageReceived == eventType;
    }


    public boolean isError() {
        return StreamEventType.Error == eventType;
    }

    public StreamEventType getEventType() {
        return eventType;
    }

    public String getTextDelta() {
        return textDelta;
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    public ModelUsage getModelUsage() {
        return modelUsage;
    }

    public Exception getException() {
        return exception;
    }

    public static StreamEvent textDelta(String textDelta) {
        if (!stringNotEmpty(textDelta)) {
            throw new IllegalArgumentException("Text delta can not be null or empty");
        }
        return new StreamEvent(StreamEventType.TextDelta, textDelta, null, null, null);
    }

    public static StreamEvent textComplete(String textComplete) {
        return new StreamEvent(StreamEventType.TextComplete, textComplete, null, null, null);
    }

    public static StreamEvent streamComplete() {
        return new StreamEvent(StreamEventType.StreamComplete, null, null, null, null);
    }

    public static StreamEvent toolCall(ToolCall toolCall) {
        if (!toolCallNotBlank(toolCall)) {
            throw new IllegalArgumentException("The tool call cannot be null or blank. ");
        }
        return new StreamEvent(StreamEventType.ToolCallReceived, null, toolCall, null, null);
    }

    public static StreamEvent usageReceived(ModelUsage modelUsage) {
        if (!usageNotBlank(modelUsage)) {
            throw new IllegalArgumentException("The model usage cannot be null or blank. ");
        }
        return new StreamEvent(StreamEventType.UsageReceived, null, null, modelUsage, null);
    }

    public static StreamEvent error(Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException("The exception cannot be null.");
        }
        return new StreamEvent(StreamEventType.Error, null, null, null, exception);
    }


}
