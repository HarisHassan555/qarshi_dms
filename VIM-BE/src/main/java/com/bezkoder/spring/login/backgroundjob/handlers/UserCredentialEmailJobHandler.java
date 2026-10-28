package com.bezkoder.spring.login.backgroundjob.handlers;

import com.bezkoder.spring.login.backgroundjob.BackgroundJobHandler;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobTypes;
import com.bezkoder.spring.login.backgroundjob.dto.UserCredentialEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.servicesimpl.UserCredentialEmailSender;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCredentialEmailJobHandler implements BackgroundJobHandler {

    private static final Logger logger = LogManager.getLogger(UserCredentialEmailJobHandler.class);

    @Autowired
    private UserCredentialEmailSender userCredentialEmailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String jobType() {
        return BackgroundJobTypes.USER_CREDENTIAL_EMAIL;
    }

    @Override
    public void execute(CfgTblBackgroundJob job) throws Exception {
        UserCredentialEmailJobPayload payload = objectMapper.readValue(job.getTxtPayload(), UserCredentialEmailJobPayload.class);
        userCredentialEmailSender.sendCredentialEmail(payload);
        logger.info("USER_CREDENTIAL_EMAIL job completed for userId=" + payload.getSerUserId());
    }
}
