package top.yudoge.hanai.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Utility for loading and processing prompt templates.
 */
public class PromptUtil {


    public static String loadClassPath(String classPath) {
        return loadClassPath(classPath, Collections.emptyMap());
    }

    /**
     * Load prompt from classpath.
     *
     * @param classPath filePath. No prefix `/`
     * @param variables ANY ${variableName} in prompt will be replaced by variables
     * @return
     */
    public static String loadClassPath(String classPath, Map<String, String> variables) {
        Objects.requireNonNull(classPath, "classPath must not be null");
        Objects.requireNonNull(variables, "variables must not be null");

        InputStream inputStream = PromptUtil.class.getClassLoader().getResourceAsStream(classPath);
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + classPath);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + classPath, e);
        }

        String result = content.toString();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }
}
