package com.amhs.swim.test.gui;

import com.amhs.swim.test.testcase.BaseTestCase.TestParameter;
import com.amhs.swim.test.config.TestConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.Base64;

public class TestParamDialog extends JDialog {

    private Map<String, String> resultValues = null;
    private final Map<String, JComponent> inputFields = new HashMap<>();
    private final Map<String, JButton> fileButtons = new HashMap<>();
    private final Map<String, JButton> clearButtons = new HashMap<>();
    private final Map<String, JButton> revertButtons = new HashMap<>();
    private final String testCaseId;
    private JTextArea guidelineArea;

    public TestParamDialog(JFrame parent, String title, List<TestParameter> parameters, Consumer<Map<String, String>> onExecute) {
        super(parent, title, true);
        
        // Extract test case ID from title (e.g., "Execute CTSW101" -> "CTSW101")
        this.testCaseId = title.replace("Execute ", "").trim();
        
        setLayout(new BorderLayout());
        
        // Create main content panel with guidelines and form
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Guidelines Panel (Top)
        JPanel guidelinePanel = new JPanel(new BorderLayout());
        guidelinePanel.setBorder(BorderFactory.createTitledBorder("Test Guideline"));
        guidelineArea = new JTextArea(8, 50);
        guidelineArea.setEditable(false);
        guidelineArea.setLineWrap(true);
        guidelineArea.setWrapStyleWord(true);
        guidelineArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JScrollPane guidelineScroll = new JScrollPane(guidelineArea);
        guidelinePanel.add(guidelineScroll, BorderLayout.CENTER);
        loadGuideline();
        mainPanel.add(guidelinePanel, BorderLayout.NORTH);
        
        // Form Panel (Center)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        int row = 0;
        for (TestParameter param : parameters) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.0;
            formPanel.add(new JLabel(param.getLabel()), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            
            // Get configured or default value
            String defaultValue = TestConfig.getInstance().getCasePayload(testCaseId, param.getKey());
            if (defaultValue == null || defaultValue.isEmpty()) {
                defaultValue = param.getDefaultValue();
            }
            
            JPanel fieldPanel = new JPanel(new BorderLayout(5, 0));
            JComponent field;
            if (param.isLargeText()) {
                JTextArea textArea = new JTextArea(defaultValue, 4, 30);
                textArea.setLineWrap(true);
                JScrollPane scrollPane = new JScrollPane(textArea);
                fieldPanel.add(scrollPane, BorderLayout.CENTER);
                field = textArea;
            } else {
                JTextField textField = new JTextField(defaultValue, 25);
                fieldPanel.add(textField, BorderLayout.CENTER);
                field = textField;
            }
            
            // Button panel for this field
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            
            // Load File button
            JButton btnLoadFile = new JButton("📁 Load File");
            btnLoadFile.setToolTipText("Load content from a file");
            btnLoadFile.addActionListener(e -> loadFromFile(field, param.getKey()));
            fieldPanel.add(buttonPanel, BorderLayout.EAST);
            buttonPanel.add(btnLoadFile);
            fileButtons.put(param.getKey(), btnLoadFile);
            
            // Clear button
            JButton btnClear = new JButton("✕ Clear");
            btnClear.setToolTipText("Clear this field");
            btnClear.addActionListener(e -> {
                if (field instanceof JTextField) {
                    ((JTextField) field).setText("");
                } else if (field instanceof JTextArea) {
                    ((JTextArea) field).setText("");
                }
            });
            buttonPanel.add(btnClear);
            clearButtons.put(param.getKey(), btnClear);
            
            // Revert to Default button
            JButton btnRevert = new JButton("↶ Default");
            btnRevert.setToolTipText("Revert to default value");
            btnRevert.addActionListener(e -> {
                String defaultVal = TestConfig.getInstance().getDefaultCasePayload(testCaseId, param.getKey());
                if (defaultVal == null) {
                    defaultVal = param.getDefaultValue();
                }
                if (field instanceof JTextField) {
                    ((JTextField) field).setText(defaultVal);
                } else if (field instanceof JTextArea) {
                    ((JTextArea) field).setText(defaultVal);
                }
            });
            buttonPanel.add(btnRevert);
            revertButtons.put(param.getKey(), btnRevert);
            
            formPanel.add(fieldPanel, gbc);
            inputFields.put(param.getKey(), field);
            row++;
        }

        JScrollPane mainScroll = new JScrollPane(formPanel);
        mainPanel.add(mainScroll, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);

        // Button Panel (Bottom)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Revert All button
        JButton btnRevertAll = new JButton("Revert All to Default");
        btnRevertAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Revert all fields to their default values?", 
                "Confirm Revert", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (TestParameter param : parameters) {
                    JComponent field = inputFields.get(param.getKey());
                    String defaultVal = TestConfig.getInstance().getDefaultCasePayload(testCaseId, param.getKey());
                    if (defaultVal == null) {
                        defaultVal = param.getDefaultValue();
                    }
                    if (field instanceof JTextField) {
                        ((JTextField) field).setText(defaultVal);
                    } else if (field instanceof JTextArea) {
                        ((JTextArea) field).setText(defaultVal);
                    }
                }
            }
        });
        buttonPanel.add(btnRevertAll);
        
        JButton btnExecute = new JButton("Execute");
        JButton btnCancel = new JButton("Cancel");

        btnExecute.addActionListener(e -> {
            resultValues = new HashMap<>();
            for (Map.Entry<String, JComponent> entry : inputFields.entrySet()) {
                String val = "";
                if (entry.getValue() instanceof JTextField) {
                    val = ((JTextField) entry.getValue()).getText();
                } else if (entry.getValue() instanceof JTextArea) {
                    val = ((JTextArea) entry.getValue()).getText();
                }
                // Save the configured value
                TestConfig.getInstance().setCasePayload(testCaseId, entry.getKey(), val);
                resultValues.put(entry.getKey(), val);
            }
            dispose();
            onExecute.accept(resultValues);
        });

        btnCancel.addActionListener(e -> {
            resultValues = null;
            dispose();
        });

        buttonPanel.add(btnExecute);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Load guideline text from cases.json for the current test case.
     */
    private void loadGuideline() {
        try {
            String guidelineContent = "";
            File casesFile = new File("cases.json");
            if (casesFile.exists()) {
                String jsonContent = new String(Files.readAllBytes(casesFile.toPath()));
                // Simple JSON parsing for guideline
                String searchKey = "\"" + testCaseId + "\":";
                int keyIndex = jsonContent.indexOf(searchKey);
                if (keyIndex >= 0) {
                    int startQuote = jsonContent.indexOf("\"", keyIndex + searchKey.length());
                    if (startQuote >= 0) {
                        int endQuote = findMatchingQuote(jsonContent, startQuote + 1);
                        if (endQuote > startQuote) {
                            guidelineContent = jsonContent.substring(startQuote + 1, endQuote);
                            // Unescape JSON string
                            guidelineContent = guidelineContent.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\u2022", "•")
                                .replace("\\u201c", "\"")
                                .replace("\\u201d", "\"")
                                .replace("\\u2013", "-");
                        }
                    }
                }
            }
            guidelineArea.setText(guidelineContent.isEmpty() ? 
                "No guideline available for " + testCaseId : guidelineContent);
        } catch (Exception ex) {
            guidelineArea.setText("Error loading guideline: " + ex.getMessage());
        }
    }
    
    /**
     * Find the matching closing quote in a JSON string.
     */
    private int findMatchingQuote(String json, int startPos) {
        boolean escaped = false;
        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Load content from a file into the specified field.
     */
    private void loadFromFile(JComponent field, String paramKey) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select file to load");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                
                // Check if file is binary
                if (isBinary(fileContent)) {
                    // Encode as Base64 for binary files
                    String encoded = Base64.getEncoder().encodeToString(fileContent);
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Binary file detected. Load as Base64 encoded?\n\n" +
                        "File: " + selectedFile.getName() + "\n" +
                        "Size: " + fileContent.length + " bytes\n" +
                        "Encoded length: " + encoded.length() + " chars",
                        "Binary File Detected", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        setContent(field, "[BASE64:" + selectedFile.getName() + "] " + encoded);
                    }
                } else {
                    // Text file - read as string
                    String content = new String(fileContent);
                    setContent(field, content);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error loading file: " + ex.getMessage(),
                    "File Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Check if content appears to be binary.
     */
    private boolean isBinary(byte[] content) {
        if (content.length == 0) return false;
        int nonPrintable = 0;
        int checkLength = Math.min(content.length, 1024);
        for (int i = 0; i < checkLength; i++) {
            byte b = content[i];
            // Check for common binary indicators
            if (b == 0) return true; // Null byte
            if (b < 0x20 && b != 0x09 && b != 0x0A && b != 0x0D) {
                nonPrintable++;
            }
        }
        // If more than 30% non-printable, consider it binary
        return (double) nonPrintable / checkLength > 0.3;
    }
    
    /**
     * Set content in the appropriate field type.
     */
    private void setContent(JComponent field, String content) {
        if (field instanceof JTextField) {
            // For single-line fields, truncate or show summary
            if (content.length() > 100) {
                ((JTextField) field).setText(content.substring(0, 100) + "...");
            } else {
                ((JTextField) field).setText(content);
            }
        } else if (field instanceof JTextArea) {
            ((JTextArea) field).setText(content);
        }
    }
}
