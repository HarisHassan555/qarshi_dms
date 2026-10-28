package com.bezkoder.spring.login.chat.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "chat_tbl_message")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatTblMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_message_id")
    private Integer serMessageId;

    @Column(name = "ser_conversation_id", nullable = false)
    private Integer serConversationId;

    @Column(name = "ser_sender_user_id", nullable = false)
    private Integer serSenderUserId;

    @Column(name = "txt_body", nullable = false, length = 4000)
    private String txtBody;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    public Integer getSerMessageId() {
        return serMessageId;
    }

    public void setSerMessageId(Integer serMessageId) {
        this.serMessageId = serMessageId;
    }

    public Integer getSerConversationId() {
        return serConversationId;
    }

    public void setSerConversationId(Integer serConversationId) {
        this.serConversationId = serConversationId;
    }

    public Integer getSerSenderUserId() {
        return serSenderUserId;
    }

    public void setSerSenderUserId(Integer serSenderUserId) {
        this.serSenderUserId = serSenderUserId;
    }

    public String getTxtBody() {
        return txtBody;
    }

    public void setTxtBody(String txtBody) {
        this.txtBody = txtBody;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }
}
