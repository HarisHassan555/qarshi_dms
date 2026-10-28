package com.bezkoder.spring.login.chat.websocket;

import com.bezkoder.spring.login.security.jwt.JwtUtils;
import com.bezkoder.spring.login.security.services.UserDetailsImpl;
import com.bezkoder.spring.login.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class ChatWebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String token = servletRequest.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        if (!jwtUtils.validateJwtToken(token.trim())) {
            return false;
        }
        String loginName = jwtUtils.getUserNameFromJwtToken(token.trim());
        try {
            UserDetailsImpl user = (UserDetailsImpl) userDetailsService.loadUserByUsername(loginName);
            if (user == null || user.getId() == null || user.getId() <= 0) {
                return false;
            }
            attributes.put("userId", user.getId().intValue());
            attributes.put("loginName", loginName);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
