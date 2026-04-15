package com.amhs.swim.test.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for the test tool.
 * Handles loading properties from classpath defaults and local file overrides.
 * Also manages case-specific payload configurations.
 */
public class TestConfig {
    private static TestConfig instance;
    private Properties props;
    private Properties casePayloads;
    private Map<String, Map<String, String>> defaultCasePayloads;

    private TestConfig() {
        props = new Properties();
        casePayloads = new Properties();
        defaultCasePayloads = new HashMap<>();
        initializeDefaultCasePayloads();
        loadConfig();
        loadCasePayloads();
    }

    /**
     * Initialize default payloads for all test cases.
     * These are used when reverting to defaults.
     */
    private void initializeDefaultCasePayloads() {
        // CTSW101 defaults
        Map<String, String> ctsW101Defaults = new HashMap<>();
        ctsW101Defaults.put("p1", "CTSW101 Text Payload");
        ctsW101Defaults.put("p2", "CTSW101 Binary Payload");
        defaultCasePayloads.put("CTSW101", ctsW101Defaults);

        // CTSW102 defaults
        Map<String, String> ctsW102Defaults = new HashMap<>();
        ctsW102Defaults.put("payload", "CTSW102 Rejection Sample");
        defaultCasePayloads.put("CTSW102", ctsW102Defaults);

        // CTSW103 defaults
        Map<String, String> ctsW103Defaults = new HashMap<>();
        ctsW103Defaults.put("p1", "CTSW103");
        defaultCasePayloads.put("CTSW103", ctsW103Defaults);

        // CTSW104 defaults (no specific payload params, uses topic/recipient)
        Map<String, String> ctsW104Defaults = new HashMap<>();
        defaultCasePayloads.put("CTSW104", ctsW104Defaults);

        // CTSW105 defaults
        Map<String, String> ctsW105Defaults = new HashMap<>();
        ctsW105Defaults.put("p1", "CTSW105 Default FT");
        ctsW105Defaults.put("p2", "CTSW105 Specific FT");
        defaultCasePayloads.put("CTSW105", ctsW105Defaults);

        // CTSW106 defaults
        Map<String, String> ctsW106Defaults = new HashMap<>();
        ctsW106Defaults.put("ohiContent", "CTSW106 OHI Content");
        defaultCasePayloads.put("CTSW106", ctsW106Defaults);

        // CTSW107 defaults
        Map<String, String> ctsW107Defaults = new HashMap<>();
        ctsW107Defaults.put("subjectText", "CTSW107 Subject");
        defaultCasePayloads.put("CTSW107", ctsW107Defaults);

        // CTSW108 defaults
        Map<String, String> ctsW108Defaults = new HashMap<>();
        ctsW108Defaults.put("originator", "VVTSYMYX");
        defaultCasePayloads.put("CTSW108", ctsW108Defaults);

        // CTSW109 defaults
        Map<String, String> ctsW109Defaults = new HashMap<>();
        ctsW109Defaults.put("originator", "UNKNOWN");
        defaultCasePayloads.put("CTSW109", ctsW109Defaults);

        // CTSW110 defaults
        Map<String, String> ctsW110Defaults = new HashMap<>();
        ctsW110Defaults.put("payload", "CTSW110 Unsupported Content");
        defaultCasePayloads.put("CTSW110", ctsW110Defaults);

        // CTSW111 defaults
        Map<String, String> ctsW111Defaults = new HashMap<>();
        ctsW111Defaults.put("payloadSize", "1000000");
        defaultCasePayloads.put("CTSW111", ctsW111Defaults);

        // CTSW112 defaults
        Map<String, String> ctsW112Defaults = new HashMap<>();
        ctsW112Defaults.put("recipientCount", "513");
        defaultCasePayloads.put("CTSW112", ctsW112Defaults);

        // CTSW113 defaults
        Map<String, String> ctsW113Defaults = new HashMap<>();
        ctsW113Defaults.put("rnPayload", "CTSW113 RN Request");
        ctsW113Defaults.put("nrnPayload", "CTSW113 NRN Request");
        defaultCasePayloads.put("CTSW113", ctsW113Defaults);

        // CTSW114 defaults
        Map<String, String> ctsW114Defaults = new HashMap<>();
        ctsW114Defaults.put("ndrPayload", "CTSW114 NDR Report");
        defaultCasePayloads.put("CTSW114", ctsW114Defaults);

        // CTSW115 defaults
        Map<String, String> ctsW115Defaults = new HashMap<>();
        ctsW115Defaults.put("textPayload", "CTSW115 Text ISO-8859-1");
        ctsW115Defaults.put("utf8Payload", "CTSW115 Text UTF-8");
        defaultCasePayloads.put("CTSW115", ctsW115Defaults);

        // CTSW116 defaults
        Map<String, String> ctsW116Defaults = new HashMap<>();
        ctsW116Defaults.put("binaryPayload", "CTSW116 GZIP Binary");
        defaultCasePayloads.put("CTSW116", ctsW116Defaults);
    }

    public static synchronized TestConfig getInstance() {
        if (instance == null) {
            instance = new TestConfig();
        }
        return instance;
    }

    private void loadConfig() {
        // 1. Load default configuration from classpath resources
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/test.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                setDefaults();
            }
        } catch (Exception ex) {
            setDefaults();
        }

        // 2. Load external overrides from config/test.properties if present
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

    public void setDefaults() {
        // Default fallback values for sandbox environment
        props.setProperty("amhs.mta.host", "localhost");
        props.setProperty("amhs.mta.port", "10000");
        props.setProperty("swim.broker.host", "localhost");
        props.setProperty("swim.broker.port", "5672");
        props.setProperty("swim.broker.user", "default");
        props.setProperty("swim.broker.password", "default");
        props.setProperty("swim.broker.vpn", "default");
        props.setProperty("swim.container.id", "amhs-swim-gateway-test");
        props.setProperty("directory.host", "ldap://localhost:389");
        props.setProperty("gateway.max_recipients", "512");
        props.setProperty("gateway.max_size", "1000000");
        props.setProperty("gateway.default_topic", "TEST.TOPIC");
        props.setProperty("gateway.test_recipient", "VVTSYMYX");
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public void saveConfig() {
        File file = new File("config/test.properties");
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(file)) {
            props.store(output, "AMHS/SWIM Gateway Test Tool Configuration");
            System.out.println("Config saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Error saving configuration: " + ex.getMessage());
        }
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

    /**
     * Load case payloads from config/case_payloads.properties
     */
    private void loadCasePayloads() {
        File file = new File("config/case_payloads.properties");
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                casePayloads.load(input);
                System.out.println("Loaded case payloads from: " + file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Error loading case payloads: " + ex.getMessage());
            }
        }
    }

    /**
     * Get a case payload value. Returns configured value or default if not set.
     * @param testCaseId The test case ID (e.g., "CTSW101")
     * @param paramKey The parameter key (e.g., "p1", "payload")
     * @return The configured value or the default value
     */
    public String getCasePayload(String testCaseId, String paramKey) {
        String configKey = testCaseId + "." + paramKey;
        String value = casePayloads.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        // Fallback to default
        Map<String, String> defaults = defaultCasePayloads.get(testCaseId);
        if (defaults != null) {
            return defaults.get(paramKey);
        }
        return "";
    }

    /**
     * Set a case payload value and save to config file.
     * @param testCaseId The test case ID
     * @param paramKey The parameter key
     * @param value The value to set
     */
    public void setCasePayload(String testCaseId, String paramKey, String value) {
        String configKey = testCaseId + "." + paramKey;
        casePayloads.setProperty(configKey, value);
        saveCasePayloads();
    }

    /**
     * Save case payloads to config/case_payloads.properties
     */
    public void saveCasePayloads() {
        File file = new File("config/case_payloads.properties");
        try (FileOutputStream output = new FileOutputStream(file)) {
            casePayloads.store(output, "AMHS/SWIM Gateway Test Tool - Case Payloads Configuration");
            System.out.println("Case payloads saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Error saving case payloads: " + ex.getMessage());
        }
    }

    /**
     * Revert a specific case's payloads to defaults.
     * @param testCaseId The test case ID to revert
     */
    public void revertCaseToDefault(String testCaseId) {
        // Remove all keys for this case from casePayloads
        casePayloads.keySet().removeIf(key -> key.toString().startsWith(testCaseId + "."));
        saveCasePayloads();
    }

    /**
     * Revert all case payloads to defaults.
     */
    public void revertAllCasesToDefault() {
        casePayloads.clear();
        saveCasePayloads();
    }

    /**
     * Get default payload for a case parameter.
     * @param testCaseId The test case ID
     * @param paramKey The parameter key
     * @return The default value
     */
    public String getDefaultCasePayload(String testCaseId, String paramKey) {
        Map<String, String> defaults = defaultCasePayloads.get(testCaseId);
        if (defaults != null) {
            return defaults.get(paramKey);
        }
        return "";
    }
}