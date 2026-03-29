package top.yudoge.hanai.core.agent;

import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.tool.Tool;

public interface Agent {

    void registerTool(Tool tool);

    void start(
            Message initialMessage,
            AgentLoopEventListener listener
    );

}
