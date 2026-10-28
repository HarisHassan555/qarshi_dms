package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.notification.bll.UserNotificationService;
import com.bezkoder.spring.login.notification.bll.dto.UserNotificationDto;
import com.bezkoder.spring.login.notification.bll.dto.UserNotificationSaveRequest;
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
public class UserNotificationController {

    private static final Logger logger = LogManager.getLogger(UserNotificationController.class);

    @Autowired
    private UserNotificationService userNotificationService;

    @RequestMapping(value = "/getUserNotifications", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserNotificationDto> getUserNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer limit,
            HttpServletResponse response) {
        try {
            return userNotificationService.listForCurrentUser(unreadOnly, limit);
        } catch (IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return List.of();
        } catch (Exception ex) {
            logger.error("getUserNotifications failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return List.of();
        }
    }

    @RequestMapping(value = "/getUserNotificationById", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserNotificationDto getUserNotificationById(@RequestParam Long id, HttpServletResponse response) {
        try {
            return userNotificationService.getByIdForCurrentUser(id);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (Exception ex) {
            logger.error("getUserNotificationById failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getUnreadNotificationCount", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getUnreadNotificationCount(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("unreadCount", userNotificationService.countUnreadForCurrentUser());
            return result;
        } catch (Exception ex) {
            logger.error("getUnreadNotificationCount failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("unreadCount", 0);
            return result;
        }
    }

    @RequestMapping(value = "/createUserNotification", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createUserNotification(@RequestBody UserNotificationSaveRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserNotificationDto dto = userNotificationService.create(request);
            result.put("status", "Success");
            result.put("data", dto);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("createUserNotification failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/updateUserNotification", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateUserNotification(@RequestBody UserNotificationSaveRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserNotificationDto dto = userNotificationService.update(request);
            result.put("status", "Success");
            result.put("data", dto);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("updateUserNotification failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/deleteUserNotification", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> deleteUserNotification(@RequestParam Long id, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            userNotificationService.deleteForCurrentUser(id);
            result.put("status", "Success");
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("deleteUserNotification failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/markNotificationRead", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> markNotificationRead(@RequestParam Long id, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserNotificationDto dto = userNotificationService.markRead(id);
            result.put("status", "Success");
            result.put("data", dto);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("markNotificationRead failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/markAllNotificationsRead", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> markAllNotificationsRead(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            long unread = userNotificationService.markAllReadForCurrentUser();
            result.put("status", "Success");
            result.put("unreadCount", unread);
            return result;
        } catch (Exception ex) {
            logger.error("markAllNotificationsRead failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }
}
