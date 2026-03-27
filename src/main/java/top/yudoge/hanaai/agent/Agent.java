package top.yudoge.hanaai.agent;

import top.yudoge.hanaai.core.Message;

public interface Agent {

    void start(
            Message initialPrompt,
            AgentLoopEventListener listener
    );

}
