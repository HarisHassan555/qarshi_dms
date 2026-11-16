package com.bezkoder.spring.login.sa.bll.servicesimpl;

/**
 * Optional metadata for {@link EmailDeliveryLogService} on the current thread.
 */
public final class EmailSendContext {

    private static final ThreadLocal<EmailSendContext> HOLDER = new ThreadLocal<>();

    private String triggerType = "SYSTEM";
    private String templateCode;
    private Integer applicationId;
    private Integer triggeredByUserId;
    private Long backgroundJobId;
    private String contextLabel;

    public static void set(EmailSendContext context) {
        HOLDER.set(context);
    }

    public static EmailSendContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static EmailSendContext forTrigger(String triggerType) {
        EmailSendContext ctx = new EmailSendContext();
        ctx.setTriggerType(triggerType);
        return ctx;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType != null && !triggerType.isBlank() ? triggerType.trim() : "SYSTEM";
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
}