package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;
import com.solace.api.jms.*;
import com.solace.client.SolSession;
import com.solace.client.XMLMessageProducer;
import com.solace.client.MessageConsumer;
import com.solace.client.JCSMPFactory;
import com.solace.client.JCSMPProperties;
import com.solace.client.JCSMPSession;
import com.solace.system.SolaceVersion;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * Solace JCSMP implementation of SwimMessagingAdapter.
 * Uses Solace proprietary JCSMP API for legacy deployments.
 */
public class SolaceSwimAdapter implements SwimMessagingAdapter {
    
    private JCSMPSession session;
    private XMLMessageProducer producer;
    private MessageConsumer consumer;
    private boolean isConnected = false;
    
    private static final String SOLACE_JCSMP_CLASS = "com.solace.client.JCSMPSession";
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if Solace JCSMP classes are present
            Class.forName(SOLACE_JCSMP_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public String getAdapterName() {
        return "Solace-JCSMP";
    }
    
    @Override
    public void connect() throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Solace JCSMP library not available");
        }
        
        TestConfig config = TestConfig.getInstance();
        String host = config.getProperty("swim.broker.host");
        String port = config.getProperty("swim.broker.port", "5672");
        String user = config.getProperty("swim.broker.user", "default");
        String pass = config.getProperty("swim.broker.password", "default");
        String vpn = config.getProperty("solace.vpn", "default");
        
        Logger.log("INFO", "Connecting to SWIM Broker via Solace JCSMP at: " + host + ":" + port);
        
        // Create Solace properties
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host + ":" + port);
        properties.setProperty(JCSMPProperties.USERNAME_USERNAME, user);
        properties.setProperty(JCSMPProperties.PASSWORD, pass);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        
        // Create session
        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        
        // Create producer
        producer = JCSMPFactory.onlyInstance().createMessageProducer(session, null);
        
        isConnected = true;
        Logger.log("SUCCESS", "Solace JCSMP connection established.");
    }
    
    @Override
    public void publishMessage(String topic, byte[] payload, Map<String, Object> properties) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Publishing message via Solace JCSMP to: " + topic);
        
        // Create text or bytes message based on content
        Message msg;
        String contentType = (String) properties.get("amhs_content_type");
        
        if (contentType != null && (contentType.contains("text") || contentType.contains("ia5"))) {
            TextMessage textMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            textMsg.setText(new String(payload));
            msg = textMsg;
        } else {
            BytesMessage bytesMsg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
            bytesMsg.writeBytes(payload);
            msg = bytesMsg;
        }
        
        // Set Solace-specific properties (mapped from AMHS properties)
        if (properties.containsKey("amhs_ats_pri")) {
            msg.setIntProperty("JMSXDeliveryCount", mapAmhsPriorityToInt((String) properties.get("amhs_ats_pri")));
        }
        if (properties.containsKey("amhs_message_id")) {
            msg.setJMSMessageID((String) properties.get("amhs_message_id"));
        }
        if (properties.containsKey("amhs_subject")) {
            msg.setStringProperty("amhs_subject", (String) properties.get("amhs_subject"));
        }
        if (properties.containsKey("amhs_originator")) {
            msg.setStringProperty("amhs_originator", (String) properties.get("amhs_originator"));
        }
        if (properties.containsKey("amhs_recipients")) {
            msg.setStringProperty("amhs_recipients", (String) properties.get("amhs_recipients"));
        }
        
        // Send message
        Destination dest = JCSMPFactory.onlyInstance().createTopic(topic);
        producer.send(msg, dest);
        
        Logger.log("SUCCESS", "Message published via Solace JCSMP.");
    }
    
    @Override
    public byte[] consumeMessage(String address, long timeoutMs) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Consuming message via Solace JCSMP from: " + address);
        
        // Create consumer
        consumer = JCSMPFactory.onlyInstance().createMessageConsumer(session, 
            JCSMPFactory.onlyInstance().createTopic(address), null, null);
        consumer.start();
        
        // Receive message with timeout
        Message msg = consumer.receive(timeoutMs);
        
        if (msg != null) {
            Logger.log("SUCCESS", "Message received via Solace JCSMP.");
            
            if (msg instanceof TextMessage) {
                return ((TextMessage) msg).getText().getBytes();
            } else if (msg instanceof BytesMessage) {
                BytesMessage bytesMsg = (BytesMessage) msg;
                byte[] data = new byte[(int) bytesMsg.getBodyLength()];
                bytesMsg.readBytes(data);
                return data;
            }
        }
        
        return null;
    }
    
    @Override
    public void close() {
        Logger.log("INFO", "Closing Solace JCSMP connection...");
        
        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }
        if (session != null) {
            session.closeSession();
        }
        
        isConnected = false;
        Logger.log("SUCCESS", "Solace JCSMP connection closed.");
    }
    
    /**
     * Map AMHS priority to integer for Solace.
     */
    private int mapAmhsPriorityToInt(String amhsPriority) {
        if (amhsPriority == null) return 0;
        switch (amhsPriority.toUpperCase()) {
            case "SS": return 6;
            case "DD": return 4;
            case "FF": return 2;
            case "GG": return 1;
            case "KK": return 0;
            default: return 0;
        }
    }
}
