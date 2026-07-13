package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApprovalReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalReminderScheduler.class);

    private final CfgTblCustomFormApplicationDAO customFormApplicationDAO;

    public ApprovalReminderScheduler(CfgTblCustomFormApplicationDAO customFormApplicationDAO) {
        this.customFormApplicationDAO = customFormApplicationDAO;
    }

    @Scheduled(cron = "${app.approval-reminder.cron:0 0 * * * *}")
    public void sendPendingApprovalReminders() {
        try {
            customFormApplicationDAO.sendPendingApprovalReminderEmails();
        } catch (Exception e) {
            log.error("Approval reminder scheduler failed: {}", e.getMessage(), e);
        }
    }
}
