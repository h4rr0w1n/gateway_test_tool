package com.amhs.swim.test;

import com.amhs.swim.test.gui.TestFrame;
import com.amhs.swim.test.util.Logger;

import javax.swing.*;

/**
 * Điểm vào chính của ứng dụng.
 */
public class Main {
    public static void main(String[] args) {
        Logger.log("INFO", "Khởi động AMHS/SWIM Gateway Test Tool...");
        
        // Thiết lập giao diện Swing
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            TestFrame frame = new TestFrame();
            frame.setVisible(true);
        });
    }
}