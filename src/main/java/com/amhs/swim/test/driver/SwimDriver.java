package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver tương tác với phía SWIM thông qua AMQP 1.0 và REST API.
 * Tuân thủ ICAO EUR Doc 047, AMHS-SWIM Gateway Testing Plan V3.0.
 * 
 * Sử dụng Adapter Pattern để tự động phát hiện và sử dụng:
 * - Solace JCSMP (proprietary) nếu có sẵn
 * - Apache Qpid Proton-J (standard AMQP 1.0) nếu không có Solace
 * 
 * Cơ chế auto-detection: Khi chạy test case, sẽ kiểm tra xem
 * Solace API có available không, nếu có thì dùng Solace, ngược lại fallback sang Qpid.
 */
public class SwimDriver {
    private SwimMessagingAdapter activeAdapter;
    private boolean isConnected = false;
    
    // REST API components (shared regardless of messaging adapter)
    private String authToken;
    
    /**
     * Auto-detect and select the appropriate messaging adapter.
     * Priority: Solace JCSMP (if available) -> Qpid AMQP 1.0 (fallback)
     */
    private void detectAndSelectAdapter() {
        Logger.log("INFO", "Detecting available SWIM messaging adapters...");
        
        // Try Solace first (legacy/proprietary)
        SolaceSwimAdapter solaceAdapter = new SolaceSwimAdapter();
        if (solaceAdapter.isAvailable()) {
            this.activeAdapter = solaceAdapter;
            Logger.log("SUCCESS", "Selected Solace JCSMP adapter (proprietary API detected).");
            return;
        }
        
        // Fallback to Qpid (standard AMQP 1.0)
        QpidSwimAdapter qpidAdapter = new QpidSwimAdapter();
        if (qpidAdapter.isAvailable()) {
            this.activeAdapter = qpidAdapter;
            Logger.log("SUCCESS", "Selected Qpid AMQP 1.0 adapter (standard AMQP 1.0).");
            return;
        }
        
        // No adapter available
        throw new IllegalStateException(
            "No SWIM messaging adapter available. " +
            "Please ensure either Solace JCSMP or Apache Qpid Proton-J is in the classpath."
        );
    }
    
    /**
     * Kết nối tới SWIM Message Broker.
     * Tự động chọn adapter phù hợp (Solace hoặc Qpid) dựa trên availability check.
     */
    public void connect() throws Exception {
        if (activeAdapter == null) {
            detectAndSelectAdapter();
        }
        
        activeAdapter.connect();
        isConnected = true;
    }
    
    /**
     * Gửi message tới SWIM.
     * Delegate đến adapter đang được chọn (Solace hoặc Qpid).
     * 
     * @param topic Destination address (ví dụ: MET.METAR.VVTS hoặc queue name)
     * @param payload Dữ liệu message (text hoặc binary)
     * @param properties Các AMQP 1.0 application properties theo spec AMHS/SWIM
     */
    public void publishMessage(String topic, byte[] payload, Map<String, Object> properties) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        activeAdapter.publishMessage(topic, payload, properties);
    }
    
    /**
     * Gửi message với AMQPProperties helper object.
     * Chuyển đổi sang Map trước khi delegate cho adapter.
     */
    public void publishMessage(String topic, byte[] payload, AMQPProperties properties) throws Exception {
        Map<String, Object> propsMap = properties.toMap();
        publishMessage(topic, payload, propsMap);
    }
    
    /**
     * Subscribe và consume message từ SWIM.
     * Delegate đến adapter đang được chọn.
     * 
     * @param address Address/queue/topic cần subscribe
     * @param timeoutMs Timeout milliseconds
     * @return Payload nhận được
     */
    public byte[] consumeMessage(String address, long timeoutMs) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        return activeAdapter.consumeMessage(address, timeoutMs);
    }
    
    /**
     * Overload với default timeout 5000ms.
     */
    public byte[] consumeMessage(String address) throws Exception {
        return consumeMessage(address, 5000);
    }
    
    /**
     * Lấy JWT token từ Keycloak OIDC endpoint.
     * Theo EUR Doc 047: REST API authentication qua OAuth2/OIDC.
     * Đây là shared functionality không phụ thuộc vào messaging adapter.
     */
    public String obtainTokenFromKeycloak(String keycloakUrl, String realm, String clientId, 
                                         String username, String password) throws IOException {
        Logger.log("INFO", "Đang lấy JWT token từ Keycloak: " + keycloakUrl);
        
        String tokenEndpoint = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        URL url = new URL(tokenEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        String postData = "grant_type=password" +
                         "&client_id=" + clientId +
                         "&username=" + username +
                         "&password=" + password;
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int statusCode = conn.getResponseCode();
        if (statusCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                authToken = extractAccessToken(response.toString());
                Logger.log("SUCCESS", "Lấy JWT token thành công.");
                return authToken;
            }
        } else {
            Logger.log("ERROR", "Lỗi lấy token: HTTP " + statusCode);
            throw new IOException("Failed to obtain token: HTTP " + statusCode);
        }
    }
    
    /**
     * Extract access_token từ JSON response.
     */
    private String extractAccessToken(String jsonResponse) {
        int start = jsonResponse.indexOf("\"access_token\":\"") + 16;
        if (start < 16) return null;
        int end = jsonResponse.indexOf("\"", start);
        return jsonResponse.substring(start, end);
    }
    
    /**
     * Lookup service trong SWIM Registry qua REST API.
     * Theo EUR Doc 047: Service discovery qua RESTful registry.
     */
    public String lookupServiceInRegistry(String registryUrl, String serviceName, 
                                          String serviceType) throws IOException {
        Logger.log("INFO", "Đang lookup service trong registry: " + registryUrl);
        
        String lookupEndpoint = registryUrl + "/services?name=" + serviceName + "&type=" + serviceType;
        URL url = new URL(lookupEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        int statusCode = conn.getResponseCode();
        if (statusCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Logger.log("SUCCESS", "Lookup service thành công.");
                return response.toString();
            }
        } else {
            Logger.log("ERROR", "Lỗi lookup service: HTTP " + statusCode);
            throw new IOException("Failed to lookup service: HTTP " + statusCode);
        }
    }
    
    /**
     * Đóng kết nối.
     */
    public void disconnect() {
        if (activeAdapter != null) {
            activeAdapter.close();
        }
        isConnected = false;
        Logger.log("SUCCESS", "Disconnected from SWIM.");
    }
    
    /**
     * Get the currently active adapter name (for logging/debugging).
     */
    public String getActiveAdapterName() {
        return activeAdapter != null ? activeAdapter.getAdapterName() : "None";
    }
    
    /**
     * Helper class để chứa các AMQP 1.0 properties theo EUR Doc 047.
     * Có thể chuyển đổi sang Map<String, Object> để sử dụng với adapter.
     */
    public static class AMQPProperties {
        private String atsPri;           // amhs_ats_pri: SS/DD/FF/GG/KK
        private String recipients;       // amhs_recipients: danh sách recipients
        private String bodyPartType;     // amhs_bodypart_type: ia5-text, utf8-text, etc.
        private String contentType;      // amhs_content_type
        private String originator;       // amhs_originator
        private String subject;          // amhs_subject
        private String messageId;        // amhs_message_id
        private Long creationTime;       // amqp creation-time
        private String filingTime;       // amhs_filing_time
        private String dlHistory;        // amhs_dl_history
        private String secEnvelope;      // amhs_sec_envelope (cho signed messages)
        private String replyTo;          // amqp reply-to
        
        public AMQPProperties() {}
        
        // Getters and Setters
        public String getAtsPri() { return atsPri; }
        public void setAtsPri(String atsPri) { this.atsPri = atsPri; }
        
        public String getRecipients() { return recipients; }
        public void setRecipients(String recipients) { this.recipients = recipients; }
        
        public String getBodyPartType() { return bodyPartType; }
        public void setBodyPartType(String bodyPartType) { this.bodyPartType = bodyPartType; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getOriginator() { return originator; }
        public void setOriginator(String originator) { this.originator = originator; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public Long getCreationTime() { return creationTime; }
        public void setCreationTime(Long creationTime) { this.creationTime = creationTime; }
        
        public String getFilingTime() { return filingTime; }
        public void setFilingTime(String filingTime) { this.filingTime = filingTime; }
        
        public String getDlHistory() { return dlHistory; }
        public void setDlHistory(String dlHistory) { this.dlHistory = dlHistory; }
        
        public String getSecEnvelope() { return secEnvelope; }
        public void setSecEnvelope(String secEnvelope) { this.secEnvelope = secEnvelope; }
        
        public String getReplyTo() { return replyTo; }
        public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
        
        /**
         * Convert to Map<String, Object> for use with SwimMessagingAdapter.
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (atsPri != null) map.put("amhs_ats_pri", atsPri);
            if (recipients != null) map.put("amhs_recipients", recipients);
            if (bodyPartType != null) map.put("amhs_bodypart_type", bodyPartType);
            if (contentType != null) map.put("amhs_content_type", contentType);
            if (originator != null) map.put("amhs_originator", originator);
            if (subject != null) map.put("amhs_subject", subject);
            if (messageId != null) map.put("amhs_message_id", messageId);
            if (creationTime != null) map.put("creation_time", creationTime);
            if (filingTime != null) map.put("amhs_filing_time", filingTime);
            if (dlHistory != null) map.put("amhs_dl_history", dlHistory);
            if (secEnvelope != null) map.put("amhs_sec_envelope", secEnvelope);
            if (replyTo != null) map.put("amhs_reply_to", replyTo);
            return map;
        }
        
        /**
         * Create from Map<String, Object>.
         */
        public static AMQPProperties fromMap(Map<String, Object> map) {
            AMQPProperties props = new AMQPProperties();
            if (map.containsKey("amhs_ats_pri")) props.setAtsPri((String) map.get("amhs_ats_pri"));
            if (map.containsKey("amhs_recipients")) props.setRecipients((String) map.get("amhs_recipients"));
            if (map.containsKey("amhs_bodypart_type")) props.setBodyPartType((String) map.get("amhs_bodypart_type"));
            if (map.containsKey("amhs_content_type")) props.setContentType((String) map.get("amhs_content_type"));
            if (map.containsKey("amhs_originator")) props.setOriginator((String) map.get("amhs_originator"));
            if (map.containsKey("amhs_subject")) props.setSubject((String) map.get("amhs_subject"));
            if (map.containsKey("amhs_message_id")) props.setMessageId((String) map.get("amhs_message_id"));
            if (map.containsKey("creation_time")) props.setCreationTime((Long) map.get("creation_time"));
            if (map.containsKey("amhs_filing_time")) props.setFilingTime((String) map.get("amhs_filing_time"));
            if (map.containsKey("amhs_dl_history")) props.setDlHistory((String) map.get("amhs_dl_history"));
            if (map.containsKey("amhs_sec_envelope")) props.setSecEnvelope((String) map.get("amhs_sec_envelope"));
            if (map.containsKey("amhs_reply_to")) props.setReplyTo((String) map.get("amhs_reply_to"));
            return props;
        }
    }
}
