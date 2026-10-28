package com.bezkoder.spring.login.notification.bll;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.notification.bll.dto.UserNotificationDto;
import com.bezkoder.spring.login.notification.bll.dto.UserNotificationSaveRequest;
import com.bezkoder.spring.login.notification.dal.UserNotificationRepository;
import com.bezkoder.spring.login.notification.dal.entities.CfgTblUserNotification;
import com.bezkoder.spring.login.notification.websocket.NotificationWebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserNotificationService {

    private static final Logger logger = LogManager.getLogger(UserNotificationService.class);
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private NotificationWebSocketSessionRegistry sessionRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<UserNotificationDto> listForCurrentUser(boolean unreadOnly, Integer limit) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            throw new IllegalStateException("Unauthorized");
        }
        return listForRecipient(userId, unreadOnly, limit);
    }

    @Transactional(readOnly = true)
    public List<UserNotificationDto> listForRecipient(int recipientUserId, boolean unreadOnly, Integer limit) {
        int pageSize = normalizeLimit(limit);
        return notificationRepository
                .findForRecipient(recipientUserId, unreadOnly, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserNotificationDto getByIdForCurrentUser(Long id) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            throw new IllegalStateException("Unauthorized");
        }
        CfgTblUserNotification row = notificationRepository.findActiveForRecipient(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        return toDto(row);
    }

    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            return 0;
        }
        return countUnread(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(int recipientUserId) {
        return notificationRepository.countUnreadForRecipient(recipientUserId);
    }

    @Transactional
    public UserNotificationDto create(UserNotificationSaveRequest request) {
        validateSaveRequest(request, false);
        int actorId = commonService.getCurrentLoggedInUser();
        int recipientId = resolveRecipientId(request, actorId);
        return persistAndPush(recipientId, request, request.getSerActorUserId() != null
                ? request.getSerActorUserId() : (actorId > 0 ? actorId : null));
    }

    /**
     * Used by workflow services to notify any user (initiator, next approver, etc.).
     */
    @Transactional
    public UserNotificationDto createSystemNotification(Integer recipientUserId, UserNotificationSaveRequest request,
            Integer actorUserId) {
        if (recipientUserId == null || recipientUserId <= 0) {
            return null;
        }
        validateSaveRequest(request, false);
        request.setSerRecipientUserId(recipientUserId);
        return persistAndPush(recipientUserId, request, actorUserId);
    }

    private UserNotificationDto persistAndPush(Integer recipientId, UserNotificationSaveRequest request,
            Integer actorUserId) {
        Timestamp now = commonService.getCurrentTimeStamp_new();
        CfgTblUserNotification entity = new CfgTblUserNotification();
        entity.setSerRecipientUserId(recipientId);
        entity.setSerApplicationId(request.getSerApplicationId());
        entity.setSerFormId(request.getSerFormId());
        entity.setTxtEventType(request.getTxtEventType().trim().toUpperCase());
        entity.setTxtTitle(request.getTxtTitle().trim());
        entity.setTxtMessage(trimToNull(request.getTxtMessage()));
        entity.setTxtDeepLink(trimToNull(request.getTxtDeepLink()));
        entity.setTxtPayload(trimToNull(request.getTxtPayload()));
        entity.setBlnRead(Boolean.TRUE.equals(request.getBlnRead()));
        entity.setDteReadDate(entity.getBlnRead() ? now : null);
        entity.setSerActorUserId(actorUserId);
        entity.setBlIsDeleted(false);
        entity.setBlnStatus(true);
        entity.setDteCreatedDate(now);
        CfgTblUserNotification saved = notificationRepository.save(entity);
        UserNotificationDto dto = toDto(saved);
        pushRealtime(recipientId, "CREATED", dto);
        return dto;
    }

    @Transactional
    public UserNotificationDto update(UserNotificationSaveRequest request) {
        if (request == null || request.getSerUserNotificationId() == null) {
            throw new IllegalArgumentException("serUserNotificationId is required");
        }
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            throw new IllegalStateException("Unauthorized");
        }
        CfgTblUserNotification entity = notificationRepository
                .findActiveForRecipient(request.getSerUserNotificationId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        Timestamp now = commonService.getCurrentTimeStamp_new();
        if (StringUtils.hasText(request.getTxtTitle())) {
            entity.setTxtTitle(request.getTxtTitle().trim());
        }
        if (request.getTxtMessage() != null) {
            entity.setTxtMessage(trimToNull(request.getTxtMessage()));
        }
        if (request.getTxtDeepLink() != null) {
            entity.setTxtDeepLink(trimToNull(request.getTxtDeepLink()));
        }
        if (request.getTxtPayload() != null) {
            entity.setTxtPayload(trimToNull(request.getTxtPayload()));
        }
        if (request.getBlnRead() != null) {
            entity.setBlnRead(request.getBlnRead());
            entity.setDteReadDate(Boolean.TRUE.equals(request.getBlnRead()) ? now : null);
        }
        entity.setDteModifiedDate(now);
        UserNotificationDto dto = toDto(notificationRepository.save(entity));
        pushRealtime(userId, "UPDATED", dto);
        return dto;
    }

    @Transactional
    public void deleteForCurrentUser(Long id) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            throw new IllegalStateException("Unauthorized");
        }
        CfgTblUserNotification entity = notificationRepository.findActiveForRecipient(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        Timestamp now = commonService.getCurrentTimeStamp_new();
        entity.setBlIsDeleted(true);
        entity.setBlnStatus(false);
        entity.setDteModifiedDate(now);
        notificationRepository.save(entity);
        pushRealtime(userId, "DELETED", toDto(entity));
    }

    @Transactional
    public UserNotificationDto markRead(Long id) {
        UserNotificationSaveRequest request = new UserNotificationSaveRequest();
        request.setSerUserNotificationId(id);
        request.setBlnRead(true);
        return update(request);
    }

    @Transactional
    public long markAllReadForCurrentUser() {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            throw new IllegalStateException("Unauthorized");
        }
        Timestamp now = commonService.getCurrentTimeStamp_new();
        notificationRepository.markAllReadForRecipient(userId, now);
        long unread = countUnread(userId);
        pushUnreadCount(userId, unread, "MARK_ALL_READ");
        return unread;
    }

    public void pushRealtime(Integer recipientUserId, String action, UserNotificationDto notification) {
        if (recipientUserId == null || recipientUserId <= 0) {
            return;
        }
        try {
            long unread = countUnread(recipientUserId);
            Map<String, Object> frame = new HashMap<>();
            frame.put("type", "NOTIFICATION");
            frame.put("action", action != null ? action : "CREATED");
            frame.put("notification", notification);
            frame.put("unreadCount", unread);
            sessionRegistry.sendToUser(recipientUserId, objectMapper.writeValueAsString(frame));
        } catch (Exception ex) {
            logger.warn("Could not push notification WS frame for userId=" + recipientUserId + ": " + ex.getMessage());
        }
    }

    private void pushUnreadCount(int recipientUserId, long unread, String action) {
        try {
            Map<String, Object> frame = new HashMap<>();
            frame.put("type", "NOTIFICATION");
            frame.put("action", action);
            frame.put("unreadCount", unread);
            sessionRegistry.sendToUser(recipientUserId, objectMapper.writeValueAsString(frame));
        } catch (Exception ex) {
            logger.warn("Could not push unread count WS frame: " + ex.getMessage());
        }
    }

    private int resolveRecipientId(UserNotificationSaveRequest request, int actorId) {
        if (request.getSerRecipientUserId() != null && request.getSerRecipientUserId() > 0) {
            if (request.getSerRecipientUserId().equals(actorId)) {
                return request.getSerRecipientUserId();
            }
            if (!isAdminActor(actorId)) {
                throw new IllegalArgumentException("You can only create notifications for yourself");
            }
            return request.getSerRecipientUserId();
        }
        if (actorId <= 0) {
            throw new IllegalArgumentException("serRecipientUserId is required");
        }
        return actorId;
    }

    private boolean isAdminActor(int actorId) {
        if (actorId <= 0) {
            return false;
        }
        try {
            CfgTblUser user = commonService.getCurrentUser(actorId);
            if (user == null || user.getCfgTblRole() == null || user.getCfgTblRole().getTxtRoleName() == null) {
                return false;
            }
            String role = user.getCfgTblRole().getTxtRoleName().trim().toUpperCase();
            return role.contains("ADMIN");
        } catch (Exception ex) {
            return false;
        }
    }

    private void validateSaveRequest(UserNotificationSaveRequest request, boolean isUpdate) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (!isUpdate) {
            if (!StringUtils.hasText(request.getTxtEventType())) {
                throw new IllegalArgumentException("txtEventType is required");
            }
            if (!StringUtils.hasText(request.getTxtTitle())) {
                throw new IllegalArgumentException("txtTitle is required");
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserNotificationDto toDto(CfgTblUserNotification entity) {
        UserNotificationDto dto = new UserNotificationDto();
        dto.setSerUserNotificationId(entity.getSerUserNotificationId());
        dto.setSerRecipientUserId(entity.getSerRecipientUserId());
        dto.setSerApplicationId(entity.getSerApplicationId());
        dto.setSerFormId(entity.getSerFormId());
        dto.setTxtEventType(entity.getTxtEventType());
        dto.setTxtTitle(entity.getTxtTitle());
        dto.setTxtMessage(entity.getTxtMessage());
        dto.setTxtDeepLink(entity.getTxtDeepLink());
        dto.setTxtPayload(entity.getTxtPayload());
        dto.setBlnRead(entity.getBlnRead());
        dto.setDteReadDate(entity.getDteReadDate() != null ? entity.getDteReadDate().toString() : null);
        dto.setSerActorUserId(entity.getSerActorUserId());
        dto.setDteCreatedDate(entity.getDteCreatedDate() != null ? entity.getDteCreatedDate().toString() : null);
        return dto;
    }
}
