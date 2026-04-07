package com.amhs.swim.test.testcase;

import com.amhs.swim.test.driver.SwimDriver;
import com.amhs.swim.test.util.Logger;
import com.amhs.swim.test.config.TestConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SwimToAmhsTests {

    private SwimDriver swimDriver;

    public SwimToAmhsTests() {
        this.swimDriver = new SwimDriver();
    }

    public SwimDriver getSwimDriver() {
        return swimDriver;
    }

    private void logTransmission(String caseId, String info) {
        Logger.log("INFO", "[" + caseId + "] Transmission: " + info);
    }

    private void logManualAction(String caseId, String step) {
        Logger.log("IMPORTANT", "\n>>> [GUIDELINE] " + caseId + " Manual Verification Step:\n" + step + "\n");
    }

    private void logProgress(String caseId, int current, int total) {
        if (total > 5) {
            if (current % 5 == 0 || current == total) {
                Logger.log("INFO", "[" + caseId + "] Progress: " + current + "/" + total + " messages sent.");
            }
        } else {
            Logger.log("INFO", "[" + caseId + "] Message " + current + "/" + total + " injected.");
        }
    }

    // ==================== DOMAIN G: Normal Message Conversion ====================
    // Standard conversion from AMQP messages (with or without AMHS-specific attributes) to X.400 IPMs.

    /**
     * CTSW101: Basic Message Conversion (AMQP unaware).
     * Purpose: Verify that the Gateway converts a simple AMQP message (no AMHS application properties)
     * into a standard AMHS IPM. 
     * 
     * Expected Result:
     * 1. AMHS message received at the AMHS tool.
     * 2. Subject maps to default (if provided) or remains empty.
     * 3. Priority maps to default value.
     */
    public BaseTestCase CTSW101 = new BaseTestCase("CTSW101", "Convert AMQP unaware message to AMHS") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Text Payload Content:", "CTSW101 Text Payload Content", true)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String textPayload = inputs.getOrDefault("p1", "CTSW101 Text Payload Content");
                logTransmission(testCaseId, "Injected simple text message, default properties.");
                
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setAmqpPriority((short) 4);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), textPayload.getBytes(), props);

                Logger.logVerification(testCaseId, "Simple message injected.\nPriority mapping: AMQP(4) -> AMHS(Normal/DD)");
                logManualAction(testCaseId, "VEFIFICATION STEPS:\n" +
                    "1. Confirm arrival of 1 message at the AMHS User Tool.\n" +
                    "2. Verify Priority is mapped correctly (default or DD).\n" +
                    "3. Ensure the text payload matches the injected string.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW102: Reject AMQP messages missing mandatory information.
     * Purpose: Verify Gateway rejection (NDR or discard) when required AMQP properties are missing.
     * 
     * Scenarios:
     * 1. Invalid Priority (e.g. string "INVALID_10")
     * 2. Empty Body/Payload
     * 3. Missing Recipients field
     * 
     * Expected Result:
     * 1. No messages arrive at the AMHS User Tool.
     * 2. Rejection log entries visible at the Gateway Control Position.
     */
    public BaseTestCase CTSW102 = new BaseTestCase("CTSW102", "Reject AMQP missing required info") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Msg 1 Payload (Invalid Priority):", "Reject Sample 1", false),
                new TestParameter("p2", "Msg 2 Payload (Empty):", "", false),
                new TestParameter("p3", "Msg 3 Payload (Missing Recipients):", "Reject Sample 3", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 3 malformed scenarios...");
                
                // 1. Invalid priority
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props1.setAtsPri("INVALID_10");
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.getOrDefault("p1", "").getBytes(), props1);

                // 2. Empty payload
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.getOrDefault("p2", "").getBytes(), props2);

                // 3. Missing recipients
                SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.getOrDefault("p3", "").getBytes(), props3);
                
                Logger.logVerification(testCaseId, "3 Rejection scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm that NO messages are delivered to the AMHS Tool for these 3 injections.\n" +
                    "2. Check Gateway Logs/Console for rejection reasons: 'Invalid Priority', 'Empty Content', or 'Missing Destination'.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW103: Service Level Mapping.
     * Purpose: Verify that the 'amhs_service_level' property is correctly mapped from AMQP to AMHS P1/P2 attributes.
     * 
     * Expected Result:
     * 1. 6 messages received.
     * 2. Message 1-2 (Basic/Extended) map to IPM.
     * 3. Message 3-4 (Content-Based) use 'content-identifier' mapping.
     * 4. Message 5-6 (Recipient-Based) use per-recipient attributes.
     */
    public BaseTestCase CTSW103 = new BaseTestCase("CTSW103", "Conversion per Service Level") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Msg 1 (Basic Text):", "CTSW103 Basic Text", false),
                new TestParameter("p2", "Msg 2 (Extended Text):", "CTSW103 Extended Text", false),
                new TestParameter("p3", "Msg 3 (Content-Octet):", "CTSW103 Content-Octet", false),
                new TestParameter("p4", "Msg 4 (Content-Text):", "CTSW103 Content-Text", false),
                new TestParameter("p5", "Msg 5 (Recip-All-Ext):", "CTSW103 Recip-All-Ext", false),
                new TestParameter("p6", "Msg 6 (Recip-Mixed):", "CTSW103 Recip-Mixed", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String[] levels = {"BASIC", "EXTENDED", "CONTENT_BASED", "CONTENT_BASED", "RECIPIENT_BASED", "RECIPIENT_BASED"};
                logTransmission(testCaseId, "Injecting 6 messages with varying Service Levels.");

                for (int i = 0; i < levels.length; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.toMap().put("amhs_service_level", levels[i]);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p" + (i+1)).getBytes(), props);
                    logProgress(testCaseId, i + 1, levels.length);
                }
                
                Logger.logVerification(testCaseId, "Service Level mapping injection complete.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Verify 6 messages at the AMHS Tool.\n" +
                    "2. Open each message and check P1/P2 headers for Service Level attributes (e.g., Content-Type, EITs, Per-Recipient flags).");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW104: Priority Mapping (AMQP -> AMHS).
     * Purpose: Verify that numeric AMQP priorities (0-9) and explicit 'amhs_ats_pri' 
     * properties map correctly to X.400 Priority (SS, DD, FF).
     * 
     * Expected Result:
     * 1. 15 messages received.
     * 2. AMQP Priority 0-3 -> AMHS Priority 'Non-Urgent' (GG/KK).
     * 3. AMQP Priority 4-6 -> AMHS Priority 'Normal' (FF/DD).
     * 4. AMQP Priority 7-9 -> AMHS Priority 'Urgent' (SS).
     * 5. Explicit 'amhs_ats_pri' (SS, DD, FF) overrides AMQP numeric priority.
     */
    public BaseTestCase CTSW104 = new BaseTestCase("CTSW104", "Convert AMQP with various priorities") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            List<TestParameter> params = new ArrayList<>();
            for (int i=0; i<10; i++) {
                params.add(new TestParameter("param_pri_" + i, "Msg AMQP Priority " + i + ":", "CTSW104 Priority " + i, false));
            }
            String[] atsPris = {"SS", "DD", "FF", "GG", "KK"};
            for (String pri : atsPris) {
                params.add(new TestParameter("param_ats_" + pri, "Msg ATS Priority " + pri + ":", "CTSW104 ATS PRI " + pri, false));
            }
            return params;
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 15-message priority matrix.");
                
                // Priorities 0-9
                for (int i = 0; i < 10; i++) {
                    String payload = inputs.getOrDefault("param_pri_" + i, "CTSW104 Priority " + i);
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setAmqpPriority((short) i);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), payload.getBytes(), props);
                    logProgress(testCaseId, i + 1, 15);
                }
                
                // ATS PRI SS, DD, FF, GG, KK
                String[] atsPris = {"SS", "DD", "FF", "GG", "KK"};
                for (int i = 0; i < atsPris.length; i++) {
                    String payload = inputs.getOrDefault("param_ats_" + atsPris[i], "CTSW104 ATS PRI " + atsPris[i]);
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setAtsPri(atsPris[i]);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), payload.getBytes(), props);
                    logProgress(testCaseId, i + 11, 15);
                }
                
                Logger.logVerification(testCaseId, "Priority matrix injection complete.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 15 messages.\n" +
                    "2. Check the Priority field of each message against the EUR Doc 047 mapping table (Table 5-1).\n" +
                    "3. Pay special attention to explicit SS/DD/FF overrides.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW105: Filing Time Mapping.
     * Purpose: Verify mapping of the 'amhs_filing_time' application property.
     * 
     * Expected Result:
     * 1. Msg 1 (No FT): AMHS Filing Time should default to the current GMT time.
     * 2. Msg 2 (Explicit FT): AMHS Filing Time should exactly match the '250102' string.
     */
    public BaseTestCase CTSW105 = new BaseTestCase("CTSW105", "Convert AMQP with filing-time mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Msg 1 Payload (Empty FT):", "CTSW105 Default FT", false),
                new TestParameter("ft", "Msg 2 Filing Time (YYMMDDHHMMSS):", "250102120000", false),
                new TestParameter("p2", "Msg 2 Payload (Specific FT):", "CTSW105 Specific FT", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                // 1. Default
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.getOrDefault("p1", "").getBytes(), props1);

                // 2. Explicit
                String ft = inputs.getOrDefault("ft", "250102120000");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props2.setFilingTime(ft);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.getOrDefault("p2", "").getBytes(), props2);
                
                Logger.logVerification(testCaseId, "Filing time scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 2 messages.\n" +
                    "2. Check Msg 1: Filing Time should match the time of injection.\n" +
                    "3. Check Msg 2: Filing Time must be '" + ft + "'.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW106: Originator-Handled-Identifier (OHI) Mapping.
     * Purpose: Verify mapping and truncation of the 'amhs_ats_ohi' application property.
     * 
     * Limits (EUR Doc 047 Table 5-2):
     * - SS/DD/FF Priority: 48 characters.
     * - GG/KK Priority: 53 characters.
     * 
     * Expected Result:
     * 1. 6 messages received.
     * 2. 'OHI' fields in AMHS match either the full string (if within limit) or truncated string (if over limit).
     */
    public BaseTestCase CTSW106 = new BaseTestCase("CTSW106", "Convert AMQP with OHI mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Msg 1 (LO-PRI-SHORT):", "OHI-SHORT", false),
                new TestParameter("p2", "Msg 2 (LO-PRI-EXACT):", "A".repeat(53), false),
                new TestParameter("p3", "Msg 3 (LO-PRI-LONG):", "A".repeat(60), false),
                new TestParameter("p4", "Msg 4 (HI-PRI-SHORT):", "OHI-HI-SHORT", false),
                new TestParameter("p5", "Msg 5 (HI-PRI-EXACT):", "B".repeat(48), false),
                new TestParameter("p6", "Msg 6 (HI-PRI-LONG):", "B".repeat(60), false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String[] ohiInputs = {inputs.get("p1"), inputs.get("p2"), inputs.get("p3"), inputs.get("p4"), inputs.get("p5"), inputs.get("p6")};
                int[] priorities = {4, 4, 4, 7, 7, 7}; // 4=Normal, 7=Urgent
                
                logTransmission(testCaseId, "Injecting 6 OHI scenarios (Short, Exact, Overflow).");
                for (int i = 0; i < ohiInputs.length; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setAmqpPriority((short) priorities[i]);
                    props.toMap().put("amhs_ats_ohi", ohiInputs[i]); 
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), "OHI Test Content".getBytes(), props);
                    logProgress(testCaseId, i + 1, 6);
                }
                
                Logger.logVerification(testCaseId, "OHI mapping injection complete.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 6 messages.\n" +
                    "2. Verify OHI mapping: High Priority (SS/DD/FF) OHI should be truncated at 48 chars.\n" +
                    "3. Verify Low Priority (GG/KK) OHI should be truncated at 53 chars.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW107: Subject Mapping.
     * Purpose: Verify mapping from AMQP 'subject' or 'amhs_subject' property 
     * to the AMHS IPM subject field.
     * 
     * Expected Result:
     * 1. Msg 1 (Long Subject): AMHS subject matches truncated AMQP subject (128 chars).
     * 2. Msg 2 (Normal Subject): AMHS subject matches AMQP subject.
     * 3. Msg 3 (Application Property): AMHS subject matches 'amhs_subject' value.
     */
    public BaseTestCase CTSW107 = new BaseTestCase("CTSW107", "Convert AMQP with subject mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("s1", "Msg 1 Subject (Long):", "S".repeat(150), false),
                new TestParameter("p1", "Msg 1 Payload:", "Payload for Long Subject", false),
                new TestParameter("s2", "Msg 2 Subject (Normal):", "Normal Test Subject", false),
                new TestParameter("p2", "Msg 2 Payload:", "Payload for Normal Subject", false),
                new TestParameter("s3", "Msg 3 App Property Subject:", "App Prop Subject", false),
                new TestParameter("p3", "Msg 3 Payload:", "Payload for App Prop", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                // 1. Long
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props1.setSubject(inputs.get("s1"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p1").getBytes(), props1);

                // 2. Normal
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props2.setSubject(inputs.get("s2"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p2").getBytes(), props2);

                // 3. App Prop
                SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                props3.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props3.toMap().put("amhs_subject", inputs.get("s3"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p3").getBytes(), props3);

                Logger.logVerification(testCaseId, "Subject mapping scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 3 messages.\n" +
                    "2. Verify Msg 1 subject is truncated at 128 characters.\n" +
                    "3. Verify Msg 2 and Msg 3 subjects match the injected strings.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW108: Known Originator Mapping.
     * Purpose: Verify conversion when the AMQP 'amhs_originator' corresponds to a known O/R address.
     * 
     * Expected Result:
     * 1. 1 message received.
     * 2. Originator O/R address matches the mapping defined in the ATN Directory.
     */
    public BaseTestCase CTSW108 = new BaseTestCase("CTSW108", "Known Originator") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("ori", "Originator Address (e.g. VVTSYMYX):", "VVTSYMYX", false),
                new TestParameter("payload", "Message Payload:", "CTSW108 Known Originator content", true)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String ori = inputs.getOrDefault("ori", "VVTSYMYX");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setOriginator(ori);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("payload").getBytes(), props);
                
                Logger.logVerification(testCaseId, "Known originator message injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 1 message.\n" +
                    "2. Verify the P1 Originator field matches the O/R address mapped to '" + ori + "'.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW109: Unknown Originator Handling.
     * Purpose: Verify handling of unknown originators (discared or default O/R address).
     * 
     * Expected Result:
     * 1. Gateway may reject the message or use a configured default-originator address.
     * 2. If rejected, no message arrives at the terminal.
     */
    public BaseTestCase CTSW109 = new BaseTestCase("CTSW109", "Unknown Originator") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("ori", "Unknown Originator (e.g. INVALID_OR):", "INVALID_OR", false),
                new TestParameter("payload", "Message Payload:", "CTSW109 Unknown Originator content", true)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String ori = inputs.getOrDefault("ori", "INVALID_OR");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setOriginator(ori);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("payload").getBytes(), props);
                
                Logger.logVerification(testCaseId, "Unknown originator message injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm if message arrives at terminal (fallback) or is rejected.\n" +
                    "2. Check Gateway log for 'Originator lookup failed' or equivalent warning.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW110: Content-Type Validation (ICAO EUR Doc 047).
     * Purpose: Verify Gateway behavior for various Content-Type and Body Section combinations.
     * 
     * Scenarios:
     * 1. application/octet-stream + Empty body (Expected: REJECT)
     * 2. text/plain (UTF-8) + Binary data in section (Expected: REJECT)
     * 3. application/octet-stream + Binary data in section (Expected: ACCEPT)
     * 4. text/plain (UTF-8) + String in AmqpValue (Expected: ACCEPT)
     * 5. text/plain (UTF-16) (Expected: REJECT - unsupported encoding)
     * 
     * Expected Result:
     * - Only messages 3 and 4 should be delivered to the AMHS Tool.
     */
    public BaseTestCase CTSW110 = new BaseTestCase("CTSW110", "Reject AMQP unsupported content-type") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p3", "Payload for Accepted Binary (Msg 3):", "Binary Content Sample", false),
                new TestParameter("p4", "Payload for Accepted Text (Msg 4):", "Text Content Sample", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String[] contentTypes = {"application/octet-stream", "text/plain; charset=utf-8", "application/octet-stream", "text/plain; charset=utf-8", "text/plain; charset=utf-16"};
                SwimDriver.AMQPProperties.BodyType[] bodyTypes = {SwimDriver.AMQPProperties.BodyType.DATA, SwimDriver.AMQPProperties.BodyType.DATA, SwimDriver.AMQPProperties.BodyType.DATA, SwimDriver.AMQPProperties.BodyType.AMQP_VALUE, SwimDriver.AMQPProperties.BodyType.AMQP_VALUE};
                byte[][] payloads = {null, new byte[]{0x01, 0x02}, inputs.get("p3").getBytes(), inputs.get("p4").getBytes(), "UTF-16 Content".getBytes()};

                logTransmission(testCaseId, "Injecting 5 Content-Type scenarios.");
                for (int i = 0; i < contentTypes.length; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setContentType(contentTypes[i]);
                    props.setBodyType(bodyTypes[i]);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), payloads[i], props);
                    logProgress(testCaseId, i + 1, 5);
                }

                Logger.logVerification(testCaseId, "Content-Type scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm that ONLY Msg 3 and Msg 4 arrive at the AMHS Tool.\n" +
                    "2. Check Gateway log for rejection of messages 1, 2, and 5.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW111: Maximum Payload Size Validation.
     * Purpose: Verify that the Gateway enforces the maximum message size limit.
     * 
     * Scenarios:
     * a) AmqpValue within limit (Accept)
     * b) Data section within limit (Accept)
     * c) AmqpValue exceeding limit (Reject)
     * d) Data section exceeding limit (Reject)
     * 
     * Expected Result:
     * - Only messages A and B should be delivered.
     */
    public BaseTestCase CTSW111 = new BaseTestCase("CTSW111", "Reject AMQP if payload exceeds max size") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("max", "Configured Max Size (bytes):", "1048576", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                int max = Integer.parseInt(inputs.getOrDefault("max", "1048576"));
                logTransmission(testCaseId, "Injecting 4 size-limit scenarios around " + max + " bytes.");
                
                byte[][] payloads = {new byte[1024], new byte[max - 512], new byte[max + 1024], new byte[max + 2048]};
                SwimDriver.AMQPProperties.BodyType[] bodyTypes = {SwimDriver.AMQPProperties.BodyType.AMQP_VALUE, SwimDriver.AMQPProperties.BodyType.DATA, SwimDriver.AMQPProperties.BodyType.AMQP_VALUE, SwimDriver.AMQPProperties.BodyType.DATA};

                for (int i = 0; i < 4; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setBodyType(bodyTypes[i]);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), payloads[i], props);
                    logProgress(testCaseId, i + 1, 4);
                }
                
                Logger.logVerification(testCaseId, "Size limit scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of FIRST TWO messages.\n" +
                    "2. Confirm Gateway rejection of LAST TWO messages (NDR or discard).");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    // ==================== DOMAIN H: Rejection and Validation ====================
    // These cases involve malformed data or policy violations, marked with ***.

    /**
     * CTSW112: Maximum Recipient Count Validation.
     * Purpose: Verify that the Gateway enforces the maximum number of recipients (e.g. 512).
     * 
     * Expected Result:
     * 1. Message rejected by the Gateway.
     * 2. No message delivered to any AMHS address.
     */
    public BaseTestCase CTSW112 = new BaseTestCase("CTSW112", "Reject AMQP if recipients exceed max") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("count", "Recipient Count (should exceed limit):", "600", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                int count = Integer.parseInt(inputs.getOrDefault("count", "600"));
                logTransmission(testCaseId, "Generating " + count + " recipient addresses.");
                
                StringBuilder recipients = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    if (i > 0) recipients.append(",");
                    recipients.append("VVTS").append(String.format("%04d", i));
                }
                
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(recipients.toString());
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), "Over-limit Payload".getBytes(), props);
                
                Logger.logVerification(testCaseId, "Over-limit recipient message injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm Gateway rejection of the message.\n" +
                    "2. Verify NO delivery in AMHS backend logs for any of the generated O/R addresses.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW113: Receipt/Non-Receipt Notifications (RN/NRN).
     * Purpose: Verify handling of AMHS notification requests and AMQP priority level 6 mapping.
     * 
     * Requirement: AMQP Priority Level 6 must map to AMHS Priority SS (Urgent).
     * 
     * Expected Result:
     * 1. Msg 1 (NRN request) received at terminal.
     * 2. Msg 2 (RN request) received at terminal.
     * 3. Tester sends NRN back for Msg 1 and RN back for Msg 2.
     */
    public BaseTestCase CTSW113 = new BaseTestCase("CTSW113", "Incoming RN/NRN Notifications") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Payload for NRN Request:", "Notification Test Msg 1", false),
                new TestParameter("p2", "Payload for RN Request:", "Notification Test Msg 2", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 2 messages with RN/NRN requests (Priority 6).");
                for (int i = 1; i <= 2; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setAmqpPriority((short) 6);
                    props.toMap().put("amhs_notification_request", "rn,nrn");
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p" + i).getBytes(), props);
                    logProgress(testCaseId, i, 2);
                }
                Logger.logVerification(testCaseId, "Notification request injection complete.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm both messages arrive at the AMHS terminal.\n" +
                    "2. Verify Priority is mapped to SS.\n" +
                    "3. Manually RETURN an NRN for Message 1 and an RN for Message 2.\n" +
                    "4. Confirm that the Gateway forwards these reports back to the SWIM side.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    // ==================== DOMAIN J: Incoming Reports and Notifications ====================

    /**
     * CTSW114: Non-Delivery Report (NDR) Forwarding.
     * Purpose: Verify that AMHS NDRs are correctly forwarded to the SWIM side.
     * 
     * Expected Result:
     * 1. Message fails delivery in AMHS (manual deletion).
     * 2. Gateway receives NDR from AMHS MTA.
     * 3. Gateway converts and forwards NDR to SWIM side.
     */
    public BaseTestCase CTSW114 = new BaseTestCase("CTSW114", "Incoming NDR Reports") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Payload for NDR Trigger:", "Msg to trigger NDR", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting message to trigger NDR.");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("p1").getBytes(), props);
                
                Logger.logVerification(testCaseId, "NDR trigger message injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Manually DELETE or REJECT this message at the AMHS MTA/Terminal to trigger an NDR.\n" +
                    "2. Wait and confirm that the Gateway produces an AMQP message on the 'REPORTS' topic representing this NDR.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    // ==================== DOMAIN I: Body Part Type and Encoding ====================

    /**
     * CTSW115: Character Encoding Mapping.
     * Purpose: Verify mapping from AMQP 'amhs_content_encoding' to GeneralText/IA5Text body parts.
     * 
     * Expected Result:
     * 4 messages received:
     * 1. Msg 1 (IA5): Maps to ia5-text-body-part.
     * 2. Msg 2 (ISO-646): Maps to general-text-body-part.
     * 3. Msg 3 (ISO-8859-1): Maps to general-text-body-part.
     * 4. Msg 4 (General-Text): Maps to general-text-body-part.
     */
    public BaseTestCase CTSW115 = new BaseTestCase("CTSW115", "Convert AMQP with encoding mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("m1", "IA5 Payload:", "Sample IA5 Content", false),
                new TestParameter("m2", "ISO-646 Payload:", "Sample ISO-646 Content", false),
                new TestParameter("m3", "ISO-8859-1 Payload:", "Sample ISO-8859-1 Content", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String[] bodyParts = {"ia5-text", "general-text-body-part", "general-text-body-part"};
                String[] encodings = {"IA5", "ISO-646", "ISO-8859-1"};
                
                logTransmission(testCaseId, "Injecting 3 character encoding scenarios.");
                for (int i = 0; i < 3; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                    props.setBodyPartType(bodyParts[i]);
                    props.toMap().put("amhs_content_encoding", encodings[i]);
                    swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("m" + (i+1)).getBytes(), props);
                    logProgress(testCaseId, i + 1, 3);
                }
                
                Logger.logVerification(testCaseId, "Character encoding mapping injection complete.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 3 messages.\n" +
                    "2. Check each message O/R structure for correct Body Part Type and Encoding identifiers.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    /**
     * CTSW116: Binary Payloads (FTBP and GZIP).
     * Purpose: Verify handling of FTBP attributes and GZIP decompression.
     * 
     * Expected Result:
     * 1. Msg 1 (FTBP): Received as a File Transfer body part in AMHS with filename 'test.txt' and size 1024.
     * 2. Msg 2 (GZIP): Gateway decompresses the payload and delivers it as plain text or binary in AMHS.
     */
    public BaseTestCase CTSW116 = new BaseTestCase("CTSW116", "Convert binary AMQP with FTBP and GZIP") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("file", "FTBP Filename:", "test.txt", false),
                new TestParameter("data", "Binary Payload (Text):", "Binary Content to be GZIPped", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String file = inputs.getOrDefault("file", "test.txt");
                logTransmission(testCaseId, "Injecting 2 binary scenarios (FTBP and GZIP).");
                
                // 1. FTBP
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props1.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                props1.toMap().put("amhs_ftbp_file_name", file);
                props1.toMap().put("amhs_ftbp_object_size", "1024");
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("data").getBytes(), props1);

                // 2. GZIP
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                props2.toMap().put("swim_compression", "gzip");
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), inputs.get("data").getBytes(), props2);
                
                Logger.logVerification(testCaseId, "Binary scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm Msg 1 is an FTBP and has filename '" + file + "'.\n" +
                    "2. Confirm Msg 2 content matches the injected string (verifying Gateway decompression).");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };
}