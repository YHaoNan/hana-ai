package top.yudoge.hanaai.core.chat;

public class Message {

    private String role;

    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return content;
    }

    public void setMessage(String content) {
        this.content = content;
    }

    public static Message user(String content) {
        Message message = new Message();
        message.setMessage(content);
        message.setRole("user");
        return message;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Message{");
        sb.append("role='").append(role).append('\'');
        sb.append(", content='").append(content).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
