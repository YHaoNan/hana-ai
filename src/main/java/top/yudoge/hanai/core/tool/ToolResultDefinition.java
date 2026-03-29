package top.yudoge.hanai.core.tool;

import lombok.Data;

@Data
public class ToolResultDefinition {
    private Type type;
    private String description;

    public static ToolResultDefinitionBuilder builder() {
        return new ToolResultDefinitionBuilder();
    }

    public static class ToolResultDefinitionBuilder {
        private Type type;
        private String description;

        public ToolResultDefinitionBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public ToolResultDefinitionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ToolResultDefinition build() {
            ToolResultDefinition result = new ToolResultDefinition();
            result.type = this.type;
            result.description = this.description;
            return result;
        }
    }
}
