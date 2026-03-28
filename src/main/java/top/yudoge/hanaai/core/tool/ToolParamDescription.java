package top.yudoge.hanaai.core.tool;

import lombok.Data;

@Data
public class ToolParamDescription {

    private String identifier;

    private Type type;

    private String description;

    private Boolean isRequired;

}
