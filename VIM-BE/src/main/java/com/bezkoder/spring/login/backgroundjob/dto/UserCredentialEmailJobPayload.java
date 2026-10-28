package com.bezkoder.spring.login.backgroundjob.dto;

/**
 * Sent asynchronously after user create or password reset. Contains a one-time plain password for email body.
 */
public class UserCredentialEmailJobPayload {

    private Integer serUserId;
    private String recipientEmail;
    private String displayName;
    private String loginName;
    private String plainPassword;
    private boolean welcomeEmail;

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

    public boolean isWelcomeEmail() {
        return welcomeEmail;
    }

    public void setWelcomeEmail(boolean welcomeEmail) {
        this.welcomeEmail = welcomeEmail;
    }
}
