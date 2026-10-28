package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDocumentShareLink;
import com.bezkoder.spring.login.sa.dal.entities.DocumentShareLinkRepository;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentShareLinkService {

    private static final String SCOPE_PDF_VIEW = "pdf_view";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private DocumentShareLinkRepository shareLinkRepository;

    @Autowired
    private ICustomFormApplicationService customFormApplicationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Map<String, Object> createShareLink(int actorUserId, Integer applicationId, Integer expiresInDays) {
        Map<String, Object> result = new HashMap<>();
        if (applicationId == null || applicationId <= 0) {
            result.put("status", "Failure");
            result.put("message", "applicationId is required");
            return result;
        }
        CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
        if (application == null || Boolean.TRUE.equals(application.getBlIsDeleted())) {
            result.put("status", "Failure");
            result.put("message", "Application not found");
            return result;
        }
        if (!canUserShareApplication(actorUserId, application)) {
            result.put("status", "Failure");
            result.put("message", "You are not allowed to share this application");
            return result;
        }

        int safeDays = expiresInDays == null || expiresInDays <= 0 ? 30 : Math.min(expiresInDays, 365);
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp expires = Timestamp.from(Instant.now().plus(safeDays, ChronoUnit.DAYS));

        CfgTblDocumentShareLink link = new CfgTblDocumentShareLink();
        link.setSerApplicationId(applicationId);
        link.setTxtShareToken(generateToken());
        link.setSerCreatedByUserId(actorUserId);
        link.setDteCreatedDate(now);
        link.setDteExpiresDate(expires);
        link.setBlRevoked(false);
        link.setIntViewCount(0);
        link.setTxtScope(SCOPE_PDF_VIEW);
        link.setBlIsDeleted(false);
        shareLinkRepository.save(link);

        result.put("status", "Success");
        result.put("shareId", link.getSerDocumentShareId());
        result.put("token", link.getTxtShareToken());
        result.put("expiresAt", link.getDteExpiresDate());
        result.put("scope", link.getTxtScope());
        return result;
    }

    public List<Map<String, Object>> listShareLinks(int actorUserId, Integer applicationId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (applicationId == null || applicationId <= 0) {
            return rows;
        }
        CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
        if (application == null || !canUserShareApplication(actorUserId, application)) {
            return rows;
        }
        for (CfgTblDocumentShareLink link : shareLinkRepository
                .findBySerApplicationIdAndBlIsDeletedFalseOrderByDteCreatedDateDesc(applicationId)) {
            rows.add(toShareLinkDto(link));
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> revokeShareLink(int actorUserId, Integer shareId) {
        Map<String, Object> result = new HashMap<>();
        if (shareId == null || shareId <= 0) {
            result.put("status", "Failure");
            result.put("message", "shareId is required");
            return result;
        }
        CfgTblDocumentShareLink link = shareLinkRepository.findById(shareId).orElse(null);
        if (link == null || Boolean.TRUE.equals(link.getBlIsDeleted())) {
            result.put("status", "Failure");
            result.put("message", "Share link not found");
            return result;
        }
        CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(link.getSerApplicationId());
        if (application == null || !canUserShareApplication(actorUserId, application)) {
            result.put("status", "Failure");
            result.put("message", "You are not allowed to revoke this link");
            return result;
        }
        link.setBlRevoked(true);
        link.setDteRevokedDate(Timestamp.from(Instant.now()));
        shareLinkRepository.save(link);
        result.put("status", "Success");
        return result;
    }

    @Transactional
    public Map<String, Object> resolvePublicShare(String token) {
        Map<String, Object> result = new HashMap<>();
        CfgTblDocumentShareLink link = findActiveLink(token);
        if (link == null) {
            result.put("status", "Failure");
            result.put("message", "This share link is invalid, expired, or has been revoked.");
            return result;
        }
        CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(link.getSerApplicationId());
        if (application == null || Boolean.TRUE.equals(application.getBlIsDeleted())) {
            result.put("status", "Failure");
            result.put("message", "Application is no longer available.");
            return result;
        }

        link.setIntViewCount((link.getIntViewCount() == null ? 0 : link.getIntViewCount()) + 1);
        link.setDteLastViewedDate(Timestamp.from(Instant.now()));
        shareLinkRepository.save(link);

        String templateName = resolveTemplateName(application.getSerFormId());
        byte[] pdfBytes = customFormApplicationService.resolveDownloadablePdf(application.getSerApplicationId());
        boolean hasPdf = pdfBytes != null && pdfBytes.length > 0;

        result.put("status", "Success");
        result.put("formCode", application.getTxtFormCode());
        result.put("templateName", templateName);
        result.put("uploadedAt", application.getDteCreatedDate());
        result.put("hasPdf", hasPdf);
        result.put("scope", link.getTxtScope());
        result.put("expiresAt", link.getDteExpiresDate());
        return result;
    }

    @Transactional
    public Map<String, Object> resolvePublicSharePdf(String token) {
        Map<String, Object> result = new HashMap<>();
        CfgTblDocumentShareLink link = findActiveLink(token);
        if (link == null) {
            result.put("status", "Failure");
            result.put("message", "This share link is invalid, expired, or has been revoked.");
            return result;
        }
        CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(link.getSerApplicationId());
        if (application == null || Boolean.TRUE.equals(application.getBlIsDeleted())) {
            result.put("status", "Failure");
            result.put("message", "Application is no longer available.");
            return result;
        }
        byte[] pdfBytes = customFormApplicationService.resolveDownloadablePdf(application.getSerApplicationId());
        if (pdfBytes == null || pdfBytes.length == 0) {
            result.put("status", "Failure");
            result.put("message", "PDF is not available for this application yet.");
            return result;
        }

        String fileName = application.getTxtPdfName();
        if (fileName == null || fileName.trim().isEmpty()) {
            String code = application.getTxtFormCode() != null && !application.getTxtFormCode().trim().isEmpty()
                    ? application.getTxtFormCode().trim()
                    : "application-" + application.getSerApplicationId();
            fileName = code + ".pdf";
        } else if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }

        String pdfMime = application.getTxtPdfMime();
        if (pdfMime == null || pdfMime.trim().isEmpty()) {
            pdfMime = "application/pdf";
        }

        result.put("status", "Success");
        result.put("fileName", fileName);
        result.put("mimeType", pdfMime);
        result.put("base64", Base64.getEncoder().encodeToString(pdfBytes));
        return result;
    }

    private CfgTblDocumentShareLink findActiveLink(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        CfgTblDocumentShareLink link = shareLinkRepository
                .findByTxtShareTokenAndBlIsDeletedFalse(token.trim())
                .orElse(null);
        if (link == null || Boolean.TRUE.equals(link.getBlRevoked())) {
            return null;
        }
        if (link.getDteExpiresDate() != null && link.getDteExpiresDate().before(Timestamp.from(Instant.now()))) {
            return null;
        }
        return link;
    }

    private Map<String, Object> toShareLinkDto(CfgTblDocumentShareLink link) {
        Map<String, Object> row = new HashMap<>();
        row.put("shareId", link.getSerDocumentShareId());
        row.put("token", link.getTxtShareToken());
        row.put("createdAt", link.getDteCreatedDate());
        row.put("expiresAt", link.getDteExpiresDate());
        row.put("revoked", Boolean.TRUE.equals(link.getBlRevoked()));
        row.put("viewCount", link.getIntViewCount() == null ? 0 : link.getIntViewCount());
        row.put("scope", link.getTxtScope());
        return row;
    }

    private String resolveTemplateName(Integer formId) {
        if (formId == null || formId <= 0) {
            return "Document";
        }
        TemplateDefinition template = entityManager.find(TemplateDefinition.class, formId);
        if (template == null || template.getTxtTemplateName() == null) {
            return "Document";
        }
        return template.getTxtTemplateName().trim();
    }

    private boolean canUserShareApplication(int actorUserId, CfgTblCustomFormApplication application) {
        if (actorUserId <= 0 || application == null) {
            return false;
        }
        if (isAdminUser(actorUserId)) {
            return true;
        }
        if (application.getSerSubmittedBy() != null && application.getSerSubmittedBy().equals(actorUserId)) {
            return true;
        }
        if (application.getSerCurrentApprover() != null && application.getSerCurrentApprover().equals(actorUserId)) {
            return true;
        }
        if (application.getSerCreatedUser() != null && application.getSerCreatedUser().equals(actorUserId)) {
            return true;
        }
        if (userReferencedInWorkflowJson(application.getTxtApprovalHistory(), actorUserId)) {
            return true;
        }
        if (userReferencedInWorkflowJson(application.getTxtPriorApprovals(), actorUserId)) {
            return true;
        }
        return false;
    }

    private boolean userReferencedInWorkflowJson(String json, int userId) {
        if (json == null || json.trim().isEmpty() || userId <= 0) {
            return false;
        }
        String needle = String.valueOf(userId);
        return json.contains("\"userId\":" + needle)
                || json.contains("\"userId\": " + needle)
                || json.contains("\"serUserId\":" + needle)
                || json.contains("\"serUserId\": " + needle)
                || json.contains("\"approvedBy\":" + needle)
                || json.contains("\"approvedBy\": " + needle);
    }

    private boolean isAdminUser(int userId) {
        CfgTblUser user = entityManager.find(CfgTblUser.class, userId);
        if (user == null) {
            return false;
        }
        CfgTblRole role = user.getCfgTblRole();
        String roleName = role != null ? role.getTxtRoleName() : null;
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ADMIN")
                || normalized.equals("ROLE_ADMIN")
                || normalized.equals("SUPER ADMIN")
                || normalized.equals("ROLE_SUPER ADMIN")
                || normalized.contains("ADMIN");
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
