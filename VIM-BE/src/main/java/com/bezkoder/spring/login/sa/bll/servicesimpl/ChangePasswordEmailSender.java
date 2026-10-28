package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.backgroundjob.dto.ChangePasswordEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.dto.RenderedEmailMessage;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class ChangePasswordEmailSender {

    public static final String TEMPLATE_USER_CHANGE_PASSWORD = "USER_CHANGE_PASSWORD";

    private static final Logger logger = LogManager.getLogger(ChangePasswordEmailSender.class);

    @Autowired
    private EmailTemplateRenderService emailTemplateRenderService;

    @Autowired
    private EmailService emailService;

    public void sendChangePasswordEmail(ChangePasswordEmailJobPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.getRecipientEmail())) {
            throw new IllegalArgumentException("recipientEmail is required");
        }
        if (!StringUtils.hasText(payload.getPlainPassword())) {
            throw new IllegalArgumentException("plainPassword is required");
        }

        String loginName = payload.getLoginName();
        if (!StringUtils.hasText(loginName)) {
            loginName = payload.getRecipientEmail().trim();
        }
        String displayName = StringUtils.hasText(payload.getDisplayName()) ? payload.getDisplayName() : loginName;

        Map<String, String> placeholders = emailTemplateRenderService.buildChangePasswordPlaceholders(
                displayName, loginName, payload.getPlainPassword(), payload.getRecipientEmail(),
                payload.isAdminInitiated());

        Optional<RenderedEmailMessage> rendered =
                emailTemplateRenderService.renderActiveTemplate(TEMPLATE_USER_CHANGE_PASSWORD, placeholders);

        if (rendered.isPresent()) {
            RenderedEmailMessage message = rendered.get();
            emailService.sendHtmlEmailStrict(
                    Collections.singletonList(payload.getRecipientEmail().trim()),
                    message.getSubject(),
                    message.getHtmlBody());
            logger.info("Sent change-password email using template for userId=" + payload.getSerUserId());
            return;
        }

        logger.warn("No active email template for " + TEMPLATE_USER_CHANGE_PASSWORD
                + "; using legacy password email for userId=" + payload.getSerUserId());
        emailService.sendPassordinMail(payload.getRecipientEmail(), loginName, payload.getPlainPassword());
    }
}
