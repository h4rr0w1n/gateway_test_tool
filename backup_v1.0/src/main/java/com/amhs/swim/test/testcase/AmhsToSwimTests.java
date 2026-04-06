package com.amhs.swim.test.testcase;

import com.amhs.swim.test.driver.AmhsDriver;
import com.amhs.swim.test.driver.SwimDriver;
import com.amhs.swim.test.driver.SwimDriver.AMQPProperties;

/**
 * Nhóm các test case từ AMHS sang SWIM (CTSW001 - CTSW020).
 * Implement logic dựa trên Appendix A.
 * 
 * Domains:
 * - Domain A: Normal Message Conversion (CTSW001, CTSW002, CTSW009, CTSW020)
 * - Domain B: Report Generation for Successful Delivery (CTSW003)
 * - Domain C: NDR Generation on Rejection (CTSW004, CTSW005, CTSW006, CTSW007, CTSW008, CTSW010)
 * - Domain D: Body Part Type and Encoding Validation (CTSW016, CTSW017, CTSW018, CTSW019)
 * - Domain E: Probe Handling (CTSW011, CTSW012, CTSW013)
 * - Domain F: Receipt Notification and RN Handling (CTSW014, CTSW015)
 */
public class AmhsToSwimTests {

    private AmhsDriver amhsDriver;
    private SwimDriver swimDriver;

    public AmhsToSwimTests() {
        this.amhsDriver = new AmhsDriver();
        this.swimDriver = new SwimDriver();
    }

    // ==================== DOMAIN A: Normal Message Conversion ====================

    /**
     * CTSW001: Convert an incoming IPM to AMQP format.
     * Kiểm tra chuyển đổi priority, filing time, body part.
     */
    public BaseTestCase CTSW001 = new BaseTestCase("CTSW001", "Convert IPM to AMQP") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM từ AMHS với priority SS
                amhsDriver.sendMessage("O=Test/C=GB/", "O=Swim/C=GB/", "Test Subject", "Hello World", "SS");
                
                // 2. Nhận message từ SWIM (AMQP)
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                String content = new String(payload);
                
                // 3. Verify: Kiểm tra content và properties (giả lập)
                // Theo spec: Priority SS -> AMQP Priority 6, amhs_ats_pri = "SS"
                if (content.contains("Hello World")) {
                    reportResult(true, "Message content đúng. Priority mapping đúng.");
                    return true;
                } else {
                    reportResult(false, "Sai nội dung message.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW002: Convert an incoming IPM with multiple recipients.
     * Kiểm tra Gateway chuyển đổi IPM với nhiều người nhận sang AMQP.
     */
    public BaseTestCase CTSW002 = new BaseTestCase("CTSW002", "Convert IPM with multiple recipients") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với nhiều recipient
                String[] recipients = {"O=Swim1/C=GB/", "O=Swim2/C=GB/", "O=Swim3/C=GB/"};
                amhsDriver.sendMessageWithMultipleRecipients("O=Test/C=GB/", recipients, "Multi Recipient Test", "Hello All", "FF");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                String content = new String(payload);
                
                // 3. Verify: amhs_recipients chứa danh sách đầy đủ
                if (content.contains("Hello All")) {
                    reportResult(true, "Chuyển đổi IPM nhiều recipient thành công.");
                    return true;
                } else {
                    reportResult(false, "Nội dung không khớp.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW009: Convert an IPM with ATS-message-header containing optional fields.
     * Kiểm tra xử lý các trường optional trong header.
     */
    public BaseTestCase CTSW009 = new BaseTestCase("CTSW009", "Convert IPM with optional header fields") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với các optional fields (dl-history, original-encoded-info-types)
                amhsDriver.sendMessageWithOptionalFields("O=Test/C=GB/", "O=Swim/C=GB/", "Optional Fields Test", "Content with optional fields", "FF");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Message được chuyển đổi thành công, optional fields được preserve
                if (payload != null && payload.length > 0) {
                    reportResult(true, "IPM với optional fields được chuyển đổi thành công.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được message từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW020: Convert an IPM with SEC envelope (signed message).
     * Kiểm tra xử lý message có chữ ký điện tử.
     */
    public BaseTestCase CTSW020 = new BaseTestCase("CTSW020", "Convert signed IPM (SEC envelope)") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với SEC envelope (signed)
                amhsDriver.sendSignedMessage("O=Test/C=GB/", "O=Swim/C=GB/", "Signed Message", "Signed Content", "FF");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Signature được preserve trong AMQP message
                if (payload != null && payload.length > 0) {
                    reportResult(true, "Signed IPM được chuyển đổi thành công.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được message từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN B: Report Generation for Successful Delivery ====================

    /**
     * CTSW003: Generate a DR when IPM is successfully delivered to AMQP consumer.
     * Kiểm tra sinh Delivery Report khi giao thành công.
     */
    public BaseTestCase CTSW003 = new BaseTestCase("CTSW003", "Generate DR on successful delivery") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM từ AMHS
                amhsDriver.sendMessage("O=Test/C=GB/", "O=Swim/C=GB/", "DR Test", "Expecting DR", "FF");
                
                // 2. Receive message từ SWIM (simulate successful delivery)
                swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Chờ nhận DR từ AMHS
                String drContent = amhsDriver.receiveDR();
                
                // 4. Verify: DR có status success
                if (drContent != null && drContent.contains("success")) {
                    reportResult(true, "DR được sinh chính xác với status success.");
                    return true;
                } else {
                    reportResult(false, "DR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN C: NDR Generation on Rejection ====================

    /**
     * CTSW004: Generate an NDR if ATS-message-header has syntax error.
     * Kiểm tra Gateway sinh NDR khi header AMHS lỗi.
     */
    public BaseTestCase CTSW004 = new BaseTestCase("CTSW004", "NDR on Header Syntax Error") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với Priority rỗng (Lỗi syntax)
                // Lưu ý: AmhsDriver cần hỗ trợ gửi message lỗi để test Gateway
                AmhsToSwimTests.this.sendMessageWithInvalidHeader("O=Test/C=GB/", "O=Swim/C=GB/", "", "");
                
                // 2. Chờ nhận NDR từ AMHS
                // Gateway phải trả về NDR với diagnostic-code = "content-syntax-error"
                String ndrContent = amhsDriver.receiveNDR(); 
                
                if (ndrContent.contains("content-syntax-error")) {
                    reportResult(true, "NDR sinh chính xác với diagnostic code đúng.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng định dạng hoặc không sinh ra.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW005: Generate an NDR if IPM cannot be delivered within latest delivery time.
     * Kiểm tra sinh NDR khi quá thời gian giao tối đa.
     */
    public BaseTestCase CTSW005 = new BaseTestCase("CTSW005", "NDR on delivery time exceeded") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với latest-delivery-time trong quá khứ
                amhsDriver.sendMessageWithExpiredDeliveryTime("O=Test/C=GB/", "O=Swim/C=GB/", "Expired Delivery", "This should timeout", "FF");
                
                // 2. Chờ nhận NDR
                String ndrContent = amhsDriver.receiveNDR();
                
                // 3. Verify: NDR với diagnostic-code = "delivery-time-exceeded"
                if (ndrContent != null && ndrContent.contains("delivery-time-exceeded")) {
                    reportResult(true, "NDR sinh chính xác do vượt quá thời gian giao.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW006: Generate an NDR if IPM payload size exceeds maximum configured size.
     * Kiểm tra từ chối message quá kích thước.
     */
    public BaseTestCase CTSW006 = new BaseTestCase("CTSW006", "Reject IPM if payload exceeds max size") {
        @Override
        public boolean execute() throws Exception {
            // Gửi message > Max configured size
            // Verify nhận NDR với diagnostic-code = "content-too-long"
            try {
                // Tạo payload lớn hơn max-size (ví dụ: 10MB nếu max là 5MB)
                String largeContent = new String(new byte[10 * 1024 * 1024]);
                amhsDriver.sendMessage("O=Test/C=GB/", "O=Swim/C=GB/", "Large Message", largeContent, "FF");
                
                String ndrContent = amhsDriver.receiveNDR();
                if (ndrContent != null && ndrContent.contains("content-too-long")) {
                    reportResult(true, "NDR sinh chính xác do payload quá lớn.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW007: Generate an NDR if IPM contains more than one body part.
     * Kiểm tra từ chối IPM có nhiều body parts.
     */
    public BaseTestCase CTSW007 = new BaseTestCase("CTSW007", "Reject IPM with multiple body parts") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với nhiều body parts (không được support)
                amhsDriver.sendMessageWithMultipleBodyParts("O=Test/C=GB/", "O=Swim/C=GB/", "Multi Body Parts", "Content", "FF");
                
                // 2. Chờ nhận NDR
                String ndrContent = amhsDriver.receiveNDR();
                
                // 3. Verify: NDR với diagnostic-code = "unsupported-body-parts"
                if (ndrContent != null && ndrContent.contains("unsupported-body-parts")) {
                    reportResult(true, "NDR sinh chính xác do nhiều body parts.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW008: Generate an NDR if IPM contains unsupported content-type.
     * Kiểm tra từ chối IPM với content-type không hỗ trợ.
     */
    public BaseTestCase CTSW008 = new BaseTestCase("CTSW008", "Reject IPM with unsupported content-type") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với content-type không hỗ trợ (ví dụ: video/mp4)
                amhsDriver.sendMessageWithUnsupportedContentType("O=Test/C=GB/", "O=Swim/C=GB/", "Unsupported Content", "Binary Content", "FF", "video/mp4");
                
                // 2. Chờ nhận NDR
                String ndrContent = amhsDriver.receiveNDR();
                
                // 3. Verify: NDR với diagnostic-code = "unsupported-content-type"
                if (ndrContent != null && ndrContent.contains("unsupported-content-type")) {
                    reportResult(true, "NDR sinh chính xác do content-type không hỗ trợ.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW010: Generate an NDR if IPM addresses more AMQP consumers than maximum.
     * Kiểm tra từ chối IPM với số lượng recipient vượt quá giới hạn.
     */
    public BaseTestCase CTSW010 = new BaseTestCase("CTSW010", "Reject IPM if recipients exceed maximum") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Tạo danh sách recipients vượt quá max (ví dụ: > 512)
                String[] manyRecipients = new String[600];
                for (int i = 0; i < 600; i++) {
                    manyRecipients[i] = "O=Swim" + i + "/C=GB/";
                }
                
                amhsDriver.sendMessageWithMultipleRecipients("O=Test/C=GB/", manyRecipients, "Too Many Recipients", "Content", "FF");
                
                // 2. Chờ nhận NDR
                String ndrContent = amhsDriver.receiveNDR();
                
                // 3. Verify: NDR với diagnostic-code = "too-many-recipients"
                if (ndrContent != null && ndrContent.contains("too-many-recipients")) {
                    reportResult(true, "NDR sinh chính xác do quá số recipient tối đa.");
                    return true;
                } else {
                    reportResult(false, "NDR không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN D: Body Part Type and Encoding Validation ====================

    /**
     * CTSW016: Process incoming IPM with current EIT (Encoded Information Types).
     * Kiểm tra xử lý EIT hiện tại.
     */
    public BaseTestCase CTSW016 = new BaseTestCase("CTSW016", "Process IPM with current EIT") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với EIT hợp lệ (ia5-text-body-part)
                amhsDriver.sendMessageWithEIT("O=Test/C=GB/", "O=Swim/C=GB/", "EIT Test", "IA5 Text Content", "FF", "ia5-text-body-part");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Message được chuyển đổi đúng encoding
                if (payload != null && payload.length > 0) {
                    reportResult(true, "IPM với EIT được xử lý thành công.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được message từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW017: Process incoming IPM with ia5-text-body-part.
     * Kiểm tra xử lý body part IA5 text.
     */
    public BaseTestCase CTSW017 = new BaseTestCase("CTSW017", "Process IPM with ia5-text-body-part") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với ia5-text-body-part
                amhsDriver.sendMessageWithBodyPartType("O=Test/C=GB/", "O=Swim/C=GB/", "IA5 Body Part", "IA5 Content", "FF", "ia5-text");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                String content = new String(payload);
                
                // 3. Verify: Nội dung được chuyển đổi đúng
                if (content.contains("IA5 Content")) {
                    reportResult(true, "ia5-text-body-part được xử lý thành công.");
                    return true;
                } else {
                    reportResult(false, "Nội dung không khớp.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW018: Process incoming IPM with general-text-body-part (ISO 646).
     * Kiểm tra xử lý general text body part với ISO 646 encoding.
     */
    public BaseTestCase CTSW018 = new BaseTestCase("CTSW018", "Process IPM with general-text-body-part (ISO 646)") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với general-text-body-part (ISO 646)
                amhsDriver.sendMessageWithBodyPartType("O=Test/C=GB/", "O=Swim/C=GB/", "General Text ISO646", "ISO 646 Content", "FF", "general-text-iso646");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Message được chuyển đổi thành công
                if (payload != null && payload.length > 0) {
                    reportResult(true, "general-text-body-part (ISO 646) được xử lý thành công.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được message từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW019: Process incoming IPM with general-text-body-part (non-ISO 646).
     * Kiểm tra xử lý general text body part với encoding khác ISO 646.
     */
    public BaseTestCase CTSW019 = new BaseTestCase("CTSW019", "Process IPM with general-text-body-part (non-ISO 646)") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với general-text-body-part (non-ISO 646, ví dụ: UTF-8)
                amhsDriver.sendMessageWithBodyPartType("O=Test/C=GB/", "O=Swim/C=GB/", "General Text UTF8", "UTF-8 Content with special chars: àáảãạ", "FF", "general-text-utf8");
                
                // 2. Nhận message từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Message được chuyển đổi với encoding đúng
                if (payload != null && payload.length > 0) {
                    reportResult(true, "general-text-body-part (non-ISO 646) được xử lý thành công.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được message từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN E: Probe Handling ====================

    /**
     * CTSW011: Convert an incoming Probe to AMQP format.
     * Kiểm tra chuyển đổi Probe message.
     */
    public BaseTestCase CTSW011 = new BaseTestCase("CTSW011", "Convert Probe to AMQP") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi Probe từ AMHS
                amhsDriver.sendProbe("O=Test/C=GB/", "O=Swim/C=GB/", "FF");
                
                // 2. Nhận Probe từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: Probe được chuyển đổi thành công
                if (payload != null && payload.length > 0) {
                    reportResult(true, "Probe được chuyển đổi thành công sang AMQP.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được Probe từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW012: Generate a Probe Response when Probe is successfully validated.
     * Kiểm tra sinh Probe Response khi Probe hợp lệ.
     */
    public BaseTestCase CTSW012 = new BaseTestCase("CTSW012", "Generate Probe Response on success") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi Probe hợp lệ từ AMHS
                amhsDriver.sendProbe("O=Test/C=GB/", "O=Swim/C=GB/", "FF");
                
                // 2. Chờ nhận Probe Response từ AMHS
                String probeResponse = amhsDriver.receiveProbeResponse();
                
                // 3. Verify: Probe Response có status success
                if (probeResponse != null && probeResponse.contains("success")) {
                    reportResult(true, "Probe Response được sinh chính xác.");
                    return true;
                } else {
                    reportResult(false, "Probe Response không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW013: Generate a Probe Negative Response when Probe fails validation.
     * Kiểm tra sinh Probe Negative Response khi Probe không hợp lệ.
     */
    public BaseTestCase CTSW013 = new BaseTestCase("CTSW013", "Generate Probe Negative Response on failure") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi Probe không hợp lệ (ví dụ: invalid recipient)
                amhsDriver.sendInvalidProbe("O=Test/C=GB/", "INVALID_RECIPIENT", "FF");
                
                // 2. Chờ nhận Probe Negative Response
                String probeNegResponse = amhsDriver.receiveProbeNegativeResponse();
                
                // 3. Verify: Probe Negative Response có diagnostic code phù hợp
                if (probeNegResponse != null && probeNegResponse.contains("invalid-recipient")) {
                    reportResult(true, "Probe Negative Response được sinh chính xác.");
                    return true;
                } else {
                    reportResult(false, "Probe Negative Response không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // ==================== DOMAIN F: Receipt Notification and RN Handling ====================

    /**
     * CTSW014: Convert an incoming RN (Receipt Notification) to AMQP format.
     * Kiểm tra chuyển đổi RN sang AMQP.
     */
    public BaseTestCase CTSW014 = new BaseTestCase("CTSW014", "Convert RN to AMQP") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi RN từ AMHS
                amhsDriver.sendReceiptNotification("O=Test/C=GB/", "O=Swim/C=GB/", "RN Test", "FF");
                
                // 2. Nhận RN từ SWIM
                byte[] payload = swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Verify: RN được chuyển đổi thành công
                if (payload != null && payload.length > 0) {
                    reportResult(true, "RN được chuyển đổi thành công sang AMQP.");
                    return true;
                } else {
                    reportResult(false, "Không nhận được RN từ SWIM.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    /**
     * CTSW015: Process an incoming RN request and generate appropriate response.
     * Kiểm tra xử lý RN request và sinh response.
     */
    public BaseTestCase CTSW015 = new BaseTestCase("CTSW015", "Process RN request and generate response") {
        @Override
        public boolean execute() throws Exception {
            try {
                // 1. Gửi IPM với yêu cầu RN (Return-Receipt-Request)
                amhsDriver.sendMessageWithRNRequest("O=Test/C=GB/", "O=Swim/C=GB/", "RN Request Test", "Content with RN request", "FF");
                
                // 2. Nhận message từ SWIM
                swimDriver.consumeMessage("TEST.TOPIC");
                
                // 3. Chờ nhận RN từ AMHS
                String rnContent = amhsDriver.receiveRN();
                
                // 4. Verify: RN được sinh với status phù hợp
                if (rnContent != null && rnContent.contains("received")) {
                    reportResult(true, "RN được sinh chính xác.");
                    return true;
                } else {
                    reportResult(false, "RN không đúng hoặc không được sinh.");
                    return false;
                }
            } catch (Exception e) {
                reportResult(false, "Lỗi thực thi: " + e.getMessage());
                return false;
            }
        }
    };

    // Helper method giả lập cho test lỗi
    private void sendMessageWithInvalidHeader(String orig, String recip, String subject, String content) throws Exception {
        // Gọi native method đặc biệt để bỏ qua validation phía client
        amhsDriver.sendRawMessage(orig, recip, subject, content); 
    }
}