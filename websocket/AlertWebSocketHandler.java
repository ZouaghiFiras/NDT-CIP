package com.network.digitaltwin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.digitaltwin.topology.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for topology alerts and notifications.
 */
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Called after a WebSocket connection is established.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        logger.info("WebSocket connection established for alerts: {}", sessionId);

        // Send a welcome message
        sendMessage(session, createWelcomeMessage(sessionId));
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

        logger.info("WebSocket connection closed for alerts: {}, reason: {}", sessionId, status);
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
            if (!"all".equals(filterType) && !"severity".equals(filterType) && 
                !"type".equals(filterType) && !"device".equals(filterType)) {
                sendMessage(session, createErrorMessage("Invalid filter type: " + filterType));
                return;
            }

            // Send confirmation
            sendMessage(session, createSubscriptionConfirmation(filterType, filterValue));

            logger.info("WebSocket session {} subscribed to alert filter: {}, value: {}", 
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

            // Send confirmation
            sendMessage(session, createUnsubscriptionConfirmation(filterType, filterValue));

            logger.info("WebSocket session {} unsubscribed from alert filter: {}, value: {}", 
                sessionId, filterType, filterValue);

        } catch (Exception e) {
            logger.error("Error handling unsubscription for WebSocket session: " + sessionId, e);
            sendMessage(session, createErrorMessage("Invalid unsubscription request"));
        }
    }

    /**
     * Send a new alert to all subscribed sessions.
     */
    public void sendNewAlert(Alert alert) {
        String messageType = "alert.new";
        Map<String, Object> payload = createAlertPayload(alert);

        // Send to all sessions
        broadcastToSessions(messageType, payload);
    }

    /**
     * Send an alert resolution notification to all subscribed sessions.
     */
    public void sendAlertResolution(Alert alert) {
        String messageType = "alert.resolved";
        Map<String, Object> payload = createAlertPayload(alert);

        // Send to all sessions
        broadcastToSessions(messageType, payload);
    }

    /**
     * Send an alert unresolution notification to all subscribed sessions.
     */
    public void sendAlertUnresolution(Alert alert) {
        String messageType = "alert.unresolved";
        Map<String, Object> payload = createAlertPayload(alert);

        // Send to all sessions
        broadcastToSessions(messageType, payload);
    }

    /**
     * Send a validation completion notification to all subscribed sessions.
     */
    public void sendValidationCompletion(String runId, int alertCount) {
        String messageType = "validation.completed";
        Map<String, Object> payload = Map.of(
            "runId", runId,
            "alertCount", alertCount,
            "timestamp", java.time.Instant.now().toString()
        );

        // Send to all sessions
        broadcastToSessions(messageType, payload);
    }

    /**
     * Send a validation failure notification to all subscribed sessions.
     */
    public void sendValidationFailure(String runId, String errorMessage) {
        String messageType = "validation.failed";
        Map<String, Object> payload = Map.of(
            "runId", runId,
            "errorMessage", errorMessage,
            "timestamp", java.time.Instant.now().toString()
        );

        // Send to all sessions
        broadcastToSessions(messageType, payload);
    }

    /**
     * Broadcast a message to all WebSocket sessions.
     */
    private void broadcastToSessions(String messageType, Map<String, Object> payload) {
        String message = createMessage(messageType, payload);

        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
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
        message.put("timestamp", java.time.Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create an error message.
     */
    private String createErrorMessage(String error) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("message", error);
        message.put("timestamp", java.time.Instant.now().toString());

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
        message.put("timestamp", java.time.Instant.now().toString());

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
        message.put("timestamp", java.time.Instant.now().toString());

        return toJson(message);
    }

    /**
     * Create an alert payload.
     */
    private Map<String, Object> createAlertPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", alert.getId());
        payload.put("alertType", alert.getAlertType());
        payload.put("severity", alert.getSeverity());
        payload.put("message", alert.getMessage());
        payload.put("timestamp", alert.getTimestamp().toString());
        payload.put("affectedDevices", alert.getAffectedDevices());
        payload.put("affectedConnections", alert.getAffectedConnections());
        payload.put("resolved", alert.isResolved());
        payload.put("resolvedAt", alert.getResolvedAt() != null ? alert.getResolvedAt().toString() : null);
        payload.put("resolutionNotes", alert.getResolutionNotes());

        return payload;
    }

    /**
     * Create a message with type and payload.
     */
    private String createMessage(String type, Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("payload", payload);
        message.put("timestamp", java.time.Instant.now().toString());

        return toJson(message);
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
}
