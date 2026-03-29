package top.yudoge.hanaai.core.memory;

import top.yudoge.hanaai.core.chat.Message;

import java.util.List;

public interface Memory {

    void add(Message message);

    List<Message> messages();

//    void registerOnMemoryEvictedListener();

}
