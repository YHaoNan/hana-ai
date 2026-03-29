package top.yudoge.hanai.test;

import top.yudoge.hanai.core.chat.ChatResponse;
import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.memory.FixedCharMemory;
import top.yudoge.hanai.core.memory.FixedCountMemory;
import top.yudoge.hanai.core.memory.Memory;
import top.yudoge.hanai.openai.OpenAIChatModel;
import top.yudoge.hanai.utils.EnvUtil;

import java.util.ArrayList;
import java.util.List;

public class MemoryTest {
    public static void main(String[] args) {
        String baseUrl = EnvUtil.get("OPENAI_BASE_URL");
        String modelName = EnvUtil.get("OPENAI_MODEL_NAME");
        String apiKey = EnvUtil.get("OPENAI_API_KEY");

        OpenAIChatModel chatModel = new OpenAIChatModel(baseUrl, modelName, apiKey);

        testFixedCountMemory(chatModel);
        System.out.println("==================");
        testFixedCharMemory(chatModel);
    }

    private static void testFixedCountMemory(OpenAIChatModel chatModel) {
        System.out.println("=== FixedCountMemory Test (max 4 messages) ===");
        Memory memory = new FixedCountMemory(4);

        // 第1轮对话
        System.out.println("\n--- 第1轮 ---");
        Message user1 = Message.user("你好，我叫小明");
        List<Message> history1 = buildHistory(memory);
        history1.add(user1);
        ChatResponse resp1 = chatModel.chat(history1);
        System.out.println("用户: " + user1.getContent());
        System.out.println("AI: " + truncate(resp1.getMessage().getContent(), 50));
        memory.add(user1);
        memory.add(Message.assistant(resp1.getMessage().getContent()));
        printMemoryState(memory, 1);

        // 第2轮对话
        System.out.println("\n--- 第2轮 ---");
        Message user2 = Message.user("我刚才说我叫什么？");
        List<Message> history2 = buildHistory(memory);
        history2.add(user2);
        ChatResponse resp2 = chatModel.chat(history2);
        System.out.println("用户: " + user2.getContent());
        System.out.println("AI: " + truncate(resp2.getMessage().getContent(), 50));
        memory.add(user2);
        memory.add(Message.assistant(resp2.getMessage().getContent()));
        printMemoryState(memory, 2);

        // 第3轮对话
        System.out.println("\n--- 第3轮 ---");
        Message user3 = Message.user("今天天气怎么样？");
        List<Message> history3 = buildHistory(memory);
        history3.add(user3);
        ChatResponse resp3 = chatModel.chat(history3);
        System.out.println("用户: " + user3.getContent());
        System.out.println("AI: " + truncate(resp3.getMessage().getContent(), 50));
        memory.add(user3);
        memory.add(Message.assistant(resp3.getMessage().getContent()));
        printMemoryState(memory, 3);

        // 第4轮对话 - 超过限制
        System.out.println("\n--- 第4轮 ---");
        Message user4 = Message.user("再问我一遍刚才的问题");
        List<Message> history4 = buildHistory(memory);
        history4.add(user4);
        ChatResponse resp4 = chatModel.chat(history4);
        System.out.println("用户: " + user4.getContent());
        System.out.println("AI: " + truncate(resp4.getMessage().getContent(), 50));
        memory.add(user4);
        memory.add(Message.assistant(resp4.getMessage().getContent()));
        printMemoryState(memory, 4);

        System.out.println("\n最终内存内容 (应为最近4条):");
        for (Message msg : memory.messages()) {
            System.out.println("  " + msg.getRole() + ": " + truncate(msg.getContent(), 30));
        }
        System.out.println("期望: 第1轮被淘汰，保留第2、3、4轮对话");
    }

    private static void testFixedCharMemory(OpenAIChatModel chatModel) {
        System.out.println("\n=== FixedCharMemory Test (max 100 chars) ===");
        Memory memory = new FixedCharMemory(100);

        // 第1轮对话
        System.out.println("\n--- 第1轮 ---");
        String content1 = "你好，我叫小明，这是我的第一个问题。";
        Message user1 = Message.user(content1);
        List<Message> history1 = buildHistory(memory);
        history1.add(user1);
        ChatResponse resp1 = chatModel.chat(history1);
        System.out.println("用户: " + content1);
        System.out.println("AI: " + truncate(resp1.getMessage().getContent(), 50));
        memory.add(user1);
        memory.add(Message.assistant(resp1.getMessage().getContent()));
        printMemoryStateWithChars(memory, 1);

        // 第2轮对话
        System.out.println("\n--- 第2轮 ---");
        String content2 = "我叫什么名字？";
        Message user2 = Message.user(content2);
        List<Message> history2 = buildHistory(memory);
        history2.add(user2);
        ChatResponse resp2 = chatModel.chat(history2);
        System.out.println("用户: " + content2);
        System.out.println("AI: " + truncate(resp2.getMessage().getContent(), 50));
        memory.add(user2);
        memory.add(Message.assistant(resp2.getMessage().getContent()));
        printMemoryStateWithChars(memory, 2);

        // 第3轮对话 - 可能触发截断
        System.out.println("\n--- 第3轮 ---");
        String content3 = "今天天气很好，我想出去散步。";
        Message user3 = Message.user(content3);
        List<Message> history3 = buildHistory(memory);
        history3.add(user3);
        ChatResponse resp3 = chatModel.chat(history3);
        System.out.println("用户: " + content3);
        System.out.println("AI: " + truncate(resp3.getMessage().getContent(), 50));
        memory.add(user3);
        memory.add(Message.assistant(resp3.getMessage().getContent()));
        printMemoryStateWithChars(memory, 3);

        System.out.println("\n最终内存内容 (字符数不应超过100):");
        for (Message msg : memory.messages()) {
            System.out.println("  " + msg.getRole() + ": " + truncate(msg.getContent(), 30));
        }
        int totalChars = calcTotalChars(memory);
        System.out.println("总字符数: " + totalChars + " (应 <= 100)");
        System.out.println("期望: 字符数不超过100，过长的消息被淘汰");
    }

    private static List<Message> buildHistory(Memory memory) {
        List<Message> history = new ArrayList<>(memory.messages());
        return history;
    }

    private static void printMemoryState(Memory memory, int round) {
        System.out.println("  第" + round + "轮后内存消息数: " + memory.messages().size());
    }

    private static void printMemoryStateWithChars(Memory memory, int round) {
        System.out.println("  第" + round + "轮后内存消息数: " + memory.messages().size() +
                ", 总字符数: " + calcTotalChars(memory));
    }

    private static int calcTotalChars(Memory memory) {
        int total = 0;
        for (Message msg : memory.messages()) {
            if (msg.getContent() != null) {
                total += msg.getContent().length();
            }
        }
        return total;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
