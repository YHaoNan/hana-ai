package top.yudoge.hanaai.core.agent;

import top.yudoge.hanaai.core.chat.Message;
import top.yudoge.hanaai.core.memory.Memory;
import top.yudoge.hanaai.core.tool.Tool;

public interface Agent {

    void registerTool(Tool tool);

    void start(
            Message initialMessage,
            AgentLoopEventListener listener
    );

}
