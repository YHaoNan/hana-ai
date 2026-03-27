package top.yudoge.hanaai.core;

import java.util.List;

/**
 * 代表一个LLM，核心能力：
 * - chat
 * - 流式chat
 */
public interface LLM {

    LLMResponse chat(List<Message> message);

    LLMResponse chat(Message message);

    void streamChat(List<Message> messages, LLMStreamListener listener);

}
