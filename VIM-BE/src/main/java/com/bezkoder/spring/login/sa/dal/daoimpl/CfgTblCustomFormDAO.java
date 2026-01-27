package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import javax.persistence.*;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;

@Repository
public class CfgTblCustomFormDAO implements ICfgTblCustomFormDAO {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    private static final Logger log = LoggerFactory.getLogger(CfgTblCustomFormDAO.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CfgTblCustomFormDAO() {
    }

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Generate next form code based on convention prefix
     * Format: PREFIX-0001, PREFIX-0002, etc.
     */
    private String generateNextFormCode(String conventionPrefix, EntityManager entityManager) {
        try {
            // Find the highest number for this convention
            String maxCodeQuery = "SELECT MAX(f.txtFormCode) FROM CfgTblCustomForm f " +
                                 "WHERE f.txtConventionPrefix = :prefix " +
                                 "AND (f.blIsDeleted = false OR f.blIsDeleted IS NULL)";
            
            String maxCode = null;
            try {
                maxCode = (String) entityManager.createQuery(maxCodeQuery)
                    .setParameter("prefix", conventionPrefix)
                    .getSingleResult();
            } catch (NoResultException e) {
                // No existing forms with this convention
                maxCode = null;
            }
            
            int nextNumber = 0;
            if (maxCode != null && maxCode.startsWith(conventionPrefix + "-")) {
                try {
                    // Extract number from code (e.g., "CAPF-0005" -> 5)
                    String numberPart = maxCode.substring(conventionPrefix.length() + 1);
                    nextNumber = Integer.parseInt(numberPart);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse number from form code: " + maxCode);
                    nextNumber = 0;
                }
            }
            
            // Generate next code with zero-padding (4 digits)
            nextNumber++;
            String nextCode = String.format("%s-%04d", conventionPrefix, nextNumber);
            
            return nextCode;
        } catch (Exception e) {
            log.error("Error generating form code: " + e.getMessage(), e);
            // Fallback: use timestamp-based code
            return conventionPrefix + "-" + System.currentTimeMillis();
        }
    }

    /**
     * Deserialize approval pipeline from JSON string to list of pipeline objects
     */
    private void deserializeApprovalPipeline(CfgTblCustomForm form, EntityManager entityManager) {
        if (form == null) {
            return;
        }
        
        List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines = new ArrayList<>();
        
        if (form.getTxtApprovalPipeline() != null && !form.getTxtApprovalPipeline().trim().isEmpty()) {
            try {
                // Parse JSON array
                List<java.util.Map<String, Object>> pipelineArray = objectMapper.readValue(
                    form.getTxtApprovalPipeline(), 
                    new TypeReference<List<java.util.Map<String, Object>>>() {}
                );
                
                // Convert each map to CfgTblCustomFormApprovalPipeline object
                for (java.util.Map<String, Object> pipelineData : pipelineArray) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline = 
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();
                    
                    // Set department ID and fetch department entity
                    Object deptIdObj = pipelineData.get("serDepartmentId");
                    if (deptIdObj != null) {
                        Integer deptId = null;
                        if (deptIdObj instanceof Integer) {
                            deptId = (Integer) deptIdObj;
                        } else if (deptIdObj instanceof Number) {
                            deptId = ((Number) deptIdObj).intValue();
                        }
                        
                        if (deptId != null) {
                            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment dept = 
                                entityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, deptId);
                            if (dept != null) {
                                pipeline.setHrTblDepartment(dept);
                                pipeline.setSerDepartmentId(deptId);
                            }
                        }
                    }
                    
                    // Set approval order
                    Object orderObj = pipelineData.get("intApprovalOrder");
                    if (orderObj != null) {
                        if (orderObj instanceof Integer) {
                            pipeline.setIntApprovalOrder((Integer) orderObj);
                        } else if (orderObj instanceof Number) {
                            pipeline.setIntApprovalOrder(((Number) orderObj).intValue());
                        }
                    }
                    
                    pipelines.add(pipeline);
                }
            } catch (Exception e) {
                log.error("Error deserializing approval pipeline JSON: " + e.getMessage(), e);
            }
        }
        
        form.setCfgTblCustomFormApprovalPipelines(pipelines);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CfgTblCustomForm> getAllCustomForms() {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            // First, fetch forms with fields (fields are EAGER, so they'll be loaded)
            List<CfgTblCustomForm> forms = entityManager.createQuery(
                    "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                    "LEFT JOIN FETCH f.cfgTblCustomFormFields field " +
                    "WHERE (f.blIsDeleted = false OR f.blIsDeleted IS NULL) " +
                    "AND (field.blIsDeleted = false OR field.blIsDeleted IS NULL OR field IS NULL) " +
                    "ORDER BY f.dteCreatedDate DESC")
                    .getResultList();
            
            // Deserialize approval pipelines from JSON for all forms
            if (forms != null && !forms.isEmpty()) {
                for (CfgTblCustomForm form : forms) {
                    deserializeApprovalPipeline(form, entityManager);
                }
            }
            
            entityManager.getTransaction().commit();
            return forms;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting all custom forms: " + e.getMessage(), e);
            e.printStackTrace();
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public CfgTblCustomForm getCustomFormById(Integer formId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            CfgTblCustomForm form = (CfgTblCustomForm) entityManager.createQuery(
                    "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                    "LEFT JOIN FETCH f.cfgTblCustomFormFields field " +
                    "WHERE f.serFormId = :formId " +
                    "AND (field.blIsDeleted = false OR field.blIsDeleted IS NULL OR field IS NULL)")
                    .setParameter("formId", formId)
                    .getSingleResult();
            // Deserialize approval pipeline from JSON
            deserializeApprovalPipeline(form, entityManager);
            entityManager.getTransaction().commit();
            return form;
        } catch (NoResultException e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
            return null;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting custom form by ID: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String addNewCustomForm(CfgTblCustomForm customForm) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            
            // Set default values
            if (customForm.getBlIsActive() == null) {
                customForm.setBlIsActive(true);
            }
            if (customForm.getBlIsDeleted() == null) {
                customForm.setBlIsDeleted(false);
            }
            if (customForm.getBlnStatus() == null) {
                customForm.setBlnStatus(true);
            }
            if (customForm.getDteCreatedDate() == null) {
                customForm.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
            }
            if (customForm.getSerCreatedUser() == null) {
                customForm.setSerCreatedUser(commonService.getCurrentLoggedInUser());
            }

            // Generate form code based on convention
            if (customForm.getTxtConventionPrefix() != null && !customForm.getTxtConventionPrefix().trim().isEmpty()) {
                String conventionPrefix = customForm.getTxtConventionPrefix().trim().toUpperCase();
                String nextCode = generateNextFormCode(conventionPrefix, entityManager);
                customForm.setTxtFormCode(nextCode);
                customForm.setTxtConventionPrefix(conventionPrefix);
            }

            // Set field order and default values for fields
            if (customForm.getCfgTblCustomFormFields() != null) {
                int order = 0;
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField field : customForm.getCfgTblCustomFormFields()) {
                    field.setCfgTblCustomForm(customForm);
                    if (field.getIntFieldOrder() == null) {
                        field.setIntFieldOrder(order++);
                    }
                    if (field.getBlIsActive() == null) {
                        field.setBlIsActive(true);
                    }
                    if (field.getBlIsDeleted() == null) {
                        field.setBlIsDeleted(false);
                    }
                    if (field.getDteCreatedDate() == null) {
                        field.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
                    }
                    if (field.getSerCreatedUser() == null) {
                        field.setSerCreatedUser(commonService.getCurrentLoggedInUser());
                    }
                }
            }

            // Convert approval pipelines to JSON array
            if (customForm.getCfgTblCustomFormApprovalPipelines() != null && !customForm.getCfgTblCustomFormApprovalPipelines().isEmpty()) {
                java.util.List<java.util.Map<String, Object>> pipelineArray = new java.util.ArrayList<>();
                
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline : customForm.getCfgTblCustomFormApprovalPipelines()) {
                    // Extract department ID
                    Integer deptId = null;
                    if (pipeline.getSerDepartmentId() != null) {
                        deptId = pipeline.getSerDepartmentId();
                    } else if (pipeline.getHrTblDepartment() != null && pipeline.getHrTblDepartment().getSerDepartmentId() != null) {
                        deptId = pipeline.getHrTblDepartment().getSerDepartmentId();
                    }
                    
                    if (deptId != null) {
                        // Create a simple map for JSON storage (no IDs, just array data)
                        java.util.Map<String, Object> pipelineData = new java.util.HashMap<>();
                        pipelineData.put("serDepartmentId", deptId);
                        pipelineData.put("intApprovalOrder", pipeline.getIntApprovalOrder() != null ? pipeline.getIntApprovalOrder() : 0);
                        pipelineArray.add(pipelineData);
                    } else {
                        log.warn("No department ID provided for pipeline, skipping");
                    }
                }
                
                // Convert to JSON string and store
                try {
                    String jsonPipeline = objectMapper.writeValueAsString(pipelineArray);
                    customForm.setTxtApprovalPipeline(jsonPipeline);
                } catch (Exception e) {
                    log.error("Error converting approval pipeline to JSON: " + e.getMessage(), e);
                    customForm.setTxtApprovalPipeline(null);
                }
            } else {
                customForm.setTxtApprovalPipeline(null);
            }

            entityManager.persist(customForm);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            // Get root cause for better error messages
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : e.getMessage();
            log.error("Error adding new custom form: " + errorMessage, e);
            e.printStackTrace();
            return "Failure: " + errorMessage;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String updateCustomForm(CfgTblCustomForm customForm) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            
            CfgTblCustomForm existingForm = entityManager.find(CfgTblCustomForm.class, customForm.getSerFormId());
            if (existingForm == null) {
                entityManager.getTransaction().rollback();
                entityManager.close();
                return "Failure: Form not found";
            }

            // Update form properties
            existingForm.setTxtFormName(customForm.getTxtFormName());
            existingForm.setTxtFormDescription(customForm.getTxtFormDescription());
            existingForm.setBlIsActive(customForm.getBlIsActive());
            existingForm.setBlnStatus(customForm.getBlnStatus());
            existingForm.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            existingForm.setSerModifiedUser(commonService.getCurrentLoggedInUser());
            
            // Handle form code and convention - preserve existing code if convention hasn't changed
            if (customForm.getTxtConventionPrefix() != null && !customForm.getTxtConventionPrefix().trim().isEmpty()) {
                String newConventionPrefix = customForm.getTxtConventionPrefix().trim().toUpperCase();
                // Only generate new code if convention changed or code doesn't exist
                if (existingForm.getTxtConventionPrefix() == null || 
                    !existingForm.getTxtConventionPrefix().equals(newConventionPrefix)) {
                    String nextCode = generateNextFormCode(newConventionPrefix, entityManager);
                    existingForm.setTxtFormCode(nextCode);
                    existingForm.setTxtConventionPrefix(newConventionPrefix);
                } else {
                    // Convention unchanged, keep existing code
                    existingForm.setTxtConventionPrefix(newConventionPrefix);
                }
            }

            // Delete existing fields by marking them as deleted
            if (existingForm.getCfgTblCustomFormFields() != null) {
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField existingField : existingForm.getCfgTblCustomFormFields()) {
                    existingField.setBlIsDeleted(true);
                    existingField.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                    existingField.setSerModifiedUser(commonService.getCurrentLoggedInUser());
                }
            }

            // Clear the collection and add new fields
            existingForm.getCfgTblCustomFormFields().clear();

            // Add new fields
            if (customForm.getCfgTblCustomFormFields() != null) {
                int order = 0;
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField field : customForm.getCfgTblCustomFormFields()) {
                    // Create new field entity
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField newField = 
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormField();
                    newField.setTxtFieldLabel(field.getTxtFieldLabel());
                    newField.setTxtFieldType(field.getTxtFieldType());
                    newField.setTxtPlaceholder(field.getTxtPlaceholder());
                    newField.setBlIsRequired(field.getBlIsRequired());
                    newField.setIntFieldOrder(order++);
                    newField.setTxtFieldOptions(field.getTxtFieldOptions()); // Set options for select/radio fields
                    newField.setBlIsActive(true);
                    newField.setBlIsDeleted(false);
                    newField.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
                    newField.setSerCreatedUser(commonService.getCurrentLoggedInUser());
                    newField.setCfgTblCustomForm(existingForm);
                    existingForm.addCfgTblCustomFormField(newField);
                }
            }

            // Delete existing approval pipelines by marking them as deleted
            if (existingForm.getCfgTblCustomFormApprovalPipelines() != null) {
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline existingPipeline : existingForm.getCfgTblCustomFormApprovalPipelines()) {
                    existingPipeline.setBlIsDeleted(true);
                    existingPipeline.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                    existingPipeline.setSerModifiedUser(commonService.getCurrentLoggedInUser());
                }
            }

            // Convert approval pipelines to JSON array
            // Log for debugging
            log.info("Update form ID " + customForm.getSerFormId() + " - Approval pipelines check: " + 
                    (customForm.getCfgTblCustomFormApprovalPipelines() != null ? 
                     "not null, size: " + customForm.getCfgTblCustomFormApprovalPipelines().size() : "null"));
            if (customForm.getCfgTblCustomFormApprovalPipelines() != null && !customForm.getCfgTblCustomFormApprovalPipelines().isEmpty()) {
                java.util.List<java.util.Map<String, Object>> pipelineArray = new java.util.ArrayList<>();
                
                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline : customForm.getCfgTblCustomFormApprovalPipelines()) {
                    // Extract department ID
                    Integer deptId = null;
                    if (pipeline.getSerDepartmentId() != null) {
                        deptId = pipeline.getSerDepartmentId();
                    } else if (pipeline.getHrTblDepartment() != null && pipeline.getHrTblDepartment().getSerDepartmentId() != null) {
                        deptId = pipeline.getHrTblDepartment().getSerDepartmentId();
                    }
                    
                    if (deptId != null) {
                        // Create a simple map for JSON storage (no IDs, just array data)
                        java.util.Map<String, Object> pipelineData = new java.util.HashMap<>();
                        pipelineData.put("serDepartmentId", deptId);
                        pipelineData.put("intApprovalOrder", pipeline.getIntApprovalOrder() != null ? pipeline.getIntApprovalOrder() : 0);
                        pipelineArray.add(pipelineData);
                    } else {
                        log.warn("No department ID provided for pipeline, skipping");
                    }
                }
                
                // Convert to JSON string and store
                try {
                    String jsonPipeline = objectMapper.writeValueAsString(pipelineArray);
                    existingForm.setTxtApprovalPipeline(jsonPipeline);
                } catch (Exception e) {
                    log.error("Error converting approval pipeline to JSON: " + e.getMessage(), e);
                    existingForm.setTxtApprovalPipeline(null);
                }
            } else {
                existingForm.setTxtApprovalPipeline(null);
            }

            entityManager.merge(existingForm);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error updating custom form: " + e.getMessage(), e);
            return "Failure";
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public String deleteCustomForm(Integer formId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            CfgTblCustomForm form = entityManager.find(CfgTblCustomForm.class, formId);
            if (form != null) {
                form.setBlIsDeleted(true);
                form.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                form.setSerModifiedUser(commonService.getCurrentLoggedInUser());
                entityManager.merge(form);
            }
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error deleting custom form: " + e.getMessage(), e);
            return "Failure";
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CfgTblCustomForm> getActiveCustomForms() {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            // First, fetch forms with fields
            List<CfgTblCustomForm> forms = entityManager.createQuery(
                    "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                    "LEFT JOIN FETCH f.cfgTblCustomFormFields field " +
                    "WHERE (f.blIsDeleted = false OR f.blIsDeleted IS NULL) " +
                    "AND (f.blIsActive = true OR f.blIsActive IS NULL) " +
                    "AND (f.blnStatus = true OR f.blnStatus IS NULL) " +
                    "AND (field.blIsDeleted = false OR field.blIsDeleted IS NULL OR field IS NULL) " +
                    "ORDER BY f.dteCreatedDate DESC")
                    .getResultList();
            
            // Deserialize approval pipelines from JSON for all forms
            if (forms != null && !forms.isEmpty()) {
                for (CfgTblCustomForm form : forms) {
                    deserializeApprovalPipeline(form, entityManager);
                }
            }
            
            entityManager.getTransaction().commit();
            return forms;
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error getting active custom forms: " + e.getMessage(), e);
            throw e;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}

