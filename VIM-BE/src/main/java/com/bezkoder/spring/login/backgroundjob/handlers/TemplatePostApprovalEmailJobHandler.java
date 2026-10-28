package com.bezkoder.spring.login.backgroundjob.handlers;

import com.bezkoder.spring.login.backgroundjob.BackgroundJobHandler;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobTypes;
import com.bezkoder.spring.login.backgroundjob.dto.TemplatePostApprovalEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.servicesimpl.TemplatePostApprovalEmailProcessor;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TemplatePostApprovalEmailJobHandler implements BackgroundJobHandler {

    private static final Logger logger = LogManager.getLogger(TemplatePostApprovalEmailJobHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplatePostApprovalEmailProcessor templatePostApprovalEmailProcessor;

    @Override
    public String jobType() {
        return BackgroundJobTypes.TEMPLATE_POST_APPROVAL_EMAIL;
    }

    @Override
    public void execute(CfgTblBackgroundJob job) throws Exception {
        TemplatePostApprovalEmailJobPayload payload =
                objectMapper.readValue(job.getTxtPayload(), TemplatePostApprovalEmailJobPayload.class);
        templatePostApprovalEmailProcessor.process(payload);
        logger.info("TEMPLATE_POST_APPROVAL_EMAIL job completed for applicationId="
                + (payload.getSerApplicationId() != null ? payload.getSerApplicationId() : "unknown"));
    }
}
