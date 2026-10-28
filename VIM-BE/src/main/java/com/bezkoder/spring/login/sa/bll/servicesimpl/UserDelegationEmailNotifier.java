package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.RenderedEmailMessage;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblUserDelegation;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class UserDelegationEmailNotifier {

    public static final String TEMPLATE_DELEGATION_DELEGATE = "DELEGATION_DELEGATE";
    public static final String TEMPLATE_DELEGATION_ON_BEHALF = "DELEGATION_ON_BEHALF";

    private static final Logger logger = LogManager.getLogger(UserDelegationEmailNotifier.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailTemplateRenderService emailTemplateRenderService;

    @Value("${app.frontend.base-url:http://localhost:4200/velocity}")
    private String frontendBaseUrl;

    public void notifyBothParties(
            CfgTblUserDelegation delegation,
            CfgTblUser delegate,
            CfgTblUser onBehalf,
            String documentsLabel,
            boolean updated) {
        try {
            notifyBothPartiesStrict(delegation, delegate, onBehalf, documentsLabel, updated);
        } catch (Exception ex) {
            logger.error("Delegation notification email failed: " + ex.getMessage(), ex);
        }
    }

    public void notifyBothPartiesStrict(
            CfgTblUserDelegation delegation,
            CfgTblUser delegate,
            CfgTblUser onBehalf,
            String documentsLabel,
            boolean updated) {
        if (delegation == null || delegate == null || onBehalf == null) {
            return;
        }
        String validFrom = delegation.getDteValidFrom() != null ? delegation.getDteValidFrom().toString() : "—";
        String validTo = delegation.getDteValidTo() != null ? delegation.getDteValidTo().toString() : "—";
        String status = delegation.getTxtStatus() != null ? delegation.getTxtStatus() : "ACTIVE";
        String docs = documentsLabel != null && !documentsLabel.trim().isEmpty() ? documentsLabel.trim() : "—";
        String notes = delegation.getTxtNotes() != null && !delegation.getTxtNotes().trim().isEmpty()
                ? delegation.getTxtNotes().trim()
                : null;
        String actionWord = updated ? "updated" : "created";
        String templateListUrl = buildTemplateListUrl();

        int sent = 0;
        sent += sendToDelegate(delegate, onBehalf, validFrom, validTo, status, docs, notes, actionWord, templateListUrl);
        sent += sendToOnBehalf(delegate, onBehalf, validFrom, validTo, status, docs, notes, actionWord, templateListUrl);
        if (sent == 0) {
            throw new IllegalStateException(
                    "Delegation notification could not be sent: delegate and on-behalf users have no valid email (txt_address)");
        }
    }

    private int sendToDelegate(
            CfgTblUser delegate,
            CfgTblUser onBehalf,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String actionWord,
            String templateListUrl) {
        String email = resolveEmail(delegate);
        if (email == null) {
            logger.warn("Delegation notify skipped for delegate (no email): userId="
                    + delegate.getSerUserId());
            return 0;
        }
        String delegateName = displayName(delegate);
        String onBehalfName = displayName(onBehalf);
        Map<String, String> placeholders = emailTemplateRenderService.buildDelegationPlaceholders(
                delegateName, delegateName, onBehalfName, validFrom, validTo, status,
                documentsLabel, notes, actionWord, templateListUrl);

        if (sendFromTemplate(TEMPLATE_DELEGATION_DELEGATE, placeholders, email, true)) {
            return 1;
        }

        String subject = UserDelegationEmailTemplateBuilder.subjectForDelegate(actionWord, onBehalfName);
        String html = UserDelegationEmailTemplateBuilder.buildDelegateEmail(
                delegateName, onBehalfName, validFrom, validTo, status, documentsLabel, notes,
                actionWord, templateListUrl);
        sendStrict(Collections.singletonList(email), subject, html);
        return 1;
    }

    private int sendToOnBehalf(
            CfgTblUser delegate,
            CfgTblUser onBehalf,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String actionWord,
            String templateListUrl) {
        String email = resolveEmail(onBehalf);
        if (email == null) {
            logger.warn("Delegation notify skipped for on-behalf user (no email): userId="
                    + onBehalf.getSerUserId());
            return 0;
        }
        String delegateName = displayName(delegate);
        String onBehalfName = displayName(onBehalf);
        Map<String, String> placeholders = emailTemplateRenderService.buildDelegationPlaceholders(
                onBehalfName, delegateName, onBehalfName, validFrom, validTo, status,
                documentsLabel, notes, actionWord, templateListUrl);

        if (sendFromTemplate(TEMPLATE_DELEGATION_ON_BEHALF, placeholders, email, true)) {
            return 1;
        }

        String subject = UserDelegationEmailTemplateBuilder.subjectForOnBehalf(actionWord, delegateName);
        String html = UserDelegationEmailTemplateBuilder.buildOnBehalfEmail(
                onBehalfName, delegateName, validFrom, validTo, status, documentsLabel, notes,
                actionWord, templateListUrl);
        sendStrict(Collections.singletonList(email), subject, html);
        return 1;
    }

    private boolean sendFromTemplate(
            String templateCode, Map<String, String> placeholders, String recipientEmail, boolean strict) {
        Optional<RenderedEmailMessage> rendered =
                emailTemplateRenderService.renderActiveTemplate(templateCode, placeholders);
        if (!rendered.isPresent()) {
            logger.info("Delegation email template not active: " + templateCode + "; using legacy HTML");
            return false;
        }
        RenderedEmailMessage message = rendered.get();
        if (strict) {
            sendStrict(Collections.singletonList(recipientEmail), message.getSubject(), message.getHtmlBody());
        } else {
            sendSafe(Collections.singletonList(recipientEmail), message.getSubject(), message.getHtmlBody());
        }
        return true;
    }

    private void sendStrict(List<String> recipients, String subject, String html) {
        emailService.sendHtmlEmailStrict(recipients, subject, html);
    }

    private String buildTemplateListUrl() {
        String base = frontendBaseUrl != null ? frontendBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return "";
        }
        return base + "/template-list";
    }

    private void sendSafe(List<String> recipients, String subject, String html) {
        try {
            emailService.sendHtmlEmail(recipients, subject, html);
        } catch (Exception ex) {
            logger.error("Delegation notification email failed: " + ex.getMessage(), ex);
        }
    }

    private String resolveEmail(CfgTblUser user) {
        if (user == null || user.getTxtAddress() == null) {
            return null;
        }
        String trimmed = user.getTxtAddress().trim();
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            return null;
        }
        return trimmed;
    }

    private String displayName(CfgTblUser user) {
        if (user == null) {
            return "User";
        }
        if (user.getTxtUserName() != null && !user.getTxtUserName().trim().isEmpty()) {
            return user.getTxtUserName().trim();
        }
        if (user.getTxtLoginName() != null && !user.getTxtLoginName().trim().isEmpty()) {
            return user.getTxtLoginName().trim();
        }
        return "User #" + user.getSerUserId();
    }
}
