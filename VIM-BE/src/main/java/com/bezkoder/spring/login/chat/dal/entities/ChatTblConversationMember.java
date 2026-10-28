package com.bezkoder.spring.login.chat.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "chat_tbl_conversation_member")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatTblConversationMember implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_member_id")
    private Integer serMemberId;

    @Column(name = "ser_conversation_id", nullable = false)
    private Integer serConversationId;

    @Column(name = "ser_user_id", nullable = false)
    private Integer serUserId;

    @Column(name = "ser_last_read_message_id")
    private Integer serLastReadMessageId;

    @Column(name = "dte_joined_date")
    private Timestamp dteJoinedDate;

    public Integer getSerMemberId() {
        return serMemberId;
    }

    public void setSerMemberId(Integer serMemberId) {
        this.serMemberId = serMemberId;
    }

    public Integer getSerConversationId() {
        return serConversationId;
    }

    public void setSerConversationId(Integer serConversationId) {
        this.serConversationId = serConversationId;
    }

    public Integer getSerUserId() {
        return serUserId;
    }

    public void setSerUserId(Integer serUserId) {
        this.serUserId = serUserId;
    }

    public Integer getSerLastReadMessageId() {
        return serLastReadMessageId;
    }

    public void setSerLastReadMessageId(Integer serLastReadMessageId) {
        this.serLastReadMessageId = serLastReadMessageId;
    }

    public Timestamp getDteJoinedDate() {
        return dteJoinedDate;
    }

    public void setDteJoinedDate(Timestamp dteJoinedDate) {
        this.dteJoinedDate = dteJoinedDate;
    }
}
