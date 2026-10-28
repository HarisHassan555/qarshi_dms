package com.bezkoder.spring.login.sa.bll.dto;

public class WorkflowEmailRenderRequest {

    private final String recipientName;
    private final Integer level;
    private final String applicationCode;
    private final String formName;
    private final String status;
    private final String remarks;
    private final boolean showActionButtons;
    private final String approveUrl;
    private final String rejectUrl;
    private final String sendBackUrl;
    private final String sendBackToInitiatorUrl;
    private final String holdUrl;
    private final String unholdUrl;
    private final String opinionUrl;
    private final boolean finalCompletion;
    private final boolean approvalReminder;
    private final String statusSubjectSuffix;
    private final String approverSubjectSuffix;
    private final String myApplicationUrl;
    private final String applicationName;

    private WorkflowEmailRenderRequest(Builder builder) {
        this.recipientName = builder.recipientName;
        this.level = builder.level;
        this.applicationCode = builder.applicationCode;
        this.formName = builder.formName;
        this.status = builder.status;
        this.remarks = builder.remarks;
        this.showActionButtons = builder.showActionButtons;
        this.approveUrl = builder.approveUrl;
        this.rejectUrl = builder.rejectUrl;
        this.sendBackUrl = builder.sendBackUrl;
        this.sendBackToInitiatorUrl = builder.sendBackToInitiatorUrl;
        this.holdUrl = builder.holdUrl;
        this.unholdUrl = builder.unholdUrl;
        this.opinionUrl = builder.opinionUrl;
        this.finalCompletion = builder.finalCompletion;
        this.approvalReminder = builder.approvalReminder;
        this.statusSubjectSuffix = builder.statusSubjectSuffix;
        this.approverSubjectSuffix = builder.approverSubjectSuffix;
        this.myApplicationUrl = builder.myApplicationUrl;
        this.applicationName = builder.applicationName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRecipientName() {
        return recipientName;
    }

    public Integer getLevel() {
        return level;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getFormName() {
        return formName;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public boolean isShowActionButtons() {
        return showActionButtons;
    }

    public String getApproveUrl() {
        return approveUrl;
    }

    public String getRejectUrl() {
        return rejectUrl;
    }

    public String getSendBackUrl() {
        return sendBackUrl;
    }

    public String getSendBackToInitiatorUrl() {
        return sendBackToInitiatorUrl;
    }

    public String getHoldUrl() {
        return holdUrl;
    }

    public String getUnholdUrl() {
        return unholdUrl;
    }

    public String getOpinionUrl() {
        return opinionUrl;
    }

    public boolean isFinalCompletion() {
        return finalCompletion;
    }

    public boolean isApprovalReminder() {
        return approvalReminder;
    }

    public String getStatusSubjectSuffix() {
        return statusSubjectSuffix;
    }

    public String getApproverSubjectSuffix() {
        return approverSubjectSuffix;
    }

    public String getMyApplicationUrl() {
        return myApplicationUrl;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public static final class Builder {
        private String recipientName;
        private Integer level;
        private String applicationCode;
        private String formName;
        private String status;
        private String remarks;
        private boolean showActionButtons;
        private String approveUrl;
        private String rejectUrl;
        private String sendBackUrl;
        private String sendBackToInitiatorUrl;
        private String holdUrl;
        private String unholdUrl;
        private String opinionUrl;
        private boolean finalCompletion;
        private boolean approvalReminder;
        private String statusSubjectSuffix;
        private String approverSubjectSuffix;
        private String myApplicationUrl;
        private String applicationName = "Qarshi DMS";

        public Builder recipientName(String recipientName) {
            this.recipientName = recipientName;
            return this;
        }

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder applicationCode(String applicationCode) {
            this.applicationCode = applicationCode;
            return this;
        }

        public Builder formName(String formName) {
            this.formName = formName;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Builder showActionButtons(boolean showActionButtons) {
            this.showActionButtons = showActionButtons;
            return this;
        }

        public Builder approveUrl(String approveUrl) {
            this.approveUrl = approveUrl;
            return this;
        }

        public Builder rejectUrl(String rejectUrl) {
            this.rejectUrl = rejectUrl;
            return this;
        }

        public Builder sendBackUrl(String sendBackUrl) {
            this.sendBackUrl = sendBackUrl;
            return this;
        }

        public Builder sendBackToInitiatorUrl(String sendBackToInitiatorUrl) {
            this.sendBackToInitiatorUrl = sendBackToInitiatorUrl;
            return this;
        }

        public Builder holdUrl(String holdUrl) {
            this.holdUrl = holdUrl;
            return this;
        }

        public Builder unholdUrl(String unholdUrl) {
            this.unholdUrl = unholdUrl;
            return this;
        }

        public Builder opinionUrl(String opinionUrl) {
            this.opinionUrl = opinionUrl;
            return this;
        }

        public Builder finalCompletion(boolean finalCompletion) {
            this.finalCompletion = finalCompletion;
            return this;
        }

        public Builder approvalReminder(boolean approvalReminder) {
            this.approvalReminder = approvalReminder;
            return this;
        }

        public Builder statusSubjectSuffix(String statusSubjectSuffix) {
            this.statusSubjectSuffix = statusSubjectSuffix;
            return this;
        }

        public Builder approverSubjectSuffix(String approverSubjectSuffix) {
            this.approverSubjectSuffix = approverSubjectSuffix;
            return this;
        }

        public Builder myApplicationUrl(String myApplicationUrl) {
            this.myApplicationUrl = myApplicationUrl;
            return this;
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public WorkflowEmailRenderRequest build() {
            return new WorkflowEmailRenderRequest(this);
        }
    }
}
