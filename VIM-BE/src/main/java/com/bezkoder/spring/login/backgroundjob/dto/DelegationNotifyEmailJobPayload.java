package com.bezkoder.spring.login.backgroundjob.dto;

public class DelegationNotifyEmailJobPayload {

    private Integer serDelegationId;
    private boolean updated;
    private String documentsLabel;

    public Integer getSerDelegationId() {
        return serDelegationId;
    }

    public void setSerDelegationId(Integer serDelegationId) {
        this.serDelegationId = serDelegationId;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public String getDocumentsLabel() {
        return documentsLabel;
    }

    public void setDocumentsLabel(String documentsLabel) {
        this.documentsLabel = documentsLabel;
    }
}
