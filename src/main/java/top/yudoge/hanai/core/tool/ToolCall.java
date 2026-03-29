package top.yudoge.hanai.core.tool;

import lombok.Data;

@Data
public class ToolCall {
    private String id;
    private String toolIdentifier;
    private Tool tool;
    private ToolCallParam params;
    private ToolCallResult result;
    private long startTime;
    private long endTime;

    public ToolCall(String id, String toolIdentifier) {
        this.id = id;
        this.toolIdentifier = toolIdentifier;
        this.params = new ToolCallParam();
    }

    public void addParam(String name, Object value) {
        if (this.params == null) {
            this.params = new ToolCallParam();
        }
        this.params.put(name, value);
    }

    public Object getParam(String name) {
        return params != null ? params.get(name) : null;
    }
}
