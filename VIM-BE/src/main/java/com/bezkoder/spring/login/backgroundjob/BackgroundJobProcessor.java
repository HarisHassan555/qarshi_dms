package com.bezkoder.spring.login.backgroundjob;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJobRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
public class BackgroundJobProcessor {

    private static final Logger logger = LogManager.getLogger(BackgroundJobProcessor.class);

    @Autowired
    private CfgTblBackgroundJobRepository jobRepository;

    @Autowired
    private BackgroundJobHandlerRegistry handlerRegistry;

    @Transactional
    public void processJob(Long jobId, String workerId) {
        CfgTblBackgroundJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.isBlIsDeleted()) {
            return;
        }
        if (!BackgroundJobStatuses.PENDING.equals(job.getTxtStatus())) {
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (job.getDteRunAfter() != null && job.getDteRunAfter().after(now)) {
            return;
        }

        BackgroundJobHandler handler = handlerRegistry.getHandler(job.getTxtJobType());
        if (handler == null) {
            markFailed(job, now, "No handler registered for job type: " + job.getTxtJobType());
            return;
        }

        job.setTxtStatus(BackgroundJobStatuses.RUNNING);
        job.setDteLockedAt(now);
        job.setTxtLockedBy(workerId);
        job.setDteStartedAt(now);
        job.setDteModifiedDate(now);
        jobRepository.save(job);

        try {
            handler.execute(job);
            job.setTxtStatus(BackgroundJobStatuses.DONE);
            job.setTxtLastError(null);
            job.setDteCompletedAt(new Timestamp(System.currentTimeMillis()));
            job.setDteModifiedDate(job.getDteCompletedAt());
            jobRepository.save(job);
        } catch (Exception ex) {
            int attempt = job.getIntAttempts() + 1;
            job.setIntAttempts(attempt);
            job.setTxtLastError(ex.getMessage());
            job.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
            if (attempt >= job.getIntMaxAttempts()) {
                job.setTxtStatus(BackgroundJobStatuses.FAILED);
                job.setDteCompletedAt(job.getDteModifiedDate());
            } else {
                job.setTxtStatus(BackgroundJobStatuses.PENDING);
                long backoffMs = Math.min(300_000L, 5_000L * (1L << Math.min(attempt, 6)));
                job.setDteRunAfter(new Timestamp(System.currentTimeMillis() + backoffMs));
            }
            job.setDteLockedAt(null);
            job.setTxtLockedBy(null);
            jobRepository.save(job);
            logger.warn("Background job id=" + jobId + " attempt " + attempt + " failed: " + ex.getMessage(), ex);
        }
    }

    private void markFailed(CfgTblBackgroundJob job, Timestamp now, String message) {
        job.setTxtStatus(BackgroundJobStatuses.FAILED);
        job.setTxtLastError(message);
        job.setDteCompletedAt(now);
        job.setDteModifiedDate(now);
        jobRepository.save(job);
    }
}
