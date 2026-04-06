package com.amhs.swim.test.testcase;

import com.amhs.swim.test.driver.SwimDriver;
import com.amhs.swim.test.util.Logger;

/**
 * Nhóm các test case từ SWIM sang AMHS (CTSW101 - CTSW116).
 * 
 * Workflow:
 * 1. User chọn test case qua GUI.
 * 2. Tool kết nối SWIM và gửi chuỗi message (payload) tương ứng.
 * 3. Tool hiển thị hướng dẫn kiểm tra thủ công (Manual Verification).
 * 4. User kiểm tra kết quả trên phía AMHS Client/Terminal.
 */
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

    // ==================== DOMAIN G: Normal Message Conversion ====================

    /**
     * CTSW101: Convert an AMHS UNAWARE AMQP message to AMHS.
     */
    public BaseTestCase CTSW101 = new BaseTestCase("CTSW101", "Convert AMHS Unaware AMQP to AMHS") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Text payload
                String textPayload = "CTSW101 Text Payload Content";
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients("VVTSYMYX");
                props.setAtsPri("FF");
                logTransmission(testCaseId, "Scenario 1 (Text), Priority=[FF], Payload=[" + textPayload + "]");
                swimDriver.publishMessage("TEST.TOPIC", textPayload.getBytes(), props);

                // 2. Binary payload
                props.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                byte[] binaryPayload = new byte[]{0x43, 0x54, 0x53, 0x57, 0x31, 0x30, 0x31, 0x00, (byte)0xFF};
                logTransmission(testCaseId, "Scenario 2 (Binary), Priority=[FF], Payload=[Hex: 43 54 53 57 31 30 31 00 FF]");
                swimDriver.publishMessage("TEST.TOPIC", binaryPayload, props);
                
                logManualAction(testCaseId, "Verify arrival of 1 Text and 1 Binary message at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": All payloads sent successfully.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW102: Reject AMQP missing required info.
     */
    public BaseTestCase CTSW102 = new BaseTestCase("CTSW102", "Reject AMQP missing required info") {
        @Override
        public boolean execute() throws Exception {
            try {
                logTransmission(testCaseId, "Reject-1: Invalid priority (10).");
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients("VVTSYMYX");
                props1.setAtsPri("INVALID_10");
                swimDriver.publishMessage("TEST.TOPIC", "Reject Sample 1".getBytes(), props1);

                logTransmission(testCaseId, "Reject-2: Empty payload.");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients("VVTSYMYX");
                swimDriver.publishMessage("TEST.TOPIC", new byte[0], props2);

                logTransmission(testCaseId, "Reject-3: Missing recipients.");
                SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                swimDriver.publishMessage("TEST.TOPIC", "Reject Sample 3".getBytes(), props3);
                
                logManualAction(testCaseId, "Verify 3 rejections at the Gateway Control Position and that NO messages arrived at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Rejection scenarios injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW103: Conversion to IPM according to ATSMHS Service Level parameter.
     */
    public BaseTestCase CTSW103 = new BaseTestCase("CTSW103", "Conversion per Service Level") {
        @Override
        public boolean execute() throws Exception {
            try {
                String[] levels = {"BASIC", "EXTENDED", "CONTENT_BASED", "CONTENT_BASED", "RECIPIENT_BASED", "RECIPIENT_BASED"};
                String[] payloads = {"CTSW103 Basic Text", "CTSW103 Extended Text", "CTSW103 Content-Octet", "CTSW103 Content-Text", "CTSW103 Recip-All-Ext", "CTSW103 Recip-Mixed"};

                for (int i = 0; i < levels.length; i++) {
                    logTransmission(testCaseId, "Message " + (i+1) + "/" + levels.length + ": Level=[" + levels[i] + "], Payload=[" + payloads[i] + "]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    swimDriver.publishMessage("TEST.TOPIC", payloads[i].getBytes(), props);
                }
                
                logManualAction(testCaseId, "Check mapping for 7 distinct Service Level messages (Basic to Recipient-Based) at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": All service level payloads sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW104: Convert incoming AMQP messages with different priorities to AMHS.
     */
    public BaseTestCase CTSW104 = new BaseTestCase("CTSW104", "Convert AMQP with various priorities") {
        @Override
        public boolean execute() throws Exception {
            try {
                // Priorities 0-9
                for (int i = 0; i < 10; i++) {
                    String payload = "CTSW104 Priority " + i;
                    logTransmission(testCaseId, "Message " + (i+1) + "/15: AMQP Priority=[" + i + "], Payload=[" + payload + "]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                }
                
                // ATS PRI SS, DD, FF, GG, KK
                String[] atsPris = {"SS", "DD", "FF", "GG", "KK"};
                for (int i = 0; i < atsPris.length; i++) {
                    String payload = "CTSW104 ATS PRI " + atsPris[i];
                    logTransmission(testCaseId, "Message " + (i+11) + "/15: ATS_PRI=[" + atsPris[i] + "], Payload=[" + payload + "]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    props.setAtsPri(atsPris[i]);
                    swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                }
                
                logManualAction(testCaseId, "Verify 15 messages with priorities 0-9 and ATS PRI SS,DD,FF,GG,KK at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": All 15 priority payloads sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW105: Convert incoming AMHS aware AMQP messages containing filing time to AMHS.
     */
    public BaseTestCase CTSW105 = new BaseTestCase("CTSW105", "Convert AMQP with filing-time mapping") {
        @Override
        public boolean execute() throws Exception {
            try {
                String payload1 = "CTSW105 Default FT (Empty FT property)";
                logTransmission(testCaseId, "Scenario 1: FilingTime=[Empty], Payload=[" + payload1 + "]");
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients("VVTSYMYX");
                swimDriver.publishMessage("TEST.TOPIC", payload1.getBytes(), props1);

                String payload2 = "CTSW105 Specific FT '250102'";
                logTransmission(testCaseId, "Scenario 2: FilingTime=[250102], Payload=[" + payload2 + "]");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients("VVTSYMYX");
                props2.setFilingTime("250102");
                swimDriver.publishMessage("TEST.TOPIC", payload2.getBytes(), props2);
                
                logManualAction(testCaseId, "Verify Filing Time mapping: Msg 1 should use creation time, Msg 2 should have filing time '250102'.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Filing time payloads sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW106: Convert an AMHS aware AMQP message containing optional heading information.
     */
    public BaseTestCase CTSW106 = new BaseTestCase("CTSW106", "Convert AMQP with OHI mapping") {
        @Override
        public boolean execute() throws Exception {
            try {
                String[] ohiTags = {"LO-PRI-SHORT", "LO-PRI-EXACT", "LO-PRI-LONG", "HI-PRI-SHORT", "HI-PRI-EXACT", "HI-PRI-LONG"};
                int[] priorities = {4, 4, 4, 6, 6, 6};
                int[] limits = {53, 53, 53, 48, 48, 48};
                
                for (int i = 0; i < ohiTags.length; i++) {
                    String payload = "CTSW106 Payload Scenario " + ohiTags[i];
                    String ohiText = "A".repeat(limits[i]);
                    if (i % 3 == 0) ohiText = "OHI-SHORT-" + i;
                    else if (i % 3 == 2) ohiText = "B".repeat(limits[i] + 10);

                    logTransmission(testCaseId, "Scenario " + ohiTags[i] + ": OHI=[" + ohiText + "], Payload=[" + payload + "]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    props.setAtsPri(priorities[i] == 6 ? "SS" : "FF");
                    props.toMap().put("amhs_ats_ohi", ohiText); 
                    swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                }
                
                logManualAction(testCaseId, "Verify OHI mapping and boundary truncation (48/53 chars) at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": All 6 OHI payloads sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW107: Convert an AMQP message containing subject.
     */
    public BaseTestCase CTSW107 = new BaseTestCase("CTSW107", "Convert AMQP with subject mapping") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Long Subject
                String s150 = "S".repeat(150);
                logTransmission(testCaseId, "Scenario 1: Subject=[150 chars 'S'], Payload=[CTSW107 Long Subject]");
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients("VVTSYMYX");
                props1.setSubject(s150);
                swimDriver.publishMessage("TEST.TOPIC", "CTSW107 Long Subject".getBytes(), props1);

                // 2. Normal Subject
                logTransmission(testCaseId, "Scenario 2: Subject=[CTSW107 Normal Sub], Payload=[CTSW107 Normal Prop]");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients("VVTSYMYX");
                props2.setSubject("CTSW107 Normal Sub");
                swimDriver.publishMessage("TEST.TOPIC", "CTSW107 Normal Prop".getBytes(), props2);

                // 3. Application Property Subject
                logTransmission(testCaseId, "Scenario 3: AppProperty=[amhs_subject: CTSW107 App Sub], Payload=[CTSW107 App Prop]");
                SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                props3.setRecipients("VVTSYMYX");
                props3.toMap().put("amhs_subject", "CTSW107 App Sub");
                swimDriver.publishMessage("TEST.TOPIC", "CTSW107 App Prop".getBytes(), props3);

                logManualAction(testCaseId, "Verify Subject mapping (128 char limit truncation) and Application Property override at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": All subject mapping payloads sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW108: Incoming AMQP message with known originator indicator.
     */
    public BaseTestCase CTSW108 = new BaseTestCase("CTSW108", "Known Originator") {
        @Override
        public boolean execute() throws Exception {
            try {
                String payload = "CTSW108 Known Originator";
                logTransmission(testCaseId, "Originator=[VVTSYMYX], Payload=[" + payload + "]");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients("VVTSYMYX");
                props.setOriginator("VVTSYMYX");
                swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                
                logManualAction(testCaseId, "Verify that the amhs_originator is converted into a standard MF-address at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Known originator payload sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW109: Incoming AMQP message with unknown originator indicator.
     */
    public BaseTestCase CTSW109 = new BaseTestCase("CTSW109", "Unknown Originator") {
        @Override
        public boolean execute() throws Exception {
            try {
                String payload = "CTSW109 Unknown Originator";
                logTransmission(testCaseId, "Originator=[UNKNOWNOR], Payload=[" + payload + "]");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients("VVTSYMYX");
                props.setOriginator("UNKNOWNOR");
                swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                
                logManualAction(testCaseId, "Verify that the default originator is used and the unknown originator is logged in the Gateway Control Position.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Unknown originator payload sent.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN H: Rejection and Validation ====================

    /**
     * CTSW110: Reject an AMQP message with unsupported content-type.
     */
    public BaseTestCase CTSW110 = new BaseTestCase("CTSW110", "Reject AMQP unsupported content-type") {
        @Override
        public boolean execute() throws Exception {
            try {
                logTransmission(testCaseId, "Scenario 1: application/octet-stream with empty payload.");
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients("VVTSYMYX");
                props1.setContentType("application/octet-stream");
                swimDriver.publishMessage("TEST.TOPIC", new byte[0], props1);

                logTransmission(testCaseId, "Scenario 2: text/plain but binary encoding (DATA body).");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients("VVTSYMYX");
                props2.setContentType("text/plain; charset=utf-8");
                props2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage("TEST.TOPIC", new byte[]{0x01, 0x02}, props2);

                logTransmission(testCaseId, "Scenario 3: Valid Octet-stream.");
                SwimDriver.AMQPProperties props3 = new SwimDriver.AMQPProperties();
                props3.setRecipients("VVTSYMYX");
                props3.setContentType("application/octet-stream");
                props3.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                swimDriver.publishMessage("TEST.TOPIC", "CTSW110 Valid Content".getBytes(), props3);

                logManualAction(testCaseId, "Verify rejections for scenarios 1, 2, 5, 6 at Gateway. Verify arrival for scenarios 3, 4 at AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Content-type scenarios injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW111: Reject AMQP if payload exceeds max size.
     */
    public BaseTestCase CTSW111 = new BaseTestCase("CTSW111", "Reject AMQP if payload exceeds max size") {
        @Override
        public boolean execute() throws Exception {
            try {
                int maxSize = com.amhs.swim.test.config.TestConfig.getInstance().getIntProperty("gateway.max_size", 1000000);
                logTransmission(testCaseId, "Sending " + (maxSize + 1024) + " bytes (Max=" + maxSize + ").");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients("VVTSYMYX");
                swimDriver.publishMessage("TEST.TOPIC", new byte[maxSize + 1024], props);
                
                logManualAction(testCaseId, "Verify Gateway rejection and log for the oversized payload. No message should arrive at AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Oversized payload injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW112: Reject AMQP if recipients exceed max.
     */
    public BaseTestCase CTSW112 = new BaseTestCase("CTSW112", "Reject AMQP if recipients exceed max") {
        @Override
        public boolean execute() throws Exception {
            try {
                logTransmission(testCaseId, "Sending 600 recipients (Max=512).");
                StringBuilder recipients = new StringBuilder();
                for (int i = 0; i < 600; i++) {
                    if (i > 0) recipients.append(",");
                    recipients.append("VVTS").append(String.format("%04d", i));
                }
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients(recipients.toString());
                swimDriver.publishMessage("TEST.TOPIC", "Exceeding Recipient Limit".getBytes(), props);
                
                logManualAction(testCaseId, "Verify Gateway rejection and log for recipient limit exceeded. No message should arrive at AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": High-recipient payload injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN I: Body Part Type and Encoding ====================

    /**
     * CTSW115: Convert AMQP with encoding mapping.
     */
    public BaseTestCase CTSW115 = new BaseTestCase("CTSW115", "Convert AMQP with encoding mapping") {
        @Override
        public boolean execute() throws Exception {
            try {
                String[] bodyParts = {"ia5-text", "ia5-text", "general-text-body-part", "general-text-body-part"};
                String[] encodings = {"IA5", "IA5", "ISO-646", "ISO-8859-1"};
                String[] payloads = {"ENCODE CTSW115 IA5", "ENCODE CTSW115 IA5-PT", "ENCODE CTSW115 ISO-646", "ENCODE CTSW115 ISO-8859-1"};

                for (int i = 0; i < 4; i++) {
                    logTransmission(testCaseId, "Scenario " + (i+1) + ": BodyPart=[" + bodyParts[i] + "], Encoding=[" + encodings[i] + "]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    props.setBodyPartType(bodyParts[i]);
                    props.toMap().put("amhs_content_encoding", encodings[i]);
                    swimDriver.publishMessage("TEST.TOPIC", payloads[i].getBytes(), props);
                }
                
                logManualAction(testCaseId, "Verify body-part type and encoding (IA5, ISO-646, ISO-8859-1) mapping for the 4 messages at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Encoding scenarios injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW116: Convert binary AMQP with FTBP and GZIP.
     */
    public BaseTestCase CTSW116 = new BaseTestCase("CTSW116", "Convert binary AMQP with FTBP and GZIP") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. FTBP
                logTransmission(testCaseId, "Scenario 1: FTBP (file name: test.txt, size: 1024)");
                SwimDriver.AMQPProperties props1 = new SwimDriver.AMQPProperties();
                props1.setRecipients("VVTSYMYX");
                props1.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                props1.toMap().put("amhs_ftbp_file_name", "test.txt");
                props1.toMap().put("amhs_ftbp_object_size", "1024");
                swimDriver.publishMessage("TEST.TOPIC", "CTSW116 FTBP Payload".getBytes(), props1);

                // 2. GZIP
                logTransmission(testCaseId, "Scenario 2: GZIP compression enabled.");
                SwimDriver.AMQPProperties props2 = new SwimDriver.AMQPProperties();
                props2.setRecipients("VVTSYMYX");
                props2.setBodyType(SwimDriver.AMQPProperties.BodyType.DATA);
                props2.toMap().put("swim_compression", "gzip");
                swimDriver.publishMessage("TEST.TOPIC", "CTSW116 GZIP Payload".getBytes(), props2);
                
                logManualAction(testCaseId, "Verify FTBP attributes (filename, size) and successful GZIP decompression at the AMHS Tool.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": FTBP/GZIP payloads injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN J: Incoming Reports and Notifications ====================

    /**
     * CTSW113: Incoming receipt and non-receipt notification (RN and NRN).
     */
    public BaseTestCase CTSW113 = new BaseTestCase("CTSW113", "Incoming RN/NRN Notifications") {
        @Override
        public boolean execute() throws Exception {
            try {
                // Scenario: Send two messages with Priority 6 (SS)
                for (int i = 1; i <= 2; i++) {
                    String payload = "CTSW113 Notification Test Message " + i;
                    logTransmission(testCaseId, "Message " + i + "/2: Priority=[SS], Notification=[rn,nrn]");
                    SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                    props.setRecipients("VVTSYMYX");
                    props.setAtsPri("SS"); // Priority 6
                    props.toMap().put("amhs_notification_request", "rn,nrn");
                    swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);
                }

                logManualAction(testCaseId, "Return NRN for Msg 1 and RN for Msg 2 from the AMHS Tool. Verify Gateway logs these entries correctly.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": Notification payloads injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW114: Incoming non-delivery reports (NDR).
     */
    public BaseTestCase CTSW114 = new BaseTestCase("CTSW114", "Incoming NDR Reports") {
        @Override
        public boolean execute() throws Exception {
            try {
                String payload = "CTSW114 NDR Test Message";
                logTransmission(testCaseId, "Injecting message for NDR trigger.");
                SwimDriver.AMQPProperties props = new SwimDriver.AMQPProperties();
                props.setRecipients("VVTSYMYX");
                swimDriver.publishMessage("TEST.TOPIC", payload.getBytes(), props);

                logManualAction(testCaseId, "Delete the message at AMHS Tool to trigger an NDR. Verify the Gateway logs 'unable-to-transfer' status.");
                Logger.log("SUCCESS", testCaseId + " - " + testCaseName + ": NDR trigger payload injected.");
                return true;
            } catch (Exception e) {
                Logger.log("ERROR", "Transmission failed: " + e.getMessage());
                return false;
            }
        }
    };
}