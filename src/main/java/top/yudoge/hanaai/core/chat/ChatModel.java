package top.yudoge.hanaai.core.chat;

import top.yudoge.hanaai.core.chat.stream.ChatStreamListener;
import top.yudoge.hanaai.core.tool.Tool;

import java.util.List;

public interface ChatModel {

    /**
     * 获取当前聊天选项配置
     */
    ChatOptions options();

    /**
     * 设置聊天选项配置
     */
    void options(ChatOptions options);

    /**
     * 给模型注册工具，允许运行时动态注册，chat发起时会使用当时具有的所有tool
     */
    void registerTool(Tool tool);

    /**
     * 聊天 携带历史会话
     * @param message
     * @return
     */
    ChatResponse chat(List<Message> message);

    /**
     * 聊天
     * @param message
     * @return
     */
    ChatResponse chat(Message message);

    /**
     * 流式聊天 携带历史会话
     * @param messages
     * @param listener
     */
    void streamChat(List<Message> messages, ChatStreamListener listener);

    /**
     * 流式聊天 携带历史会话
     * @param message
     * @param listener
     */
    void streamChat(Message message, ChatStreamListener listener);


    /**
     * 模型是否支持工具调用
     * @return
     */
    boolean supportToolCalling();

    /**
     * 模型是否支持结构化输出
     * @return
     */
    boolean supportStructuredOutput();

    /**
     * 模型是否支持并行工具调用
     * @return
     */
    boolean supportParallelToolCalling();

}
