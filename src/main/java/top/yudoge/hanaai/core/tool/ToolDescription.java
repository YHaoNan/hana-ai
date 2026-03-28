package top.yudoge.hanaai.core.tool;

import lombok.Data;

import java.util.List;

@Data
public class ToolDescription {

    private String id;

    private String description;

    private List<ToolParamDescription> params;

    private ToolResultDescription result;
}
