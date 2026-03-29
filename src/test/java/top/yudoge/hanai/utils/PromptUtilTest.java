package top.yudoge.hanai.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for PromptUtil.loadClassPath method.
 */
public class PromptUtilTest {

    @Test
    public void testLoadClassPath_SimpleAgent() {
        // Load the simple_agent.md file from classpath
        String content = PromptUtil.loadClassPath("prompt/simple_agent.md", Map.of());
        
        // Verify content is loaded
        assertNotNull(content, "Content should not be null");
        assertFalse(content.isEmpty(), "Content should not be empty");
        
        // Verify it contains expected sections from simple_agent.md
        assertTrue(content.contains("You are a general Agent designed to execute user task automatically."), 
                "Should contain agent description");
        assertTrue(content.contains("When you got a user task, you must follow the loop below:"), 
                "Should contain loop instruction");
        assertTrue(content.contains("# Tools"), 
                "Should contain tools section");
        assertTrue(content.contains("# Limitation"), 
                "Should contain limitation section");
        assertTrue(content.contains("## Other Response"), 
                "Should contain other response section");
    }

    @Test
    public void testLoadClassPath_WithVariables() {
        // Test variable replacement
        Map<String, String> variables = new HashMap<>();
        variables.put("ToolJson", "[{\"name\": \"test_tool\"}]");
        
        String content = PromptUtil.loadClassPath("prompt/simple_agent.md", variables);
        
        // Verify variables were replaced
        assertTrue(content.contains("[{\"name\": \"test_tool\"}]"), 
                "Should contain replaced ToolJson");
        
        // Verify original placeholders are gone
        assertFalse(content.contains("${ToolJson}"), 
                "Should not contain unreplaced ToolJson placeholder");
    }

    @Test
    public void testLoadClassPath_ResourceNotFound() {
        // Test that exception is thrown for non-existent resource
        assertThrows(IllegalArgumentException.class, () -> 
                PromptUtil.loadClassPath("non/existent/file.md", Map.of()),
                "Should throw IllegalArgumentException for non-existent resource");
    }

    @Test
    public void testLoadClassPath_NullInputs() {
        // Test null classPath
        assertThrows(NullPointerException.class, () -> 
                PromptUtil.loadClassPath(null, Map.of()),
                "Should throw NullPointerException for null classPath");
        
        // Test null variables
        assertThrows(NullPointerException.class, () -> 
                PromptUtil.loadClassPath("prompt/simple_agent.md", null),
                "Should throw NullPointerException for null variables");
    }
}
