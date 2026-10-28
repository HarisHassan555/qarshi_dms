package com.bezkoder.spring.login.notification.websocket;

import com.bezkoder.spring.login.notification.bll.UserNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LogManager.getLogger(NotificationWebSocketHandler.class);

    @Autowired
    private NotificationWebSocketSessionRegistry sessionRegistry;

    @Autowired
    @Lazy
    private UserNotificationService userNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        sessionRegistry.register(userId, session);
        try {
            long unread = userNotificationService.countUnread(userId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SYNC");
            payload.put("unreadCount", unread);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ex) {
            logger.warn("Could not send notification SYNC frame: " + ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        try {
            String body = message.getPayload();
            if (body != null && body.trim().equalsIgnoreCase("PING")) {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
        } catch (Exception ex) {
            logger.debug("Notification WS client message ignored: " + ex.getMessage());
        }
    }
}
