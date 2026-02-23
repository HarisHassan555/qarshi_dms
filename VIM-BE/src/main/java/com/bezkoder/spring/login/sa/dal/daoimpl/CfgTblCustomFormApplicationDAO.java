package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
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
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import java.io.InputStream;
import java.io.File;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Repository
public class CfgTblCustomFormApplicationDAO implements ICfgTblCustomFormApplicationDAO {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(CfgTblCustomFormApplicationDAO.class);
    
    /**
     * Get base URL from application.properties for email links
     */
    private String getBaseUrl() {
        try {
            Properties props = new Properties();
            InputStream input = new ClassPathResource("application.properties").getInputStream();
            props.load(input);
            String baseUrl = props.getProperty("app.base.url", "http://localhost:4200");
            input.close();
            return baseUrl;
        } catch (Exception e) {
            log.warn("Error reading base URL from properties, using default: " + e.getMessage());
            return "http://localhost:4200";
        }
    }

    public CfgTblCustomFormApplicationDAO() {
    }

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
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
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = 
                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, formId);
            
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
                    log.warn("Form " + formId + " has no convention prefix or form code, cannot generate application code");
                    return null;
                }
            }
            
            conventionPrefix = conventionPrefix.trim().toUpperCase();

            // Find the highest application code for this form (or same convention)
            String maxCodeQuery = "SELECT MAX(a.txtFormCode) FROM CfgTblCustomFormApplication a " +
                                 "WHERE a.serFormId = :formId " +
                                 "AND a.txtFormCode IS NOT NULL " +
                                 "AND a.txtFormCode LIKE :prefixPattern " +
                                 "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL)";
            
            String maxCode = null;
            try {
                maxCode = (String) entityManager.createQuery(maxCodeQuery)
                    .setParameter("formId", formId)
                    .setParameter("prefixPattern", conventionPrefix + "-%")
                    .getSingleResult();
            } catch (NoResultException e) {
                // No existing applications with this convention
                maxCode = null;
            }
            
            // If no applications exist, check the form code itself
            if (maxCode == null && form.getTxtFormCode() != null && form.getTxtFormCode().startsWith(conventionPrefix + "-")) {
                maxCode = form.getTxtFormCode();
            }
            
            int nextNumber = 0;
            if (maxCode != null && maxCode.startsWith(conventionPrefix + "-")) {
                try {
                    // Extract number from code (e.g., "PRC-0001" -> 1)
                    String numberPart = maxCode.substring(conventionPrefix.length() + 1);
                    nextNumber = Integer.parseInt(numberPart);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse number from application code: " + maxCode);
                    nextNumber = 0;
                }
            }
            
            // Generate next code with zero-padding (4 digits)
            nextNumber++;
            String nextCode = String.format("%s-%04d", conventionPrefix, nextNumber);
            
            return nextCode;
        } catch (Exception e) {
            log.error("Error generating application code: " + e.getMessage(), e);
            // Fallback: use timestamp-based code
            return "APP-" + System.currentTimeMillis();
        }
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

        List<CfgTblCustomFormApplication> applications =
                entityManager.createQuery(
                        "SELECT a FROM CfgTblCustomFormApplication a " +
                        "JOIN FETCH a.cfgTblCustomForm f " +
                        "WHERE a.serSubmittedBy = :userId " +
                        "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                        "ORDER BY a.dteCreatedDate DESC",
                        CfgTblCustomFormApplication.class)
                        .setParameter("userId", userId)
                        .setFirstResult(0)      // 🔥 Prevent large sort
                        .setMaxResults(200)     // 🔥 Limit results
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
    private void deserializeApprovalPipeline(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form, EntityManager entityManager) {
        try {
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines = new java.util.ArrayList<>();
            
            if (form.getTxtApprovalPipeline() != null && !form.getTxtApprovalPipeline().trim().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<java.util.Map<String, Object>> pipelineMaps = mapper.readValue(
                    form.getTxtApprovalPipeline(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                );
                
                // Convert each map to CfgTblCustomFormApprovalPipeline object
                for (java.util.Map<String, Object> pipelineMap : pipelineMaps) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline = 
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();
                    
                    // Set approval order
                    Object orderObj = pipelineMap.get("intApprovalOrder");
                    if (orderObj != null) {
                        pipeline.setIntApprovalOrder(orderObj instanceof Integer ? (Integer) orderObj : 
                                                     Integer.parseInt(orderObj.toString()));
                    }
                    
                    // Set department ID
                    Object deptIdObj = pipelineMap.get("serDepartmentId");
                    if (deptIdObj != null) {
                        Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                        Integer.parseInt(deptIdObj.toString());
                        pipeline.setSerDepartmentId(deptId);
                        
                        // Fetch department entity
                        com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment department = 
                            entityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, deptId);
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

            // Detect Budget Approval form
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm formForBudget =
                application.getSerFormId() != null
                    ? entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, application.getSerFormId())
                    : null;
            boolean isBudgetApproval = isBudgetApprovalForm(formForBudget);

            // Generate application code based on form's convention if not provided
            if (application.getTxtFormCode() == null || application.getTxtFormCode().trim().isEmpty()) {
                if (application.getSerFormId() != null) {
                    String generatedCode = generateNextApplicationCode(application.getSerFormId(), entityManager);
                    if (generatedCode != null) {
                        application.setTxtFormCode(generatedCode);
                    }
                }
            }

            // Auto-sign "Prepared By" for Budget Approval on submission
            if (isBudgetApproval) {
                try {
                    Map<String, Object> appData = parseApplicationData(application);
                    BudgetApprover preparedBy = getPreparedBy(appData, entityManager);
                    if (preparedBy == null || preparedBy.userId == null) {
                        Integer fallbackUserId = application.getSerSubmittedBy();
                        if (fallbackUserId == null) {
                            fallbackUserId = application.getSerCreatedUser();
                        }
                        if (fallbackUserId != null) {
                            preparedBy = buildBudgetApprover(java.util.Collections.singletonMap("serUserId", fallbackUserId), "PREPARED", entityManager);
                        }
                    }
                    List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();

                    if (preparedBy != null && preparedBy.userId != null) {
                        java.util.Map<String, Object> approvalEntry = new java.util.HashMap<>();
                        approvalEntry.put("level", 0);
                        approvalEntry.put("departmentId", null);
                        approvalEntry.put("departmentName", "Prepared By");
                        approvalEntry.put("remarks", "Auto-signed on submission");
                        approvalEntry.put("approvedBy", preparedBy.userId);
                        approvalEntry.put("approverName", preparedBy.name != null ? preparedBy.name : "Prepared By");
                        approvalEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
                        approvalEntry.put("signaturePath", preparedBy.signaturePath != null ? preparedBy.signaturePath : "");
                        approvalEntry.put("approvedVia", "SYSTEM");
                        approvalEntry.put("action", "APPROVED");
                        approvalEntry.put("role", "PREPARED");
                        approvalHistory.add(approvalEntry);

                        ObjectMapper mapper = new ObjectMapper();
                        application.setTxtApprovalHistory(mapper.writeValueAsString(approvalHistory));
                    }

                    application.setIntCurrentApprovalLevel(0);
                    application.setTxtStatus("IN_PROGRESS");
                } catch (Exception e) {
                    log.warn("Error preparing budget approval auto-sign: " + e.getMessage(), e);
                }
            }

            // Generate and store PDF on creation (summary)
            if (application.getBlbPdfData() == null || application.getBlbPdfData().length == 0) {
                try {
                    Map<String, Object> appData = parseApplicationData(application);
                    byte[] pdfBytes = generateApplicationPdf(application, formForBudget, appData);
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        String code = application.getTxtFormCode() != null ? application.getTxtFormCode() : "application";
                        application.setBlbPdfData(pdfBytes);
                        application.setTxtPdfName(code + ".pdf");
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
                    if (isBudgetApproval) {
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
                Map<String, Object> applicationData = objectMapper.readValue(applicationDataJson, new TypeReference<Map<String, Object>>() {});

                applicationData.put("approvedBySignature", commonService.getCurrentUserName());
                applicationData.put("approvedTimestamp", commonService.getCurrentTimeStamp_new().toString());

                existingApplication.setTxtApplicationData(objectMapper.writeValueAsString(applicationData));
            } catch (Exception jsonException) {
                log.error("Error processing application data JSON for signature/timestamp: " + jsonException.getMessage(), jsonException);
                // Optionally, handle this error more gracefully, e.g., by not updating txtApplicationData
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
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
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
    public List<CfgTblCustomFormApplication> getApplicationsPendingApprovalForDepartmentHead(Integer departmentHeadUserId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            
            // First, find all departments where this user is the head
            java.util.List<com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment> departments = new java.util.ArrayList<>();
            try {
                departments = entityManager.createQuery(
                    "SELECT d FROM HrTblDepartment d " +
                    "WHERE d.serDepartmentHeadId = :headUserId " +
                    "AND (d.blIsDeleted = false OR d.blIsDeleted IS NULL)")
                    .setParameter("headUserId", departmentHeadUserId)
                    .getResultList();
            } catch (Exception e) {
                log.warn("Error finding department for head user " + departmentHeadUserId + ": " + e.getMessage());
            }
            
            if (departments == null || departments.isEmpty()) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }
            
            java.util.Set<Integer> departmentIds = new java.util.HashSet<>();
            for (com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept : departments) {
                if (dept != null && dept.getSerDepartmentId() != null) {
                    departmentIds.add(dept.getSerDepartmentId());
                }
            }
            if (departmentIds.isEmpty()) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }
            
            // Get applications with PENDING or IN_PROGRESS status (capped to avoid MySQL sort buffer overflow)
            List<CfgTblCustomFormApplication> allPendingApplications = entityManager.createQuery(
                "SELECT a FROM CfgTblCustomFormApplication a " +
                "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                "WHERE (a.txtStatus = 'PENDING' OR a.txtStatus = 'IN_PROGRESS') " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                "ORDER BY a.dteCreatedDate DESC",
                CfgTblCustomFormApplication.class)
                .setFirstResult(0)
                .setMaxResults(2000)
                .getResultList();
            
            // Filter applications where the current approval level matches this department's order in the pipeline
            List<CfgTblCustomFormApplication> filteredApplications = new java.util.ArrayList<>();
            
            for (CfgTblCustomFormApplication app : allPendingApplications) {
                if (app.getCfgTblCustomForm() == null) {
                    continue;
                }
                
                // Deserialize approval pipeline from JSON
                String pipelineJson = app.getCfgTblCustomForm().getTxtApprovalPipeline();
                if (pipelineJson == null || pipelineJson.trim().isEmpty()) {
                    continue; // No approval pipeline configured
                }
                
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<java.util.Map<String, Object>> pipelines = mapper.readValue(
                        pipelineJson,
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
                    
                    Integer currentLevel = app.getIntCurrentApprovalLevel();
                    if (currentLevel == null) {
                        currentLevel = 0;
                    }

                    // Check if any department headed by this user matches the current approval level
                    for (java.util.Map<String, Object> pipeline : pipelines) {
                        Object deptIdObj = pipeline.get("serDepartmentId");
                        Object orderObj = pipeline.get("intApprovalOrder");
                        if (deptIdObj == null || orderObj == null) {
                            continue;
                        }
                        Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj :
                                        Integer.parseInt(deptIdObj.toString());
                        if (!departmentIds.contains(deptId)) {
                            continue;
                        }
                        Integer departmentOrder = orderObj instanceof Integer ? (Integer) orderObj :
                                                Integer.parseInt(orderObj.toString());
                        // Approval level 0 means first department (order 1), level 1 means second department (order 2), etc.
                        if (currentLevel.equals(departmentOrder - 1)) {
                            filteredApplications.add(app);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing approval pipeline for application " + app.getSerApplicationId() + ": " + e.getMessage());
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

            List<CfgTblCustomFormApplication> applications = entityManager.createQuery(
                "SELECT a FROM CfgTblCustomFormApplication a " +
                "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                "WHERE (a.txtStatus = 'PENDING' OR a.txtStatus = 'IN_PROGRESS') " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                "ORDER BY a.dteCreatedDate DESC",
                CfgTblCustomFormApplication.class)
                .setFirstResult(0)
                .setMaxResults(2000)
                .getResultList();

            entityManager.getTransaction().commit();
            return applications;
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

    @Override
    public String approveApplication(Integer applicationId, String remarks) {
        return approveApplication(applicationId, remarks, null, "SYSTEM");
    }

    @Override
    public String approveApplication(Integer applicationId, String remarks, Integer approverUserId, String approvedVia) {
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

            // Get the form to check approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = 
                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, 
                    application.getSerFormId());
            
            if (form == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Form not found";
            }

            boolean isBudgetApproval = isBudgetApprovalForm(form);
            if (isBudgetApproval) {
                Map<String, Object> appData = parseApplicationData(application);
                List<BudgetApprover> sequence = getBudgetApprovalSequence(appData, entityManager);
                if (sequence.isEmpty()) {
                    entityManager.getTransaction().rollback();
                    return "Failure: Budget approval sequence not configured";
                }

                Integer currentLevel = application.getIntCurrentApprovalLevel();
                if (currentLevel == null) currentLevel = 0;
                if (currentLevel < 0 || currentLevel >= sequence.size()) {
                    entityManager.getTransaction().rollback();
                    return "Failure: Approval already completed";
                }

                BudgetApprover expected = sequence.get(currentLevel);
                if (expected.userId == null || !expected.userId.equals(resolvedApproverId)) {
                    entityManager.getTransaction().rollback();
                    return "Failure: You are not authorized to approve at this stage";
                }

                // Get or create approval history array
                List<java.util.Map<String, Object>> approvalHistory = new java.util.ArrayList<>();
                String historyJson = application.getTxtApprovalHistory();
                if (historyJson != null && !historyJson.trim().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        approvalHistory = mapper.readValue(
                            historyJson,
                            new TypeReference<List<java.util.Map<String, Object>>>() {}
                        );
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
                approvalEntry.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
                approvalEntry.put("action", "APPROVED");
                approvalEntry.put("role", expected.role);
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
                    application.setTxtStatus("APPROVED");
                } else {
                    application.setTxtStatus("IN_PROGRESS");
                }
                application.setIntCurrentApprovalLevel(currentLevel);
                application.setSerCurrentApprover(resolvedApproverId);
                application.setTxtRemarks(remarks);
                application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                application.setSerModifiedUser(resolvedApproverId);

                // Regenerate PDF only when approval happens via email or when no PDF exists.
                // UI approvals upload the latest printed PDF before approval; preserve that file.
                boolean shouldRegeneratePdf = "EMAIL".equalsIgnoreCase(approvedVia) ||
                    application.getBlbPdfData() == null || application.getBlbPdfData().length == 0;
                if (shouldRegeneratePdf) {
                    try {
                        byte[] pdfBytes = generateBudgetApprovalPdf(application, form, appData);
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            String code = application.getTxtFormCode() != null ? application.getTxtFormCode() : "application";
                            application.setBlbPdfData(pdfBytes);
                            application.setTxtPdfName(code + ".pdf");
                            application.setTxtPdfMime("application/pdf");
                        }
                    } catch (Exception e) {
                        log.warn("Error regenerating budget approval PDF: " + e.getMessage(), e);
                    }
                }

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
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
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
            
            // Get the department that is currently approving (at currentLevel, which is 0-indexed)
            // Pipeline order is 1-indexed, so currentLevel 0 = pipeline order 1
            java.util.Map<String, Object> currentDepartmentPipeline = null;
            Integer departmentId = null;
            String departmentName = null;
            Integer pipelineOrder = null;
            
            if (!pipelines.isEmpty() && currentLevel < pipelines.size()) {
                currentDepartmentPipeline = pipelines.get(currentLevel);
                if (currentDepartmentPipeline != null) {
                    Object deptIdObj = currentDepartmentPipeline.get("serDepartmentId");
                    Object orderObj = currentDepartmentPipeline.get("intApprovalOrder");
                    
                    if (deptIdObj != null) {
                        departmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                      Integer.parseInt(deptIdObj.toString());
                        // Resolve department name from pipeline or DB
                        departmentName = resolveDepartmentName(entityManager, departmentId, currentDepartmentPipeline);
                    }
                    
                    if (orderObj != null) {
                        pipelineOrder = orderObj instanceof Integer ? (Integer) orderObj : 
                                       Integer.parseInt(orderObj.toString());
                    }
                }
            }

            // Enforce department-based approval: approver must belong to current pipeline department
            if (departmentId != null) {
                Integer approverDeptId = loadUserDepartmentId(entityManager, resolvedApproverId);
                if (approverDeptId == null || !departmentId.equals(approverDeptId)) {
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
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
                } catch (Exception e) {
                    log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                    approvalHistory = new java.util.ArrayList<>();
                }
            }
            
            // Add current approval to history (the level being approved is currentLevel + 1 in terms of pipeline order)
            // But we store the actual pipeline order (1-indexed)
            java.util.Map<String, Object> approvalEntry = new java.util.HashMap<>();
            approvalEntry.put("level", pipelineOrder != null ? pipelineOrder : (currentLevel + 1)); // Pipeline order (1-indexed)
            approvalEntry.put("departmentId", departmentId);
            approvalEntry.put("departmentName", departmentName != null ? departmentName : (departmentId != null ? "Department " + departmentId : "Unknown"));
            approvalEntry.put("remarks", remarks != null ? remarks : "");
            approvalEntry.put("approvedBy", resolvedApproverId);
            approvalEntry.put("approverName", approverUser.getTxtUserName());
            approvalEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
            approvalEntry.put("signaturePath", approverSignaturePath);
            approvalEntry.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
            approvalEntry.put("action", "APPROVED");
            approvalEntry.put("role", departmentName != null ? departmentName : "");
            approvalHistory.add(approvalEntry);
            
            // Save updated history
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String updatedHistoryJson = mapper.writeValueAsString(approvalHistory);
                application.setTxtApprovalHistory(updatedHistoryJson);
            } catch (Exception e) {
                log.error("Error serializing approval history: " + e.getMessage());
            }
            
            // Increment approval level
            currentLevel++;
            
            // Check if this is the last level
            if (pipelines.isEmpty() || currentLevel >= pipelines.size()) {
                // All approvals complete
                application.setTxtStatus("APPROVED");
                application.setIntCurrentApprovalLevel(currentLevel);
            } else {
                // Move to next level
                application.setTxtStatus("IN_PROGRESS");
                application.setIntCurrentApprovalLevel(currentLevel);
            }
            
            application.setSerCurrentApprover(resolvedApproverId);
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(resolvedApproverId);

            // Regenerate CAPF PDF only when approval happens via email or when no PDF exists.
            // UI approvals upload the latest printed PDF before approval; preserve that file.
            boolean shouldRegeneratePdf = "EMAIL".equalsIgnoreCase(approvedVia) ||
                application.getBlbPdfData() == null || application.getBlbPdfData().length == 0;
            if (shouldRegeneratePdf) {
                try {
                    if (isCapfForm(form)) {
                        Map<String, Object> appData = parseApplicationData(application);
                        byte[] pdfBytes = generateCapfPdf(application, form, appData);
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            String code = application.getTxtFormCode() != null ? application.getTxtFormCode() : "application";
                            application.setBlbPdfData(pdfBytes);
                            application.setTxtPdfName(code + ".pdf");
                            application.setTxtPdfMime("application/pdf");
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error regenerating CAPF PDF: " + e.getMessage(), e);
                }
            }
            
            entityManager.merge(application);
            entityManager.getTransaction().commit();
            
            // Send email notifications after successful approval (use new entity manager since transaction is closed)
            // currentLevel is now the new level (0-indexed) after increment
            // The approved level was at index (currentLevel - 1), but pipelineOrder is 1-indexed
            // If pipelineOrder is null, calculate it: currentLevel (0-indexed) = pipelineOrder (1-indexed)
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
            log.error("Error approving application: " + e.getMessage(), e);
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
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form =
                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class,
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
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );

                    if (!pipelines.isEmpty() && currentLevel < pipelines.size()) {
                        java.util.Map<String, Object> currentPipeline = pipelines.get(currentLevel);
                        if (currentPipeline != null) {
                            Object deptIdObj = currentPipeline.get("serDepartmentId");
                            Object orderObj = currentPipeline.get("intApprovalOrder");
                            if (deptIdObj != null) {
                                departmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj :
                                    Integer.parseInt(deptIdObj.toString());
                                departmentName = resolveDepartmentName(entityManager, departmentId, currentPipeline);
                            }
                            if (orderObj != null) {
                                pipelineOrder = orderObj instanceof Integer ? (Integer) orderObj :
                                    Integer.parseInt(orderObj.toString());
                            }
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
                            new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                        );
                    } catch (Exception e) {
                        approvalHistory = new java.util.ArrayList<>();
                    }
                }

                java.util.Map<String, Object> rejectionEntry = new java.util.HashMap<>();
                rejectionEntry.put("level", pipelineOrder != null ? pipelineOrder : (currentLevel + 1));
                rejectionEntry.put("departmentId", departmentId);
                rejectionEntry.put("departmentName", departmentName != null ? departmentName : (departmentId != null ? "Department " + departmentId : "Unknown"));
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
        } catch (Exception e) {
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
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = 
                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, 
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
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
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
            
            if (!pipelines.isEmpty() && currentLevel > 0 && currentLevel <= pipelines.size()) {
                // Get the department at the current level (the one sending back)
                // currentLevel is 1-based, array index is 0-based, so index = currentLevel - 1
                int currentLevelIndex = currentLevel - 1;
                if (currentLevelIndex >= 0 && currentLevelIndex < pipelines.size()) {
                    currentDepartmentPipeline = pipelines.get(currentLevelIndex);
                    if (currentDepartmentPipeline != null) {
                        Object deptIdObj = currentDepartmentPipeline.get("serDepartmentId");
                        Object orderObj = currentDepartmentPipeline.get("intApprovalOrder");
                        
                        if (deptIdObj != null) {
                            currentDepartmentId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                                 Integer.parseInt(deptIdObj.toString());
                            currentDepartmentName = resolveDepartmentName(entityManager, currentDepartmentId, currentDepartmentPipeline);
                        }
                        
                        if (orderObj != null) {
                            currentPipelineOrder = orderObj instanceof Integer ? (Integer) orderObj : 
                                                  Integer.parseInt(orderObj.toString());
                        }
                    }
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
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
                } catch (Exception e) {
                    log.warn("Error parsing approval history, starting fresh: " + e.getMessage());
                    approvalHistory = new java.util.ArrayList<>();
                }
            }
            
            // Add send-back action to history
            java.util.Map<String, Object> sendBackEntry = new java.util.HashMap<>();
            sendBackEntry.put("action", "SENT_BACK");
            sendBackEntry.put("level", currentLevel); // Current level before decrement
            sendBackEntry.put("departmentId", currentDepartmentId);
            sendBackEntry.put("departmentName", currentDepartmentName != null ? currentDepartmentName : (currentDepartmentId != null ? "Department " + currentDepartmentId : "Unknown"));
            sendBackEntry.put("remarks", remarks != null ? remarks : "");
            sendBackEntry.put("sentBackBy", commonService.getCurrentLoggedInUser());
            sendBackEntry.put("sentBackDate", commonService.getCurrentTimeStamp_new().toString());
            approvalHistory.add(sendBackEntry);
            
            // Save updated history
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String updatedHistoryJson = mapper.writeValueAsString(approvalHistory);
                application.setTxtApprovalHistory(updatedHistoryJson);
            } catch (Exception e) {
                log.error("Error serializing approval history: " + e.getMessage());
            }
            
            // Decrement approval level (send back to previous department)
            if (currentLevel > 0) {
                currentLevel--;
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
            return "Success";
        } catch (Exception e) {
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
    public String sendSubmissionEmailsForApplication(Integer applicationId) {
        EntityManager entityManager = getEntityManager();
        try {
            if (applicationId == null) {
                return "Failure: Application ID is required";
            }

            entityManager.getTransaction().begin();
            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                entityManager.getTransaction().rollback();
                return "Failure: Application not found";
            }

            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form =
                application.getSerFormId() != null
                    ? entityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, application.getSerFormId())
                    : null;
            boolean isBudgetApproval = isBudgetApprovalForm(form);

            entityManager.getTransaction().commit();

            try {
                if (isBudgetApproval) {
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

    /**
     * Send email notifications when an application is approved
     * Sends email to:
     * 1. The user who submitted the application (notifying them of approval at current level)
     * 2. The next level department head (if there is a next level)
     * 
     * @param application The approved application
     * @param approvedPipelineOrder The pipeline order (1-indexed) that was just approved
     * @param currentLevel The new current level (0-indexed) after approval
     * @param pipelines The approval pipeline list
     */
    private void sendApprovalEmails(CfgTblCustomFormApplication application, Integer approvedPipelineOrder, 
                                   Integer currentLevel, List<java.util.Map<String, Object>> pipelines) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            String formName = "Unknown Form";
            
            // Get form name
            if (application.getCfgTblCustomForm() != null) {
                formName = application.getCfgTblCustomForm().getTxtFormName();
            }
            
            // 1. Get email of the user who submitted the application
            if (application.getSerSubmittedBy() != null) {
                try {
                    CfgTblUser submittedByUser = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                    if (submittedByUser != null && submittedByUser.getTxtAddress() != null && 
                        !submittedByUser.getTxtAddress().trim().isEmpty()) {
                        
                        // Send HTML email to submitter
                        String submitterSubject = "Application Approved at Level " + approvedPipelineOrder + " - " + 
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
                            application.getTxtApprovalHistory(),
                            getBaseUrl()
                        );
                        
                        if (application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                            emailService.sendHtmlEmailWithAttachment(java.util.Arrays.asList(submittedByUser.getTxtAddress()),
                                submitterSubject, submitterHtmlMessage,
                                application.getBlbPdfData(), application.getTxtPdfName(), application.getTxtPdfMime());
                        } else {
                            emailService.sendHtmlEmail(java.util.Arrays.asList(submittedByUser.getTxtAddress()), 
                                submitterSubject, submitterHtmlMessage);
                        }
                        log.info("Approval email sent to submitter: " + submittedByUser.getTxtAddress());
                    }
                } catch (Exception e) {
                    log.error("Error getting submitter email: " + e.getMessage(), e);
                }
            }
            
            // 2. Get email of the next level department head (if there is a next level)
            // currentLevel is 0-indexed and represents the next level to be approved
            if (pipelines != null && !pipelines.isEmpty() && currentLevel < pipelines.size()) {
                try {
                    // Get the next level pipeline (currentLevel is 0-indexed, so this is the next level)
                    java.util.Map<String, Object> nextLevelPipeline = pipelines.get(currentLevel);
                    if (nextLevelPipeline != null) {
                        Object deptIdObj = nextLevelPipeline.get("serDepartmentId");
                        if (deptIdObj != null) {
                            Integer nextDeptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                                Integer.parseInt(deptIdObj.toString());
                            
                            // Get the department with department head
                            emailEntityManager.getTransaction().begin();
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment nextDept = 
                                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, nextDeptId);
                            
                            if (nextDept != null) {
                                Integer headId = nextDept.getSerDepartmentHeadId();
                                if (headId != null) {
                                    Integer headDeptId = loadUserDepartmentId(emailEntityManager, headId);
                                    if (headDeptId == null || !headDeptId.equals(nextDeptId)) {
                                        headId = null;
                                    }
                                }
                                if (headId == null) {
                                    headId = findDepartmentHeadUserId(emailEntityManager, nextDeptId);
                                }
                                CfgTblUser nextDeptHead = headId != null ? emailEntityManager.find(CfgTblUser.class, headId) : null;
                                    if (nextDeptHead != null && nextDeptHead.getTxtAddress() != null && 
                                        !nextDeptHead.getTxtAddress().trim().isEmpty()) {
                                        
                                        // Get the next level's pipeline order
                                        Object nextOrderObj = nextLevelPipeline.get("intApprovalOrder");
                                        Integer nextLevelOrder = nextOrderObj != null ? 
                                            (nextOrderObj instanceof Integer ? (Integer) nextOrderObj : 
                                             Integer.parseInt(nextOrderObj.toString())) : (currentLevel + 1);
                                        
                                        // Send HTML email to next level department head with approve/reject buttons
                                        String deptHeadSubject = "New Application Pending Approval - Level " + nextLevelOrder + 
                                                               " - " + (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                                        String baseUrl = getBaseUrl();
                                        String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                                                          "&userId=" + nextDeptHead.getSerUserId();
                                        String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                                                         "&userId=" + nextDeptHead.getSerUserId();
                                        
                                        String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                "&userId=" + nextDeptHead.getSerUserId();
                                        String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                            nextDeptHead.getTxtUserName() != null ? nextDeptHead.getTxtUserName() : "Department Head",
                                            nextLevelOrder,
                                            application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                            formName,
                                            application.getTxtStatus(),
                                            null,
                                            true, // For department head
                                            approveUrl,
                                            rejectUrl,
                                            sendBackUrl,
                                            application.getTxtApprovalHistory(),
                                            getBaseUrl()
                                        );
                                        
                                        if (application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                                            emailService.sendHtmlEmailWithAttachment(java.util.Arrays.asList(nextDeptHead.getTxtAddress()), 
                                                deptHeadSubject, deptHeadHtmlMessage,
                                                application.getBlbPdfData(), application.getTxtPdfName(), application.getTxtPdfMime());
                                        } else {
                                            emailService.sendHtmlEmail(java.util.Arrays.asList(nextDeptHead.getTxtAddress()), 
                                                deptHeadSubject, deptHeadHtmlMessage);
                                        }
                                        log.info("Approval notification email sent to next level department head: " + nextDeptHead.getTxtAddress());
                                    }
                            }
                            emailEntityManager.getTransaction().commit();
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
            // Don't throw - email failure shouldn't break approval
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
     * 2. The first level department head (notification of new application pending approval)
     */
    private void sendSubmissionEmails(CfgTblCustomFormApplication application) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            
            // Fetch the form with approval pipeline
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = null;
            if (application.getSerFormId() != null) {
                form = emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class, application.getSerFormId());
            }
            
            String formName = "Unknown Form";
            if (form != null) {
                formName = form.getTxtFormName();
            }

            boolean isBudgetApproval = isBudgetApprovalForm(form);
            
            // Get approval pipeline from form
            List<java.util.Map<String, Object>> pipelines = new java.util.ArrayList<>();
            if (form != null && form.getTxtApprovalPipeline() != null &&
                !form.getTxtApprovalPipeline().trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pipelines = mapper.readValue(
                        form.getTxtApprovalPipeline(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, Object>>>() {}
                    );
                } catch (Exception e) {
                    log.error("Error parsing approval pipeline for submission emails: " + e.getMessage(), e);
                }
            }
            
            emailEntityManager.getTransaction().commit();
            
            // 1. Send email to the user who submitted the application
            if (application.getSerSubmittedBy() != null) {
                try {
                    emailEntityManager.getTransaction().begin();
                    CfgTblUser submittedByUser = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                    if (submittedByUser != null && submittedByUser.getTxtAddress() != null && 
                        !submittedByUser.getTxtAddress().trim().isEmpty()) {
                        
                        // Send HTML confirmation email to submitter
                        String submitterSubject = "Application Submitted Successfully - " + 
                                               (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtmlMessage = generateSubmissionEmailHtml(
                            submittedByUser.getTxtUserName() != null ? submittedByUser.getTxtUserName() : "User",
                            application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                            formName,
                            application.getTxtStatus(),
                            application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : "N/A"
                        );
                        
                        emailService.sendHtmlEmail(java.util.Arrays.asList(submittedByUser.getTxtAddress()), 
                                                  submitterSubject, submitterHtmlMessage);
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
            
            // 2. Send email to the first level department head (if approval pipeline exists)
            if (!isBudgetApproval && pipelines != null && !pipelines.isEmpty()) {
                try {
                    emailEntityManager.getTransaction().begin();
                    // Get the first level pipeline (index 0)
                    java.util.Map<String, Object> firstLevelPipeline = pipelines.get(0);
                    if (firstLevelPipeline != null) {
                        Object deptIdObj = firstLevelPipeline.get("serDepartmentId");
                        if (deptIdObj != null) {
                            Integer firstDeptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                                  Integer.parseInt(deptIdObj.toString());
                            
                            // Get the department with department head
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment firstDept = 
                                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, firstDeptId);
                            
                            if (firstDept != null) {
                                Integer headId = firstDept.getSerDepartmentHeadId();
                                if (headId != null) {
                                    Integer headDeptId = loadUserDepartmentId(emailEntityManager, headId);
                                    if (headDeptId == null || !headDeptId.equals(firstDeptId)) {
                                        headId = null;
                                    }
                                }
                                if (headId == null) {
                                    headId = findDepartmentHeadUserId(emailEntityManager, firstDeptId);
                                }
                                CfgTblUser firstDeptHead = headId != null ? emailEntityManager.find(CfgTblUser.class, headId) : null;
                                if (firstDeptHead != null && firstDeptHead.getTxtAddress() != null && 
                                    !firstDeptHead.getTxtAddress().trim().isEmpty()) {
                                    
                                    // Get the first level's pipeline order
                                    Object orderObj = firstLevelPipeline.get("intApprovalOrder");
                                    Integer firstLevelOrder = orderObj != null ? 
                                        (orderObj instanceof Integer ? (Integer) orderObj : 
                                         Integer.parseInt(orderObj.toString())) : 1;
                                    
                                    // Send HTML email to first level department head with approve/reject buttons
                                    String deptHeadSubject = "New Application Pending Approval - Level " + firstLevelOrder + 
                                                           " - " + (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                                    String baseUrl = getBaseUrl();
                                    String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                                                      "&userId=" + firstDeptHead.getSerUserId();
                                    String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                                                     "&userId=" + firstDeptHead.getSerUserId();
                                    
                                    String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                            "&userId=" + firstDeptHead.getSerUserId();
                                    String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                        firstDeptHead.getTxtUserName() != null ? firstDeptHead.getTxtUserName() : "Department Head",
                                        firstLevelOrder,
                                        application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                        formName,
                                        application.getTxtStatus(),
                                        null,
                                        true, // For department head
                                        approveUrl,
                                        rejectUrl,
                                            sendBackUrl,
                                            application.getTxtApprovalHistory(),
                                            getBaseUrl()
                                    );
                                    
                                    if (application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                                        emailService.sendHtmlEmailWithAttachment(java.util.Arrays.asList(firstDeptHead.getTxtAddress()), 
                                            deptHeadSubject, deptHeadHtmlMessage,
                                            application.getBlbPdfData(), application.getTxtPdfName(), application.getTxtPdfMime());
                                    } else {
                                        emailService.sendHtmlEmail(java.util.Arrays.asList(firstDeptHead.getTxtAddress()), 
                                            deptHeadSubject, deptHeadHtmlMessage);
                                    }
                                    log.info("Submission notification email sent to first level department head: " + firstDeptHead.getTxtAddress());
                                }
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
        if (form == null) return false;
        String name = form.getTxtFormName() != null ? form.getTxtFormName().toUpperCase() : "";
        String code = form.getTxtFormCode() != null ? form.getTxtFormCode().toUpperCase() : "";
        return name.contains("BUDGET APPROVAL") || code.startsWith("BDG");
    }


    private Map<String, Object> parseApplicationData(CfgTblCustomFormApplication application) {
        try {
            String raw = application.getTxtApplicationData();
            if (raw == null || raw.trim().isEmpty()) return new java.util.HashMap<>();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Error parsing application data: " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    private BudgetApprover getPreparedBy(Map<String, Object> appData, EntityManager em) {
        Object obj = appData.get("preparedBy");
        return buildBudgetApprover(obj, "PREPARED", em);
    }

    private List<BudgetApprover> getBudgetApprovalSequence(Map<String, Object> appData, EntityManager em) {
        List<BudgetApprover> seq = new java.util.ArrayList<>();
        Object reviewersObj = appData.get("reviewers");
        if (reviewersObj instanceof List) {
            for (Object r : (List<?>) reviewersObj) {
                BudgetApprover b = buildBudgetApprover(r, "REVIEWER", em);
                if (b != null && b.userId != null) seq.add(b);
            }
        }
        Object recommendersObj = appData.get("recommenders");
        if (recommendersObj instanceof List) {
            for (Object r : (List<?>) recommendersObj) {
                BudgetApprover b = buildBudgetApprover(r, "RECOMMENDER", em);
                if (b != null && b.userId != null) seq.add(b);
            }
        }
        Object approverObj = appData.get("approver");
        BudgetApprover approver = buildBudgetApprover(approverObj, "APPROVER", em);
        if (approver != null && approver.userId != null) seq.add(approver);
        return seq;
    }

    private BudgetApprover buildBudgetApprover(Object userObj, String role, EntityManager em) {
        if (userObj == null) return null;
        Integer userId = null;
        String name = null;
        String email = null;
        String signaturePath = null;

        if (userObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) userObj;
            Object idObj = map.get("serUserId");
            if (idObj == null) idObj = map.get("userId");
            if (idObj != null) userId = Integer.parseInt(idObj.toString());
            Object nameObj = map.get("txtUserName");
            if (nameObj == null) nameObj = map.get("userName");
            if (nameObj != null) name = nameObj.toString();
            Object emailObj = map.get("txtAddress");
            if (emailObj == null) emailObj = map.get("email");
            if (emailObj != null) email = emailObj.toString();
        }

        if (userId != null) {
            CfgTblUser user = em.find(CfgTblUser.class, userId);
            if (user != null) {
                if (name == null) name = user.getTxtUserName();
                if (email == null) email = user.getTxtAddress();
                signaturePath = user.getTxtSignaturePath();
            }
        }

        BudgetApprover b = new BudgetApprover();
        b.userId = userId;
        b.name = name;
        b.email = email;
        b.role = role;
        b.signaturePath = signaturePath;
        return b;
    }

    private void sendBudgetApprovalNextEmail(CfgTblCustomFormApplication application, Integer sequenceIndex) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            emailEntityManager.getTransaction().begin();
            Map<String, Object> appData = parseApplicationData(application);
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

            String formName = "Budget Approval";
            String subject = "Budget Approval Pending - " +
                (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");

            String baseUrl = getBaseUrl();
            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                "&userId=" + next.userId;
            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                "&userId=" + next.userId;

            String sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
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
                                            application.getTxtApprovalHistory(),
                                            getBaseUrl()
            );

            if (application.getBlbPdfData() != null && application.getBlbPdfData().length > 0) {
                emailService.sendHtmlEmailWithAttachment(java.util.Arrays.asList(next.email), subject, html,
                        application.getBlbPdfData(), application.getTxtPdfName(), application.getTxtPdfMime());
            } else {
                emailService.sendHtmlEmail(java.util.Arrays.asList(next.email), subject, html);
            }
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
            y = writeLine(content, y, "Form: " + (form != null && form.getTxtFormName() != null ? form.getTxtFormName() : "N/A"));
            y = writeLine(content, y, "Code: " + (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A"));
            y = writeLine(content, y, "Status: " + (application.getTxtStatus() != null ? application.getTxtStatus() : "N/A"));
            y = writeLine(content, y, "Created: " + (application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : "N/A"));

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
                getValueByKeyContains(appData, "heading"),
                getValueByKeyContains(appData, "title"),
                getValueByKeyContains(appData, "subject"),
                form != null ? form.getTxtFormName() : null,
                "Budget Approval Form"
            );

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
            String address = "15-6, Jam-e-Shirin Boulevard, Gulberg-III, Lahore";
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

            drawSignatureTable(content, margin, tableBottomY, pageWidth - margin * 2, tableHeight, appData, application.getTxtApprovalHistory(), document);

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

            String dateStr = application != null && application.getDteCreatedDate() != null
                ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(application.getDteCreatedDate())
                : new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());

            String division = pickFirstNonEmpty(getCapfValue(appData, "division"), getCapfValue(appData, "department"));
            String department = pickFirstNonEmpty(getCapfValue(appData, "department"), division);
            String section = pickFirstNonEmpty(getCapfValue(appData, "section"), "GEN");
            String documentNo = pickFirstNonEmpty(getCapfValue(appData, "capfNumber"), getCapfValue(appData, "capf #"), getCapfValue(appData, "document no"), "CAPF");
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

            y -= 36;

            // Meta table (Division/Department/Section/Document No/Original Issue/Rev/Rev Date)
            float metaHeight = 32f;
            drawRect(content, margin + 6, y - metaHeight, pageWidth - margin * 2 - 12, metaHeight);
            float metaY = y - metaHeight + 20;
            content.setFont(PDType1Font.HELVETICA, 8);
            drawMeta(content, margin + 10, metaY, "Division: " + nullSafe(division));
            drawMeta(content, margin + 150, metaY, "Department: " + nullSafe(department));
            drawMeta(content, margin + 300, metaY, "Section: " + nullSafe(section));
            drawMeta(content, margin + 10, metaY - 12, "Document No: " + nullSafe(documentNo));
            drawMeta(content, margin + 150, metaY - 12, "Original Issue: " + nullSafe(originalIssue));
            drawMeta(content, margin + 300, metaY - 12, "Rev: " + nullSafe(rev));
            drawMeta(content, margin + 360, metaY - 12, "Rev. Date: " + nullSafe(revDate));

            y -= (metaHeight + 14);

            // Title bar
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            drawCentered(content, pageWidth, y, "CAPITAL ASSETS PURCHASE FORM");
            y -= 16;
            drawCentered(content, pageWidth, y, "PART I (TO BE FILLED BY CONCERNED DEPARTMENT)");
            y -= 10;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 12;

            float lineStart = margin + 12;
            float lineEnd = pageWidth - margin - 12;
            float labelWidth = 140;
            content.setFont(PDType1Font.HELVETICA, 9);

            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DIVISION / DEPARTMENT:", nullSafe(division));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "CAPF #:", nullSafe(documentNo), 180);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DATE:", nullSafe(dateStr), 180);
            y -= 4;
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "NAME OF ASSET / ITEM:", nullSafe(assetName));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DETAIL SPECIFICATION:", nullSafe(specification));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "UTILITY & PURPOSE:", nullSafe(utility));

            y -= 2;
            y = drawYesNoRow(content, lineStart, y, lineEnd, "FEASIBILITY REPORT ATTACHED:", feasibility);
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "IF NO THEN MENTION REASON:", nullSafe(reason));

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
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "DELIVERY PERIOD & DATE:", nullSafe(delivery));
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "TERMS & CONDITIONS:", nullSafe(terms));
            y -= 2;
            y = drawYesNoNaRow(content, lineStart, y, lineEnd, "Third Party assessment carried out:", thirdParty);

            y -= 6;
            drawLine(content, margin + 6, y, pageWidth - margin - 6, y);
            y -= 8;
            y = drawCapfSignatureSection(content, lineStart, y, lineEnd - lineStart, application.getTxtApprovalHistory(), document);
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
            y = drawLabeledLine(content, lineStart, y, labelWidth, lineEnd, "This is to certify that job against CAPF:", "");
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
            if (is == null) return;
            byte[] data = readAllBytes(is);
            if (data == null || data.length == 0) return;
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image =
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(document, data, "qarshi-logo");
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
            getValueByKey(appData, "content"),
            getValueByKey(appData, "editorContent"),
            getValueByKey(appData, "html")
        );
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
        if (value == null || value.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("\n\n");
        sb.append(title).append(":\n").append(value.trim());
    }

    private String htmlToPlainText(String html) {
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?s)<[^>]*>", "");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        return text.trim();
    }

    private String getValueByKeyContains(Map<String, Object> appData, String needle) {
        if (appData == null || needle == null) return null;
        String n = needle.toLowerCase();
        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.toLowerCase().contains(n)) {
                Object val = entry.getValue();
                if (val != null) return String.valueOf(val);
            }
        }
        return null;
    }

    private String getValueByKey(Map<String, Object> appData, String key) {
        if (appData == null || key == null) return null;
        Object val = appData.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private String pickFirstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private float drawWrappedText(PDPageContentStream content, String text,
                                  float x, float y, float maxWidth, float leading, float minY) throws java.io.IOException {
        if (text == null) return y;
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

    private java.util.List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws java.io.IOException {
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
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private void drawSignatureTable(PDPageContentStream content, float x, float y, float width, float height, Map<String, Object> appData, String approvalHistoryJson, PDDocument document) throws java.io.IOException {
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
        java.util.List<java.util.Map<String, String>> recommenders = extractUserListDisplay(appData.get("recommenders"));
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

        Integer[] userIds = new Integer[] { preparedId, reviewer1Id, reviewer2Id, recommender1Id, recommender2Id, approverId };
        String[] roles = new String[] { "PREPARED", "REVIEWER", "REVIEWER", "RECOMMENDER", "RECOMMENDER", "APPROVER" };
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(userIds);
        float sigRowY = y + rowSig + rowHeader + 4;
        float sigRowHeight = rowNames - 8;
        for (int i = 0; i < userIds.length; i++) {
            Integer uid = userIds[i];
            boolean allowFallback = "PREPARED".equalsIgnoreCase(roles[i]);
            String sigPath = findSignatureForUser(approvalHistory, uid, roles[i], allowFallback, signatureFromDb);
            if (sigPath == null || sigPath.trim().isEmpty()) continue;
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
            Object action = entry.get("action");
            if (action != null && "APPROVED".equalsIgnoreCase(action.toString())) {
                approved.add(entry);
            }
        }

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
        Map<String, Object>[] mapped = new Map[6];
        Integer[] approvedUserIds = new Integer[approved.size()];
        for (int i = 0; i < approved.size(); i++) {
            approvedUserIds[i] = extractUserId(approved.get(i).get("approvedBy"));
        }
        Map<Integer, String> signatureFromDb = loadUserSignaturePaths(approvedUserIds);
        for (int i = 0; i < 6; i++) {
            Map<String, Object> entry = i < approved.size() ? approved.get(i) : null;
            mapped[i] = entry;
            String sigPath = null;
            if (entry != null) {
                Integer approvedBy = extractUserId(entry.get("approvedBy"));
                if (approvedBy != null && signatureFromDb.containsKey(approvedBy)) {
                    sigPath = signatureFromDb.get(approvedBy);
                } else if (entry.get("signaturePath") != null) {
                    sigPath = String.valueOf(entry.get("signaturePath"));
                }
            }
            if (sigPath != null && !sigPath.trim().isEmpty()) {
                drawSignatureImage(document, content, sigPath, x + colWidth * i + 4, sigRowY, colWidth - 8, sigHeight);
            }
        }

        content.setFont(PDType1Font.HELVETICA, 6.5f);
        float dateY = sigRowY - 8;
        float nameY = dateY - 8;
        for (int i = 0; i < 6; i++) {
            Map<String, Object> entry = mapped[i];
            String date = entry != null ? formatApprovalDate(entry.get("approvedDate")) : "";
            if (!date.isEmpty()) {
                content.beginText();
                content.newLineAtOffset(x + colWidth * i + 4, dateY);
                content.showText(date);
                content.endText();
            }
            String name = entry != null && entry.get("approverName") != null ? entry.get("approverName").toString() : "";
            if (!name.trim().isEmpty()) {
                for (String line : wrapText(name, PDType1Font.HELVETICA, 6.5f, colWidth - 6)) {
                    content.beginText();
                    content.newLineAtOffset(x + colWidth * i + 4, nameY);
                    content.showText(line);
                    content.endText();
                    nameY -= 7;
                }
                nameY = dateY - 8;
            }
        }

        content.setFont(PDType1Font.HELVETICA_BOLD, 7f);
        float labelY = nameY - 12;
        for (int i = 0; i < roleLabels.length; i++) {
            String label = roleLabels[i];
            for (String line : wrapText(label, PDType1Font.HELVETICA_BOLD, 7f, colWidth - 6)) {
                content.beginText();
                content.newLineAtOffset(x + colWidth * i + 3, labelY);
                content.showText(line);
                content.endText();
                labelY -= 8;
            }
            labelY = dateY - 10;
        }

        // Approved By label on right side
        content.setFont(PDType1Font.HELVETICA_BOLD, 7f);
        content.beginText();
        content.newLineAtOffset(x + colWidth * 5 + 3, labelY - 12);
        content.showText("Approved By:");
        content.endText();

        return y - blockHeight;
    }

    private Map<String, Object> findCapfEntryForRole(List<Map<String, Object>> approved, String[] keywords, int fallbackIndex, java.util.Set<Integer> usedIndexes) {
        if (approved == null || approved.isEmpty()) return null;
        if (keywords != null && keywords.length > 0) {
            for (int i = 0; i < approved.size(); i++) {
                if (usedIndexes != null && usedIndexes.contains(i)) continue;
                Map<String, Object> entry = approved.get(i);
                String dept = entry.get("departmentName") != null ? entry.get("departmentName").toString().toLowerCase() : "";
                String role = entry.get("role") != null ? entry.get("role").toString().toLowerCase() : "";
                String combined = (dept + " " + role).trim();
                if (combined.isEmpty()) continue;
                boolean allMatch = true;
                for (String kw : keywords) {
                    if (kw == null || kw.isEmpty()) continue;
                    if (!combined.contains(kw.toLowerCase())) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    if (usedIndexes != null) usedIndexes.add(i);
                    return entry;
                }
            }
            // If keywords are provided and no match, do not fallback to index.
            return null;
        }
        // Fallback by level/order if role match isn't found
        if (fallbackIndex >= 0 && fallbackIndex < approved.size()) {
            if (usedIndexes == null || !usedIndexes.contains(fallbackIndex)) {
                if (usedIndexes != null) usedIndexes.add(fallbackIndex);
                return approved.get(fallbackIndex);
            }
        }
        return null;
    }

    private String formatApprovalDate(Object raw) {
        if (raw == null) return "";
        try {
            String s = raw.toString();
            if (s.trim().isEmpty()) return "";
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

    private List<Map<String, Object>> parseApprovalHistory(String approvalHistoryJson) {
        if (approvalHistoryJson == null || approvalHistoryJson.trim().isEmpty()) return new java.util.ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(approvalHistoryJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Error parsing approval history: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    private String findSignatureForUser(List<Map<String, Object>> history, Integer userId, String roleExpected, boolean allowFallback, Map<Integer, String> signatureFromDb) {
        if (userId == null) return "";
        if (history != null && !history.isEmpty()) {
            for (Map<String, Object> entry : history) {
                Object idObj = entry.get("approvedBy");
                if (idObj == null) continue;
                Integer id = idObj instanceof Integer ? (Integer) idObj : Integer.parseInt(idObj.toString());
                if (!id.equals(userId)) continue;
                String role = entry.get("role") != null ? entry.get("role").toString() : "";
                if (roleExpected != null && !roleExpected.equalsIgnoreCase(role)) continue;
                String action = entry.get("action") != null ? entry.get("action").toString() : "";
                if ("REJECTED".equalsIgnoreCase(action)) continue;
                Object sigObj = entry.get("signaturePath");
                if (sigObj == null) return "";
                String sig = sigObj.toString();
                if (!sig.trim().isEmpty()) return sig;
            }
        }
        if (allowFallback && signatureFromDb != null) {
            String sig = signatureFromDb.get(userId);
            return sig != null ? sig : "";
        }
        return "";
    }

    private String resolveDepartmentName(EntityManager entityManager, Integer departmentId, Map<String, Object> pipelineMap) {
        String name = null;
        if (pipelineMap != null) {
            Object nameObj = pipelineMap.get("departmentName");
            if (nameObj == null) nameObj = pipelineMap.get("txtDepartmentName");
            if (nameObj == null) {
                Object deptObj = pipelineMap.get("hrTblDepartment");
                if (deptObj instanceof Map) {
                    Map<?, ?> deptMap = (Map<?, ?>) deptObj;
                    Object nestedName = deptMap.get("txtDepartmentName");
                    if (nestedName == null) nestedName = deptMap.get("departmentName");
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
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept =
                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, departmentId);
            if (dept != null && dept.getTxtDepartmentName() != null && !dept.getTxtDepartmentName().trim().isEmpty()) {
                name = dept.getTxtDepartmentName();
            }
        }

        return name;
    }

    private Map<Integer, String> loadUserSignaturePaths(Integer[] userIds) {
        Map<Integer, String> map = new java.util.HashMap<>();
        if (userIds == null || userIds.length == 0) return map;
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            for (Integer id : userIds) {
                if (id == null || map.containsKey(id)) continue;
                CfgTblUser u = em.find(CfgTblUser.class, id);
                if (u != null && u.getTxtSignaturePath() != null && !u.getTxtSignaturePath().trim().isEmpty()) {
                    map.put(id, u.getTxtSignaturePath());
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.warn("Error loading user signatures: " + e.getMessage(), e);
        } finally {
            if (em.isOpen()) em.close();
        }
        return map;
    }

    private Integer extractUserId(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object idObj = map.get("serUserId");
            if (idObj == null) idObj = map.get("userId");
            if (idObj == null) idObj = map.get("id");
            if (idObj == null) return null;
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
        if (obj == null) return null;
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            return s.isEmpty() ? null : s;
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object nameObj = map.get("txtUserName");
            if (nameObj == null) nameObj = map.get("userName");
            if (nameObj == null) nameObj = map.get("name");
            return nameObj != null ? nameObj.toString() : null;
        }
        return null;
    }

    private Integer resolveUserIdByName(String userName) {
        if (userName == null || userName.trim().isEmpty()) return null;
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
            if (em.isOpen()) em.close();
        }
    }

    private Integer extractUserIdByIndex(Object listObj, int index) {
        if (!(listObj instanceof java.util.List)) return null;
        java.util.List<?> list = (java.util.List<?>) listObj;
        if (index < 0 || index >= list.size()) return null;
        return extractUserId(list.get(index));
    }

    private Object extractUserByIndex(Object listObj, int index) {
        if (!(listObj instanceof java.util.List)) return null;
        java.util.List<?> list = (java.util.List<?>) listObj;
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    private void drawSignatureImage(PDDocument document, PDPageContentStream content, String signaturePath,
                                    float x, float y, float maxWidth, float maxHeight) {
        try {
            String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
            File sigFile = resolveSignatureFile(rootPath, signaturePath);
            if (sigFile == null || !sigFile.exists()) return;
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject img =
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFile(sigFile.getAbsolutePath(), document);
            float imgW = img.getWidth();
            float imgH = img.getHeight();
            if (imgW <= 0 || imgH <= 0) return;
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
        if (signaturePath == null || signaturePath.trim().isEmpty()) return null;
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
        if (fileName == null) return null;
        try {
            // Expected: signature_{userId}_xxxx.ext
            if (!fileName.startsWith("signature_")) return null;
            String rest = fileName.substring("signature_".length());
            int idx = rest.indexOf('_');
            if (idx <= 0) return null;
            String idStr = rest.substring(0, idx);
            return Integer.parseInt(idStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer loadUserDepartmentId(EntityManager em, Integer userId) {
        if (em == null || userId == null) return null;
        try {
            Object result = em.createNativeQuery(
                "select ser_department_id from cfg_tbl_user where ser_user_id = :id")
                .setParameter("id", userId)
                .getSingleResult();
            if (result == null) return null;
            if (result instanceof Number) return ((Number) result).intValue();
            return Integer.parseInt(result.toString());
        } catch (Exception e) {
            log.warn("Error loading user department: " + e.getMessage(), e);
            return null;
        }
    }

    private Integer findDepartmentHeadUserId(EntityManager em, Integer departmentId) {
        if (em == null || departmentId == null) return null;
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
            if (rows == null || rows.isEmpty()) return null;
            Object val = rows.get(0);
            if (val instanceof Number) return ((Number) val).intValue();
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            log.warn("Error finding department head user: " + e.getMessage(), e);
            return null;
        }
    }

    private void drawCenteredHeader(PDPageContentStream content, String text, float x, float width, float y) throws java.io.IOException {
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * 9;
        float tx = x + (width - textWidth) / 2;
        content.beginText();
        content.newLineAtOffset(tx, y);
        content.showText(text);
        content.endText();
    }

    private void drawRect(PDPageContentStream content, float x, float y, float width, float height) throws java.io.IOException {
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws java.io.IOException {
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

    private void drawCentered(PDPageContentStream content, float pageWidth, float y, String text) throws java.io.IOException {
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

    private float drawYesNoRow(PDPageContentStream content, float x, float y, float x2, String label, String value) throws java.io.IOException {
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

    private float drawYesNoNaRow(PDPageContentStream content, float x, float y, float x2, String label, String value) throws java.io.IOException {
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

    private void drawSignatureLineRow(PDPageContentStream content, float x, float y, float x2, String leftLabel, String rightLabel) throws java.io.IOException {
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

    private String trimToWidth(String text, PDType1Font font, float fontSize, float maxWidth) throws java.io.IOException {
        if (text == null) return "";
        String t = text.replaceAll("\\s+", " ").trim();
        while (font.getStringWidth(t) / 1000 * fontSize > maxWidth && t.length() > 0) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private String getCapfValue(Map<String, Object> appData, String key) {
        if (appData == null || key == null) return null;
        String k = key.toLowerCase();
        for (Map.Entry<String, Object> entry : appData.entrySet()) {
            String label = entry.getKey();
            if (label == null) continue;
            String normalized = label.replace(":", "").toLowerCase();
            if (normalized.contains(k)) {
                Object val = entry.getValue();
                if (val != null) return String.valueOf(val);
            }
        }
        return null;
    }

    private String nullSafe(String v) {
        return v != null ? v : "";
    }

    private boolean isCapfForm(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form) {
        if (form == null) return false;
        String name = form.getTxtFormName();
        String code = form.getTxtFormCode();
        if (name != null && name.toLowerCase().contains("capf")) return true;
        if (code != null && code.toLowerCase().startsWith("capf")) return true;
        return false;
    }

    private java.util.Map<String, String> extractUserDisplay(Object obj) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (!(obj instanceof Map)) return result;
        Map<?, ?> map = (Map<?, ?>) obj;
        Object nameObj = map.get("txtUserName");
        if (nameObj == null) nameObj = map.get("userName");
        Object roleObj = null;
        Object roleMap = map.get("cfgTblRole");
        if (roleMap instanceof Map) {
            roleObj = ((Map<?, ?>) roleMap).get("txtRoleName");
        }
        if (roleObj == null) roleObj = map.get("roleName");
        if (nameObj != null) result.put("name", nameObj.toString());
        if (roleObj != null) result.put("role", roleObj.toString());
        return result;
    }

    private java.util.List<java.util.Map<String, String>> extractUserListDisplay(Object obj) {
        java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();
        if (!(obj instanceof java.util.List)) return list;
        for (Object item : (java.util.List<?>) obj) {
            list.add(extractUserDisplay(item));
        }
        return list;
    }

    private String formatUserDisplay(java.util.Map<String, String> data) {
        if (data == null || data.isEmpty()) return "";
        String name = data.getOrDefault("name", "");
        String role = data.getOrDefault("role", "");
        if (!role.isEmpty()) return name + " (" + role + ")";
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
        if (valObj == null) return "";
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

    private static class BudgetApprover {
        Integer userId;
        String name;
        String email;
        String role;
        String signaturePath;
    }

    /**
     * Generate HTML email content with optional approve/reject buttons
     */
    private String generateApprovalEmailHtml(String recipientName, Integer level, String applicationCode, 
                                            String formName, String status, String remarks, 
boolean showActionButtons, String approveUrl, String rejectUrl, String sendBackUrl, String approvalHistoryJson, String baseUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;background-color:#f5f5f5}");
        html.append(".email-container{background-color:#ffffff;border-radius:8px;padding:30px;box-shadow:0 2px 4px rgba(0,0,0,0.1)}");
        html.append(".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:20px;border-radius:8px 8px 0 0;margin:-30px -30px 20px -30px}");
        html.append(".header h1{margin:0;font-size:24px;font-weight:600}");
        html.append(".content{padding:20px 0}");
        html.append(".greeting{font-size:16px;margin-bottom:20px;color:#555}");
        html.append(".details{background-color:#f8f9fa;border-left:4px solid #667eea;padding:15px;margin:20px 0;border-radius:4px}");
        html.append(".detail-row{margin:10px 0;display:flex}");
        html.append(".detail-label{font-weight:600;color:#555;min-width:150px}");
        html.append(".detail-value{color:#333;flex:1}");
        html.append(".remarks-box{background-color:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px}");
        html.append(".button-container{margin:30px 0;text-align:center}");
        html.append(".btn{display:inline-block;padding:12px 30px;margin:0 10px;text-decoration:none;border-radius:6px;font-weight:600;font-size:16px;transition:all 0.3s}");
        html.append(".btn-approve{background-color:#27ae60;color:white}");
        html.append(".btn-approve:hover{background-color:#229954;transform:translateY(-2px);box-shadow:0 4px 8px rgba(39,174,96,0.3)}");
        html.append(".btn-reject{background-color:#e74c3c;color:white}");
        html.append(".btn-reject:hover{background-color:#c0392b;transform:translateY(-2px);box-shadow:0 4px 8px rgba(231,76,60,0.3)}");
        html.append(".btn-sendback{background-color:#f39c12;color:white}");
        html.append(".btn-sendback:hover{background-color:#d68910;transform:translateY(-2px);box-shadow:0 4px 8px rgba(243,156,18,0.3)}");
        html.append(".footer{margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;text-align:center;color:#95a5a6;font-size:12px}");
        html.append(".status-badge{display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:600;text-transform:uppercase}");
        html.append(".status-approved{background-color:#d5f4e6;color:#27ae60}");
        html.append(".status-pending{background-color:#fef5e7;color:#f39c12}");
        html.append(".status-rejected{background-color:#fadbd8;color:#e74c3c}");
        html.append(".status-in-progress{background-color:#d6eaf8;color:#3498db}");
        html.append(".history{margin-top:20px}");
        html.append(".history h3{margin:0 0 10px 0;font-size:16px;color:#333}");
        html.append(".history table{width:100%;border-collapse:collapse;font-size:12px}");
        html.append(".history th,.history td{border:1px solid #e5e7eb;padding:6px 8px;text-align:left;vertical-align:top}");
        html.append(".history th{background:#f3f4f6;font-weight:600}");
        html.append(".sig-img{max-height:36px;display:block;margin-top:4px}");
        html.append("</style></head><body>");
        String headerTitle = (formName != null && !formName.trim().isEmpty()) ? formName.trim() : "Application";
        html.append("<div class='email-container'>");
        html.append("<div class='header'><h1>").append(escapeHtml(headerTitle)).append("</h1></div>");
        html.append("<div class='content'>");
        html.append("<div class='greeting'>Dear ").append(escapeHtml(recipientName)).append(",</div>");
        
        if (showActionButtons) {
            html.append("<p>A new application is pending your approval at Level ").append(level).append(".</p>");
        } else {
            html.append("<p>Your application has been approved at Level ").append(level).append(".</p>");
        }
        
        html.append("<div class='details'>");
        html.append("<div class='detail-row'><div class='detail-label'>Application Code:</div><div class='detail-value'>").append(escapeHtml(applicationCode)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Form Name:</div><div class='detail-value'>").append(escapeHtml(formName)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Approval Level:</div><div class='detail-value'>").append(level).append("</div></div>");
        String statusClass = status != null ? status.toLowerCase().replace("_", "-") : "pending";
        html.append("<div class='detail-row'><div class='detail-label'>Status:</div><div class='detail-value'><span class='status-badge status-").append(statusClass).append("'>").append(escapeHtml(status != null ? status : "PENDING")).append("</span></div></div>");
        if (remarks != null && !remarks.trim().isEmpty()) {
            html.append("<div class='remarks-box'><strong>Remarks:</strong><br>").append(escapeHtml(remarks)).append("</div>");
        }
        html.append("</div>");

        String historyHtml = buildApprovalHistoryHtml(approvalHistoryJson, baseUrl);
        if (historyHtml != null && !historyHtml.isEmpty()) {
            html.append(historyHtml);
        }
        
        boolean isFirstLevel = level != null && level <= 1;
        boolean canApproveReject = showActionButtons && isFirstLevel && approveUrl != null && rejectUrl != null;
        boolean canSendBack = showActionButtons && sendBackUrl != null && (level == null || level >= 1);

        if (showActionButtons && (canApproveReject || canSendBack)) {
            html.append("<div class='button-container'>");
            if (canApproveReject) {
                html.append("<a href='").append(approveUrl).append("' class='btn btn-approve' style='color:white;text-decoration:none;'>✓ Approve Application</a>");
                html.append("<a href='").append(rejectUrl).append("' class='btn btn-reject' style='color:white;text-decoration:none;'>✗ Reject Application</a>");
            }
            if (canSendBack) {
                html.append("<a href='").append(sendBackUrl).append("' class='btn btn-sendback' style='color:white;text-decoration:none;'>Send Back</a>");
            }
            html.append("</div>");
            html.append("<p style='text-align:center;color:#7f8c8d;font-size:12px;margin-top:20px;'>You can also review this application in the system dashboard.</p>");
        } else {
            html.append("<p>Thank you for using our system.</p>");
        }
        
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Best Regards,<br>System Administrator</p>");
        html.append("<p style='font-size:10px;color:#bdc3c7;'>This is an automated email. Please do not reply.</p>");
        html.append("</div>");
        html.append("</div></body></html>");
        
        return html.toString();
    }

    private String buildApprovalHistoryHtml(String approvalHistoryJson, String baseUrl) {
        if (approvalHistoryJson == null || approvalHistoryJson.trim().isEmpty()) return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(approvalHistoryJson, new TypeReference<List<Map<String, Object>>>() {});
            if (list == null || list.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("<div class='history'>");
            sb.append("<h3>Prior Approvals</h3>");
            sb.append("<table>");
            sb.append("<thead><tr>");
            sb.append("<th>Level</th><th>Approver</th><th>Role</th><th>Status</th><th>Date</th><th>Signature</th>");
            sb.append("</tr></thead><tbody>");

            for (Map<String, Object> entry : list) {
                String level = entry.get("level") != null ? String.valueOf(entry.get("level")) : "";
                String name = entry.get("approverName") != null ? String.valueOf(entry.get("approverName")) : "";
                String role = entry.get("role") != null ? String.valueOf(entry.get("role")) : "";
                if (role == null || role.trim().isEmpty()) {
                    role = entry.get("departmentName") != null ? String.valueOf(entry.get("departmentName")) : "";
                }
                String action = entry.get("action") != null ? String.valueOf(entry.get("action")) :
                               entry.get("status") != null ? String.valueOf(entry.get("status")) : "";
                String date = entry.get("approvedDate") != null ? String.valueOf(entry.get("approvedDate")) : "";
                String signaturePath = entry.get("signaturePath") != null ? String.valueOf(entry.get("signaturePath")) : "";
                String approvedBy = entry.get("approvedBy") != null ? String.valueOf(entry.get("approvedBy")) : "";

                String sigHtml = "";
                if (!signaturePath.trim().isEmpty() && approvedBy != null && !approvedBy.trim().isEmpty() && baseUrl != null) {
                    String sigUrl = baseUrl + "/getSignature?userId=" + approvedBy;
                    sigHtml = "<img class='sig-img' src='" + sigUrl + "' alt='Signature' />";
                }

                sb.append("<tr>");
                sb.append("<td>").append(escapeHtml(level)).append("</td>");
                sb.append("<td>").append(escapeHtml(name)).append("</td>");
                sb.append("<td>").append(escapeHtml(role)).append("</td>");
                sb.append("<td>").append(escapeHtml(action)).append("</td>");
                sb.append("<td>").append(escapeHtml(date)).append("</td>");
                sb.append("<td>").append(sigHtml).append("</td>");
                sb.append("</tr>");
            }

            sb.append("</tbody></table></div>");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Error building approval history HTML: " + e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * Generate HTML email for application submission confirmation
     */
    private String generateSubmissionEmailHtml(String recipientName, String applicationCode, 
                                              String formName, String status, String submittedDate) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;background-color:#f5f5f5}");
        html.append(".email-container{background-color:#ffffff;border-radius:8px;padding:30px;box-shadow:0 2px 4px rgba(0,0,0,0.1)}");
        html.append(".header{background:linear-gradient(135deg,#27ae60 0%,#229954 100%);color:white;padding:20px;border-radius:8px 8px 0 0;margin:-30px -30px 20px -30px}");
        html.append(".header h1{margin:0;font-size:24px;font-weight:600}");
        html.append(".content{padding:20px 0}");
        html.append(".greeting{font-size:16px;margin-bottom:20px;color:#555}");
        html.append(".details{background-color:#f8f9fa;border-left:4px solid #27ae60;padding:15px;margin:20px 0;border-radius:4px}");
        html.append(".detail-row{margin:10px 0;display:flex}");
        html.append(".detail-label{font-weight:600;color:#555;min-width:150px}");
        html.append(".detail-value{color:#333;flex:1}");
        html.append(".footer{margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;text-align:center;color:#95a5a6;font-size:12px}");
        html.append("</style></head><body>");
        String headerTitle = (formName != null && !formName.trim().isEmpty()) ? formName.trim() : "Application";
        html.append("<div class='email-container'>");
        html.append("<div class='header'><h1>Application Submitted Successfully</h1></div>");
        html.append("<div class='content'>");
        html.append("<div class='greeting'>Dear ").append(escapeHtml(recipientName)).append(",</div>");
        html.append("<p>Your application has been submitted successfully.</p>");
        html.append("<div class='details'>");
        html.append("<div class='detail-row'><div class='detail-label'>Application Code:</div><div class='detail-value'>").append(escapeHtml(applicationCode)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Form Name:</div><div class='detail-value'>").append(escapeHtml(formName)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Status:</div><div class='detail-value'>").append(escapeHtml(status)).append("</div></div>");
        html.append("<div class='detail-row'><div class='detail-label'>Submitted Date:</div><div class='detail-value'>").append(escapeHtml(submittedDate)).append("</div></div>");
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
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
