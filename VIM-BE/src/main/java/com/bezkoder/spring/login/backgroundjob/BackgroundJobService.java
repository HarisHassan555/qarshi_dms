package com.bezkoder.spring.login.backgroundjob;

import com.bezkoder.spring.login.backgroundjob.dto.ChangePasswordEmailJobPayload;
import com.bezkoder.spring.login.backgroundjob.dto.DelegationNotifyEmailJobPayload;
import com.bezkoder.spring.login.backgroundjob.dto.TemplatePostApprovalEmailJobPayload;
import com.bezkoder.spring.login.backgroundjob.dto.UserCredentialEmailJobPayload;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class BackgroundJobService {

    private static final Logger logger = LogManager.getLogger(BackgroundJobService.class);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    @Value("${background.job.enabled:true}")
    private boolean enabled;

    @Autowired
    private CfgTblBackgroundJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private BackgroundJobService self;

    public boolean isEnabled() {
        return enabled;
    }

    @Transactional
    public void enqueueUserCredentialEmailOnCreate(UserCredentialEmailJobPayload payload, Integer createdByUserId) {
        if (payload == null || payload.getSerUserId() == null) {
            return;
        }
        String key = BackgroundJobTypes.USER_CREDENTIAL_EMAIL + ":CREATE:" + payload.getSerUserId();
        enqueue(BackgroundJobTypes.USER_CREDENTIAL_EMAIL, key, payload, createdByUserId);
    }

    /**
     * Queue delegation notify email in the current transaction (same pattern as user credential emails).
     * The job row commits with the delegation save; the worker runs after commit.
     */
    @Transactional
    public void enqueueDelegationNotifyEmail(DelegationNotifyEmailJobPayload payload, Integer triggeredByUserId) {
        if (payload == null || payload.getSerDelegationId() == null) {
            return;
        }
        String event = payload.isUpdated() ? "UPD" : "NEW";
        String key = BackgroundJobTypes.DELEGATION_NOTIFY_EMAIL + ":"
                + payload.getSerDelegationId() + ":" + event + ":" + Instant.now().toEpochMilli();
        enqueue(BackgroundJobTypes.DELEGATION_NOTIFY_EMAIL, key, payload, triggeredByUserId);
        logger.info("Queued DELEGATION_NOTIFY_EMAIL job for delegationId=" + payload.getSerDelegationId()
                + " key=" + key);
    }

    @Transactional
    public void enqueueDelegationNotifyEmailAfterCommit(
            DelegationNotifyEmailJobPayload payload, Integer triggeredByUserId) {
        if (payload == null || payload.getSerDelegationId() == null) {
            return;
        }
        String event = payload.isUpdated() ? "UPD" : "NEW";
        String key = BackgroundJobTypes.DELEGATION_NOTIFY_EMAIL + ":"
                + payload.getSerDelegationId() + ":" + event + ":" + Instant.now().toEpochMilli();
        enqueueAfterCommit(BackgroundJobTypes.DELEGATION_NOTIFY_EMAIL, key, payload, triggeredByUserId);
    }

    @Transactional
    public void enqueueUserCredentialEmailOnPasswordReset(UserCredentialEmailJobPayload payload, Integer triggeredByUserId) {
        if (payload == null || payload.getSerUserId() == null) {
            return;
        }
        String key = BackgroundJobTypes.USER_CREDENTIAL_EMAIL + ":PWD:" + payload.getSerUserId() + ":" + Instant.now().toEpochMilli();
        enqueue(BackgroundJobTypes.USER_CREDENTIAL_EMAIL, key, payload, triggeredByUserId);
    }

    @Transactional
    public void enqueueChangePasswordEmail(ChangePasswordEmailJobPayload payload, Integer triggeredByUserId) {
        if (payload == null || payload.getSerUserId() == null) {
            return;
        }
        String key = BackgroundJobTypes.CHANGE_PASSWORD_EMAIL + ":"
                + payload.getSerUserId() + ":" + Instant.now().toEpochMilli();
        enqueue(BackgroundJobTypes.CHANGE_PASSWORD_EMAIL, key, payload, triggeredByUserId);
        logger.info("Queued CHANGE_PASSWORD_EMAIL job for userId=" + payload.getSerUserId() + " key=" + key);
    }

    @Transactional
    public void enqueueTemplatePostApprovalEmail(TemplatePostApprovalEmailJobPayload payload, Integer triggeredByUserId) {
        if (payload == null || payload.getSerApplicationId() == null) {
            return;
        }
        String suffix = payload.hasAttachedPdfs() ? "PDF" : (payload.isUseStoredApplicationPdf() ? "STORED" : "STD");
        if (payload.isRefreshPdfBeforeEmail()) {
            suffix = suffix + ":REFRESH";
        }
        String key = BackgroundJobTypes.TEMPLATE_POST_APPROVAL_EMAIL + ":"
                + payload.getSerApplicationId() + ":" + suffix + ":" + Instant.now().toEpochMilli();
        enqueue(BackgroundJobTypes.TEMPLATE_POST_APPROVAL_EMAIL, key, payload, triggeredByUserId);
        logger.info("Queued TEMPLATE_POST_APPROVAL_EMAIL job for applicationId=" + payload.getSerApplicationId()
                + " key=" + key);
    }

    public void enqueueAfterCommit(String jobType, String idempotencyKey, Object payload, Integer createdByUserId) {
        if (!enabled) {
            return;
        }
        Runnable task = () -> {
            try {
                self.enqueueInNewTransaction(jobType, idempotencyKey, payload, createdByUserId);
                logger.info("Enqueued background job type=" + jobType + " key=" + idempotencyKey);
            } catch (Exception ex) {
                logger.error("Failed to enqueue background job after commit type=" + jobType
                        + " key=" + idempotencyKey + ": " + ex.getMessage(), ex);
                throw ex;
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Transactional
    public void enqueueInNewTransaction(String jobType, String idempotencyKey, Object payload, Integer createdByUserId) {
        enqueue(jobType, idempotencyKey, payload, createdByUserId);
    }

    @Transactional
    public void enqueue(String jobType, String idempotencyKey, Object payload, Integer createdByUserId) {
        if (!enabled) {
            return;
        }
        if (!org.springframework.util.StringUtils.hasText(jobType) || !org.springframework.util.StringUtils.hasText(idempotencyKey)) {
            return;
        }
        try {
            CfgTblBackgroundJob job = new CfgTblBackgroundJob();
            job.setTxtJobType(jobType);
            job.setTxtIdempotencyKey(idempotencyKey.trim());
            job.setTxtPayload(objectMapper.writeValueAsString(payload));
            job.setTxtStatus(BackgroundJobStatuses.PENDING);
            job.setIntAttempts(0);
            job.setIntMaxAttempts(DEFAULT_MAX_ATTEMPTS);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            job.setDteRunAfter(now);
            job.setSerCreatedUserId(createdByUserId);
            job.setBlIsDeleted(false);
            job.setBlnStatus(true);
            job.setDteCreatedDate(now);
            jobRepository.save(job);
        } catch (DataIntegrityViolationException dup) {
            logger.info("Background job skipped (duplicate idempotency key): " + idempotencyKey);
        } catch (Exception ex) {
            logger.error("Failed to enqueue background job " + jobType + ": " + ex.getMessage(), ex);
            throw new RuntimeException("Failed to enqueue background job", ex);
        }
    }
}
