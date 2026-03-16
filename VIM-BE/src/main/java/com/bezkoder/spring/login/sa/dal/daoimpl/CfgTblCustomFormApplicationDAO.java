package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.util.Locale;
import java.text.DateFormat;
import java.nio.charset.StandardCharsets;
import javax.persistence.*;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.util.StreamUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.util.Base64;
import java.util.Set;
import java.util.HashSet;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;

@Repository
public class CfgTblCustomFormApplicationDAO implements ICfgTblCustomFormApplicationDAO {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(CfgTblCustomFormApplicationDAO.class);

    @Value("${app.backend.url:http://localhost:8080/velocity}")
    private String backendBaseUrl;

    @Value("${app.base.url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Get base URL for email links.
     * Uses the injected backend URL if available, otherwise falls back to a
     * default.
     */
    private String getBaseUrl() {
        if (backendBaseUrl != null && !backendBaseUrl.trim().isEmpty()) {
            return backendBaseUrl;
        }
        return "http://localhost:8080/velocity";
    }

    public CfgTblCustomFormApplicationDAO() {
    }

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    private Set<Integer> parseDepartmentHeadIds(EntityManager em, Integer departmentId) {
        Set<Integer> ids = new HashSet<>();
        if (departmentId == null)
            return ids;
        try {
            HrTblDepartment dept = em.find(HrTblDepartment.class, departmentId);
            if (dept == null)
                return ids;
            String raw = dept.getSerDepartmentHeadId();
            if (raw == null || raw.trim().isEmpty())
                return ids;
            String[] parts = raw.split(",");
            for (String p : parts) {
                if (p == null)
                    continue;
                String trimmed = p.trim();
                if (trimmed.isEmpty())
                    continue;
                try {
                    ids.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    private Set<Integer> getApprovedHodsForStage(List<java.util.Map<String, Object>> approvalHistory,
            Integer departmentId, Integer level) {
        Set<Integer> approved = new HashSet<>();
        if (approvalHistory == null || approvalHistory.isEmpty())
            return approved;
        for (Map<String, Object> entry : approvalHistory) {
            if (entry == null)
                continue;
            Integer dept = safeInt(entry.get("departmentId"), null);
            Integer lvl = safeInt(entry.get("level"), null);
            if (dept != null && lvl != null && dept.equals(departmentId) && lvl.equals(level)) {
                Integer uid = safeInt(entry.get("approvedBy"), null);
                if (uid != null)
                    approved.add(uid);
            }
        }
        return approved;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsByStatusAndUserId(String status, Integer userId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.txtStatus = :status " +
                            "AND a.serSubmittedBy = :userId " +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .setParameter("status", status)
                    .setParameter("userId", userId)
                    .getResultList();
            entityManager.getTransaction().commit();
            return applications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications by status and user: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String getNextApplicationCode(Integer formId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String code = generateNextApplicationCode(formId, entityManager);
            entityManager.getTransaction().commit();
            return code;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting next application code: " + e.getMessage(), e);
            return null;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    /**
     * Generate next application code based on form's convention prefix
     * Format: PREFIX-0001, PREFIX-0002, etc.
     * Looks at both the form code and existing application codes for the same form
     */
    private String generateNextApplicationCode(Integer formId, EntityManager entityManager) {
        try {
            // Get the form to find its convention prefix
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = entityManager
                    .find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, formId);

            if (form == null) {
                log.warn("Form with ID " + formId + " not found, cannot generate application code");
                return null;
            }

            String conventionPrefix = form.getTxtConventionPrefix();
            if (conventionPrefix == null || conventionPrefix.trim().isEmpty()) {
                // If no convention prefix, try to extract from form code
                String formCode = form.getTxtFormCode();
                if (formCode != null && formCode.contains("-")) {
                    conventionPrefix = formCode.substring(0, formCode.indexOf("-"));
                } else {
                    log.warn("Form " + formId
                            + " has no convention prefix or form code, cannot generate application code");
                    return null;
                }
            }

            conventionPrefix = conventionPrefix.trim().toUpperCase(Locale.ROOT);

            String appCodesQuery = "SELECT a.txtFormCode FROM CfgTblCustomFormApplication a " +
                    "WHERE a.serFormId = :formId " +
                    "AND a.txtFormCode IS NOT NULL " +
                    "AND UPPER(a.txtFormCode) LIKE :prefixPattern " +
                    "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL)";

            @SuppressWarnings("unchecked")
            List<String> appCodes = entityManager.createQuery(appCodesQuery)
                    .setParameter("formId", formId)
                    .setParameter("prefixPattern", conventionPrefix + "-%")
                    .getResultList();

            int maxNumber = 0;
            int width = 4;

            Integer formCodeNumber = extractNumericSuffix(form.getTxtFormCode(), conventionPrefix);
            if (formCodeNumber != null) {
                maxNumber = Math.max(maxNumber, formCodeNumber);
                width = Math.max(width, getNumericSuffixLength(form.getTxtFormCode(), conventionPrefix));
            }

            if (appCodes != null) {
                for (String code : appCodes) {
                    Integer numeric = extractNumericSuffix(code, conventionPrefix);
                    if (numeric != null) {
                        maxNumber = Math.max(maxNumber, numeric);
                        width = Math.max(width, getNumericSuffixLength(code, conventionPrefix));
                    }
                }
            }

            return String.format("%s-%0" + width + "d", conventionPrefix, maxNumber + 1);
        } catch (Exception e) {
            log.error("Error generating application code: " + e.getMessage(), e);
            // Fallback: use timestamp-based code
            return "APP-" + System.currentTimeMillis();
        }
    }

    private Integer extractNumericSuffix(String code, String prefix) {
        if (code == null || prefix == null) {
            return null;
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT) + "-";
        if (!normalizedCode.startsWith(normalizedPrefix)) {
            return null;
        }
        String numberPart = normalizedCode.substring(normalizedPrefix.length());
        if (!numberPart.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int getNumericSuffixLength(String code, String prefix) {
        if (code == null || prefix == null) {
            return 4;
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT) + "-";
        if (!normalizedCode.startsWith(normalizedPrefix)) {
            return 4;
        }
        String numberPart = normalizedCode.substring(normalizedPrefix.length());
        return numberPart.matches("\\d+") ? Math.max(4, numberPart.length()) : 4;
    }

    private boolean applicationCodeExists(Integer formId, String formCode, EntityManager entityManager) {
        if (formId == null || formCode == null || formCode.trim().isEmpty()) {
            return false;
        }
        Long count = (Long) entityManager.createQuery(
                "SELECT COUNT(a.serApplicationId) FROM CfgTblCustomFormApplication a " +
                        "WHERE a.serFormId = :formId " +
                        "AND UPPER(a.txtFormCode) = :code " +
                        "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL)")
                .setParameter("formId", formId)
                .setParameter("code", formCode.trim().toUpperCase(Locale.ROOT))
                .getSingleResult();
        return count != null && count > 0;
    }

    private String incrementCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim().toUpperCase(Locale.ROOT);
        int sep = trimmed.lastIndexOf("-");
        if (sep <= 0 || sep >= trimmed.length() - 1) {
            return null;
        }
        String prefix = trimmed.substring(0, sep);
        String numberPart = trimmed.substring(sep + 1);
        if (!numberPart.matches("\\d+")) {
            return null;
        }
        try {
            int number = Integer.parseInt(numberPart);
            int width = Math.max(4, numberPart.length());
            return String.format("%s-%0" + width + "d", prefix, number + 1);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveUniqueApplicationCode(Integer formId, String requestedCode, EntityManager entityManager) {
        if (formId == null) {
            return requestedCode != null ? requestedCode.trim().toUpperCase(Locale.ROOT) : null;
        }

        String candidate = requestedCode != null ? requestedCode.trim().toUpperCase(Locale.ROOT) : null;
        if (candidate == null || candidate.isEmpty()) {
            return generateNextApplicationCode(formId, entityManager);
        }

        int attempts = 0;
        while (applicationCodeExists(formId, candidate, entityManager) && attempts < 1000) {
            String incremented = incrementCode(candidate);
            if (incremented == null) {
                return generateNextApplicationCode(formId, entityManager);
            }
            candidate = incremented;
            attempts++;
        }

        if (attempts >= 1000 && applicationCodeExists(formId, candidate, entityManager)) {
            return generateNextApplicationCode(formId, entityManager);
        }

        return candidate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getAllApplications() {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .getResultList();
            entityManager.getTransaction().commit();
            return applications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting all applications: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsByFormId(Integer formId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.serFormId = :formId " +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .setParameter("formId", formId)
                    .getResultList();
            entityManager.getTransaction().commit();
            return applications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications by form ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsByUserId(Integer userId) {

        EntityManager entityManager = getEntityManager();

        try {

            entityManager.getTransaction().begin();

            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.serSubmittedBy = :userId " +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC",
                    CfgTblCustomFormApplication.class)
                    .setParameter("userId", userId)
                    .setFirstResult(0) // 🔥 Prevent large sort
                    .setMaxResults(200) // 🔥 Limit results
                    .getResultList();

            entityManager.getTransaction().commit();
            return applications;

        } catch (Exception e) {

            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            log.error("Error getting applications by user ID", e);
            throw e;

        } finally {

            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public CfgTblCustomFormApplication getApplicationById(Integer applicationId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.serApplicationId = :applicationId")
                    .setParameter("applicationId", applicationId)
                    .getResultList();

            if (!applications.isEmpty()) {
                CfgTblCustomFormApplication application = applications.get(0);
                // Deserialize approval pipelines if form exists
                if (application.getCfgTblCustomForm() != null) {
                    deserializeApprovalPipeline(application.getCfgTblCustomForm(), entityManager);
                }
            }

            entityManager.getTransaction().commit();
            return applications.isEmpty() ? null : applications.get(0);
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting application by ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    /**
     * Deserialize approval pipeline from JSON string to list of pipeline objects
     */
    private void deserializeApprovalPipeline(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            EntityManager entityManager) {
        try {
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines = new java.util.ArrayList<>();

            if (form.getTxtApprovalPipeline() != null && !form.getTxtApprovalPipeline().trim().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<java.util.Map<String, Object>> pipelineMaps = mapper.readValue(
                        form.getTxtApprovalPipeline(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                        });

                // Convert each map to CfgTblCustomFormApprovalPipeline object
                for (java.util.Map<String, Object> pipelineMap : pipelineMaps) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline = new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();

                    // Set approval order
                    Object orderObj = pipelineMap.get("intApprovalOrder");
                    if (orderObj != null) {
                        pipeline.setIntApprovalOrder(orderObj instanceof Integer ? (Integer) orderObj
                                : Integer.parseInt(orderObj.toString()));
                    }

                    // Set department ID
                    Object deptIdObj = pipelineMap.get("serDepartmentId");
                    if (deptIdObj != null) {
                        Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj
                                : Integer.parseInt(deptIdObj.toString());
                        pipeline.setSerDepartmentId(deptId);

                        // Fetch department entity
                        com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment department = entityManager
                                .find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, deptId);
                        if (department != null) {
                            pipeline.setHrTblDepartment(department);
                        }
                    }

                    pipelines.add(pipeline);
                }
            }

            form.setCfgTblCustomFormApprovalPipelines(pipelines);
        } catch (Exception e) {
            log.error("Error deserializing approval pipeline JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String submitApplication(CfgTblCustomFormApplication application) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            // Set default values
            if (application.getBlIsActive() == null) {
                application.setBlIsActive(true);
            }
            if (application.getBlIsDeleted() == null) {
                application.setBlIsDeleted(false);
            }
            if (application.getBlnStatus() == null) {
                application.setBlnStatus(true);
            }
            if (application.getTxtStatus() == null || application.getTxtStatus().isEmpty()) {
                application.setTxtStatus("PENDING");
            }
            if (application.getIntCurrentApprovalLevel() == null) {
                application.setIntCurrentApprovalLevel(0);
            }
            if (application.getDteCreatedDate() == null) {
                application.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
            }
            if (application.getSerCreatedUser() == null) {
                application.setSerCreatedUser(commonService.getCurrentLoggedInUser());
            }
            if (application.getSerSubmittedBy() == null) {
                application.setSerSubmittedBy(commonService.getCurrentLoggedInUser());
            }
            if (application.getTxtApplicationData() == null || application.getTxtApplicationData().trim().isEmpty()) {
                application.setTxtApplicationData("{}");
            }

            // Detect Budget Approval form
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm formForBudget = application
                    .getSerFormId() != null
                            ? entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                                    application.getSerFormId())
                            : null;
            boolean isBudgetApproval = isBudgetApprovalForm(formForBudget);
            boolean isCapf = isCapfForm(formForBudget);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;

            // If CAPF form and has initial signer in JSON data, set level to -1
            if (isCapf && hasInitialSigner(application)) {
                application.setIntCurrentApprovalLevel(-1);
                log.info("Setting initial approval level to -1 for CAPF with Initial Signer");
            }

            // Ensure a unique code at submission time; if requested code already exists,
            // increment until available.
            if (application.getSerFormId() != null) {
                String uniqueCode = resolveUniqueApplicationCode(
                        application.getSerFormId(),
                        application.getTxtFormCode(),
                        entityManager);
                if (uniqueCode != null && !uniqueCode.trim().isEmpty()) {
                    application.setTxtFormCode(uniqueCode);
                }
            } else if (application.getTxtFormCode() != null) {
                application.setTxtFormCode(application.getTxtFormCode().trim().toUpperCase(Locale.ROOT));
            }

            // Initialize approval level and status for individual pipeline flow
            if (useIndividualPipelineFlow) {
                application.setIntCurrentApprovalLevel(0);
                application.setTxtStatus("IN_PROGRESS");
            }

            // Generate and store PDF on creation (summary)
            if (application.getBlbPdfData() == null || application.getBlbPdfData().length == 0) {
                try {
                    Map<String, Object> appData = parseApplicationData(application);
                    byte[] pdfBytes = generateApplicationPdf(application, formForBudget, appData);
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String code = application.getTxtFormCode() != null ? application.getTxtFormCode()
                                : "application";
                        application.setBlbPdfData(pdfBytes);
                        application.setTxtPdfName(buildPdfFileName(formForBudget, code));
                        application.setTxtPdfMime("application/pdf");
                    }
                } catch (Exception e) {
                    log.warn("Error generating application PDF: " + e.getMessage(), e);
                }
            }

            entityManager.persist(application);
            entityManager.getTransaction().commit();

            // Send email notifications after successful submission (unless deferred)
            boolean deferEmail = Boolean.TRUE.equals(application.getDeferEmail());
            if (!deferEmail) {
                try {
                    if (useIndividualPipelineFlow) {
                        // Send email to first approver in sequence (everyone should get emails sequentially)
                        sendBudgetApprovalNextEmail(application, 0);
                        sendSubmissionEmails(application); // still send submitter confirmation
                    } else {
                        sendSubmissionEmails(application);
                    }
                } catch (Exception emailEx) {
                    log.error("Error sending submission emails: " + emailEx.getMessage(), emailEx);
                    // Don't fail the submission if email fails
                }
            }

            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : e.getMessage();
            log.error("Error submitting application: " + errorMessage, e);
            e.printStackTrace();
            return "Failure: " + errorMessage;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String updateApplication(CfgTblCustomFormApplication application) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication existingApplication = entityManager.find(
                    CfgTblCustomFormApplication.class, application.getSerApplicationId());
            if (existingApplication == null) {
                entityManager.getTransaction().rollback();
                entityManager.close();
                return "Failure: Application not found";
            }

            // Update application properties
            existingApplication.setTxtApplicationData(application.getTxtApplicationData());
            existingApplication.setTxtStatus(application.getTxtStatus());
            existingApplication.setIntCurrentApprovalLevel(application.getIntCurrentApprovalLevel());
            existingApplication.setSerCurrentApprover(application.getSerCurrentApprover());
            existingApplication.setTxtRemarks(application.getTxtRemarks());
            existingApplication.setBlIsActive(application.getBlIsActive());
            existingApplication.setBlnStatus(application.getBlnStatus());
            existingApplication.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            existingApplication.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            // Add signature and timestamp to txtApplicationData
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String applicationDataJson = existingApplication.getTxtApplicationData();
                Map<String, Object> applicationData = objectMapper.readValue(applicationDataJson,
                        new TypeReference<Map<String, Object>>() {
                        });

                applicationData.put("approvedBySignature", commonService.getCurrentUserName());
                applicationData.put("approvedTimestamp", commonService.getCurrentTimeStamp_new().toString());

                existingApplication.setTxtApplicationData(objectMapper.writeValueAsString(applicationData));
            } catch (Exception jsonException) {
                log.error(
                        "Error processing application data JSON for signature/timestamp: " + jsonException.getMessage(),
                        jsonException);
                // Optionally, handle this error more gracefully, e.g., by not updating
                // txtApplicationData
            }

            entityManager.merge(existingApplication);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : e.getMessage();
            log.error("Error updating application: " + errorMessage, e);
            e.printStackTrace();
            return "Failure: " + errorMessage;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String updateApplicationPdf(Integer applicationId, byte[] pdfData, String pdfName, String pdfMime) {
        EntityManager entityManager = getEntityManager();
        try {
            if (applicationId == null) {
                return "Failure: Application ID is required";
            }
            if (pdfData == null || pdfData.length == 0) {
                return "Failure: PDF data is empty";
            }

            entityManager.getTransaction().begin();
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class,
                    applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            application.setBlbPdfData(pdfData);
            application.setTxtPdfName(pdfName != null && !pdfName.trim().isEmpty() ? pdfName : "application.pdf");
            application.setTxtPdfMime(pdfMime != null && !pdfMime.trim().isEmpty() ? pdfMime : "application/pdf");
            entityManager.merge(application);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error updating application PDF: " + e.getMessage(), e);
            return "Failure: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String deleteApplication(Integer applicationId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication application = entityManager.find(
                    CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                entityManager.close();
                return "Failure: Application not found";
            }

            // Soft delete
            application.setBlIsDeleted(true);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error deleting application: " + e.getMessage(), e);
            e.printStackTrace();
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsApprovedByUser(String status, Integer userId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            // Fetch all non-deleted applications that have txtApprovalHistory populated
            // and mention this userId. We filter precisely in Java below using Jackson.
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.txtApprovalHistory IS NOT NULL " +
                            "AND a.txtApprovalHistory LIKE :userPattern " +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .setParameter("userPattern", "%" + userId + "%")
                    .getResultList();

            // Filter using Jackson: user must have action=APPROVED at level >= 1.
            // Filter approvals to only include real approvals (level >= 1).
            ObjectMapper mapper = new ObjectMapper();
            java.util.List<CfgTblCustomFormApplication> filtered = new java.util.ArrayList<>();
            for (CfgTblCustomFormApplication app : applications) {
                if (userHasRealApprovalInHistory(app, userId, mapper)) {
                    filtered.add(app);
                }
            }

            entityManager.getTransaction().commit();
            log.info("getApplicationsApprovedByUser() - userId: {} - found {} of {} candidates",
                    userId, filtered.size(), applications.size());
            return filtered;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications by status and user ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsByStatus(String status) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE a.txtStatus = :status " +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .setParameter("status", status)
                    .getResultList();
            entityManager.getTransaction().commit();
            return applications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications by status: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getApplicationsPendingApprovalForDepartmentHead(
            Integer departmentHeadUserId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            if (departmentHeadUserId == null || departmentHeadUserId <= 0) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }

            CfgTblUser approverUser = commonService.getCurrentUser(departmentHeadUserId);
            if (approverUser == null) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }

            // Keep query broad, then apply precise "is pending for this user at this stage"
            // filtering.
            List<CfgTblCustomFormApplication> allPendingApplications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE (a.txtStatus = 'PENDING' OR a.txtStatus = 'IN_PROGRESS' OR a.txtStatus = 'CEO_PENDING' OR a.txtStatus = 'ASSET_PENDING') "
                            +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC",
                    CfgTblCustomFormApplication.class)
                    .setFirstResult(0)
                    .setMaxResults(2000)
                    .getResultList();

            List<CfgTblCustomFormApplication> filteredApplications = new java.util.ArrayList<>();
            for (CfgTblCustomFormApplication app : allPendingApplications) {
                if (isPendingForUserAtCurrentStage(entityManager, app, departmentHeadUserId, approverUser)) {
                    filteredApplications.add(app);
                }
            }

            entityManager.getTransaction().commit();
            return filteredApplications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications pending approval for department head: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CfgTblCustomFormApplication> getAllApplicationsPendingApproval() {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            Integer currentUserId = commonService.getCurrentLoggedInUser();
            if (currentUserId == null || currentUserId <= 0) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }

            CfgTblUser currentUser = commonService.getCurrentUser(currentUserId);
            if (currentUser == null) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }

            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                    "SELECT a FROM CfgTblCustomFormApplication a " +
                            "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                            "WHERE (a.txtStatus = 'PENDING' OR a.txtStatus = 'IN_PROGRESS' OR a.txtStatus = 'CEO_PENDING' OR a.txtStatus = 'ASSET_PENDING') "
                            +
                            "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                            "ORDER BY a.dteCreatedDate DESC",
                    CfgTblCustomFormApplication.class)
                    .setFirstResult(0)
                    .setMaxResults(2000)
                    .getResultList();

            List<CfgTblCustomFormApplication> filteredApplications = new java.util.ArrayList<>();
            for (CfgTblCustomFormApplication app : applications) {
                if (isPendingForUserAtCurrentStage(entityManager, app, currentUserId, currentUser)) {
                    filteredApplications.add(app);
                }
            }

            entityManager.getTransaction().commit();
            return filteredApplications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting all applications pending approval: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private boolean isPendingForUserAtCurrentStage(EntityManager entityManager,
            CfgTblCustomFormApplication application,
            Integer userId,
            CfgTblUser user) {
        if (entityManager == null || application == null || userId == null || userId <= 0) {
            return false;
        }
        String status = application.getTxtStatus() != null ? application.getTxtStatus().trim().toUpperCase() : "";
        if ("CEO_PENDING".equals(status)) {
            return userHasRole(user, "CEO");
        }
        if ("ASSET_PENDING".equals(status)) {
            return userHasRole(user, "FINANCE_HEAD") || userHasRole(user, "FINANCE");
        }

        com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = application.getCfgTblCustomForm();
        if (form == null && application.getSerFormId() != null) {
            form = entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                    application.getSerFormId());
        }
        if (form == null) {
            return false;
        }

        if (isCapfForm(form) && application.getIntCurrentApprovalLevel() != null
                && application.getIntCurrentApprovalLevel() == -1) {
            Integer initialSignerId = extractInitialSignerId(application);
            return initialSignerId != null && initialSignerId.equals(userId);
        }

        Map<String, Object> appData = parseApplicationData(application);
        boolean useIndividualPipelineFlow = isBudgetApprovalForm(form) || !extractFooterFields(appData).isEmpty();
        if (useIndividualPipelineFlow) {
            List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
            int currentLevel = currentLevelSafe(application);
            if (currentLevel >= 0 && currentLevel < sequence.size()) {
                BudgetApprover expected = sequence.get(currentLevel);
                return expected != null && expected.userId != null && expected.userId.equals(userId);
            }
            return false;
        }

        String pipelineJson = form.getTxtApprovalPipeline();
        if (pipelineJson == null || pipelineJson.trim().isEmpty()) {
            return false;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> pipelines = mapper.readValue(
                    pipelineJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            int currentLevel = currentLevelSafe(application);
            boolean isCapf = isCapfForm(form);

            // Calculate which pipeline entry to use. CAPF Level 0 is dynamic HOD.
            int pipelineIndex = isCapf ? currentLevel - 1 : currentLevel;
            Map<String, Object> currentPipeline = null;

            if (pipelineIndex >= 0 && pipelineIndex < pipelines.size()) {
                currentPipeline = pipelines.get(pipelineIndex);
            } else if (isCapf && currentLevel == 0) {
                // Valid stage (HOD), no pipeline entry needed yet
            } else {
                return false;
            }

            Integer departmentId = null;
            String departmentName = null;

            if (currentPipeline != null) {
                departmentId = safeInt(currentPipeline.get("serDepartmentId"),
                        safeInt(currentPipeline.get("departmentId"), null));
                departmentName = resolveDepartmentName(entityManager, departmentId, currentPipeline);
            }

            // For CAPF forms, the first stage ALWAYS routes to the initiator's (submitter's) HOD.
            if (isCapf && currentLevel == 0) {
                Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                if (submitterDeptId != null) {
                    departmentId = submitterDeptId;
                }
            } else if (currentPipeline != null && isUserDepartmentHodStage(currentPipeline, departmentName)) {
                Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                if (submitterDeptId != null) {
                    departmentId = submitterDeptId;
                }
            }

            Integer userDepartmentId = loadUserDepartmentId(entityManager, userId);
            return departmentId != null && userDepartmentId != null && departmentId.equals(userDepartmentId);
        } catch (Exception e) {
            log.warn("Error filtering pending app {} for user {}: {}", application.getSerApplicationId(), userId,
                    e.getMessage());
            return false;
        }
    }

    @Override
    public String approveApplication(Integer applicationId, String remarks) {
        return approveApplication(applicationId, remarks, null, "SYSTEM", null);
    }

    @Override
    public String approveApplication(Integer applicationId, String remarks, Integer approverUserId, String approvedVia,
            String approvedIp) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication application = entityManager.find(
                    CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            // Resolve approver (current logged in user or explicit userId)
            Integer resolvedApproverId = approverUserId;
            if (resolvedApproverId == null || resolvedApproverId <= 0) {
                resolvedApproverId = commonService.getCurrentLoggedInUser();
            }
            if (resolvedApproverId == null || resolvedApproverId <= 0) {
                entityManager.getTransaction().rollback();
                return "Failure: User not authenticated";
            }

            CfgTblUser approverUser = commonService.getCurrentUser(resolvedApproverId);
            if (approverUser == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Approver user not found";
            }

            String approverSignaturePath = approverUser.getTxtSignaturePath();
            if (approverSignaturePath == null || approverSignaturePath.trim().isEmpty()) {
                entityManager.getTransaction().rollback();
                return "Failure: Signature not uploaded. Please upload your signature before approving.";
            }

            // Short-circuit CEO approval stage
            if ("CEO_PENDING".equalsIgnoreCase(application.getTxtStatus())) {
                if (!userHasRole(approverUser, "CEO")) {
                    entityManager.getTransaction().rollback();
                    return "Failure: Only CEO can approve at this stage";
                }
                application.setTxtStatus("ASSET_PENDING");
                Integer financeUserId = findFirstUserIdByRole(entityManager, "FINANCE_HEAD");
                if (financeUserId == null) {
                    financeUserId = findFirstUserIdByRole(entityManager, "FINANCE");
                }
                application.setSerCurrentApprover(financeUserId);
                // Append history entry for CEO approval
                appendHistoryEntry(entityManager, application, resolvedApproverId, "APPROVED", "CEO", -99, approvedVia,
                        approvedIp);
                application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                entityManager.merge(application);
                entityManager.getTransaction().commit();
                // Send Finance email
                try {
                    sendFinanceEmails(application, null);
                } catch (Exception e) {
                    log.warn("Finance email send failed: {}", e.getMessage());
                }
                return "Success";
            }

            // Get the form to check approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = entityManager.find(
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                    application.getSerFormId());

            if (form == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Form not found";
            }

            boolean isBudgetApproval = isBudgetApprovalForm(form);
            Map<String, Object> appData = parseApplicationData(application);
            boolean hasDynamicFooterFlow = !extractFooterFields(appData).isEmpty();
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;
            if (useIndividualPipelineFlow) {
                List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
                if (sequence.isEmpty()) {
                    entityManager.getTransaction().rollback();
                    return "Failure: Budget approval sequence not configured";
                }

                Integer currentLevel = application.getIntCurrentApprovalLevel();
                if (currentLevel == null)
                    currentLevel = 0;
                if (currentLevel < 0 || currentLevel >= sequence.size()) {
                    // Check if it's already approved
                    if ("APPROVED".equalsIgnoreCase(application.getTxtStatus())) {
                        entityManager.getTransaction().commit();
                        return "Success";
                    }
                    entityManager.getTransaction().rollback();
                    return "Failure: Approval already completed or in invalid state";
                }

                BudgetApprover expected = sequence.get(currentLevel);
                if (expected.userId == null || !expected.userId.equals(resolvedApproverId)) {
                    // Check if this user already approved this application recently (duplicate
                    // click)
                    if (isUserAlreadyInApprovedHistory(application, resolvedApproverId)) {
                        entityManager.getTransaction().commit();
                        return "Success";
                    }
                    entityManager.getTransaction().rollback();
                    return "Failure: You are not authorized to approve at this stage";
                }

                // All approvers must manually approve - no auto-approval
                // This includes submitters who selected themselves in the pipeline

                // Get or create approval history array
                List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
                String historyJson = application.getTxtApprovalHistory();
                if (historyJson != null && !historyJson.trim().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        approvalHistory = mapper.readValue(
                                historyJson,
                                new TypeReference<List<java.util.Map<String, Object>>>() {
                                });
                    } catch (Exception e) {
                        log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                        approvalHistory = new java.util.ArrayList<>();
                    }
                }

                java.util.Map<String, Object> approvalEntry = new java.util.HashMap<>();
                approvalEntry.put("level", currentLevel + 1);
                approvalEntry.put("departmentId", null);
                approvalEntry.put("departmentName", expected.role);
                approvalEntry.put("remarks", remarks != null ? remarks : "");
                approvalEntry.put("approvedBy", resolvedApproverId);
                approvalEntry.put("approverName", approverUser.getTxtUserName());
                approvalEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
                approvalEntry.put("signaturePath", approverSignaturePath);
                approvalEntry.put("txtDepartmentName",
                        approverUser.getTxtDepartmentName() != null ? approverUser.getTxtDepartmentName() : "");
                approvalEntry.put("userDepartmentName",
                        approverUser.getTxtDepartmentName() != null ? approverUser.getTxtDepartmentName() : "");
                approvalEntry.put("designation",
                        approverUser.getTxtDesignation() != null ? approverUser.getTxtDesignation() : "");
                approvalEntry.put("txtDesignation",
                        approverUser.getTxtDesignation() != null ? approverUser.getTxtDesignation() : "");
                approvalEntry.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
                approvalEntry.put("approvedIp", approvedIp != null ? approvedIp : "");
                approvalEntry.put("action", "APPROVED");
                approvalEntry.put("role", expected.role);
                log.info(
                        "CAPF signature log [budget-approval-entry]: appId={}, userId={}, level={}, role={}, signaturePath={}",
                        application.getSerApplicationId(), resolvedApproverId, currentLevel + 1, expected.role,
                        approverSignaturePath != null ? approverSignaturePath : "");
                approvalHistory.add(approvalEntry);

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String updatedHistoryJson = mapper.writeValueAsString(approvalHistory);
                    application.setTxtApprovalHistory(updatedHistoryJson);
                } catch (Exception e) {
                    log.error("Error serializing approval history: " + e.getMessage());
                }

                currentLevel++;
                if (currentLevel >= sequence.size()) {
                    // For individual pipeline footer forms (not budget approval), set status to COMPLETED
                    if (hasDynamicFooterFlow && !isBudgetApproval) {
                        application.setTxtStatus("COMPLETED");
                        application.setSerCurrentApprover(null);
                    } else {
                        // For budget approval forms, route to CEO/Finance as before
                        Integer ceoUserId = findFirstUserIdByRole(entityManager, "CEO");
                        if (ceoUserId != null) {
                            application.setTxtStatus("CEO_PENDING");
                            application.setSerCurrentApprover(ceoUserId);
                        } else {
                            application.setTxtStatus("ASSET_PENDING");
                            Integer financeUserId = findFirstUserIdByRole(entityManager, "FINANCE_HEAD");
                            if (financeUserId == null) {
                                financeUserId = findFirstUserIdByRole(entityManager, "FINANCE");
                            }
                            application.setSerCurrentApprover(financeUserId);
                        }
                    }
                } else {
                    application.setTxtStatus("IN_PROGRESS");
                    application.setSerCurrentApprover(resolvedApproverId);
                }
                application.setIntCurrentApprovalLevel(currentLevel);
                application.setTxtRemarks(remarks);
                application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                application.setSerModifiedUser(resolvedApproverId);

                // For general forms with individual pipeline footer: DO NOT update the stored PDF
                // The email PDF should stay independent and use the original PDF generated at submission
                // Only update PDF for budget approval forms, or when no PDF exists
                // isBudgetApproval and hasDynamicFooterFlow are already defined earlier in the method
                boolean shouldRegeneratePdf = (isBudgetApproval && hasDynamicFooterFlow) ||
                        application.getBlbPdfData() == null || application.getBlbPdfData().length == 0;
                if (shouldRegeneratePdf) {
                    try {
                        byte[] pdfBytes = generateApplicationPdf(application, form, appData);
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            String code = application.getTxtFormCode() != null ? application.getTxtFormCode()
                                    : "application";
                            application.setBlbPdfData(pdfBytes);
                            application.setTxtPdfName(buildPdfFileName(form, code));
                            application.setTxtPdfMime("application/pdf");
                        }
                    } catch (Exception e) {
                        log.warn("Error regenerating application PDF: " + e.getMessage(), e);
                    }
                } else if (isBudgetApproval && hasDynamicFooterFlow && application.getBlbPdfData() != null
                        && application.getBlbPdfData().length > 0) {
                    // Only update PDF for budget approval forms, not general forms with individual pipeline footer
                    // Keep the exact existing form layout and only refresh footer signatures.
                    try {
                        byte[] signedPdf = applyDynamicFooterSignaturesToPdf(
                                application.getBlbPdfData(),
                                appData,
                                application.getTxtApprovalHistory());
                        if (signedPdf != null && signedPdf.length > 0) {
                            application.setBlbPdfData(signedPdf);
                            String code = application.getTxtFormCode() != null ? application.getTxtFormCode()
                                    : "application";
                            application.setTxtPdfName(buildPdfFileName(form, code));
                            application.setTxtPdfMime("application/pdf");
                        }
                    } catch (Exception e) {
                        log.warn("Error applying dynamic footer signatures to existing PDF: " + e.getMessage(), e);
                    }
                }
                // For general forms with individual pipeline footer (hasDynamicFooterFlow but NOT isBudgetApproval):
                // Do nothing - preserve the original PDF so email layout stays independent

                entityManager.merge(application);
                entityManager.getTransaction().commit();

                try {
                    if (currentLevel < sequence.size()) {
                        sendBudgetApprovalNextEmail(application, currentLevel);
                    }
                } catch (Exception emailEx) {
                    log.error("Error sending budget approval emails: " + emailEx.getMessage(), emailEx);
                }

                return "Success";
            }

            // Deserialize approval pipeline
            String pipelineJson = form.getTxtApprovalPipeline();
            List<java.util.Map<String, Object>> pipelines = new java.util.ArrayList<>();
            if (pipelineJson != null && !pipelineJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pipelines = mapper.readValue(
                            pipelineJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.error("Error parsing approval pipeline: " + e.getMessage());
                    entityManager.getTransaction().rollback();
                    return "Failure: Error parsing approval pipeline";
                }
            }

            Integer currentLevel = application.getIntCurrentApprovalLevel();
            if (currentLevel == null) {
                currentLevel = 0;
            }

            // Get the department that is currently approving (at currentLevel, which is
            // 0-indexed)
            // Pipeline order is 1-indexed, so currentLevel 0 = pipeline order 1
            java.util.Map<String, Object> currentDepartmentPipeline = null;
            Integer departmentId = null;
            String departmentName = null;
            Integer pipelineOrder = null;

            boolean isCapf = isCapfForm(form);

            if (currentLevel == -1 && isCapf) {
                // Initial Signer stage for CAPF
                Integer initialSignerId = extractInitialSignerId(application);
                if (initialSignerId == null || !initialSignerId.equals(resolvedApproverId)) {
                    entityManager.getTransaction().rollback();
                    return "Failure: You are not authorized to sign this application at this stage (Initial Signer required).";
                }
                departmentName = "Initial Signer";
                log.info("Initial Signer approved application " + application.getSerApplicationId());
            } else {
                int pipelineIndex = isCapf ? currentLevel - 1 : currentLevel;

                if (isCapf && currentLevel == 0) {
                    // Level 0 for CAPF is ALWAYS Initiator HOD
                    Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                    if (submitterDeptId != null) {
                        departmentId = submitterDeptId;
                        departmentName = resolveDepartmentName(entityManager, departmentId, null);
                        log.info("CAPF detected. Resolved current approval stage to initiator department ID: " + departmentId +
                                " for application " + application.getSerApplicationId());
                    }
                } else if (!pipelines.isEmpty() && pipelineIndex >= 0 && pipelineIndex < pipelines.size()) {
                    currentDepartmentPipeline = pipelines.get(pipelineIndex);
                    if (currentDepartmentPipeline != null) {
                        Object deptIdObj = currentDepartmentPipeline.get("serDepartmentId");
                        Object orderObj = currentDepartmentPipeline.get("intApprovalOrder");

                        if (deptIdObj != null) {
                            departmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj
                                    : Integer.parseInt(deptIdObj.toString());
                            // Resolve department name from pipeline or DB
                            departmentName = resolveDepartmentName(entityManager, departmentId, currentDepartmentPipeline);
                        }

                        // Dynamic CAPF stage: "User Dept (HoD)" should authorize against submitter's
                        // department.
                        if (isUserDepartmentHodStage(currentDepartmentPipeline, departmentName)) {
                            Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                            if (submitterDeptId != null) {
                                departmentId = submitterDeptId;
                                departmentName = resolveDepartmentName(entityManager, departmentId, null);
                                log.info("Resolved current dynamic stage to submitter department ID: " + departmentId +
                                        " for application " + application.getSerApplicationId());
                            } else {
                                log.warn("Could not resolve submitter department for User Dept (HoD) stage, application: " +
                                        application.getSerApplicationId());
                            }
                        }

                        if (orderObj != null) {
                            pipelineOrder = orderObj instanceof Integer ? (Integer) orderObj
                                    : Integer.parseInt(orderObj.toString());
                        }
                    }
                }
            }

            // Enforce department-based approval: approver must belong to current pipeline
            // department
            if (departmentId != null) {
                Integer approverDeptId = loadUserDepartmentId(entityManager, resolvedApproverId);
                if (approverDeptId == null || !departmentId.equals(approverDeptId)) {
                    // Check if this user already approved this application recently (duplicate
                    // click)
                    if (isUserAlreadyInApprovedHistory(application, resolvedApproverId)) {
                        entityManager.getTransaction().commit();
                        return "Success";
                    }
                    entityManager.getTransaction().rollback();
                    return "Failure: You are not authorized to approve this department step.";
                }
            }

            // Get or create approval history array
            List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
            String historyJson = application.getTxtApprovalHistory();
            if (historyJson != null && !historyJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    approvalHistory = mapper.readValue(
                            historyJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                    approvalHistory = new java.util.ArrayList<>();
                }
            }

            boolean skipCeoHistory = shouldSkipCeoForCapf(form, departmentName, approverUser);
            if (!skipCeoHistory) {
                // Add current approval to history
                // For CAPF, Level 0 is the virtual Initiator HOD stage
                Integer historyLevel;
                if (isCapf && currentLevel == 0) {
                    historyLevel = 0;
                } else {
                    historyLevel = pipelineOrder != null ? pipelineOrder : (currentLevel + 1);
                }

                java.util.Map<String, Object> approvalEntry = new java.util.HashMap<>();
                approvalEntry.put("level", historyLevel);
                approvalEntry.put("departmentId", departmentId);
                approvalEntry.put("departmentName", departmentName != null ? departmentName
                        : (departmentId != null ? "Department " + departmentId : "Unknown"));
                approvalEntry.put("remarks", remarks != null ? remarks : "");
                approvalEntry.put("approvedBy", resolvedApproverId);
                approvalEntry.put("approverName", approverUser.getTxtUserName());
                approvalEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
                approvalEntry.put("signaturePath", approverSignaturePath);
                approvalEntry.put("txtDepartmentName",
                        approverUser.getTxtDepartmentName() != null ? approverUser.getTxtDepartmentName() : "");
                approvalEntry.put("userDepartmentName",
                        approverUser.getTxtDepartmentName() != null ? approverUser.getTxtDepartmentName() : "");
                approvalEntry.put("designation",
                        approverUser.getTxtDesignation() != null ? approverUser.getTxtDesignation() : "");
                approvalEntry.put("txtDesignation",
                        approverUser.getTxtDesignation() != null ? approverUser.getTxtDesignation() : "");
                approvalEntry.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
                approvalEntry.put("approvedIp", approvedIp != null ? approvedIp : "");
                approvalEntry.put("action", "APPROVED");
                approvalEntry.put("role", departmentName != null ? departmentName : "");
                log.info(
                        "CAPF signature log [pipeline-approval-entry]: appId={}, userId={}, level={}, deptId={}, deptName={}, signaturePath={}",
                        application.getSerApplicationId(), resolvedApproverId, historyLevel,
                        departmentId, departmentName, approverSignaturePath != null ? approverSignaturePath : "");
                approvalHistory.add(approvalEntry);

                // Save updated history
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String updatedHistoryJson = mapper.writeValueAsString(approvalHistory);
                    application.setTxtApprovalHistory(updatedHistoryJson);
                } catch (Exception e) {
                    log.error("Error serializing approval history: " + e.getMessage());
                }
            }

            // Enforce multi-HOD approval: if a department lists multiple HOD IDs, require
            // all to approve before advancing
            Integer pipelineLevelIndex;
            if (isCapf && currentLevel == 0) {
                pipelineLevelIndex = 0;
            } else {
                pipelineLevelIndex = pipelineOrder != null ? pipelineOrder : (currentLevel + 1);
            }
            Set<Integer> requiredHods = parseDepartmentHeadIds(entityManager, departmentId);
            if (!requiredHods.isEmpty()) {
                Set<Integer> approvedHods = getApprovedHodsForStage(approvalHistory, departmentId, pipelineLevelIndex);
                approvedHods.add(resolvedApproverId);
                if (!approvedHods.containsAll(requiredHods)) {
                    // Stay on the same level until all HODs approve
                    application.setTxtStatus("IN_PROGRESS");
                    application.setSerCurrentApprover(null); // keep stage open to all HODs
                    application.setIntCurrentApprovalLevel(currentLevel);
                    application.setTxtRemarks(remarks);
                    application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                    application.setSerModifiedUser(resolvedApproverId);

                    // Persist and exit without advancing pipeline
                    entityManager.merge(application);
                    entityManager.getTransaction().commit();
                    return "Success";
                }
            }

            // Increment approval level (advance to next department)
            currentLevel++;

            // Check if this is the last level. 
            // For CAPF, the pipeline starts at index 0 when currentLevel=1.
            // So if currentLevel is 5 and pipelines.size is 4, we are done with pipeline.
            boolean isLastStage;
            if (isCapfForm(form)) {
                isLastStage = pipelines.isEmpty() || currentLevel > pipelines.size();
            } else {
                isLastStage = pipelines.isEmpty() || currentLevel >= pipelines.size();
            }

            if (isLastStage) {
                // All approvals complete -> route to CEO (if available) otherwise go straight
                // to Finance
                application.setIntCurrentApprovalLevel(currentLevel);
                Integer ceoUserId = findFirstUserIdByRole(entityManager, "CEO");
                if (ceoUserId != null) {
                    application.setTxtStatus("CEO_PENDING");
                    application.setSerCurrentApprover(ceoUserId);
                } else {
                    application.setTxtStatus("ASSET_PENDING");
                    Integer financeUserId = findFirstUserIdByRole(entityManager, "FINANCE_HEAD");
                    if (financeUserId == null) {
                        financeUserId = findFirstUserIdByRole(entityManager, "FINANCE");
                    }
                    application.setSerCurrentApprover(financeUserId);
                }
            } else {
                // Move to next level
                application.setTxtStatus("IN_PROGRESS");
                application.setIntCurrentApprovalLevel(currentLevel);
            }

            if (!"CEO_PENDING".equalsIgnoreCase(application.getTxtStatus())) {
                application.setSerCurrentApprover(resolvedApproverId);
            }
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(resolvedApproverId);

            // Preserve the original CAPF PDF to keep emails consistent.
            // For CAPF, persist signatures directly into the stored PDF after each approval
            // so all subsequent views/emails use the same signed document.
            if (isCapfForm(form)) {
                persistCapfSignedPdf(application, form);
            } else {
                // Non-CAPF: only regenerate if no PDF exists.
                boolean shouldRegeneratePdf = application.getBlbPdfData() == null
                        || application.getBlbPdfData().length == 0;
                if (shouldRegeneratePdf) {
                    try {
                        appData = parseApplicationData(application);
                        byte[] pdfBytes = generateApplicationPdf(application, form, appData);
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            String code = application.getTxtFormCode() != null ? application.getTxtFormCode()
                                    : "application";
                            application.setBlbPdfData(pdfBytes);
                            application.setTxtPdfName(buildPdfFileName(form, code));
                            application.setTxtPdfMime("application/pdf");
                        }
                    } catch (Exception e) {
                        log.warn("Error regenerating application PDF: " + e.getMessage(), e);
                    }
                }
            }

            entityManager.merge(application);
            entityManager.getTransaction().commit();

            // Send email notifications after successful approval (use new entity manager
            // since transaction is closed)
            // currentLevel is now the new level (0-indexed) after increment
            // The approved level was at index (currentLevel - 1), but pipelineOrder is
            // 1-indexed
            // If pipelineOrder is null, calculate it: currentLevel (0-indexed) =
            // pipelineOrder (1-indexed)
            Integer approvedPipelineOrder = pipelineOrder != null ? pipelineOrder : (currentLevel);
            try {
                sendApprovalEmails(application, approvedPipelineOrder, currentLevel, pipelines);
            } catch (Exception emailEx) {
                log.error("Error sending approval emails: " + emailEx.getMessage(), emailEx);
                // Don't fail the approval if email fails
            }
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error approving application (rolled back): " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String rejectApplication(Integer applicationId, String remarks) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication application = entityManager.find(
                    CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            // Get the form to check approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = entityManager.find(
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                    application.getSerFormId());

            // Resolve approver
            Integer resolvedApproverId = commonService.getCurrentLoggedInUser();
            CfgTblUser approverUser = null;
            if (resolvedApproverId != null && resolvedApproverId > 0) {
                approverUser = commonService.getCurrentUser(resolvedApproverId);
            }

            application.setTxtStatus("REJECTED");
            application.setSerCurrentApprover(resolvedApproverId);
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(resolvedApproverId);

            // Append rejection to approval history for performance reporting
            try {
                Integer currentLevel = application.getIntCurrentApprovalLevel();
                if (currentLevel == null) {
                    currentLevel = 0;
                }

                Integer departmentId = null;
                String departmentName = null;
                Integer pipelineOrder = null;

                if (form != null && form.getTxtApprovalPipeline() != null &&
                        !form.getTxtApprovalPipeline().trim().isEmpty()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<java.util.Map<String, Object>> pipelines = mapper.readValue(
                            form.getTxtApprovalPipeline(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });

                    boolean isCapf = isCapfForm(form);
                    int pipelineIndex = isCapf ? currentLevel - 1 : currentLevel;

                    if (!pipelines.isEmpty() && pipelineIndex >= 0 && pipelineIndex < pipelines.size()) {
                        java.util.Map<String, Object> currentPipeline = pipelines.get(pipelineIndex);
                        if (currentPipeline != null) {
                            Object deptIdObj = currentPipeline.get("serDepartmentId");
                            Object orderObj = currentPipeline.get("intApprovalOrder");
                            if (deptIdObj != null) {
                                departmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj
                                        : Integer.parseInt(deptIdObj.toString());
                                departmentName = resolveDepartmentName(entityManager, departmentId, currentPipeline);
                            }
                            if (orderObj != null) {
                                pipelineOrder = orderObj instanceof Integer ? (Integer) orderObj
                                        : Integer.parseInt(orderObj.toString());
                            }
                        }
                    } else if (isCapf && currentLevel == 0) {
                        // Level 0 for CAPF is ALWAYS Initiator HOD
                        Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                        if (submitterDeptId != null) {
                            departmentId = submitterDeptId;
                            departmentName = resolveDepartmentName(entityManager, departmentId, null);
                        }
                    }
                }

                // Load existing approval history
                List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
                String historyJson = application.getTxtApprovalHistory();
                if (historyJson != null && !historyJson.trim().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        approvalHistory = mapper.readValue(
                                historyJson,
                                new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                                });
                    } catch (Exception e) {
                        approvalHistory = new java.util.ArrayList<>();
                    }
                }

                java.util.Map<String, Object> rejectionEntry = new java.util.HashMap<>();
                rejectionEntry.put("level", pipelineOrder != null ? pipelineOrder : (currentLevel + 1));
                rejectionEntry.put("departmentId", departmentId);
                rejectionEntry.put("departmentName", departmentName != null ? departmentName
                        : (departmentId != null ? "Department " + departmentId : "Unknown"));
                rejectionEntry.put("remarks", remarks != null ? remarks : "");
                rejectionEntry.put("approvedBy", resolvedApproverId);
                rejectionEntry.put("approverName", approverUser != null ? approverUser.getTxtUserName() : "");
                rejectionEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
                rejectionEntry.put("approvedVia", "SYSTEM");
                rejectionEntry.put("action", "REJECTED");
                approvalHistory.add(rejectionEntry);

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                application.setTxtApprovalHistory(mapper.writeValueAsString(approvalHistory));
            } catch (Exception e) {
                log.warn("Error updating approval history for rejection: " + e.getMessage());
            }

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (

        Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error rejecting application: " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String sendBackApplication(Integer applicationId, String remarks) {
        return sendBackApplication(applicationId, remarks, null);
    }
    
    public String sendBackApplication(Integer applicationId, String remarks, Integer userId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication application = entityManager.find(
                    CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            // Get the form to check approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = entityManager.find(
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                    application.getSerFormId());

            if (form == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Form not found";
            }

            // Deserialize approval pipeline
            String pipelineJson = form.getTxtApprovalPipeline();
            List<java.util.Map<String, Object>> pipelines = new java.util.ArrayList<>();
            if (pipelineJson != null && !pipelineJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pipelines = mapper.readValue(
                            pipelineJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.error("Error parsing approval pipeline: " + e.getMessage());
                    entityManager.getTransaction().rollback();
                    return "Failure: Error parsing approval pipeline";
                }
            }

            Integer currentLevel = application.getIntCurrentApprovalLevel();
            if (currentLevel == null) {
                currentLevel = 0;
            }

            // Get current department info (the department that is sending back)
            java.util.Map<String, Object> currentDepartmentPipeline = null;
            Integer currentDepartmentId = null;
            String currentDepartmentName = null;
            Integer currentPipelineOrder = null;

            boolean isCapf = isCapfForm(form);
            if (!pipelines.isEmpty() && currentLevel > 0) {
                // Get the department at the current level (the one sending back)
                int currentLevelIndex = isCapf ? currentLevel - 1 : currentLevel - 1;
                // Wait, if isCapf is true:
                // Level 1 is Technical Expert (Index 0). Index = 1 - 1 = 0.
                // Level 2 is Procurement (Index 1). Index = 2 - 1 = 1.
                // It seems the current index logic (currentLevel - 1) is ALREADY correct for CAPF
                // because currentLevel is 1-based in this context (Level 1, Level 2, etc.)
                // But wait, if isCapf is false, Level 0 is pipelines[0]. currentLevel would be 0.
                // Let's re-verify the index logic for non-CAPF.
                
                // If non-CAPF, Stage 1 (Index 0) approves, currentLevel becomes 1.
                // If Stage 2 (Index 1) sends back, currentLevel is 1. currentLevelIndex = 1 - 1 = 0. Correct.
                
                // So the index logic (currentLevel - 1) is actually correct for both if currentLevel 
                // represents the current stage's 0-based index.
                
                // HOWEVER, for CAPF, currentLevel 1 is Pipeline Index 0.
                // For non-CAPF, currentLevel 1 is Pipeline Index 1.
                
                int pipelineIndex = isCapf ? currentLevel - 1 : currentLevel;
                
                if (pipelineIndex >= 0 && pipelineIndex < pipelines.size()) {
                    currentDepartmentPipeline = pipelines.get(pipelineIndex);
                    if (currentDepartmentPipeline != null) {
                        Object deptIdObj = currentDepartmentPipeline.get("serDepartmentId");
                        Object orderObj = currentDepartmentPipeline.get("intApprovalOrder");

                        if (deptIdObj != null) {
                            currentDepartmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj
                                    : Integer.parseInt(deptIdObj.toString());
                            currentDepartmentName = resolveDepartmentName(entityManager, currentDepartmentId,
                                    currentDepartmentPipeline);
                        }

                        if (orderObj != null) {
                            currentPipelineOrder = orderObj instanceof Integer ? (Integer) orderObj
                                    : Integer.parseInt(orderObj.toString());
                        }
                    }
                } else if (isCapf && currentLevel == 0) {
                    // Level 0 for CAPF is ALWAYS Initiator HOD
                    Integer submitterDeptId = loadUserDepartmentId(entityManager, application.getSerSubmittedBy());
                    if (submitterDeptId != null) {
                        currentDepartmentId = submitterDeptId;
                        currentDepartmentName = resolveDepartmentName(entityManager, currentDepartmentId, null);
                    }
                }
            }

            // Check if this is an individual pipeline footer form
            Map<String, Object> appData = parseApplicationData(application);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;

            // Get or create approval history array
            List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
            String historyJson = application.getTxtApprovalHistory();
            if (historyJson != null && !historyJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    approvalHistory = mapper.readValue(
                            historyJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                    approvalHistory = new java.util.ArrayList<>();
                }
            }

            // Store original level and decrement for send back
            Integer originalLevel = currentLevel;
            if (currentLevel > 0) {
                currentLevel--;
            }

            // Keep ALL approval history entries - don't remove them, just mark entries that need re-approval
            // The frontend will filter them out for display based on currentLevel
            List<java.util.Map<String, Object>> updatedHistory = new java.util.ArrayList<>(approvalHistory);

            // Get current user who is sending back
            Integer currentUserId = commonService.getCurrentLoggedInUser();
            CfgTblUser currentUser = null;
            if (currentUserId != null) {
                currentUser = commonService.getCurrentUser(currentUserId);
                // Fallback to entityManager if commonService doesn't return user
                if (currentUser == null) {
                    currentUser = entityManager.find(CfgTblUser.class, currentUserId);
                }
            }
            
            // For individual pipeline footer forms, get the role from the sequence
            String sendBackRole = null;
            if (useIndividualPipelineFlow) {
                try {
                    List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
                    if (originalLevel != null && originalLevel >= 0 && originalLevel < sequence.size()) {
                        BudgetApprover currentApprover = sequence.get(originalLevel);
                        if (currentApprover != null && currentApprover.role != null) {
                            sendBackRole = currentApprover.role;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error getting role from sequence for send back: " + e.getMessage());
                }
            }

            // Add send-back action to history with proper data
            java.util.Map<String, Object> sendBackEntry = new java.util.HashMap<>();
            sendBackEntry.put("action", "SENT_BACK");
            
            // Log clearer transition in history
            int fromLevel = originalLevel != null ? originalLevel : 0;
            int toLevel = (fromLevel > 0) ? fromLevel - 1 : 0;
            
            sendBackEntry.put("level", fromLevel + 1); // 1-indexed for consistency with approval entries
            sendBackEntry.put("fromLevel", fromLevel + 1); // 1-indexed for display
            sendBackEntry.put("toLevel", toLevel + 1);     // 1-indexed for display
            sendBackEntry.put("departmentId", currentDepartmentId);
            
            // For individual pipeline footer forms, use role as departmentName
            if (useIndividualPipelineFlow && sendBackRole != null) {
                sendBackEntry.put("departmentName", sendBackRole);
                sendBackEntry.put("role", sendBackRole);
            } else {
                sendBackEntry.put("departmentName", currentDepartmentName != null ? currentDepartmentName
                        : (currentDepartmentId != null ? "Department " + currentDepartmentId : "Unknown"));
            }
            
            sendBackEntry.put("remarks", remarks != null ? remarks : "");
            sendBackEntry.put("sentBackBy", currentUserId);
            sendBackEntry.put("sentBackDate", commonService.getCurrentTimeStamp_new().toString());
            sendBackEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString()); // For consistency
            
            // Add user details like approval entries
            if (currentUser != null) {
                sendBackEntry.put("approvedBy", currentUserId);
                sendBackEntry.put("approverName", currentUser.getTxtUserName());
                sendBackEntry.put("userId", currentUserId);
                sendBackEntry.put("txtDepartmentName", currentUser.getTxtDepartmentName() != null ? currentUser.getTxtDepartmentName() : "");
                sendBackEntry.put("userDepartmentName", currentUser.getTxtDepartmentName() != null ? currentUser.getTxtDepartmentName() : "");
                sendBackEntry.put("designation", currentUser.getTxtDesignation() != null ? currentUser.getTxtDesignation() : "");
                sendBackEntry.put("txtDesignation", currentUser.getTxtDesignation() != null ? currentUser.getTxtDesignation() : "");
            } else {
                sendBackEntry.put("approvedBy", currentUserId);
                sendBackEntry.put("approverName", "");
            }
            
            log.info("Application sent back from Stage {} to Stage {} for appId={}", 
                fromLevel + 1, toLevel + 1, application.getSerApplicationId());
            
            updatedHistory.add(sendBackEntry);

            // Save updated history
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String updatedHistoryJson = mapper.writeValueAsString(updatedHistory);
                application.setTxtApprovalHistory(updatedHistoryJson);
                
                // For individual pipeline footer forms, update signatures on existing PDF instead of clearing it
                // This preserves the formatted document layout while removing signatures
                if (hasDynamicFooterFlow && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                    try {
                        byte[] signedPdf = applyDynamicFooterSignaturesToPdf(
                                application.getBlbPdfData(),
                                appData,
                                updatedHistoryJson);
                        if (signedPdf != null && signedPdf.length > 0) {
                            application.setBlbPdfData(signedPdf);
                            log.info("Updated signatures in PDF for appId={} when sending back from level {} to {}", 
                                application.getSerApplicationId(), originalLevel, currentLevel);
                        } else {
                            // If signature update fails, clear PDF to force regeneration
                            application.setBlbPdfData(null);
                            application.setTxtPdfName(null);
                            application.setTxtPdfMime(null);
                            log.warn("Failed to update signatures in PDF, cleared PDF for appId={}", 
                                application.getSerApplicationId());
                        }
                    } catch (Exception e) {
                        log.warn("Error updating signatures in PDF during send-back: " + e.getMessage(), e);
                        // Clear PDF if signature update fails
                        application.setBlbPdfData(null);
                        application.setTxtPdfName(null);
                        application.setTxtPdfMime(null);
                    }
                } else {
                    // For non-individual pipeline footer forms, clear the PDF data so it gets regenerated
                    application.setBlbPdfData(null);
                    application.setTxtPdfName(null);
                    application.setTxtPdfMime(null);
                    
                    log.info("Cleared signatures and PDF for appId={} when sending back from level {} to {}", 
                        application.getSerApplicationId(), originalLevel, currentLevel);
                }
                    
            } catch (Exception e) {
                log.error("Error serializing approval history: " + e.getMessage());
            }

            // Update status based on new level
            if (currentLevel <= 0) {
                // Back to initial state
                application.setTxtStatus("PENDING");
                application.setIntCurrentApprovalLevel(0);
            } else {
                // Back to a previous department
                application.setTxtStatus("IN_PROGRESS");
                application.setIntCurrentApprovalLevel(currentLevel);
            }

            application.setSerCurrentApprover(null); // Clear current approver
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            
            // Send email notification to previous department after successful send back
            try {
                sendBackEmailNotification(application, originalLevel, currentLevel, pipelines);
            } catch (Exception emailEx) {
                log.error("Error sending send-back email notification: " + emailEx.getMessage(), emailEx);
                // Don't fail the send back if email fails
            }
            
            return "Success";
        } catch (

        Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error sending back application: " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String sendBackApplicationToInitiator(Integer applicationId, String remarks) {
        return sendBackApplicationToInitiator(applicationId, remarks, null);
    }
    
    public String sendBackApplicationToInitiator(Integer applicationId, String remarks, Integer userId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class,
                    applicationId);
            if (application == null) {
                return "Failure: Application not found";
            }

            // Fetch the form
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = null;
            if (application.getSerFormId() != null) {
                form = entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                        application.getSerFormId());
            }

            // Check if this is an individual pipeline footer form
            Map<String, Object> appData = parseApplicationData(application);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;

            Integer currentLevel = application.getIntCurrentApprovalLevel();
            Integer currentDepartmentId = null;
            String currentDepartmentName = null;

            List<java.util.Map<String, Object>> pipelines = new java.util.ArrayList<>();
            if (form != null && form.getTxtApprovalPipeline() != null
                    && !form.getTxtApprovalPipeline().trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pipelines = mapper.readValue(
                            form.getTxtApprovalPipeline(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.error("Error parsing approval pipeline for send back to initiator: " + e.getMessage(), e);
                }
            }

            if (pipelines != null && !pipelines.isEmpty() && currentLevel != null && currentLevel >= 0
                    && currentLevel < pipelines.size()) {
                java.util.Map<String, Object> currentPipeline = pipelines.get(currentLevel);
                currentDepartmentId = (Integer) currentPipeline.get("serDepartmentId");
                currentDepartmentName = resolveDepartmentName(entityManager, currentDepartmentId, currentPipeline);
            }

            // Reset to 0 for send back to first person in pipeline
            Integer originalLevel = currentLevel;
            currentLevel = 0; 

            // Get or create approval history array
            List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
            String historyJson = application.getTxtApprovalHistory();
            if (historyJson != null && !historyJson.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    approvalHistory = mapper.readValue(
                            historyJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                    approvalHistory = new java.util.ArrayList<>();
                }
            }

            // Keep ALL approval history entries - don't remove them, just mark entries that need re-approval
            // The frontend will filter them out for display based on currentLevel
            List<java.util.Map<String, Object>> updatedHistory = new java.util.ArrayList<>(approvalHistory);

            // Get current user who is sending back - use provided userId or fallback to logged in user
            Integer currentUserId = userId != null ? userId : commonService.getCurrentLoggedInUser();
            CfgTblUser currentUser = null;
            if (currentUserId != null) {
                currentUser = commonService.getCurrentUser(currentUserId);
                // Fallback to entityManager if commonService doesn't return user
                if (currentUser == null) {
                    currentUser = entityManager.find(CfgTblUser.class, currentUserId);
                }
            }
            
            // For individual pipeline footer forms, get the role from the sequence
            String sendBackRole = null;
            if (useIndividualPipelineFlow) {
                try {
                    List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
                    if (originalLevel != null && originalLevel >= 0 && originalLevel < sequence.size()) {
                        BudgetApprover currentApprover = sequence.get(originalLevel);
                        if (currentApprover != null && currentApprover.role != null) {
                            sendBackRole = currentApprover.role;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error getting role from sequence for send back to initiator: " + e.getMessage());
                }
            }
            
            // Add send-back action to history with proper data
            java.util.Map<String, Object> sendBackEntry = new java.util.HashMap<>();
            sendBackEntry.put("action", "SENT_BACK_TO_INITIATOR");
            
            int fromLevel = originalLevel != null ? originalLevel : 0;
            
            sendBackEntry.put("level", fromLevel + 1); // 1-indexed for consistency with approval entries
            sendBackEntry.put("fromLevel", fromLevel + 1); 
            sendBackEntry.put("toLevel", 1);     
            sendBackEntry.put("departmentId", currentDepartmentId);
            
            // For individual pipeline footer forms, use role as departmentName
            if (useIndividualPipelineFlow && sendBackRole != null) {
                sendBackEntry.put("departmentName", sendBackRole);
                sendBackEntry.put("role", sendBackRole);
            } else {
                sendBackEntry.put("departmentName", currentDepartmentName != null ? currentDepartmentName
                        : (currentDepartmentId != null ? "Department " + currentDepartmentId : "Unknown"));
            }
            
            sendBackEntry.put("remarks", remarks != null ? remarks : "");
            sendBackEntry.put("sentBackBy", currentUserId);
            sendBackEntry.put("sentBackDate", commonService.getCurrentTimeStamp_new().toString());
            sendBackEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString()); // For consistency
            
            // Add user details like approval entries
            if (currentUser != null) {
                sendBackEntry.put("approvedBy", currentUserId);
                sendBackEntry.put("approverName", currentUser.getTxtUserName());
                sendBackEntry.put("userId", currentUserId);
                sendBackEntry.put("txtDepartmentName", currentUser.getTxtDepartmentName() != null ? currentUser.getTxtDepartmentName() : "");
                sendBackEntry.put("userDepartmentName", currentUser.getTxtDepartmentName() != null ? currentUser.getTxtDepartmentName() : "");
                sendBackEntry.put("designation", currentUser.getTxtDesignation() != null ? currentUser.getTxtDesignation() : "");
                sendBackEntry.put("txtDesignation", currentUser.getTxtDesignation() != null ? currentUser.getTxtDesignation() : "");
            } else {
                sendBackEntry.put("approvedBy", currentUserId);
                sendBackEntry.put("approverName", "");
            }
            
            log.info("Application sent back to first person in pipeline from Stage {} for appId={}", 
                fromLevel + 1, application.getSerApplicationId());
            
            updatedHistory.add(sendBackEntry);

            // Save updated history
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String updatedHistoryJson = mapper.writeValueAsString(updatedHistory);
                application.setTxtApprovalHistory(updatedHistoryJson);
                
                // For individual pipeline footer forms, update signatures on existing PDF instead of clearing it
                if (hasDynamicFooterFlow && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                    try {
                        byte[] signedPdf = applyDynamicFooterSignaturesToPdf(
                                application.getBlbPdfData(),
                                appData,
                                updatedHistoryJson);
                        if (signedPdf != null && signedPdf.length > 0) {
                            application.setBlbPdfData(signedPdf);
                            log.info("Updated signatures in PDF for appId={} when sending back to first person from level {}", 
                                application.getSerApplicationId(), originalLevel);
                        } else {
                            application.setBlbPdfData(null);
                            application.setTxtPdfName(null);
                            application.setTxtPdfMime(null);
                            log.warn("Failed to update signatures in PDF, cleared PDF for appId={}", 
                                application.getSerApplicationId());
                        }
                    } catch (Exception e) {
                        log.warn("Error updating signatures in PDF during send back to first person: " + e.getMessage(), e);
                        application.setBlbPdfData(null);
                        application.setTxtPdfName(null);
                        application.setTxtPdfMime(null);
                    }
                } else {
                    // For non-individual pipeline footer forms, clear the PDF data so it gets regenerated
                    application.setBlbPdfData(null);
                    application.setTxtPdfName(null);
                    application.setTxtPdfMime(null);
                }
            } catch (Exception e) {
                log.error("Error serializing approval history: " + e.getMessage());
            }

            application.setTxtStatus("PENDING"); 
            application.setIntCurrentApprovalLevel(0);
            application.setSerCurrentApprover(null); 
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            
            // Send email notification to first person in pipeline after successful send back
            try {
                if (useIndividualPipelineFlow) {
                    // For individual pipeline footer forms, send email to first person in sequence
                    List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
                    if (!sequence.isEmpty()) {
                        BudgetApprover firstApprover = sequence.get(0);
                        if (firstApprover != null && firstApprover.email != null && !firstApprover.email.trim().isEmpty()) {
                            String baseUrl = getBaseUrl();
                            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                                    + application.getSerApplicationId() + "&userId=" + firstApprover.userId;
                            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                                    + application.getSerApplicationId() + "&userId=" + firstApprover.userId;
                            String sendBackUrl = null;
                            String sendBackToInitiatorUrl = null;
                            
                            boolean isCapf = isCapfForm(form);
                            String cid = isCapf ? "capf-inline" : "form-inline";
                            String formName = getResolvedFormName(form);
                            
                            String subject = formName + " Sent Back - Requires Your Approval - " + 
                                (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                            
                            String html = generateApprovalEmailHtml(
                                firstApprover.name != null ? firstApprover.name : "User",
                                1, // level (Stage number, 1-indexed)
                                application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                formName,
                                "IN_PROGRESS", // status - showing it's in progress after send back
                                application.getTxtRemarks(),
                                true, // showActionButtons
                                approveUrl,
                                rejectUrl,
                                sendBackUrl,
                                sendBackToInitiatorUrl,
                                application.getTxtApprovalHistory(),
                                baseUrl
                            );
                            
                            sendEmailWithInlineFormPreview(
                                java.util.Arrays.asList(firstApprover.email),
                                subject,
                                html,
                                application,
                                form,
                                isCapf,
                                cid
                            );
                            
                            log.info("Send-back to first person notification email sent to: {} at sequence index 0", 
                                firstApprover.email);
                        }
                    }
                } else {
                    // For regular pipeline forms, use existing sendBackEmailNotification
                    sendBackEmailNotification(application, originalLevel, currentLevel, pipelines);
                }
            } catch (Exception emailEx) {
                log.error("Error sending send-back email notification: " + emailEx.getMessage(), emailEx);
            }
            
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error sending back application to initiator: " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }


    @Override
    public String sendSubmissionEmailsForApplication(Integer applicationId) {
        EntityManager entityManager = getEntityManager();
        try {
            if (applicationId == null) {
                return "Failure: Application ID is required";
            }

            entityManager.getTransaction().begin();
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class,
                    applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = application.getSerFormId() != null
                    ? entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                            application.getSerFormId())
                    : null;
            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;
            Map<String, Object> appData = parseApplicationData(application);
            String quotationAttachmentHtml = buildQuotationAttachmentHtml(appData);

            entityManager.getTransaction().commit();

            try {
                if (useIndividualPipelineFlow) {
                    sendBudgetApprovalNextEmail(application, 0);
                }
                sendSubmissionEmails(application);
            } catch (Exception emailEx) {
                log.error("Error sending submission emails: " + emailEx.getMessage(), emailEx);
            }
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error in sendSubmissionEmailsForApplication: " + e.getMessage(), e);
            return "Failure: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String assignAssetCode(Integer applicationId, String assetCode, Integer userId, String approvedIp) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class,
                    applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }
            if (!"ASSET_PENDING".equalsIgnoreCase(application.getTxtStatus())
                    && !"ASSET_CODE_PENDING".equalsIgnoreCase(application.getTxtStatus())) {
                entityManager.getTransaction().rollback();
                return "Failure: Application is not pending asset code";
            }
            if (assetCode == null || assetCode.trim().isEmpty()) {
                entityManager.getTransaction().rollback();
                return "Failure: Asset code is required";
            }
            application.setTxtAssetCode(assetCode.trim());
            application.setTxtStatus("APPROVED");
            application.setSerCurrentApprover(null);
            appendHistoryEntry(entityManager, application, userId, "APPROVED", "FINANCE", 999, "SYSTEM", approvedIp);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(application);
            entityManager.getTransaction().commit();

            try {
                sendFinalInitiatorEmail(application);
            } catch (Exception e) {
                log.warn("Failed to send final initiator email after asset code: {}", e.getMessage());
            }
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error assigning asset code: " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String assignPrCode(Integer applicationId, String prCode, Integer userId, String approvedIp) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class,
                    applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }
            if (prCode == null || prCode.trim().isEmpty()) {
                entityManager.getTransaction().rollback();
                return "Failure: PR code is required";
            }
            if (application.getTxtAssetCode() == null || application.getTxtAssetCode().trim().isEmpty()) {
                entityManager.getTransaction().rollback();
                return "Failure: Asset code must be assigned first";
            }
            if (application.getTxtPrCode() != null && prCode.trim().equalsIgnoreCase(application.getTxtPrCode())) {
                entityManager.getTransaction().rollback();
                return "Failure: PR code is unchanged";
            }
            application.setTxtPrCode(prCode.trim());
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            appendHistoryEntry(entityManager, application, userId, "PR_CODE_ASSIGNED", "INITIATOR",
                    currentLevelSafe(application), "SYSTEM", approvedIp);
            entityManager.merge(application);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error assigning PR code: " + e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    /**
     * Send email notifications when an application is approved
     * Sends email to:
     * 1. The user who submitted the application (notifying them of approval at
     * current level)
     * 2. The next level department head (if there is a next level)
     * 
     * @param application           The approved application
     * @param approvedPipelineOrder The pipeline order (1-indexed) that was just
     *                              approved
     * @param currentLevel          The new current level (0-indexed) after approval
     * @param pipelines             The approval pipeline list
     */
    private void sendApprovalEmails(CfgTblCustomFormApplication application, Integer approvedPipelineOrder,
            Integer currentLevel, List<java.util.Map<String, Object>> pipelines) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            String formName = "Unknown Form";
            CfgTblCustomForm form = application.getCfgTblCustomForm();
            if (form == null && application.getSerFormId() != null) {
                form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            }
            boolean isCapf = isCapfForm(form);
            log.info("Email debug [sendApprovalEmails]: appId={}, isCapf={}, formName={}, formCode={}",
                    application.getSerApplicationId(), isCapf,
                    form != null ? form.getTxtFormName() : "null",
                    form != null ? form.getTxtFormCode() : "null");

            // Get form name
            if (form != null && form.getTxtFormName() != null) {
                formName = form.getTxtFormName();
            }

            // 1. Get email of the user who submitted the application
            if (application.getSerSubmittedBy() != null) {
                try {
                    CfgTblUser submittedByUser = emailEntityManager.find(CfgTblUser.class,
                            application.getSerSubmittedBy());
                    if (submittedByUser != null && submittedByUser.getTxtAddress() != null &&
                            !submittedByUser.getTxtAddress().trim().isEmpty()) {

                        // Send HTML email to submitter
                        String submitterSubject = formName + " Approved at Level " + approvedPipelineOrder + " - " +
                                (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtmlMessage = generateApprovalEmailHtml(
                                submittedByUser.getTxtUserName() != null ? submittedByUser.getTxtUserName() : "User",
                                approvedPipelineOrder,
                                application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                formName,
                                application.getTxtStatus(),
                                application.getTxtRemarks(),
                                false, // Not for department head
                                null, // No action buttons for submitter
                                null,
                                null,
                                null,
                                application.getTxtApprovalHistory(),
                                getBaseUrl());

                        sendEmailWithInlineFormPreview(
                                java.util.Arrays.asList(submittedByUser.getTxtAddress()),
                                submitterSubject,
                                submitterHtmlMessage,
                                application,
                                form,
                                isCapf,
                                isCapf ? "capf-inline" : "form-inline");
                        log.info("Approval email sent to submitter: " + submittedByUser.getTxtAddress());
                    }
                } catch (Exception e) {
                    log.error("Error getting submitter email: " + e.getMessage(), e);
                }
            }

            // 2. Get email of the next level department head (if there is a next level)
            // For CAPF, Level 0 was HOD. Level 1 is Pipeline Index 0.
            int nextPipelineIndex = isCapf ? currentLevel - 1 : currentLevel;

            boolean isLastStage;
            if (isCapf) {
                isLastStage = pipelines == null || pipelines.isEmpty() || currentLevel > pipelines.size();
            } else {
                isLastStage = pipelines == null || pipelines.isEmpty() || currentLevel >= pipelines.size();
            }

            if (isLastStage) {
                // If it was the last stage, send CEO/Finance emails instead of next department
                if ("CEO_PENDING".equalsIgnoreCase(application.getTxtStatus())) {
                    sendCeoApprovalEmails(application, form);
                } else if ("ASSET_PENDING".equalsIgnoreCase(application.getTxtStatus())) {
                    sendFinanceEmails(application, null);
                }
                return;
            }

            if (pipelines != null && !pipelines.isEmpty() && nextPipelineIndex >= 0 && nextPipelineIndex < pipelines.size()) {
                try {
                    // Get the next level pipeline
                    java.util.Map<String, Object> nextLevelPipeline = pipelines.get(nextPipelineIndex);
                    if (nextLevelPipeline != null) {
                        Object deptIdObj = nextLevelPipeline.get("serDepartmentId");
                        if (deptIdObj != null) {
                            Integer nextDeptId = deptIdObj instanceof Integer ? (Integer) deptIdObj
                                    : Integer.parseInt(deptIdObj.toString());

                            // Get the department with department head
                            emailEntityManager.getTransaction().begin();
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment nextDept = emailEntityManager
                                    .find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, nextDeptId);

                            if (nextDept != null) {
                                String nextDeptName = resolveDepartmentName(emailEntityManager, nextDeptId,
                                        nextLevelPipeline);

                                // Dynamic CAPF stage: "User Dept (HoD)" in pipeline should route to
                                // submitter's HOD
                                if (isUserDepartmentHodStage(nextLevelPipeline, nextDeptName)) {
                                    Integer submitterDeptId = loadUserDepartmentId(emailEntityManager,
                                            application.getSerSubmittedBy());
                                    if (submitterDeptId != null) {
                                        nextDeptId = submitterDeptId;
                                        nextDept = emailEntityManager.find(
                                                com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class,
                                                nextDeptId);
                                        nextDeptName = resolveDepartmentName(emailEntityManager, nextDeptId, null);
                                    }
                                }

                                // Collect all Head IDs
                                java.util.List<Integer> headIds = new java.util.ArrayList<>();
                                String headIdsStr = nextDept.getSerDepartmentHeadId();
                                if (headIdsStr != null && !headIdsStr.trim().isEmpty()) {
                                    for (String id : headIdsStr.split(",")) {
                                        try {
                                            headIds.add(Integer.parseInt(id.trim()));
                                        } catch (Exception e) {
                                        }
                                    }
                                }

                                // Fallback if no head IDs found
                                if (headIds.isEmpty()) {
                                    Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, nextDeptId);
                                    if (fallbackHeadId != null)
                                        headIds.add(fallbackHeadId);
                                }

                                for (Integer currentHeadId : headIds) {
                                    Integer headId = currentHeadId;
                                    CfgTblUser nextDeptHead = headId != null
                                            ? emailEntityManager.find(CfgTblUser.class, headId)
                                            : null;

                                    if (nextDeptHead != null && nextDeptHead.getTxtAddress() != null &&
                                            !nextDeptHead.getTxtAddress().trim().isEmpty()) {

                                        if (shouldSkipCeoForCapf(form, nextDeptName, nextDeptHead)) {
                                            log.info("Skipping CAPF CEO approval email for user: "
                                                    + nextDeptHead.getSerUserId());
                                        } else {
                                            // Get the next level's pipeline order
                                            Object nextOrderObj = nextLevelPipeline.get("intApprovalOrder");
                                            Integer nextLevelOrder = nextOrderObj != null
                                                    ? (nextOrderObj instanceof Integer ? (Integer) nextOrderObj
                                                            : Integer.parseInt(nextOrderObj.toString()))
                                                    : (currentLevel + 1);

                                            // Send HTML email to next level department head with approve/reject buttons
                                            String deptHeadSubject = formName + " Pending Approval - Level "
                                                    + nextLevelOrder +
                                                    " - "
                                                    + (application.getTxtFormCode() != null
                                                            ? application.getTxtFormCode()
                                                            : "N/A");
                                            String baseUrl = getBaseUrl();
                                            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                                                    + application.getSerApplicationId() +
                                                    "&userId=" + nextDeptHead.getSerUserId();
                                            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                                                    + application.getSerApplicationId() +
                                                    "&userId=" + nextDeptHead.getSerUserId();

                                            String sendBackUrl = baseUrl
                                                    + "/sendBackApplicationFromEmail?applicationId="
                                                    + application.getSerApplicationId() +
                                                    "&userId=" + nextDeptHead.getSerUserId();

                                            String sendBackToInitiatorUrl = baseUrl
                                                    + "/sendBackToInitiatorFromEmail?applicationId="
                                                    + application.getSerApplicationId() +
                                                    "&userId=" + nextDeptHead.getSerUserId();

                                            String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                                    nextDeptHead.getTxtUserName() != null
                                                            ? nextDeptHead.getTxtUserName()
                                                            : "Department Head",
                                                    nextLevelOrder,
                                                    application.getTxtFormCode() != null ? application.getTxtFormCode()
                                                            : "N/A",
                                                    formName,
                                                    application.getTxtStatus(),
                                                    null,
                                                    true, // For department head
                                                    approveUrl,
                                                    rejectUrl,
                                                    sendBackUrl,
                                                    sendBackToInitiatorUrl,
                                                    application.getTxtApprovalHistory(),
                                                    getBaseUrl());

                                            sendEmailWithInlineFormPreview(
                                                    java.util.Arrays.asList(nextDeptHead.getTxtAddress()),
                                                    deptHeadSubject,
                                                    deptHeadHtmlMessage,
                                                    application,
                                                    form,
                                                    isCapf,
                                                    isCapf ? "capf-inline" : "form-inline");
                                            log.info("Approval notification email sent to next level department head: "
                                                    + nextDeptHead.getTxtAddress());
                                        }
                                    }
                                } // End currentHeadId loop
                                emailEntityManager.getTransaction().commit();
                            } else {
                                emailEntityManager.getTransaction().rollback();
                            }
                        }
                    }
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.error("Error getting next level department head email: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in sendApprovalEmails: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    private void sendCeoApprovalEmails(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            java.util.List<String> ceoEmails = findEmailsByRole(emailEntityManager, "CEO");
            if (ceoEmails == null || ceoEmails.isEmpty()) {
                log.warn("CEO notification skipped (no CEO emails) for appId={}", application.getSerApplicationId());
                emailEntityManager.getTransaction().rollback();
                return;
            }
            String formName = form != null ? form.getTxtFormName() : "Application";
            String baseUrl = getBaseUrl();
            Integer ceoUserId = findFirstUserIdByRole(emailEntityManager, "CEO");
            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                    + application.getSerApplicationId()
                    + "&userId=" + (ceoUserId != null ? ceoUserId : "");
            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                    + application.getSerApplicationId()
                    + "&userId=" + (ceoUserId != null ? ceoUserId : "");
            String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId="
                    + application.getSerApplicationId()
                    + "&userId=" + (ceoUserId != null ? ceoUserId : "");

            String subject = "CEO Approval Required - " + (application.getTxtFormCode() != null
                    ? application.getTxtFormCode()
                    : formName);

            String sendBackToInitiatorUrl = baseUrl + "/sendBackToInitiatorFromEmail?applicationId="
                    + application.getSerApplicationId()
                    + "&userId=" + (ceoUserId != null ? ceoUserId : "");

            String html = generateApprovalEmailHtml(
                    "CEO",
                    currentLevelSafe(application),
                    application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                    formName,
                    "CEO_APPROVAL_REQUIRED",
                    null,
                    true,
                    approveUrl,
                    rejectUrl,
                    sendBackUrl,
                    sendBackToInitiatorUrl,
                    application.getTxtApprovalHistory(),
                    getBaseUrl());

            if (isCapfForm(form)) {
                String cid = "capf-inline";
                byte[] imageBytes = buildCapfPreviewPng(application, form);
                if (imageBytes != null && imageBytes.length > 0) {
                    html = appendCapfInlineImage(html, cid);
                    emailService.sendHtmlEmailWithInlineImage(ceoEmails, subject, html, imageBytes, "image/png", cid);
                } else {
                    emailService.sendHtmlEmail(ceoEmails, subject, html);
                }
            } else {
                emailService.sendHtmlEmail(ceoEmails, subject, html);
            }
            emailEntityManager.getTransaction().commit();
            log.info("CEO approval emails sent for appId={} to {}", application.getSerApplicationId(), ceoEmails);
        } catch (Exception e) {
            if (emailEntityManager.getTransaction().isActive())
                emailEntityManager.getTransaction().rollback();
            log.error("Error sending CEO approval emails: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    private void sendFinanceEmails(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            java.util.List<String> financeEmails = findEmailsByRole(emailEntityManager, "FINANCE_HEAD");
            if (financeEmails == null || financeEmails.isEmpty()) {
                financeEmails = findEmailsByRole(emailEntityManager, "FINANCE");
            }
            if (financeEmails == null || financeEmails.isEmpty()) {
                log.warn("Finance notification skipped (no finance emails) for appId={}",
                        application.getSerApplicationId());
                emailEntityManager.getTransaction().rollback();
                return;
            }
            String formName = form != null ? form.getTxtFormName() : "Application";
            String subject = "Finance Action Required (Asset Code) - " + (application.getTxtFormCode() != null
                    ? application.getTxtFormCode()
                    : formName);

            String frontendUrl = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:4200";
            // direct the finance HOD to the dedicated asset-code page
            String assignUrl = frontendUrl + "/velocity/assign-asset-code/" + application.getSerApplicationId();

            StringBuilder html = new StringBuilder();
            html.append("<p>Dear Finance Team,</p>");
            html.append("<p>The application <strong>").append(application.getTxtFormCode())
                    .append("</strong> is awaiting asset code assignment.</p>");
            html.append("<p>Please review and assign the asset code:</p>");
            html.append("<p><a href='").append(assignUrl).append(
                    "' style='padding:10px 16px;background:#2c7be5;color:#fff;text-decoration:none;border-radius:4px;'>Assign Asset Code</a></p>");
            html.append(
                    "<p>If you need to reject or send back, use the standard action buttons in the application.</p>");
            html.append("<p>Thank you.</p>");

            if (isCapfForm(form)) {
                String cid = "capf-inline";
                byte[] imageBytes = buildCapfPreviewPng(application, form);
                String htmlStr = html.toString();
                if (imageBytes != null && imageBytes.length > 0) {
                    htmlStr = appendCapfInlineImage(htmlStr, cid);
                    emailService.sendHtmlEmailWithInlineImage(financeEmails, subject, htmlStr, imageBytes, "image/png",
                            cid);
                } else {
                    emailService.sendHtmlEmail(financeEmails, subject, htmlStr);
                }
            } else {
                emailService.sendHtmlEmail(financeEmails, subject, html.toString());
            }
            emailEntityManager.getTransaction().commit();
            log.info("Finance emails sent for appId={} to {}", application.getSerApplicationId(), financeEmails);
        } catch (Exception e) {
            if (emailEntityManager.getTransaction().isActive())
                emailEntityManager.getTransaction().rollback();
            log.error("Error sending Finance emails: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    private void sendFinalInitiatorEmail(CfgTblCustomFormApplication application) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            if (application.getSerSubmittedBy() == null) {
                emailEntityManager.getTransaction().rollback();
                return;
            }
            CfgTblUser submitter = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
            if (submitter == null || submitter.getTxtAddress() == null || submitter.getTxtAddress().trim().isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                return;
            }
            String subject = "Application Approved - " + (application.getTxtFormCode() != null
                    ? application.getTxtFormCode()
                    : "Application");
            String frontendUrl = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:4200";
            if (!frontendUrl.endsWith("/")) {
                frontendUrl += "/";
            }
            String prUrl = frontendUrl + "velocity/pr-code/" + application.getSerApplicationId();
            StringBuilder html = new StringBuilder();
            html.append("<p>Dear ").append(submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User")
                    .append(",</p>");
            html.append("<p>Your application has been fully approved.</p>");
            if (application.getTxtAssetCode() != null) {
                html.append("<p>Asset Code: <strong>").append(application.getTxtAssetCode()).append("</strong></p>");
            }
            if (application.getTxtPrCode() != null) {
                html.append("<p>PR Code: <strong>").append(application.getTxtPrCode()).append("</strong></p>");
            }
            html.append("<p style='margin-top:18px;'>To proceed with purchasing, please assign the PR code:</p>");
            html.append(
                    "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='margin:0 0 12px 0;'><tr><td align='left' style='border-radius:6px' bgcolor='#2c7be5'>");
            html.append("<a href='").append(prUrl)
                    .append("' style='font-family:Arial,sans-serif;padding:12px 18px;display:inline-block;color:#ffffff;text-decoration:none;font-weight:600;background:#2c7be5;border-radius:6px;'>Assign / Update PR Code</a>");
            html.append("</td></tr></table>");
            html.append(
                    "<p style='font-size:12px;color:#444;margin-top:4px;'>If the button does not work, copy and paste this link into your browser:<br><a href='")
                    .append(prUrl).append("'>").append(prUrl).append("</a></p>");

            html.append("<p>Thank you.</p>");
            emailService.sendHtmlEmail(java.util.Arrays.asList(submitter.getTxtAddress()), subject, html.toString());
            emailEntityManager.getTransaction().commit();
        } catch (Exception e) {
            if (emailEntityManager.getTransaction().isActive())
                emailEntityManager.getTransaction().rollback();
            log.warn("Failed to send final initiator email: {}", e.getMessage());
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    /**
     * Send email notifications when an application is submitted
     * Sends email to:
     * 1. The user who submitted the application (confirmation)
     * 2. The first level department head (notification of new application pending
     * approval)
     */
    private void sendSubmissionEmails(CfgTblCustomFormApplication application) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();

            // Fetch the form with approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = null;
            if (application.getSerFormId() != null) {
                form = emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
                        application.getSerFormId());
            }

            String formName = "Unknown Form";
            if (form != null) {
                formName = form.getTxtFormName();
            }

            boolean isCapf = isCapfForm(form);
            log.info("Email debug [sendSubmissionEmails]: appId={}, isCapf={}, formName={}, formCode={}",
                    application.getSerApplicationId(), isCapf, formName,
                    application.getTxtFormCode() != null ? application.getTxtFormCode() : "null");

            Map<String, Object> appData = parseApplicationData(application);
            String quotationAttachmentHtml = buildQuotationAttachmentHtml(appData);

            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;

            // Get approval pipeline from form
            List<java.util.Map<String, Object>> pipelines = new java.util.ArrayList<>();
            if (form != null && form.getTxtApprovalPipeline() != null &&
                    !form.getTxtApprovalPipeline().trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pipelines = mapper.readValue(
                            form.getTxtApprovalPipeline(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {
                            });
                } catch (Exception e) {
                    log.error("Error parsing approval pipeline for submission emails: " + e.getMessage(), e);
                }
            }

            emailEntityManager.getTransaction().commit();

            // 1. Send email to the user who submitted the application
            if (application.getSerSubmittedBy() != null) {
                try {
                    emailEntityManager.getTransaction().begin();
                    CfgTblUser submittedByUser = emailEntityManager.find(CfgTblUser.class,
                            application.getSerSubmittedBy());
                    if (submittedByUser != null && submittedByUser.getTxtAddress() != null &&
                            !submittedByUser.getTxtAddress().trim().isEmpty()) {

                        // Send HTML confirmation email to submitter
                        String submitterSubject = formName + " Submitted Successfully - " +
                                (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtmlMessage = generateSubmissionEmailHtml(
                                submittedByUser.getTxtUserName() != null ? submittedByUser.getTxtUserName() : "User",
                                application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                formName,
                                application.getTxtStatus(),
                                application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString()
                                        : "N/A");
                        if (!quotationAttachmentHtml.isEmpty()) {
                            submitterHtmlMessage += quotationAttachmentHtml;
                        }

                        sendEmailWithInlineFormPreview(
                                java.util.Arrays.asList(submittedByUser.getTxtAddress()),
                                submitterSubject,
                                submitterHtmlMessage,
                                application,
                                form,
                                isCapf,
                                isCapf ? "capf-inline" : "form-inline");
                        log.info("Submission confirmation email sent to submitter: " + submittedByUser.getTxtAddress());
                    }
                    emailEntityManager.getTransaction().commit();
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.error("Error getting submitter email: " + e.getMessage(), e);
                }
            }

            // 2. Send email to the first level (Initial Signer OR Department Head)
            if (!useIndividualPipelineFlow) {
                Integer currentLevel = application.getIntCurrentApprovalLevel();
                if (currentLevel != null && currentLevel == -1 && isCapf) {
                    Integer initialSignerId = extractInitialSignerId(application);
                    if (initialSignerId != null) {
                        try {
                            emailEntityManager.getTransaction().begin();
                            CfgTblUser initialSigner = emailEntityManager.find(CfgTblUser.class, initialSignerId);
                            if (initialSigner != null && initialSigner.getTxtAddress() != null
                                    && !initialSigner.getTxtAddress().trim().isEmpty()) {
                                String signerSubject = formName + " Initial Signature Required - " +
                                        (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                                String baseUrl = getBaseUrl();
                                String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                                        + application.getSerApplicationId() +
                                        "&userId=" + initialSigner.getSerUserId();
                                String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                                        + application.getSerApplicationId() +
                                        "&userId=" + initialSigner.getSerUserId();
                                String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId="
                                        + application.getSerApplicationId() +
                                        "&userId=" + initialSigner.getSerUserId();

                                String signerHtml = generateApprovalEmailHtml(
                                        initialSigner.getTxtUserName() != null ? initialSigner.getTxtUserName()
                                                : "User",
                                        0, // Level display
                                        application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                        formName,
                                        "INITIAL SIGNATURE REQUIRED",
                                        "", // Remarks
                                        true, // Show buttons
                                        approveUrl, rejectUrl, sendBackUrl,
                                        null, // No send back to initiator for first level
                                        application.getTxtApprovalHistory(),
                                        getBaseUrl());
                                if (!quotationAttachmentHtml.isEmpty()) {
                                    signerHtml += quotationAttachmentHtml;
                                }

                                String cid = "capf-inline";
                                byte[] imageBytes = buildCapfPreviewPng(application, form);
                                if (imageBytes != null && imageBytes.length > 0) {
                                    signerHtml = appendCapfInlineImage(signerHtml, cid);
                                    emailService.sendHtmlEmailWithInlineImage(
                                            java.util.Arrays.asList(initialSigner.getTxtAddress()),
                                            signerSubject, signerHtml, imageBytes, "image/png", cid);
                                } else {
                                    emailService.sendHtmlEmail(java.util.Arrays.asList(initialSigner.getTxtAddress()),
                                            signerSubject, signerHtml);
                                }
                                log.info("Initial Signer email sent to: " + initialSigner.getTxtAddress());
                            }
                            emailEntityManager.getTransaction().commit();
                        } catch (Exception e) {
                            if (emailEntityManager.getTransaction().isActive())
                                emailEntityManager.getTransaction().rollback();
                            log.error("Error sending Initial Signer email: " + e.getMessage(), e);
                        }
                    }
                } else if (pipelines != null && !pipelines.isEmpty()) {
                    try {
                        emailEntityManager.getTransaction().begin();
                        // Get the first level pipeline (index 0)
                        java.util.Map<String, Object> firstLevelPipeline = pipelines.get(0);
                        if (firstLevelPipeline != null) {
                            Integer firstDeptId = safeInt(firstLevelPipeline.get("serDepartmentId"),
                                    safeInt(firstLevelPipeline.get("departmentId"), null));
                            String firstDeptName = resolveDepartmentName(emailEntityManager, firstDeptId,
                                    firstLevelPipeline);

                            // For CAPF forms, the first stage ALWAYS routes to the initiator's (submitter's) HOD.
                            if (isCapf) {
                                Integer submitterDeptId = loadUserDepartmentId(emailEntityManager,
                                        application.getSerSubmittedBy());
                                if (submitterDeptId != null) {
                                    firstDeptId = submitterDeptId;
                                    firstDeptName = resolveDepartmentName(emailEntityManager, firstDeptId, null);
                                    log.info("CAPF detected. Resolved first stage to initiator department ID: " + firstDeptId);
                                }
                            } else if (isUserDepartmentHodStage(firstLevelPipeline, firstDeptName)) {
                                Integer submitterDeptId = loadUserDepartmentId(emailEntityManager,
                                        application.getSerSubmittedBy());
                                if (submitterDeptId != null) {
                                    firstDeptId = submitterDeptId;
                                    firstDeptName = resolveDepartmentName(emailEntityManager, firstDeptId, null);
                                    log.info("Resolved first dynamic stage to submitter department ID: " + firstDeptId);
                                } else {
                                    log.warn("Submitter department not found for application "
                                            + application.getSerApplicationId() +
                                            ". Unable to resolve User Dept (HoD) stage.");
                                }
                            }

                            if (firstDeptId != null) {
                                // Get the department with department head
                                com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment firstDept = emailEntityManager
                                        .find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class,
                                                firstDeptId);

                                if (firstDept != null) {
                                    // Collect all Head IDs
                                    java.util.List<Integer> headIds = new java.util.ArrayList<>();
                                    String headIdsStr = firstDept.getSerDepartmentHeadId();
                                    if (headIdsStr != null && !headIdsStr.trim().isEmpty()) {
                                        for (String id : headIdsStr.split(",")) {
                                            try {
                                                headIds.add(Integer.parseInt(id.trim()));
                                            } catch (Exception e) {
                                            }
                                        }
                                    }

                                    // Fallback if no head IDs found
                                    if (headIds.isEmpty()) {
                                        Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager,
                                                firstDeptId);
                                        if (fallbackHeadId != null)
                                            headIds.add(fallbackHeadId);
                                    }

                                    for (Integer currentHeadId : headIds) {
                                        Integer headId = currentHeadId;
                                        CfgTblUser firstDeptHead = headId != null
                                                ? emailEntityManager.find(CfgTblUser.class, headId)
                                                : null;

                                        if (firstDeptHead != null && firstDeptHead.getTxtAddress() != null &&
                                                !firstDeptHead.getTxtAddress().trim().isEmpty()) {

                                            if (shouldSkipCeoForCapf(form, firstDeptName, firstDeptHead)) {
                                                log.info("Skipping CAPF CEO submission email for user: "
                                                        + firstDeptHead.getSerUserId());
                                            } else {
                                                // Get the first level's pipeline order
                                                Object orderObj = firstLevelPipeline.get("intApprovalOrder");
                                                Integer firstLevelOrder = orderObj != null
                                                        ? (orderObj instanceof Integer ? (Integer) orderObj
                                                                : Integer.parseInt(orderObj.toString()))
                                                        : 1;

                                                // Send HTML email to first level department head with approve/reject
                                                // buttons
                                                String deptHeadSubject = formName + " Pending Approval - Level "
                                                        + firstLevelOrder +
                                                        " - "
                                                        + (application.getTxtFormCode() != null
                                                                ? application.getTxtFormCode()
                                                                : "N/A");
                                                String baseUrl = getBaseUrl();
                                                String approveUrl = baseUrl
                                                        + "/approveApplicationFromEmail?applicationId="
                                                        + application.getSerApplicationId() +
                                                        "&userId=" + firstDeptHead.getSerUserId();
                                                String rejectUrl = baseUrl
                                                        + "/rejectApplicationFromEmail?applicationId="
                                                        + application.getSerApplicationId() +
                                                        "&userId=" + firstDeptHead.getSerUserId();

                                                String sendBackUrl = baseUrl
                                                        + "/sendBackApplicationFromEmail?applicationId="
                                                        + application.getSerApplicationId() +
                                                        "&userId=" + firstDeptHead.getSerUserId();
                                                        String sendBackToInitiatorUrl = baseUrl
                                                                + "/sendBackToInitiatorFromEmail?applicationId="
                                                                + application.getSerApplicationId() +
                                                                "&userId=" + firstDeptHead.getSerUserId();
                                                        String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                                                firstDeptHead.getTxtUserName() != null
                                                                        ? firstDeptHead.getTxtUserName()
                                                                        : "Department Head",
                                                                firstLevelOrder,
                                                                application.getTxtFormCode() != null
                                                                        ? application.getTxtFormCode()
                                                                        : "N/A",
                                                                formName,
                                                                application.getTxtStatus(),
                                                                null,
                                                                true, // For department head
                                                                approveUrl,
                                                                rejectUrl,
                                                                sendBackUrl,
                                                                sendBackToInitiatorUrl,
                                                                application.getTxtApprovalHistory(),
                                                                getBaseUrl());

                                                boolean isCapfMail = isCapfForm(form);
                                                sendEmailWithInlineFormPreview(
                                                        java.util.Arrays.asList(firstDeptHead.getTxtAddress()),
                                                        deptHeadSubject,
                                                        deptHeadHtmlMessage,
                                                        application,
                                                        form,
                                                        isCapfMail,
                                                        isCapfMail ? "capf-inline" : "form-inline");
                                                log.info(
                                                        "Submission notification email sent to first level department head: "
                                                                + firstDeptHead.getTxtAddress());
                                            }
                                        }
                                    } // End loop
                                }
                            }
                        }
                        emailEntityManager.getTransaction().commit();
                    } catch (Exception e) {
                        if (emailEntityManager.getTransaction().isActive()) {
                            emailEntityManager.getTransaction().rollback();
                        }
                        log.error("Error getting first level department head email: " + e.getMessage(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error in sendSubmissionEmails: " + e.getMessage(), e);
            // Don't throw - email failure shouldn't break submission
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    private boolean isBudgetApprovalForm(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form) {
        if (form == null)
            return false;
        String name = form.getTxtFormName() != null ? form.getTxtFormName().toUpperCase() : "";
        String code = form.getTxtFormCode() != null ? form.getTxtFormCode().toUpperCase() : "";
        return name.contains("BUDGET APPROVAL") || code.startsWith("BDG");
    }

    private boolean shouldSkipCeoForCapf(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            String departmentName,
            CfgTblUser user) {
        if (!isCapfForm(form))
            return false;
        return isCeoDepartmentName(departmentName) || isCeoUser(user);
    }

    private boolean isCeoDepartmentName(String departmentName) {
        if (departmentName == null)
            return false;
        String name = departmentName.trim().toUpperCase();
        return "CEO".equals(name) ||
                name.contains("CHIEF EXECUTIVE") ||
                name.contains("CHIEF EXECUTIVE OFFICER") ||
                name.contains("CEO OFFICE");
    }

    private boolean isUserDepartmentHodStage(Map<String, Object> pipelineMap, String resolvedDepartmentName) {
        StringBuilder sb = new StringBuilder();
        if (resolvedDepartmentName != null) {
            sb.append(resolvedDepartmentName).append(" ");
        }
        if (pipelineMap != null) {
            Object departmentName = pipelineMap.get("departmentName");
            if (departmentName == null)
                departmentName = pipelineMap.get("txtDepartmentName");
            if (departmentName == null)
                departmentName = pipelineMap.get("role");
            if (departmentName == null)
                departmentName = pipelineMap.get("stageName");
            if (departmentName != null)
                sb.append(String.valueOf(departmentName)).append(" ");
            Object hrTblDepartment = pipelineMap.get("hrTblDepartment");
            if (hrTblDepartment instanceof Map) {
                Object nestedName = ((Map<?, ?>) hrTblDepartment).get("txtDepartmentName");
                if (nestedName == null)
                    nestedName = ((Map<?, ?>) hrTblDepartment).get("departmentName");
                if (nestedName != null)
                    sb.append(String.valueOf(nestedName)).append(" ");
            }
        }

        String text = sb.toString().trim().toUpperCase();
        if (text.isEmpty())
            return false;
        return text.contains("USER DEPT") || text.contains("USER DEPTT") || text.contains("HOD");
    }

    private boolean isCeoUser(CfgTblUser user) {
        if (user == null)
            return false;
        String roleName = null;
        if (user.getCfgTblRole() != null && user.getCfgTblRole().getTxtRoleName() != null) {
            roleName = user.getCfgTblRole().getTxtRoleName();
        }
        if (roleName != null && roleName.toUpperCase().contains("CEO")) {
            return true;
        }
        String userName = user.getTxtUserName();
        if (userName != null && userName.toUpperCase().contains("CEO")) {
            return true;
        }
        String email = user.getTxtAddress();
        return email != null && email.toUpperCase().contains("CEO");
    }

    private Map<String, Object> parseApplicationData(CfgTblCustomFormApplication application) {
        try {
            String raw = application.getTxtApplicationData();
            if (raw == null || raw.trim().isEmpty())
                return new java.util.HashMap<>();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing application data: " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    private boolean hasDynamicFooterFlow(CfgTblCustomFormApplication application) {
        try {
            Map<String, Object> appData = parseApplicationData(application);
            return !extractFooterFields(appData).isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    private BudgetApprover getPreparedBy(Map<String, Object> appData, EntityManager em) {
        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        if (footerFields != null && !footerFields.isEmpty()) {
            for (Map<String, Object> field : footerFields) {
                String key = field != null && field.get("key") != null ? String.valueOf(field.get("key")).toLowerCase()
                        : "";
                if (!"prepared_by".equals(key))
                    continue;
                List<Object> users = extractFooterUsers(field);
                if (users != null && !users.isEmpty()) {
                    BudgetApprover prepared = buildBudgetApprover(users.get(0), "PREPARED", em);
                    if (prepared != null && prepared.userId != null) {
                        return prepared;
                    }
                }
            }
        }
        Object obj = appData.get("preparedBy");
        return buildBudgetApprover(obj, "PREPARED", em);
    }

    private List<BudgetApprover> getBudgetApprovalSequence(Map<String, Object> appData, EntityManager em) {
        List<BudgetApprover> seq = new java.util.ArrayList<>();
        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        if (footerFields != null && !footerFields.isEmpty()) {
            for (Map<String, Object> field : footerFields) {
                String key = field != null && field.get("key") != null ? String.valueOf(field.get("key")).toLowerCase()
                        : "";
                String label = field != null && field.get("label") != null ? String.valueOf(field.get("label"))
                        : "APPROVER";
                if ("prepared_by".equals(key)) {
                    continue;
                }
                List<Object> users = extractFooterUsers(field);
                if (users == null || users.isEmpty())
                    continue;
                for (Object userObj : users) {
                    BudgetApprover b = buildBudgetApprover(userObj, label, em);
                    if (b != null && b.userId != null) {
                        seq.add(b);
                    }
                }
            }
            if (!seq.isEmpty()) {
                return seq;
            }
        }

        Object reviewersObj = appData.get("reviewers");
        if (reviewersObj instanceof List) {
            for (Object r : (List<?>) reviewersObj) {
                BudgetApprover b = buildBudgetApprover(r, "REVIEWER", em);
                if (b != null && b.userId != null)
                    seq.add(b);
            }
        }
        Object recommendersObj = appData.get("recommenders");
        if (recommendersObj instanceof List) {
            for (Object r : (List<?>) recommendersObj) {
                BudgetApprover b = buildBudgetApprover(r, "RECOMMENDER", em);
                if (b != null && b.userId != null)
                    seq.add(b);
            }
        }
        Object approverObj = appData.get("approver");
        BudgetApprover approver = buildBudgetApprover(approverObj, "APPROVER", em);
        if (approver != null && approver.userId != null)
            seq.add(approver);
        return seq;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFooterFields(Map<String, Object> appData) {
        if (appData == null)
            return java.util.Collections.emptyList();
        Object obj = appData.get("footerFields");
        if (!(obj instanceof List))
            return java.util.Collections.emptyList();

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object item : (List<?>) obj) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        result.sort((a, b) -> {
            Integer oa = parseInteger(a != null ? a.get("order") : null);
            Integer ob = parseInteger(b != null ? b.get("order") : null);
            if (oa == null && ob == null)
                return 0;
            if (oa == null)
                return 1;
            if (ob == null)
                return -1;
            return Integer.compare(oa, ob);
        });
        return result;
    }

    private List<Object> extractFooterUsers(Map<String, Object> field) {
        if (field == null)
            return java.util.Collections.emptyList();
        Object usersObj = field.get("users");
        if (!(usersObj instanceof List))
            return java.util.Collections.emptyList();
        List<Object> users = new java.util.ArrayList<>();
        for (Object o : (List<?>) usersObj) {
            users.add(o);
        }
        return users;
    }

    private Integer parseInteger(Object value) {
        if (value == null)
            return null;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private BudgetApprover buildBudgetApprover(Object userObj, String role, EntityManager em) {
        if (userObj == null)
            return null;
        Integer userId = null;
        String name = null;
        String email = null;
        String signaturePath = null;
        String department = null;
        String designation = null;

        if (userObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) userObj;
            Object idObj = map.get("serUserId");
            if (idObj == null)
                idObj = map.get("userId");
            if (idObj != null)
                userId = Integer.parseInt(idObj.toString());
            Object nameObj = map.get("txtUserName");
            if (nameObj == null)
                nameObj = map.get("userName");
            if (nameObj != null)
                name = nameObj.toString();
            Object emailObj = map.get("txtAddress");
            if (emailObj == null)
                emailObj = map.get("email");
            if (emailObj != null)
                email = emailObj.toString();
            Object departmentObj = map.get("txtDepartmentName");
            if (departmentObj == null)
                departmentObj = map.get("departmentName");
            if (departmentObj != null)
                department = departmentObj.toString();
            Object designationObj = map.get("txtDesignation");
            if (designationObj == null)
                designationObj = map.get("designation");
            if (designationObj != null)
                designation = designationObj.toString();
        }

        if (userId != null) {
            CfgTblUser user = em.find(CfgTblUser.class, userId);
            if (user != null) {
                if (name == null)
                    name = user.getTxtUserName();
                if (email == null)
                    email = user.getTxtAddress();
                signaturePath = user.getTxtSignaturePath();
                if (department == null)
                    department = user.getTxtDepartmentName();
                if (designation == null)
                    designation = user.getTxtDesignation();
            }
        }

        BudgetApprover b = new BudgetApprover();
        b.userId = userId;
        b.name = name;
        b.email = email;
        b.role = role;
        b.signaturePath = signaturePath;
        b.department = department;
        b.designation = designation;
        return b;
    }

    private void sendBudgetApprovalNextEmail(CfgTblCustomFormApplication application, Integer sequenceIndex) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            Map<String, Object> appData = parseApplicationData(application);
            CfgTblCustomForm form = application.getCfgTblCustomForm();
            if (form == null && application.getSerFormId() != null) {
                form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            }
            List<BudgetApprover> seq = getBudgetApprovalSequence(appData, emailEntityManager);
            if (sequenceIndex == null || sequenceIndex < 0 || sequenceIndex >= seq.size()) {
                emailEntityManager.getTransaction().commit();
                return;
            }

            BudgetApprover next = seq.get(sequenceIndex);
            if (next.email == null || next.email.trim().isEmpty()) {
                emailEntityManager.getTransaction().commit();
                return;
            }

            // Send email to all approvers in sequence - everyone should receive emails sequentially
            // No pre-approval - each person must manually approve

            String formName = getResolvedFormName(form);
            String subject = formName + " Pending Approval - Level " + (sequenceIndex + 1) + " - " +
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");

            String baseUrl = getBaseUrl();
            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                    + application.getSerApplicationId() +
                    "&userId=" + next.userId;
            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                    + application.getSerApplicationId() +
                    "&userId=" + next.userId;

            String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId="
                    + application.getSerApplicationId() +
                    "&userId=" + next.userId;
            String sendBackToInitiatorUrl = baseUrl
                    + "/sendBackToInitiatorFromEmail?applicationId="
                    + application.getSerApplicationId() +
                    "&userId=" + next.userId;

            String html = generateApprovalEmailHtml(
                    next.name != null ? next.name : "User",
                    sequenceIndex + 1,
                    application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                    formName,
                    application.getTxtStatus(),
                    null,
                    true,
                    approveUrl,
                    rejectUrl,
                    sendBackUrl,
                    sendBackToInitiatorUrl,
                    application.getTxtApprovalHistory(),
                    getBaseUrl());

            sendEmailWithInlineFormPreview(
                    java.util.Arrays.asList(next.email),
                    subject,
                    html,
                    application,
                    form,
                    false,
                    "budget-inline");
            emailEntityManager.getTransaction().commit();
        } catch (Exception e) {
            if (emailEntityManager.getTransaction().isActive()) {
                emailEntityManager.getTransaction().rollback();
            }
            log.error("Error sending budget approval email: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }

    private byte[] generateApplicationPdf(CfgTblCustomFormApplication application,
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            Map<String, Object> appData) {
        try {
            if (isBudgetApprovalForm(form)) {
                return generateBudgetApprovalPdf(application, form, appData);
            }
            if (isCapfForm(form)) {
                return generateCapfPdf(application, form, appData);
            }
        } catch (Exception e) {
            log.warn("Error generating budget approval PDF, falling back to summary: " + e.getMessage());
        }
        return generateSummaryPdf(application, form, appData);
    }

    private byte[] generateSummaryPdf(CfgTblCustomFormApplication application,
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            Map<String, Object> appData) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.beginText();
            content.newLineAtOffset(40, 800);
            content.showText("Application Summary");
            content.endText();

            content.setFont(PDType1Font.HELVETICA, 10);
            float y = 780;
            y = writeLine(content, y,
                    "Form: " + (form != null && form.getTxtFormName() != null ? form.getTxtFormName() : "N/A"));
            y = writeLine(content, y,
                    "Code: " + (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A"));
            y = writeLine(content, y,
                    "Status: " + (application.getTxtStatus() != null ? application.getTxtStatus() : "N/A"));
            y = writeLine(content, y, "Created: "
                    + (application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : "N/A"));

            y -= 10;
            y = writeLine(content, y, "Fields:");
            for (Map.Entry<String, Object> entry : appData.entrySet()) {
                String key = entry.getKey();
                Object valObj = entry.getValue();
                if (key != null) {
                    String keyLower = key.toLowerCase();
                    if (keyLower.contains("dataurl") || keyLower.contains("base64")) {
                        continue;
                    }
                }
                String val = formatPdfValue(valObj);
                y = writeLine(content, y, "  " + key + ": " + val);
                if (y < 60) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.setFont(PDType1Font.HELVETICA, 10);
                    y = 800;
                }
            }

            content.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    private byte[] generateBudgetApprovalPdf(CfgTblCustomFormApplication application,
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            Map<String, Object> appData) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 40f;
            float y = pageHeight - margin;

            String heading = pickFirstNonEmpty(
                    getValueByKeyContains(appData, "header"),
                    getValueByKeyContains(appData, "heading"),
                    getValueByKeyContains(appData, "title"),
                    getValueByKeyContains(appData, "subject"),
                    form != null ? form.getTxtFormName() : null,
                    "Budget Approval Form");

            String dateStr = application != null && application.getDteCreatedDate() != null
                    ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(application.getDteCreatedDate())
                    : new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());

            // Header: logo + company + date
            content.setFont(PDType1Font.TIMES_BOLD, 12);
            drawLogo(document, content, margin, y - 35, 45);
            content.beginText();
            content.newLineAtOffset(pageWidth - margin - 120, y - 10);
            content.showText("Date: " + dateStr);
            content.endText();

            content.setFont(PDType1Font.TIMES_BOLD, 18);
            float titleWidth = PDType1Font.TIMES_BOLD.getStringWidth("Qarshi Industries (Pvt) Ltd.") / 1000 * 18;
            content.beginText();
            content.newLineAtOffset((pageWidth - titleWidth) / 2, y - 30);
            content.showText("Qarshi Industries (Pvt) Ltd.");
            content.endText();

            content.setFont(PDType1Font.TIMES_ROMAN, 10);
            String address = "15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore";
            float addrWidth = PDType1Font.TIMES_ROMAN.getStringWidth(address) / 1000 * 10;
            content.beginText();
            content.newLineAtOffset((pageWidth - addrWidth) / 2, y - 45);
            content.showText(address);
            content.endText();

            // Double line
            content.setLineWidth(0.8f);
            content.moveTo(margin, y - 60);
            content.lineTo(pageWidth - margin, y - 60);
            content.stroke();
            content.moveTo(margin, y - 62);
            content.lineTo(pageWidth - margin, y - 62);
            content.stroke();

            // Heading
            content.setFont(PDType1Font.TIMES_BOLD, 16);
            float headingWidth = PDType1Font.TIMES_BOLD.getStringWidth(heading) / 1000 * 16;
            content.beginText();
            content.newLineAtOffset((pageWidth - headingWidth) / 2, y - 95);
            content.showText(heading);
            content.endText();

            y = y - 120;

            // Content body
            content.setFont(PDType1Font.TIMES_ROMAN, 12);
            String body = buildBudgetBody(appData);
            y = drawWrappedText(content, body, margin, y, pageWidth - margin * 2, 14, 170);

            // Signature table at bottom
            float tableBottomY = 60f;
            float tableHeight = 110f;
            float tableTopY = tableBottomY + tableHeight;
            if (y < tableTopY + 20) {
                content.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                content = new PDPageContentStream(document, page);
                pageWidth = page.getMediaBox().getWidth();
                pageHeight = page.getMediaBox().getHeight();
                y = pageHeight - margin;
            }

            drawSignatureTable(content, margin, tableBottomY, pageWidth - margin * 2, tableHeight, appData,
                    application.getTxtApprovalHistory(), document);

            content.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error generating Budget Approval PDF: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateCapfPdf(CfgTblCustomFormApplication application,
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
            Map<String, Object> appData) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 26f;
            float y = pageHeight - margin;
            float lineStart = margin + 12;
            float lineEnd = pageWidth - margin - 12;
            float contentWidth = lineEnd - lineStart;

            String dateStr = application != null && application.getDteCreatedDate() != null
                    ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(application.getDteCreatedDate())
                    : new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());

            String division = pickFirstNonEmpty(getCapfValue(appData, "division"), getCapfValue(appData, "department"));
            String department = pickFirstNonEmpty(getCapfValue(appData, "department"), division);
            String section = pickFirstNonEmpty(getCapfValue(appData, "section"), "GEN");
            String documentNo = pickFirstNonEmpty(getCapfValue(appData, "capfNumber"), getCapfValue(appData, "capf #"),
                    getCapfValue(appData, "document no"), "CAPF");
            String originalIssue = pickFirstNonEmpty(getCapfValue(appData, "original issue"), "01-01-2020");
            String rev = pickFirstNonEmpty(getCapfValue(appData, "rev"), "05");
            String revDate = pickFirstNonEmpty(getCapfValue(appData, "rev date"), dateStr);

            String assetName = getCapfValue(appData, "assetName");
            String specification = getCapfValue(appData, "specification");
            String utility = getCapfValue(appData, "utility");
            String feasibility = getCapfValue(appData, "feasibilityReport");
            String reason = getCapfValue(appData, "reason");
            String note = getCapfValue(appData, "note");

            String vendorName = getCapfValue(appData, "vendorName");
            String vendorAddress = getCapfValue(appData, "vendorAddress");
            String approvedPrice = getCapfValue(appData, "approvedPrice");
            String delivery = getCapfValue(appData, "deliveryPeriod");
            String terms = getCapfValue(appData, "termsConditions");
            String thirdParty = getCapfValue(appData, "thirdPartyAssessment");

            // Outer border
            content.setLineWidth(0.7f);
            content.addRect(margin, margin, pageWidth - margin * 2, pageHeight - margin * 2);
            content.stroke();

            // Header row
            drawLogo(document, content, margin + 6, y - 28, 22);
            content.setFont(PDType1Font.TIMES_BOLD, 12);
            content.beginText();
            content.newLineAtOffset(margin + 40, y - 16);
            content.showText("Qarshi Industries (Pvt) Ltd.");
            content.endText();
            if (application != null && application.getTxtAssetCode() != null
                    && !application.getTxtAssetCode().trim().isEmpty()) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                String codeLabel = "Asset Code: " + application.getTxtAssetCode().trim();
                float codeWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(codeLabel) / 1000 * 9;
                content.beginText();
                content.newLineAtOffset(lineEnd - codeWidth, y - 12);
                content.showText(codeLabel);
                content.endText();
            }

            y -= 36;

            // Meta table (Division/Department/Section/Document No/Original Issue/Rev/Rev
            // Date)
            float metaHeight = 32f;
            drawRect(content, lineStart, y - metaHeight, contentWidth, metaHeight);

            // Vertical separators
            float col1Width = contentWidth * 0.33f;
            float col2Width = contentWidth * 0.33f;
            float metaCol2 = lineStart + col1Width;
            float metaCol3 = lineStart + col1Width + col2Width;
            float metaCol4 = lineStart + contentWidth * 0.82f;

            // Row horizontal line
            drawLine(content, lineStart, y - 16, lineEnd, y - 16);

            // Vertical lines for the grid
            drawLine(content, metaCol2, y, metaCol2, y - metaHeight);
            drawLine(content, metaCol3, y, metaCol3, y - metaHeight);

            float metaY = y - 12;
            content.setFont(PDType1Font.HELVETICA, 8);

            drawMeta(content, lineStart + 4, metaY, "Division: " + nullSafe(division));
            drawMeta(content, metaCol2 + 4, metaY, "Department: " + nullSafe(department));
            drawMeta(content, metaCol3 + 4, metaY, "Section: " + nullSafe(section));

            drawMeta(content, lineStart + 4, metaY - 16, "Document No: " + nullSafe(documentNo));
            drawMeta(content, metaCol2 + 4, metaY - 16, "Original Issue: " + nullSafe(originalIssue));
            drawMeta(content, metaCol3 + 4, metaY - 16, "Rev: " + nullSafe(rev));
            drawMeta(content, metaCol4 + 2, metaY - 16, "Rev. Date: " + nullSafe(revDate));

            y -= (metaHeight + 14);

            // Title bar
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            drawCentered(content, pageWidth, y, "CAPITAL ASSETS PURCHASE FORM");
            y -= 16;
            drawCentered(content, pageWidth, y, "PART I (TO BE FILLED BY CONCERNED DEPARTMENT)");
            y -= 10;
            drawLine(content, lineStart, y, lineEnd, y);
            y -= 12;

            float labelWidth = 140;
            content.setFont(PDType1Font.HELVETICA, 9);

            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DIVISION / DEPARTMENT:",
                    nullSafe(division));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "CAPF #:", nullSafe(documentNo), 180);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DATE:", nullSafe(dateStr), 180);
            y -= 4;
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "NAME OF ASSET / ITEM:",
                    nullSafe(assetName));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DETAIL SPECIFICATION:",
                    nullSafe(specification));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "UTILITY & PURPOSE:", nullSafe(utility));

            y -= 2;
            y = drawYesNoRow(content, lineStart, y, lineEnd, "FEASIBILITY REPORT ATTACHED:", feasibility);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "IF NO THEN MENTION REASON:",
                    nullSafe(reason));

            y -= 2;
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "NOTE:", nullSafe(note));

            y -= 6;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 10;
            content.setFont(PDType1Font.HELVETICA_BOLD, 9);
            drawCentered(content, pageWidth, y, "PARTICULARS OF SELECTED VENDOR(S) (AS PER APPROVED QUOTATION)");
            y -= 12;
            content.setFont(PDType1Font.HELVETICA, 9);

            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "NAME:", nullSafe(vendorName));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "ADDRESS:", nullSafe(vendorAddress));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "APPROVED PRICE:", nullSafe(approvedPrice));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DELIVERY PERIOD & DATE:",
                    nullSafe(delivery));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "TERMS & CONDITIONS:", nullSafe(terms));
            y -= 2;
            y = drawYesNoNaRow(content, lineStart, y, lineEnd, "Third Party assessment carried out:", thirdParty);

            y -= 6;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 8;
            y = drawCapfSignatureSection(content, lineStart, y, lineEnd - lineStart,
                    application.getTxtApprovalHistory(), document);
            y -= 8;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 8;
            content.setFont(PDType1Font.HELVETICA_BOLD, 9);
            drawCentered(content, pageWidth, y, "PART II (TO BE FILLED BY PROCUREMENT DEPARTMENT)");
            y -= 10;
            content.setFont(PDType1Font.HELVETICA, 8.5f);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "P.O. NO. WITH DATE:", "");
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "PARTICULARS OF VENDOR(S):", "");
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DELIVERY DATE:", "");
            y -= 8;
            drawSignatureLineRow(content, lineStart, y, lineEnd, "Checked by:", "Verified by:");
            y -= 14;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 8;
            content.setFont(PDType1Font.HELVETICA_BOLD, 9);
            drawCentered(content, pageWidth, y, "JOB COMPLETION CERTIFICATE");
            y -= 10;
            content.setFont(PDType1Font.HELVETICA, 8.5f);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "This is to certify that job against CAPF:",
                    "");
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "GRN #:", "");
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "Date:", "");
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "Report Attached:", "");
            drawSignatureLineRow(content, lineStart, y - 2, lineEnd, "Checked by:", "Verified by:");

            content.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error generating CAPF PDF: " + e.getMessage(), e);
            return null;
        }
    }

    private void drawLogo(PDDocument document, PDPageContentStream content, float x, float y, float size) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/assets/images/qarshi-logo.png")) {
            if (is == null)
                return;
            byte[] data = readAllBytes(is);
            if (data == null || data.length == 0)
                return;
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
                    .createFromByteArray(document, data, "qarshi-logo");
            float width = size;
            float height = size * (image.getHeight() / (float) image.getWidth());
            content.drawImage(image, x, y, width, height);
        } catch (Exception e) {
            log.warn("Unable to load logo: " + e.getMessage());
        }
    }

    private byte[] readAllBytes(InputStream is) throws java.io.IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }

    private String buildBudgetBody(Map<String, Object> appData) {
        String rawHtml = pickFirstNonEmpty(
                getValueByKey(appData, "word_editor"),
                getValueByKey(appData, "content"),
                getValueByKey(appData, "editorContent"),
                getValueByKey(appData, "html"));
        if (rawHtml != null && !rawHtml.trim().isEmpty()) {
            return htmlToPlainText(rawHtml);
        }

        StringBuilder sb = new StringBuilder();
        String background = getValueByKeyContains(appData, "background");
        String proposal = getValueByKeyContains(appData, "proposal");
        String request = getValueByKeyContains(appData, "request");
        String finances = getValueByKeyContains(appData, "finance");
        String note = getValueByKeyContains(appData, "note");

        appendSection(sb, "Background", background);
        appendSection(sb, "Proposal", proposal);
        appendSection(sb, "Request", request);
        appendSection(sb, "Finances", finances);
        appendSection(sb, "Note", note);

        if (sb.length() == 0) {
            sb.append("No content provided.");
        }
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, String value) {
        if (value == null || value.trim().isEmpty())
            return;
        if (sb.length() > 0)
            sb.append("\n\n");
        sb.append(title).append(":\n").append(value.trim());
    }

    private String htmlToPlainText(String html) {
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?s)<[^>]*>", "");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        return text.trim();
    }

    private String getValueByKeyContains(Map<String, Object> appData, String needle) {
        if (appData == null || needle == null)
            return null;
        String n = needle.toLowerCase();
        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.toLowerCase().contains(n)) {
                Object val = entry.getValue();
                if (val != null)
                    return String.valueOf(val);
            }
        }
        return null;
    }

    private String getValueByKey(Map<String, Object> appData, String key) {
        if (appData == null || key == null)
            return null;
        Object val = appData.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private String pickFirstNonEmpty(String... values) {
        if (values == null)
            return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty())
                return v.trim();
        }
        return null;
    }

    private float drawWrappedText(PDPageContentStream content, String text,
            float x, float y, float maxWidth, float leading, float minY) throws java.io.IOException {
        if (text == null)
            return y;
        String[] paragraphs = text.split("\\r?\\n");
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) {
                y -= leading;
                continue;
            }
            for (String line : wrapText(para, PDType1Font.TIMES_ROMAN, 12, maxWidth)) {
                if (y < minY) {
                    return y;
                }
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(line);
                content.endText();
                y -= leading;
            }
            y -= 2;
        }
        return y;
    }

    private java.util.List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth)
            throws java.io.IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            float width = font.getStringWidth(test) / 1000 * fontSize;
            if (width > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0)
            lines.add(line.toString());
        return lines;
    }

    private void drawSignatureTable(PDPageContentStream content, float x, float y, float width, float height,
            Map<String, Object> appData, String approvalHistoryJson, PDDocument document) throws java.io.IOException {
        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        if (footerFields != null && !footerFields.isEmpty()) {
            drawDynamicBudgetSignatureTable(content, x, y, width, height, footerFields, approvalHistoryJson, document);
            return;
        }

        float colWidth = width / 6f;
        float rowSig = 50f;
        float rowHeader = 22f;
        float rowNames = height - rowSig - rowHeader;

        // Borders
        content.setLineWidth(0.6f);
        content.addRect(x, y, width, height);
        content.stroke();
        for (int i = 1; i < 6; i++) {
            content.moveTo(x + colWidth * i, y);
            content.lineTo(x + colWidth * i, y + height);
            content.stroke();
        }
        content.moveTo(x, y + rowSig);
        content.lineTo(x + width, y + rowSig);
        content.stroke();
        content.moveTo(x, y + rowSig + rowHeader);
        content.lineTo(x + width, y + rowSig + rowHeader);
        content.stroke();

        // Header background
        content.setNonStrokingColor(220, 220, 220);
        content.addRect(x, y + rowSig, width, rowHeader);
        content.fill();
        content.setNonStrokingColor(0, 0, 0);

        content.setFont(PDType1Font.HELVETICA_BOLD, 9);
        float headerY = y + rowSig + 6;
        drawCenteredHeader(content, "Prepared by:", x, colWidth, headerY);
        drawCenteredHeader(content, "Reviewed by:", x + colWidth, colWidth * 2, headerY);
        drawCenteredHeader(content, "Recommended by:", x + colWidth * 3, colWidth * 2, headerY);
        drawCenteredHeader(content, "Approved by:", x + colWidth * 5, colWidth, headerY);

        // Signatures row (top row)
        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        Object preparedObj = appData.get("preparedBy");
        java.util.List<java.util.Map<String, String>> reviewers = extractUserListDisplay(appData.get("reviewers"));
        java.util.List<java.util.Map<String, String>> recommenders = extractUserListDisplay(
                appData.get("recommenders"));
        Object approverObj = appData.get("approver");

        Integer preparedId = extractUserId(preparedObj);
        if (preparedId == null) {
            String preparedName = extractUserName(preparedObj);
            if (preparedName != null && !preparedName.trim().isEmpty()) {
                preparedId = resolveUserIdByName(preparedName);
            }
        }
        Integer reviewer1Id = extractUserIdByIndex(appData.get("reviewers"), 0);
        Integer reviewer2Id = extractUserIdByIndex(appData.get("reviewers"), 1);
        Integer recommender1Id = extractUserIdByIndex(appData.get("recommenders"), 0);
        Integer recommender2Id = extractUserIdByIndex(appData.get("recommenders"), 1);
        Integer approverId = extractUserId(approverObj);

        Object reviewer1Obj = extractUserByIndex(appData.get("reviewers"), 0);
        Object reviewer2Obj = extractUserByIndex(appData.get("reviewers"), 1);
        Object recommender1Obj = extractUserByIndex(appData.get("recommenders"), 0);
        Object recommender2Obj = extractUserByIndex(appData.get("recommenders"), 1);

        Integer[] userIds = new Integer[] { preparedId, reviewer1Id, reviewer2Id, recommender1Id, recommender2Id,
                approverId };
        String[] roles = new String[] { "PREPARED", "REVIEWER", "REVIEWER", "RECOMMENDER", "RECOMMENDER", "APPROVER" };
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(userIds);
        float sigRowY = y + rowSig + rowHeader + 4;
        float sigRowHeight = rowNames - 8;
        for (int i = 0; i < userIds.length; i++) {
            Integer uid = userIds[i];
            boolean allowFallback = "PREPARED".equalsIgnoreCase(roles[i]);
            String sigPath = findSignatureForUser(approvalHistory, uid, roles[i], allowFallback, signatureFromDb);
            if (sigPath == null || sigPath.trim().isEmpty())
                continue;
            drawSignatureImage(document, content, sigPath, x + colWidth * i + 4, sigRowY, colWidth - 8, sigRowHeight);
        }

        // Names row
        java.util.Map<String, String> prepared = extractUserDisplay(preparedObj);
        java.util.Map<String, String> approver = extractUserDisplay(approverObj);

        String[] names = new String[] {
                formatUserDisplay(prepared),
                formatUserDisplay(reviewers.size() > 0 ? reviewers.get(0) : null),
                formatUserDisplay(reviewers.size() > 1 ? reviewers.get(1) : null),
                formatUserDisplay(recommenders.size() > 0 ? recommenders.get(0) : null),
                formatUserDisplay(recommenders.size() > 1 ? recommenders.get(1) : null),
                formatUserDisplay(approver)
        };

        content.setFont(PDType1Font.HELVETICA, 9);
        for (int i = 0; i < names.length; i++) {
            float tx = x + colWidth * i + 4;
            float ty = y + 8;
            for (String line : wrapText(names[i], PDType1Font.HELVETICA, 9, colWidth - 8)) {
                content.beginText();
                content.newLineAtOffset(tx, ty);
                content.showText(line);
                content.endText();
                ty += 10;
            }
        }
    }

    private float drawCapfSignatureSection(PDPageContentStream content, float x, float y, float width,
            String approvalHistoryJson, PDDocument document) throws java.io.IOException {
        float colWidth = width / 6f;
        float sigHeight = 18f;
        float dateHeight = 10f;
        float labelHeight = 12f;
        float blockHeight = sigHeight + dateHeight + labelHeight + 18;

        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        java.util.List<Map<String, Object>> approved = new java.util.ArrayList<>();
        for (Map<String, Object> entry : approvalHistory) {
            if (isApprovedEntry(entry)) {
                approved.add(entry);
            }
        }
        log.info("CAPF signature log [draw-section]: historyCount={}, approvedCount={}",
                approvalHistory != null ? approvalHistory.size() : 0, approved.size());

        String[] roleLabels = new String[] {
                "User Dept. (HOD)",
                "Technical Expert",
                "Procurement",
                "Finance",
                "Core Team HRT / CCT HO",
                "Chief Executive"
        };

        float sigRowY = y - sigHeight;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>>[] mapped = mapCapfApprovalEntries(approved);
        Integer[] approvedUserIds = new Integer[approved.size()];
        for (int i = 0; i < approved.size(); i++) {
            approvedUserIds[i] = extractApprovalUserId(approved.get(i));
        }
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(approvedUserIds);
        for (int i = 0; i < 6; i++) {
            List<Map<String, Object>> entries = mapped[i];
            if (entries.isEmpty()) continue;
            
            float slotX = x + (colWidth * i) + 4;
            float sigW = (colWidth - 8);
            if (entries.size() > 1) sigW = sigW / 2 - 2;

            for (int k = 0; k < entries.size() && k < 2; k++) {
                Map<String, Object> entry = entries.get(k);
                String sigPath = null;
                Integer approvedBy = extractApprovalUserId(entry);
                if (approvedBy != null && signatureFromDb.containsKey(approvedBy)) {
                    sigPath = signatureFromDb.get(approvedBy);
                } else if (entry.get("signaturePath") != null) {
                    sigPath = String.valueOf(entry.get("signaturePath"));
                }

                if (sigPath != null && !sigPath.trim().isEmpty()) {
                    float currentX = slotX + (k * (sigW + 4));
                    drawSignatureImage(document, content, sigPath, currentX, sigRowY, sigW, sigHeight);
                }
            }
        }

        content.setFont(PDType1Font.HELVETICA, 6.5f);
        float baseDateY = sigRowY - 8;
        float baseNameY = sigRowY - 18;
        float baseDesignY = sigRowY - 28;

        for (int i = 0; i < 6; i++) {
            List<Map<String, Object>> entries = mapped[i];
            if (entries.isEmpty()) continue;
            
            // Only draw metadata for the first entry in slot to avoid clutter
            Map<String, Object> entry = entries.get(0);
            String date = formatApprovalDate(entry.get("approvedDate"));
            if (!date.isEmpty()) {
                content.beginText();
                content.newLineAtOffset(x + colWidth * i + 4, baseDateY);
                content.showText(date);
                content.endText();
            }
            
            String name = entry.get("approverName") != null ? entry.get("approverName").toString() : "";
            if (!name.isEmpty()) {
                float ny = baseNameY;
                for (String line : wrapText(name.toLowerCase(), PDType1Font.HELVETICA, 6.5f, colWidth - 6)) {
                    content.beginText();
                    content.newLineAtOffset(x + colWidth * i + 4, ny);
                    content.showText(line);
                    content.endText();
                    ny -= 7;
                }
            }
            
            String designation = entry.get("txtDesignation") != null ? entry.get("txtDesignation").toString() 
                               : (entry.get("designation") != null ? entry.get("designation").toString() : "");
            if (!designation.isEmpty()) {
                float dy = baseDesignY;
                for (String line : wrapText(designation, PDType1Font.HELVETICA, 6.0f, colWidth - 6)) {
                    content.beginText();
                    content.newLineAtOffset(x + colWidth * i + 4, dy);
                    content.showText(line);
                    content.endText();
                    dy -= 7;
                }
            }
        }

        content.setFont(PDType1Font.HELVETICA_BOLD, 7f);
        float fixedLabelY = sigRowY - 50; 
        for (int i = 0; i < roleLabels.length; i++) {
            String label = roleLabels[i];
            float ly = fixedLabelY;
            for (String line : wrapText(label, PDType1Font.HELVETICA_BOLD, 7f, colWidth - 6)) {
                content.beginText();
                content.newLineAtOffset(x + colWidth * i + 3, ly);
                content.showText(line);
                content.endText();
                ly -= 8;
            }
        }

        // Approved By label on right side
        content.setFont(PDType1Font.HELVETICA_BOLD, 7f);
        content.beginText();
        content.newLineAtOffset(x + colWidth * 5 + 3, fixedLabelY - 12);
        content.showText("Approved By:");
        content.endText();

        return y - blockHeight;
    }

    private Map<String, Object> findCapfEntryForRole(List<Map<String, Object>> approved, String[] keywords,
            int fallbackIndex, java.util.Set<Integer> usedIndexes) {
        if (approved == null || approved.isEmpty())
            return null;
        if (keywords != null && keywords.length > 0) {
            for (int i = 0; i < approved.size(); i++) {
                if (usedIndexes != null && usedIndexes.contains(i))
                    continue;
                Map<String, Object> entry = approved.get(i);
                String dept = entry.get("departmentName") != null ? entry.get("departmentName").toString().toLowerCase()
                        : "";
                String role = entry.get("role") != null ? entry.get("role").toString().toLowerCase() : "";
                String combined = (dept + " " + role).trim();
                if (combined.isEmpty())
                    continue;
                boolean allMatch = true;
                for (String kw : keywords) {
                    if (kw == null || kw.isEmpty())
                        continue;
                    if (!combined.contains(kw.toLowerCase())) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    if (usedIndexes != null)
                        usedIndexes.add(i);
                    return entry;
                }
            }
            // If keywords are provided and no match, do not fallback to index.
            return null;
        }
        // Fallback by level/order if role match isn't found
        if (fallbackIndex >= 0 && fallbackIndex < approved.size()) {
            if (usedIndexes == null || !usedIndexes.contains(fallbackIndex)) {
                if (usedIndexes != null)
                    usedIndexes.add(fallbackIndex);
                return approved.get(fallbackIndex);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>>[] mapCapfApprovalEntries(List<Map<String, Object>> approved) {
        return mapCapfApprovalEntries(approved, null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>>[] mapCapfApprovalEntries(List<Map<String, Object>> approved,
            List<Map<String, Object>> pipelines) {
        // CAPF footer has 6 functional slots; keep mapping array length 6
        List<Map<String, Object>>[] mapped = new List[6];
        for (int i = 0; i < 6; i++) {
            mapped[i] = new java.util.ArrayList<>();
        }
        if (approved == null || approved.isEmpty())
            return mapped;

        java.util.List<String> pipelineSlots = new java.util.ArrayList<>();
        if (pipelines != null && !pipelines.isEmpty()) {
            java.util.List<Map<String, Object>> sorted = new java.util.ArrayList<>(pipelines);
            sorted.sort(
                    (a, b) -> safeInt(a.get("intApprovalOrder"), 0).compareTo(safeInt(b.get("intApprovalOrder"), 0)));
            for (Map<String, Object> p : sorted) {
                if (pipelineSlots.size() >= 6)
                    break;
                pipelineSlots.add(extractPipelineDepartmentName(p));
            }
        }

        java.util.Set<Integer> usedIndexes = new java.util.HashSet<>();

        // Primary mapping by explicit order/level.
        for (int i = 0; i < approved.size(); i++) {
            if (usedIndexes.contains(i)) continue;
            Map<String, Object> entry = approved.get(i);
            Integer order = safeInt(entry.get("intApprovalOrder"), safeInt(entry.get("level"), null));
            if (order != null && order >= 0 && order < 6) {
                mapped[order].add(entry);
                usedIndexes.add(i);
                // Group extra signatures from same department into this slot
                collectSameDeptEntries(mapped[order], entry, approved, usedIndexes);
            }
        }

        // Secondary mapping by pipeline department names.
        if (!pipelineSlots.isEmpty()) {
            for (int i = 0; i < approved.size(); i++) {
                if (usedIndexes.contains(i))
                    continue;
                Map<String, Object> entry = approved.get(i);
                String entryDept = normalizeDeptText(
                        entry.get("departmentName") != null ? String.valueOf(entry.get("departmentName"))
                                : (entry.get("role") != null ? String.valueOf(entry.get("role")) : ""));
                if (entryDept.isEmpty())
                    continue;
                for (int s = 0; s < pipelineSlots.size(); s++) {
                    int targetSlot = s + 1; // Since slot 0 is reserved for Initiator's HOD
                    if (targetSlot >= mapped.length || !mapped[targetSlot].isEmpty())
                        continue;
                    String slotDept = normalizeDeptText(pipelineSlots.get(s));
                    if (slotDept.isEmpty())
                        continue;
                    if (entryDept.contains(slotDept) || slotDept.contains(entryDept)) {
                        mapped[targetSlot].add(entry);
                        usedIndexes.add(i);
                        collectSameDeptEntries(mapped[targetSlot], entry, approved, usedIndexes);
                        break;
                    }
                }
            }
        }

        // Tertiary mapping: role keyword fallback for unresolved slots.
        if (mapped[0].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "hod", "head", "dept" }, 0, usedIndexes);
            if (e != null) { mapped[0].add(e); collectSameDeptEntries(mapped[0], e, approved, usedIndexes); }
        }
        if (mapped[1].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "technical", "expert" }, 1, usedIndexes);
            if (e != null) { mapped[1].add(e); collectSameDeptEntries(mapped[1], e, approved, usedIndexes); }
        }
        if (mapped[2].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "procurement", "purchase" }, 2, usedIndexes);
            if (e != null) { mapped[2].add(e); collectSameDeptEntries(mapped[2], e, approved, usedIndexes); }
        }
        if (mapped[3].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "finance", "account" }, 3, usedIndexes);
            if (e != null) { mapped[3].add(e); collectSameDeptEntries(mapped[3], e, approved, usedIndexes); }
        }
        if (mapped[4].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "core", "htr", "cct" }, 4, usedIndexes);
            if (e != null) { mapped[4].add(e); collectSameDeptEntries(mapped[4], e, approved, usedIndexes); }
        }
        if (mapped[5].isEmpty()) {
            Map<String, Object> e = findCapfEntryForRole(approved, new String[] { "chief", "executive", "ceo", "md" }, 5, usedIndexes);
            if (e != null) { mapped[5].add(e); collectSameDeptEntries(mapped[5], e, approved, usedIndexes); }
        }

        int fillIdx = 0;
        for (int i = 0; i < mapped.length; i++) {
            if (!mapped[i].isEmpty())
                continue;
            while (fillIdx < approved.size() && usedIndexes.contains(fillIdx)) {
                fillIdx++;
            }
            if (fillIdx < approved.size()) {
                Map<String, Object> e = approved.get(fillIdx);
                mapped[i].add(e);
                usedIndexes.add(fillIdx);
                collectSameDeptEntries(mapped[i], e, approved, usedIndexes);
                fillIdx++;
            }
        }
        return mapped;
    }

    private void collectSameDeptEntries(List<Map<String, Object>> results, Map<String, Object> entry, List<Map<String, Object>> approved,
            java.util.Set<Integer> usedIndexes) {
        if (entry == null || approved == null)
            return;
        Integer deptId = safeInt(entry.get("departmentId"), safeInt(entry.get("serDepartmentId"), null));
        Object deptNameObj = entry.get("departmentName");
        String deptName = deptNameObj != null ? deptNameObj.toString().toLowerCase().trim() : null;

        for (int j = 0; j < approved.size(); j++) {
            if (usedIndexes.contains(j))
                continue;
            Map<String, Object> other = approved.get(j);
            boolean match = false;
            if (deptId != null) {
                Integer oId = safeInt(other.get("departmentId"), safeInt(other.get("serDepartmentId"), null));
                if (deptId.equals(oId))
                    match = true;
            }
            if (!match && deptName != null) {
                Object oNameObj = other.get("departmentName");
                if (oNameObj != null && deptName.equals(oNameObj.toString().toLowerCase().trim()))
                    match = true;
            }
            if (match) {
                results.add(other);
                usedIndexes.add(j);
            }
        }
    }

    private String formatApprovalDate(Object raw) {
        if (raw == null)
            return "";
        try {
            String s = raw.toString();
            if (s.trim().isEmpty())
                return "";
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd/MM/yyyy");
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return out.format(in.parse(s.substring(0, 10)));
            }
            if (s.matches("^\\d{2}/\\d{2}/\\d{4}.*")) {
                return s.substring(0, 10);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String extractPipelineDepartmentName(Map<String, Object> pipeline) {
        if (pipeline == null)
            return "";
        Object name = pipeline.get("departmentName");
        if (name == null)
            name = pipeline.get("txtDepartmentName");
        if (name == null) {
            Object hrDept = pipeline.get("hrTblDepartment");
            if (hrDept instanceof Map) {
                Object nested = ((Map<?, ?>) hrDept).get("txtDepartmentName");
                if (nested == null)
                    nested = ((Map<?, ?>) hrDept).get("departmentName");
                name = nested;
            }
        }
        return name != null ? String.valueOf(name) : "";
    }

    private String normalizeDeptText(String text) {
        if (text == null)
            return "";
        return text.toLowerCase()
                .replace(".", " ")
                .replace("/", " ")
                .replace("-", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<Map<String, Object>> parseApprovalHistory(String approvalHistoryJson) {
        if (approvalHistoryJson == null || approvalHistoryJson.trim().isEmpty())
            return new java.util.ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(approvalHistoryJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing approval history: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    private String findSignatureForUser(List<Map<String, Object>> history, Integer userId, String roleExpected,
            boolean allowFallback, Map<Integer, String> signatureFromDb) {
        if (userId == null)
            return "";
        if (history != null && !history.isEmpty()) {
            for (Map<String, Object> entry : history) {
                Object idObj = entry.get("approvedBy");
                if (idObj == null)
                    continue;
                Integer id = idObj instanceof Integer ? (Integer) idObj : Integer.parseInt(idObj.toString());
                if (!id.equals(userId))
                    continue;
                String role = entry.get("role") != null ? entry.get("role").toString() : "";
                if (roleExpected != null && !roleExpected.equalsIgnoreCase(role))
                    continue;
                String action = entry.get("action") != null ? entry.get("action").toString() : "";
                if ("REJECTED".equalsIgnoreCase(action))
                    continue;
                Object sigObj = entry.get("signaturePath");
                if (sigObj == null)
                    return "";
                String sig = sigObj.toString();
                if (!sig.trim().isEmpty())
                    return sig;
            }
        }
        if (allowFallback && signatureFromDb != null) {
            String sig = signatureFromDb.get(userId);
            return sig != null ? sig : "";
        }
        return "";
    }

    private String resolveDepartmentName(EntityManager entityManager, Integer departmentId,
            Map<String, Object> pipelineMap) {
        String name = null;
        if (pipelineMap != null) {
            Object nameObj = pipelineMap.get("departmentName");
            if (nameObj == null)
                nameObj = pipelineMap.get("txtDepartmentName");
            if (nameObj == null) {
                Object deptObj = pipelineMap.get("hrTblDepartment");
                if (deptObj instanceof Map) {
                    Map<?, ?> deptMap = (Map<?, ?>) deptObj;
                    Object nestedName = deptMap.get("txtDepartmentName");
                    if (nestedName == null)
                        nestedName = deptMap.get("departmentName");
                    if (nestedName != null) {
                        nameObj = nestedName;
                    }
                }
            }
            if (nameObj != null) {
                name = String.valueOf(nameObj);
            }
        }

        if ((name == null || name.trim().isEmpty()) && departmentId != null) {
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept = entityManager
                    .find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, departmentId);
            if (dept != null && dept.getTxtDepartmentName() != null && !dept.getTxtDepartmentName().trim().isEmpty()) {
                name = dept.getTxtDepartmentName();
            }
        }

        return name;
    }

    private Map<Integer, String> loadUserSignaturePaths(Integer[] userIds) {
        Map<Integer, String> map = new java.util.HashMap<>();
        if (userIds == null || userIds.length == 0)
            return map;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            for (Integer id : userIds) {
                if (id == null || map.containsKey(id))
                    continue;
                CfgTblUser u = em.find(CfgTblUser.class, id);
                if (u != null && u.getTxtSignaturePath() != null && !u.getTxtSignaturePath().trim().isEmpty()) {
                    map.put(id, u.getTxtSignaturePath());
                    log.info("CAPF signature log [db-signature]: userId={}, signaturePath={}", id,
                            u.getTxtSignaturePath());
                } else {
                    log.warn(
                            "CAPF signature log [db-signature-missing]: userId={} has no signaturePath in cfg_tbl_user",
                            id);
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.warn("Error loading user signatures: " + e.getMessage(), e);
        } finally {
            if (em.isOpen())
                em.close();
        }
        return map;
    }

    private Integer extractUserId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object idObj = map.get("serUserId");
            if (idObj == null)
                idObj = map.get("userId");
            if (idObj == null)
                idObj = map.get("id");
            if (idObj == null)
                return null;
            try {
                return idObj instanceof Integer ? (Integer) idObj : Integer.parseInt(idObj.toString());
            } catch (Exception e) {
                return null;
            }
        }
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String extractUserName(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            return s.isEmpty() ? null : s;
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object nameObj = map.get("txtUserName");
            if (nameObj == null)
                nameObj = map.get("userName");
            if (nameObj == null)
                nameObj = map.get("name");
            return nameObj != null ? nameObj.toString() : null;
        }
        return null;
    }

    private Integer resolveUserIdByName(String userName) {
        if (userName == null || userName.trim().isEmpty())
            return null;
        EntityManager em = getEntityManager();
        try {
            String cleaned = userName.trim();
            int parenIdx = cleaned.indexOf('(');
            if (parenIdx > 0) {
                cleaned = cleaned.substring(0, parenIdx).trim();
            }
            TypedQuery<Integer> q = em.createQuery(
                    "select u.serUserId from CfgTblUser u where lower(u.txtUserName) = :name", Integer.class);
            q.setParameter("name", cleaned.toLowerCase());
            List<Integer> ids = q.setMaxResults(1).getResultList();
            return ids.isEmpty() ? null : ids.get(0);
        } catch (Exception e) {
            log.warn("Error resolving userId by name: " + e.getMessage(), e);
            return null;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    private Integer extractUserIdByIndex(Object listObj, int index) {
        if (!(listObj instanceof java.util.List))
            return null;
        java.util.List<?> list = (java.util.List<?>) listObj;
        if (index < 0 || index >= list.size())
            return null;
        return extractUserId(list.get(index));
    }

    private Object extractUserByIndex(Object listObj, int index) {
        if (!(listObj instanceof java.util.List))
            return null;
        java.util.List<?> list = (java.util.List<?>) listObj;
        if (index < 0 || index >= list.size())
            return null;
        return list.get(index);
    }

    private void drawSignatureImage(PDDocument document, PDPageContentStream content, String signaturePath,
            float x, float y, float maxWidth, float maxHeight) {
        try {
            String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
            File sigFile = resolveSignatureFile(rootPath, signaturePath);
            if (sigFile == null || !sigFile.exists())
                return;
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject img = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
                    .createFromFile(sigFile.getAbsolutePath(), document);
            float imgW = img.getWidth();
            float imgH = img.getHeight();
            if (imgW <= 0 || imgH <= 0)
                return;
            float scale = Math.min(maxWidth / imgW, maxHeight / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;
            float drawX = x + (maxWidth - drawW) / 2f;
            float drawY = y + (maxHeight - drawH) / 2f;
            content.drawImage(img, drawX, drawY, drawW, drawH);
        } catch (Exception e) {
            log.warn("Error drawing signature image: " + e.getMessage());
        }
    }

    private File resolveSignatureFile(String rootPath, String signaturePath) {
        if (signaturePath == null || signaturePath.trim().isEmpty())
            return null;
        File direct = new File(signaturePath);
        if (!direct.isAbsolute()) {
            direct = new File(rootPath + File.separator + signaturePath);
        }
        if (!direct.exists()) {
            File inSignatures = new File(rootPath + File.separator + "signatures" + File.separator + signaturePath);
            if (inSignatures.exists()) {
                direct = inSignatures;
            }
        }
        String directName = direct.getName().toLowerCase();
        if (direct.exists() && !directName.endsWith(".webp")) {
            return direct;
        }
        if (direct.exists() && directName.endsWith(".webp")) {
            // Try to find a PNG/JPG alternative for the same user
            String fileName = direct.getName();
            Integer userId = extractUserIdFromSignatureName(fileName);
            File dir = direct.getParentFile();
            if (dir != null && userId != null) {
                File[] matches = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.startsWith("signature_" + userId + "_") &&
                            (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"));
                });
                if (matches != null && matches.length > 0) {
                    return matches[0];
                }
            }
        }
        if (direct.exists() && directName.endsWith(".webp")) {
            return null;
        }
        return direct.exists() ? direct : null;
    }

    private Integer extractUserIdFromSignatureName(String fileName) {
        if (fileName == null)
            return null;
        try {
            // Expected: signature_{userId}_xxxx.ext
            if (!fileName.startsWith("signature_"))
                return null;
            String rest = fileName.substring("signature_".length());
            int idx = rest.indexOf('_');
            if (idx <= 0)
                return null;
            String idStr = rest.substring(0, idx);
            return Integer.parseInt(idStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer loadUserDepartmentId(EntityManager em, Integer userId) {
        if (em == null || userId == null)
            return null;
        try {
            Object result = em.createNativeQuery(
                    "select ser_department_id from cfg_tbl_user where ser_user_id = :id")
                    .setParameter("id", userId)
                    .getSingleResult();
            if (result == null)
                return null;
            if (result instanceof Number)
                return ((Number) result).intValue();
            return Integer.parseInt(result.toString());
        } catch (Exception e) {
            log.warn("Error loading user department: " + e.getMessage(), e);
            return null;
        }
    }

    private Integer findDepartmentHeadUserId(EntityManager em, Integer departmentId) {
        if (em == null || departmentId == null)
            return null;
        try {
            List<?> rows = em.createNativeQuery(
                    "select u.ser_user_id " +
                            "from cfg_tbl_user u " +
                            "left join cfg_tbl_role r on r.ser_role_id = u.ser_role_id " +
                            "where u.ser_department_id = :dept " +
                            "and (upper(r.txt_role_name) like '%HEAD%' or upper(r.txt_role_name) like '%HOD%') " +
                            "and (u.bl_is_active = 1 or u.bl_is_active is null) " +
                            "and (u.bl_is_deleted = 0 or u.bl_is_deleted is null) " +
                            "limit 1")
                    .setParameter("dept", departmentId)
                    .getResultList();
            if (rows == null || rows.isEmpty())
                return null;
            Object val = rows.get(0);
            if (val instanceof Number)
                return ((Number) val).intValue();
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            log.warn("Error finding department head user: " + e.getMessage(), e);
            return null;
        }
    }

    private java.util.List<Integer> findDepartmentHeadUserIds(EntityManager em, Integer departmentId) {
        java.util.List<Integer> headIds = new java.util.ArrayList<>();
        if (em == null || departmentId == null)
            return headIds;
        try {
            List<?> rows = em.createNativeQuery(
                    "select u.ser_user_id " +
                            "from cfg_tbl_user u " +
                            "left join cfg_tbl_role r on r.ser_role_id = u.ser_role_id " +
                            "where u.ser_department_id = :dept " +
                            "and (upper(r.txt_role_name) like '%HEAD%' or upper(r.txt_role_name) like '%HOD%') " +
                            "and (u.bl_is_active = 1 or u.bl_is_active is null) " +
                            "and (u.bl_is_deleted = 0 or u.bl_is_deleted is null)")
                    .setParameter("dept", departmentId)
                    .getResultList();
            if (rows != null) {
                for (Object row : rows) {
                    if (row instanceof Number) {
                        headIds.add(((Number) row).intValue());
                    } else {
                        headIds.add(Integer.parseInt(row.toString()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error finding department head users: " + e.getMessage(), e);
        }
        return headIds;
    }

    private void drawCenteredHeader(PDPageContentStream content, String text, float x, float width, float y)
            throws java.io.IOException {
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * 9;
        float tx = x + (width - textWidth) / 2;
        content.beginText();
        content.newLineAtOffset(tx, y);
        content.showText(text);
        content.endText();
    }

    private void drawRect(PDPageContentStream content, float x, float y, float width, float height)
            throws java.io.IOException {
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2)
            throws java.io.IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void drawMeta(PDPageContentStream content, float x, float y, String text) throws java.io.IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }

    private void drawCentered(PDPageContentStream content, float pageWidth, float y, String text)
            throws java.io.IOException {
        float size = 10f;
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * size;
        content.beginText();
        content.newLineAtOffset((pageWidth - textWidth) / 2, y);
        content.showText(text);
        content.endText();
    }

    private float drawLabeledLine(PDPageContentStream content, float x, float y, float labelWidth, float x2,
            String label, String value) throws java.io.IOException {
        return drawLabeledLine(content, x, y, labelWidth, x2, label, value, 0);
    }

    private float drawLabeledLine(PDPageContentStream content, float x, float y, float labelWidth, float x2,
            String label, String value, float valueOffset) throws java.io.IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(label);
        content.endText();
        float lineY = y - 3;
        float lineStart = x + labelWidth + valueOffset;
        drawLine(content, lineStart, lineY, x2, lineY);
        if (value != null && !value.trim().isEmpty()) {
            content.beginText();
            content.newLineAtOffset(lineStart + 2, y - 2);
            content.showText(trimToWidth(value, PDType1Font.HELVETICA, 9, x2 - lineStart - 4));
            content.endText();
        }
        return y - 14;
    }

    private float drawYesNoRow(PDPageContentStream content, float x, float y, float x2, String label, String value)
            throws java.io.IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(label);
        content.endText();
        float boxSize = 8f;
        float start = x + 180;
        drawBox(content, start, y - 6, boxSize);
        drawBox(content, start + 30, y - 6, boxSize);
        content.beginText();
        content.newLineAtOffset(start + 12, y - 1);
        content.showText("Yes");
        content.endText();
        content.beginText();
        content.newLineAtOffset(start + 42, y - 1);
        content.showText("No");
        content.endText();

        String v = value != null ? value.toLowerCase() : "";
        if (v.contains("yes")) {
            drawCheck(content, start + 1, y - 5);
        } else if (v.contains("no")) {
            drawCheck(content, start + 31, y - 5);
        }
        return y - 14;
    }

    private float drawYesNoNaRow(PDPageContentStream content, float x, float y, float x2, String label, String value)
            throws java.io.IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(label);
        content.endText();
        float boxSize = 8f;
        float start = x + 220;
        drawBox(content, start, y - 6, boxSize);
        drawBox(content, start + 30, y - 6, boxSize);
        drawBox(content, start + 60, y - 6, boxSize);
        content.beginText();
        content.newLineAtOffset(start + 12, y - 1);
        content.showText("Yes");
        content.endText();
        content.beginText();
        content.newLineAtOffset(start + 42, y - 1);
        content.showText("No");
        content.endText();
        content.beginText();
        content.newLineAtOffset(start + 72, y - 1);
        content.showText("NA");
        content.endText();

        String v = value != null ? value.toLowerCase() : "";
        if (v.contains("yes")) {
            drawCheck(content, start + 1, y - 5);
        } else if (v.contains("no")) {
            drawCheck(content, start + 31, y - 5);
        } else if (v.contains("na") || v.contains("n/a")) {
            drawCheck(content, start + 61, y - 5);
        }
        return y - 14;
    }

    private void drawSignatureLineRow(PDPageContentStream content, float x, float y, float x2, String leftLabel,
            String rightLabel) throws java.io.IOException {
        float mid = x + (x2 - x) / 2;
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(leftLabel);
        content.endText();
        drawLine(content, x + 55, y - 3, mid - 10, y - 3);
        content.beginText();
        content.newLineAtOffset(mid + 5, y);
        content.showText(rightLabel);
        content.endText();
        drawLine(content, mid + 65, y - 3, x2, y - 3);
    }

    private void drawBox(PDPageContentStream content, float x, float y, float size) throws java.io.IOException {
        content.addRect(x, y, size, size);
        content.stroke();
    }

    private void drawCheck(PDPageContentStream content, float x, float y) throws java.io.IOException {
        content.moveTo(x + 1, y + 3);
        content.lineTo(x + 3, y + 1);
        content.lineTo(x + 7, y + 6);
        content.stroke();
    }

    private String trimToWidth(String text, PDType1Font font, float fontSize, float maxWidth)
            throws java.io.IOException {
        if (text == null)
            return "";
        String t = text.replaceAll("\\s+", " ").trim();
        while (font.getStringWidth(t) / 1000 * fontSize > maxWidth && t.length() > 0) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private String getCapfValue(Map<String, Object> appData, String key) {
        if (appData == null || key == null)
            return null;
        String k = key.toLowerCase();
        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            String label = entry.getKey();
            if (label == null)
                continue;
            String normalized = label.replace(":", "").toLowerCase();
            if (normalized.contains(k)) {
                Object val = entry.getValue();
                if (val != null)
                    return String.valueOf(val);
            }
        }
        return null;
    }

    private String nullSafe(String v) {
        return v != null ? v : "";
    }

    private Integer extractInitialSignerId(CfgTblCustomFormApplication application) {
        try {
            String appDataJson = application.getTxtApplicationData();
            if (appDataJson == null || appDataJson.trim().isEmpty())
                return null;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> appData = mapper.readValue(
                    appDataJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });

            Object signer = appData.get("initial_signer");
            if (signer != null && !signer.toString().trim().isEmpty()) {
                return resolveUserIdByName(signer.toString());
            }
        } catch (Exception e) {
            log.warn("Error extracting initial signer ID: " + e.getMessage());
        }
        return null;
    }

    private boolean hasInitialSigner(CfgTblCustomFormApplication application) {
        return extractInitialSignerId(application) != null;
    }

    private boolean isCapfForm(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form) {
        if (form == null)
            return false;
        String name = form.getTxtFormName();
        String code = form.getTxtFormCode();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("capf") || lower.contains("capital assets purchase"))
                return true;
        }
        if (code != null) {
            String lower = code.toLowerCase();
            if (lower.startsWith("capf") || lower.contains("capf"))
                return true;
        }
        return false;
    }

    private java.util.Map<String, String> extractUserDisplay(Object obj) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (!(obj instanceof Map))
            return result;
        Map<?, ?> map = (Map<?, ?>) obj;
        Object nameObj = map.get("txtUserName");
        if (nameObj == null)
            nameObj = map.get("userName");
        Object roleObj = null;
        Object roleMap = map.get("cfgTblRole");
        if (roleMap instanceof Map) {
            roleObj = ((Map<?, ?>) roleMap).get("txtRoleName");
        }
        if (roleObj == null)
            roleObj = map.get("roleName");
        if (nameObj != null)
            result.put("name", nameObj.toString());
        if (roleObj != null)
            result.put("role", roleObj.toString());
        return result;
    }

    private java.util.List<java.util.Map<String, String>> extractUserListDisplay(Object obj) {
        java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();
        if (!(obj instanceof java.util.List))
            return list;
        for (Object item : (java.util.List<?>) obj) {
            list.add(extractUserDisplay(item));
        }
        return list;
    }

    private String formatUserDisplay(java.util.Map<String, String> data) {
        if (data == null || data.isEmpty())
            return "";
        String name = data.getOrDefault("name", "");
        String role = data.getOrDefault("role", "");
        if (!role.isEmpty())
            return name + " (" + role + ")";
        return name;
    }

    private float writeLine(PDPageContentStream content, float y, String text) throws java.io.IOException {
        content.beginText();
        content.newLineAtOffset(40, y);
        content.showText(text != null ? text : "");
        content.endText();
        return y - 14;
    }

    private String formatPdfValue(Object valObj) {
        if (valObj == null)
            return "";
        if (valObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) valObj;
            if (map.containsKey("dataUrl") || map.containsKey("base64")) {
                return "[attachment]";
            }
            return map.toString();
        }
        String val = valObj.toString();
        if (val.length() > 200) {
            return val.substring(0, 200) + "...";
        }
        return val;
    }

    private static String capfEmailTemplateCache = null;

    private String loadCapfEmailTemplate() {
        if (capfEmailTemplateCache != null)
            return capfEmailTemplateCache;
        try (InputStream input = new ClassPathResource("templates/capf-email-fragment.html").getInputStream()) {
            capfEmailTemplateCache = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            return capfEmailTemplateCache;
        } catch (Exception e) {
            log.warn("Unable to load CAPF email template: " + e.getMessage());
            return "";
        }
    }

    private String generateCapfEmailFragment(CfgTblCustomFormApplication application, CfgTblCustomForm form,
            List<Map<String, Object>> pipelines) {
        String template = loadCapfEmailTemplate();
        if (template == null || template.trim().isEmpty())
            return "";

        Map<String, Object> appData = parseApplicationData(application);
        List<CfgTblCustomFormField> formFields = form != null ? form.getCfgTblCustomFormFields() : null;

        String capfNumber = getFieldValue("CAPF #", appData, formFields);
        if (capfNumber.isEmpty() && application.getTxtFormCode() != null) {
            capfNumber = application.getTxtFormCode();
        }

        String dateValue = getFieldValue("Date", appData, formFields);
        if (dateValue.isEmpty() && application.getDteCreatedDate() != null) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            dateValue = df.format(application.getDteCreatedDate());
        }

        String feasibilityValue = getFieldValue("FEASIBILITY REPORT ATTACHED", appData, formFields);
        boolean feasibilityYes = isTruthyYes(feasibilityValue);
        boolean feasibilityNo = isTruthyNo(feasibilityValue);

        String thirdPartyValue = getFieldValue("Third Party assessment carried out", appData, formFields);
        if (thirdPartyValue.isEmpty())
            thirdPartyValue = getFieldValue("Third Party Assessment", appData, formFields);
        if (thirdPartyValue.isEmpty())
            thirdPartyValue = getFieldValue("Third Party assessment", appData, formFields);
        boolean thirdPartyYes = isTruthyYes(thirdPartyValue);
        boolean thirdPartyNo = isTruthyNo(thirdPartyValue);
        boolean thirdPartyNA = isTruthyNa(thirdPartyValue);

        String reason = getFieldValue("IF NO THEN MENTION REASON:", appData, formFields);
        if (reason.isEmpty())
            reason = getFieldValue("IF NO THEN MENTION REASON", appData, formFields);

        String signatureSlots = buildCapfSignatureSlotsHtml(pipelines, application.getTxtApprovalHistory(),
                getBaseUrl());

        String check = "<span>&#10003;</span>";
        String logoUrl = getBaseUrl() + "/assets/images/qarshi-logo.png";

        String html = template;
        html = html.replace("{{LOGO_URL}}", escapeHtml(logoUrl));
        html = html.replace("{{CAPF_NUMBER}}", escapeHtml(capfNumber));
        html = html.replace("{{DIVISION_DEPARTMENT}}",
                escapeHtml(getFieldValue("DIVISION / DEPARTMENT", appData, formFields)));
        html = html.replace("{{DATE}}", escapeHtml(dateValue));
        html = html.replace("{{ASSET_NAME}}", escapeHtml(getFieldValue("NAME OF ASSET / ITEM", appData, formFields)));
        html = html.replace("{{SPECIFICATION}}",
                escapeHtml(getFieldValue("DETAIL SPECIFICATION", appData, formFields)));
        html = html.replace("{{UTILITY_PURPOSE}}", escapeHtml(getFieldValue("UTILITY & PURPOSE", appData, formFields)));
        html = html.replace("{{FEASIBILITY_YES}}", feasibilityYes ? check : "");
        html = html.replace("{{FEASIBILITY_NO}}", feasibilityNo ? check : "");
        html = html.replace("{{FEASIBILITY_REASON}}", escapeHtml(reason));
        html = html.replace("{{VENDOR_NAME}}", escapeHtml(getFieldValue("NAME", appData, formFields)));
        html = html.replace("{{VENDOR_ADDRESS}}", escapeHtml(getFieldValue("ADDRESS", appData, formFields)));
        html = html.replace("{{APPROVED_PRICE}}", escapeHtml(getFieldValue("APPROVED PRICE", appData, formFields)));
        html = html.replace("{{DELIVERY_PERIOD}}",
                escapeHtml(getFieldValue("DELIVERY PERIOD & DATE", appData, formFields)));
        html = html.replace("{{TERMS_CONDITIONS}}",
                escapeHtml(getFieldValue("TERMS & CONDITIONS", appData, formFields)));
        html = html.replace("{{THIRD_PARTY_YES}}", thirdPartyYes ? check : "");
        html = html.replace("{{THIRD_PARTY_NO}}", thirdPartyNo ? check : "");
        html = html.replace("{{THIRD_PARTY_NA}}", thirdPartyNA ? check : "");
        html = html.replace("{{SIGNATURE_SLOTS}}", signatureSlots != null ? signatureSlots : "");

        return html;
    }

    private boolean isTruthyYes(String value) {
        if (value == null)
            return false;
        String v = value.trim().toLowerCase();
        return "yes".equals(v) || "true".equals(v);
    }

    private boolean isTruthyNo(String value) {
        if (value == null)
            return false;
        String v = value.trim().toLowerCase();
        return "no".equals(v) || "false".equals(v);
    }

    private boolean isTruthyNa(String value) {
        if (value == null)
            return false;
        String v = value.trim().toLowerCase();
        return "na".equals(v) || "n/a".equals(v) || "not applicable".equals(v);
    }

    private String buildCapfSignatureSlotsHtml(List<Map<String, Object>> pipelines, String approvalHistoryJson,
            String baseUrl) {
        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        java.util.Set<Integer> usedIndices = new java.util.HashSet<>();
        List<Map<String, Object>> sortedPipelines = new java.util.ArrayList<>();
        if (pipelines != null) {
            sortedPipelines.addAll(pipelines);
            sortedPipelines.sort((a, b) -> {
                Integer aOrder = safeInt(a.get("intApprovalOrder"), 0);
                Integer bOrder = safeInt(b.get("intApprovalOrder"), 0);
                return aOrder.compareTo(bOrder);
            });
        }

        List<CapfSignatureSlot> slots = new java.util.ArrayList<>();
        if (sortedPipelines.isEmpty()) {
            slots.add(buildFallbackSlot(1, "User Deptt. (HoD)", approvalHistory, baseUrl, usedIndices));
            slots.add(buildFallbackSlot(2, "Technical Expert", approvalHistory, baseUrl, usedIndices));
            slots.add(buildFallbackSlot(3, "Procurement", approvalHistory, baseUrl, usedIndices));
            slots.add(buildFallbackSlot(4, "Finance", approvalHistory, baseUrl, usedIndices));
            slots.add(buildFallbackSlot(5, "Core Team HTR. / CCT HO", approvalHistory, baseUrl, usedIndices));
        } else {
            int index = 0;
            for (Map<String, Object> pipeline : sortedPipelines) {
                index++;
                Integer order = safeInt(pipeline.get("intApprovalOrder"), index);
                Integer departmentId = null;
                Object deptMap = pipeline.get("hrTblDepartment");
                String label = null;
                if (deptMap instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) deptMap;
                    departmentId = safeInt(map.get("serDepartmentId"), null);
                    Object name = map.get("txtDepartmentName");
                    if (name != null)
                        label = name.toString();
                }
                if (departmentId == null) {
                    departmentId = safeInt(pipeline.get("serDepartmentId"), null);
                }
                if (departmentId == null) {
                    departmentId = safeInt(pipeline.get("departmentId"), null);
                }
                if (label == null) {
                    Object name = pipeline.get("departmentName");
                    if (name == null)
                        name = pipeline.get("txtDepartmentName");
                    if (name != null)
                        label = name.toString();
                }

                List<Map<String, Object>> entries = getApprovalEntriesForPipeline(order, departmentId, label,
                        approvalHistory, usedIndices);
                String slotLabel = label != null && !label.trim().isEmpty() ? label : ("Department " + order);
                slots.add(buildSlotFromEntries(slotLabel, entries, baseUrl));
            }
        }

        StringBuilder html = new StringBuilder();
        html.append(
                "<table width=\"100%\" style=\"width:100%; border-collapse: collapse; table-layout: fixed; margin-top: 15px;\">");

        // ROW 1: Signatures and lines
        html.append("<tr>");
        for (CapfSignatureSlot slot : slots) {
            html.append(
                    "<td align=\"center\" valign=\"bottom\" style=\"padding: 0 4px; border-bottom: 2px solid #222; height: 50px;\">");
            html.append(slot.html != null ? slot.html : "");
            html.append("</td>");
        }
        html.append("</tr>");

        // ROW 2: Metadata (Names and Dates)
        html.append("<tr>");
        for (CapfSignatureSlot slot : slots) {
            html.append("<td align=\"center\" valign=\"top\" style=\"padding: 4px 2px; height: 35px;\">");
            if (slot.metaHtml != null && !slot.metaHtml.isEmpty()) {
                html.append(slot.metaHtml);
            } else if (slot.time != null && !slot.time.isEmpty()) {
                html.append("<div style=\"font-size: 10px; line-height: 1.1; text-align: center;\">")
                        .append(escapeHtml(slot.time)).append("</div>");
            }
            html.append("</td>");
        }
        html.append("</tr>");

        // ROW 3: Slot Labels (e.g. Procurement)
        html.append("<tr>");
        for (CapfSignatureSlot slot : slots) {
            html.append("<td align=\"center\" valign=\"top\" style=\"padding-top: 2px;\">");
            html.append(
                    "<div style=\"font-size: 11px; font-weight: bold; text-align: center; line-height: 1.2; text-transform: uppercase;\">")
                    .append(escapeHtml(slot.label)).append("</div>");
            html.append("</td>");
        }
        html.append("</tr>");

        html.append("</table>");
        return html.toString();
    }

    private CapfSignatureSlot buildFallbackSlot(int order, String label, List<Map<String, Object>> approvalHistory,
            String baseUrl, java.util.Set<Integer> usedIndices) {
        List<Map<String, Object>> entries = getApprovalEntriesForPipeline(order, null, null, approvalHistory,
                usedIndices);
        return buildSlotFromEntries(label, entries, baseUrl);
    }

    private CapfSignatureSlot buildSlotFromEntries(String label, List<Map<String, Object>> entries, String baseUrl) {
        CapfSignatureSlot slot = new CapfSignatureSlot();
        slot.label = label != null ? label : "";
        slot.html = "";
        slot.time = "";
        slot.metaHtml = "";

        if (entries == null || entries.isEmpty())
            return slot;

        StringBuilder sigHtml = new StringBuilder();
        StringBuilder metaHtml = new StringBuilder();

        boolean isMulti = entries.size() > 1;
        if (isMulti) {
            sigHtml.append("<table width=\"100%\" style=\"width:100%; border-collapse: collapse;\"><tr>");
            metaHtml.append("<table width=\"100%\" style=\"width:100%; border-collapse: collapse;\"><tr>");
        }

        for (Map<String, Object> entry : entries) {
            String signaturePath = entry.get("signaturePath") != null ? String.valueOf(entry.get("signaturePath")) : "";
            String approvedBy = entry.get("approvedBy") != null ? String.valueOf(entry.get("approvedBy"))
                    : entry.get("approverUserId") != null ? String.valueOf(entry.get("approverUserId"))
                            : entry.get("userId") != null ? String.valueOf(entry.get("userId")) : "";
            Integer approvedById = safeInt(approvedBy, null);
            String inlineSignature = buildInlineSignatureDataUri(signaturePath, approvedById);

            String sigImgHtml = "";
            if (inlineSignature != null && !inlineSignature.isEmpty()) {
                sigImgHtml = "<img src=\"" + inlineSignature
                        + "\" style=\"max-height: 24px; max-width: 100%; width: auto; height: auto; object-fit: contain; display: block; margin: 0 auto 4px auto; box-sizing: border-box;\" alt=\"Sig\" />";
            } else if (!signaturePath.trim().isEmpty() && !approvedBy.trim().isEmpty() && baseUrl != null) {
                String sigUrl = baseUrl + "/getSignature?userId=" + approvedBy;
                sigImgHtml = "<img src=\"" + sigUrl
                        + "\" style=\"max-height: 24px; max-width: 100%; width: auto; height: auto; object-fit: contain; display: block; margin: 0 auto 4px auto; box-sizing: border-box;\" alt=\"Sig\" />";
            }

            String dateStr = entry.get("approvedDate") != null ? formatApprovalDate(entry.get("approvedDate")) : "";
            String userName = entry.get("approverName") != null ? String.valueOf(entry.get("approverName"))
                    : entry.get("userName") != null ? String.valueOf(entry.get("userName")) : "";
            String designation = entry.get("txtDesignation") != null ? String.valueOf(entry.get("txtDesignation"))
                    : entry.get("designation") != null ? String.valueOf(entry.get("designation"))
                            : entry.get("role") != null ? String.valueOf(entry.get("role")) : "";

            if (isMulti) {
                sigHtml.append("<td align=\"center\" width=\"50%\">").append(sigImgHtml).append("</td>");

                metaHtml.append(
                        "<td align=\"center\" width=\"50%\" valign=\"top\" style=\"font-size: 9px; line-height: 1.0;\">");
                if (!dateStr.isEmpty())
                    metaHtml.append("<div>").append(escapeHtml(dateStr)).append("</div>");
                if (!userName.isEmpty())
                    metaHtml.append("<div style=\"font-weight:bold;\">").append(escapeHtml(userName.toLowerCase()))
                            .append("</div>");
                if (!designation.isEmpty())
                    metaHtml.append("<div>").append(escapeHtml(designation)).append("</div>");
                metaHtml.append("</td>");
            } else {
                sigHtml.append(sigImgHtml);

                if (!dateStr.isEmpty())
                    metaHtml.append("<div style=\"font-size: 10px; margin-bottom: 1px;\">").append(escapeHtml(dateStr))
                            .append("</div>");
                if (!userName.isEmpty())
                    metaHtml.append("<div style=\"font-size: 10px; font-weight: bold;\">")
                            .append(escapeHtml(userName.toLowerCase())).append("</div>");
                if (!designation.isEmpty())
                    metaHtml.append("<div style=\"font-size: 9px;\">").append(escapeHtml(designation)).append("</div>");
                slot.time = dateStr;
            }
        }

        if (isMulti) {
            sigHtml.append("</tr></table>");
            metaHtml.append("</tr></table>");
        }

        slot.html = sigHtml.toString();
        slot.metaHtml = metaHtml.toString();

        return slot;
    }

    private List<Map<String, Object>> getApprovalEntriesForPipeline(int order, Integer departmentId,
            String departmentName, List<Map<String, Object>> approvalHistory, java.util.Set<Integer> usedIndices) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        if (approvalHistory == null || approvalHistory.isEmpty())
            return results;

        java.util.Set<String> seenKeys = new java.util.HashSet<>();

        // 1. Primary Mapping: Group by Department (ID or Name) if provided via pipeline
        if (departmentId != null) {
            for (int i = 0; i < approvalHistory.size(); i++) {
                if (usedIndices.contains(i))
                    continue;
                Map<String, Object> e = approvalHistory.get(i);
                Integer dId = safeInt(e.get("departmentId"), safeInt(e.get("serDepartmentId"), null));
                if (dId != null && dId.equals(departmentId)) {
                    addUniqueEntry(e, results, seenKeys);
                    usedIndices.add(i);
                }
            }
        }

        if (results.isEmpty() && departmentName != null && !departmentName.trim().isEmpty()) {
            String nameLower = departmentName.toLowerCase().trim();
            for (int i = 0; i < approvalHistory.size(); i++) {
                if (usedIndices.contains(i))
                    continue;
                Map<String, Object> e = approvalHistory.get(i);
                Object dep = e.get("departmentName");
                if (dep != null && dep.toString().toLowerCase().trim().equals(nameLower)) {
                    addUniqueEntry(e, results, seenKeys);
                    usedIndices.add(i);
                }
            }
        }

        // 2. Fallback Mapping: Match by Order (Level) and then GRAB ALL from the same department
        // This handles cases where levels increment but they belong to the same functional slot.
        if (results.isEmpty()) {
            Integer primaryDeptId = null;
            String primaryDeptName = null;

            // Find the first matching entry by level/order
            for (int i = 0; i < approvalHistory.size(); i++) {
                if (usedIndices.contains(i))
                    continue;
                Map<String, Object> e = approvalHistory.get(i);
                Integer level = safeInt(e.get("level"), null);
                Integer intApprovalOrder = safeInt(e.get("intApprovalOrder"), null);
                if ((level != null && level == order) || (intApprovalOrder != null && intApprovalOrder == order)) {
                    addUniqueEntry(e, results, seenKeys);
                    usedIndices.add(i);
                    primaryDeptId = safeInt(e.get("departmentId"), safeInt(e.get("serDepartmentId"), null));
                    Object dep = e.get("departmentName");
                    if (dep != null)
                        primaryDeptName = dep.toString();
                    break;
                }
            }

            // Group all other signatures from the same department into this slot
            if (primaryDeptId != null || primaryDeptName != null) {
                for (int i = 0; i < approvalHistory.size(); i++) {
                    if (usedIndices.contains(i))
                        continue;
                    Map<String, Object> e = approvalHistory.get(i);
                    boolean deptMatch = false;
                    if (primaryDeptId != null) {
                        Integer dId = safeInt(e.get("departmentId"), safeInt(e.get("serDepartmentId"), null));
                        if (dId != null && dId.equals(primaryDeptId))
                            deptMatch = true;
                    }
                    if (!deptMatch && primaryDeptName != null) {
                        Object dep = e.get("departmentName");
                        if (dep != null && dep.toString().equals(primaryDeptName))
                            deptMatch = true;
                    }

                    if (deptMatch) {
                        addUniqueEntry(e, results, seenKeys);
                        usedIndices.add(i);
                    }
                }
            }
        }

        return results;
    }

    private void addUniqueEntry(Map<String, Object> entry, List<Map<String, Object>> list, Set<String> seenKeys) {
        String approverId = entry.get("approvedBy") != null ? String.valueOf(entry.get("approvedBy"))
                : entry.get("approverUserId") != null ? String.valueOf(entry.get("approverUserId"))
                        : entry.get("userId") != null ? String.valueOf(entry.get("userId")) : "";
        String sigPath = entry.get("signaturePath") != null ? String.valueOf(entry.get("signaturePath")) : "";
        String date = entry.get("approvedDate") != null ? String.valueOf(entry.get("approvedDate")) : "";
        String key = approverId + "_" + sigPath + "_" + date;

        if (!seenKeys.contains(key)) {
            seenKeys.add(key);
            list.add(entry);
        }
    }

    private Map<String, Object> getApprovalEntryForPipeline(int order, Integer departmentId, String departmentName,
            List<Map<String, Object>> approvalHistory) {
        if (approvalHistory == null || approvalHistory.isEmpty())
            return null;

        Map<String, Object> entry = null;
        if (departmentId != null) {
            for (Map<String, Object> e : approvalHistory) {
                Integer level = safeInt(e.get("level"), null);
                Integer intApprovalOrder = safeInt(e.get("intApprovalOrder"), null);
                Integer deptId = safeInt(e.get("departmentId"), safeInt(e.get("serDepartmentId"), null));
                if ((level != null && level == order || intApprovalOrder != null && intApprovalOrder == order)
                        && deptId != null && deptId.equals(departmentId)) {
                    entry = e;
                    break;
                }
            }
        }
        if (entry == null) {
            for (Map<String, Object> e : approvalHistory) {
                Integer level = safeInt(e.get("level"), null);
                Integer intApprovalOrder = safeInt(e.get("intApprovalOrder"), null);
                if (level != null && level == order || intApprovalOrder != null && intApprovalOrder == order) {
                    entry = e;
                    break;
                }
            }
        }
        if (entry == null && departmentId != null) {
            for (Map<String, Object> e : approvalHistory) {
                Integer deptId = safeInt(e.get("departmentId"), safeInt(e.get("serDepartmentId"), null));
                if (deptId != null && deptId.equals(departmentId)) {
                    entry = e;
                    break;
                }
            }
        }
        if (entry == null && departmentName != null) {
            String nameLower = departmentName.toLowerCase();
            for (Map<String, Object> e : approvalHistory) {
                Object dep = e.get("departmentName");
                if (dep != null && dep.toString().toLowerCase().equals(nameLower)) {
                    entry = e;
                    break;
                }
            }
        }
        return entry;
    }

    private Integer safeInt(Object val, Integer fallback) {
        if (val == null)
            return fallback;
        try {
            if (val instanceof Integer)
                return (Integer) val;
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getFieldValue(String fieldLabel, Map<String, Object> applicationFormData,
            List<CfgTblCustomFormField> formFields) {
        if (applicationFormData == null || applicationFormData.isEmpty())
            return "";

        Map<String, String> templateKeyToConcept = new java.util.HashMap<>();
        templateKeyToConcept.put("DIVISION / DEPARTMENT", "division");
        templateKeyToConcept.put("CAPF #", "capfNumber");
        templateKeyToConcept.put("Date", "date");
        templateKeyToConcept.put("NAME OF ASSET / ITEM", "assetName");
        templateKeyToConcept.put("DETAIL SPECIFICATION", "specification");
        templateKeyToConcept.put("UTILITY & PURPOSE", "utility");
        templateKeyToConcept.put("FEASIBILITY REPORT ATTACHED", "feasibilityReport");
        templateKeyToConcept.put("IF NO THEN MENTION REASON", "reason");
        templateKeyToConcept.put("NAME", "vendorName");
        templateKeyToConcept.put("ADDRESS", "vendorAddress");
        templateKeyToConcept.put("APPROVED PRICE", "approvedPrice");
        templateKeyToConcept.put("DELIVERY PERIOD & DATE", "deliveryPeriod");
        templateKeyToConcept.put("TERMS & CONDITIONS", "termsConditions");
        templateKeyToConcept.put("Third Party assessment carried out", "thirdPartyAssessment");
        templateKeyToConcept.put("Third Party Assessment", "thirdPartyAssessment");
        templateKeyToConcept.put("Third Party assessment", "thirdPartyAssessment");

        Map<String, List<String>> fieldMappings = new java.util.HashMap<>();
        fieldMappings.put("division",
                java.util.Arrays.asList("DIVISION / DEPARTMENT", "Division", "Department", "division"));
        fieldMappings.put("capfNumber", java.util.Arrays.asList("CAPF #", "CAPF", "Capf Number", "capf_number"));
        fieldMappings.put("date", java.util.Arrays.asList("Date", "Submission Date", "date"));
        fieldMappings.put("assetName", java.util.Arrays.asList("NAME OF ASSET / ITEM", "Name of Asset", "Asset Name",
                "Item Name", "asset_name"));
        fieldMappings.put("specification",
                java.util.Arrays.asList("DETAIL SPECIFICATION", "DETAIL SPECIFICATION:", "Detail Specification",
                        "Detail Specification:", "Specification", "specification", "detail_specification",
                        "DETAIL_SPECIFICATION"));
        fieldMappings.put("utility",
                java.util.Arrays.asList("UTILITY & PURPOSE", "Utility", "Purpose", "utility_purpose"));
        fieldMappings.put("feasibilityReport",
                java.util.Arrays.asList("FEASIBILITY REPORT ATTACHED", "Feasibility Report", "feasibility_report"));
        fieldMappings.put("reason",
                java.util.Arrays.asList("IF NO THEN MENTION REASON", "Reason", "If No Reason", "reason"));
        fieldMappings.put("vendorName", java.util.Arrays.asList("Vendor Name", "Vendor", "Name of Vendor", "NAME"));
        fieldMappings.put("vendorAddress", java.util.Arrays.asList("Vendor Address", "Address", "ADDRESS"));
        fieldMappings.put("approvedPrice",
                java.util.Arrays.asList("APPROVED PRICE", "Approved Price", "Price", "Cost"));
        fieldMappings.put("deliveryPeriod",
                java.util.Arrays.asList("DELIVERY PERIOD & DATE", "Delivery Period", "Delivery Date"));
        fieldMappings.put("termsConditions",
                java.util.Arrays.asList("TERMS & CONDITIONS", "Terms and Conditions", "Terms & Conditions"));
        fieldMappings.put("thirdPartyAssessment",
                java.util.Arrays.asList("Third Party assessment carried out", "Third Party Assessment",
                        "Third Party assessment", "Third Party Assessment Carried Out", "third_party_assessment",
                        "thirdPartyAssessment"));

        String concept = templateKeyToConcept.get(fieldLabel);
        if (concept != null && fieldMappings.containsKey(concept)) {
            for (String label : fieldMappings.get(concept)) {
                String value = lookupLabel(label, applicationFormData, formFields);
                if (!value.isEmpty())
                    return value;
            }
        }

        return lookupLabel(fieldLabel, applicationFormData, formFields);
    }

    private String lookupLabel(String label, Map<String, Object> applicationFormData,
            List<CfgTblCustomFormField> formFields) {
        if (label == null)
            return "";
        String normalizedLbl = label.replace(":", "").replace(";", "").trim();
        String lowerLbl = label.toLowerCase().trim();
        String lowerNormalized = normalizedLbl.toLowerCase().trim();

        String val = getNonEmptyValue(applicationFormData, label);
        if (!val.isEmpty())
            return val;
        val = getNonEmptyValue(applicationFormData, normalizedLbl);
        if (!val.isEmpty())
            return val;

        String slug = slugify(label);
        val = getNonEmptyValue(applicationFormData, slug);
        if (!val.isEmpty())
            return val;

        String normalizedSlug = slugify(normalizedLbl);
        val = getNonEmptyValue(applicationFormData, normalizedSlug);
        if (!val.isEmpty())
            return val;

        for (Map.Entry<String, Object> entry : applicationFormData.entrySet()) {
            String key = entry.getKey();
            if (key == null)
                continue;
            if (key.toLowerCase().trim().equals(lowerLbl)) {
                return safeToString(entry.getValue());
            }
        }

        for (Map.Entry<String, Object> entry : applicationFormData.entrySet()) {
            String key = entry.getKey();
            if (key == null)
                continue;
            String keyNormalized = key.replace(":", "").replace(";", "").toLowerCase().trim();
            if (keyNormalized.equals(lowerNormalized)) {
                return safeToString(entry.getValue());
            }
        }

        if (formFields != null && !formFields.isEmpty()) {
            for (CfgTblCustomFormField field : formFields) {
                if (field == null || field.getTxtFieldLabel() == null)
                    continue;
                String fieldLabelNormalized = field.getTxtFieldLabel().replace(":", "").replace(";", "").toLowerCase()
                        .trim();
                if (fieldLabelNormalized.equals(lowerNormalized)
                        || field.getTxtFieldLabel().toLowerCase().trim().equals(lowerLbl)) {
                    String fieldSlug = slugify(field.getTxtFieldLabel());
                    val = getNonEmptyValue(applicationFormData, fieldSlug);
                    if (!val.isEmpty())
                        return val;

                    String normalizedFieldSlug = slugify(
                            field.getTxtFieldLabel().replace(":", "").replace(";", "").trim());
                    val = getNonEmptyValue(applicationFormData, normalizedFieldSlug);
                    if (!val.isEmpty())
                        return val;
                }
            }

            if (lowerNormalized.contains("specification") || lowerLbl.contains("specification")) {
                for (CfgTblCustomFormField field : formFields) {
                    if (field == null || field.getTxtFieldLabel() == null)
                        continue;
                    String fieldLabelLower = field.getTxtFieldLabel().toLowerCase();
                    if (fieldLabelLower.contains("specification") || fieldLabelLower.contains("detail")) {
                        String fieldSlug = slugify(field.getTxtFieldLabel());
                        val = getNonEmptyValue(applicationFormData, fieldSlug);
                        if (!val.isEmpty())
                            return val;
                    }
                }
            }
        }

        return "";
    }

    private String getNonEmptyValue(Map<String, Object> data, String key) {
        if (key == null || data == null)
            return "";
        Object val = data.get(key);
        if (val == null)
            return "";
        String str = safeToString(val).trim();
        return str.isEmpty() ? "" : str;
    }

    private String safeToString(Object val) {
        if (val == null)
            return "";
        return String.valueOf(val);
    }

    private String slugify(String label) {
        if (label == null)
            return "";
        String slug = label.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return slug;
    }

    private static class CapfSignatureSlot {
        String label;
        String html;
        String time;
        String metaHtml;
    }

    private static class BudgetApprover {
        Integer userId;
        String name;
        String email;
        String role;
        String signaturePath;
        String department;
        String designation;
    }

    private boolean isApprovedEntry(Map<String, Object> entry) {
        if (entry == null)
            return false;
        String action = entry.get("action") != null ? String.valueOf(entry.get("action")) : "";
        String status = entry.get("status") != null ? String.valueOf(entry.get("status")) : "";
        String state = !action.trim().isEmpty() ? action : status;
        return "APPROVED".equalsIgnoreCase(state != null ? state.trim() : "");
    }

    private Integer extractApprovalUserId(Map<String, Object> entry) {
        if (entry == null)
            return null;
        Integer id = extractUserId(entry.get("approvedBy"));
        if (id == null)
            id = extractUserId(entry.get("approverUserId"));
        if (id == null)
            id = extractUserId(entry.get("userId"));
        return id;
    }

    /**
     * Generate HTML email content with optional approve/reject buttons
     */
    private String generateApprovalEmailHtml(String recipientName, Integer level, String applicationCode,
            String formName, String status, String remarks,
            boolean showActionButtons, String approveUrl, String rejectUrl, String sendBackUrl,
            String sendBackToInitiatorUrl,
            String approvalHistoryJson, String baseUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        // Outlook-compatible styles with fallbacks
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;margin:0;padding:0;background-color:#f5f5f5}");
        html.append("table{border-collapse:collapse;mso-table-lspace:0pt;mso-table-rspace:0pt;}");
        html.append(".email-wrapper{max-width:700px;margin:0 auto;background-color:#f5f5f5;padding:15px 10px;}");
        html.append(".email-container{background-color:#ffffff;border-radius:8px;padding:25px 30px;box-shadow:0 2px 4px rgba(0,0,0,0.1);max-width:700px;width:100%;}");
        html.append(".header{background-color:#667eea;color:white;padding:20px;border-radius:8px 8px 0 0;margin:-30px -30px 20px -30px;}");
        html.append(".header h1{margin:0;font-size:24px;font-weight:600;}");
        html.append(".content{padding:20px 0;}");
        html.append(".greeting{font-size:16px;margin-bottom:20px;color:#555;}");
        html.append(".details{background-color:#f8f9fa;border-left:4px solid #667eea;padding:15px;margin:20px 0;border-radius:4px;}");
        html.append(".detail-row{margin:10px 0;}");
        html.append(".detail-label{font-weight:600;color:#555;display:inline-block;min-width:150px;}");
        html.append(".detail-value{color:#333;display:inline-block;}");
        html.append(".remarks-box{background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px;}");
        html.append(".button-container{margin:30px 0;text-align:center;}");
        html.append(".btn{display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;color:#ffffff !important;}");
        html.append(".btn-approve{background-color:#27ae60;}");
        html.append(".btn-reject{background-color:#e74c3c;}");
        html.append(".btn-sendback{background-color:#f39c12;}");
        html.append(".footer{margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;text-align:center;color:#95a5a6;font-size:12px;}");
        html.append(".status-badge{display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:600;text-transform:uppercase;}");
        html.append(".status-approved{background-color:#d5f4e6;color:#27ae60;}");
        html.append(".status-pending{background-color:#fef5e7;color:#f39c12;}");
        html.append(".status-rejected{background-color:#fadbd8;color:#e74c3c;}");
        html.append(".status-in-progress{background-color:#d6eaf8;color:#3498db;}");
        html.append(".history{margin-top:20px;}");
        html.append(".history h3{margin:0 0 10px 0;font-size:16px;color:#333;}");
        html.append(".history table{width:100%;border-collapse:collapse;font-size:12px;}");
        html.append(".history th,.history td{border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;}");
        html.append(".history th{background:#f3f4f6;font-weight:600;}");
        html.append(".sig-img{max-height:24px;max-width:100%;width:auto;height:auto;object-fit:contain;display:block;margin:0 auto 4px auto;box-sizing:border-box;}");
        html.append("</style>");
        // Outlook-specific conditional styles
        html.append("<!--[if mso]>");
        html.append("<style type='text/css'>");
        html.append(".email-wrapper{width:700px !important;}");
        html.append(".email-container{width:680px !important;}");
        html.append("</style>");
        html.append("<![endif]-->");
        html.append("</head><body>");
        
        String headerTitle = (formName != null && !formName.trim().isEmpty()) ? formName.trim() : "Application";
        
        // Table-based layout for Outlook compatibility - wider to utilize more space
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#f5f5f5;'>");
        html.append("<tr><td align='center' style='padding:15px 10px;'>");
        html.append("<table role='presentation' width='700' cellpadding='0' cellspacing='0' border='0' class='email-container' style='background-color:#ffffff;border-radius:8px;padding:25px 30px;max-width:700px;width:100%;'>");
        
        // Header
        html.append("<tr><td style='background-color:#667eea;color:#ffffff;padding:20px 30px;border-radius:8px 8px 0 0;margin:-25px -30px 20px -30px;'>");
        html.append("<h1 style='margin:0;font-size:24px;font-weight:600;color:#ffffff;'>").append(escapeHtml(headerTitle)).append("</h1>");
        html.append("</td></tr>");
        
        // Content
        html.append("<tr><td style='padding:20px 0;'>");
        html.append("<div style='font-size:16px;margin-bottom:20px;color:#555;'>Dear ").append(escapeHtml(recipientName)).append(",</div>");

        if (showActionButtons) {
            html.append("<p style='margin:0 0 15px 0;'>A new application is pending your approval at Level ").append(level).append(".</p>");
        } else {
            // Check if this is a send-back/revision case
            if ("SENT_BACK".equalsIgnoreCase(status) || "REVISION_REQUIRED".equalsIgnoreCase(status)) {
                html.append("<p style='margin:0 0 15px 0;'>Your application has been <strong>sent back for revision</strong> from Level ").append(level).append(".</p>");
                html.append("<p style='margin:0 0 15px 0;'>Please review the remarks and update your application accordingly.</p>");
            } else {
                html.append("<p style='margin:0 0 15px 0;'>Your application has been approved at Level ").append(level).append(".</p>");
            }
        }

        // Details section using table for Outlook
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#f8f9fa;border-left:4px solid #667eea;padding:15px 20px;margin:20px 0;border-radius:4px;'>");
        html.append("<tr><td style='padding:5px 0;'><span style='font-weight:600;color:#555;display:inline-block;min-width:150px;'>Application Code:</span> <span style='color:#333;'>").append(escapeHtml(applicationCode)).append("</span></td></tr>");
        html.append("<tr><td style='padding:5px 0;'><span style='font-weight:600;color:#555;display:inline-block;min-width:150px;'>Form Name:</span> <span style='color:#333;'>").append(escapeHtml(formName)).append("</span></td></tr>");
        html.append("<tr><td style='padding:5px 0;'><span style='font-weight:600;color:#555;display:inline-block;min-width:150px;'>Approval Level:</span> <span style='color:#333;'>").append(level).append("</span></td></tr>");
        String statusClass = status != null ? status.toLowerCase().replace("_", "-") : "pending";
        String statusColor = statusClass.contains("approved") ? "#27ae60" : statusClass.contains("rejected") ? "#e74c3c" : statusClass.contains("pending") ? "#f39c12" : "#3498db";
        String statusBg = statusClass.contains("approved") ? "#d5f4e6" : statusClass.contains("rejected") ? "#fadbd8" : statusClass.contains("pending") ? "#fef5e7" : "#d6eaf8";
        html.append("<tr><td style='padding:5px 0;'><span style='font-weight:600;color:#555;display:inline-block;min-width:150px;'>Status:</span> <span style='display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:600;text-transform:uppercase;background-color:").append(statusBg).append(";color:").append(statusColor).append(";'>").append(escapeHtml(status != null ? status : "PENDING")).append("</span></td></tr>");
        html.append("</table>");
        
        if (remarks != null && !remarks.trim().isEmpty()) {
            html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px 20px;margin:20px 0;border-radius:4px;'>");
            html.append("<tr><td><strong>Remarks:</strong><br>").append(escapeHtml(remarks)).append("</td></tr>");
            html.append("</table>");
        }

        String historyHtml = buildApprovalHistoryHtml(approvalHistoryJson, baseUrl);
        if (historyHtml != null && !historyHtml.trim().isEmpty()) {
            html.append(historyHtml);
        }

        boolean canApproveReject = showActionButtons && approveUrl != null && rejectUrl != null;
        boolean canSendBack = showActionButtons && sendBackUrl != null && level != null && level >= 2;
        boolean canSendBackToInitiator = showActionButtons && sendBackToInitiatorUrl != null && level != null && level >= 2;

        if (showActionButtons && (canApproveReject || canSendBack || canSendBackToInitiator)) {
            // Button container using table for Outlook
            html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:30px 0;'>");
            html.append("<tr><td align='center' style='padding:10px 20px;'>");
            if (canApproveReject) {
                html.append("<!--[if mso]>");
                html.append("<v:roundrect xmlns:v='urn:schemas-microsoft-com:vml' xmlns:w='urn:schemas-microsoft-com:office:word' href='").append(approveUrl).append("' style='height:44px;v-text-anchor:middle;width:180px;' arcsize='12%' strokecolor='#27ae60' fillcolor='#27ae60'>");
                html.append("<w:anchorlock/><center style='color:#ffffff;font-family:sans-serif;font-size:16px;font-weight:600;'>Approve Application</center>");
                html.append("</v:roundrect>");
                html.append("<![endif]-->");
                html.append("<!--[if !mso]><!-- -->");
                html.append("<a href='").append(approveUrl).append("' style='display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;background-color:#27ae60;color:#ffffff !important;'>Approve Application</a>");
                html.append("<!--<![endif]-->");
                
                html.append("<!--[if mso]>");
                html.append("<v:roundrect xmlns:v='urn:schemas-microsoft-com:vml' xmlns:w='urn:schemas-microsoft-com:office:word' href='").append(rejectUrl).append("' style='height:44px;v-text-anchor:middle;width:180px;' arcsize='12%' strokecolor='#e74c3c' fillcolor='#e74c3c'>");
                html.append("<w:anchorlock/><center style='color:#ffffff;font-family:sans-serif;font-size:16px;font-weight:600;'>Reject Application</center>");
                html.append("</v:roundrect>");
                html.append("<![endif]-->");
                html.append("<!--[if !mso]><!-- -->");
                html.append("<a href='").append(rejectUrl).append("' style='display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;background-color:#e74c3c;color:#ffffff !important;'>Reject Application</a>");
                html.append("<!--<![endif]-->");
            }
            if (canSendBack) {
                html.append("<!--[if mso]>");
                html.append("<v:roundrect xmlns:v='urn:schemas-microsoft-com:vml' xmlns:w='urn:schemas-microsoft-com:office:word' href='").append(sendBackUrl).append("' style='height:44px;v-text-anchor:middle;width:150px;' arcsize='12%' strokecolor='#f39c12' fillcolor='#f39c12'>");
                html.append("<w:anchorlock/><center style='color:#ffffff;font-family:sans-serif;font-size:16px;font-weight:600;'>Send Back</center>");
                html.append("</v:roundrect>");
                html.append("<![endif]-->");
                html.append("<!--[if !mso]><!-- -->");
                html.append("<a href='").append(sendBackUrl).append("' style='display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;background-color:#f39c12;color:#ffffff !important;'>Send Back</a>");
                html.append("<!--<![endif]-->");
            }
            if (canSendBackToInitiator) {
                html.append("<!--[if mso]>");
                html.append("<v:roundrect xmlns:v='urn:schemas-microsoft-com:vml' xmlns:w='urn:schemas-microsoft-com:office:word' href='").append(sendBackToInitiatorUrl).append("' style='height:44px;v-text-anchor:middle;width:220px;' arcsize='12%' strokecolor='#c0392b' fillcolor='#c0392b'>");
                html.append("<w:anchorlock/><center style='color:#ffffff;font-family:sans-serif;font-size:16px;font-weight:600;'>Send Back to Initiator</center>");
                html.append("</v:roundrect>");
                html.append("<![endif]-->");
                html.append("<!--[if !mso]><!-- -->");
                html.append("<a href='").append(sendBackToInitiatorUrl).append("' style='display:inline-block;padding:12px 30px;margin:5px 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;background-color:#c0392b;color:#ffffff !important;'>Send Back to Initiator</a>");
                html.append("<!--<![endif]-->");
            }
            html.append("</td></tr>");
            html.append("</table>");
            html.append("<p style='text-align:center;color:#7f8c8d;font-size:12px;margin-top:20px;'>You can also review this application in the system dashboard.</p>");
        } else {
            html.append("<p style='margin:15px 0;'>Thank you for using our system.</p>");
        }

        // Footer
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;'>");
        html.append("<tr><td align='center' style='color:#95a5a6;font-size:12px;padding:10px 0;'>");
        html.append("<p style='margin:5px 0;'>Best Regards,<br>System Administrator</p>");
        html.append("<p style='font-size:10px;color:#bdc3c7;margin:5px 0;'>This is an automated email. Please do not reply.</p>");
        html.append("</td></tr>");
        html.append("</table>");
        
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("</body></html>");

        return html.toString();
    }

    private String buildApprovalHistoryHtml(String approvalHistoryJson, String baseUrl) {
        if (approvalHistoryJson == null || approvalHistoryJson.trim().isEmpty())
            return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(approvalHistoryJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            if (list == null || list.isEmpty())
                return "";

            StringBuilder sb = new StringBuilder();
            sb.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-top:20px;'>");
            sb.append("<tr><td>");
            sb.append("<h3 style='margin:0 0 10px 0;font-size:16px;color:#333;'>Prior Approvals</h3>");
            sb.append("<table role='presentation' width='100%' cellpadding='6' cellspacing='0' border='1' style='border-collapse:collapse;font-size:12px;border:1px solid #e5e7eb;'>");
            sb.append("<thead><tr>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Level</th>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Approver</th>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Role</th>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Status</th>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Date</th>");
            sb.append("<th style='background:#f3f4f6;font-weight:600;border:1px solid #e5e7eb;padding:6px 8px;text-align:left;'>Signature</th>");
            sb.append("</tr></thead><tbody>");

            for (Map<String, Object> entry : list) {
                String level = entry.get("level") != null ? String.valueOf(entry.get("level")) : "";
                String name = entry.get("approverName") != null ? String.valueOf(entry.get("approverName")) : "";
                String role = entry.get("role") != null ? String.valueOf(entry.get("role")) : "";
                if (role == null || role.trim().isEmpty()) {
                    role = entry.get("departmentName") != null ? String.valueOf(entry.get("departmentName")) : "";
                }
                String action = entry.get("action") != null ? String.valueOf(entry.get("action"))
                        : entry.get("status") != null ? String.valueOf(entry.get("status")) : "";
                String date = entry.get("approvedDate") != null ? String.valueOf(entry.get("approvedDate")) 
                        : (entry.get("sentBackDate") != null ? String.valueOf(entry.get("sentBackDate")) : "");
                String signaturePath = entry.get("signaturePath") != null ? String.valueOf(entry.get("signaturePath"))
                        : "";
                String approvedBy = entry.get("approvedBy") != null ? String.valueOf(entry.get("approvedBy")) : "";

                String sigHtml = "";
                Integer approvedById = safeInt(approvedBy, null);
                if (!signaturePath.trim().isEmpty() && approvedBy != null && !approvedBy.trim().isEmpty()
                        && baseUrl != null) {
                    String sigUrl = baseUrl + "/getSignature?userId=" + approvedBy;
                    sigHtml = "<img src='" + sigUrl + "' alt='Signature' style='max-height:24px;max-width:100%;width:auto;height:auto;display:block;margin:0 auto 4px auto;box-sizing:border-box;' />";
                } else {
                    String inlineSignature = buildInlineSignatureDataUri(signaturePath, approvedById);
                    if (inlineSignature != null && !inlineSignature.isEmpty()) {
                        sigHtml = "<img src='" + inlineSignature + "' alt='Signature' style='max-height:24px;max-width:100%;width:auto;height:auto;display:block;margin:0 auto 4px auto;box-sizing:border-box;' />";
                    }
                }

                sb.append("<tr>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;'>").append(escapeHtml(level)).append("</td>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;'>").append(escapeHtml(name)).append("</td>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;'>").append(escapeHtml(role)).append("</td>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;'>").append(escapeHtml(action)).append("</td>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top;'>").append(escapeHtml(date)).append("</td>");
                sb.append("<td style='border:1px solid #e5e7eb;padding:6px 8px;text-align:center;vertical-align:middle;'>").append(sigHtml).append("</td>");
                sb.append("</tr>");
            }

            sb.append("</tbody></table>");
            sb.append("</td></tr>");
            sb.append("</table>");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Error building approval history HTML: " + e.getMessage(), e);
            return "";
        }
    }

    private void drawDynamicBudgetSignatureTable(PDPageContentStream content,
            float x, float y, float width, float height,
            List<Map<String, Object>> footerFields,
            String approvalHistoryJson,
            PDDocument document) throws java.io.IOException {
        int cols = footerFields != null ? footerFields.size() : 0;
        if (cols <= 0) {
            return;
        }

        float colWidth = width / cols;
        float rowSig = 50f;
        float rowHeader = 22f;
        float rowNames = height - rowSig - rowHeader;

        content.setLineWidth(0.6f);
        content.addRect(x, y, width, height);
        content.stroke();
        for (int i = 1; i < cols; i++) {
            content.moveTo(x + colWidth * i, y);
            content.lineTo(x + colWidth * i, y + height);
            content.stroke();
        }
        content.moveTo(x, y + rowSig);
        content.lineTo(x + width, y + rowSig);
        content.stroke();
        content.moveTo(x, y + rowSig + rowHeader);
        content.lineTo(x + width, y + rowSig + rowHeader);
        content.stroke();

        content.setNonStrokingColor(220, 220, 220);
        content.addRect(x, y + rowSig, width, rowHeader);
        content.fill();
        content.setNonStrokingColor(0, 0, 0);

        content.setFont(PDType1Font.HELVETICA_BOLD, 9);
        float headerY = y + rowSig + 6;
        for (int i = 0; i < cols; i++) {
            String label = footerFields.get(i) != null && footerFields.get(i).get("label") != null
                    ? String.valueOf(footerFields.get(i).get("label"))
                    : "New Field";
            drawCenteredHeader(content, label, x + colWidth * i, colWidth, headerY);
        }

        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        java.util.Set<Integer> allUserIds = new java.util.LinkedHashSet<>();
        for (Map<String, Object> field : footerFields) {
            for (Object userObj : extractFooterUsers(field)) {
                Integer uid = extractUserId(userObj);
                if (uid != null)
                    allUserIds.add(uid);
            }
        }
        Integer[] userIds = allUserIds.toArray(new Integer[0]);
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(userIds);

        float sigRowY = y + rowSig + rowHeader + 4;
        float sigRowHeight = rowNames - 8;
        for (int i = 0; i < cols; i++) {
            Map<String, Object> field = footerFields.get(i);
            String role = field != null && field.get("label") != null ? String.valueOf(field.get("label")) : "APPROVER";
            String key = field != null && field.get("key") != null ? String.valueOf(field.get("key")).toLowerCase()
                    : "";
            boolean allowFallback = "prepared_by".equals(key);
            String sigPath = findSignatureForFooterFieldUsers(approvalHistory, extractFooterUsers(field), role,
                    allowFallback, signatureFromDb);
            if (sigPath != null && !sigPath.trim().isEmpty()) {
                drawSignatureImage(document, content, sigPath, x + colWidth * i + 4, sigRowY, colWidth - 8,
                        sigRowHeight);
            }
        }

        content.setFont(PDType1Font.HELVETICA, 9);
        for (int i = 0; i < cols; i++) {
            Map<String, Object> field = footerFields.get(i);
            List<Object> users = extractFooterUsers(field);
            String usersText = formatFooterUsers(users);
            float tx = x + colWidth * i + 4;
            float ty = y + 8;
            for (String line : wrapText(usersText, PDType1Font.HELVETICA, 9, colWidth - 8)) {
                content.beginText();
                content.newLineAtOffset(tx, ty);
                content.showText(line);
                content.endText();
                ty += 10;
            }
        }
    }

    private String formatFooterUsers(List<Object> users) {
        if (users == null || users.isEmpty())
            return "--";
        List<String> chunks = new java.util.ArrayList<>();
        for (Object userObj : users) {
            Map<String, String> display = extractUserDisplay(userObj);
            String txt = formatUserDisplay(display);
            if (txt != null && !txt.trim().isEmpty()) {
                chunks.add(txt.replace("\n", " "));
            }
        }
        if (chunks.isEmpty())
            return "--";
        return String.join(", ", chunks);
    }

    private String findSignatureForFooterFieldUsers(List<Map<String, Object>> approvalHistory,
            List<Object> users,
            String role,
            boolean allowFallback,
            Map<Integer, String> signatureFromDb) {
        if (users == null || users.isEmpty())
            return null;
        for (Object userObj : users) {
            Integer uid = extractUserId(userObj);
            String sigPath = findSignatureForUser(approvalHistory, uid, role, allowFallback, signatureFromDb);
            if (sigPath != null && !sigPath.trim().isEmpty()) {
                return sigPath;
            }
        }
        return null;
    }

    private String buildInlineSignatureDataUri(String signaturePath, Integer userId) {
        try {
            String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
            String effectivePath = signaturePath;

            // Fallback to latest DB signature path if entry doesn't contain one.
            if ((effectivePath == null || effectivePath.trim().isEmpty()) && userId != null) {
                Map<Integer, String> dbPaths = loadUserSignaturePaths(new Integer[] { userId });
                if (dbPaths != null) {
                    effectivePath = dbPaths.get(userId);
                }
            }
            if (effectivePath == null || effectivePath.trim().isEmpty()) {
                return "";
            }

            File sigFile = resolveSignatureFile(rootPath, effectivePath);
            if (sigFile == null || !sigFile.exists()) {
                return "";
            }

            byte[] fileBytes;
            try (FileInputStream input = new FileInputStream(sigFile)) {
                fileBytes = StreamUtils.copyToByteArray(input);
            }
            if (fileBytes == null || fileBytes.length == 0) {
                return "";
            }

            String contentType = detectSignatureContentType(sigFile.getName());
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            log.warn("Unable to inline signature image: " + e.getMessage());
            return "";
        }
    }

    private String detectSignatureContentType(String fileName) {
        if (fileName == null)
            return "image/png";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".gif"))
            return "image/gif";
        if (lower.endsWith(".webp"))
            return "image/webp";
        return "image/png";
    }

    private String appendCapfFragment(String baseHtml, String capfFragment) {
        if (capfFragment == null || capfFragment.trim().isEmpty())
            return baseHtml;
        if (baseHtml == null || baseHtml.trim().isEmpty())
            return capfFragment;

        String fragmentHtml = capfFragment;
        String styleBlock = "";
        int styleStart = fragmentHtml.indexOf("<style>");
        int styleEnd = fragmentHtml.indexOf("</style>");
        if (styleStart >= 0 && styleEnd > styleStart) {
            styleBlock = fragmentHtml.substring(styleStart + 7, styleEnd);
            fragmentHtml = fragmentHtml.substring(0, styleStart) + fragmentHtml.substring(styleEnd + 8);
        }

        if (!styleBlock.trim().isEmpty()) {
            String styleTag = "<style>" + styleBlock + "</style>";
            String headMarker = "</head>";
            int headIdx = baseHtml.indexOf(headMarker);
            if (headIdx >= 0) {
                baseHtml = baseHtml.substring(0, headIdx) + styleTag + baseHtml.substring(headIdx);
            } else {
                baseHtml = styleTag + baseHtml;
            }
        }

        String marker = "</body>";
        int idx = baseHtml.lastIndexOf(marker);
        if (idx == -1) {
            return baseHtml + fragmentHtml;
        }
        return baseHtml.substring(0, idx) + fragmentHtml + baseHtml.substring(idx);
    }

    private byte[] renderCapfPdfToPng(byte[] pdfBytes, String approvalHistoryJson) {
        if (pdfBytes == null || pdfBytes.length == 0)
            return null;
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            // Ensure email image reflects latest approved signatures before rasterizing.
            overlayCapfSignaturesOnPdf(document, approvalHistoryJson, null);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error rendering CAPF PDF to PNG: " + e.getMessage(), e);
            return null;
        }
    }

    private void overlayCapfSignaturesOnPdf(PDDocument document, String approvalHistoryJson,
            List<Map<String, Object>> pipelines) {
        if (document == null || document.getNumberOfPages() == 0)
            return;
        try {
            PDPage page = document.getPage(0);
            PDRectangle box = page.getMediaBox();
            float pageWidth = box.getWidth();
            float pageHeight = box.getHeight();
            float margin = 26f;
            float y = pageHeight - margin;

            // Header row
            y -= 36;

            // Meta table
            float metaHeight = 32f;
            y -= (metaHeight + 14);

            // Title bar + divider
            y -= 16;
            y -= 10;
            y -= 12;

            // Fields
            y -= 14; // Division/Department
            y -= 14; // CAPF #
            y -= 14; // Date
            y -= 4;
            y -= 14; // Name of Asset
            y -= 14; // Detail Specification
            y -= 14; // Utility & Purpose
            y -= 2;
            y -= 14; // Feasibility
            y -= 14; // Reason
            y -= 2;
            y -= 14; // Note
            y -= 6; // line
            y -= 10; // title
            y -= 12;

            // Vendor section
            y -= 14; // Name
            y -= 14; // Address
            y -= 14; // Approved Price
            y -= 14; // Delivery
            y -= 14; // Terms
            y -= 2;
            y -= 14; // Third party assessment
            y -= 6; // line
            y -= 8; // spacing before signature section

            float lineStart = margin + 12;
            float lineEnd = pageWidth - margin - 12;
            Map<String, Float> anchors = findCapfAnchorsY(document);
            Float userDeptY = anchors.get("userDept");
            Float thirdPartyY = anchors.get("thirdParty");
            Float approvedByY = anchors.get("approvedBy");

            java.util.List<Float> candidates = new java.util.ArrayList<>();
            if (userDeptY != null)
                candidates.add(userDeptY + 32f);
            if (thirdPartyY != null)
                candidates.add(thirdPartyY - 48f);
            if (approvedByY != null)
                candidates.add(approvedByY + 76f);

            Float anchoredSigRowY = null;
            if (!candidates.isEmpty()) {
                candidates.sort(Float::compare);
                anchoredSigRowY = candidates.get(candidates.size() / 2); // median
                float minY = margin + 20f;
                float maxY = pageHeight - margin - 20f;
                if (anchoredSigRowY < minY)
                    anchoredSigRowY = minY;
                if (anchoredSigRowY > maxY)
                    anchoredSigRowY = maxY;
                log.info(
                        "CAPF signature log [anchor-multi]: userDeptY={}, thirdPartyY={}, approvedByY={}, candidates={}, signatureRowY={}",
                        userDeptY, thirdPartyY, approvedByY, candidates, anchoredSigRowY);
            } else {
                // Stored CAPF PDFs are often image-based; text anchors may be unavailable.
                // Use stable template-relative fallback so placement stays on signature row.
                anchoredSigRowY = pageHeight * 0.370f;
                log.warn(
                        "CAPF signature log [anchor-missing]: no anchors found, using template-ratio fallback signatureRowY={}",
                        anchoredSigRowY);
            }

            try (PDPageContentStream content = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                drawCapfSignatureImages(content, lineStart, y, lineEnd - lineStart, approvalHistoryJson, document,
                        anchoredSigRowY, pipelines);
            }
        } catch (Exception e) {
            log.warn("Error overlaying CAPF signatures: " + e.getMessage(), e);
        }
    }

    private void drawCapfSignatureImages(PDPageContentStream content, float x, float y, float width,
            String approvalHistoryJson, PDDocument document, Float anchoredSigRowY,
            List<Map<String, Object>> pipelines) throws java.io.IOException {
        float colWidth = width / 6f;
        float sigHeight = 22f;
        float sigRowY = anchoredSigRowY != null ? anchoredSigRowY : (y - sigHeight);

        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        java.util.List<Map<String, Object>> approved = new java.util.ArrayList<>();
        for (Map<String, Object> entry : approvalHistory) {
            if (isApprovedEntry(entry)) {
                approved.add(entry);
            }
        }
        log.info("CAPF signature log [overlay-images]: historyCount={}, approvedCount={}",
                approvalHistory != null ? approvalHistory.size() : 0, approved.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>>[] mapped = mapCapfApprovalEntries(approved, pipelines);

        Integer[] approvedUserIds = new Integer[approved.size()];
        for (int i = 0; i < approved.size(); i++) {
            approvedUserIds[i] = extractApprovalUserId(approved.get(i));
        }
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(approvedUserIds);
        Map<Integer, UserSignatureMeta> userMeta = loadUserSignatureMeta(approvedUserIds);

        for (int i = 0; i < 6; i++) {
            List<Map<String, Object>> entries = mapped[i];
            if (entries == null || entries.isEmpty()) continue;
            
            float slotX = x + (colWidth * i) + 4;
            float sigW = (colWidth - 8);
            if (entries.size() > 1) sigW = sigW / 2 - 2;

            for (int k = 0; k < entries.size() && k < 2; k++) {
                Map<String, Object> entry = entries.get(k);
                String sigPath = null;
                Integer approvedBy = extractApprovalUserId(entry);
                if (entry.get("signaturePath") != null
                        && !String.valueOf(entry.get("signaturePath")).trim().isEmpty()) {
                    sigPath = String.valueOf(entry.get("signaturePath"));
                } else if (approvedBy != null && signatureFromDb.containsKey(approvedBy)) {
                    sigPath = signatureFromDb.get(approvedBy);
                }

                float currentX = slotX + (k * (sigW + 4));
                if (sigPath != null && !sigPath.trim().isEmpty()) {
                    drawSignatureImage(document, content, sigPath, currentX, sigRowY, sigW, sigHeight);
                }
                
                // The underlying HTML template already renders the date, name, and department text for the CAPF form.
                // We do not need to use PDFBox to manually draw it again as it causes overlapping text issues.
                // drawSignatureMetaText(content, currentX, sigW, sigRowY, entry, approvedBy, userMeta);
            }
        }
    }

    private void drawSignatureMetaText(PDPageContentStream content, float colX, float colWidth, float sigRowY,
            Map<String, Object> entry, Integer approvedBy,
            Map<Integer, UserSignatureMeta> userMeta) throws java.io.IOException {
        String dateText = formatApprovalDate(entry != null ? entry.get("approvedDate") : null);
        UserSignatureMeta meta = approvedBy != null ? userMeta.get(approvedBy) : null;
        String name = meta != null && meta.userName != null ? meta.userName
                : (entry != null && entry.get("approverName") != null ? String.valueOf(entry.get("approverName")) : "");
        String designation = meta != null && meta.designation != null ? meta.designation
                : (entry != null && entry.get("txtDesignation") != null ? String.valueOf(entry.get("txtDesignation"))
                        : (entry != null && entry.get("designation") != null ? String.valueOf(entry.get("designation"))
                                : ""));
        String department = meta != null && meta.departmentName != null ? meta.departmentName
                : (entry != null && entry.get("departmentName") != null ? String.valueOf(entry.get("departmentName"))
                        : (entry != null && entry.get("txtDepartmentName") != null
                                ? String.valueOf(entry.get("txtDepartmentName"))
                                : ""));

        // Keep metadata compact and high enough so it stays above printed slot labels.
        float metaFont = 6.0f;
        float dateY = sigRowY - 4.5f;
        float nameY = dateY - 6.5f;
        float desigY = nameY - 6.5f;
        float deptY = desigY - 6.5f;
        float maxW = colWidth - 6f;

        content.setFont(PDType1Font.HELVETICA, metaFont);
        if (dateText != null && !dateText.trim().isEmpty()) {
            String line = firstWrappedLine(dateText, PDType1Font.HELVETICA, metaFont, maxW);
            drawCenteredMetaLine(content, line, PDType1Font.HELVETICA, metaFont, colX, colWidth, dateY);
        }
        if (name != null && !name.trim().isEmpty()) {
            String line = firstWrappedLine(name, PDType1Font.HELVETICA, metaFont, maxW);
            drawCenteredMetaLine(content, line, PDType1Font.HELVETICA, metaFont, colX, colWidth, nameY);
        }
        if (designation != null && !designation.trim().isEmpty()) {
            String line = firstWrappedLine(designation, PDType1Font.HELVETICA, metaFont, maxW);
            drawCenteredMetaLine(content, line, PDType1Font.HELVETICA, metaFont, colX, colWidth, desigY);
        }
        if (department != null && !department.trim().isEmpty()) {
            String line = firstWrappedLine(department, PDType1Font.HELVETICA, metaFont, maxW);
            drawCenteredMetaLine(content, line, PDType1Font.HELVETICA, metaFont, colX, colWidth, deptY);
        }
    }

    private String firstWrappedLine(String text, PDType1Font font, float fontSize, float maxWidth) {
        if (text == null)
            return "";
        try {
            java.util.List<String> lines = wrapText(text, font, fontSize, maxWidth);
            if (lines == null || lines.isEmpty())
                return text;
            return lines.get(0);
        } catch (Exception e) {
            return text.length() > 60 ? text.substring(0, 60) : text;
        }
    }

    private void drawCenteredMetaLine(PDPageContentStream content, String text, PDType1Font font, float fontSize,
            float colX, float colWidth, float y) throws java.io.IOException {
        if (text == null || text.trim().isEmpty())
            return;
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float x = colX + (colWidth - textWidth) / 2f;
        if (x < colX + 2f)
            x = colX + 2f;
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String formatApprovalDateTime(Object raw) {
        if (raw == null)
            return "";
        try {
            String s = String.valueOf(raw).trim();
            if (s.isEmpty())
                return "";
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
                String normalized = s.length() >= 19 ? s.substring(0, 19).replace('T', ' ') : s.replace('T', ' ');
                try {
                    return out.format(in.parse(normalized));
                } catch (Exception ignore) {
                    java.text.SimpleDateFormat inDate = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date d = inDate.parse(s.substring(0, 10));
                    return new java.text.SimpleDateFormat("dd/MM/yyyy").format(d);
                }
            }
            if (s.matches("^\\d{2}/\\d{2}/\\d{4}.*"))
                return s;
        } catch (Exception ignored) {
        }
        return String.valueOf(raw);
    }

    private Map<Integer, UserSignatureMeta> loadUserSignatureMeta(Integer[] userIds) {
        Map<Integer, UserSignatureMeta> map = new java.util.HashMap<>();
        if (userIds == null || userIds.length == 0)
            return map;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            for (Integer id : userIds) {
                if (id == null || map.containsKey(id))
                    continue;
                CfgTblUser u = em.find(CfgTblUser.class, id);
                if (u != null) {
                    UserSignatureMeta m = new UserSignatureMeta();
                    m.userName = u.getTxtUserName();
                    m.designation = u.getTxtDesignation();
                    if (u.getHrTblDepartment() != null && u.getHrTblDepartment().getTxtDepartmentName() != null) {
                        m.departmentName = u.getHrTblDepartment().getTxtDepartmentName();
                    } else if (u.getTxtDepartmentName() != null) {
                        m.departmentName = u.getTxtDepartmentName();
                    }
                    map.put(id, m);
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            log.warn("Error loading user signature meta: " + e.getMessage(), e);
        } finally {
            if (em.isOpen())
                em.close();
        }
        return map;
    }

    private static class UserSignatureMeta {
        String userName;
        String designation;
        String departmentName;
    }

    private Map<String, Float> findCapfAnchorsY(PDDocument document) {
        Map<String, Float> out = new java.util.HashMap<>();
        if (document == null || document.getNumberOfPages() == 0)
            return out;
        try {
            CapfAnchorStripper stripper = new CapfAnchorStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            stripper.getText(document);
            out.put("userDept", stripper.getAnchorY("userDept"));
            out.put("thirdParty", stripper.getAnchorY("thirdParty"));
            out.put("approvedBy", stripper.getAnchorY("approvedBy"));
            return out;
        } catch (Exception e) {
            log.warn("CAPF signature log [anchor-error]: {}", e.getMessage());
            return out;
        }
    }

    private static class CapfAnchorStripper extends PDFTextStripper {
        private final Map<String, java.util.List<Float>> anchors = new java.util.HashMap<>();

        CapfAnchorStripper() throws java.io.IOException {
            super();
        }

        Float getAnchorY(String key) {
            java.util.List<Float> ys = anchors.get(key);
            if (ys == null || ys.isEmpty())
                return null;
            // Use the lowest occurrence on page (closest to signature block in this form).
            return ys.stream().min(Float::compareTo).orElse(null);
        }

        @Override
        protected void writeString(String text, java.util.List<TextPosition> textPositions) throws java.io.IOException {
            if (text != null && textPositions != null && !textPositions.isEmpty()) {
                String lower = text.toLowerCase();
                float pageHeight = getCurrentPage().getMediaBox().getHeight();
                float yFromBottom = pageHeight - textPositions.get(0).getYDirAdj();

                if ((lower.contains("user dept") || lower.contains("user deptt")) && lower.contains("hod")) {
                    anchors.computeIfAbsent("userDept", k -> new java.util.ArrayList<>()).add(yFromBottom);
                }
                if (lower.contains("third party assessment")) {
                    anchors.computeIfAbsent("thirdParty", k -> new java.util.ArrayList<>()).add(yFromBottom);
                }
                if (lower.contains("approved by")) {
                    anchors.computeIfAbsent("approvedBy", k -> new java.util.ArrayList<>()).add(yFromBottom);
                }
            }
            super.writeString(text, textPositions);
        }
    }

    private byte[] getOrBuildCapfPdf(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        if (application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
            return application.getBlbPdfData();
        }
        try {
            Map<String, Object> appData = parseApplicationData(application);
            return generateApplicationPdf(application, form, appData);
        } catch (Exception e) {
            log.warn("Error generating CAPF PDF for inline image: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] renderPdfToPng(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0)
            return null;
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error rendering PDF to PNG: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] renderPdfFirstPageToPng(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0)
            return null;
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error rendering PDF first page to PNG: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] buildCapfPreviewPng(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        // Prefer stored PDF because it is produced by the same application-details
        // rendering flow.
        if (application != null && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
            log.info("CAPF preview source: stored blbPdfData ({} bytes) for appId={}",
                    application.getBlbPdfData().length, application.getSerApplicationId());
            byte[] storedPng = renderPdfFirstPageToPng(application.getBlbPdfData());
            if (storedPng != null && storedPng.length > 0) {
                return storedPng;
            }
            log.warn("CAPF preview source stored blbPdfData failed to render PNG, trying generated CAPF PDF");
        }
        try {
            Map<String, Object> appData = parseApplicationData(application);
            byte[] capfPdf = generateCapfPdf(application, form, appData);
            if (capfPdf != null && capfPdf.length > 0) {
                log.info("CAPF preview source: generated CAPF PDF ({} bytes) for appId={}",
                        capfPdf.length, application != null ? application.getSerApplicationId() : null);
                return renderPdfFirstPageToPng(capfPdf);
            }
        } catch (Exception e) {
            log.warn("Error building CAPF preview PNG from generated PDF: " + e.getMessage(), e);
        }
        // Safe fallback to previous path if generation fails
        return renderCapfPdfToPng(getOrBuildCapfPdf(application, form), application.getTxtApprovalHistory());
    }

    private void persistCapfSignedPdf(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        if (application == null)
            return;
        try {
            byte[] basePdf = application.getBlbPdfData();
            if (basePdf == null || basePdf.length == 0) {
                Map<String, Object> appData = parseApplicationData(application);
                basePdf = generateCapfPdf(application, form, appData);
            }
            if (basePdf == null || basePdf.length == 0) {
                log.warn("CAPF signed PDF persist skipped: no base PDF for appId={}",
                        application.getSerApplicationId());
                return;
            }

            byte[] signedPdf = applyCapfSignaturesToPdf(basePdf, application.getTxtApprovalHistory(),
                    loadApprovalPipeline(form));
            if (signedPdf != null && signedPdf.length > 0) {
                String code = application.getTxtFormCode() != null ? application.getTxtFormCode() : "application";
                application.setBlbPdfData(signedPdf);
                application.setTxtPdfName(buildPdfFileName(form, code));
                application.setTxtPdfMime("application/pdf");
                log.info("CAPF signed PDF persisted: appId={}, bytes={}", application.getSerApplicationId(),
                        signedPdf.length);
            } else {
                log.warn("CAPF signed PDF persist failed to produce output for appId={}",
                        application.getSerApplicationId());
            }
        } catch (Exception e) {
            log.warn("Error persisting CAPF signed PDF for appId={}: {}", application.getSerApplicationId(),
                    e.getMessage(), e);
        }
    }

    private byte[] applyCapfSignaturesToPdf(byte[] pdfBytes, String approvalHistoryJson,
            List<Map<String, Object>> pipelines) {
        if (pdfBytes == null || pdfBytes.length == 0)
            return null;
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            overlayCapfSignaturesOnPdf(document, approvalHistoryJson, pipelines);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error applying CAPF signatures to PDF: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] applyDynamicFooterSignaturesToPdf(byte[] pdfBytes, Map<String, Object> appData,
            String approvalHistoryJson) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }
        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        if (footerFields == null || footerFields.isEmpty()) {
            return pdfBytes;
        }
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            if (document.getNumberOfPages() <= 0) {
                return pdfBytes;
            }
            PDPage firstPage = document.getPage(0);
            float pageWidth = firstPage.getMediaBox().getWidth();
            float margin = 40f;
            float tableBottomY = 60f;
            float tableHeight = 110f;
            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    firstPage,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true)) {
                drawDynamicFooterSignaturesOnly(
                        content,
                        margin,
                        tableBottomY,
                        pageWidth - margin * 2,
                        tableHeight,
                        footerFields,
                        approvalHistoryJson,
                        document);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Error applying dynamic footer signatures: " + e.getMessage(), e);
            return pdfBytes;
        }
    }

    private void drawDynamicFooterSignaturesOnly(PDPageContentStream content,
            float x, float y, float width, float height,
            List<Map<String, Object>> footerFields,
            String approvalHistoryJson,
            PDDocument document) throws java.io.IOException {
        int sections = footerFields != null ? footerFields.size() : 0;
        if (sections <= 0) {
            return;
        }
        List<List<Object>> sectionSlots = new java.util.ArrayList<>();
        int totalSlots = 0;
        for (int i = 0; i < sections; i++) {
            Map<String, Object> field = footerFields.get(i);
            List<Object> users = extractFooterUsers(field);
            if (users == null || users.isEmpty()) {
                users = new java.util.ArrayList<>();
                users.add(null); // keep section visible as one slot
            }
            sectionSlots.add(users);
            totalSlots += users.size();
        }
        if (totalSlots <= 0) {
            return;
        }
        float colWidth = width / totalSlots;
        float rowSig = 50f;
        float rowHeader = 22f;
        float rowNames = height - rowSig - rowHeader;

        List<Map<String, Object>> approvalHistory = parseApprovalHistory(approvalHistoryJson);
        java.util.Set<Integer> allUserIds = new java.util.LinkedHashSet<>();
        for (Map<String, Object> field : footerFields) {
            for (Object userObj : extractFooterUsers(field)) {
                Integer uid = extractUserId(userObj);
                if (uid != null)
                    allUserIds.add(uid);
            }
        }
        Integer[] userIds = allUserIds.toArray(new Integer[0]);
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(userIds);

        float sigRowY = y + rowSig + rowHeader + 4;
        float sigRowHeight = rowNames - 8;
        int slotIndex = 0;
        for (int i = 0; i < sections; i++) {
            Map<String, Object> field = footerFields.get(i);
            String role = field != null && field.get("label") != null ? String.valueOf(field.get("label")) : "APPROVER";
            List<Object> users = sectionSlots.get(i);
            for (Object userObj : users) {
                Integer uid = extractUserId(userObj);
                String sigPath = "";
                if (uid != null) {
                    // Strict match by approved user and section label; no fallback to avoid
                    // placing signatures in wrong person's slot.
                    sigPath = findSignatureForUser(approvalHistory, uid, role, false, signatureFromDb);
                }
                if (sigPath != null && !sigPath.trim().isEmpty()) {
                    float cellX = x + colWidth * slotIndex;
                    drawSignatureImage(document, content, sigPath, cellX + 4, sigRowY, colWidth - 8, sigRowHeight);
                }
                slotIndex++;
            }
        }
    }

    private List<Map<String, Object>> loadApprovalPipeline(CfgTblCustomForm form) {
        if (form == null || form.getTxtApprovalPipeline() == null || form.getTxtApprovalPipeline().trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> pipelines = mapper.readValue(form.getTxtApprovalPipeline(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return pipelines != null ? pipelines : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.warn("Error parsing CAPF approval pipeline for signature mapping: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    private String appendCapfInlineImage(String baseHtml, String imageCid) {
        if (baseHtml == null || baseHtml.trim().isEmpty())
            return baseHtml;
        String cid = imageCid != null ? imageCid : "capf-inline";
        String fragment = "<div style='margin:20px 0 0 0;text-align:center;'>" +
                "<img src='cid:" + cid
                + "' style='width:100%;max-width:820px;border:1px solid #222;display:block;margin:0 auto;' alt='CAPF Form' />"
                +
                "</div>";
        String marker = "</body>";
        int idx = baseHtml.lastIndexOf(marker);
        if (idx == -1) {
            return baseHtml + fragment;
        }
        return baseHtml.substring(0, idx) + fragment + baseHtml.substring(idx);
    }

    private String appendInlinePdfImage(String baseHtml, String imageCid, String altText) {
        if (baseHtml == null || baseHtml.trim().isEmpty())
            return baseHtml;
        String cid = imageCid != null ? imageCid : "pdf-inline";
        String alt = altText != null ? altText : "Document";
        String fragment = "<div style='margin:20px 0 0 0;text-align:center;'>" +
                "<img src='cid:" + cid
                + "' style='width:100%;max-width:820px;display:block;margin:0 auto;' alt='"
                + escapeHtml(alt) + "' />" +
                "</div>";
        String marker = "</body>";
        int idx = baseHtml.lastIndexOf(marker);
        if (idx == -1) {
            return baseHtml + fragment;
        }
        return baseHtml.substring(0, idx) + fragment + baseHtml.substring(idx);
    }

    private String getResolvedFormName(CfgTblCustomForm form) {
        if (form != null && form.getTxtFormName() != null && !form.getTxtFormName().trim().isEmpty()) {
            return form.getTxtFormName().trim();
        }
        return "Application Form";
    }

    private String buildPdfFileName(CfgTblCustomForm form, String fallbackCode) {
        String base = getResolvedFormName(form);
        if (base == null || base.trim().isEmpty()) {
            base = (fallbackCode != null && !fallbackCode.trim().isEmpty()) ? fallbackCode.trim() : "application";
        }
        String safe = base.replaceAll("[^a-zA-Z0-9._ -]", "").trim();
        if (safe.isEmpty()) {
            safe = "application";
        }
        return safe + ".pdf";
    }

    private byte[] resolveBestPdfBytesForEmail(CfgTblCustomFormApplication application, CfgTblCustomForm form) {
        try {
            // Check if this is an individual pipeline footer form
            Map<String, Object> appData = parseApplicationData(application);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;
            
            // ALWAYS generate fresh PDF for email preview if it is a Budget or CAPF form
            // to ensure latest data and signatures are visible.
            if (isBudgetApproval || isCapfForm(form)) {
                return generateApplicationPdf(application, form, appData != null ? appData : new java.util.HashMap<>());
            }

            // For individual pipeline footer forms, use the stored PDF if available
            // These PDFs are generated by the frontend and have the proper formatting
            if (hasDynamicFooterFlow) {
                // For general forms with individual pipeline footer: use original PDF without applying signatures
                // This keeps the email layout independent from web approval updates
                // For budget approval forms: apply signatures to show current approval state
                if (application != null && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                    if (isBudgetApproval) {
                        // For budget approval forms, apply updated signatures
                        try {
                            byte[] signedPdf = applyDynamicFooterSignaturesToPdf(
                                    application.getBlbPdfData(),
                                    appData,
                                    application.getTxtApprovalHistory());
                            if (signedPdf != null && signedPdf.length > 0) {
                                return signedPdf;
                            }
                        } catch (Exception e) {
                            log.warn("Error applying signatures to PDF for email, using original: " + e.getMessage());
                        }
                    }
                    // For general forms with individual pipeline footer, return original PDF as-is
                    return application.getBlbPdfData();
                }
                
                // If PDF doesn't exist, try to get it from database
                CfgTblCustomFormApplication dbApp = null;
                EntityManager em = getEntityManager();
                try {
                    if (application != null && application.getSerApplicationId() != null) {
                        dbApp = em.find(CfgTblCustomFormApplication.class, application.getSerApplicationId());
                        if (dbApp != null && dbApp.getBlbPdfData() != null && dbApp.getBlbPdfData().length > 0) {
                            if (isBudgetApproval) {
                                // For budget approval forms, apply updated signatures
                                try {
                                    byte[] signedPdf = applyDynamicFooterSignaturesToPdf(
                                            dbApp.getBlbPdfData(),
                                            appData,
                                            application.getTxtApprovalHistory());
                                    if (signedPdf != null && signedPdf.length > 0) {
                                        return signedPdf;
                                    }
                                } catch (Exception e) {
                                    log.warn("Error applying signatures to database PDF for email, using original: " + e.getMessage());
                                }
                            }
                            // For general forms with individual pipeline footer, return original PDF as-is
                            return dbApp.getBlbPdfData();
                        }
                    }
                } finally {
                    if (em.isOpen()) {
                        em.close();
                    }
                }
                
                // If still no PDF, don't generate a summary - return null so email sends without preview
                // The frontend should regenerate the PDF when needed
                log.warn("No PDF available for individual pipeline footer form in email, appId={}", 
                    application != null ? application.getSerApplicationId() : "null");
                return null;
            }

            if (application != null && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                return application.getBlbPdfData();
            }

            CfgTblCustomFormApplication dbApp = null;
            EntityManager em = getEntityManager();
            try {
                if (application != null && application.getSerApplicationId() != null) {
                    dbApp = em.find(CfgTblCustomFormApplication.class, application.getSerApplicationId());
                }
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }

            if (dbApp != null && dbApp.getBlbPdfData() != null && dbApp.getBlbPdfData().length > 0) {
                return dbApp.getBlbPdfData();
            }

            CfgTblCustomForm useForm = form;
            if (useForm == null && dbApp != null) {
                useForm = dbApp.getCfgTblCustomForm();
            }
            if (useForm == null && application != null) {
                useForm = application.getCfgTblCustomForm();
            }

            // Reuse appData if already parsed, otherwise parse from dbApp or application
            if (appData == null) {
                if (dbApp != null) {
                    appData = parseApplicationData(dbApp);
                } else if (application != null) {
                    appData = parseApplicationData(application);
                }
            }
            CfgTblCustomFormApplication src = dbApp != null ? dbApp : application;
            if (src != null) {
                return generateApplicationPdf(src, useForm, appData != null ? appData : new java.util.HashMap<>());
            }
        } catch (Exception e) {
            log.warn("Could not resolve PDF bytes for inline email preview: {}", e.getMessage());
        }
        return null;
    }

    private List<com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService.EmailAttachment> extractEmailAttachmentsFromAppData(Map<String, Object> appData) {
        List<com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService.EmailAttachment> emailAttachments = new java.util.ArrayList<>();
        if (appData == null) return emailAttachments;

        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toLowerCase() : "";
            if (key.contains("quotation") || key.contains("feasibility")) {
                Object val = entry.getValue();
                if (val instanceof List<?>) {
                    List<?> attachments = (List<?>) val;
                    int i = 1;
                    for (Object att : attachments) {
                        try {
                            if (att instanceof Map<?, ?>) {
                                Map<?, ?> m = (Map<?, ?>) att;
                                String name = stringFirst(m.get("fileName"), m.get("name"), m.get("originalName"), m.get("filename"), m.get("title"));
                                String mimeHint = stringFirst(m.get("mimeType"), m.get("type"));
                                
                                String base64 = stringFirst(m.get("base64"), m.get("data"), m.get("content"), m.get("fileBase64"), m.get("fileData"));
                                
                                if (base64 == null) {
                                    Object dataUrlObj = m.get("dataUrl");
                                    if (dataUrlObj instanceof String && ((String) dataUrlObj).startsWith("data:")) {
                                        base64 = (String) dataUrlObj;
                                    }
                                }
                                
                                if (name == null || name.trim().isEmpty()) {
                                    name = "attachment_" + i;
                                }
                                
                                if (base64 != null) {
                                    if (base64.startsWith("data:")) {
                                        int commaIndex = base64.indexOf(',');
                                        if (commaIndex != -1) {
                                            if (mimeHint == null || mimeHint.isEmpty()) {
                                                int semiIndex = base64.indexOf(';');
                                                if (semiIndex != -1 && semiIndex > 5) {
                                                    mimeHint = base64.substring(5, semiIndex);
                                                }
                                            }
                                            base64 = base64.substring(commaIndex + 1);
                                        }
                                    }
                                    
                                    try {
                                        byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                                        emailAttachments.add(new com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService.EmailAttachment(bytes, name, mimeHint != null && !mimeHint.isEmpty() ? mimeHint : "application/octet-stream"));
                                    } catch (IllegalArgumentException ex) {
                                        log.warn("Invalid base64 string for attachment: {}", name);
                                    }
                                }
                            }
                        } catch(Exception e) {
                            log.warn("Failed to extract attachment from appData: {}", e.getMessage());
                        }
                        i++;
                    }
                }
            }
        }
        return emailAttachments;
    }

    private void sendEmailWithInlineFormPreview(List<String> recipients, String subject, String html,
            CfgTblCustomFormApplication application, CfgTblCustomForm form, boolean isCapf, String imageCid) {
        try {
            String cidBase = imageCid != null ? imageCid : (isCapf ? "capf-inline" : "form-inline");
            String appIdPart = application != null && application.getSerApplicationId() != null
                    ? String.valueOf(application.getSerApplicationId())
                    : "na";
            String cid = cidBase + "-" + appIdPart + "-" + System.currentTimeMillis();

            List<com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService.EmailAttachment> attachments = new java.util.ArrayList<>();
            if (isCapf && application != null) {
                try {
                    Map<String, Object> appData = parseApplicationData(application);
                    attachments = extractEmailAttachmentsFromAppData(appData);
                } catch (Exception e) {
                    log.warn("Failed to extract application attachments: {}", e.getMessage());
                }
            }

            if (isCapf) {
                byte[] imageBytes = buildCapfPreviewPng(application, form);
                if (imageBytes != null && imageBytes.length > 0) {
                    String htmlWithImage = appendCapfInlineImage(html, cid);
                    if (attachments.isEmpty()) {
                        emailService.sendHtmlEmailWithInlineImage(recipients, subject, htmlWithImage, imageBytes,
                                "image/png",
                                cid);
                    } else {
                        emailService.sendHtmlEmailWithInlineImageAndAttachments(recipients, subject, htmlWithImage, imageBytes,
                                "image/png",
                                cid, attachments);
                    }
                    return;
                }
            } else {
                byte[] pdfBytes = resolveBestPdfBytesForEmail(application, form);
                byte[] imageBytes = renderPdfToPng(pdfBytes);
                if (imageBytes != null && imageBytes.length > 0) {
                    String formTitle = getResolvedFormName(form) + " Form";
                    String htmlWithImage = appendInlinePdfImage(html, cid, formTitle);
                    emailService.sendHtmlEmailWithInlineImage(recipients, subject, htmlWithImage, imageBytes,
                            "image/png",
                            cid);
                    return;
                }
            }
            emailService.sendHtmlEmail(recipients, subject, html);
        } catch (Exception e) {
            log.warn("Inline preview email failed, fallback to HTML only: {}", e.getMessage());
            emailService.sendHtmlEmail(recipients, subject, html);
        }
    }

    private String buildQuotationAttachmentHtml(Map<String, Object> appData) {
        if (appData == null)
            return "";
        Object val = appData.get("quotation_attachments");
        if (!(val instanceof List<?>))
            return "";
        List<?> attachments = (List<?>) val;
        if (attachments.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style='margin-top:14px;padding:12px;border:1px solid #e5e7eb;border-radius:6px;background:#fafafa;'>");
        sb.append("<div style='font-weight:600;margin-bottom:6px;'>Quotation Attachments</div>");
        sb.append("<ul style='margin:0;padding-left:18px;font-size:13px;'>");
        int idx = 1;
        for (Object att : attachments) {
            String name = null;
            String link = null;
            String mimeHint = null;
            if (att instanceof Map<?, ?>) {
                Map<?, ?> m = (Map<?, ?>) att;
                name = stringFirst(m.get("fileName"), m.get("name"), m.get("originalName"), m.get("filename"),
                        m.get("title"));
                mimeHint = stringFirst(m.get("mimeType"), m.get("type"));
                Object urlObj = m.get("url");
                if (urlObj instanceof String && ((String) urlObj).trim().toLowerCase().startsWith("http")) {
                    link = ((String) urlObj).trim();
                } else {
                    Object dataUrlObj = m.get("dataUrl");
                    if (dataUrlObj instanceof String) {
                        link = ((String) dataUrlObj).trim();
                    } else {
                        Object content = stringFirst(m.get("base64"), m.get("data"), m.get("content"),
                                m.get("fileBase64"), m.get("fileData"));
                        if (content instanceof String && ((String) content).length() > 40) {
                            String mime = mimeHint != null ? mimeHint : "image/png";
                            link = "data:" + htmlEscape(mime) + ";base64," + content;
                        }
                    }
                }
            } else if (att instanceof String) {
                String s = ((String) att).trim();
                name = s;
                if (s.toLowerCase().startsWith("http")) {
                    link = s;
                }
            }
            if (name == null || name.trim().isEmpty()) {
                name = "Attachment " + idx;
            }
            sb.append("<li>");
            boolean isImageLink = link != null && (link.startsWith("data:image")
                    || link.toLowerCase().matches("(?i).+\\.(png|jpe?g|gif|webp|bmp)$")
                    || (mimeHint != null && mimeHint.toLowerCase().startsWith("image")));

            if (link != null && isImageLink) {
                sb.append("<div style='margin:6px 0;'>")
                        .append("<div style='font-weight:500;'>").append(htmlEscape(name)).append("</div>")
                        .append("<img src='").append(htmlEscape(link))
                        .append("' style='max-width:480px;border:1px solid #e5e7eb;border-radius:4px;padding:4px;margin-top:4px;'>")
                        .append("</div>");
            } else if (link != null) {
                sb.append("<a href='").append(htmlEscape(link)).append("' target='_blank' rel='noopener'>")
                        .append(htmlEscape(name)).append("</a>");
            } else {
                sb.append(htmlEscape(name));
            }
            sb.append("</li>");
            idx++;
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private String stringFirst(Object... objs) {
        if (objs == null)
            return null;
        for (Object o : objs) {
            if (o instanceof String && !((String) o).trim().isEmpty()) {
                return ((String) o).trim();
            }
        }
        return null;
    }

    private String htmlEscape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Integer findFirstUserIdByRole(EntityManager entityManager, String roleName) {
        if (entityManager == null || roleName == null)
            return null;
        try {
            return (Integer) entityManager
                    .createQuery(
                            "SELECT u.serUserId FROM com.bezkoder.spring.login.admin.dal.entities.CfgTblUser u "
                                    + "WHERE (u.blIsDeleted = false OR u.blIsDeleted IS NULL) "
                                    + "AND UPPER(u.cfgTblRole.txtRoleName) = :roleName "
                                    + "ORDER BY u.serUserId ASC")
                    .setParameter("roleName", roleName.trim().toUpperCase())
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("findFirstUserIdByRole failed for role {}: {}", roleName, e.getMessage());
            return null;
        }
    }


    /**
     * Send email notification when an application is sent back to previous department
     * Uses the same format as approval emails with approve/reject buttons
     */
    private void sendBackEmailNotification(CfgTblCustomFormApplication application, Integer oldLevelBeforeSendBack, 
            Integer newLevelAfterSendBack, 
            List<java.util.Map<String, Object>> pipelines) {
        if (application == null || newLevelAfterSendBack == null) {
            log.info("Skipping send-back email: appId={}, newLevelAfterSendBack={}", 
                application != null ? application.getSerApplicationId() : "null", newLevelAfterSendBack);
            return;
        }
        
        EntityManager emailEntityManager = getEntityManager();
        try {
            // Get the form
            CfgTblCustomForm form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            String formName = form != null ? form.getTxtFormName() : "Application";
            
            // Check if this is an individual pipeline footer form
            Map<String, Object> appData = parseApplicationData(application);
            boolean hasDynamicFooterFlow = hasDynamicFooterFlow(application);
            boolean isBudgetApproval = isBudgetApprovalForm(form);
            boolean useIndividualPipelineFlow = isBudgetApproval || hasDynamicFooterFlow;
            
            // For individual pipeline footer forms, send email to the approver at the previous sequence index
            if (useIndividualPipelineFlow && newLevelAfterSendBack >= 0) {
                List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, emailEntityManager);
                if (newLevelAfterSendBack < sequence.size()) {
                    BudgetApprover previousApprover = sequence.get(newLevelAfterSendBack);
                    if (previousApprover != null && previousApprover.email != null && !previousApprover.email.trim().isEmpty()) {
                        String baseUrl = getBaseUrl();
                        String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId="
                                + application.getSerApplicationId() + "&userId=" + previousApprover.userId;
                        String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId="
                                + application.getSerApplicationId() + "&userId=" + previousApprover.userId;
                        String sendBackUrl = null;
                        String sendBackToInitiatorUrl = null;
                        
                        boolean isCapf = isCapfForm(form);
                        String cid = isCapf ? "capf-inline" : "form-inline";
                        
                        String subject = formName + " Sent Back - Requires Your Approval - " + 
                            (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        
                        String html = generateApprovalEmailHtml(
                            previousApprover.name != null ? previousApprover.name : "User",
                            newLevelAfterSendBack + 1, // level (Stage number, 1-indexed)
                            application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                            formName,
                            "IN_PROGRESS", // status - showing it's in progress after send back
                            application.getTxtRemarks(),
                            true, // showActionButtons
                            approveUrl,
                            rejectUrl,
                            sendBackUrl,
                            sendBackToInitiatorUrl,
                            application.getTxtApprovalHistory(),
                            baseUrl
                        );
                        
                        sendEmailWithInlineFormPreview(
                            java.util.Arrays.asList(previousApprover.email),
                            subject,
                            html,
                            application,
                            form,
                            isCapf,
                            cid
                        );
                        
                        log.info("Send-back notification email sent to previous approver (individual pipeline): {} at sequence index {}", 
                            previousApprover.email, newLevelAfterSendBack);
                        
                        // Also notify the submitter
                        if (application.getSerSubmittedBy() != null) {
                            try {
                                emailEntityManager.getTransaction().begin();
                                CfgTblUser submitter = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                                if (submitter != null && submitter.getTxtAddress() != null && !submitter.getTxtAddress().trim().isEmpty()) {
                                    String submitterSubject = formName + " Requires Revision - " + 
                                        (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                                    
                                    String submitterHtml = generateApprovalEmailHtml(
                                        submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User",
                                        (oldLevelBeforeSendBack != null ? oldLevelBeforeSendBack + 1 : 0),
                                        application.getTxtFormCode(),
                                        formName,
                                        "SENT_BACK",
                                        application.getTxtRemarks(),
                                        false, // showActionButtons
                                        null, null, null, null,
                                        application.getTxtApprovalHistory(),
                                        baseUrl
                                    );
                                    
                                    sendEmailWithInlineFormPreview(
                                        java.util.Arrays.asList(submitter.getTxtAddress()),
                                        submitterSubject,
                                        submitterHtml,
                                        application,
                                        form,
                                        isCapf,
                                        cid
                                    );
                                    log.info("Send-back notification email sent to submitter: " + submitter.getTxtAddress());
                                }
                                emailEntityManager.getTransaction().commit();
                            } catch (Exception e) {
                                if (emailEntityManager.getTransaction().isActive()) {
                                    emailEntityManager.getTransaction().rollback();
                                }
                                log.warn("Failed to send send-back email to submitter: " + e.getMessage());
                            }
                        }
                        
                        return; // Exit early for individual pipeline footer forms
                    }
                }
            }
            
            // For regular pipeline forms, use the existing department-based logic
            if (pipelines == null || pipelines.isEmpty()) {
                log.info("No pipelines to notify for send-back, appId={}", application.getSerApplicationId());
                return;
            }
            
            if (newLevelAfterSendBack < 0) {
                log.info("Skipping send-back email: newLevelAfterSendBack={} is negative", newLevelAfterSendBack);
                return;
            }
            
            // Log for debugging
            log.info("=== SEND BACK EMAIL DEBUG ===");
            log.info("appId={}, newLevelAfterSendBack={}, pipelines.size={}", 
                application.getSerApplicationId(), newLevelAfterSendBack, pipelines.size());
            
            // newLevelAfterSendBack is 0-based index of the new level (the level it's going back to)
            // Stage 1 = level 0, Stage 2 = level 1, etc.
            int targetLevelIndex = newLevelAfterSendBack;
            
            log.info("Looking for department at index {} (Stage {})", targetLevelIndex, targetLevelIndex + 1);
            
            if (targetLevelIndex < 0 || targetLevelIndex >= pipelines.size()) {
                log.error("Target level index out of bounds for send-back email, appId={}, newLevelIndex={}, pipelineSize={}", 
                    application.getSerApplicationId(), targetLevelIndex, pipelines.size());
                return;
            }
            
            java.util.Map<String, Object> targetPipeline = pipelines.get(targetLevelIndex);
            if (targetPipeline == null) {
                log.error("Target pipeline is null for send-back, appId={}, level={}, index={}", 
                    application.getSerApplicationId(), newLevelAfterSendBack, targetLevelIndex);
                return;
            }
            
            // Log the target pipeline for debugging
            log.info("Target pipeline for send-back: index={}, data={}", targetLevelIndex, targetPipeline);
            
            // Get department ID - try multiple key names
            Integer targetDeptId = safeInt(targetPipeline.get("serDepartmentId"), 
                safeInt(targetPipeline.get("departmentId"), null));
            
            if (targetDeptId == null) {
                log.error("Could not find target department ID for send-back email, appId={}, pipeline={}", 
                    application.getSerApplicationId(), targetPipeline);
                return;
            }
            
            log.info("Sending send-back email to department ID {} for appId={}, level={}", 
                targetDeptId, application.getSerApplicationId(), newLevelAfterSendBack);
            
            // Get department head from HrTblDepartment table (like sendApprovalEmails does)
            emailEntityManager.getTransaction().begin();
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment prevDept = 
                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, targetDeptId);
            
            java.util.List<Integer> headIds = new java.util.ArrayList<>();
            if (prevDept != null && prevDept.getSerDepartmentHeadId() != null) {
                String headIdsStr = prevDept.getSerDepartmentHeadId();
                for (String id : headIdsStr.split(",")) {
                    try {
                        headIds.add(Integer.parseInt(id.trim()));
                    } catch (Exception e) {
                    }
                }
            }
            
            // Fallback if no head IDs found
            if (headIds.isEmpty()) {
                Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, targetDeptId);
                if (fallbackHeadId != null) {
                    headIds.add(fallbackHeadId);
                }
            }
            
            if (headIds.isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                log.warn("No department head found for previous department {} in send-back email", targetDeptId);
                return;
            }
            
            // Build URLs
            String baseUrl = getBaseUrl();
            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId();
            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId();
            String sendBackUrl = null; // Don't allow sending back again from email
            String sendBackToInitiatorUrl = null;
            
                boolean isCapf = isCapfForm(form);
                String cid = isCapf ? "capf-inline" : "form-inline";

                // Send email to each department head with inline preview/attachment
                for (Integer headId : headIds) {
                    CfgTblUser deptHead = emailEntityManager.find(CfgTblUser.class, headId);
                    if (deptHead == null || deptHead.getTxtAddress() == null || deptHead.getTxtAddress().trim().isEmpty()) {
                        log.warn("Department head not found or no email, headId={}", headId);
                        continue;
                    }
                    
                    approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                        "&userId=" + deptHead.getSerUserId();
                    rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                        "&userId=" + deptHead.getSerUserId();
                    
                    // Use the same email format as approval emails
                    String subject = formName + " Sent Back - Requires Your Approval - " + 
                        (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                    
                    String html = generateApprovalEmailHtml(
                        deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                        newLevelAfterSendBack + 1, // level (Stage number, 1-indexed)
                        application.getTxtFormCode(),
                        formName,
                        "IN_PROGRESS", // status - showing it's in progress after send back
                        application.getTxtRemarks(),
                        true, // showActionButtons
                        approveUrl,
                        rejectUrl,
                        sendBackUrl,
                        sendBackToInitiatorUrl,
                        application.getTxtApprovalHistory(),
                        baseUrl
                    );
                    
                    sendEmailWithInlineFormPreview(
                        java.util.Arrays.asList(deptHead.getTxtAddress()),
                        subject,
                        html,
                        application,
                        form,
                        isCapf,
                        cid
                    );
                    
                    log.info("Send-back notification email sent to previous department head: " + deptHead.getTxtAddress());
                }
                
                emailEntityManager.getTransaction().commit();
                
                // Also notify the submitter
                if (application.getSerSubmittedBy() != null) {
                    try {
                        emailEntityManager.getTransaction().begin();
                        CfgTblUser submitter = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                        if (submitter != null && submitter.getTxtAddress() != null && !submitter.getTxtAddress().trim().isEmpty()) {
                            String submitterSubject = formName + " Requires Revision - " + 
                                (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                            
                            // Use the same email format for submitter (without action buttons)
                            String submitterHtml = generateApprovalEmailHtml(
                                submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User",
                                (oldLevelBeforeSendBack != null ? oldLevelBeforeSendBack + 1 : 0), // from stage
                                application.getTxtFormCode(),
                                formName,
                                "SENT_BACK", // status set to SENT_BACK so applicant doesn't see "approved"
                                application.getTxtRemarks(),
                                false, // showActionButtons - submitter can't approve
                                null, null, null, null,
                                application.getTxtApprovalHistory(),
                                baseUrl
                            );
                            
                            sendEmailWithInlineFormPreview(
                                java.util.Arrays.asList(submitter.getTxtAddress()),
                                submitterSubject,
                                submitterHtml,
                                application,
                                form,
                                isCapf,
                                cid
                            );
                            log.info("Send-back notification email sent to submitter: " + submitter.getTxtAddress());
                        }
                    emailEntityManager.getTransaction().commit();
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.warn("Failed to send send-back email to submitter: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Error sending send-back email notification: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }
    
    private java.util.List<String> findEmailsByRole(EntityManager entityManager, String roleName) {
        java.util.List<String> emails = new java.util.ArrayList<>();
        if (entityManager == null || roleName == null)
            return emails;
        try {
            emails = entityManager
                    .createQuery(
                            "SELECT u.txtAddress FROM com.bezkoder.spring.login.admin.dal.entities.CfgTblUser u "
                                    + "WHERE (u.blIsDeleted = false OR u.blIsDeleted IS NULL) "
                                    + "AND UPPER(u.cfgTblRole.txtRoleName) = :roleName "
                                    + "AND u.txtAddress IS NOT NULL",
                            String.class)
                    .setParameter("roleName", roleName.trim().toUpperCase())
                    .getResultList();
        } catch (Exception e) {
            log.warn("findEmailsByRole failed for role {}: {}", roleName, e.getMessage());
        }
        return emails != null ? emails : new java.util.ArrayList<>();
    }

    private int currentLevelSafe(CfgTblCustomFormApplication application) {
        Integer lvl = application != null ? application.getIntCurrentApprovalLevel() : null;
        return lvl != null ? lvl : 0;
    }

    private boolean userHasRole(CfgTblUser user, String roleName) {
        if (user == null || roleName == null)
            return false;
        String rn = "";
        try {
            rn = user.getCfgTblRole() != null && user.getCfgTblRole().getTxtRoleName() != null
                    ? user.getCfgTblRole().getTxtRoleName()
                    : "";
        } catch (Exception ignored) {
        }
        return roleName.trim().equalsIgnoreCase(rn.trim());
    }

    private void appendHistoryEntry(EntityManager em, CfgTblCustomFormApplication application, Integer userId,
            String action, String role, Integer level, String approvedVia, String approvedIp) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            java.util.List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
            if (application.getTxtApprovalHistory() != null && !application.getTxtApprovalHistory().trim().isEmpty()) {
                history = mapper.readValue(application.getTxtApprovalHistory(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {
                        });
            }

            // Load user details for signature, name and designation
            CfgTblUser user = commonService.getCurrentUser(userId);

            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("approvedBy", userId);
            entry.put("action", action);
            entry.put("role", role);
            entry.put("level", level);
            entry.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
            entry.put("approvedIp", approvedIp != null ? approvedIp : "");
            entry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
            entry.put("approvedAt", commonService.getCurrentTimeStamp_new());

            if (user != null) {
                entry.put("approverName", user.getTxtUserName());
                entry.put("signaturePath", user.getTxtSignaturePath());
                entry.put("designation", user.getTxtDesignation());
                entry.put("txtDesignation", user.getTxtDesignation());
                entry.put("txtDepartmentName", user.getTxtDepartmentName());
            }

            history.add(entry);
            application.setTxtApprovalHistory(mapper.writeValueAsString(history));
        } catch (Exception e) {
            log.warn("appendHistoryEntry failed: {}", e.getMessage());
        }
    }

    /**
     * Generate HTML email for application submission confirmation
     */
    private String generateSubmissionEmailHtml(String recipientName, String applicationCode,
            String formName, String status, String submittedDate) {
        StringBuilder html = new StringBuilder();
        html.append(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append(
                "body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;background-color:#f5f5f5}");
        html.append(
                ".email-container{background-color:#ffffff;border-radius:8px;padding:30px;box-shadow:0 2px 4px rgba(0,0,0,0.1)}");
        html.append(
                ".header{background:linear-gradient(135deg,#27ae60 0%,#229954 100%);color:white;padding:20px;border-radius:8px 8px 0 0;margin:-30px -30px 20px -30px}");
        html.append(".header h1{margin:0;font-size:24px;font-weight:600}");
        html.append(".content{padding:20px 0}");
        html.append(".greeting{font-size:16px;margin-bottom:20px;color:#555}");
        html.append(
                ".details{background-color:#f8f9fa;border-left:4px solid #27ae60;padding:15px;margin:20px 0;border-radius:4px}");
        html.append(".detail-row{margin:10px 0;display:flex}");
        html.append(".detail-label{font-weight:600;color:#555;min-width:150px}");
        html.append(".detail-value{color:#333;flex:1}");
        html.append(
                ".footer{margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;text-align:center;color:#95a5a6;font-size:12px}");
        html.append("</style></head><body>");
        String headerTitle = (applicationCode != null && !applicationCode.trim().isEmpty())
                ? applicationCode.trim()
                : (formName != null && !formName.trim().isEmpty() ? formName.trim() : "Application");
        html.append("<div class='email-container'>");
        html.append("<div class='header'><h1>").append(escapeHtml(headerTitle))
                .append(" Submitted Successfully</h1></div>");
        html.append("<div class='content'>");
        html.append("<div class='greeting'>Dear ").append(escapeHtml(recipientName)).append(",</div>");
        html.append("<p>Your application has been submitted successfully.</p>");
        html.append("<div class='details'>");
        html.append(
                "<div class='detail-row'><div class='detail-label'>Application Code:</div><div class='detail-value'>")
                .append(escapeHtml(applicationCode)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Form Name:</div><div class='detail-value'>")
                .append(escapeHtml(formName)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Status:</div><div class='detail-value'>")
                .append(escapeHtml(status)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Submitted Date:</div><div class='detail-value'>")
                .append(escapeHtml(submittedDate)).append("</div></div>");
        html.append("</div>");
        html.append("<p>Your application is now pending approval. You will be notified once it is reviewed.</p>");
        html.append("<p>Thank you for using our system.</p>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Best Regards,<br>System Administrator</p>");
        html.append("<p style='font-size:10px;color:#bdc3c7;'>This is an automated email. Please do not reply.</p>");
        html.append("</div>");
        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Helper to check if a user has already approved this application at any stage
     */
    private boolean isUserAlreadyInApprovedHistory(CfgTblCustomFormApplication application, Integer userId) {
        if (application == null || userId == null)
            return false;
        String history = application.getTxtApprovalHistory();
        if (history == null || history.trim().isEmpty())
            return false;

        // Flexible check for userId in approved state within this history
        // Handles both "approvedBy":145 and "approvedBy": 145
        String userIdPattern = "\"approvedBy\":";
        String userIdValue = String.valueOf(userId);
        String approvedPattern = "\"action\":\"APPROVED\"";
        String approvedPatternWithSpace = "\"action\": \"APPROVED\"";

        int idx = history.indexOf(userIdPattern);
        while (idx != -1) {
            // Find if the next non-whitespace characters match our userId
            int startOfValue = idx + userIdPattern.length();
            while (startOfValue < history.length() && Character.isWhitespace(history.charAt(startOfValue))) {
                startOfValue++;
            }

            if (history.startsWith(userIdValue, startOfValue)) {
                // Find the start and end of this JSON object {}
                int start = history.lastIndexOf("{", idx);
                int end = history.indexOf("}", idx);
                if (start != -1 && end != -1) {
                    String entry = history.substring(start, end);
                    if (entry.contains(approvedPattern) || entry.contains(approvedPatternWithSpace)) {
                        return true;
                    }
                }
            }
            idx = history.indexOf(userIdPattern, idx + 1);
        }
        return false;
    }

    /**
     * Uses Jackson to parse txtApprovalHistory JSON and check if the given userId
     * has performed a REAL approval (action=APPROVED, level >= 1).
     */
    private boolean userHasRealApprovalInHistory(CfgTblCustomFormApplication application,
            Integer userId, ObjectMapper mapper) {
        if (application == null || userId == null)
            return false;
        String history = application.getTxtApprovalHistory();
        if (history == null || history.trim().isEmpty())
            return false;
        try {
            List<java.util.Map<String, Object>> entries = mapper.readValue(history,
                    new TypeReference<List<java.util.Map<String, Object>>>() {
                    });
            if (entries == null)
                return false;
            for (java.util.Map<String, Object> entry : entries) {
                if (entry == null)
                    continue;
                Integer approvedBy = extractApprovalUserId(entry);
                Integer level = safeInt(entry.get("level"), null);
                // Must match userId, be an approved entry, and level >= 1 (not prepared-by)
                // OR level -99 (CEO approval)
                if (userId.equals(approvedBy) && isApprovedEntry(entry)
                        && level != null && (level >= 1 || level == -99)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse approval history for applicationId={}: {}",
                    application.getSerApplicationId(), e.getMessage());
            // Fallback to string-based check
            return isUserAlreadyInApprovedHistory(application, userId);
        }
        return false;
    }
}
