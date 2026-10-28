package com.bezkoder.spring.login.notification.bll;

import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.notification.bll.dto.UserNotificationSaveRequest;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class TemplateWorkflowNotificationService {

    private static final Logger logger = LogManager.getLogger(TemplateWorkflowNotificationService.class);

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private IUserService userService;

    public void notifyApproved(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String status = normalizeStatus(application.getTxtStatus());
            Integer initiatorId = application.getSerSubmittedBy();
            Integer currentApproverId = application.getSerCurrentApprover();

            if (initiatorId != null && initiatorId > 0) {
                String title;
                String message;
                if ("COMPLETED".equals(status)) {
                    title = "Document completed";
                    message = docLabel + " has been fully approved and completed.";
                } else {
                    title = "Document approved";
                    message = docLabel + " was approved by " + actorName + ".";
                }
                if (StringUtils.hasText(remarks)) {
                    message = message + " Remarks: " + remarks.trim();
                }
                send(initiatorId, application, "APPROVED", title, message, deepLinkForInitiator(application), actorUserId);
            }

            if ("IN_PROGRESS".equals(status) && currentApproverId != null && currentApproverId > 0) {
                if (!currentApproverId.equals(actorUserId)) {
                    String title = "Approval pending";
                    String message = docLabel + " is pending your approval.";
                    send(currentApproverId, application, "APPROVAL_PENDING", title, message,
                            deepLinkForApprover(application), actorUserId);
                }
            }
        } catch (Exception ex) {
            logger.warn("Template approve notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyRejected(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId == null || initiatorId <= 0) {
                return;
            }
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String message = docLabel + " was rejected by " + actorName + ".";
            if (StringUtils.hasText(remarks)) {
                message = message + " Remarks: " + remarks.trim();
            }
            send(initiatorId, application, "REJECTED", "Document rejected", message,
                    deepLinkForInitiator(application), actorUserId);
        } catch (Exception ex) {
            logger.warn("Template reject notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifySentBack(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String baseMessage = docLabel + " was sent back by " + actorName + ".";
            if (StringUtils.hasText(remarks)) {
                baseMessage = baseMessage + " Remarks: " + remarks.trim();
            }

            Set<Integer> recipients = new LinkedHashSet<>();
            if (application.getSerSubmittedBy() != null && application.getSerSubmittedBy() > 0) {
                recipients.add(application.getSerSubmittedBy());
            }
            if (application.getSerCurrentApprover() != null && application.getSerCurrentApprover() > 0) {
                recipients.add(application.getSerCurrentApprover());
            }

            for (Integer recipientId : recipients) {
                boolean isApprover = application.getSerCurrentApprover() != null
                        && application.getSerCurrentApprover().equals(recipientId)
                        && !recipientId.equals(application.getSerSubmittedBy());
                String title = isApprover ? "Approval pending" : "Document sent back";
                String eventType = isApprover ? "APPROVAL_PENDING" : "SENT_BACK";
                String message = isApprover ? docLabel + " was sent back and is pending your approval again." : baseMessage;
                String deepLink = isApprover ? deepLinkForApprover(application) : deepLinkForInitiator(application);
                send(recipientId, application, eventType, title, message, deepLink, actorUserId);
            }
        } catch (Exception ex) {
            logger.warn("Template send-back notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifySubmitted(CfgTblCustomFormApplication application, Integer actorUserId) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String status = normalizeStatus(application.getTxtStatus());
            Integer initiatorId = application.getSerSubmittedBy();

            if (initiatorId != null && initiatorId > 0) {
                String title = "COMPLETED".equals(status) ? "Document completed" : "Document submitted";
                String message = "COMPLETED".equals(status)
                        ? docLabel + " was submitted and completed (no approval steps required)."
                        : docLabel + " was submitted successfully.";
                String eventType = "COMPLETED".equals(status) ? "COMPLETED" : "SUBMITTED";
                send(initiatorId, application, eventType, title, message, deepLinkForInitiator(application), actorUserId);
            }

            notifyCurrentApproverPending(application, actorUserId, docLabel + " is pending your approval.");
        } catch (Exception ex) {
            logger.warn("Template submit notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyResubmitted(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId != null && initiatorId > 0) {
                String message = docLabel + " was resubmitted by the initiator.";
                message = appendRemarks(message, remarks);
                send(initiatorId, application, "RESUBMITTED", "Document resubmitted", message,
                        deepLinkForInitiator(application), actorUserId);
            }
            String pendingMessage = docLabel + " was resubmitted and is pending your approval.";
            pendingMessage = appendRemarks(pendingMessage, remarks);
            notifyCurrentApproverPending(application, actorUserId, pendingMessage);
        } catch (Exception ex) {
            logger.warn("Template resubmit notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyOpinionRequested(CfgTblCustomFormApplication application, Integer actorUserId,
            Integer opinionUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            Integer initiatorId = application.getSerSubmittedBy();

            if (initiatorId != null && initiatorId > 0) {
                String message = docLabel + " was sent for opinion by " + actorName + ".";
                message = appendRemarks(message, remarks);
                send(initiatorId, application, "OPINION_REQUESTED", "Opinion requested", message,
                        deepLinkForInitiator(application), actorUserId);
            }

            if (opinionUserId != null && opinionUserId > 0) {
                String message = docLabel + " requires your opinion.";
                message = appendRemarks(message, remarks);
                send(opinionUserId, application, "OPINION_PENDING", "Opinion pending", message,
                        deepLinkForApprover(application), actorUserId);
            }
        } catch (Exception ex) {
            logger.warn("Template opinion-request notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyOpinionSubmitted(CfgTblCustomFormApplication application, Integer actorUserId,
            String opinionAction, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            boolean rejected = "reject".equalsIgnoreCase(String.valueOf(opinionAction).trim());
            String eventType = rejected ? "OPINION_REJECTED" : "OPINION_APPROVED";
            String actionLabel = rejected ? "rejected (opinion)" : "approved (opinion)";

            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId != null && initiatorId > 0) {
                String message = docLabel + " opinion was " + actionLabel + " by " + actorName + ".";
                message = appendRemarks(message, remarks);
                send(initiatorId, application, eventType, "Opinion submitted", message,
                        deepLinkForInitiator(application), actorUserId);
            }

            Integer returnToApproverId = application.getSerCurrentApprover();
            if (returnToApproverId != null && returnToApproverId > 0 && !returnToApproverId.equals(actorUserId)) {
                String message = docLabel + " opinion was " + actionLabel
                        + " by " + actorName + ". The document is back with you for approval.";
                message = appendRemarks(message, remarks);
                send(returnToApproverId, application, "APPROVAL_PENDING", "Opinion completed", message,
                        deepLinkForApprover(application), actorUserId);
            }
        } catch (Exception ex) {
            logger.warn("Template opinion-submit notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyHold(CfgTblCustomFormApplication application, Integer actorUserId, String reason) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String message = docLabel + " was put on hold by " + actorName + ".";
            message = appendRemarks(message, reason);

            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId != null && initiatorId > 0) {
                send(initiatorId, application, "HOLD", "Document on hold", message,
                        deepLinkForInitiator(application), actorUserId);
            }
        } catch (Exception ex) {
            logger.warn("Template hold notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifyUnhold(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String status = normalizeStatus(application.getTxtStatus());

            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId != null && initiatorId > 0) {
                String message = docLabel + " was taken off hold by " + actorName + ".";
                message = appendRemarks(message, remarks);
                send(initiatorId, application, "UNHOLD", "Document off hold", message,
                        deepLinkForInitiator(application), actorUserId);
            }

            if ("IN_PROGRESS".equals(status) || "PENDING".equals(status)) {
                String pendingMessage = docLabel + " is pending your approval again.";
                pendingMessage = appendRemarks(pendingMessage, remarks);
                notifyCurrentApproverPending(application, actorUserId, pendingMessage);
            }
        } catch (Exception ex) {
            logger.warn("Template unhold notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    public void notifySentBackToInitiator(CfgTblCustomFormApplication application, Integer actorUserId, String remarks) {
        if (application == null) {
            return;
        }
        try {
            Integer initiatorId = application.getSerSubmittedBy();
            if (initiatorId == null || initiatorId <= 0) {
                return;
            }
            String docLabel = documentLabel(application);
            String actorName = resolveActorName(actorUserId);
            String message = docLabel + " was sent back to you for correction by " + actorName + ".";
            if (StringUtils.hasText(remarks)) {
                message = message + " Remarks: " + remarks.trim();
            }
            send(initiatorId, application, "SENT_BACK_TO_INITIATOR", "Sent back to initiator", message,
                    deepLinkForInitiator(application), actorUserId);
        } catch (Exception ex) {
            logger.warn("Template send-back-to-initiator notification failed for applicationId="
                    + application.getSerApplicationId() + ": " + ex.getMessage(), ex);
        }
    }

    private void notifyCurrentApproverPending(CfgTblCustomFormApplication application, Integer actorUserId,
            String message) {
        if (application == null) {
            return;
        }
        String status = normalizeStatus(application.getTxtStatus());
        if (!"IN_PROGRESS".equals(status) && !"PENDING".equals(status)) {
            return;
        }
        Integer approverId = application.getSerCurrentApprover();
        if (approverId == null || approverId <= 0 || approverId.equals(actorUserId)) {
            return;
        }
        send(approverId, application, "APPROVAL_PENDING", "Approval pending", message,
                deepLinkForApprover(application), actorUserId);
    }

    private String appendRemarks(String message, String remarks) {
        if (!StringUtils.hasText(remarks)) {
            return message;
        }
        return message + " Remarks: " + remarks.trim();
    }

    private void send(Integer recipientUserId, CfgTblCustomFormApplication application, String eventType,
            String title, String message, String deepLink, Integer actorUserId) {
        UserNotificationSaveRequest request = new UserNotificationSaveRequest();
        request.setSerApplicationId(application.getSerApplicationId());
        request.setSerFormId(application.getSerFormId());
        request.setTxtEventType(eventType);
        request.setTxtTitle(title);
        request.setTxtMessage(message);
        request.setTxtDeepLink(deepLink);
        userNotificationService.createSystemNotification(recipientUserId, request, actorUserId);
    }

    private String documentLabel(CfgTblCustomFormApplication application) {
        if (application.getTxtFormCode() != null && !application.getTxtFormCode().trim().isEmpty()) {
            return application.getTxtFormCode().trim();
        }
        if (application.getSerApplicationId() != null) {
            return "Application #" + application.getSerApplicationId();
        }
        return "Document";
    }

    private String deepLinkForApprover(CfgTblCustomFormApplication application) {
        return "template-approval/" + application.getSerApplicationId();
    }

    private String deepLinkForInitiator(CfgTblCustomFormApplication application) {
        return "my-application/" + application.getSerApplicationId();
    }

    private String normalizeStatus(String status) {
        return status != null ? status.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String resolveActorName(Integer actorUserId) {
        if (actorUserId == null || actorUserId <= 0) {
            return "A user";
        }
        try {
            CfgTblUser user = userService.getUserById(actorUserId);
            if (user != null && StringUtils.hasText(user.getTxtUserName())) {
                return user.getTxtUserName().trim();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "User #" + actorUserId;
    }
}
