package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
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
    private ICustomFormApplicationService customFormApplicationService;

    public void queuePostApprovalEmails(Integer applicationId, String contextLabel) {
        queuePostApprovalEmails(applicationId, contextLabel, false);
    }

    public void queuePostApprovalEmails(Integer applicationId, String contextLabel, boolean refreshPdfBeforeEmail) {
        if (applicationId == null) {
            return;
        }
        executorService.submit(() -> {
            try {
                if (refreshPdfBeforeEmail) {
                    String refreshStatus = customFormApplicationService.refreshTemplateApplicationPdfFromStage0(applicationId);
                    if (!"Success".equalsIgnoreCase(refreshStatus)) {
                        logger.warn("Failed to refresh template PDF before " + contextLabel + " for applicationId="
                                + applicationId + ": " + refreshStatus);
                    }
                }
                String status = customFormApplicationService.sendTemplatePostApprovalEmails(applicationId);
                if (!"Success".equalsIgnoreCase(status)) {
                    logger.warn("Failed to send " + contextLabel + " for applicationId=" + applicationId + ": " + status);
                }
            } catch (Exception ex) {
                logger.warn("Failed to send " + contextLabel + " for applicationId="
                        + applicationId + ": " + ex.getMessage(), ex);
            }
        });
    }

    public void queuePostApprovalEmailsWithPdfs(Integer applicationId, byte[] initiatorPdf, byte[] approverPdf,
            String pdfName, String pdfMime, String contextLabel) {
        if (applicationId == null) {
            return;
        }
        executorService.submit(() -> {
            try {
                String status = customFormApplicationService.sendTemplatePostApprovalEmails(applicationId,
                        initiatorPdf, approverPdf, pdfName, pdfMime);
                if (!"Success".equalsIgnoreCase(status)) {
                    logger.warn("Failed to send " + contextLabel + " for applicationId=" + applicationId + ": " + status);
                }
            } catch (Exception ex) {
                logger.warn("Failed to send " + contextLabel + " for applicationId="
                        + applicationId + ": " + ex.getMessage(), ex);
            }
        });
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
