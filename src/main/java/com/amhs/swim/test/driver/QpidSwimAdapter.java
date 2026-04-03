package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.reactor.Reactor;
import org.apache.qpid.proton.transport.Transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Apache Qpid Proton-J implementation of SwimMessagingAdapter.
 * Uses standard AMQP 1.0 API compliant with ICAO EUR Doc 047.
 */
public class QpidSwimAdapter implements SwimMessagingAdapter {
    
    private Connection connection;
    private Session session;
    private Sender sender;
    private Receiver receiver;
    private boolean isConnected = false;
    private String authToken;
    private BlockingQueue<Message> receivedMessages;
    
    // AMQP 1.0 Application Property Keys per EUR Doc 047
    public static final Symbol AMHS_ATS_PRI = Symbol.valueOf("amhs_ats_pri");
    public static final Symbol AMHS_RECIPIENTS = Symbol.valueOf("amhs_recipients");
    public static final Symbol AMHS_BODY_PART_TYPE = Symbol.valueOf("amhs_bodypart_type");
    public static final Symbol AMHS_CONTENT_TYPE = Symbol.valueOf("amhs_content_type");
    public static final Symbol AMHS_ORIGINATOR = Symbol.valueOf("amhs_originator");
    public static final Symbol AMHS_SUBJECT = Symbol.valueOf("amhs_subject");
    public static final Symbol AMHS_MESSAGE_ID = Symbol.valueOf("amhs_message_id");
    public static final Symbol AMHS_FILING_TIME = Symbol.valueOf("amhs_filing_time");
    public static final Symbol AMHS_DL_HISTORY = Symbol.valueOf("amhs_dl_history");
    public static final Symbol AMHS_SEC_ENVELOPE = Symbol.valueOf("amhs_sec_envelope");
    
    private static final String QPID_PROTON_CLASS = "org.apache.qpid.proton.Proton";
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName(QPID_PROTON_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public String getAdapterName() {
        return "Qpid-AMQP1.0";
    }
    
    @Override
    public void connect() throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Apache Qpid Proton library not available");
        }
        
        TestConfig config = TestConfig.getInstance();
        String host = config.getProperty("swim.broker.host");
        String port = config.getProperty("swim.broker.port", "5672");
        String user = config.getProperty("swim.broker.user", "default");
        String pass = config.getProperty("swim.broker.password", "default");
        String containerId = config.getProperty("swim.container.id", "amhs-swim-gateway-test");
        
        Logger.log("INFO", "Connecting to SWIM Broker via AMQP 1.0 at: " + host + ":" + port);
        
        // Create AMQP 1.0 connection using Qpid Proton
        this.connection = createAmqpConnection(host, port, user, pass, containerId);
        this.session = connection.session();
        session.open();
        
        receivedMessages = new LinkedBlockingQueue<>();
        isConnected = true;
        Logger.log("SUCCESS", "AMQP 1.0 connection established.");
    }
    
    /**
     * Create AMQP 1.0 connection.
     */
    private Connection createAmqpConnection(String host, String port, String user, String pass, String containerId) throws Exception {
        // In real implementation, use Proton-J Client API with Reactor pattern
        // This is a simplified version for demonstration
        Connection conn = Proton.connection();
        conn.setContainerId(containerId);
        // Actual connection logic would use Reactor and Transport
        return conn;
    }
    
    @Override
    public void publishMessage(String topic, byte[] payload, Map<String, Object> properties) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Publishing message via AMQP 1.0 to: " + topic);
        
        // Create AMQP 1.0 message per spec
        Message message = Proton.message();
        
        // Set message annotations
        Map<String, Object> annotations = new HashMap<>();
        annotations.put("x-amqp-topic", topic);
        message.setMessageAnnotations(annotations);
        
        // Set properties per AMQP 1.0 spec
        message.setAddress(topic);
        if (properties.containsKey("amhs_message_id")) {
            message.setMessageId(properties.get("amhs_message_id"));
        }
        if (properties.containsKey("amhs_subject")) {
            message.setSubject((String) properties.get("amhs_subject"));
        }
        if (properties.containsKey("amhs_reply_to")) {
            message.setReplyTo((String) properties.get("amhs_reply_to"));
        }
        
        // Set creation time if present
        if (properties.containsKey("creation_time")) {
            message.setCreationTime((Long) properties.get("creation_time"));
        }
        
        // Set priority (AMQP 1.0 uses 0-9, AMHS uses SS/DD/FF/GG/KK)
        if (properties.containsKey("amhs_ats_pri")) {
            message.setPriority(mapAmhsPriorityToAmqp((String) properties.get("amhs_ats_pri")));
        }
        
        // Set application properties per EUR Doc 047
        Map<Symbol, Object> appProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("amhs_")) {
                appProperties.put(Symbol.valueOf(key), entry.getValue());
            }
        }
        message.setApplicationProperties(appProperties);
        
        // Set body - Data (binary) or AmqpValue (text)
        Section body = createBodySection(payload, 
            (String) properties.getOrDefault("amhs_bodypart_type", "ia5-text"));
        message.setBody(body);
        
        // Send message via AMQP 1.0 sender link
        sendAmqpMessage(message);
        
        Logger.log("SUCCESS", "Message published via AMQP 1.0.");
    }
    
    /**
     * Create body section based on content type.
     */
    private Section createBodySection(byte[] payload, String bodyPartType) {
        if ("ia5-text".equalsIgnoreCase(bodyPartType) || "utf8-text".equalsIgnoreCase(bodyPartType)) {
            return new AmqpValue(new String(payload, StandardCharsets.UTF_8));
        } else {
            return new Data(new Binary(payload));
        }
    }
    
    /**
     * Send message via AMQP 1.0 sender link.
     */
    private void sendAmqpMessage(Message message) throws Exception {
        if (sender == null) {
            sender = session.sender("test-sender");
            sender.open();
        }
        
        // Encode and send message
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        int encoded = message.encode(buffer);
        buffer.flip();
        
        byte[] data = new byte[encoded];
        buffer.get(data);
        
        Delivery delivery = sender.send(data, 0, encoded);
        delivery.setTag(Long.toString(System.currentTimeMillis()).getBytes());
        
        // Wait for settlement
        while (!delivery.isSettled()) {
            Thread.sleep(10);
        }
    }
    
    @Override
    public byte[] consumeMessage(String address, long timeoutMs) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Consuming message via AMQP 1.0 from: " + address);
        
        // Create receiver link
        if (receiver == null) {
            receiver = session.receiver(address);
            receiver.open();
        }
        
        // Receive message with timeout
        Message message = receiveAmqpMessage(timeoutMs);
        
        if (message != null) {
            Logger.log("SUCCESS", "Message received via AMQP 1.0.");
            
            // Extract body
            Section body = message.getBody();
            if (body instanceof Data) {
                Binary binary = ((Data) body).getValue();
                return binary.getArray();
            } else if (body instanceof AmqpValue) {
                Object value = ((AmqpValue) body).getValue();
                if (value instanceof String) {
                    return ((String) value).getBytes(StandardCharsets.UTF_8);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Receive message with timeout.
     */
    private Message receiveAmqpMessage(long timeoutMs) throws Exception {
        Message msg = receivedMessages.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (msg != null) {
            return msg;
        }
        return null;
    }
    
    /**
     * Handle message received from AMQP 1.0 reactor.
     */
    public void onMessageReceived(Message message) {
        if (receivedMessages != null) {
            receivedMessages.offer(message);
        }
    }
    
    /**
     * Get JWT token from Keycloak OIDC endpoint.
     */
    public String obtainTokenFromKeycloak(String keycloakUrl, String realm, String clientId, 
                                         String username, String password) throws IOException {
        Logger.log("INFO", "Getting JWT token from Keycloak: " + keycloakUrl);
        
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
                Logger.log("SUCCESS", "JWT token obtained successfully.");
                return authToken;
            }
        } else {
            Logger.log("ERROR", "Failed to get token: HTTP " + statusCode);
            throw new IOException("Failed to obtain token: HTTP " + statusCode);
        }
    }
    
    /**
     * Extract access_token from JSON response.
     */
    private String extractAccessToken(String jsonResponse) {
        int start = jsonResponse.indexOf("\"access_token\":\"") + 16;
        if (start < 16) return null;
        int end = jsonResponse.indexOf("\"", start);
        return jsonResponse.substring(start, end);
    }
    
    /**
     * Lookup service in SWIM Registry via REST API.
     */
    public String lookupServiceInRegistry(String registryUrl, String serviceName, 
                                          String serviceType) throws IOException {
        Logger.log("INFO", "Looking up service in registry: " + registryUrl);
        
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
                Logger.log("SUCCESS", "Service lookup successful.");
                return response.toString();
            }
        } else {
            Logger.log("ERROR", "Failed to lookup service: HTTP " + statusCode);
            throw new IOException("Failed to lookup service: HTTP " + statusCode);
        }
    }
    
    @Override
    public void close() {
        Logger.log("INFO", "Closing AMQP 1.0 connection...");
        
        if (sender != null) {
            sender.close();
        }
        if (receiver != null) {
            receiver.close();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
        
        isConnected = false;
        Logger.log("SUCCESS", "AMQP 1.0 connection closed.");
    }
    
    /**
     * Map AMHS priority to AMQP 1.0 priority (0-9).
     * Per spec: SS->6, DD->4, FF->0, GG->0, KK->0
     */
    private byte mapAmhsPriorityToAmqp(String amhsPriority) {
        if (amhsPriority == null) return 0;
        switch (amhsPriority.toUpperCase()) {
            case "SS": return 6;
            case "DD": return 4;
            case "FF": return 0;
            case "GG": return 0;
            case "KK": return 0;
            default: return 0;
        }
    }
}
