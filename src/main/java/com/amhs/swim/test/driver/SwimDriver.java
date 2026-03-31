package com.amhs.swim.test.driver;

import com.amhs.swim.test.config.TestConfig;
import com.amhs.swim.test.util.Logger;
import com.solacesystems.jcsmp.*;

/**
 * Driver tương tác với phía SWIM thông qua AMQP (Solace) và REST.
 */
public class SwimDriver {
    private JCSMPSession session;
    private XMLMessageProducer producer;
    private boolean isConnected = false;

    /**
     * Kết nối tới SWIM Message Broker (Solace).
     */
    public void connect() throws JCSMPException {
        TestConfig config = TestConfig.getInstance();
        String host = config.getProperty("swim.broker.host");
        String vpn = config.getProperty("swim.broker.vpn");
        String user = config.getProperty("swim.broker.user", "default");
        String pass = config.getProperty("swim.broker.password", "default");

        Logger.log("INFO", "Đang kết nối SWIM Broker tại: " + host);

        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, user);
        properties.setProperty(JCSMPProperties.PASSWORD, pass);

        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandlerImpl());
        
        isConnected = true;
        Logger.log("SUCCESS", "Kết nối SWIM thành công.");
    }

    /**
     * Gửi message AMQP tới SWIM.
     * @param topic Destination topic (ví dụ: MET.METAR.VVTS).
     * @param payload Dữ liệu message (text hoặc binary).
     * @param properties Các application properties (amhs_ats_pri, amhs_recipients...).
     */
    public void publishMessage(String topic, byte[] payload, AMQPProperties properties) throws Exception {
        if (!isConnected) connect();

        Logger.log("INFO", "Đang publish message SWIM tới topic: " + topic);

        // Tạo message AMQP (giả lập qua Solace TextMessage hoặc BytesMessage)
        // Trong thực tế AMQP 1.0 cần mapping sang Solace API phù hợp
        BytesMessage msg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
        msg.setData(payload);
        Topic dest = JCSMPFactory.onlyInstance().createTopic(topic);

        // Thêm Application Properties theo spec AMHS/SWIM Gateway
        if (properties != null) {
            SDTMap map = JCSMPFactory.onlyInstance().createMap();
            map.putString("amhs_ats_pri", properties.getAtsPri());
            map.putString("amhs_recipients", properties.getRecipients());
            map.putString("amhs_bodypart_type", properties.getBodyPartType());
            msg.setProperties(map);
            // ... các field khác
        }

        producer.send(msg, dest);
        Logger.log("SUCCESS", "Publish message SWIM thành công.");
    }

    /**
     * Subscribe và nhận message từ SWIM.
     * @param topic Topic cần subscribe.
     * @return Payload nhận được.
     */
    public byte[] consumeMessage(String topic) throws Exception {
        if (!isConnected) connect();

        Logger.log("INFO", "Đang subscribe topic: " + topic);
        
        Topic t = JCSMPFactory.onlyInstance().createTopic(topic);
        
        XMLMessageConsumer cons = session.getMessageConsumer((XMLMessageListener) null);
        session.addSubscription(t);
        cons.start();
        
        BytesXMLMessage xmlMsg = cons.receive(5000);
        if (xmlMsg instanceof BytesMessage) {
            Logger.log("SUCCESS", "Nhận message SWIM thành công.");
            return ((BytesMessage) xmlMsg).getData();
        } else if (xmlMsg instanceof TextMessage) {
            Logger.log("SUCCESS", "Nhận message SWIM thành công.");
            return ((TextMessage) xmlMsg).getText().getBytes();
        } else if (xmlMsg != null) {
            Logger.log("SUCCESS", "Nhận message SWIM thành công.");
            return new byte[0];
        }
        return null;
    }

    // Helper class để chứa các property AMQP
    public static class AMQPProperties {
        private String atsPri;
        private String recipients;
        private String bodyPartType;
        // Getters and Setters
        public String getAtsPri() { return atsPri; }
        public void setAtsPri(String atsPri) { this.atsPri = atsPri; }
        public String getRecipients() { return recipients; }
        public void setRecipients(String recipients) { this.recipients = recipients; }
        public String getBodyPartType() { return bodyPartType; }
        public void setBodyPartType(String bodyPartType) { this.bodyPartType = bodyPartType; }
    }
    
    // Placeholder implementations for event handlers
    private static class JCSMPStreamingPublishCorrelatingEventHandlerImpl implements JCSMPStreamingPublishCorrelatingEventHandler {
        @Override
        public void responseReceivedEx(Object key) {}
        @Override
        public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {}
        @Override
        public void responseReceived(String messageID) {}
        @Override
        public void handleError(String messageID, JCSMPException e, long timestamp) {}
    }
}