package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.chat.bll.ChatService;
import com.bezkoder.spring.login.chat.bll.dto.ChatConversationDto;
import com.bezkoder.spring.login.chat.bll.dto.ChatMessageDto;
import com.bezkoder.spring.login.chat.bll.dto.ChatUserDto;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class ChatController {

    private static final Logger logger = LogManager.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private ICommonService commonService;

    @RequestMapping(value = "/chat/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ChatUserDto> listUsers(HttpServletResponse response) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return List.of();
        }
        return chatService.listChatUsers(userId);
    }

    @RequestMapping(value = "/chat/conversations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ChatConversationDto> listConversations(HttpServletResponse response) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return List.of();
        }
        return chatService.listConversations(userId);
    }

    @RequestMapping(value = "/chat/conversations/direct", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatConversationDto openDirect(@RequestBody Map<String, Object> body, HttpServletResponse response) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        int targetUserId = parseInt(body.get("targetUserId"));
        try {
            return chatService.openDirectConversation(userId, targetUserId);
        } catch (Exception ex) {
            logger.warn("openDirect failed: " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    @RequestMapping(value = "/chat/conversations/{conversationId}/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ChatMessageDto> getMessages(@PathVariable int conversationId,
            @RequestParam(defaultValue = "100") int limit,
            HttpServletResponse response) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return List.of();
        }
        try {
            return chatService.getMessages(userId, conversationId, limit);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return List.of();
        }
    }

    @RequestMapping(value = "/chat/conversations/{conversationId}/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendMessage(@PathVariable int conversationId,
            @RequestBody Map<String, Object> body,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("status", "Failure");
            return result;
        }
        String text = body.get("body") != null ? String.valueOf(body.get("body")).trim() : "";
        if (text.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            return result;
        }
        try {
            ChatMessageDto saved = chatService.sendMessage(userId, conversationId, text);
            chatService.deliverMessage(saved);
            result.put("status", "Success");
            result.put("message", saved);
            return result;
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/chat/conversations/{conversationId}/read", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> markRead(@PathVariable int conversationId,
            @RequestBody Map<String, Object> body,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("status", "Failure");
            return result;
        }
        int lastReadMessageId = parseInt(body.get("lastReadMessageId"));
        try {
            chatService.markRead(userId, conversationId, lastReadMessageId);
            result.put("status", "Success");
            return result;
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.put("status", "Failure");
            return result;
        }
    }

    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
