package top.yudoge.hanaai.example.tools;

import top.yudoge.hanaai.core.tool.Tool;
import top.yudoge.hanaai.core.tool.ToolCall;
import top.yudoge.hanaai.core.tool.ToolCallResult;
import top.yudoge.hanaai.core.tool.ToolDefinition;
import top.yudoge.hanaai.core.tool.ToolParamDefinition;
import top.yudoge.hanaai.core.tool.Type;

import java.util.Arrays;
import java.util.Random;

public class WeatherTool implements Tool {

    private final Random random = new Random();

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder()
                .identifier("get_weather")
                .description("Get the current weather for a location")
                .params(Arrays.asList(
                        ToolParamDefinition.builder()
                                .name("location")
                                .description("The city or location name")
                                .type(Type.String)
                                .required(true)
                                .build()
                ))
                .build();
    }

    @Override
    public ToolCallResult invoke(ToolCall toolCall) {
        String location = (String) toolCall.getParam("location");
        
        ToolCallResult result = new ToolCallResult();
        
        // Simulated weather data
        int temperature = random.nextInt(35) - 5; // -5 to 30 degrees
        int humidity = random.nextInt(60) + 30; // 30% to 90%
        String[] conditions = {"Sunny", "Cloudy", "Partly Cloudy", "Rainy", "Clear"};
        String condition = conditions[random.nextInt(conditions.length)];
        
        StringBuilder weatherReport = new StringBuilder();
        weatherReport.append("Weather for ").append(location).append(":\n");
        weatherReport.append("Condition: ").append(condition).append("\n");
        weatherReport.append("Temperature: ").append(temperature).append("°C\n");
        weatherReport.append("Humidity: ").append(humidity).append("%\n");
        weatherReport.append("Wind: ").append(random.nextInt(30) + 5).append(" km/h");
        
        result.setValue(weatherReport.toString());
        return result;
    }
}
