package top.yudoge.hanai.core.memory;

import top.yudoge.hanai.core.chat.Message;

import java.util.List;

public interface Memory {

    void add(Message message);

    List<Message> messages();

//    void registerOnMemoryEvictedListener();

}
