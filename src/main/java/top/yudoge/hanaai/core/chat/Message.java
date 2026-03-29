package top.yudoge.hanaai.core.chat;

import top.yudoge.hanaai.core.tool.ToolCall;
import java.util.ArrayList;
import java.util.List;

public class Message {

    private String role;
    private String content;
    private String toolCallId;
    private List<ToolCall> toolCalls;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public void addToolCall(ToolCall toolCall) {
        if (this.toolCalls == null) {
            this.toolCalls = new ArrayList<>();
        }
        this.toolCalls.add(toolCall);
    }

    public static Message user(String content) {
        Message message = new Message();
        message.setContent(content);
        message.setRole("user");
        return message;
    }

    public static Message assistant(String content) {
        Message message = new Message();
        message.setContent(content);
        message.setRole("assistant");
        return message;
    }

    public static Message assistantWithToolCalls(List<ToolCall> toolCalls) {
        Message message = new Message();
        message.setRole("assistant");
        message.setToolCalls(toolCalls);
        return message;
    }

    public static Message toolResult(String toolCallId, String content) {
        Message message = new Message();
        message.setRole("tool");
        message.setToolCallId(toolCallId);
        message.setContent(content);
        return message;
    }

    public static Message system(String content) {
        Message message = new Message();
        message.setContent(content);
        message.setRole("system");
        return message;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Message{");
        sb.append("role='").append(role).append('\'');
        sb.append(", content='").append(content).append('\'');
        sb.append(", toolCallId='").append(toolCallId).append('\'');
        sb.append(", toolCalls=").append(toolCalls);
        sb.append('}');
        return sb.toString();
    }
}
