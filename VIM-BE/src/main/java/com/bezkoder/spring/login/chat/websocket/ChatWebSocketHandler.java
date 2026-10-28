package com.bezkoder.spring.login.chat.websocket;

import com.bezkoder.spring.login.chat.bll.ChatService;
import com.bezkoder.spring.login.chat.bll.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LogManager.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private ChatWebSocketSessionRegistry sessionRegistry;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            sessionRegistry.register(userId, session);
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
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            if ("SEND".equalsIgnoreCase(type)) {
                int conversationId = root.path("conversationId").asInt(0);
                String body = root.path("body").asText("").trim();
                if (conversationId <= 0 || body.isEmpty()) {
                    return;
                }
                ChatMessageDto saved = chatService.sendMessage(userId, conversationId, body);
                chatService.deliverMessage(saved);
            }
        } catch (Exception ex) {
            logger.warn("Chat WebSocket message failed: " + ex.getMessage());
        }
    }

    public void sendJson(WebSocketSession session, String type, Object payload) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("type", type);
            envelope.put("payload", payload);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
        } catch (Exception ex) {
            logger.warn("Failed to send WS json: " + ex.getMessage());
        }
    }
}
