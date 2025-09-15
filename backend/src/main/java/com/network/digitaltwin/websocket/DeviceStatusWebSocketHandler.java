package com.network.digitaltwin.websocket;

import com.network.digitaltwin.monitoring.model.DeviceStatus;
import com.network.digitaltwin.monitoring.model.Alert;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket handler for device status updates and alerts.
 */
@Component
public class DeviceStatusWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStatusWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SubscriptionInfo> subscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Called after a WebSocket connection is established.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        logger.info("WebSocket connection established: {}", sessionId);

        // Send a welcome message
        sendMessage(session, createWelcomeMessage(sessionId));

        // Start ping/pong to keep connection alive
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("{"type":"ping"}"));
                }
            } catch (IOException e) {
                logger.warn("Error sending ping to WebSocket session: {}", sessionId, e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Called when a WebSocket message is received.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        logger.debug("Received message from WebSocket session {}: {}", sessionId, payload);

        try {
            // Parse the message
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");

            if ("subscribe".equals(type)) {
                handleSubscribe(session, msg);
            } else if ("unsubscribe".equals(type)) {
                handleUnsubscribe(session, msg);
            } else if ("pong".equals(type)) {
                // Handle pong response (do nothing for now)
            } else {
                logger.warn("Unknown message type from WebSocket session {}: {}", sessionId, type);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message from session: " + sessionId, e);
            sendMessage(session, createErrorMessage("Invalid message format"));
        }
    }

    /**
     * Called after a WebSocket connection is closed.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        subscriptions.remove(sessionId);

        logger.info("WebSocket connection closed: {}, reason: {}", sessionId, status);
    }

    /**
     * Handle a WebSocket error.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket transport error for session: " + sessionId, exception);

        try {
            session.close();
        } catch (IOException e) {
            logger.warn("Error closing WebSocket session after transport error: " + sessionId, e);
        }
    }

    /**
     * Handle a subscription request.
     */
    private void handleSubscribe(WebSocketSession session, Map<String, Object> message) {
        String sessionId = session.getId();

        try {
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            String filterType = (String) payload.get("filterType");
            String filterValue = (String) payload.get("filterValue");

            // Validate filter type
            if (!"all".equals(filterType) && !"device".equals(filterType) && 
                !"owner".equals(filterType) && !"criticality".equals(filterType)) {
                sendMessage(session, createErrorMessage("Invalid filter type: " + filterType));
                return;
            }

            // Store subscription information
            SubscriptionInfo subscription = new SubscriptionInfo(
                sessionId, filterType, filterValue, Instant.now());
            subscriptions.put(sessionId, subscription);

            // Send confirmation
            sendMessage(session, createSubscriptionConfirmation(filterType, filterValue));

            logger.info("WebSocket session {} subscribed to filter: {}, value: {}", 
                sessionId, filterType, filterValue);

        } catch (Exception e) {
            logger.error("Error handling subscription for WebSocket session: " + sessionId, e);
            sendMessage(session, createErrorMessage("Invalid subscription request"));
        }
    }

    /**
     * Handle an unsubscribe request.
     */
    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> message) {
        String sessionId = session.getId();

        try {
            Map<String, Object> payload = (Map<String, Object>) message.get("payload");
            String filterType = (String) payload.get("filterType");
            String filterValue = (String) payload.get("filterValue");

            // Check if subscription matches
            SubscriptionInfo subscription = subscriptions.get(sessionId);
            if (subscription != null && 
                subscription.getFilterType().equals(filterType) && 
                subscription.getFilterValue().equals(filterValue)) {

                subscriptions.remove(sessionId);

                // Send confirmation
                sendMessage(session, createUnsubscriptionConfirmation(filterType, filterValue));

                logger.info("WebSocket session {} unsubscribed from filter: {}, value: {}", 
                    sessionId, filterType, filterValue);
            } else {
                sendMessage(session, createErrorMessage("No matching subscription found"));
            }

        } catch (Exception e) {
            logger.error("Error handling unsubscription for WebSocket session: " + sessionId, e);
            sendMessage(session, createErrorMessage("Invalid unsubscription request"));
        }
    }

    /**
     * Send a device status update to all subscribed sessions.
     */
    public void sendDeviceStatusUpdate(DeviceStatus status) {
        String messageType = "device.status.update";
        Map<String, Object> payload = createDeviceStatusPayload(status);

        broadcastToSubscribers(messageType, payload, status.getDevice().getId().toString());
    }

    /**
     * Send an alert to all relevant sessions.
     */
    public void sendAlertUpdate(Alert alert) {
        String messageType = "alert.update";
        Map<String, Object> payload = createAlertPayload(alert);

        // Send to sessions subscribed to all devices
        broadcastToSubscribers(messageType, payload, null);

        // Send to sessions subscribed to the specific device
        broadcastToSubscribers(messageType, payload, alert.getDevice().getId().toString());

        // Send to sessions subscribed to the device owner
        broadcastToSubscribers(messageType, payload, 
            alert.getDevice().getOwner() != null ? 
            alert.getDevice().getOwner().getId().toString() : null);
    }

    /**
     * Broadcast a message to relevant subscribers.
     */
    private void broadcastToSubscribers(String messageType, Map<String, Object> payload, String deviceId) {
        sessions.forEach((sessionId, session) -> {
            if (!session.isOpen()) {
                return;
            }

            SubscriptionInfo subscription = subscriptions.get(sessionId);
            if (subscription == null) {
                // No subscription, send only to "all" subscribers
                if ("all".equals(subscription.getFilterType())) {
                    sendMessage(session, createMessage(messageType, payload));
                }
                return;
            }

            // Check if this session should receive the message
            if ("all".equals(subscription.getFilterType()) || 
                shouldReceiveMessage(subscription, deviceId)) {
                sendMessage(session, createMessage(messageType, payload));
            }
        });
    }

    /**
     * Check if a subscription should receive a message for a specific device.
     */
    private boolean shouldReceiveMessage(SubscriptionInfo subscription, String deviceId) {
        if (deviceId == null) {
            return false;
        }

        switch (subscription.getFilterType()) {
            case "device":
                return deviceId.equals(subscription.getFilterValue());
            case "owner":
                // In a real implementation, we would check if the device belongs to the owner
                return true; // Simplified for this example
            case "criticality":
                // In a real implementation, we would check the device's criticality
                return true; // Simplified for this example
            default:
                return false;
        }
    }

    /**
     * Send a message to a specific session.
     */
    private void sendMessage(WebSocketSession session, String message) {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            logger.error("Error sending WebSocket message to session: " + session.getId(), e);
        }
    }

    /**
     * Create a welcome message.
     */
    private String createWelcomeMessage(String sessionId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "welcome");
        message.put("version", 1);
        message.put("sessionId", sessionId);
        message.put("timestamp", Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create an error message.
     */
    private String createErrorMessage(String error) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("message", error);
        message.put("timestamp", Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create a subscription confirmation message.
     */
    private String createSubscriptionConfirmation(String filterType, String filterValue) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "subscription.confirmed");
        message.put("filterType", filterType);
        message.put("filterValue", filterValue);
        message.put("timestamp", Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create an unsubscription confirmation message.
     */
    private String createUnsubscriptionConfirmation(String filterType, String filterValue) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "unsubscription.confirmed");
        message.put("filterType", filterType);
        message.put("filterValue", filterValue);
        message.put("timestamp", Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create a message with type and payload.
     */
    private String createMessage(String type, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("version", 1);
        message.put("payload", payload);
        message.put("timestamp", Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create a device status payload.
     */
    private Map<String, Object> createDeviceStatusPayload(DeviceStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", status.getDevice().getId().toString());
        payload.put("deviceName", status.getDevice().getName());
        payload.put("status", status.getStatus());
        payload.put("lastSeen", status.getLastSeen().toString());
        payload.put("receivedAt", status.getReceivedAt().toString());
        payload.put("cpuPercent", status.getCpuPercent());
        payload.put("memoryPercent", status.getMemoryPercent());
        payload.put("diskPercent", status.getDiskPercent());
        payload.put("networkInKb", status.getNetworkInKb());
        payload.put("networkOutKb", status.getNetworkOutKb());
        payload.put("uptimeSeconds", status.getUptimeSeconds());

        // Add any additional metrics
        if (status.getMetrics() != null) {
            status.getMetrics().forEach(payload::put);
        }

        return payload;
    }

    /**
     * Create an alert payload.
     */
    private Map<String, Object> createAlertPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", alert.getId().toString());
        payload.put("deviceId", alert.getDevice().getId().toString());
        payload.put("deviceName", alert.getDevice().getName());
        payload.put("alertType", alert.getAlertType());
        payload.put("severity", alert.getSeverity());
        payload.put("status", alert.getStatus());
        payload.put("message", alert.getMessage());
        payload.put("createdAt", alert.getCreatedAt().toString());
        payload.put("escalationLevel", alert.getEscalationLevel());

        // Add payload data if available
        if (alert.getPayload() != null) {
            alert.getPayload().forEach(payload::put);
        }

        return payload;
    }

    /**
     * Convert a map to JSON string.
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("Error converting map to JSON", e);
            return "{}";
        }
    }

    /**
     * Subscription information for a WebSocket session.
     */
    private static class SubscriptionInfo {
        private final String sessionId;
        private final String filterType;
        private final String filterValue;
        private final Instant subscribedAt;

        public SubscriptionInfo(String sessionId, String filterType, String filterValue, Instant subscribedAt) {
            this.sessionId = sessionId;
            this.filterType = filterType;
            this.filterValue = filterValue;
            this.subscribedAt = subscribedAt;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getFilterType() {
            return filterType;
        }

        public String getFilterValue() {
            return filterValue;
        }

        public Instant getSubscribedAt() {
            return subscribedAt;
        }
    }
}
