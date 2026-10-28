package com.bezkoder.spring.login.notification.bll.dto;

public class UserNotificationDto {

    private Long serUserNotificationId;
    private Integer serRecipientUserId;
    private Integer serApplicationId;
    private Integer serFormId;
    private String txtEventType;
    private String txtTitle;
    private String txtMessage;
    private String txtDeepLink;
    private String txtPayload;
    private Boolean blnRead;
    private String dteReadDate;
    private Integer serActorUserId;
    private String dteCreatedDate;

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

    public String getDteReadDate() {
        return dteReadDate;
    }

    public void setDteReadDate(String dteReadDate) {
        this.dteReadDate = dteReadDate;
    }

    public Integer getSerActorUserId() {
        return serActorUserId;
    }

    public void setSerActorUserId(Integer serActorUserId) {
        this.serActorUserId = serActorUserId;
    }

    public String getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(String dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }
}
