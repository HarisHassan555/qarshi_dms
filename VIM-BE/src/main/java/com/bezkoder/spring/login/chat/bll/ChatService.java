package com.bezkoder.spring.login.chat.bll;

import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.chat.bll.dto.ChatConversationDto;
import com.bezkoder.spring.login.chat.bll.dto.ChatMessageDto;
import com.bezkoder.spring.login.chat.bll.dto.ChatUserDto;
import com.bezkoder.spring.login.chat.dal.ChatConversationMemberRepository;
import com.bezkoder.spring.login.chat.dal.ChatConversationRepository;
import com.bezkoder.spring.login.chat.dal.ChatMessageRepository;
import com.bezkoder.spring.login.chat.dal.entities.ChatTblConversation;
import com.bezkoder.spring.login.chat.dal.entities.ChatTblConversationMember;
import com.bezkoder.spring.login.chat.dal.entities.ChatTblMessage;
import com.bezkoder.spring.login.chat.websocket.ChatWebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatConversationMemberRepository memberRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private ChatWebSocketSessionRegistry sessionRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    public List<ChatUserDto> listChatUsers(int currentUserId) {
        List<CfgTblUser> users = userService.getActiveUser();
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .filter((u) -> u.getSerUserId() != null && u.getSerUserId() > 0 && !u.getSerUserId().equals(currentUserId))
                .map(this::toChatUser)
                .sorted(Comparator.comparing(ChatUserDto::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<ChatConversationDto> listConversations(int currentUserId) {
        List<ChatTblConversationMember> memberships = memberRepository.findBySerUserId(currentUserId);
        if (memberships == null || memberships.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatConversationDto> result = new ArrayList<>();
        for (ChatTblConversationMember membership : memberships) {
            ChatConversationDto dto = buildConversationSummary(currentUserId, membership);
            if (dto != null) {
                result.add(dto);
            }
        }
        result.sort((a, b) -> Long.compare(
                b.getLastMessageAt() != null ? b.getLastMessageAt() : 0L,
                a.getLastMessageAt() != null ? a.getLastMessageAt() : 0L));
        return result;
    }

    @Transactional
    public ChatConversationDto openDirectConversation(int currentUserId, int targetUserId) {
        if (targetUserId <= 0 || targetUserId == currentUserId) {
            throw new IllegalArgumentException("Invalid target user");
        }
        Integer existingId = findDirectConversationId(currentUserId, targetUserId);
        if (existingId != null) {
            ChatTblConversationMember membership = memberRepository.findBySerConversationIdAndSerUserId(existingId, currentUserId);
            return buildConversationSummary(currentUserId, membership);
        }
        ChatTblConversation conversation = new ChatTblConversation();
        conversation.setTxtType("DIRECT");
        conversation.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        conversation = conversationRepository.save(conversation);

        addMember(conversation.getSerConversationId(), currentUserId);
        addMember(conversation.getSerConversationId(), targetUserId);

        ChatTblConversationMember membership = memberRepository.findBySerConversationIdAndSerUserId(
                conversation.getSerConversationId(), currentUserId);
        return buildConversationSummary(currentUserId, membership);
    }

    public List<ChatMessageDto> getMessages(int currentUserId, int conversationId, int limit) {
        assertMember(currentUserId, conversationId);
        int otherUserId = findOtherMemberUserId(conversationId, currentUserId);
        int otherLastRead = otherLastReadMessageId(conversationId, otherUserId);
        boolean otherOnline = otherUserId > 0 && sessionRegistry.isUserOnline(otherUserId);
        int pageSize = Math.min(Math.max(limit, 1), 200);
        List<ChatTblMessage> messages = messageRepository.findBySerConversationIdOrderByDteCreatedDateDesc(
                conversationId, PageRequest.of(0, pageSize));
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (ChatTblMessage message : messages) {
            ChatMessageDto dto = toMessageDto(message);
            if (message.getSerSenderUserId() != null && message.getSerSenderUserId().equals(currentUserId)
                    && message.getSerMessageId() != null) {
                dto.setDeliveryStatus(resolveDeliveryStatus(message.getSerMessageId(), otherLastRead, otherOnline));
            }
            dtos.add(dto);
        }
        Collections.reverse(dtos);
        return dtos;
    }

    @Transactional
    public ChatMessageDto sendMessage(int senderUserId, int conversationId, String body) {
        assertMember(senderUserId, conversationId);
        ChatTblMessage message = new ChatTblMessage();
        message.setSerConversationId(conversationId);
        message.setSerSenderUserId(senderUserId);
        message.setTxtBody(body);
        message.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        message = messageRepository.save(message);

        ChatTblConversationMember senderMember = memberRepository.findBySerConversationIdAndSerUserId(conversationId, senderUserId);
        if (senderMember != null) {
            senderMember.setSerLastReadMessageId(message.getSerMessageId());
            memberRepository.save(senderMember);
        }
        ChatMessageDto dto = toMessageDto(message);
        int otherUserId = findOtherMemberUserId(conversationId, senderUserId);
        boolean otherOnline = otherUserId > 0 && sessionRegistry.isUserOnline(otherUserId);
        dto.setDeliveryStatus(resolveDeliveryStatus(message.getSerMessageId(), 0, otherOnline));
        return dto;
    }

    @Transactional
    public void markRead(int currentUserId, int conversationId, int lastReadMessageId) {
        assertMember(currentUserId, conversationId);
        ChatTblConversationMember member = memberRepository.findBySerConversationIdAndSerUserId(conversationId, currentUserId);
        if (member == null) {
            return;
        }
        if (member.getSerLastReadMessageId() == null || lastReadMessageId > member.getSerLastReadMessageId()) {
            member.setSerLastReadMessageId(lastReadMessageId);
            memberRepository.save(member);
            deliverReadReceipt(conversationId, currentUserId, lastReadMessageId);
        }
    }

    public void deliverReadReceipt(int conversationId, int readerUserId, int lastReadMessageId) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "READ_RECEIPT",
                    "payload", Map.of(
                            "conversationId", conversationId,
                            "readerUserId", readerUserId,
                            "lastReadMessageId", lastReadMessageId)));
            List<ChatTblConversationMember> members = memberRepository.findBySerConversationId(conversationId);
            for (ChatTblConversationMember member : members) {
                if (member.getSerUserId() != null && !member.getSerUserId().equals(readerUserId)) {
                    sessionRegistry.sendToUser(member.getSerUserId(), json);
                }
            }
        } catch (Exception ignored) {
            // best-effort
        }
    }

    public void deliverMessage(ChatMessageDto message) {
        if (message == null || message.getConversationId() == null) {
            return;
        }
        Integer senderUserId = message.getSenderUserId();
        int conversationId = message.getConversationId();
        int otherUserId = senderUserId != null ? findOtherMemberUserId(conversationId, senderUserId) : 0;
        List<ChatTblConversationMember> members = memberRepository.findBySerConversationId(conversationId);
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "NEW_MESSAGE",
                    "payload", message));
            for (ChatTblConversationMember member : members) {
                if (member.getSerUserId() != null) {
                    sessionRegistry.sendToUser(member.getSerUserId(), json);
                }
            }
            if (senderUserId != null && message.getMessageId() != null && otherUserId > 0
                    && sessionRegistry.isUserOnline(otherUserId)) {
                deliverDeliveryReceipt(conversationId, message.getMessageId(), senderUserId);
            }
        } catch (Exception ignored) {
            // delivery best-effort
        }
    }

    public void deliverDeliveryReceipt(int conversationId, int messageId, int senderUserId) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "DELIVERY_RECEIPT",
                    "payload", Map.of(
                            "conversationId", conversationId,
                            "messageId", messageId)));
            sessionRegistry.sendToUser(senderUserId, json);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private void addMember(int conversationId, int userId) {
        ChatTblConversationMember member = new ChatTblConversationMember();
        member.setSerConversationId(conversationId);
        member.setSerUserId(userId);
        member.setDteJoinedDate(new Timestamp(System.currentTimeMillis()));
        memberRepository.save(member);
    }

    private Integer findDirectConversationId(int userA, int userB) {
        List<ChatTblConversationMember> memberships = memberRepository.findBySerUserId(userA);
        for (ChatTblConversationMember membership : memberships) {
            Integer conversationId = membership.getSerConversationId();
            List<ChatTblConversationMember> members = memberRepository.findBySerConversationId(conversationId);
            if (members.size() != 2) {
                continue;
            }
            boolean hasA = false;
            boolean hasB = false;
            for (ChatTblConversationMember member : members) {
                if (member.getSerUserId() != null && member.getSerUserId().equals(userA)) {
                    hasA = true;
                }
                if (member.getSerUserId() != null && member.getSerUserId().equals(userB)) {
                    hasB = true;
                }
            }
            if (hasA && hasB) {
                Optional<ChatTblConversation> conversation = conversationRepository.findById(conversationId);
                if (conversation.isPresent() && "DIRECT".equalsIgnoreCase(conversation.get().getTxtType())) {
                    return conversationId;
                }
            }
        }
        return null;
    }

    private ChatConversationDto buildConversationSummary(int currentUserId, ChatTblConversationMember membership) {
        if (membership == null) {
            return null;
        }
        int conversationId = membership.getSerConversationId();
        List<ChatTblConversationMember> members = memberRepository.findBySerConversationId(conversationId);
        Integer otherUserId = null;
        for (ChatTblConversationMember member : members) {
            if (member.getSerUserId() != null && !member.getSerUserId().equals(currentUserId)) {
                otherUserId = member.getSerUserId();
                break;
            }
        }
        ChatConversationDto dto = new ChatConversationDto();
        dto.setConversationId(conversationId);
        dto.setOtherUserId(otherUserId);
        if (otherUserId != null) {
            CfgTblUser other = userService.getUserById(otherUserId);
            if (other != null) {
                dto.setOtherUserName(displayName(other));
                dto.setOtherUserHasProfilePicture(hasProfilePicture(other));
            }
        }
        List<ChatTblMessage> latest = messageRepository.findBySerConversationIdOrderByDteCreatedDateDesc(
                conversationId, PageRequest.of(0, 1));
        if (!latest.isEmpty()) {
            ChatTblMessage last = latest.get(0);
            dto.setLastMessageBody(last.getTxtBody());
            dto.setLastMessageAt(last.getDteCreatedDate() != null ? last.getDteCreatedDate().getTime() : null);
            int lastRead = membership.getSerLastReadMessageId() != null ? membership.getSerLastReadMessageId() : 0;
            if (last.getSerMessageId() != null && last.getSerSenderUserId() != null
                    && !last.getSerSenderUserId().equals(currentUserId)
                    && last.getSerMessageId() > lastRead) {
                long unread = messageRepository.countBySerConversationIdAndSerMessageIdGreaterThan(conversationId, lastRead);
                dto.setUnreadCount((int) unread);
            }
        }
        return dto;
    }

    private int findOtherMemberUserId(int conversationId, int currentUserId) {
        List<ChatTblConversationMember> members = memberRepository.findBySerConversationId(conversationId);
        for (ChatTblConversationMember member : members) {
            if (member.getSerUserId() != null && !member.getSerUserId().equals(currentUserId)) {
                return member.getSerUserId();
            }
        }
        return 0;
    }

    private int otherLastReadMessageId(int conversationId, int otherUserId) {
        if (otherUserId <= 0) {
            return 0;
        }
        ChatTblConversationMember other = memberRepository.findBySerConversationIdAndSerUserId(conversationId, otherUserId);
        if (other == null || other.getSerLastReadMessageId() == null) {
            return 0;
        }
        return other.getSerLastReadMessageId();
    }

    private String resolveDeliveryStatus(int messageId, int otherLastRead, boolean otherOnline) {
        if (messageId > 0 && otherLastRead >= messageId) {
            return "read";
        }
        if (otherOnline) {
            return "delivered";
        }
        return "sent";
    }

    private void assertMember(int userId, int conversationId) {
        ChatTblConversationMember member = memberRepository.findBySerConversationIdAndSerUserId(conversationId, userId);
        if (member == null) {
            throw new IllegalArgumentException("Not a member of this conversation");
        }
    }

    private ChatMessageDto toMessageDto(ChatTblMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(message.getSerMessageId());
        dto.setConversationId(message.getSerConversationId());
        dto.setSenderUserId(message.getSerSenderUserId());
        dto.setBody(message.getTxtBody());
        dto.setSentAt(message.getDteCreatedDate() != null ? message.getDteCreatedDate().getTime() : null);
        if (message.getSerSenderUserId() != null) {
            CfgTblUser sender = userService.getUserById(message.getSerSenderUserId());
            if (sender != null) {
                dto.setSenderName(displayName(sender));
            }
        }
        return dto;
    }

    private ChatUserDto toChatUser(CfgTblUser user) {
        ChatUserDto dto = new ChatUserDto();
        dto.setUserId(user.getSerUserId());
        dto.setLoginName(user.getTxtLoginName());
        dto.setDisplayName(displayName(user));
        dto.setOrganizationName(blankToNull(user.getTxtOrganizationName()));
        dto.setDivisionName(blankToNull(user.getTxtDivisionName()));
        dto.setDepartmentName(departmentName(user));
        dto.setHasProfilePicture(hasProfilePicture(user));
        return dto;
    }

    private boolean hasProfilePicture(CfgTblUser user) {
        if (user == null) {
            return false;
        }
        String path = user.getTxtProfilePicturePath();
        return path != null && !path.trim().isEmpty();
    }

    private String departmentName(CfgTblUser user) {
        String direct = blankToNull(user.getTxtDepartmentName());
        if (direct != null) {
            return direct;
        }
        if (user.getHrTblDepartment() != null) {
            return blankToNull(user.getHrTblDepartment().getTxtDepartmentName());
        }
        return null;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String displayName(CfgTblUser user) {
        if (user.getTxtUserName() != null && !user.getTxtUserName().trim().isEmpty()) {
            return user.getTxtUserName().trim();
        }
        if (user.getTxtLoginName() != null && !user.getTxtLoginName().trim().isEmpty()) {
            return user.getTxtLoginName().trim();
        }
        return "User " + user.getSerUserId();
    }
}
