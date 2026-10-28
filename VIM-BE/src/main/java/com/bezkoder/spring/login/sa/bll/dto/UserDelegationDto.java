package com.bezkoder.spring.login.sa.bll.dto;

import java.util.ArrayList;
import java.util.List;

public class UserDelegationDto {

    private Integer serDelegationId;
    private Integer serDelegateUserId;
    private String delegateUserName;
    private String delegateEmployeeId;
    private Integer serOnBehalfUserId;
    private String onBehalfUserName;
    private String onBehalfRole;
    private String dteValidFrom;
    private String dteValidTo;
    private String txtScope;
    private List<String> documentIds = new ArrayList<>();
    /** Resolved names for documents in {@link #txtScope} only. */
    private List<UserDelegatableDocumentDto> scopedDocuments = new ArrayList<>();
    private String documentsLabel;
    private String txtStatus;
    private String txtNotes;
    private boolean hasAttachment;
    private String attachmentFileName;
    private String createdByName;

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

    public String getDelegateUserName() {
        return delegateUserName;
    }

    public void setDelegateUserName(String delegateUserName) {
        this.delegateUserName = delegateUserName;
    }

    public String getDelegateEmployeeId() {
        return delegateEmployeeId;
    }

    public void setDelegateEmployeeId(String delegateEmployeeId) {
        this.delegateEmployeeId = delegateEmployeeId;
    }

    public Integer getSerOnBehalfUserId() {
        return serOnBehalfUserId;
    }

    public void setSerOnBehalfUserId(Integer serOnBehalfUserId) {
        this.serOnBehalfUserId = serOnBehalfUserId;
    }

    public String getOnBehalfUserName() {
        return onBehalfUserName;
    }

    public void setOnBehalfUserName(String onBehalfUserName) {
        this.onBehalfUserName = onBehalfUserName;
    }

    public String getOnBehalfRole() {
        return onBehalfRole;
    }

    public void setOnBehalfRole(String onBehalfRole) {
        this.onBehalfRole = onBehalfRole;
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

    public List<UserDelegatableDocumentDto> getScopedDocuments() {
        return scopedDocuments;
    }

    public void setScopedDocuments(List<UserDelegatableDocumentDto> scopedDocuments) {
        this.scopedDocuments = scopedDocuments;
    }

    public String getDocumentsLabel() {
        return documentsLabel;
    }

    public void setDocumentsLabel(String documentsLabel) {
        this.documentsLabel = documentsLabel;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public String getTxtNotes() {
        return txtNotes;
    }

    public void setTxtNotes(String txtNotes) {
        this.txtNotes = txtNotes;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}
