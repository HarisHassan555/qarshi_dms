package com.bezkoder.spring.login.backgroundjob;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJobRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;

@Component
public class BackgroundJobWorker {

    private static final Logger logger = LogManager.getLogger(BackgroundJobWorker.class);

    @Value("${background.job.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${background.job.batch-size:10}")
    private int batchSize;

    @Autowired
    private CfgTblBackgroundJobRepository jobRepository;

    @Autowired
    private BackgroundJobProcessor jobProcessor;

    private volatile String workerId;

    @Scheduled(fixedDelayString = "${background.job.poll-interval-ms:5000}")
    public void pollAndRun() {
        if (!workerEnabled) {
            return;
        }
        ensureWorkerId();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<CfgTblBackgroundJob> due = jobRepository.findDuePending(now, PageRequest.of(0, Math.max(1, batchSize)));
        for (CfgTblBackgroundJob job : due) {
            try {
                jobProcessor.processJob(job.getSerBackgroundJobId(), workerId);
            } catch (Exception ex) {
                logger.warn("Unexpected error processing job id=" + job.getSerBackgroundJobId() + ": " + ex.getMessage(), ex);
            }
        }
    }

    private void ensureWorkerId() {
        if (workerId != null) {
            return;
        }
        try {
            String host = InetAddress.getLocalHost().getHostName();
            workerId = host + "-" + ProcessHandle.current().pid();
        } catch (Exception ex) {
            workerId = "worker-" + ProcessHandle.current().pid();
        }
    }
}
