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
import java.util.Properties;

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

            // Generate application code based on form's convention if not provided
            if (application.getTxtFormCode() == null || application.getTxtFormCode().trim().isEmpty()) {
                if (application.getSerFormId() != null) {
                    String generatedCode = generateNextApplicationCode(application.getSerFormId(), entityManager);
                    if (generatedCode != null) {
                        application.setTxtFormCode(generatedCode);
                    }
                }
            }

            entityManager.persist(application);
            entityManager.getTransaction().commit();
            
            // Send email notifications after successful submission
            try {
                sendSubmissionEmails(application);
            } catch (Exception emailEx) {
                log.error("Error sending submission emails: " + emailEx.getMessage(), emailEx);
                // Don't fail the submission if email fails
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
            
            // First, find the department where this user is the head
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment department = null;
            try {
                List<com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment> departments = entityManager.createQuery(
                    "SELECT d FROM HrTblDepartment d " +
                    "WHERE d.serDepartmentHeadId = :headUserId " +
                    "AND (d.blIsDeleted = false OR d.blIsDeleted IS NULL)")
                    .setParameter("headUserId", departmentHeadUserId)
                    .getResultList();
                if (!departments.isEmpty()) {
                    department = departments.get(0);
                }
            } catch (Exception e) {
                log.warn("Error finding department for head user " + departmentHeadUserId + ": " + e.getMessage());
            }
            
            if (department == null) {
                entityManager.getTransaction().commit();
                return new java.util.ArrayList<>();
            }
            
            Integer departmentId = department.getSerDepartmentId();
            
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
                    
                    // Find the pipeline entry for this department
                    Integer departmentOrder = null;
                    for (java.util.Map<String, Object> pipeline : pipelines) {
                        Object deptIdObj = pipeline.get("serDepartmentId");
                        if (deptIdObj != null) {
                            Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                            Integer.parseInt(deptIdObj.toString());
                            if (deptId.equals(departmentId)) {
                                Object orderObj = pipeline.get("intApprovalOrder");
                                if (orderObj != null) {
                                    departmentOrder = orderObj instanceof Integer ? (Integer) orderObj : 
                                                    Integer.parseInt(orderObj.toString());
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Check if current approval level matches this department's order
                    // Approval level 0 means first department (order 1), level 1 means second department (order 2), etc.
                    if (departmentOrder != null) {
                        Integer currentLevel = app.getIntCurrentApprovalLevel();
                        if (currentLevel == null) {
                            currentLevel = 0;
                        }
                        // Current level should be (departmentOrder - 1) for this department to be the approver
                        if (currentLevel.equals(departmentOrder - 1)) {
                            filteredApplications.add(app);
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
    public String approveApplication(Integer applicationId, String remarks) {
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
                        // Fetch department name
                        com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept = 
                            entityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, departmentId);
                        if (dept != null) {
                            departmentName = dept.getTxtDepartmentName();
                        }
                    }
                    
                    if (orderObj != null) {
                        pipelineOrder = orderObj instanceof Integer ? (Integer) orderObj : 
                                       Integer.parseInt(orderObj.toString());
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
            
            // Add current approval to history (the level being approved is currentLevel + 1 in terms of pipeline order)
            // But we store the actual pipeline order (1-indexed)
            java.util.Map<String, Object> approvalEntry = new java.util.HashMap<>();
            approvalEntry.put("level", pipelineOrder != null ? pipelineOrder : (currentLevel + 1)); // Pipeline order (1-indexed)
            approvalEntry.put("departmentId", departmentId);
            approvalEntry.put("departmentName", departmentName != null ? departmentName : (departmentId != null ? "Department " + departmentId : "Unknown"));
            approvalEntry.put("remarks", remarks != null ? remarks : "");
            approvalEntry.put("approvedBy", commonService.getCurrentLoggedInUser());
            approvalEntry.put("approvedDate", commonService.getCurrentTimeStamp_new().toString());
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
            
            application.setSerCurrentApprover(commonService.getCurrentLoggedInUser());
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());
            
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
            
            application.setTxtStatus("REJECTED");
            application.setSerCurrentApprover(commonService.getCurrentLoggedInUser());
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
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept = 
                                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, currentDepartmentId);
                            if (dept != null) {
                                currentDepartmentName = dept.getTxtDepartmentName();
                            }
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
                            null
                        );
                        
                        emailService.sendHtmlEmail(java.util.Arrays.asList(submittedByUser.getTxtAddress()), 
                                                  submitterSubject, submitterHtmlMessage);
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
                                // Eagerly fetch department head
                                if (nextDept.getSerDepartmentHeadId() != null) {
                                    CfgTblUser nextDeptHead = emailEntityManager.find(CfgTblUser.class, nextDept.getSerDepartmentHeadId());
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
                                        
                                        String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                            nextDeptHead.getTxtUserName() != null ? nextDeptHead.getTxtUserName() : "Department Head",
                                            nextLevelOrder,
                                            application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                            formName,
                                            application.getTxtStatus(),
                                            null,
                                            true, // For department head
                                            approveUrl,
                                            rejectUrl
                                        );
                                        
                                        emailService.sendHtmlEmail(java.util.Arrays.asList(nextDeptHead.getTxtAddress()), 
                                                                  deptHeadSubject, deptHeadHtmlMessage);
                                        log.info("Approval notification email sent to next level department head: " + nextDeptHead.getTxtAddress());
                                    }
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
            if (pipelines != null && !pipelines.isEmpty()) {
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
                            
                            if (firstDept != null && firstDept.getSerDepartmentHeadId() != null) {
                                CfgTblUser firstDeptHead = emailEntityManager.find(CfgTblUser.class, firstDept.getSerDepartmentHeadId());
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
                                    
                                    String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                        firstDeptHead.getTxtUserName() != null ? firstDeptHead.getTxtUserName() : "Department Head",
                                        firstLevelOrder,
                                        application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                        formName,
                                        application.getTxtStatus(),
                                        null,
                                        true, // For department head
                                        approveUrl,
                                        rejectUrl
                                    );
                                    
                                    emailService.sendHtmlEmail(java.util.Arrays.asList(firstDeptHead.getTxtAddress()), 
                                                              deptHeadSubject, deptHeadHtmlMessage);
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

    /**
     * Generate HTML email content with optional approve/reject buttons
     */
    private String generateApprovalEmailHtml(String recipientName, Integer level, String applicationCode, 
                                            String formName, String status, String remarks, 
                                            boolean showActionButtons, String approveUrl, String rejectUrl) {
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
        html.append(".footer{margin-top:30px;padding-top:20px;border-top:2px solid #ecf0f1;text-align:center;color:#95a5a6;font-size:12px}");
        html.append(".status-badge{display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:600;text-transform:uppercase}");
        html.append(".status-approved{background-color:#d5f4e6;color:#27ae60}");
        html.append(".status-pending{background-color:#fef5e7;color:#f39c12}");
        html.append(".status-rejected{background-color:#fadbd8;color:#e74c3c}");
        html.append(".status-in-progress{background-color:#d6eaf8;color:#3498db}");
        html.append("</style></head><body>");
        html.append("<div class='email-container'>");
        html.append("<div class='header'><h1>Application Notification</h1></div>");
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
        
        if (showActionButtons && approveUrl != null && rejectUrl != null) {
            html.append("<div class='button-container'>");
            html.append("<a href='").append(approveUrl).append("' class='btn btn-approve' style='color:white;text-decoration:none;'>✓ Approve Application</a>");
            html.append("<a href='").append(rejectUrl).append("' class='btn btn-reject' style='color:white;text-decoration:none;'>✗ Reject Application</a>");
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

