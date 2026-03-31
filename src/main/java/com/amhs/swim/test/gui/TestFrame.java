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
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // --- Tab: AMHS to SWIM ---
        JPanel amhsToSwimPanel = new JPanel(new BorderLayout());
        JPanel amhsButtons = new JPanel(new GridLayout(0, 2, 5, 5));
        
        addTestButton(amhsButtons, "CTSW001 (IPM to AMQP)", () -> runTest(amhsToSwimTests.CTSW001));
        addTestButton(amhsButtons, "CTSW002 (Multi Recipients)", () -> runTest(amhsToSwimTests.CTSW002));
        addTestButton(amhsButtons, "CTSW003 (DR Success)", () -> runTest(amhsToSwimTests.CTSW003));
        addTestButton(amhsButtons, "CTSW004 (NDR Syntax Error)", () -> runTest(amhsToSwimTests.CTSW004));
        addTestButton(amhsButtons, "CTSW005 (DR Exceed)", () -> runTest(amhsToSwimTests.CTSW005));
        addTestButton(amhsButtons, "CTSW006 (Max Size)", () -> runTest(amhsToSwimTests.CTSW006));
        addTestButton(amhsButtons, "CTSW007 (Multi Body Part)", () -> runTest(amhsToSwimTests.CTSW007));
        addTestButton(amhsButtons, "CTSW008 (Unsupported Type)", () -> runTest(amhsToSwimTests.CTSW008));
        addTestButton(amhsButtons, "CTSW009 (Optional Fields)", () -> runTest(amhsToSwimTests.CTSW009));
        addTestButton(amhsButtons, "CTSW010 (Excessive Recip)", () -> runTest(amhsToSwimTests.CTSW010));
        addTestButton(amhsButtons, "CTSW011 (Probe to AMQP)", () -> runTest(amhsToSwimTests.CTSW011));
        addTestButton(amhsButtons, "CTSW012 (Probe Resp OK)", () -> runTest(amhsToSwimTests.CTSW012));
        addTestButton(amhsButtons, "CTSW013 (Probe Resp Fail)", () -> runTest(amhsToSwimTests.CTSW013));
        addTestButton(amhsButtons, "CTSW014 (RN to AMQP)", () -> runTest(amhsToSwimTests.CTSW014));
        addTestButton(amhsButtons, "CTSW015 (RN Request)", () -> runTest(amhsToSwimTests.CTSW015));
        addTestButton(amhsButtons, "CTSW016 (Current EIT)", () -> runTest(amhsToSwimTests.CTSW016));
        addTestButton(amhsButtons, "CTSW017 (IA5 Body Part)", () -> runTest(amhsToSwimTests.CTSW017));
        addTestButton(amhsButtons, "CTSW018 (ISO 646)", () -> runTest(amhsToSwimTests.CTSW018));
        addTestButton(amhsButtons, "CTSW019 (Non-ISO 646)", () -> runTest(amhsToSwimTests.CTSW019));
        addTestButton(amhsButtons, "CTSW020 (SEC Envelope)", () -> runTest(amhsToSwimTests.CTSW020));
        
        JScrollPane amhsScroll = new JScrollPane(amhsButtons);
        amhsToSwimPanel.add(amhsScroll, BorderLayout.CENTER);
        tabbedPane.addTab("AMHS to SWIM (20)", amhsToSwimPanel);
        
        // --- Tab: SWIM to AMHS ---
        JPanel swimToAmhsPanel = new JPanel(new BorderLayout());
        JPanel swimButtons = new JPanel(new GridLayout(0, 2, 5, 5));
        
        addTestButton(swimButtons, "CTSW101 (AMQP to AMHS)", () -> runTest(swimToAmhsTests.CTSW101));
        addTestButton(swimButtons, "CTSW102 (Missing Info)", () -> runTest(swimToAmhsTests.CTSW102));
        addTestButton(swimButtons, "CTSW103 (Explicit Pri)", () -> runTest(swimToAmhsTests.CTSW103));
        addTestButton(swimButtons, "CTSW104 (Multi Recip)", () -> runTest(swimToAmhsTests.CTSW104));
        addTestButton(swimButtons, "CTSW105 (Filing Time)", () -> runTest(swimToAmhsTests.CTSW105));
        addTestButton(swimButtons, "CTSW106 (Message ID)", () -> runTest(swimToAmhsTests.CTSW106));
        addTestButton(swimButtons, "CTSW107 (Originator)", () -> runTest(swimToAmhsTests.CTSW107));
        addTestButton(swimButtons, "CTSW108 (Subject)", () -> runTest(swimToAmhsTests.CTSW108));
        addTestButton(swimButtons, "CTSW109 (Optional Prop)", () -> runTest(swimToAmhsTests.CTSW109));
        addTestButton(swimButtons, "CTSW110 (Invalid Type)", () -> runTest(swimToAmhsTests.CTSW110));
        addTestButton(swimButtons, "CTSW111 (Max Payload)", () -> runTest(swimToAmhsTests.CTSW111));
        addTestButton(swimButtons, "CTSW112 (Max Recipients)", () -> runTest(swimToAmhsTests.CTSW112));
        addTestButton(swimButtons, "CTSW113 (DR to AMHS)", () -> runTest(swimToAmhsTests.CTSW113));
        addTestButton(swimButtons, "CTSW114 (NDR to AMHS)", () -> runTest(swimToAmhsTests.CTSW114));
        addTestButton(swimButtons, "CTSW115 (Body Part Typ)", () -> runTest(swimToAmhsTests.CTSW115));
        addTestButton(swimButtons, "CTSW116 (FTBP Binary)", () -> runTest(swimToAmhsTests.CTSW116));
        
        JScrollPane swimScroll = new JScrollPane(swimButtons);
        swimToAmhsPanel.add(swimScroll, BorderLayout.CENTER);
        tabbedPane.addTab("SWIM to AMHS (16)", swimToAmhsPanel);
        
        // --- Execution Log ---
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