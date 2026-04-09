package com.amhs.swim.test.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logging utility for the test tool.
 * Supports console output and file logging for traffic tracing.
 */
public class Logger {
    public static final String LOG_FILE = "test_results.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static LogListener listener;

    public interface LogListener {
        void onLog(String message);
    }

    public static void setLogListener(LogListener l) {
        listener = l;
    }

    /**
     * Ghi thông báo log.
     * @param level Mức độ log (INFO, ERROR, SUCCESS, IMPORTANT).
     * @param message Nội dung log.
     */
    public static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        System.out.println(logEntry);
        writeToFile(logEntry + "\n");
        
        if (listener != null) {
            listener.onLog(logEntry);
        }
    }

    /**
     * Records a verification summary for a specific test case.
     * @param caseId The ID of the test case (e.g., CTSW101).
     * @param details The summary details to be displayed.
     */
    public static void logVerification(String caseId, String details) {
        log("SUCCESS", "\n" + "=".repeat(60) + "\n" + 
            " [" + caseId + "] VERIFICATION SUMMARY\n" + 
            " ".repeat(2) + details.replace("\n", "\n  ") + "\n" +
            "=".repeat(60) + "\n");
    }

    /**
     * Records a deep trace of AMQP 1.0 properties for compliance verification.
     * @param props Map of AMQP properties and application properties.
     */
    public static void logAMQPDeepTrace(java.util.Map<String, Object> props) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  [DEEP INSPECTION] AMQP 1.0 Message Metadata:\n");
        sb.append("  ").append("-".repeat(50)).append("\n");
        props.forEach((k, v) -> {
            if (v != null) {
                sb.append(String.format("  %-25s : %s\n", k, v));
            }
        });
        sb.append("  ").append("-".repeat(50)).append("\n");
        log("TRACE", sb.toString());
    }

    /**
     * Records traffic log for a test request (as per EUR Doc 047).
     * @param direction Direction (AMHS->SWIM or SWIM->AMHS).
     * @param content Raw message content.
     */
    public static void logTraffic(String direction, String content) {
        log("TRAFFIC", "Direction: " + direction);
        log("TRAFFIC", "Content: " + content);
    }

    private static synchronized void writeToFile(String text) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(text);
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi log file: " + e.getMessage());
        }
    }
}