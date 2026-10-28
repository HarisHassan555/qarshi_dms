package com.bezkoder.spring.login.chat.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "chat_tbl_conversation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatTblConversation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_conversation_id")
    private Integer serConversationId;

    @Column(name = "txt_type", nullable = false, length = 16)
    private String txtType = "DIRECT";

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    public Integer getSerConversationId() {
        return serConversationId;
    }

    public void setSerConversationId(Integer serConversationId) {
        this.serConversationId = serConversationId;
    }

    public String getTxtType() {
        return txtType;
    }

    public void setTxtType(String txtType) {
        this.txtType = txtType;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }
}
