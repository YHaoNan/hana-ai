package top.yudoge.hanaai.core.chat;

public class ModelUsage {

    private Long inputTokens;

    private Long outputTokens;

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public static ModelUsage zero() {
        ModelUsage modelUsage = new ModelUsage();
        modelUsage.setInputTokens(0L);
        modelUsage.setOutputTokens(0L);
        return modelUsage;
    }
}
