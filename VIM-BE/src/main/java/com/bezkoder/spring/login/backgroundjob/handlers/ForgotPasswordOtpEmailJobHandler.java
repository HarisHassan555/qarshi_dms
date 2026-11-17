package com.bezkoder.spring.login.backgroundjob.handlers;

import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobHandler;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobTypes;
import com.bezkoder.spring.login.backgroundjob.dto.ForgotPasswordOtpEmailJobPayload;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ForgotPasswordOtpEmailJobHandler implements BackgroundJobHandler {

    private static final Logger logger = LogManager.getLogger(ForgotPasswordOtpEmailJobHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    @Override
    public String jobType() {
        return BackgroundJobTypes.FORGOT_PASSWORD_OTP_EMAIL;
    }

    @Override
    public void execute(CfgTblBackgroundJob job) throws Exception {
        ForgotPasswordOtpEmailJobPayload payload =
                objectMapper.readValue(job.getTxtPayload(), ForgotPasswordOtpEmailJobPayload.class);
        String email = payload.getRecipientEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing recipient email");
        }
        String displayName = payload.getDisplayName() != null ? payload.getDisplayName() : "User";
        String otp = payload.getOtp() != null ? payload.getOtp() : "";
        String body = "Hello " + displayName + ",\n\n"
                + "Your password reset OTP is: " + otp + "\n"
                + "This code expires in 10 minutes.\n\n"
                + "If you did not request this, you can ignore this email.\n";
        emailService.sendEmail(Collections.singletonList(email.trim()), "Password reset OTP", body);
        logger.info("FORGOT_PASSWORD_OTP_EMAIL job completed for " + email);
    }
}