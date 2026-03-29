package top.yudoge.hanaai.core.agent;

import lombok.Getter;
import top.yudoge.hanaai.core.chat.ModelUsage;

@Getter
public class AgentUsage {

    private ModelUsage thinkingUsage;

    private ModelUsage toolCallingUsage;

    private ModelUsage summarizingUsage;

    public AgentUsage() {
        this.thinkingUsage = ModelUsage.zero();
        this.toolCallingUsage = ModelUsage.zero();
        this.summarizingUsage = ModelUsage.zero();
    }


}
