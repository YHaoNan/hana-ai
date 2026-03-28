package top.yudoge.hanaai.core.chat.stream;

public enum StreamEventType {

    /**
     * 模型返回了一个文本增量
     */
    TextDelta,

    /**
     * 模型输出文本完毕
     */
    TextComplete,

    /**
     * 模型返回工具调用
     */
    ToolCallReceived,

    /**
     * 模型返回用量信息
     */
    UsageReceived,

    /**
     * 流结束
     */
    StreamComplete,

    /**
     * 出错
     */
    Error

}
