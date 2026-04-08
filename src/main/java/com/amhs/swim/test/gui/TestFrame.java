package com.amhs.swim.test.gui;

import com.amhs.swim.test.testcase.*;
import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

/**
 * Main Graphical User Interface for the AMHS/SWIM Gateway Test Tool.
 * Provides access to configured test cases and a real-time execution log.
 */
public class TestFrame extends JFrame {
    private JTextArea logArea;
    private SwimToAmhsTests swimToAmhsTests;

    public TestFrame() {
        super("AMHS/SWIM Gateway Test Tool v1.0");
        swimToAmhsTests = new SwimToAmhsTests();
        
        initComponents();
        
        // Connect GUI log area to the central Logger for real-time reporting
        Logger.setLogListener(msg -> {
            SwingUtilities.invokeLater(() -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        });

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // --- Tab: SWIM to AMHS Injection ---
        JPanel swimToAmhsPanel = new JPanel(new BorderLayout());
        JPanel swimButtons = new JPanel(new GridLayout(0, 2, 5, 5));
        
        addTestButton(swimButtons, "CTSW101 (Basic Conv)", swimToAmhsTests.CTSW101);
        addTestButton(swimButtons, "CTSW102 (Missing Info)", swimToAmhsTests.CTSW102);
        addTestButton(swimButtons, "CTSW103 (Service Level)", swimToAmhsTests.CTSW103);
        addTestButton(swimButtons, "CTSW104 (Pri Mapping)", swimToAmhsTests.CTSW104);
        addTestButton(swimButtons, "CTSW105 (Filing Time)", swimToAmhsTests.CTSW105);
        addTestButton(swimButtons, "CTSW106 (OHI Mapping)", swimToAmhsTests.CTSW106);
        addTestButton(swimButtons, "CTSW107 (Subject Mapping)", swimToAmhsTests.CTSW107);
        addTestButton(swimButtons, "CTSW108 (Known Origin)", swimToAmhsTests.CTSW108);
        addTestButton(swimButtons, "CTSW109 (Unknown Origin)", swimToAmhsTests.CTSW109);
        addTestButton(swimButtons, "CTSW110 (Content-Type)", swimToAmhsTests.CTSW110);
        addTestButton(swimButtons, "CTSW111 (Max Payload)", swimToAmhsTests.CTSW111);
        addTestButton(swimButtons, "CTSW112 (Recip Limit)", swimToAmhsTests.CTSW112);
        addTestButton(swimButtons, "CTSW113 (RN/NRN Req)", swimToAmhsTests.CTSW113);
        addTestButton(swimButtons, "CTSW114 (NDR Forwarding)", swimToAmhsTests.CTSW114);
        addTestButton(swimButtons, "CTSW115 (Encoding/Body)", swimToAmhsTests.CTSW115);
        addTestButton(swimButtons, "CTSW116 (FTBP/GZIP)", swimToAmhsTests.CTSW116);
        
        JScrollPane swimScroll = new JScrollPane(swimButtons);
        swimToAmhsPanel.add(swimScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Injection: SWIM to AMHS (CTSW1xx)", swimToAmhsPanel);
        
        // --- Tab: Configuration Settings ---
        JPanel configPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        configPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Broker Profile Selection
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profilePanel.add(new JLabel("AMQP Broker Profile:"));
        String[] profiles = {"STANDARD", "AZURE_SERVICE_BUS", "IBM_MQ", "RABBITMQ", "SOLACE"};
        JComboBox<String> profileCombo = new JComboBox<>(profiles);
        profileCombo.setSelectedItem(TestConfig.getInstance().getProperty("amqp_broker_profile", "STANDARD"));
        profilePanel.add(profileCombo);
        configPanel.add(profilePanel);

        // Host & Port Configuration
        JPanel hostPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hostPanel.add(new JLabel("SWIM Broker Host:"));
        JTextField hostField = new JTextField(TestConfig.getInstance().getProperty("swim.broker.host", "localhost"), 15);
        hostPanel.add(hostField);
        hostPanel.add(new JLabel("Port:"));
        JTextField portField = new JTextField(TestConfig.getInstance().getProperty("swim.broker.port", "5672"), 5);
        hostPanel.add(portField);
        configPanel.add(hostPanel);

        // Authentication Details
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authPanel.add(new JLabel("Username:"));
        JTextField userField = new JTextField(TestConfig.getInstance().getProperty("swim.broker.user", "default"), 10);
        authPanel.add(userField);
        authPanel.add(new JLabel("Password:"));
        JPasswordField passField = new JPasswordField(TestConfig.getInstance().getProperty("swim.broker.password", "default"), 10);
        authPanel.add(passField);
        authPanel.add(new JLabel("VPN:"));
        JTextField vpnField = new JTextField(TestConfig.getInstance().getProperty("swim.broker.vpn", "default"), 10);
        authPanel.add(vpnField);
        configPanel.add(authPanel);

        // Target Endpoints
        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        targetPanel.add(new JLabel("Target AMQP Topic:"));
        JTextField topicField = new JTextField(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), 15);
        targetPanel.add(topicField);
        targetPanel.add(new JLabel("Test Recipient (AMHS O/R):"));
        JTextField recipientField = new JTextField(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"), 15);
        targetPanel.add(recipientField);
        configPanel.add(targetPanel);

        JButton checkConnBtn = new JButton("Check Connection");
        checkConnBtn.addActionListener(e -> {
            new Thread(() -> {
                log("Testing connection to " + hostField.getText() + ":" + portField.getText() + "...");
                swimToAmhsTests.getSwimDriver().testConnection();
            }).start();
        });
        configPanel.add(checkConnBtn);

        JButton saveBtn = new JButton("Save & Apply Configuration");
        saveBtn.addActionListener(e -> {
            TestConfig config = TestConfig.getInstance();
            config.setProperty("swim.broker.host", hostField.getText());
            config.setProperty("swim.broker.port", portField.getText());
            config.setProperty("swim.broker.user", userField.getText());
            config.setProperty("swim.broker.password", new String(passField.getPassword()));
            config.setProperty("swim.broker.vpn", vpnField.getText());
            config.setProperty("gateway.default_topic", topicField.getText());
            config.setProperty("gateway.test_recipient", recipientField.getText());
            config.setProperty("amqp_broker_profile", (String) profileCombo.getSelectedItem());
            config.saveConfig();
            
            swimToAmhsTests = new SwimToAmhsTests();
            log("Configuration saved. Test cases re-initialized.");
        });
        configPanel.add(saveBtn);

        tabbedPane.addTab("Settings", new JScrollPane(configPanel));
        
        // --- Real-time Execution Log Display ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        logScroll.setPreferredSize(new Dimension(800, 200));
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(logScroll, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private void addTestButton(JPanel panel, String label, BaseTestCase testCase) {
        JButton btn = new JButton(label);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runTest(testCase);
            }
        });
        panel.add(btn);
    }

    private void runTest(BaseTestCase testCase) {
        List<BaseTestCase.TestParameter> params = testCase.getRequiredParameters();
        if (params != null && !params.isEmpty()) {
            TestParamDialog dialog = new TestParamDialog(this, "Execute " + testCase.getTestCaseId(), params, userInputs -> {
                executeTestThread(testCase, userInputs);
            });
            dialog.setVisible(true);
        } else {
            executeTestThread(testCase, null);
        }
    }

    private void executeTestThread(BaseTestCase testCase, Map<String, String> userInputs) {
        // Execute the test in a separate thread to keep the GUI responsive
        new Thread(() -> {
            try {
                log("Starting injection: " + testCase.getTestCaseId());
                boolean result;
                if (userInputs != null) {
                    result = testCase.execute(userInputs);
                } else {
                    result = testCase.execute();
                }
                log("Injection Status: " + (result ? "SUCCESSFUL" : "FAILED"));
            } catch (Exception ex) {
                log("Exception during injection: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        Logger.log("GUI", message);
    }
}