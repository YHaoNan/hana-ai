package top.yudoge.hanai.example.tools;

import top.yudoge.hanai.core.tool.Tool;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.core.tool.ToolParamDefinition;
import top.yudoge.hanai.core.tool.Type;

import java.util.Arrays;

public class WebSearchTool implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("web_search")
                .description("Search the web for information")
                .params(Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("query")
                                .description("The search query")
                                .type(Type.String)
                                .required(true)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        String query = (String) toolCall.getParam("query");
        
        ToolCallResult result = new ToolCallResult();
        
        // Simulated search results
        StringBuilder mockResult = new StringBuilder();
        mockResult.append("Search results for: ").append(query).append("\n\n");
        mockResult.append("1. Wikipedia - ").append(query).append("\n");
        mockResult.append("   This is a comprehensive article about ").append(query).append(".\n\n");
        mockResult.append("2. News Article - Latest on ").append(query).append("\n");
        mockResult.append("   Recent developments regarding ").append(query).append(" have been reported.\n\n");
        mockResult.append("3. Blog Post - Understanding ").append(query).append("\n");
        mockResult.append("   A detailed guide about ").append(query).append(" and its applications.");
        
        result.setValue(mockResult.toString());
        return result;
    }
}
