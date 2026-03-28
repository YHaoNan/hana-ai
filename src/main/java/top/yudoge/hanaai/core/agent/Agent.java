package top.yudoge.hanaai.core.agent;

import top.yudoge.hanaai.core.chat.Message;

public interface Agent {

    void start(
            Message initialPrompt,
            AgentLoopEventListener listener
    );

}
