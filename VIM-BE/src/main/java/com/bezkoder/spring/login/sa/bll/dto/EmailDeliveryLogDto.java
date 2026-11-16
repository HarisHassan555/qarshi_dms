package com.bezkoder.spring.login.sa.bll.dto;

import java.io.Serializable;

public class EmailDeliveryLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String triggerType;
    private String templateCode;
    private Integer applicationId;
    private Integer recipientUserId;
    private String recipientEmail;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String status;
    private String errorMessage;
    private boolean hasAttachments;
    private Integer triggeredByUserId;
    private Long backgroundJobId;
    private String contextLabel;
    private String sentAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public Integer getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public void setTriggeredByUserId(Integer triggeredByUserId) {
        this.triggeredByUserId = triggeredByUserId;
    }

    public Long getBackgroundJobId() {
        return backgroundJobId;
    }

    public void setBackgroundJobId(Long backgroundJobId) {
        this.backgroundJobId = backgroundJobId;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public void setContextLabel(String contextLabel) {
        this.contextLabel = contextLabel;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
}