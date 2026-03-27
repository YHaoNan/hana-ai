package top.yudoge.hanaai.core;

public class LLMUsage {

    private Boolean hasUsageStatics;

    private Long inputTokens;

    private Long outputTokens;

    public Boolean getHasUsageStatics() {
        return hasUsageStatics;
    }

    public void setHasUsageStatics(Boolean hasUsageStatics) {
        this.hasUsageStatics = hasUsageStatics;
    }

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
}
