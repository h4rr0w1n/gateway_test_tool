package com.amhs.swim.test.util;

import com.amhs.swim.test.config.TestConfig;

/**
 * Tiện ích xác thực các ràng buộc của Gateway.
 * Tuân thủ EUR Doc 047 v3.0, Chương 3.3 (Message Constraints).
 */
public class Validator {

    /**
     * Kiểm tra kích thước payload có vượt quá cấu hình không.
     * Tham khảo: Spec v3.0 Section 3.3.1.
     * @param payloadSize Kích thước payload tính bằng bytes.
     * @return true nếu hợp lệ, false nếu vượt quá giới hạn.
     */
    public static boolean validatePayloadSize(long payloadSize) {
        TestConfig config = TestConfig.getInstance();
        int maxSize = config.getIntProperty("gateway.max.message.size", 0);

        // Nếu maxSize = 0 hoặc không tồn tại, không giới hạn
        if (maxSize == 0) {
            return true;
        }

        if (payloadSize > maxSize) {
            Logger.log("ERROR", "Payload size (" + payloadSize + ") exceeds maximum configured (" + maxSize + ")");
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra số lượng người nhận có vượt quá cấu hình không.
     * Tham khảo: Spec v3.0 Section 3.3.2.
     * @param recipientCount Số lượng người nhận.
     * @return true nếu hợp lệ, false nếu vượt quá giới hạn.
     */
    public static boolean validateRecipientCount(int recipientCount) {
        TestConfig config = TestConfig.getInstance();
        int maxRecipients = config.getIntProperty("gateway.max.recipients", 0);

        // Nếu maxRecipients = 0 hoặc không tồn tại, không giới hạn
        if (maxRecipients == 0) {
            return true;
        }

        if (recipientCount > maxRecipients) {
            Logger.log("ERROR", "Recipient count (" + recipientCount + ") exceeds maximum configured (" + maxRecipients + ")");
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra định dạng địa chỉ AFTN (8 ký tự chữ hoa).
     * @param address Địa chỉ cần kiểm tra.
     * @return true nếu hợp lệ.
     */
    public static boolean validateAftnAddress(String address) {
        if (address == null || address.length() != 8) {
            return false;
        }
        return address.matches("[A-Z0-9]{8}");
    }

    /**
     * Kiểm tra các trường bắt buộc của AMQP (AMHS Unaware).
     * Tham khảo: Appendix A, CTSW102.
     * @param properties Các property của message AMQP.
     * @return true nếu đầy đủ trường bắt buộc.
     */
    public static boolean validateAmqpMinimumFields(AMQPProperties properties) {
        // Các trường bắt buộc theo CTSW102: priority, message-id, creation-time, data/amqp-value, amhs_recipients, content-type
        if (properties.getPriority() == null) {
            Logger.log("ERROR", "Missing mandatory field: priority");
            return false;
        }
        if (properties.getMessageId() == null || properties.getMessageId().isEmpty()) {
            Logger.log("ERROR", "Missing mandatory field: message-id");
            return false;
        }
        if (properties.getCreationTime() == null) {
            Logger.log("ERROR", "Missing mandatory field: creation-time");
            return false;
        }
        if (properties.getRecipients() == null || properties.getRecipients().isEmpty()) {
            Logger.log("ERROR", "Missing mandatory field: amhs_recipients");
            return false;
        }
        if (properties.getContentType() == null || properties.getContentType().isEmpty()) {
            Logger.log("ERROR", "Missing mandatory field: content-type");
            return false;
        }
        // Kiểm tra data hoặc amqp-value phải có một trong hai
        if (properties.getData() == null && properties.getAmqpValue() == null) {
            Logger.log("ERROR", "Missing mandatory field: data or amqp-value");
            return false;
        }
        return true;
    }

    // Lớp nội bộ để chứa properties AMQP phục vụ validation
    public static class AMQPProperties {
        private Integer priority;
        private String messageId;
        private Long creationTime;
        private byte[] data;
        private String amqpValue;
        private String recipients;
        private String contentType;

        // Getters and Setters
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public Long getCreationTime() { return creationTime; }
        public void setCreationTime(Long creationTime) { this.creationTime = creationTime; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public String getAmqpValue() { return amqpValue; }
        public void setAmqpValue(String amqpValue) { this.amqpValue = amqpValue; }
        public String getRecipients() { return recipients; }
        public void setRecipients(String recipients) { this.recipients = recipients; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }
}