package top.yudoge.hanaai.core.tool;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ToolCall {

    private String toolIdentifier;

    private Map<String, Object> arguments;

    public ToolCall() {
        this.arguments = new HashMap<>();
    }

    public ToolCall(String toolIdentifier) {
        this.toolIdentifier = toolIdentifier;
        this.arguments = new HashMap<>();
    }

    public void addArgument(String name, Object value) {
        if (this.arguments == null) {
            this.arguments = new HashMap<>();
        }
        this.arguments.put(name, value);
    }

    public Object getArgument(String name) {
        return arguments != null ? arguments.get(name) : null;
    }

}
