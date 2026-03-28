package top.yudoge.hanaai.test;

import top.yudoge.hanaai.core.chat.ChatResponse;
import top.yudoge.hanaai.core.chat.stream.ChatStreamListener;
import top.yudoge.hanaai.core.chat.ModelUsage;
import top.yudoge.hanaai.core.chat.Message;
import top.yudoge.hanaai.openai.OpenAIChatModel;
import top.yudoge.hanaai.utils.EnvUtil;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class OpenAIChatModelTest {
    public static void main(String[] args) throws InterruptedException {

        String baseUrl = EnvUtil.get("OPENAI_BASE_URL");
        String modelName = EnvUtil.get("OPENAI_MODEL_NAME");
        String apiKey = EnvUtil.get("OPENAI_API_KEY");

        CountDownLatch latch = new CountDownLatch(1);
        OpenAIChatModel openAIChatModel = new OpenAIChatModel(baseUrl, modelName, apiKey);

        ChatResponse response = openAIChatModel.chat(Message.user("你好，你是什么玩意儿"));
        System.out.println(response);

        openAIChatModel.streamChat(Collections.singletonList(Message.user("你好，你是什么玩意儿")), new ChatStreamListener() {
            @Override
            public void onTextDelta(String content) {
                System.out.print(content);
            }

            @Override
            public void onError(String reason, Exception e) {
                System.out.println(reason);
                throw new RuntimeException(e);
            }

            @Override
            public void onComplete() {
                System.out.println("\n======DONE======");
            }

            @Override
            public void onFinish(ModelUsage usage) {
                System.out.println("finish");
                latch.countDown();
            }
        });

        latch.await();
    }
}
