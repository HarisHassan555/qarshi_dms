package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.backgroundjob.BackgroundJobService;
import com.bezkoder.spring.login.backgroundjob.dto.TemplatePostApprovalEmailJobPayload;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TemplateEmailDispatchService {

    private static final int THREAD_COUNT = 3;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final Logger logger = LogManager.getLogger(TemplateEmailDispatchService.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT, new TemplateEmailThreadFactory());

    @Autowired
    private BackgroundJobService backgroundJobService;

    @Autowired
    private TemplatePostApprovalEmailProcessor templatePostApprovalEmailProcessor;

    public void queuePostApprovalEmails(Integer applicationId, String contextLabel) {
        queuePostApprovalEmails(applicationId, contextLabel, false);
    }

    public void queuePostApprovalEmails(Integer applicationId, String contextLabel, boolean refreshPdfBeforeEmail) {
        if (applicationId == null) {
            return;
        }
        TemplatePostApprovalEmailJobPayload payload = new TemplatePostApprovalEmailJobPayload();
        payload.setSerApplicationId(applicationId);
        payload.setContextLabel(contextLabel);
        payload.setRefreshPdfBeforeEmail(refreshPdfBeforeEmail);
        payload.setUseStoredApplicationPdf(true);
        dispatch(payload);
    }

    public void queuePostApprovalEmailsWithPdfs(Integer applicationId, byte[] initiatorPdf, byte[] approverPdf,
            String pdfName, String pdfMime, String contextLabel) {
        if (applicationId == null) {
            return;
        }
        TemplatePostApprovalEmailJobPayload payload = new TemplatePostApprovalEmailJobPayload();
        payload.setSerApplicationId(applicationId);
        payload.setContextLabel(contextLabel);
        payload.setPdfName(pdfName);
        payload.setPdfMime(pdfMime);
        // PDF is already persisted via updateApplicationPdf before this call; avoid huge job payloads.
        payload.setUseStoredApplicationPdf(true);
        dispatch(payload);
    }

    private void dispatch(TemplatePostApprovalEmailJobPayload payload) {
        if (backgroundJobService != null && backgroundJobService.isEnabled()) {
            try {
                backgroundJobService.enqueueTemplatePostApprovalEmail(payload, null);
                return;
            } catch (Exception queueEx) {
                logger.warn("Could not queue TEMPLATE_POST_APPROVAL_EMAIL for applicationId="
                        + payload.getSerApplicationId() + ", running async fallback: " + queueEx.getMessage(), queueEx);
            }
        } else {
            logger.info("background.job.enabled is false; template post-approval email for applicationId="
                    + payload.getSerApplicationId() + " will run on thread pool (no cfg_tbl_background_job row)");
        }
        executorService.submit(() -> runPayload(payload));
    }

    private void runPayload(TemplatePostApprovalEmailJobPayload payload) {
        try {
            templatePostApprovalEmailProcessor.process(payload);
        } catch (Exception ex) {
            String label = payload.getContextLabel() != null ? payload.getContextLabel() : "template email";
            logger.warn("Failed to send " + label + " for applicationId=" + payload.getSerApplicationId()
                    + ": " + ex.getMessage(), ex);
        }
    }

    private static String encodeBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class TemplateEmailThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "template-email-dispatch-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
