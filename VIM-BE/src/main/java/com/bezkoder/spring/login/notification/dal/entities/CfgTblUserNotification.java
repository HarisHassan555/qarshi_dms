package com.bezkoder.spring.login.notification.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_user_notification")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblUserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_user_notification_id")
    private Long serUserNotificationId;

    @Column(name = "ser_recipient_user_id", nullable = false)
    private Integer serRecipientUserId;

    @Column(name = "ser_application_id")
    private Integer serApplicationId;

    @Column(name = "ser_form_id")
    private Integer serFormId;

    @Column(name = "txt_event_type", nullable = false, length = 64)
    private String txtEventType;

    @Column(name = "txt_title", nullable = false, length = 255)
    private String txtTitle;

    @Column(name = "txt_message", length = 2000)
    private String txtMessage;

    @Column(name = "txt_deep_link", length = 512)
    private String txtDeepLink;

    @Lob
    @Column(name = "txt_payload", columnDefinition = "LONGTEXT")
    private String txtPayload;

    @Column(name = "bln_read", nullable = false)
    private Boolean blnRead;

    @Column(name = "dte_read_date")
    private Timestamp dteReadDate;

    @Column(name = "ser_actor_user_id")
    private Integer serActorUserId;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    public Long getSerUserNotificationId() {
        return serUserNotificationId;
    }

    public void setSerUserNotificationId(Long serUserNotificationId) {
        this.serUserNotificationId = serUserNotificationId;
    }

    public Integer getSerRecipientUserId() {
        return serRecipientUserId;
    }

    public void setSerRecipientUserId(Integer serRecipientUserId) {
        this.serRecipientUserId = serRecipientUserId;
    }

    public Integer getSerApplicationId() {
        return serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtEventType() {
        return txtEventType;
    }

    public void setTxtEventType(String txtEventType) {
        this.txtEventType = txtEventType;
    }

    public String getTxtTitle() {
        return txtTitle;
    }

    public void setTxtTitle(String txtTitle) {
        this.txtTitle = txtTitle;
    }

    public String getTxtMessage() {
        return txtMessage;
    }

    public void setTxtMessage(String txtMessage) {
        this.txtMessage = txtMessage;
    }

    public String getTxtDeepLink() {
        return txtDeepLink;
    }

    public void setTxtDeepLink(String txtDeepLink) {
        this.txtDeepLink = txtDeepLink;
    }

    public String getTxtPayload() {
        return txtPayload;
    }

    public void setTxtPayload(String txtPayload) {
        this.txtPayload = txtPayload;
    }

    public Boolean getBlnRead() {
        return blnRead;
    }

    public void setBlnRead(Boolean blnRead) {
        this.blnRead = blnRead;
    }

    public Timestamp getDteReadDate() {
        return dteReadDate;
    }

    public void setDteReadDate(Timestamp dteReadDate) {
        this.dteReadDate = dteReadDate;
    }

    public Integer getSerActorUserId() {
        return serActorUserId;
    }

    public void setSerActorUserId(Integer serActorUserId) {
        this.serActorUserId = serActorUserId;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }
}
