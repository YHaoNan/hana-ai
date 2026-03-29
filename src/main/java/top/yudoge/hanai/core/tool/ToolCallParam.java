package top.yudoge.hanai.core.tool;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class ToolCallParam {
    private Map<String, Object> values;

    public ToolCallParam() {
        this.values = new HashMap<>();
    }

    public void put(String name, Object value) {
        values.put(name, value);
    }

    public Object get(String name) {
        return values.get(name);
    }
}
