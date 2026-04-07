package com.amhs.swim.test.testcase;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Base abstract class for all test cases in the AMHS/SWIM Gateway Test Tool.
 * This class provides the foundation for defining test parameters and execution logic.
 * 
 * According to EUR Doc 047, test results are manually verified. Each test case 
 * implementation should log specific instructions for the tester.
 */
public abstract class BaseTestCase {
    protected String testCaseId;
    protected String testCaseName;

    /**
     * @param id The standard ICAO test case ID (e.g., CTSW101)
     * @param name A descriptive name for the test case
     */
    public BaseTestCase(String id, String name) {
        this.testCaseId = id;
        this.testCaseName = name;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * Legacy execution method. Should be avoided in favor of the parameterized version.
     * @return Execution status
     */
    public abstract boolean execute() throws Exception;
    
    /**
     * Main execution entry point for the test tool GUI. 
     * Allows testers to inject custom payloads or parameters before execution.
     * 
     * @param userInputs A map of key-value pairs provided by the user in the GUI forms.
     * @return true if injection was successful, false otherwise.
     */
    public boolean execute(Map<String, String> userInputs) throws Exception {
        return execute();
    }
    
    /**
     * Returns a list of parameters that will be rendered as input fields in the GUI.
     * @return List of TestParameter objects.
     */
    public List<TestParameter> getRequiredParameters() {
        return Collections.emptyList();
    }

    /**
     * Metadata class for GUI form generation.
     */
    public static class TestParameter {
        private String key;
        private String label;
        private String defaultValue;
        private boolean isLargeText;

        public TestParameter(String key, String label, String defaultValue, boolean isLargeText) {
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
            this.isLargeText = isLargeText;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getDefaultValue() { return defaultValue; }
        public boolean isLargeText() { return isLargeText; }
    }
}