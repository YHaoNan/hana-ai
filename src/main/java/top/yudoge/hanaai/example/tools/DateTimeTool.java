package top.yudoge.hanaai.example.tools;

import top.yudoge.hanaai.core.tool.Tool;
import top.yudoge.hanaai.core.tool.ToolCall;
import top.yudoge.hanaai.core.tool.ToolCallResult;
import top.yudoge.hanaai.core.tool.ToolDefinition;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTool implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("get_current_datetime")
                .description("Get the current date and time")
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        ToolCallResult result = new ToolCallResult();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        result.setValue(now.format(formatter));
        return result;
    }
}
