package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

public class UserDelegationSaveRequest {

    private Integer serDelegationId;
    private Integer serDelegateUserId;
    private Integer serOnBehalfUserId;
    private String dteValidFrom;
    private String dteValidTo;
    /** Comma-separated form IDs; optional if documentIds is set */
    private String txtScope;
    private List<String> documentIds;
    private String txtNotes;
    /** Optional data URL or base64 supporting document */
    private String attachmentBase64;
    private String attachmentFileName;
    /** When true on update, removes stored attachment */
    private Boolean removeAttachment;
    /** When true, send notification emails to delegate and on-behalf users */
    private Boolean notifyBothParties;

    public Integer getSerDelegationId() {
        return serDelegationId;
    }

    public void setSerDelegationId(Integer serDelegationId) {
        this.serDelegationId = serDelegationId;
    }

    public Integer getSerDelegateUserId() {
        return serDelegateUserId;
    }

    public void setSerDelegateUserId(Integer serDelegateUserId) {
        this.serDelegateUserId = serDelegateUserId;
    }

    public Integer getSerOnBehalfUserId() {
        return serOnBehalfUserId;
    }

    public void setSerOnBehalfUserId(Integer serOnBehalfUserId) {
        this.serOnBehalfUserId = serOnBehalfUserId;
    }

    public String getDteValidFrom() {
        return dteValidFrom;
    }

    public void setDteValidFrom(String dteValidFrom) {
        this.dteValidFrom = dteValidFrom;
    }

    public String getDteValidTo() {
        return dteValidTo;
    }

    public void setDteValidTo(String dteValidTo) {
        this.dteValidTo = dteValidTo;
    }

    public String getTxtScope() {
        return txtScope;
    }

    public void setTxtScope(String txtScope) {
        this.txtScope = txtScope;
    }

    public List<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<String> documentIds) {
        this.documentIds = documentIds;
    }

    public String getTxtNotes() {
        return txtNotes;
    }

    public void setTxtNotes(String txtNotes) {
        this.txtNotes = txtNotes;
    }

    public String getAttachmentBase64() {
        return attachmentBase64;
    }

    public void setAttachmentBase64(String attachmentBase64) {
        this.attachmentBase64 = attachmentBase64;
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }

    public Boolean getRemoveAttachment() {
        return removeAttachment;
    }

    public void setRemoveAttachment(Boolean removeAttachment) {
        this.removeAttachment = removeAttachment;
    }

    public Boolean getNotifyBothParties() {
        return notifyBothParties;
    }

    public void setNotifyBothParties(Boolean notifyBothParties) {
        this.notifyBothParties = notifyBothParties;
    }
}
