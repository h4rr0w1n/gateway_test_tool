package com.amhs.swim.test.gui;

import com.amhs.swim.test.testcase.*;
import com.amhs.swim.test.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Giao diện chính của công cụ kiểm thử.
 * Hiển thị danh sách các test case và nút thực thi.
 */
public class TestFrame extends JFrame {
    private JTextArea logArea;
    private AmhsToSwimTests amhsToSwimTests;
    private SwimToAmhsTests swimToAmhsTests;

    public TestFrame() {
        super("AMHS/SWIM Gateway Test Tool v1.0");
        amhsToSwimTests = new AmhsToSwimTests();
        swimToAmhsTests = new SwimToAmhsTests();
        
        initComponents();
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Panel chứa các nút test
        JPanel buttonPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Test Cases"));
        
        // Thêm nút cho các test case AMHS -> SWIM
        addTestButton(buttonPanel, "CTSW001 (IPM to AMQP)", () -> runTest(amhsToSwimTests.CTSW001));
        addTestButton(buttonPanel, "CTSW004 (NDR Syntax Error)", () -> runTest(amhsToSwimTests.CTSW004));
        addTestButton(buttonPanel, "CTSW006 (Max Size)", () -> runTest(amhsToSwimTests.CTSW006));
        // ... Thêm các nút khác tương ứng với danh sách test case
        
        // Thêm nút cho các test case SWIM -> AMHS
        addTestButton(buttonPanel, "CTSW101 (AMQP to AMHS)", () -> runTest(swimToAmhsTests.CTSW101));
        addTestButton(buttonPanel, "CTSW110 (Invalid Content-Type)", () -> runTest(swimToAmhsTests.CTSW110));
        addTestButton(buttonPanel, "CTSW112 (Max Recipients)", () -> runTest(swimToAmhsTests.CTSW112));
        
        // Area hiển thị log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(mainPanel);
    }

    private void addTestButton(JPanel panel, String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        panel.add(btn);
    }

    private void runTest(BaseTestCase testCase) {
        // Chạy test trong thread riêng để không treo GUI
        new Thread(() -> {
            try {
                log("Đang thực thi: " + testCase.getTestCaseId());
                boolean result = testCase.execute();
                log("Kết quả: " + (result ? "PASS" : "FAIL"));
            } catch (Exception ex) {
                log("Ngoại lệ: " + ex.getMessage());
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