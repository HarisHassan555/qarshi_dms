package com.bezkoder.spring.login.backgroundjob.dto;

/**
 * Queued after password change (Change Password page). Contains one-time plain password for the email body.
 */
public class ChangePasswordEmailJobPayload {

    private Integer serUserId;
    private String recipientEmail;
    private String displayName;
    private String loginName;
    private String plainPassword;
    /** True when an administrator reset the password (no old password supplied). */
    private boolean adminInitiated;
    /** Browser origin captured when the email was queued, for example http://192.0.0.203:8080/velocity. */
    private String publicBaseUrl;

    public Integer getSerUserId() {
        return serUserId;
    }

    public void setSerUserId(Integer serUserId) {
        this.serUserId = serUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    public boolean isAdminInitiated() {
        return adminInitiated;
    }

    public void setAdminInitiated(boolean adminInitiated) {
        this.adminInitiated = adminInitiated;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
