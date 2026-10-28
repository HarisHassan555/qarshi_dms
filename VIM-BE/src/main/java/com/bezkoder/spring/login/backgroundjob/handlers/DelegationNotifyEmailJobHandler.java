package com.bezkoder.spring.login.backgroundjob.handlers;

import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobHandler;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobTypes;
import com.bezkoder.spring.login.backgroundjob.dto.DelegationNotifyEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.servicesimpl.UserDelegationEmailNotifier;
import com.bezkoder.spring.login.sa.dal.UserDelegationRepository;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblUserDelegation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DelegationNotifyEmailJobHandler implements BackgroundJobHandler {

    private static final Logger logger = LogManager.getLogger(DelegationNotifyEmailJobHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDelegationRepository delegationRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserDelegationEmailNotifier delegationEmailNotifier;

    @Override
    public String jobType() {
        return BackgroundJobTypes.DELEGATION_NOTIFY_EMAIL;
    }

    @Override
    public void execute(CfgTblBackgroundJob job) throws Exception {
        DelegationNotifyEmailJobPayload payload =
                objectMapper.readValue(job.getTxtPayload(), DelegationNotifyEmailJobPayload.class);
        if (payload.getSerDelegationId() == null || payload.getSerDelegationId() <= 0) {
            throw new IllegalArgumentException("serDelegationId is required for DELEGATION_NOTIFY_EMAIL");
        }

        CfgTblUserDelegation delegation = delegationRepository.findActiveById(payload.getSerDelegationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Delegation not found: " + payload.getSerDelegationId()));

        CfgTblUser delegate = userService.getUserById(delegation.getSerDelegateUserId());
        CfgTblUser onBehalf = userService.getUserById(delegation.getSerOnBehalfUserId());
        if (delegate == null || onBehalf == null) {
            throw new IllegalStateException("Delegate or on-behalf user not found for delegation "
                    + payload.getSerDelegationId());
        }

        String documentsLabel = payload.getDocumentsLabel();
        if (!StringUtils.hasText(documentsLabel)) {
            documentsLabel = "—";
        }

        delegationEmailNotifier.notifyBothPartiesStrict(
                delegation, delegate, onBehalf, documentsLabel.trim(), payload.isUpdated());
        logger.info("DELEGATION_NOTIFY_EMAIL job completed for delegationId=" + payload.getSerDelegationId());
    }
}
