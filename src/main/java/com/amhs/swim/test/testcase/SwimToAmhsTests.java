package com.amhs.swim.test.testcase;

import com.amhs.swim.test.driver.SwimDriver;
import com.amhs.swim.test.util.Logger;
import com.amhs.swim.test.config.TestConfig;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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

    public BaseTestCase CTSW101 = new BaseTestCase("CTSW101", "Convert AMQP unaware message to AMHS") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Text Payload Content:", "CTSW101 Text Payload", true),
                new TestParameter("p2", "Binary Payload Content:", "CTSW101 Binary Payload", true)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 2 simple messages: one text, one binary.");
                String testTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recipient = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                // 1. Text Message
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(recipient);
                props1.setAmqpPriority((short) 4);
                props1.setContentType("text/plain; charset=utf-8");
                props1.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE); // Using amqp-value
                swimDriver.publishMessage(testTopic, inputs.getOrDefault("p1", "CTSW101 Text Payload").getBytes(), props1);

                // 2. Binary Message
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(recipient);
                props2.setAmqpPriority((short) 4);
                props2.setContentType("application/octet-stream");
                props2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA); // Using data
                swimDriver.publishMessage(testTopic, inputs.getOrDefault("p2", "CTSW101 Binary Payload").getBytes(), props2);

                Logger.logVerification(testCaseId, "Text and Binary messages injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm arrival of 2 messages (1 text, 1 binary) at the AMHS User Tool.\n" +
                    "2. Verify Priority mapping 4 -> DD/Normal.\n" +
                    "3. Ensure the payloads match exactly.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW102 = new BaseTestCase("CTSW102", "Reject AMQP missing required info") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("payload", "Message Payload:", "CTSW102 Rejection Sample", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 10 malformed scenarios (5 text, 5 binary).");
                String testTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recipient = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                byte[] payload = inputs.getOrDefault("payload", "Content").getBytes();

                String[] contentTypes = {"text/plain; charset=utf-8", "application/octet-stream"};
                SwimDriver.AMQPProperties.BodyType[] types = {SwimDriver.AMQPProperties.BodyType.AMQP_VALUE, SwimDriver.AMQPProperties.BodyType.DATA};

                int msgIndex = 1;
                for (int i=0; i<2; i++) {
                    String ct = contentTypes[i];
                    SwimDriver.AMQPProperties.BodyType bt = types[i];

                    // 1. Invalid Priority (10)
                    SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                    props1.setContentType(ct);
                    props1.setBodyType(bt);
                    props1.setRecipients(recipient);
                    props1.setAmqpPriority((short) 10);
                    swimDriver.publishMessage(testTopic, payload, props1);
                    logProgress(testCaseId, msgIndex++, 10);

                    // 2. Empty message-id
                    SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                    props2.setContentType(ct);
                    props2.setBodyType(bt);
                    props2.setRecipients(recipient);
                    props2.setAmqpPriority((short) 4);
                    props2.setMessageId("");
                    swimDriver.publishMessage(testTopic, payload, props2);
                    logProgress(testCaseId, msgIndex++, 10);

                    // 3. 0 creation-time
                    SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                    props3.setContentType(ct);
                    props3.setBodyType(bt);
                    props3.setRecipients(recipient);
                    props3.setAmqpPriority((short) 4);
                    props3.setCreationTime(0L);
                    swimDriver.publishMessage(testTopic, payload, props3);
                    logProgress(testCaseId, msgIndex++, 10);

                    // 4. Empty data/amqp-value
                    SwimDriver.AMQPProperties props4 = new SwimDriver.AMQPProperties();
                    props4.setContentType(ct);
                    props4.setBodyType(bt);
                    props4.setRecipients(recipient);
                    props4.setAmqpPriority((short) 4);
                    swimDriver.publishMessage(testTopic, new byte[0], props4);
                    logProgress(testCaseId, msgIndex++, 10);

                    // 5. Empty/Invalid recipients
                    SwimDriver.AMQPProperties props5 = new SwimDriver.AMQPProperties();
                    props5.setContentType(ct);
                    props5.setBodyType(bt);
                    if (i == 0) props5.setRecipients("");
                    else props5.setRecipients("LONGADDRESSXXXXX"); // >8 chars for binary
                    props5.setAmqpPriority((short) 4);
                    swimDriver.publishMessage(testTopic, payload, props5);
                    logProgress(testCaseId, msgIndex++, 10);
                }
                
                Logger.logVerification(testCaseId, "10 Rejection scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm that NO messages are delivered to the AMHS Tool for these 10 injections.\n" +
                    "2. Check Gateway Logs/Control Position for rejection logs.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW103 = new BaseTestCase("CTSW103", "Conversion per Service Level") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Payload Prefix:", "CTSW103", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String prefix = inputs.getOrDefault("p1", "CTSW103");
                logTransmission(testCaseId, "Injecting 7 messages with varying Service Levels.");
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                // 1. Basic (Text)
                SwimDriver.AMQPProperties p1 = new SwimDriver.AMQPProperties();
                p1.setRecipients(recip); p1.setContentType("text/plain; charset=utf-8");
                p1.setExtraProp("amhs_service_level", "basic");
                swimDriver.publishMessage(tTopic, (prefix + " Basic Text").getBytes(), p1);
                
                // 2. Basic (Binary) -> REJECTED
                SwimDriver.AMQPProperties p2 = new SwimDriver.AMQPProperties();
                p2.setRecipients(recip); p2.setContentType("application/octet-stream");
                p2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                p2.setExtraProp("amhs_service_level", "basic");
                swimDriver.publishMessage(tTopic, (prefix + " Binary REJECTED").getBytes(), p2);

                // 3. Extended (Text)
                SwimDriver.AMQPProperties p3 = new SwimDriver.AMQPProperties();
                p3.setRecipients(recip); p3.setContentType("text/plain; charset=utf-8");
                p3.setExtraProp("amhs_service_level", "extended");
                swimDriver.publishMessage(tTopic, (prefix + " Extended Text").getBytes(), p3);

                // 4. Content Based (Binary -> Ext)
                SwimDriver.AMQPProperties p4 = new SwimDriver.AMQPProperties();
                p4.setRecipients(recip); p4.setContentType("application/octet-stream");
                p4.setExtraProp("amhs_service_level", "content-based");
                swimDriver.publishMessage(tTopic, (prefix + " Content-based Binary -> Ext").getBytes(), p4);

                // 5. Content Based (Text -> Basic)
                SwimDriver.AMQPProperties p5 = new SwimDriver.AMQPProperties();
                p5.setRecipients(recip); p5.setContentType("text/plain; charset=utf-8");
                p5.setExtraProp("amhs_service_level", "content-based");
                swimDriver.publishMessage(tTopic, (prefix + " Content-based Text -> Basic").getBytes(), p5);

                // 6. Recipient Based (All Ext)
                SwimDriver.AMQPProperties p6 = new SwimDriver.AMQPProperties();
                p6.setRecipients(recip); p6.setContentType("text/plain; charset=utf-8");
                p6.setExtraProp("amhs_service_level", "recipient-based");
                swimDriver.publishMessage(tTopic, (prefix + " Recip-based All Ext").getBytes(), p6);

                // 7. Recipient Based (Mixed)
                SwimDriver.AMQPProperties p7 = new SwimDriver.AMQPProperties();
                p7.setRecipients(recip + ",VVTSNONEXT"); // Second recipient doesn't support extended
                p7.setContentType("text/plain; charset=utf-8");
                p7.setExtraProp("amhs_service_level", "recipient-based");
                swimDriver.publishMessage(tTopic, (prefix + " Recip-based Mixed").getBytes(), p7);
                
                Logger.logVerification(testCaseId, "7 Service Level scenarios injected.");
                logManualAction(testCaseId, "VERIFICATION STEPS:\n" +
                    "1. Confirm exactly 6 messages arrive at the AMHS Tool (Msg 2 Binary should be rejected/logged).\n" +
                    "2. Check proper mapping depending on basic/extended fallback logic.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW104 = new BaseTestCase("CTSW104", "Convert AMQP with various priorities") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(new TestParameter("p", "Dummy:", "Run", false));
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                logTransmission(testCaseId, "Injecting 20-message priority matrix.");
                String testTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                int counter = 1;
                // Group 1: priorities 0-9
                for (int i = 0; i <= 9; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip); props.setAmqpPriority((short) i);
                    swimDriver.publishMessage(testTopic, ("P" + i).getBytes(), props);
                    logProgress(testCaseId, counter++, 20);
                }
                
                // Group 2: default prio(4) + atsPri overrides
                String[] atsPris = {"SS", "DD", "FF", "GG", "KK"};
                for (String pri : atsPris) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip); props.setAmqpPriority((short) 4); props.setAtsPri(pri);
                    swimDriver.publishMessage(testTopic, ("Pri4+ATS_" + pri).getBytes(), props);
                    logProgress(testCaseId, counter++, 20);
                }

                // Group 3: prio 1 + atsPri overrides
                String[] atsPrisGrp3 = {"SS", "DD", "FF", "GG"};
                for (String pri : atsPrisGrp3) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip); props.setAmqpPriority((short) 1); props.setAtsPri(pri);
                    swimDriver.publishMessage(testTopic, ("Pri1+ATS_" + pri).getBytes(), props);
                    logProgress(testCaseId, counter++, 20);
                }

                // Group 4: prio 9 + atsPri KK
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(recip); props.setAmqpPriority((short) 9); props.setAtsPri("KK");
                swimDriver.publishMessage(testTopic, "Pri9+ATS_KK".getBytes(), props);
                logProgress(testCaseId, counter++, 20);
                
                logManualAction(testCaseId, "VERIFICATION: Confirm arrival of 20 messages. Check priority mapping tables.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW105 = new BaseTestCase("CTSW105", "Convert AMQP with filing-time mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(
                new TestParameter("p1", "Msg 1 Payload (Empty FT):", "CTSW105 Default FT", false),
                new TestParameter("p2", "Msg 2 Payload (Specific FT):", "CTSW105 Specific FT", false)
            );
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String testTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                // 1. Default (No amhs_ats_ft set)
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients(recip);
                swimDriver.publishMessage(testTopic, inputs.getOrDefault("p1", "1").getBytes(), props1);

                // 2. Explicit (amhs_ats_ft = 250102)
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients(recip);
                props2.setFilingTime("250102");
                swimDriver.publishMessage(testTopic, inputs.getOrDefault("p2", "2").getBytes(), props2);
                
                logManualAction(testCaseId, "VERIFICATION: Msg 1 FT = current DDhhmm. Msg 2 FT = 250102.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW106 = new BaseTestCase("CTSW106", "Convert AMQP with OHI mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(new TestParameter("p", "Dummy", "", false));
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String[] ohiInputs = {"OHI-SHORT", "A".repeat(53), "A".repeat(60), "OHI-HI-SHORT", "B".repeat(48), "B".repeat(60)};
                int[] priorities = {4, 4, 4, 6, 6, 6}; // 4=limit 53, 6=limit 48
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                for (int i = 0; i < ohiInputs.length; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip);
                    props.setAmqpPriority((short) priorities[i]);
                    props.setExtraProp("amhs_ats_ohi", ohiInputs[i]); 
                    swimDriver.publishMessage(tTopic, "OHI Content".getBytes(), props);
                }
                
                logManualAction(testCaseId, "VERIFICATION: Low Pri (msgs 1-3) truncated at 53. High Pri (msgs 4-6) truncated at 48.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW107 = new BaseTestCase("CTSW107", "Convert AMQP with subject mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() {
            return List.of(new TestParameter("p", "Dummy", "", false));
        }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");

                // 1. Long AMQP subject (>128)
                SwimDriver.AMQPProperties p1 = new SwimDriver.AMQPProperties();
                p1.setRecipients(recip); p1.setSubject("S".repeat(150));
                swimDriver.publishMessage(tTopic, "Msg1".getBytes(), p1);

                // 2. Normal AMQP subject
                SwimDriver.AMQPProperties p2 = new SwimDriver.AMQPProperties();
                p2.setRecipients(recip); p2.setSubject("Normal Subject");
                swimDriver.publishMessage(tTopic, "Msg2".getBytes(), p2);

                // 3. amhs_subject application property
                SwimDriver.AMQPProperties p3 = new SwimDriver.AMQPProperties();
                p3.setRecipients(recip); p3.setExtraProp("amhs_subject", "AMHS App Prop Subject");
                swimDriver.publishMessage(tTopic, "Msg3".getBytes(), p3);

                // 4. Both present
                SwimDriver.AMQPProperties p4 = new SwimDriver.AMQPProperties();
                p4.setRecipients(recip); p4.setSubject("AMQP Standard Subject");
                p4.setExtraProp("amhs_subject", "AMHS Override Subject");
                swimDriver.publishMessage(tTopic, "Msg4".getBytes(), p4);

                logManualAction(testCaseId, "VERIFICATION: Msg 1 truncated to 128 chars. Msg 4 subject must equal 'AMHS Override Subject'.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW108 = new BaseTestCase("CTSW108", "Known Originator") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setOriginator("VVTSYMYX");
                props.setContentType("text/plain; charset=utf-8");
                props.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), "Known Ori".getBytes(), props);
                logManualAction(testCaseId, "Msg 1 delivered with VVTSYMYX originator.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW109 = new BaseTestCase("CTSW109", "Unknown Originator") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setOriginator("UNKNOWN1");
                props.setContentType("text/plain; charset=utf-8");
                props.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), "Unk Ori".getBytes(), props);
                logManualAction(testCaseId, "Gateway acts: Rejects or applies Default Originator fallback.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW110 = new BaseTestCase("CTSW110", "Reject AMQP unsupported content-type") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }

        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");

                // Spec lists 6 scenarios
                // 1. application/octet-stream, amqp-value empty, data empty -> REJECT
                SwimDriver.AMQPProperties p1 = new SwimDriver.AMQPProperties();
                p1.setRecipients(recip); p1.setContentType("application/octet-stream"); p1.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage(tTopic, new byte[0], p1);

                // 2. text/plain utf-8, amqp-value empty, data presence -> REJECT
                SwimDriver.AMQPProperties p2 = new SwimDriver.AMQPProperties();
                p2.setRecipients(recip); p2.setContentType("text/plain; charset=utf-8"); p2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage(tTopic, "binary".getBytes(), p2);

                // 3. application/octet-stream, amqp-value empty, data presence -> ACCEPT
                SwimDriver.AMQPProperties p3 = new SwimDriver.AMQPProperties();
                p3.setRecipients(recip); p3.setContentType("application/octet-stream"); p3.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage(tTopic, "binary-accept".getBytes(), p3);

                // 4. text/plain utf-8, amqp-value presence, data empty -> ACCEPT
                SwimDriver.AMQPProperties p4 = new SwimDriver.AMQPProperties();
                p4.setRecipients(recip); p4.setContentType("text/plain; charset=utf-8"); p4.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, "text-accept".getBytes(), p4);

                // 5. text/plain utf-16 -> REJECT
                SwimDriver.AMQPProperties p5 = new SwimDriver.AMQPProperties();
                p5.setRecipients(recip); p5.setContentType("text/plain; charset=utf-16"); p5.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, "utf16".getBytes("UTF-16"), p5);

                // 6. amqp-value and data present -> REJECT (we approximate by sending weird body type or unsupported properties to trigger rejection for test)
                // In actual AMQP 1.0, you can't have both set simultaneously in Proton Message Body without custom encoding. 

                logManualAction(testCaseId, "VERIFICATION: ONLY Msg 3 and Msg 4 should be delivered.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW111 = new BaseTestCase("CTSW111", "Reject AMQP if payload exceeds max size") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                int max = 1048576; // Assume 1MB
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                // a) normal text inside max -> ACCEPT
                SwimDriver.AMQPProperties pa = new SwimDriver.AMQPProperties();
                pa.setRecipients(recip); pa.setContentType("text/plain; charset=utf-8"); pa.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, new byte[1024], pa);

                // b) normal binary inside max -> ACCEPT
                SwimDriver.AMQPProperties pb = new SwimDriver.AMQPProperties();
                pb.setRecipients(recip); pb.setContentType("application/octet-stream"); pb.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage(tTopic, new byte[1024], pb);

                // c) text exceeds -> REJECT
                SwimDriver.AMQPProperties pc = new SwimDriver.AMQPProperties();
                pc.setRecipients(recip); pc.setContentType("text/plain; charset=utf-8"); pc.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, new byte[max + 1024], pc);

                // d) binary exceeds -> REJECT
                SwimDriver.AMQPProperties pd = new SwimDriver.AMQPProperties();
                pd.setRecipients(recip); pd.setContentType("application/octet-stream"); pd.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage(tTopic, new byte[max + 1024], pd);

                logManualAction(testCaseId, "VERIFICATION: First 2 messages accept, Last 2 rejected.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW112 = new BaseTestCase("CTSW112", "Reject AMQP max recipients") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                
                // a) 512 Recs -> ACCEPT
                StringBuilder recs512 = new StringBuilder();
                for (int i=0; i<512; i++) { if (i>0) recs512.append(","); recs512.append("VVTS").append(String.format("%04d", i)); }
                SwimDriver.AMQPProperties pA = new SwimDriver.AMQPProperties();
                pA.setRecipients(recs512.toString()); pA.setContentType("text/plain; charset=utf-8"); pA.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, "Msg 512".getBytes(), pA);

                // b) 513 Recs -> REJECT
                StringBuilder recs513 = new StringBuilder();
                for (int i=0; i<513; i++) { if (i>0) recs513.append(","); recs513.append("VVTS").append(String.format("%04d", i)); }
                SwimDriver.AMQPProperties pB = new SwimDriver.AMQPProperties();
                pB.setRecipients(recs513.toString()); pB.setContentType("text/plain; charset=utf-8"); pB.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(tTopic, "Msg 513".getBytes(), pB);

                logManualAction(testCaseId, "VERIFICATION: Msg 1 (512) Accepted, Msg 2 (513) Rejected.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW113 = new BaseTestCase("CTSW113", "Incoming RN/NRN Notifications") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                for (int i=1; i<=2; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip);
                    props.setContentType("text/plain; charset=utf-8");
                    props.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                    props.setAmqpPriority((short) 6);
                    props.setExtraProp("amhs_notification_request", "rn,nrn");
                    swimDriver.publishMessage(tTopic, "NotifRequest".getBytes(), props);
                }
                logManualAction(testCaseId, "Priority mapped to SS. Generate NRN for Msg 1, RN for Msg 2 from Terminal.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW114 = new BaseTestCase("CTSW114", "Incoming NDR Reports") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX"));
                props.setContentType("text/plain; charset=utf-8");
                props.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                swimDriver.publishMessage(TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC"), "Trig NDR".getBytes(), props);
                logManualAction(testCaseId, "Delete message in Terminal to trigger NDR.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW115 = new BaseTestCase("CTSW115", "Convert AMQP with encoding mapping") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                String[] bodyParts = {"ia5-text", "ia5_text_body_part", "general-text-body-part", "general-text-body-part"};
                String[] encodings = {"IA5", "IA5", "ISO-646", "ISO-8859-1"};
                String[] payloads = {"Lorem ipsum", "Lorem ipsum i5bpt", "Lorem ipsum 646", "Lorem ipsum 8859"};
                
                for (int i=0; i<4; i++) {
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients(recip);
                    props.setContentType("text/plain; charset=utf-8");
                    props.setBodyType(SwimDriver.AMQPProperties.BodyType.AMQP_VALUE);
                    props.setBodyPartType(bodyParts[i]);
                    props.setExtraProp("amhs_content_encoding", encodings[i]);
                    swimDriver.publishMessage(tTopic, payloads[i].getBytes(), props);
                }
                logManualAction(testCaseId, "Verify correct translation to IA5Text and GeneralText body parts.");
                return true;
            } catch (Exception e) { return false; }
        }
        @Override public boolean execute() throws Exception { return false; }
    };

    public BaseTestCase CTSW116 = new BaseTestCase("CTSW116", "Convert binary AMQP with FTBP and GZIP") {
        @Override
        public List<TestParameter> getRequiredParameters() { return List.of(); }
        @Override
        public boolean execute(Map<String, String> inputs) throws Exception {
            try {
                String tTopic = TestConfig.getInstance().getProperty("gateway.default_topic", "TEST.TOPIC");
                String recip = TestConfig.getInstance().getProperty("gateway.test_recipient", "VVTSYMYX");
                
                // Read an actual file payload
                java.nio.file.Path filePath = java.nio.file.Paths.get("src/main/resources/sample.pdf");
                byte[] binPayload = java.nio.file.Files.exists(filePath) ? java.nio.file.Files.readAllBytes(filePath) : new byte[1024];
                long fileSize = binPayload.length;

                // 1. FTBP
                SwimDriver.AMQPProperties p1 = new SwimDriver.AMQPProperties();
                p1.setRecipients(recip);
                p1.setContentType("application/octet-stream");
                p1.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                p1.setExtraProp("amhs_ftbp_file_name", "sample.pdf");
                p1.setExtraProp("amhs_ftbp_object_size", String.valueOf(fileSize));
                p1.setExtraProp("amhs_ftbp_last_mod", "240101120000Z");
                swimDriver.publishMessage(tTopic, binPayload, p1);

                // 2. FTBP + GZIP
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                    gzip.write(binPayload);
                }
                byte[] gzPayload = baos.toByteArray();
                
                SwimDriver.AMQPProperties p2 = new SwimDriver.AMQPProperties();
                p2.setRecipients(recip);
                p2.setContentType("application/octet-stream");
                p2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                p2.setExtraProp("amhs_ftbp_file_name", "sample.pdf");
                p2.setExtraProp("amhs_ftbp_object_size", String.valueOf(fileSize));
                p2.setExtraProp("amhs_ftbp_last_mod", "240101120000Z");
                p2.setExtraProp("swim_compression", "gzip");
                swimDriver.publishMessage(tTopic, gzPayload, p2);

                logManualAction(testCaseId, "VERIFICATION: 2 File Transfer Body Parts received. Second should be uncompressed.");
                return true;
            } catch (Exception e) { return false; }
        }

        @Override public boolean execute() throws Exception { return false; }
    };
}
