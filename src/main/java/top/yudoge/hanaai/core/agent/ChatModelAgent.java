package top.yudoge.hanaai.core.agent;

import top.yudoge.hanaai.core.chat.ChatModel;
import top.yudoge.hanaai.core.memory.Memory;

public interface ChatModelAgent extends Agent {

    void registerChatModel(ChatModel chatModel);



}
