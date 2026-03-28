package top.yudoge.hanaai.core.chat;

import java.util.List;

public class ChatOptions {

    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer frequencyPenalty;
    private Integer presencePenalty;
    private List<String> stop;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public ChatOptions withTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public ChatOptions withMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public ChatOptions withTopP(Double topP) {
        this.topP = topP;
        return this;
    }

    public Integer getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Integer frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public ChatOptions withFrequencyPenalty(Integer frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }

    public Integer getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Integer presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public ChatOptions withPresencePenalty(Integer presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public ChatOptions withStop(List<String> stop) {
        this.stop = stop;
        return this;
    }
}
