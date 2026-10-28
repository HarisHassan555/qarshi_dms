package com.bezkoder.spring.login.backgroundjob.handlers;

import com.bezkoder.spring.login.backgroundjob.BackgroundJobHandler;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobTypes;
import com.bezkoder.spring.login.backgroundjob.dto.ChangePasswordEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.servicesimpl.ChangePasswordEmailSender;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChangePasswordEmailJobHandler implements BackgroundJobHandler {

    private static final Logger logger = LogManager.getLogger(ChangePasswordEmailJobHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChangePasswordEmailSender changePasswordEmailSender;

    @Override
    public String jobType() {
        return BackgroundJobTypes.CHANGE_PASSWORD_EMAIL;
    }

    @Override
    public void execute(CfgTblBackgroundJob job) throws Exception {
        ChangePasswordEmailJobPayload payload =
                objectMapper.readValue(job.getTxtPayload(), ChangePasswordEmailJobPayload.class);
        changePasswordEmailSender.sendChangePasswordEmail(payload);
        logger.info("CHANGE_PASSWORD_EMAIL job completed for userId=" + payload.getSerUserId());
    }
}
