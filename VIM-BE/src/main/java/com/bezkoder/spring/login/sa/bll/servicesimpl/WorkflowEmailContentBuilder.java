package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.WorkflowEmailRenderRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class WorkflowEmailContentBuilder {

    public static final Set<String> RAW_HTML_PLACEHOLDER_KEYS = Collections.unmodifiableSet(new HashSet<String>() {
        {
            add("introMessage");
            add("remarksBlock");
            add("actionButtonsHtml");
            add("approvalHistoryHtml");
        }
    });

    public String resolveTemplateCode(WorkflowEmailRenderRequest request) {
        if (request.isApprovalReminder()) {
            return WorkflowEmailTemplateCodes.APPROVAL_REMINDER;
        }
        if (request.isFinalCompletion()) {
            return WorkflowEmailTemplateCodes.WORKFLOW_INITIATOR_COMPLETED;
        }
        String normalized = normalizeStatus(request.getStatus());
        if (request.isShowActionButtons()) {
            if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)
                    || "SENT_BACK_TO_INITIATOR".equals(normalized)) {
                return WorkflowEmailTemplateCodes.TEMPLATE_SENDBACK;
            }
            return WorkflowEmailTemplateCodes.TEMPLATE_APPROVAL_PENDING;
        }
        if ("REJECTED".equals(normalized)) {
            return WorkflowEmailTemplateCodes.TEMPLATE_REJECTED;
        }
        if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)
                || "SENT_BACK_TO_INITIATOR".equals(normalized)) {
            return WorkflowEmailTemplateCodes.TEMPLATE_SENDBACK;
        }
        if ("COMPLETED".equals(normalized)) {
            return WorkflowEmailTemplateCodes.WORKFLOW_INITIATOR_COMPLETED;
        }
        return WorkflowEmailTemplateCodes.TEMPLATE_APPROVED;
    }

    public Map<String, String> buildPlaceholders(WorkflowEmailRenderRequest request, String approvalHistoryHtml) {
        Map<String, String> map = new HashMap<>();
        String recipient = StringUtils.hasText(request.getRecipientName()) ? request.getRecipientName().trim() : "User";
        String formName = request.getFormName() != null ? request.getFormName() : "";
        String applicationCode = StringUtils.hasText(request.getApplicationCode()) ? request.getApplicationCode() : "N/A";
        Integer level = request.getLevel() != null ? request.getLevel() : 0;
        String status = request.getStatus();
        String statusLabel = resolveEmailStatusBadgeText(status);
        String applicationName = StringUtils.hasText(request.getApplicationName())
                ? request.getApplicationName().trim() : "Qarshi DMS";
        String myApplicationUrl = StringUtils.hasText(request.getMyApplicationUrl())
                ? request.getMyApplicationUrl().trim() : "";

        map.put("recipientName", recipient);
        map.put("formName", formName);
        map.put("applicationCode", applicationCode);
        map.put("approvalLevel", String.valueOf(level));
        map.put("statusLabel", statusLabel);
        map.put("applicationName", applicationName);
        map.put("myApplicationUrl", myApplicationUrl);
        map.put("introMessage", buildIntroMessageHtml(request));
        map.put("remarksBlock", buildRemarksBlockHtml(request.getRemarks()));
        map.put("actionButtonsHtml", buildActionButtonsHtml(request));
        map.put("approvalHistoryHtml", approvalHistoryHtml != null ? approvalHistoryHtml : "");

        String statusSuffix = request.getStatusSubjectSuffix();
        if (!StringUtils.hasText(statusSuffix)) {
            statusSuffix = resolveInitiatorSubjectSuffix(status);
        }
        map.put("statusSubjectSuffix", statusSuffix);

        String approverSuffix = request.getApproverSubjectSuffix();
        if (!StringUtils.hasText(approverSuffix)) {
            approverSuffix = resolveApproverSubjectSuffix(status, level);
        }
        map.put("approverSubjectSuffix", approverSuffix);

        return map;
    }

    public String buildIntroMessageHtml(WorkflowEmailRenderRequest request) {
        if (request.isApprovalReminder()) {
            if (StringUtils.hasText(request.getRemarks())) {
                return escapeHtml(request.getRemarks().trim());
            }
            return "This is a friendly reminder to review and take action on this application.";
        }
        boolean showActionButtons = request.isShowActionButtons();
        String status = request.getStatus();
        Integer level = request.getLevel() != null ? request.getLevel() : 0;
        String normalized = normalizeStatus(status);

        if (showActionButtons) {
            if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)) {
                return "This application has been <strong>sent back to you</strong> and now requires your action at Level "
                        + level + ".";
            }
            if ("SENT_BACK_TO_INITIATOR".equals(normalized)) {
                return "This application has been <strong>sent back to you as initiator</strong> and now requires your review.";
            }
            return "A new application is pending your approval at Level " + level + ".";
        }
        if ("SUBMITTED".equals(normalized)) {
            return "Your application has been submitted successfully and is now pending approval.";
        }
        if ("RESUBMITTED_BY_INITIATOR".equals(normalized)) {
            return "Your application has been resubmitted successfully and is now pending approval.";
        }
        if ("SENT_BACK_TO_INITIATOR".equals(normalized)) {
            return "Your application has been <strong>sent back to you</strong>."
                    + "<br><br>Please review the remarks and update your application accordingly.";
        }
        if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)) {
            return "Your application has been <strong>sent back for revision</strong> from Level " + level + "."
                    + "<br><br>Please review the remarks and update your application accordingly.";
        }
        if ("REJECTED".equals(normalized)) {
            return "Your application has been <strong>rejected</strong> at Level " + level + ".";
        }
        if ("IN_PROGRESS".equals(normalized)) {
            return "Your application has been approved at the previous level and is now pending approval at Level "
                    + level + ".";
        }
        if (request.isFinalCompletion()) {
            return "Your application has been fully processed and is now complete.";
        }
        return "Your application has been approved at Level " + level + ".";
    }

    public String buildRemarksBlockHtml(String remarks) {
        if (!StringUtils.hasText(remarks)) {
            return "";
        }
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px 20px;margin:20px 0;border-radius:4px;\">"
                + "<tr><td><strong>Remarks:</strong><br>" + escapeHtml(remarks.trim()) + "</td></tr></table>";
    }

    public String buildActionButtonsHtml(WorkflowEmailRenderRequest request) {
        if (!request.isShowActionButtons()) {
            return "";
        }
        String status = request.getStatus();
        boolean onHold = "HOLD".equalsIgnoreCase(status != null ? status.trim() : "");
        boolean canApproveReject = !onHold && request.getApproveUrl() != null && request.getRejectUrl() != null;
        boolean canSendBack = !onHold && request.getSendBackUrl() != null;
        boolean canSendBackToInitiator = !onHold && request.getSendBackToInitiatorUrl() != null;
        boolean canHold = !onHold && request.getHoldUrl() != null;
        boolean canUnhold = onHold && request.getUnholdUrl() != null;
        boolean canOpinion = !onHold && StringUtils.hasText(request.getOpinionUrl())
                && ("OPINION_PENDING".equalsIgnoreCase(status != null ? status.trim() : "")
                        || "OPINION_REQUESTED".equalsIgnoreCase(status != null ? status.trim() : ""));

        if (!canApproveReject && !canSendBack && !canSendBackToInitiator && !canHold && !canUnhold && !canOpinion) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:30px 0;\">");
        html.append("<tr><td align=\"center\" style=\"padding:10px 20px;\">");
        if (canOpinion) {
            appendOutlookButton(html, request.getOpinionUrl(), "Opinion", "#4361ee", "150px");
        }
        if (canApproveReject) {
            appendOutlookButton(html, request.getApproveUrl(), "Approve Application", "#27ae60", "180px");
        }
        if (canSendBack) {
            appendOutlookButton(html, request.getSendBackUrl(), "Send Back", "#f39c12", "150px");
        }
        if (canSendBackToInitiator) {
            appendOutlookButton(html, request.getSendBackToInitiatorUrl(), "Send Back to Initiator", "#c0392b", "270px");
        }
        if (canHold) {
            appendOutlookButton(html, request.getHoldUrl(), "Put on Hold", "#e67e22", "170px");
        }
        if (canUnhold) {
            appendOutlookButton(html, request.getUnholdUrl(), "Unhold", "#27ae60", "150px");
        }
        html.append("</td></tr></table>");
        if (canApproveReject || canSendBack || canSendBackToInitiator || canHold || canUnhold || canOpinion) {
            html.append("<p style=\"text-align:center;color:#7f8c8d;font-size:12px;margin-top:20px;\">"
                    + "You can also review this application in the system dashboard.</p>");
        }
        return html.toString();
    }

    private void appendOutlookButton(StringBuilder html, String url, String label, String color, String width) {
        html.append("<!--[if mso]>");
        html.append("<v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"")
                .append(url).append("\" style=\"height:44px;v-text-anchor:middle;width:").append(width)
                .append(";\" arcsize=\"12%\" strokecolor=\"").append(color).append("\" fillcolor=\"").append(color).append("\">");
        html.append("<w:anchorlock/><center style=\"color:#ffffff;font-family:sans-serif;font-size:16px;font-weight:600;\">")
                .append(escapeHtml(label)).append("</center>");
        html.append("</v:roundrect>");
        html.append("<![endif]-->");
        html.append("<!--[if !mso]><!-- -->");
        String extraStyle = "Send Back to Initiator".equals(label)
                ? "white-space:nowrap;min-width:270px;box-sizing:border-box;" : "";
        html.append("<a href=\"").append(url)
                .append("\" style=\"display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;background-color:")
                .append(color).append(";color:#ffffff !important;").append(extraStyle).append("\">")
                .append(escapeHtml(label)).append("</a>");
        html.append("<!--<![endif]-->");
    }

    public String resolveEmailStatusBadgeText(String status) {
        String normalized = normalizeStatus(status);
        if ("SENT_BACK_TO_INITIATOR".equals(normalized)) {
            return "PENDING";
        }
        if ("SUBMITTED".equals(normalized) || "RESUBMITTED_BY_INITIATOR".equals(normalized)
                || "SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)) {
            return "IN_PROGRESS";
        }
        if (normalized.isEmpty()) {
            return "PENDING";
        }
        return normalized;
    }

    public String resolveInitiatorSubjectSuffix(String status) {
        String normalized = normalizeStatus(status);
        if ("OPINION_PENDING".equals(normalized) || "OPINION_REQUESTED".equals(normalized)) {
            return "Pending Opinion";
        }
        if ("OPINION_APPROVED".equals(normalized) || "OPINION_REJECTED".equals(normalized)) {
            return "Opinion Submitted";
        }
        if ("SENT_BACK_TO_INITIATOR".equals(normalized)) {
            return "Sent Back To Initiator";
        }
        if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)) {
            return "Sent Back";
        }
        if ("REJECTED".equals(normalized)) {
            return "Rejected";
        }
        if ("SUBMITTED".equals(normalized)) {
            return "Submitted";
        }
        if ("RESUBMITTED_BY_INITIATOR".equals(normalized)) {
            return "Resubmitted";
        }
        if ("APPROVED".equals(normalized) || "COMPLETED".equals(normalized)) {
            return "Approved";
        }
        return "Updated";
    }

    public String resolveApproverSubjectSuffix(String status, int displayLevel) {
        String normalized = normalizeStatus(status);
        if ("OPINION_PENDING".equals(normalized) || "OPINION_REQUESTED".equals(normalized)) {
            return "Opinion Requested - Requires Your Action - Level " + displayLevel;
        }
        if ("OPINION_APPROVED".equals(normalized) || "OPINION_REJECTED".equals(normalized)) {
            return "Opinion Submitted - Requires Your Action - Level " + displayLevel;
        }
        if ("SENT_BACK".equals(normalized) || "REVISION_REQUIRED".equals(normalized)) {
            return "Sent Back - Requires Your Action - Level " + displayLevel;
        }
        if ("SENT_BACK_TO_INITIATOR".equals(normalized)) {
            return "Returned To Initiator";
        }
        return "Pending Approval - Level " + displayLevel;
    }

    private static String normalizeStatus(String status) {
        return status != null ? status.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
