package top.yudoge.hanaai.core;

public class LLMResponse {

    private Message message;

    private LLMUsage usage;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public LLMUsage getUsage() {
        return usage;
    }

    public void setUsage(LLMUsage usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LLMResponse{");
        sb.append("message=").append(message);
        sb.append('}');
        return sb.toString();
    }
}
