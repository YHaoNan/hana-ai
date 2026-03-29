package top.yudoge.hanaai.core.tool;

import lombok.Data;

@Data
public class ToolCallResult {
    private Object value;
    private boolean success;
    private String errorMessage;

    public static ToolCallResult ok(Object value) {
        ToolCallResult result = new ToolCallResult();
        result.value = value;
        result.success = true;
        return result;
    }

    public static ToolCallResult error(String message) {
        ToolCallResult result = new ToolCallResult();
        result.success = false;
        result.errorMessage = message;
        return result;
    }
}
