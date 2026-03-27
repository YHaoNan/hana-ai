package top.yudoge.hanaai.test;

import io.github.cdimascio.dotenv.Dotenv;
import top.yudoge.hanaai.core.*;
import top.yudoge.hanaai.utils.EnvUtil;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class OpenAILLMTest {
    public static void main(String[] args) throws InterruptedException {

        String baseUrl = EnvUtil.get("OPENAI_BASE_URL");
        String modelName = EnvUtil.get("OPENAI_MODEL_NAME");
        String apiKey = EnvUtil.get("OPENAI_API_KEY");

        CountDownLatch latch = new CountDownLatch(1);
        OpenAILLM openAILLM = new OpenAILLM(baseUrl, modelName, apiKey);

//        LLMResponse response = openAILLM.chat(Message.user("你好，你是什么玩意儿"));
//        System.out.println(response);

        openAILLM.streamChat(Collections.singletonList(Message.user("你好，你是什么玩意儿")), new LLMStreamListener() {
            @Override
            public void onResponse(String content) {
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
            public void onFinish(LLMUsage usage) {
                System.out.println("finish");
                latch.countDown();
            }
        });

        latch.await();
    }
}
