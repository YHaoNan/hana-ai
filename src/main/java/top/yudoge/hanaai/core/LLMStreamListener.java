package top.yudoge.hanaai.core;


public interface LLMStreamListener {

    /**
     * 流式响应时回调
     * @param content
     */
    void onResponse(String content);

    /**
     * 错误时回调
     * @param reason 错误原因
     * @param e      异常，可能为空
     */
    void onError(String reason, Exception e);

    /**
     * 成功结束时回调
     */
    void onComplete();

    /**
     * 任何情况下结束时回调（成功/失败），并返回LLMUsage
     * @param usage
     */
    void onFinish(LLMUsage usage);

}
