package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;
import com.solacesystems.jcsmp.*;

import java.util.Map;

/**
 * Solace JCSMP implementation of SwimMessagingAdapter.
 * Uses Solace proprietary JCSMP API for legacy deployments.
 */
public class SolaceSwimAdapter implements SwimMessagingAdapter {
    
    private JCSMPSession session;
    private XMLMessageProducer producer;
    private XMLMessageConsumer consumer;
    private boolean isConnected = false;
    
    private static final String SOLACE_JCSMP_CLASS = "com.solacesystems.jcsmp.JCSMPSession";
    
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
        String port = config.getProperty("swim.broker.port", "55555");
        String user = config.getProperty("swim.broker.username", "default");
        String pass = config.getProperty("swim.broker.password", "default");
        String vpn = config.getProperty("solace.vpn", "default");
        
        Logger.log("INFO", "Connecting to SWIM Broker via Solace JCSMP at: " + host + ":" + port);
        
        // Create Solace properties
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host + ":" + port);
        properties.setProperty(JCSMPProperties.USERNAME, user);
        properties.setProperty(JCSMPProperties.PASSWORD, pass);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        
        // Create session
        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        
        // Create producer
        producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void responseReceived(String messageID) {}
            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                Logger.log("ERROR", "Solace Publish Error: " + e.getMessage());
            }
        });
        
        isConnected = true;
        Logger.log("SUCCESS", "Solace JCSMP connection established.");
    }
    
    @Override
    public void publishMessage(String topic, byte[] payload, Map<String, Object> properties) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Publishing message via Solace JCSMP to: " + topic);
        
        // Create message
        XMLMessage msg;
        String bodyPartType = (String) properties.get("amhs_bodypart_type");
        
        if (bodyPartType != null && (bodyPartType.contains("text") || bodyPartType.contains("ia5"))) {
            TextMessage textMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            textMsg.setText(new String(payload));
            msg = textMsg;
        } else {
            BytesMessage bytesMsg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
            bytesMsg.setData(payload);
            msg = bytesMsg;
        }
        
        // Set User Properties
        SDTMap userProps = JCSMPFactory.onlyInstance().createMap();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("amhs_")) {
                userProps.putString(key, String.valueOf(entry.getValue()));
            }
        }
        msg.setProperties(userProps);
        
        // Set Priority (0-9)
        if (properties.containsKey("amhs_ats_pri")) {
            msg.setPriority(mapAmhsPriorityToInt((String) properties.get("amhs_ats_pri")));
        }
        
        // Send message
        Topic solaceTopic = JCSMPFactory.onlyInstance().createTopic(topic);
        producer.send(msg, solaceTopic);
        
        Logger.log("SUCCESS", "Message published via Solace JCSMP.");
    }
    
    @Override
    public byte[] consumeMessage(String address, long timeoutMs) throws Exception {
        if (!isConnected) {
            connect();
        }
        
        Logger.log("INFO", "Consuming message via Solace JCSMP from: " + address);
        
        Topic solaceTopic = JCSMPFactory.onlyInstance().createTopic(address);
        consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {}
            @Override
            public void onException(JCSMPException e) {
                Logger.log("ERROR", "Solace Consumer Error: " + e.getMessage());
            }
        });
        
        session.addSubscription(solaceTopic);
        consumer.start();
        
        BytesXMLMessage rxMsg = consumer.receive((int)timeoutMs);
        
        if (rxMsg != null) {
            Logger.log("SUCCESS", "Message received via Solace JCSMP.");
            
            if (rxMsg instanceof TextMessage) {
                return ((TextMessage) rxMsg).getText().getBytes();
            } else if (rxMsg instanceof BytesMessage) {
                return ((BytesMessage) rxMsg).getData();
            } else {
                byte[] attachment = new byte[rxMsg.getAttachmentContentLength()];
                rxMsg.readAttachmentBytes(attachment);
                return attachment;
            }
        }
        
        return null;
    }
    
    @Override
    public void close() {
        Logger.log("INFO", "Closing Solace JCSMP connection...");
        if (consumer != null) consumer.close();
        if (producer != null) producer.close();
        if (session != null) session.closeSession();
        isConnected = false;
        Logger.log("SUCCESS", "Solace JCSMP connection closed.");
    }
    
    private int mapAmhsPriorityToInt(String amhsPriority) {
        if (amhsPriority == null) return 4;
        switch (amhsPriority.toUpperCase()) {
            case "SS": return 9;
            case "DD": return 7;
            case "FF": return 4;
            case "GG": return 2;
            case "KK": return 0;
            default: return 4;
        }
    }
}
