package top.yudoge.hanaai.memory;

import top.yudoge.hanaai.core.Message;

import java.util.List;

public interface Memory {

    void add(Message message);

    List<Message> messages();

}
