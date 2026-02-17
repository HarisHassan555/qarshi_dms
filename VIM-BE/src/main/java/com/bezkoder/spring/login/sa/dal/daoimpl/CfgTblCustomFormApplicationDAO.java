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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
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
                "WHERE a.serSubmittedBy = :userId " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                "ORDER BY a.serApplicationId DESC")
                .setParameter("userId", userId)
                .setMaxResults(500)
                .getResultList();
            entityManager.getTransaction().commit();
            return applications;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting applications by user ID: " + e.getMessage(), e);
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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
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
            
            // Get all applications with PENDING or IN_PROGRESS status
            List<CfgTblCustomFormApplication> allPendingApplications = entityManager.createQuery(
                "SELECT a FROM CfgTblCustomFormApplication a " +
                "WHERE a.txtStatus IN ('PENDING','IN_PROGRESS') " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL)")
                .setMaxResults(1000)
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

            if (currentLevel <= 0) {
                entityManager.getTransaction().rollback();
                return "Failure: Cannot send back from the first approval level";
            }
            
            // Get current department info (the department that is sending back)
            java.util.Map<String, Object> currentDepartmentPipeline = null;
            Integer currentDepartmentId = null;
            String currentDepartmentName = null;
            Integer currentPipelineOrder = null;
            
            if (!pipelines.isEmpty() && currentLevel < pipelines.size()) {
                // Get the department at the current level (0-indexed)
                int currentLevelIndex = currentLevel;
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
            sendBackEntry.put("level", currentPipelineOrder != null ? currentPipelineOrder : (currentLevel + 1));
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
            currentLevel--;
            
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

            try {
                sendBackEmails(application, currentLevel, pipelines, remarks);
            } catch (Exception emailEx) {
                log.error("Error sending send-back emails: " + emailEx.getMessage(), emailEx);
            }
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
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm formForEmail = getFormForEmail(application, emailEntityManager);
            if (formForEmail != null && formForEmail.getTxtFormName() != null) {
                formName = formForEmail.getTxtFormName();
            }
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelinesForEmail =
                loadFormApprovalPipelines(formForEmail != null ? formForEmail.getSerFormId() : null, emailEntityManager);
            String formHtml = buildFormHtmlForEmail(application, formForEmail, pipelinesForEmail);
            
            // Get form name
            if (application.getCfgTblCustomForm() != null && application.getCfgTblCustomForm().getTxtFormName() != null) {
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
                            formHtml
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
                                        
                                        String sendBackUrl = null;
                                        if (nextLevelOrder != null && nextLevelOrder > 1) {
                                            sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                          "&userId=" + nextDeptHead.getSerUserId();
                                        }

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
                                            formHtml
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
            
            // Fetch the form with fields for email rendering
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form = getFormForEmail(application, emailEntityManager);
            String formName = "Unknown Form";
            if (form != null && form.getTxtFormName() != null) {
                formName = form.getTxtFormName();
            }
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelinesForEmail =
                loadFormApprovalPipelines(form != null ? form.getSerFormId() : null, emailEntityManager);
            String formHtml = buildFormHtmlForEmail(application, form, pipelinesForEmail);
            
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
                            application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : "N/A",
                            formHtml
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
                                    
                                    String sendBackUrl = null;
                                    if (firstLevelOrder != null && firstLevelOrder > 1) {
                                        sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                      "&userId=" + firstDeptHead.getSerUserId();
                                    }

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
                                        formHtml
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
     * Send email notifications when an application is sent back
     * Sends email to:
     * 1. The user who submitted the application
     * 2. The previous level department head (new current level)
     */
    private void sendBackEmails(CfgTblCustomFormApplication application, Integer currentLevel,
                                List<java.util.Map<String, Object>> pipelines, String remarks) {
        EntityManager emailEntityManager = getEntityManager();
        try {
            String formName = "Unknown Form";
            com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm formForEmail = getFormForEmail(application, emailEntityManager);
            if (formForEmail != null && formForEmail.getTxtFormName() != null) {
                formName = formForEmail.getTxtFormName();
            }
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelinesForEmail =
                loadFormApprovalPipelines(formForEmail != null ? formForEmail.getSerFormId() : null, emailEntityManager);
            String formHtml = buildFormHtmlForEmail(application, formForEmail, pipelinesForEmail);

            // 1. Notify submitter
            if (application.getSerSubmittedBy() != null) {
                try {
                    CfgTblUser submittedByUser = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                    if (submittedByUser != null && submittedByUser.getTxtAddress() != null &&
                        !submittedByUser.getTxtAddress().trim().isEmpty()) {

                        String submitterSubject = "Application Sent Back - " +
                                               (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtmlMessage = generateApprovalEmailHtml(
                            submittedByUser.getTxtUserName() != null ? submittedByUser.getTxtUserName() : "User",
                            currentLevel != null ? currentLevel + 1 : 1,
                            application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                            formName,
                            application.getTxtStatus(),
                            remarks,
                            false,
                            null,
                            null,
                            null,
                            formHtml
                        );

                        emailService.sendHtmlEmail(java.util.Arrays.asList(submittedByUser.getTxtAddress()),
                                                  submitterSubject, submitterHtmlMessage);
                        log.info("Send-back notification email sent to submitter: " + submittedByUser.getTxtAddress());
                    }
                } catch (Exception e) {
                    log.error("Error sending send-back email to submitter: " + e.getMessage(), e);
                }
            }

            // 2. Notify previous level department head (currentLevel is 0-indexed)
            if (pipelines != null && !pipelines.isEmpty() && currentLevel != null && currentLevel >= 0 && currentLevel < pipelines.size()) {
                try {
                    java.util.Map<String, Object> levelPipeline = pipelines.get(currentLevel);
                    if (levelPipeline != null) {
                        Object deptIdObj = levelPipeline.get("serDepartmentId");
                        if (deptIdObj != null) {
                            Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj :
                                            Integer.parseInt(deptIdObj.toString());

                            emailEntityManager.getTransaction().begin();
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept =
                                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, deptId);

                            if (dept != null && dept.getSerDepartmentHeadId() != null) {
                                CfgTblUser deptHead = emailEntityManager.find(CfgTblUser.class, dept.getSerDepartmentHeadId());
                                if (deptHead != null && deptHead.getTxtAddress() != null &&
                                    !deptHead.getTxtAddress().trim().isEmpty()) {

                                    Object orderObj = levelPipeline.get("intApprovalOrder");
                                    Integer levelOrder = orderObj != null ?
                                        (orderObj instanceof Integer ? (Integer) orderObj :
                                         Integer.parseInt(orderObj.toString())) : (currentLevel + 1);

                                    String baseUrl = getBaseUrl();
                                    String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                      "&userId=" + deptHead.getSerUserId();
                                    String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                     "&userId=" + deptHead.getSerUserId();
                                    String sendBackUrl = null;
                                    if (levelOrder != null && levelOrder > 1) {
                                        sendBackUrl = baseUrl + "/sendBackApplicationFromEmail?applicationId=" + application.getSerApplicationId() +
                                                    "&userId=" + deptHead.getSerUserId();
                                    }

                                    String deptHeadSubject = "Application Sent Back - Level " + levelOrder + " - " +
                                                           (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                                    String deptHeadHtmlMessage = generateApprovalEmailHtml(
                                        deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "Department Head",
                                        levelOrder,
                                        application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A",
                                        formName,
                                        application.getTxtStatus(),
                                        remarks,
                                        true,
                                        approveUrl,
                                        rejectUrl,
                                        sendBackUrl,
                                        formHtml
                                    );

                                    emailService.sendHtmlEmail(java.util.Arrays.asList(deptHead.getTxtAddress()),
                                                              deptHeadSubject, deptHeadHtmlMessage);
                                    log.info("Send-back notification email sent to department head: " + deptHead.getTxtAddress());
                                }
                            }
                            emailEntityManager.getTransaction().commit();
                        }
                    }
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.error("Error sending send-back email to department head: " + e.getMessage(), e);
                }
            }
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
                                            boolean showActionButtons, String approveUrl, String rejectUrl,
                                            String sendBackUrl, String formHtml) {
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
        html.append(".form-section{margin:25px 0;padding:15px;border:1px solid #e0e6ed;border-radius:6px;background:#ffffff}");
        html.append(".form-title{font-weight:600;margin-bottom:10px;color:#333;font-size:16px}");
        html.append(".form-row{margin:8px 0}");
        html.append(".form-label{font-weight:600;color:#555;display:block;margin-bottom:4px}");
        html.append(".form-value{color:#333;white-space:pre-wrap}");
        html.append(".form-table{border-collapse:collapse;width:100%;margin-top:8px}");
        html.append(".form-table th,.form-table td{border:1px solid #e0e6ed;padding:6px 8px;font-size:12px;text-align:left}");
        html.append(".form-table th{background:#f8f9fa;font-weight:600}");
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
        
        if (formHtml != null && !formHtml.trim().isEmpty()) {
            boolean isAbc = formHtml.contains("abc-wrapper");
            if (isAbc) {
                html.append("<div style='margin-top:20px;'>");
                html.append(formHtml);
                html.append("</div>");
            } else {
                html.append("<div class='form-section'>");
                html.append("<div class='form-title'>Application Details</div>");
                html.append(formHtml);
                html.append("</div>");
            }
        }
        
        if (showActionButtons && approveUrl != null && rejectUrl != null) {
            html.append("<div class='button-container'>");
            html.append("<a href='").append(approveUrl).append("' class='btn btn-approve' style='color:white;text-decoration:none;'>✓ Approve Application</a>");
            html.append("<a href='").append(rejectUrl).append("' class='btn btn-reject' style='color:white;text-decoration:none;'>✗ Reject Application</a>");
            if (sendBackUrl != null && level != null && level > 1) {
                html.append("<a href='").append(sendBackUrl).append("' class='btn btn-sendback' style='color:white;text-decoration:none;'>← Send Back</a>");
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
    
    /**
     * Generate HTML email for application submission confirmation
     */
    private String generateSubmissionEmailHtml(String recipientName, String applicationCode, 
                                              String formName, String status, String submittedDate,
                                              String formHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;background-color:#f5f5f5}");
        html.append(".email-container{background-color:#ffffff;border-radius:8px;padding:30px;box-shadow:0 2px 4px rgba(0,0,0,0.1)}");
        html.append(".header{background:linear-gradient(135deg,#27ae60 0%,#229954 100%);color:white;padding:20px;border-radius:8px 8px 0 0;margin:-30px -30px 20px -30px}");
        html.append(".header h1{margin:0;font-size:24px;font-weight:600}");
        html.append(".content{padding:20px 0}");
        html.append(".greeting{font-size:16px;margin-bottom:20px;color:#555}");
        html.append(".form-section{margin:25px 0;padding:15px;border:1px solid #e0e6ed;border-radius:6px;background:#ffffff}");
        html.append(".form-title{font-weight:600;margin-bottom:10px;color:#333;font-size:16px}");
        html.append(".form-row{margin:8px 0}");
        html.append(".form-label{font-weight:600;color:#555;display:block;margin-bottom:4px}");
        html.append(".form-value{color:#333;white-space:pre-wrap}");
        html.append(".form-table{border-collapse:collapse;width:100%;margin-top:8px}");
        html.append(".form-table th,.form-table td{border:1px solid #e0e6ed;padding:6px 8px;font-size:12px;text-align:left}");
        html.append(".form-table th{background:#f8f9fa;font-weight:600}");
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
        if (formHtml != null && !formHtml.trim().isEmpty()) {
            boolean isAbc = formHtml.contains("abc-wrapper");
            if (isAbc) {
                html.append("<div style='margin-top:20px;'>");
                html.append(formHtml);
                html.append("</div>");
            } else {
                html.append("<div class='form-section'>");
                html.append("<div class='form-title'>Application Details</div>");
                html.append(formHtml);
                html.append("</div>");
            }
        }
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

    private com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm getFormForEmail(
            CfgTblCustomFormApplication application, EntityManager entityManager) {
        if (application == null) {
            return null;
        }
        Integer formId = application.getSerFormId();
        if (formId == null && application.getCfgTblCustomForm() != null) {
            try {
                formId = application.getCfgTblCustomForm().getSerFormId();
            } catch (Exception e) {
                formId = null;
            }
        }
        if (formId == null) {
            return application.getCfgTblCustomForm();
        }
        try {
            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm> forms = entityManager.createQuery(
                "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                "LEFT JOIN FETCH f.cfgTblCustomFormFields " +
                "WHERE f.serFormId = :formId",
                com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm.class)
                .setParameter("formId", formId)
                .getResultList();
            if (!forms.isEmpty()) {
                return forms.get(0);
            }
        } catch (Exception e) {
            log.warn("Error loading form for email: " + e.getMessage());
        }
        return application.getCfgTblCustomForm();
    }

    private String buildFormHtml(CfgTblCustomFormApplication application,
                                 com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form) {
        if (application == null || form == null || form.getCfgTblCustomFormFields() == null) {
            return "";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            if (application.getTxtApplicationData() != null && !application.getTxtApplicationData().trim().isEmpty()) {
                data = mapper.readValue(application.getTxtApplicationData(), java.util.Map.class);
            }

            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField> fields =
                new java.util.ArrayList<>(form.getCfgTblCustomFormFields());
            fields.sort((a, b) -> {
                Integer aOrder = a.getIntFieldOrder() != null ? a.getIntFieldOrder() : 0;
                Integer bOrder = b.getIntFieldOrder() != null ? b.getIntFieldOrder() : 0;
                return aOrder.compareTo(bOrder);
            });

            StringBuilder html = new StringBuilder();
            for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField field : fields) {
                String label = field.getTxtFieldLabel() != null ? field.getTxtFieldLabel() : "Field";
                Object value = getFieldValueFromData(field, data);
                String type = field.getTxtFieldType() != null ? field.getTxtFieldType() : "text";

                html.append("<div class='form-row'>");
                html.append("<span class='form-label'>").append(escapeHtml(label)).append("</span>");

                if ("table".equalsIgnoreCase(type)) {
                    html.append(renderTableField(field, value));
                } else if ("checkbox".equalsIgnoreCase(type)) {
                    String display = booleanDisplay(value);
                    html.append("<div class='form-value'>").append(escapeHtml(display)).append("</div>");
                } else if ("attachment".equalsIgnoreCase(type)) {
                    html.append("<div class='form-value'>").append(escapeHtml(formatAttachment(value))).append("</div>");
                } else {
                    String display = formatDisplayValue(value);
                    html.append("<div class='form-value'>").append(escapeHtml(display)).append("</div>");
                }

                html.append("</div>");
            }
            return html.toString();
        } catch (Exception e) {
            log.warn("Error building form HTML: " + e.getMessage());
            return "";
        }
    }

    private Object getFieldValueFromData(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField field,
                                         java.util.Map<String, Object> data) {
        if (field == null || data == null) return null;
        String label = field.getTxtFieldLabel() != null ? field.getTxtFieldLabel() : "";
        String key = label.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (data.containsKey(key)) return data.get(key);
        if (data.containsKey(label)) return data.get(label);
        if (field.getSerFieldId() != null) {
            String fieldIdKey = "field_" + field.getSerFieldId();
            if (data.containsKey(fieldIdKey)) return data.get(fieldIdKey);
        }
        return null;
    }

    private String renderTableField(com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField field, Object value) {
        int rows = 2;
        int columns = 2;
        java.util.List<String> rowLabels = new java.util.ArrayList<>();
        try {
            if (field.getTxtFieldOptions() != null && !field.getTxtFieldOptions().trim().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> config = mapper.readValue(field.getTxtFieldOptions(), java.util.Map.class);
                Object rowsObj = config.get("rows");
                Object colsObj = config.get("columns");
                if (rowsObj != null) rows = Integer.parseInt(rowsObj.toString());
                if (colsObj != null) columns = Integer.parseInt(colsObj.toString());
                Object labelsObj = config.get("rowLabels");
                if (labelsObj instanceof java.util.List) {
                    for (Object item : (java.util.List) labelsObj) {
                        if (item != null) rowLabels.add(String.valueOf(item));
                    }
                }
            }
        } catch (Exception e) {
            // Use defaults
        }

        java.util.List<java.util.List<Object>> tableData = new java.util.ArrayList<>();
        if (value instanceof java.util.List) {
            for (Object rowObj : (java.util.List) value) {
                if (rowObj instanceof java.util.List) {
                    tableData.add((java.util.List<Object>) rowObj);
                }
            }
        }

        StringBuilder html = new StringBuilder();
        html.append("<table class='form-table'>");
        html.append("<thead><tr><th></th>");
        for (int c = 0; c < columns; c++) {
            html.append("<th>Column ").append(c + 1).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (int r = 0; r < rows; r++) {
            String rowLabel = rowLabels.size() > r ? rowLabels.get(r) : "Row " + (r + 1);
            html.append("<tr>");
            html.append("<td>").append(escapeHtml(rowLabel)).append("</td>");
            for (int c = 0; c < columns; c++) {
                String cell = "";
                if (r < tableData.size()) {
                    java.util.List<Object> row = tableData.get(r);
                    if (row != null && c < row.size() && row.get(c) != null) {
                        cell = String.valueOf(row.get(c));
                    }
                }
                html.append("<td>").append(escapeHtml(cell)).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String booleanDisplay(Object value) {
        if (value == null) return "No";
        if (value instanceof Boolean) return ((Boolean) value) ? "Yes" : "No";
        String v = String.valueOf(value).trim().toLowerCase();
        return ("true".equals(v) || "1".equals(v) || "yes".equals(v)) ? "Yes" : "No";
    }

    private String formatAttachment(Object value) {
        if (value == null) return "-";
        if (value instanceof java.util.List) {
            java.util.List<String> items = new java.util.ArrayList<>();
            for (Object item : (java.util.List) value) {
                String formatted = formatAttachment(item);
                if (formatted != null && !formatted.trim().isEmpty() && !"-".equals(formatted)) {
                    items.add(formatted);
                }
            }
            return items.isEmpty() ? "-" : String.join(", ", items);
        }
        if (value instanceof java.util.Map) {
            java.util.Map map = (java.util.Map) value;
            Object name = map.get("name");
            Object size = map.get("size");
            String sizeText = "";
            if (size != null) {
                try {
                    long bytes = Long.parseLong(String.valueOf(size));
                    if (bytes < 1024) sizeText = bytes + " B";
                    else if (bytes < 1024 * 1024) sizeText = String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
                    else sizeText = String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
                } catch (Exception e) {
                    sizeText = String.valueOf(size);
                }
            }
            if (name != null) {
                return String.valueOf(name) + (sizeText.isEmpty() ? "" : " (" + sizeText + ")");
            }
        }
        return String.valueOf(value);
    }

    private String formatDisplayValue(Object value) {
        if (value == null) return "-";
        if (value instanceof java.util.List) {
            java.util.List<String> items = new java.util.ArrayList<>();
            for (Object item : (java.util.List) value) {
                String display = formatDisplayValue(item);
                if (display != null && !display.trim().isEmpty() && !"-".equals(display)) {
                    items.add(display);
                }
            }
            return items.isEmpty() ? "-" : String.join(", ", items);
        }
        if (value instanceof java.util.Map) {
            java.util.Map map = (java.util.Map) value;
            Object label = map.get("label");
            if (label != null) return String.valueOf(label);
            Object name = map.get("name");
            if (name != null) return String.valueOf(name);
            Object val = map.get("value");
            if (val != null) return String.valueOf(val);
        }
        return String.valueOf(value);
    }

    private List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> loadFormApprovalPipelines(
            Integer formId, EntityManager entityManager) {
        if (formId == null) {
            return new java.util.ArrayList<>();
        }
        try {
            return entityManager.createQuery(
                "SELECT p FROM CfgTblCustomFormApprovalPipeline p " +
                "LEFT JOIN FETCH p.hrTblDepartment " +
                "WHERE p.cfgTblCustomForm.serFormId = :formId " +
                "AND (p.blIsDeleted = false OR p.blIsDeleted IS NULL) " +
                "ORDER BY p.intApprovalOrder",
                com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline.class)
                .setParameter("formId", formId)
                .getResultList();
        } catch (Exception e) {
            log.warn("Error loading approval pipelines for email: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    private boolean isCapfForm(CfgTblCustomFormApplication application,
                               com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form) {
        String formCode = application != null ? application.getTxtFormCode() : null;
        String formName = form != null ? form.getTxtFormName() : null;
        if (formCode != null && formCode.trim().toUpperCase().startsWith("CAPF")) {
            return true;
        }
        if (formName != null && formName.trim().equalsIgnoreCase("CAPF FORM")) {
            return true;
        }
        return false;
    }

    private String buildFormHtmlForEmail(CfgTblCustomFormApplication application,
                                         com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
                                         List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines) {
        if (isCapfForm(application, form)) {
            return buildCapfAbcHtml(application, form, pipelines != null ? pipelines : new java.util.ArrayList<>());
        }
        return buildFormHtml(application, form);
    }

    private String buildCapfAbcHtml(CfgTblCustomFormApplication application,
                                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm form,
                                    List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines) {
        if (application == null) {
            return "";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            if (application.getTxtApplicationData() != null && !application.getTxtApplicationData().trim().isEmpty()) {
                data = mapper.readValue(application.getTxtApplicationData(), java.util.Map.class);
            }

            List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField> fields =
                form != null && form.getCfgTblCustomFormFields() != null
                    ? new java.util.ArrayList<>(form.getCfgTblCustomFormFields())
                    : new java.util.ArrayList<>();

            java.util.Map<String, String> templateKeyToConcept = new java.util.HashMap<>();
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

            java.util.Map<String, java.util.List<String>> fieldMappings = new java.util.HashMap<>();
            fieldMappings.put("division", java.util.Arrays.asList("DIVISION / DEPARTMENT", "Division", "Department", "division"));
            fieldMappings.put("capfNumber", java.util.Arrays.asList("CAPF #", "CAPF", "Capf Number", "capf_number"));
            fieldMappings.put("date", java.util.Arrays.asList("Date", "Submission Date", "date"));
            fieldMappings.put("assetName", java.util.Arrays.asList("NAME OF ASSET / ITEM", "Name of Asset", "Asset Name", "Item Name", "asset_name"));
            fieldMappings.put("specification", java.util.Arrays.asList("DETAIL SPECIFICATION", "DETAIL SPECIFICATION:", "Detail Specification", "Detail Specification:", "Specification", "specification", "detail_specification", "DETAIL_SPECIFICATION"));
            fieldMappings.put("utility", java.util.Arrays.asList("UTILITY & PURPOSE", "Utility", "Purpose", "utility_purpose"));
            fieldMappings.put("feasibilityReport", java.util.Arrays.asList("FEASIBILITY REPORT ATTACHED", "Feasibility Report", "feasibility_report"));
            fieldMappings.put("reason", java.util.Arrays.asList("IF NO THEN MENTION REASON", "Reason", "If No Reason", "reason"));
            fieldMappings.put("vendorName", java.util.Arrays.asList("Vendor Name", "Vendor", "Name of Vendor", "NAME"));
            fieldMappings.put("vendorAddress", java.util.Arrays.asList("Vendor Address", "Address", "ADDRESS"));
            fieldMappings.put("approvedPrice", java.util.Arrays.asList("APPROVED PRICE", "Approved Price", "Price", "Cost"));
            fieldMappings.put("deliveryPeriod", java.util.Arrays.asList("DELIVERY PERIOD & DATE", "Delivery Period", "Delivery Date"));
            fieldMappings.put("termsConditions", java.util.Arrays.asList("TERMS & CONDITIONS", "Terms and Conditions", "Terms & Conditions"));
            fieldMappings.put("thirdPartyAssessment", java.util.Arrays.asList("Third Party assessment carried out", "Third Party Assessment", "Third Party assessment", "Third Party Assessment Carried Out", "third_party_assessment", "thirdPartyAssessment"));

            java.util.Map<String, java.util.List<String>> conceptKeywords = new java.util.HashMap<>();
            conceptKeywords.put("specification", java.util.Arrays.asList("specification", "detail", "spec"));
            conceptKeywords.put("division", java.util.Arrays.asList("division", "dept", "department"));
            conceptKeywords.put("assetName", java.util.Arrays.asList("asset", "item", "name"));
            conceptKeywords.put("utility", java.util.Arrays.asList("utility", "purpose"));
            conceptKeywords.put("vendorName", java.util.Arrays.asList("vendor", "name"));
            conceptKeywords.put("vendorAddress", java.util.Arrays.asList("address", "vendor"));
            conceptKeywords.put("approvedPrice", java.util.Arrays.asList("price", "approved", "cost"));
            conceptKeywords.put("deliveryPeriod", java.util.Arrays.asList("delivery", "period", "date"));
            conceptKeywords.put("termsConditions", java.util.Arrays.asList("terms", "conditions"));
            conceptKeywords.put("thirdPartyAssessment", java.util.Arrays.asList("third", "party", "assessment", "carried"));

            java.util.function.Function<String, String> slugify = (label) -> {
                if (label == null) return "";
                return label.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            };

            java.util.function.Function<String, String> normalize = (label) -> {
                if (label == null) return "";
                return label.replaceAll("[:;]", "").trim();
            };

            java.util.function.Function<String, String> lookupLabel = (lbl) -> {
                if (lbl == null) return null;
                String normalizedLbl = normalize.apply(lbl);
                if (data.containsKey(lbl) && data.get(lbl) != null && !"".equals(String.valueOf(data.get(lbl)))) {
                    return String.valueOf(data.get(lbl));
                }
                if (data.containsKey(normalizedLbl) && data.get(normalizedLbl) != null && !"".equals(String.valueOf(data.get(normalizedLbl)))) {
                    return String.valueOf(data.get(normalizedLbl));
                }
                String slug = slugify.apply(lbl);
                if (data.containsKey(slug) && data.get(slug) != null && !"".equals(String.valueOf(data.get(slug)))) {
                    return String.valueOf(data.get(slug));
                }
                String normalizedSlug = slugify.apply(normalizedLbl);
                if (data.containsKey(normalizedSlug) && data.get(normalizedSlug) != null && !"".equals(String.valueOf(data.get(normalizedSlug)))) {
                    return String.valueOf(data.get(normalizedSlug));
                }
                String lowerLbl = lbl.toLowerCase().trim();
                for (String key : data.keySet()) {
                    if (key != null && key.toLowerCase().trim().equals(lowerLbl)) {
                        Object v = data.get(key);
                        if (v != null && !"".equals(String.valueOf(v))) return String.valueOf(v);
                    }
                }
                String lowerNormalized = normalizedLbl.toLowerCase().trim();
                for (String key : data.keySet()) {
                    if (key == null) continue;
                    String keyNormalized = key.replaceAll("[:;]", "").toLowerCase().trim();
                    if (keyNormalized.equals(lowerNormalized)) {
                        Object v = data.get(key);
                        if (v != null && !"".equals(String.valueOf(v))) return String.valueOf(v);
                    }
                }
                if (fields != null && !fields.isEmpty()) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField exactField = null;
                    for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField f : fields) {
                        if (f.getTxtFieldLabel() == null) continue;
                        String fieldLabelNormalized = normalize.apply(f.getTxtFieldLabel()).toLowerCase().trim();
                        if (fieldLabelNormalized.equals(lowerNormalized) || f.getTxtFieldLabel().toLowerCase().trim().equals(lowerLbl)) {
                            exactField = f;
                            break;
                        }
                    }
                    if (exactField != null) {
                        String fieldSlug = slugify.apply(exactField.getTxtFieldLabel());
                        if (data.containsKey(fieldSlug) && data.get(fieldSlug) != null && !"".equals(String.valueOf(data.get(fieldSlug)))) {
                            return String.valueOf(data.get(fieldSlug));
                        }
                        String normalizedFieldSlug = slugify.apply(normalize.apply(exactField.getTxtFieldLabel()));
                        if (data.containsKey(normalizedFieldSlug) && data.get(normalizedFieldSlug) != null && !"".equals(String.valueOf(data.get(normalizedFieldSlug)))) {
                            return String.valueOf(data.get(normalizedFieldSlug));
                        }
                    }
                    if (lowerNormalized.contains("specification") || lowerLbl.contains("specification")) {
                        for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField f : fields) {
                            if (f.getTxtFieldLabel() == null) continue;
                            String fieldLabelLower = f.getTxtFieldLabel().toLowerCase();
                            if (fieldLabelLower.contains("specification") || fieldLabelLower.contains("detail")) {
                                String specSlug = slugify.apply(f.getTxtFieldLabel());
                                if (data.containsKey(specSlug) && data.get(specSlug) != null && !"".equals(String.valueOf(data.get(specSlug)))) {
                                    return String.valueOf(data.get(specSlug));
                                }
                            }
                        }
                    }
                }
                return null;
            };

            java.util.function.Function<String, String> getFieldValue = (fieldLabel) -> {
                String concept = templateKeyToConcept.get(fieldLabel);
                if (concept != null && fieldMappings.containsKey(concept)) {
                    for (String label : fieldMappings.get(concept)) {
                        String val = lookupLabel.apply(label);
                        if (val != null && !val.isEmpty()) return val;
                    }
                    if (fields != null && !fields.isEmpty() && conceptKeywords.containsKey(concept)) {
                        java.util.List<String> keywords = conceptKeywords.get(concept);
                        for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField f : fields) {
                            if (f.getTxtFieldLabel() == null) continue;
                            String fieldLabelLower = f.getTxtFieldLabel().toLowerCase();
                            boolean matches = false;
                            for (String kw : keywords) {
                                if (fieldLabelLower.contains(kw)) {
                                    matches = true;
                                    break;
                                }
                            }
                            if (matches) {
                                String fieldSlug = slugify.apply(f.getTxtFieldLabel());
                                if (data.containsKey(fieldSlug) && data.get(fieldSlug) != null && !"".equals(String.valueOf(data.get(fieldSlug)))) {
                                    return String.valueOf(data.get(fieldSlug));
                                }
                            }
                        }
                    }
                }
                String val = lookupLabel.apply(fieldLabel);
                return val != null && !val.isEmpty() ? val : "";
            };

            String capfNumber = getFieldValue.apply("CAPF #");
            if (capfNumber == null || capfNumber.isEmpty()) {
                capfNumber = application.getTxtFormCode() != null ? application.getTxtFormCode() : "";
            }

            String dateValue = getFieldValue.apply("Date");
            if (dateValue == null || dateValue.isEmpty()) {
                if (application.getDteCreatedDate() != null) {
                    dateValue = String.valueOf(application.getDteCreatedDate());
                }
            }
            String formattedDate = "";
            if (dateValue != null && !dateValue.isEmpty()) {
                try {
                    java.util.Date date;
                    if (dateValue.matches("^\\d+$")) {
                        date = new java.util.Date(Long.parseLong(dateValue));
                    } else {
                        try {
                            java.time.Instant instant = java.time.Instant.parse(dateValue);
                            date = java.util.Date.from(instant);
                        } catch (Exception e) {
                            try {
                                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(dateValue);
                                date = java.util.Date.from(odt.toInstant());
                            } catch (Exception e2) {
                                date = new java.util.Date();
                            }
                        }
                    }
                    formattedDate = new java.text.SimpleDateFormat("M/d/yyyy").format(date);
                } catch (Exception e) {
                    formattedDate = dateValue;
                }
            }

            String feasibilityValue = getFieldValue.apply("FEASIBILITY REPORT ATTACHED");
            String feasibilityLower = feasibilityValue != null ? feasibilityValue.toLowerCase() : "";
            boolean feasibilityYes = "yes".equals(feasibilityLower) || "true".equals(feasibilityLower);
            boolean feasibilityNo = "no".equals(feasibilityLower) || "false".equals(feasibilityLower);

            String thirdPartyValue = getFieldValue.apply("Third Party assessment carried out");
            if (thirdPartyValue == null || thirdPartyValue.isEmpty()) {
                thirdPartyValue = getFieldValue.apply("Third Party Assessment");
            }
            if (thirdPartyValue == null || thirdPartyValue.isEmpty()) {
                thirdPartyValue = getFieldValue.apply("Third Party assessment");
            }
            String thirdPartyLower = thirdPartyValue != null ? thirdPartyValue.toLowerCase() : "";
            boolean thirdPartyYes = "yes".equals(thirdPartyLower) || "true".equals(thirdPartyLower);
            boolean thirdPartyNo = "no".equals(thirdPartyLower) || "false".equals(thirdPartyLower);
            boolean thirdPartyNA = "na".equals(thirdPartyLower) || "n/a".equals(thirdPartyLower) || "not applicable".equals(thirdPartyLower);

            java.util.function.Function<Integer, Boolean> isSignatureApproved = (signatureIndex) -> {
                Integer currentLevel = application.getIntCurrentApprovalLevel() != null ? application.getIntCurrentApprovalLevel() : 0;
                String status = application.getTxtStatus() != null ? application.getTxtStatus().toUpperCase() : "";
                if ("APPROVED".equals(status)) return true;
                int approvalOrder = signatureIndex + 1;
                return currentLevel >= approvalOrder;
            };

            java.util.function.Function<java.util.List<String>, Boolean> isSignatureApprovedByDept = (keywords) -> {
                if (pipelines == null || pipelines.isEmpty()) return false;
                Integer currentLevel = application.getIntCurrentApprovalLevel() != null ? application.getIntCurrentApprovalLevel() : 0;
                String status = application.getTxtStatus() != null ? application.getTxtStatus().toUpperCase() : "";
                if ("APPROVED".equals(status)) return true;
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline : pipelines) {
                    Integer pipelineOrder = pipeline.getIntApprovalOrder() != null ? pipeline.getIntApprovalOrder() : 0;
                    String deptName = pipeline.getHrTblDepartment() != null ? pipeline.getHrTblDepartment().getTxtDepartmentName() : "";
                    String deptNameLower = deptName != null ? deptName.toLowerCase() : "";
                    boolean matches = false;
                    for (String keyword : keywords) {
                        if (deptNameLower.contains(keyword.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches && currentLevel >= pipelineOrder) return true;
                }
                return false;
            };

            StringBuilder css = new StringBuilder();
            css.append(":root{--ink:#111;--line:#222;}");
            css.append(".abc-wrapper{box-sizing:border-box;}");
            css.append(".abc-wrapper *{box-sizing:border-box;}");
            css.append(".abc-wrapper{margin:0;padding:0;background:#fff;color:var(--ink);font-family:Arial,Helvetica,sans-serif;min-height:100vh;width:100%;position:relative;}");
            css.append(".page{width:820px;margin:0 auto;border:2px solid var(--line);padding:5px;box-sizing:border-box;}");
            css.append(".b{font-weight:700;}.u{text-decoration:underline;}.small{font-size:12px;}.xs{font-size:11px;}.tight{line-height:1.1;}");
            css.append(".mt6{margin-top:6px;}.mt8{margin-top:8px;}.mt10{margin-top:10px;}.mt12{margin-top:12px;}.mt14{margin-top:14px;}");
            css.append(".mb6{margin-bottom:6px;}.mb8{margin-bottom:8px;}.mb10{margin-bottom:10px;}.mb12{margin-bottom:12px;}");
            css.append("table{width:100%!important;border-collapse:collapse;margin:0;box-sizing:border-box;}");
            css.append(".grid{width:100%;table-layout:fixed;margin:0;border-spacing:0;box-sizing:border-box;display:table;}");
            css.append(".grid td,.grid th{border:1px solid var(--line);padding:4px 6px;vertical-align:top;font-size:12px;box-sizing:border-box;}");
            css.append(".grid tr:first-child td{width:calc(100%/3);}");
            css.append(".grid th{font-weight:700;text-align:left;}");
            css.append(".brand-row{display:flex;gap:10px;align-items:flex-start;margin-bottom:2px;}");
            css.append(".logo{display:flex;align-items:center;justify-content:center;}");
            css.append(".logo img{max-width:64px;height:auto;}");
            css.append(".brand-title{font-weight:700;font-size:20px;letter-spacing:.2px;}");
            css.append(".titlebar{border:2px solid var(--line);text-align:center;font-weight:700;padding:4px 8px;margin-top:4px;margin-bottom:4px;letter-spacing:.5px;}");
            css.append(".box{border:2px solid var(--line);padding:5px 5px 4px 5px;margin-top:4px;}");
            css.append(".box-title{text-align:center;font-weight:700;margin:-2px 0 5px 0;letter-spacing:.2px;}");
            css.append(".row{display:flex;gap:12px;align-items:flex-end;margin-top:4px;margin-bottom:5px;}");
            css.append(".field{display:flex;gap:8px;align-items:flex-end;flex:1;min-width:0;}");
            css.append(".label{font-size:13px;font-weight:700;white-space:nowrap;}");
            css.append(".line{flex:1;border-bottom:1px solid var(--line);min-height:22px;min-width:40px;padding-bottom:5px;margin-bottom:5px;word-wrap:break-word;overflow-wrap:break-word;white-space:normal;line-height:1.4;}");
            css.append(".line.tall{min-height:26px;}.line.xl{min-height:32px;}");
            css.append(".capf-right{display:flex;align-items:flex-end;gap:12px;white-space:nowrap;margin-left:auto;}");
            css.append(".capf-box{display:flex;align-items:flex-end;gap:8px;}");
            css.append(".capf-num{font-weight:700;font-size:20px;letter-spacing:2px;border-bottom:1px solid var(--line);padding:0 6px 5px 6px;min-width:86px;text-align:right;margin-bottom:5px;}");
            css.append(".date-line{width:150px;border-bottom:1px solid var(--line);min-height:22px;padding-bottom:5px;margin-bottom:5px;word-wrap:break-word;overflow-wrap:break-word;white-space:normal;line-height:1.4;}");
            css.append(".checks{display:flex;align-items:center;gap:14px;margin:8px 0;font-size:13px;font-weight:700;}");
            css.append(".checks .label{flex-shrink:0;white-space:nowrap;}");
            css.append(".check-group{flex:1;display:flex;align-items:center;justify-content:center;gap:40px;}");
            css.append(".check{display:flex;align-items:center;gap:8px;font-weight:700;}");
            css.append(".boxcheck{width:60px;height:22px;border:1px solid var(--line);border-radius:4px;display:inline-block;position:relative;background:transparent;}");
            css.append(".boxcheck.checked::after{content:'\\\\2713';position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);font-size:16px;font-weight:bold;color:#000;}");
            css.append(".note{margin-top:10px;font-size:12px;}");
            css.append(".note ol{margin:6px 0 0 18px;padding:0;}");
            css.append(".note li{margin:2px 0;}");
            css.append(".subhead{margin:10px -10px 10px -10px;padding:6px 10px;border-top:2px solid var(--line);font-weight:700;letter-spacing:.2px;}");
            css.append(".paren{font-size:12px;font-weight:normal;}");
            css.append(".sig-row{display:flex;gap:18px;align-items:flex-end;margin-top:30px;flex-wrap:nowrap;}");
            css.append(".sig{flex:1;min-width:0;}");
            css.append(".sig-line{border-bottom:1px solid var(--line);height:18px;margin-bottom:4px;position:relative;display:flex;align-items:center;justify-content:center;}");
            css.append(".sig-label{font-size:12px;font-weight:700;text-align:center;white-space:nowrap;}");
            css.append(".approved{display:flex;justify-content:flex-end;gap:10px;align-items:flex-end;margin-top:8px;}");
            css.append(".approved .who{font-weight:700;white-space:nowrap;}");
            css.append(".approved-sig{display:flex;flex-direction:column;align-items:flex-start;}");
            css.append(".approved .appline{width:150px;border-bottom:1px solid var(--line);height:14px;margin-bottom:2px;}");
            css.append(".approved-sig .who{font-size:12px;font-weight:700;text-align:center;white-space:nowrap;width:100%;}");
            css.append(".part2-title{border-top:2px solid var(--line);margin:4px -5px 4px -5px;padding:4px 10px;text-align:center;font-weight:700;letter-spacing:.2px;}");
            css.append(".two-col{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-top:5px;}");
            css.append(".sign-block{width:46%;display:flex;flex-direction:column;align-items:flex-start;gap:4px;}");
            css.append(".sign-block .sb-label{font-weight:700;font-size:13px;}");
            css.append(".sign-block .sb-line{width:100%;border-bottom:1px solid var(--line);height:14px;}");
            css.append(".sign-block .sb-sub{font-size:11px;font-weight:700;margin-top:-2px;padding-left:4px;}");
            css.append(".job-title{text-align:center;font-weight:700;margin:5px -5px 5px -5px;padding-top:4px;border-top:2px solid var(--line);letter-spacing:.2px;}");
            css.append(".job-text{font-size:13px;font-weight:700;margin-top:4px;line-height:1.35;}");
            css.append(".inline-line{display:inline-block;border-bottom:1px solid var(--line);min-height:22px;vertical-align:baseline;width:140px;margin:0 6px 5px 6px;padding-bottom:5px;word-wrap:break-word;overflow-wrap:break-word;white-space:normal;line-height:1.4;}");
            css.append(".inline-line.short{width:110px;}.inline-line.long{width:190px;}");

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"abc-wrapper\">");
            html.append("<style>").append(css.toString()).append("</style>");
            html.append("<div class=\"page\">");
            html.append("<div class=\"brand-row\">");
            html.append("<div class=\"logo\"><img src=\"assets/images/qarshi-logo.png\" alt=\"\" style=\"max-width:64px;\"></div>");
            html.append("<div class=\"brand-title\">Qarshi Industries (Pvt) Ltd.</div>");
            html.append("</div>");
            html.append("<table class=\"grid\">");
            html.append("<tr>");
            html.append("<td><span class=\"b\">Division:</span> ***</td>");
            html.append("<td><span class=\"b\">Department:</span> PRC</td>");
            html.append("<td><span class=\"b\">Section:</span> GEN</td>");
            html.append("</tr>");
            html.append("<tr>");
            html.append("<td><span class=\"b\">Document No.:</span> PRC-GEN-FM-03</td>");
            html.append("<td><span class=\"b\">Original Issue:</span> 01-06-2006</td>");
            html.append("<td><div style=\"display:flex;justify-content:space-between;gap:10px;\"><span><span class=\"b\">Rev.</span> # 05</span><span><span class=\"b\">Rev. Date:</span> 01-12-2015</span></div></td>");
            html.append("</tr>");
            html.append("</table>");
            html.append("<div class=\"titlebar\">CAPITAL ASSETS PURCHASE FORM</div>");
            html.append("<div class=\"box\">");
            html.append("<div class=\"box-title\">PART-1 (TO BE FILLED BY <span class=\"u\">CONCERNED</span> DEPARTMENT)</div>");
            html.append("<div class=\"row\">");
            html.append("<div class=\"field\"><div class=\"label\">DIVISION / DEPARTMENT:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("DIVISION / DEPARTMENT"))).append("</div></div>");
            html.append("<div class=\"capf-right\" style=\"flex-direction:column;align-items:flex-end;\">");
            html.append("<div class=\"capf-box\" style=\"margin-bottom:8px;\"><div class=\"label\">CAPF #</div><div class=\"capf-num\">")
                .append(escapeHtml(capfNumber)).append("</div></div>");
            html.append("<div class=\"capf-box\"><div class=\"label\">Date:</div><div class=\"date-line\">")
                .append(escapeHtml(formattedDate)).append("</div></div>");
            html.append("</div>");
            html.append("</div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">NAME OF ASSET / ITEM:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("NAME OF ASSET / ITEM"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">DETAIL SPECIFICATION:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("DETAIL SPECIFICATION"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">UTILITY &amp; PURPOSE:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("UTILITY & PURPOSE"))).append("</div></div></div>");
            html.append("<div class=\"checks\"><div class=\"label\">FEASIBILITY REPORT ATTACHED:</div><div class=\"check-group\">");
            html.append("<div class=\"check\">Yes <span class=\"boxcheck ").append(feasibilityYes ? "checked" : "").append("\"></span></div>");
            html.append("<div class=\"check\">No <span class=\"boxcheck ").append(feasibilityNo ? "checked" : "").append("\"></span></div>");
            html.append("</div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">IF NO THEN MENTION REASON:</div><div class=\"line xl\">")
                .append(escapeHtml(getFieldValue.apply("IF NO THEN MENTION REASON"))).append("</div></div></div>");
            html.append("<div class=\"note\"><div class=\"b\">NOTE:</div>");
            html.append("<ol class=\"xs tight\" type=\"i\">");
            html.append("<li>In case of technical item, verification of technical expert is mandatory on feasibility / proposal.</li>");
            html.append("<li>In case of price more than 5 million, third party assessment is mandatory.</li>");
            html.append("<li>Capital Asset Purchase Checklist (PRC-GEN-FM-29) must be completed along with Capital Asset Purchase Form (PRC-GEN-FM-03).</li>");
            html.append("</ol></div>");
            html.append("<div class=\"subhead\">PARTICULARS OF SELECTED VENDOR(S) (AS PER APPROVED QUOTATION)</div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">NAME:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("NAME"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">ADDRESS:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("ADDRESS"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">APPROVED PRICE:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("APPROVED PRICE"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">DELIVERY PERIOD &amp; DATE:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("DELIVERY PERIOD & DATE"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">TERMS &amp; CONDITIONS:</div><div class=\"line\">")
                .append(escapeHtml(getFieldValue.apply("TERMS & CONDITIONS"))).append("</div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label paren\">(Payment, After Sale Service, Warranty etc.)</div><div class=\"line xl\">")
                .append(escapeHtml(getFieldValue.apply("TERMS & CONDITIONS"))).append("</div></div></div>");
            html.append("<div class=\"checks\"><div class=\"label\">Third Party assessment carried out</div><div class=\"check-group\">");
            html.append("<div class=\"check\">Yes <span class=\"boxcheck ").append(thirdPartyYes ? "checked" : "").append("\"></span></div>");
            html.append("<div class=\"check\">No <span class=\"boxcheck ").append(thirdPartyNo ? "checked" : "").append("\"></span></div>");
            html.append("<div class=\"check\">NA <span class=\"boxcheck ").append(thirdPartyNA ? "checked" : "").append("\"></span></div>");
            html.append("</div></div>");
            html.append("<div class=\"sig-row\">");
            html.append("<div class=\"sig\"><div class=\"sig-line\">")
                .append((isSignatureApproved.apply(0) || isSignatureApprovedByDept.apply(java.util.Arrays.asList(\"user\", \"hod\", \"department\", \"head\"))) ? "<span style=\"font-weight:bold;font-size:11px;\">Approved</span>" : "")
                .append("</div><div class=\"sig-label\">User Deptt. (HoD)</div></div>");
            html.append("<div class=\"sig\"><div class=\"sig-line\">")
                .append((isSignatureApproved.apply(1) || isSignatureApprovedByDept.apply(java.util.Arrays.asList(\"technical\", \"expert\"))) ? "<span style=\"font-weight:bold;font-size:11px;\">Approved</span>" : "")
                .append("</div><div class=\"sig-label\">Technical Expert</div></div>");
            html.append("<div class=\"sig\"><div class=\"sig-line\">")
                .append((isSignatureApproved.apply(2) || isSignatureApprovedByDept.apply(java.util.Arrays.asList(\"procurement\"))) ? "<span style=\"font-weight:bold;font-size:11px;\">Approved</span>" : "")
                .append("</div><div class=\"sig-label\">Procurement</div></div>");
            html.append("<div class=\"sig\"><div class=\"sig-line\">")
                .append((isSignatureApproved.apply(3) || isSignatureApprovedByDept.apply(java.util.Arrays.asList(\"finance\"))) ? "<span style=\"font-weight:bold;font-size:11px;\">Approved</span>" : "")
                .append("</div><div class=\"sig-label\">Finance</div></div>");
            html.append("<div class=\"sig\"><div class=\"sig-line\">")
                .append((isSignatureApproved.apply(4) || isSignatureApprovedByDept.apply(java.util.Arrays.asList(\"core team\", \"htr\", \"cct\", \"ho\"))) ? "<span style=\"font-weight:bold;font-size:11px;\">Approved</span>" : "")
                .append("</div><div class=\"sig-label\">Core Team HTR. / CCT HO</div></div>");
            html.append("</div>");
            html.append("<div class=\"xs mt6\"><span class=\"b\">Note:</span> Designation must be mentioned against each signature.</div>");
            html.append("<div class=\"approved\"><div class=\"who b\">Approved By:</div><div class=\"approved-sig\"><div class=\"appline\"></div><div class=\"who b\">Chief Executive</div></div></div>");
            html.append("<div class=\"part2-title\">PART-2 (TO BE FILLED BY PROCUREMENT DEPARTMENT)</div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">P. O. No. WITH DATE:</div><div class=\"line\"></div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">PARTICULARS OF VENDOR(S):</div><div class=\"line\"></div></div></div>");
            html.append("<div class=\"row\"><div class=\"field\"><div class=\"label\">DELIVERY DATE:</div><div class=\"line\" style=\"max-width:220px;\"></div></div></div>");
            html.append("<div class=\"two-col\">");
            html.append("<div class=\"sign-block\"><div class=\"sb-label\">Checked By:</div><div class=\"sb-line\"></div><div class=\"sb-sub\">(Sign &amp; Desg.)</div></div>");
            html.append("<div class=\"sign-block\" style=\"align-items:flex-end;\"><div class=\"sb-label\">Verified By:</div><div class=\"sb-line\"></div><div class=\"sb-sub\">(Sign &amp; Desg.)</div></div>");
            html.append("</div>");
            html.append("<div class=\"job-title\">(JOB COMPLETION CERTIFICATE)</div>");
            html.append("<div class=\"job-text\">THIS IS TO CERTIFY THAT JOB AGAINST CAPF # <span class=\"inline-line\">")
                .append(escapeHtml(capfNumber)).append("</span> DATED <span class=\"inline-line short\">")
                .append(escapeHtml(formattedDate)).append("</span> HAS BEEN COMPLETED.</div>");
            html.append("<div class=\"job-text\">GRN #: <span class=\"inline-line short\"></span> DATED <span class=\"inline-line long\"></span> (REPORT ATTACHED)</div>");
            html.append("<div class=\"two-col\">");
            html.append("<div class=\"sign-block\"><div class=\"sb-label\">Checked By:</div><div class=\"sb-line\"></div><div class=\"sb-sub\">(Sign &amp; Desg.)</div></div>");
            html.append("<div class=\"sign-block\" style=\"align-items:flex-end;\"><div class=\"sb-label\">Verified By:</div><div class=\"sb-line\"></div><div class=\"sb-sub\">(Sign &amp; Desg.)</div></div>");
            html.append("</div>");
            html.append("</div>");
            html.append("</div>");
            html.append("</div>");
            return html.toString();
        } catch (Exception e) {
            log.warn("Error building CAPF ABC HTML: " + e.getMessage());
            return "";
        }
    }
}
