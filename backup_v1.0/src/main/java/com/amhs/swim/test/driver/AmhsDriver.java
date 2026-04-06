package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;

/**
 * Driver tương tác với phía AMHS thông qua Isode X.400 Gateway/Client API.
 * Lưu ý: Các phương thức này giả định tồn tại lớp JNI wrapper cho thư viện C của Isode.
 */
public class AmhsDriver {
    private long sessionHandle; // Handle cho session X.400 (JNI pointer)
    private boolean isConnected = false;

    /**
     * Khởi tạo kết nối tới MTA (Gateway) hoặc Message Store (Client).
     * Sử dụng API: X400mtOpen (Gateway) hoặc X400msOpen (Client).
     */
    public void connect() throws Exception {
        TestConfig config = TestConfig.getInstance();
        String host = config.getProperty("amhs.mta.host");
        String channel = config.getProperty("amhs.channel", "default");
        
        Logger.log("INFO", "Đang kết nối AMHS MTA tại: " + host);
        
        // Giả lập gọi JNI: X400mtOpen(channel, &session)
        // Trong thực tế cần load library Isode qua System.loadLibrary
        this.sessionHandle = nativeOpenSession(channel); 
        this.isConnected = true;
        Logger.log("SUCCESS", "Kết nối AMHS thành công.");
    }

    /**
     * Gửi một IPM (Interpersonal Message) tới AMHS.
     * Sử dụng API: X400mtMsgNew, X400mtMsgAddStrParam, X400mtMsgSend.
     * @param originator Địa chỉ người gửi (O/R Address).
     * @param recipient Địa chỉ người nhận.
     * @param subject Tiêu đề.
     * @param content Nội dung text (IA5Text).
     * @param priority Độ ưu tiên (SS, DD, FF, GG, KK).
     */
    public void sendMessage(String originator, String recipient, String subject, String content, String priority) throws Exception {
        if (!isConnected) connect();

        Logger.log("INFO", "Đang gửi message AMHS từ " + originator + " tới " + recipient);

        // 1. Tạo message object: X400mtMsgNew
        long msgHandle = nativeMsgNew();

        // 2. Thiết lập Originator: X400mtMsgAddStrParam(mp, X400_S_OR_ADDRESS, originator)
        nativeMsgAddParam(msgHandle, "OR_ADDRESS", originator);

        // 3. Thiết lập Recipient: X400mtRecipNew, X400mtRecipAddStrParam
        nativeMsgAddRecipient(msgHandle, recipient);

        // 4. Thiết lập Subject: X400_S_SUBJECT
        nativeMsgAddParam(msgHandle, "SUBJECT", subject);

        // 5. Thiết lập Priority: X400_N_PRIORITY
        nativeMsgAddParam(msgHandle, "PRIORITY", String.valueOf(mapPriority(priority)));

        // 6. Thiết lập Content (IA5Text): X400_T_IA5TEXT
        nativeMsgAddParam(msgHandle, "IA5TEXT", content);

        // 7. Gửi: X400mtMsgSend
        nativeMsgSend(msgHandle);

        Logger.log("SUCCESS", "Gửi message AMHS thành công.");
        
        // Dọn dẹp: X400mtMsgDelete
        nativeMsgDelete(msgHandle);
    }

    /**
     * Nhận message từ AMHS (dùng cho phía Client/UA để verify NDR/DR).
     * Sử dụng API: X400msMsgGetStart, X400msMsgGetStrParam.
     * @return Nội dung message nhận được.
     */
    public String receiveMessage() throws Exception {
        if (!isConnected) connect();
        
        Logger.log("INFO", "Đang chờ nhận message AMHS...");
        
        // X400msMsgGetStart
        long msgHandle = nativeClientMsgGet();
        
        // Lấy nội dung
        String content = nativeMsgGetParam(msgHandle, "IA5TEXT");
        
        // X400msMsgGetFinish (Acknowledge)
        nativeClientMsgFinish(msgHandle, 0); // 0 = Success
        
        Logger.log("SUCCESS", "Nhận message AMHS thành công.");
        return content;
    }

    public String receiveNDR() throws Exception {
        return receiveMessage();
    }

    public void sendRawMessage(String originator, String recipient, String subject, String content) throws Exception {
        sendMessage(originator, recipient, subject, content, "FF");
    }

    public void receiveMessageWithTimeout(int timeoutSeconds) throws java.util.concurrent.TimeoutException, Exception {
        // Giả lập Timeout khi không nhận được message trong vòng timeoutSeconds
        Thread.sleep(timeoutSeconds * 1000L);
        throw new java.util.concurrent.TimeoutException("Timeout waiting for message.");
    }

    public String receiveDR() throws Exception {
        return receiveMessage();
    }

    public void sendMessageWithMultipleRecipients(String originator, String[] recipients, String subject, String content, String priority) throws Exception {
        for (String rec : recipients) {
            sendMessage(originator, rec, subject, content, priority);
        }
    }

    public void sendMessageWithOptionalFields(String originator, String recipient, String subject, String content, String priority) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendSignedMessage(String originator, String recipient, String subject, String content, String priority) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendMessageWithExpiredDeliveryTime(String originator, String recipient, String subject, String content, String priority) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendMessageWithMultipleBodyParts(String originator, String recipient, String subject, String content, String priority) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendMessageWithUnsupportedContentType(String originator, String recipient, String subject, String content, String priority, String contentType) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendMessageWithEIT(String originator, String recipient, String subject, String content, String priority, String eit) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendMessageWithBodyPartType(String originator, String recipient, String subject, String content, String priority, String bodyPart) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public void sendProbe(String originator, String recipient, String subject) throws Exception {
        sendMessage(originator, recipient, subject, "PROBE", "FF");
    }

    public String receiveProbeResponse() throws Exception {
        return receiveMessage();
    }

    public void sendInvalidProbe(String originator, String recipient, String subject) throws Exception {
        sendMessage(originator, recipient, subject, "INVALID_PROBE", "FF");
    }

    public String receiveProbeNegativeResponse() throws Exception {
        return receiveMessage();
    }

    public void sendReceiptNotification(String originator, String recipient, String subject, String content) throws Exception {
        sendMessage(originator, recipient, subject, content, "FF");
    }

    public void sendMessageWithRNRequest(String originator, String recipient, String subject, String content, String priority) throws Exception {
        sendMessage(originator, recipient, subject, content, priority);
    }

    public String receiveRN() throws Exception {
        return receiveMessage();
    }

    // --- JNI Native Methods (Giả lập) ---
    private native long nativeOpenSession(String channel);
    private native long nativeMsgNew();
    private native void nativeMsgAddParam(long msg, String key, String value);
    private native void nativeMsgAddRecipient(long msg, String recip);
    private native void nativeMsgSend(long msg);
    private native void nativeMsgDelete(long msg);
    private native long nativeClientMsgGet();
    private native String nativeMsgGetParam(long msg, String key);
    private native void nativeClientMsgFinish(long msg, int status);

    private int mapPriority(String priority) {
        // Ánh xạ priority AMHS sang số nguyên X.400 (0: normal, 2: urgent)
        switch (priority) {
            case "SS": return 2; // Urgent
            case "DD": return 1; // Non-urgent
            default: return 0;   // Normal
        }
    }
}