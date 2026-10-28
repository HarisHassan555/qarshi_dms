package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.backgroundjob.dto.TemplatePostApprovalEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Base64;

@Component
public class TemplatePostApprovalEmailProcessor {

    private static final Logger logger = LogManager.getLogger(TemplatePostApprovalEmailProcessor.class);

    @Autowired
    private ICustomFormApplicationService customFormApplicationService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public void process(TemplatePostApprovalEmailJobPayload payload) {
        if (payload == null || payload.getSerApplicationId() == null) {
            throw new IllegalArgumentException("serApplicationId is required for template post-approval email job");
        }
        Integer applicationId = payload.getSerApplicationId();
        String contextLabel = StringUtils.hasText(payload.getContextLabel())
                ? payload.getContextLabel().trim()
                : "template post-approval email";

        if (payload.isRefreshPdfBeforeEmail()) {
            String refreshStatus = customFormApplicationService.refreshTemplateApplicationPdfFromStage0(applicationId);
            if (!"Success".equalsIgnoreCase(refreshStatus)) {
                logger.warn("Failed to refresh template PDF before " + contextLabel + " for applicationId="
                        + applicationId + ": " + refreshStatus);
            }
        }

        String status;
        if (payload.hasAttachedPdfs()) {
            byte[] initiatorBytes = decodeBase64(payload.getInitiatorPdfBase64());
            byte[] approverBytes = decodeBase64(payload.getApproverPdfBase64());
            String pdfName = resolvePdfName(payload);
            String pdfMime = resolvePdfMime(payload);
            status = customFormApplicationService.sendTemplatePostApprovalEmails(
                    applicationId, initiatorBytes, approverBytes, pdfName, pdfMime);
        } else if (payload.isUseStoredApplicationPdf()) {
            StoredApplicationPdf stored = loadStoredApplicationPdf(applicationId);
            byte[] pdfBytes = stored.pdfBytes;
            String pdfName = StringUtils.hasText(payload.getPdfName()) ? payload.getPdfName().trim() : stored.pdfName;
            String pdfMime = StringUtils.hasText(payload.getPdfMime()) ? payload.getPdfMime().trim() : stored.pdfMime;
            status = customFormApplicationService.sendTemplatePostApprovalEmails(
                    applicationId, pdfBytes, pdfBytes, pdfName, pdfMime);
        } else {
            status = customFormApplicationService.sendTemplatePostApprovalEmails(applicationId);
        }

        if (!"Success".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Template post-approval email failed for applicationId=" + applicationId
                    + " (" + contextLabel + "): " + status);
        }
        logger.info("Template post-approval email completed for applicationId=" + applicationId + " (" + contextLabel + ")");
    }

    private StoredApplicationPdf loadStoredApplicationPdf(Integer applicationId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CfgTblCustomFormApplication application = em.find(CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                throw new IllegalStateException("Application not found: " + applicationId);
            }
            byte[] pdfBytes = application.getBlbPdfData();
            if (pdfBytes == null || pdfBytes.length == 0) {
                logger.warn("No stored PDF on applicationId=" + applicationId + "; sending emails without PDF attachment");
            }
            String pdfName = application.getTxtPdfName();
            String pdfMime = application.getTxtPdfMime();
            return new StoredApplicationPdf(pdfBytes, pdfName, pdfMime);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private static String resolvePdfName(TemplatePostApprovalEmailJobPayload payload) {
        return StringUtils.hasText(payload.getPdfName()) ? payload.getPdfName().trim() : "template-application.pdf";
    }

    private static String resolvePdfMime(TemplatePostApprovalEmailJobPayload payload) {
        return StringUtils.hasText(payload.getPdfMime()) ? payload.getPdfMime().trim() : "application/pdf";
    }

    private static byte[] decodeBase64(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Base64.getDecoder().decode(value.trim());
    }

    private static final class StoredApplicationPdf {
        final byte[] pdfBytes;
        final String pdfName;
        final String pdfMime;

        StoredApplicationPdf(byte[] pdfBytes, String pdfName, String pdfMime) {
            this.pdfBytes = pdfBytes;
            this.pdfName = pdfName;
            this.pdfMime = pdfMime;
        }
    }
}
