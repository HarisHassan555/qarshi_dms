package com.bezkoder.spring.login.chat.bll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatConversationDto {
    private Integer conversationId;
    private Integer otherUserId;
    private String otherUserName;
    private String lastMessageBody;
    private Long lastMessageAt;
    private int unreadCount;
    @JsonProperty("otherUserHasProfilePicture")
    private boolean otherUserHasProfilePicture;

    public Integer getConversationId() {
        return conversationId;
    }

    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(Integer otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getLastMessageBody() {
        return lastMessageBody;
    }

    public void setLastMessageBody(String lastMessageBody) {
        this.lastMessageBody = lastMessageBody;
    }

    public Long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean getOtherUserHasProfilePicture() {
        return otherUserHasProfilePicture;
    }

    public void setOtherUserHasProfilePicture(boolean otherUserHasProfilePicture) {
        this.otherUserHasProfilePicture = otherUserHasProfilePicture;
    }
}
