package top.yudoge.hanaai.core.memory;

import top.yudoge.hanaai.core.chat.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FixedCharMemory implements Memory {

    private final int maxChars;
    private final LinkedList<Message> messages = new LinkedList<>();

    public FixedCharMemory(int maxChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        this.maxChars = maxChars;
    }

    @Override
    public void add(Message message) {
        messages.addLast(message);
        while (overCharLimit() && !messages.isEmpty()) {
            messages.removeFirst();
        }
    }

    @Override
    public List<Message> messages() {
        return new ArrayList<>(messages);
    }

    private boolean overCharLimit() {
        int total = 0;
        for (Message msg : messages) {
            String content = msg.getContent();
            if (content != null) {
                total += content.length();
            }
        }
        return total > maxChars;
    }
}
