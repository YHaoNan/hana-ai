package top.yudoge.hanai.core.memory;

import top.yudoge.hanai.core.chat.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FixedCountMemory implements Memory {

    private final int maxCount;
    private final LinkedList<Message> messages = new LinkedList<>();

    public FixedCountMemory(int maxCount) {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount must be positive");
        }
        this.maxCount = maxCount;
    }

    @Override
    public void add(Message message) {
        messages.addLast(message);
        while (messages.size() > maxCount) {
            messages.removeFirst();
        }
    }

    @Override
    public List<Message> messages() {
        return new ArrayList<>(messages);
    }
}
