package com.amhs.swim.test.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích ghi log cho công cụ kiểm thử.
 * Hỗ trợ ghi log ra console và file để phục vụ tra vết (traffic logging).
 */
public class Logger {
    private static final String LOG_FILE = "test_results.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Ghi thông báo log.
     * @param level Mức độ log (INFO, ERROR, SUCCESS).
     * @param message Nội dung log.
     */
    public static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);
        System.out.print(logEntry);
        writeToFile(logEntry);
    }

    /**
     * Ghi log traffic cho yêu cầu kiểm thử (theo EUR Doc 047).
     * @param direction Hướng message (AMHS->SWIM hoặc SWIM->AMHS).
     * @param content Nội dung raw của message.
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