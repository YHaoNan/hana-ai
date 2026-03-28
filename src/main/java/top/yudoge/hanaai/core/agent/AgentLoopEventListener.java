package top.yudoge.hanaai.core.agent;

import top.yudoge.hanaai.core.tool.ToolDescription;

public interface AgentLoopEventListener {

    void onStartThinking();

    void onThinking(String content);

    void onEndThinking();





    void onToolCalling(ToolDescription description); // todo




    void onStartSummarizing();

    void onSummarizing(String content);

    void onEndSummarizing();



    void onError(String reason, Exception e);
    void onComplete();
    void onFinish(AgentUsage usage);

}
