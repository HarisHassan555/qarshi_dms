package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import javax.persistence.*;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;

@Repository
public class CfgTblCustomFormApplicationDAO implements ICfgTblCustomFormApplicationDAO {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    private static final Logger log = LoggerFactory.getLogger(CfgTblCustomFormApplicationDAO.class);

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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
                "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                "WHERE a.serSubmittedBy = :userId " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                "ORDER BY a.dteCreatedDate DESC")
                .setParameter("userId", userId)
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
                "SELECT DISTINCT a FROM CfgTblCustomFormApplication a " +
                "LEFT JOIN FETCH a.cfgTblCustomForm f " +
                "WHERE (a.txtStatus = 'PENDING' OR a.txtStatus = 'IN_PROGRESS') " +
                "AND (a.blIsDeleted = false OR a.blIsDeleted IS NULL) " +
                "ORDER BY a.dteCreatedDate DESC")
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
}

