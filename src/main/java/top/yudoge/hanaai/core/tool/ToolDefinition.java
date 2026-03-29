package top.yudoge.hanaai.core.tool;

import lombok.Data;
import java.util.List;

@Data
public class ToolDefinition {
    private String identifier;
    private String description;
    private List<ToolParamDefinition> params;
    private ToolResultDefinition result;

    public static ToolDefinitionBuilder builder() {
        return new ToolDefinitionBuilder();
    }

    public static class ToolDefinitionBuilder {
        private String identifier;
        private String description;
        private List<ToolParamDefinition> params;
        private ToolResultDefinition result;

        public ToolDefinitionBuilder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public ToolDefinitionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ToolDefinitionBuilder params(List<ToolParamDefinition> params) {
            this.params = params;
            return this;
        }

        public ToolDefinitionBuilder result(ToolResultDefinition result) {
            this.result = result;
            return this;
        }

        public ToolDefinition build() {
            ToolDefinition def = new ToolDefinition();
            def.identifier = this.identifier;
            def.description = this.description;
            def.params = this.params;
            def.result = this.result;
            return def;
        }
    }
}
