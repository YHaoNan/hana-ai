package top.yudoge.hanaai.core.tool;

import lombok.Data;

@Data
public class ToolParamDefinition {
    private String name;
    private Type type;
    private String description;
    private boolean required;

    public static ToolParamDefinitionBuilder builder() {
        return new ToolParamDefinitionBuilder();
    }

    public static class ToolParamDefinitionBuilder {
        private String name;
        private Type type;
        private String description;
        private boolean required;

        public ToolParamDefinitionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ToolParamDefinitionBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public ToolParamDefinitionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ToolParamDefinitionBuilder required(boolean required) {
            this.required = required;
            return this;
        }

        public ToolParamDefinition build() {
            ToolParamDefinition param = new ToolParamDefinition();
            param.name = this.name;
            param.type = this.type;
            param.description = this.description;
            param.required = this.required;
            return param;
        }
    }
}
