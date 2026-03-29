package top.yudoge.hanai.example;

import top.yudoge.hanai.core.agent.Agent;
import top.yudoge.hanai.core.agent.AgentLoopEventListener;
import top.yudoge.hanai.core.agent.AgentUsage;
import top.yudoge.hanai.core.agent.SimpleChatModelAgent;
import top.yudoge.hanai.core.chat.Message;
import top.yudoge.hanai.core.memory.FixedCountMemory;
import top.yudoge.hanai.core.memory.Memory;
import top.yudoge.hanai.core.tool.ToolCall;
import top.yudoge.hanai.core.tool.ToolCallResult;
import top.yudoge.hanai.core.tool.ToolDefinition;
import top.yudoge.hanai.example.tools.DateTimeTool;
import top.yudoge.hanai.example.tools.WebSearchTool;
import top.yudoge.hanai.example.tools.WeatherTool;
import top.yudoge.hanai.openai.OpenAIChatModel;
import top.yudoge.hanai.utils.EnvUtil;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SimpleAgentGUI {

    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private Memory memory;
    private OpenAIChatModel chatModel;
    private StringBuilder thinkingBuffer;
    private Agent agent;
    private StyledDocument doc;
    private int lastAgentStartPosition = -1;

    // Style constants
    private static final Color USER_COLOR = new Color(0, 100, 200);
    private static final Color AGENT_COLOR = new Color(0, 128, 0);
    private static final Color TOOL_CALL_COLOR = new Color(255, 140, 0);
    private static final Color TOOL_RESULT_COLOR = new Color(128, 0, 128);
    private static final Color ERROR_COLOR = new Color(200, 0, 0);
    private static final Color SYSTEM_COLOR = new Color(100, 100, 100);

    public SimpleAgentGUI() {
        createGUI();
        initializeComponents();
    }

    private void createGUI() {
        JFrame frame = new JFrame("Simple Agent Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        // Chat display area with styled text
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatPane.setMargin(new Insets(10, 10, 10, 10));
        doc = chatPane.getStyledDocument();
        
        // Initialize styles
        initStyles();
        
        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Input panel
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendButton.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Legend panel
        JPanel legendPanel = createLegendPanel();

        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(legendPanel, BorderLayout.NORTH);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void initStyles() {
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        Style userStyle = doc.addStyle("user", defaultStyle);
        StyleConstants.setForeground(userStyle, USER_COLOR);
        StyleConstants.setBold(userStyle, true);
        
        Style agentStyle = doc.addStyle("agent", defaultStyle);
        StyleConstants.setForeground(agentStyle, AGENT_COLOR);
        StyleConstants.setBold(agentStyle, true);
        
        Style toolCallStyle = doc.addStyle("toolCall", defaultStyle);
        StyleConstants.setForeground(toolCallStyle, TOOL_CALL_COLOR);
        StyleConstants.setBold(toolCallStyle, true);
        
        Style toolResultStyle = doc.addStyle("toolResult", defaultStyle);
        StyleConstants.setForeground(toolResultStyle, TOOL_RESULT_COLOR);
        
        Style errorStyle = doc.addStyle("error", defaultStyle);
        StyleConstants.setForeground(errorStyle, ERROR_COLOR);
        StyleConstants.setBold(errorStyle, true);
        
        Style systemStyle = doc.addStyle("system", defaultStyle);
        StyleConstants.setForeground(systemStyle, SYSTEM_COLOR);
        StyleConstants.setItalic(systemStyle, true);
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.setBackground(Color.WHITE);

        panel.add(createLegendItem("You", USER_COLOR));
        panel.add(createLegendItem("Agent Thinking", AGENT_COLOR));
        panel.add(createLegendItem("Tool Calling", TOOL_CALL_COLOR));
        panel.add(createLegendItem("Tool Result", TOOL_RESULT_COLOR));
        panel.add(createLegendItem("Error", ERROR_COLOR));

        return panel;
    }

    private JPanel createLegendItem(String text, Color color) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(dot);
        panel.add(label);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private void initializeComponents() {
        String baseUrl = EnvUtil.get("OPENAI_BASE_URL");
        String modelName = EnvUtil.get("OPENAI_MODEL_NAME");
        String apiKey = EnvUtil.get("OPENAI_API_KEY");

        chatModel = new OpenAIChatModel(baseUrl, modelName, apiKey);
        memory = new FixedCountMemory(1000);
        SimpleChatModelAgent agentImpl = new SimpleChatModelAgent(memory);
        agentImpl.registerChatModel(chatModel);
        agentImpl.registerTool(new WebSearchTool());
        agentImpl.registerTool(new WeatherTool());
        agentImpl.registerTool(new DateTimeTool());
        this.agent = agentImpl;

        appendToChat("System", "Agent initialized with tools: web_search, get_weather, get_current_datetime", "system");
        appendToChat("System", "Ready to chat! Type your message below.", "system");
    }

    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) return;

        inputField.setText("");
        appendToChat("You", userInput, "user");

        sendButton.setEnabled(false);
        inputField.setEnabled(false);

        new Thread(() -> {
            thinkingBuffer = new StringBuilder();
            
            agent.start(Message.user(userInput), new AgentLoopEventListener() {
                @Override
                public void onThinkingStart(String turnId) {
                    System.out.println("==================think start["+turnId+"]===================");
                    SwingUtilities.invokeLater(() -> {
                        try {
                            lastAgentStartPosition = doc.getLength();
                            doc.insertString(doc.getLength(), "[Agent]: ", doc.getStyle("agent"));
                            doc.insertString(doc.getLength(), "Thinking...\n\n", doc.getStyle("agent"));
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onThinking(String turnId, String content) {
                    thinkingBuffer.append(content);
                    System.out.print(content);
                    SwingUtilities.invokeLater(() -> {
                        updateLastAgentMessage(thinkingBuffer.toString());
                    });
                }

                @Override
                public void onThinkingEnd(String turnId, Long timeConsumed) {
                    System.out.println("==================think end["+turnId+"]===================");
                }

                @Override
                public void onToolCallingStart(String turnId, ToolDefinition definition, ToolCall toolCall) {
                    System.out.println("==================tool call start["+turnId+"]["+definition.getIdentifier()+"]===================");
                    SwingUtilities.invokeLater(() -> {
                        try {
                            doc.insertString(doc.getLength(), "[Tool]: ", doc.getStyle("toolCall"));
                            doc.insertString(doc.getLength(), "Calling: " + definition.getIdentifier() + "\n", doc.getStyle("toolCall"));
                            if (toolCall.getParams() != null && !toolCall.getParams().getValues().isEmpty()) {
                                doc.insertString(doc.getLength(), "        Arguments: " + toolCall.getParams() + "\n", doc.getStyle("toolCall"));
                            }
                            doc.insertString(doc.getLength(), "\n", null);
                            chatPane.setCaretPosition(doc.getLength());
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onToolCallingEnd(String turnId, ToolCall toolCall, ToolCallResult toolCallResult, Long timeConsumed) {
                    System.out.println("==================tool call end["+turnId+"]===================");
                    SwingUtilities.invokeLater(() -> {
                        if (toolCallResult != null && toolCallResult.getValue() != null) {
                            try {
                                doc.insertString(doc.getLength(), "[Result]: ", doc.getStyle("toolResult"));
                                String result = toolCallResult.getValue().toString();
                                String[] lines = result.split("\n");
                                for (String line : lines) {
                                    doc.insertString(doc.getLength(), "        " + line + "\n", doc.getStyle("toolResult"));
                                }
                                doc.insertString(doc.getLength(), "\n", null);
                                chatPane.setCaretPosition(doc.getLength());
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onError(String reason, Exception e, Long timeConsumed) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Error", reason + (e != null ? ": " + e.getMessage() : ""), "error");
                        enableInput();
                    });
                }

                @Override
                public void onComplete(Long timeConsumed) {
                    SwingUtilities.invokeLater(() -> {
                        enableInput();
                    });
                }

                @Override
                public void onFinish(String turnId, AgentUsage usage, Long timeConsumed) {
                    System.out.println("==================turn finish["+turnId+"]===================");
                    SwingUtilities.invokeLater(() -> {
                        enableInput();
                    });
                }
            });
        }).start();
    }

    private void appendToChat(String sender, String message, String styleName) {
        try {
            doc.insertString(doc.getLength(), "[" + sender + "]: ", doc.getStyle(styleName));
            doc.insertString(doc.getLength(), message + "\n\n", null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateLastAgentMessage(String content) {
        try {
            if (lastAgentStartPosition >= 0) {
                doc.remove(lastAgentStartPosition, doc.getLength() - lastAgentStartPosition);
                doc.insertString(doc.getLength(), "[Agent]: ", doc.getStyle("agent"));
                doc.insertString(doc.getLength(), content + "\n\n", null);
                chatPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void enableInput() {
        sendButton.setEnabled(true);
        inputField.setEnabled(true);
        inputField.requestFocus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleAgentGUI::new);
    }
}
