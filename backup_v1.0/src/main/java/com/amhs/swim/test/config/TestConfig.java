package com.amhs.swim.test.config;

import java.io.File;
import java.io.FileInputStream;
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
        // 1. Load default from classpath
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/test.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                setDefaults();
            }
        } catch (Exception ex) {
            setDefaults();
        }

        // 2. Load overriding config from local config/test.properties
        File file = new File("config/test.properties");
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                props.load(input);
                System.out.println("Loaded external configuration from: " + file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Error loading external configuration: " + ex.getMessage());
            }
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
        String value = System.getProperty(key);
        if (value != null) return value;
        return props.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) return value;
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