package com.amhs.swim.test.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Lớp quản lý cấu hình cho công cụ kiểm thử.
 * Đọc các tham số kết nối từ file properties.
 */
public class TestConfig {
    private static TestConfig instance;
    private Properties props;

    private TestConfig() {
        props = new Properties();
        loadConfig();
    }

    public static synchronized TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/test.properties")) {
            if (input == null) {
                System.out.println("Không tìm thấy file cấu hình, sử dụng giá trị mặc định.");
                setDefaults();
                return;
            }
            props.load(input);
        } catch (Exception ex) {
            ex.printStackTrace();
            setDefaults();
        }
    }

    private void setDefaults() {
        // Cấu hình mặc định cho môi trường test sandbox
        props.setProperty("amhs.mta.host", "localhost");
        props.setProperty("amhs.mta.port", "10000");
        props.setProperty("swim.broker.host", "tcp://localhost:55555");
        props.setProperty("swim.broker.vpn", "default");
        props.setProperty("directory.host", "ldap://localhost:389");
        props.setProperty("gateway.max recipients", "512");
        props.setProperty("gateway.max size", "1000000");
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}