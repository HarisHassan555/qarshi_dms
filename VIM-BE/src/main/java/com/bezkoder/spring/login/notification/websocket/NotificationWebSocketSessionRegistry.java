package com.bezkoder.spring.login.notification.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationWebSocketSessionRegistry {

    private final Map<Integer, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public void register(Integer userId, WebSocketSession session) {
        sessionsByUserId.computeIfAbsent(userId, (key) -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Integer) {
            Set<WebSocketSession> sessions = sessionsByUserId.get((Integer) userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByUserId.remove((Integer) userId);
                }
            }
        }
    }

    public void sendToUser(Integer userId, String json) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException ignored) {
                    // drop broken session
                }
            }
        }
    }
}
