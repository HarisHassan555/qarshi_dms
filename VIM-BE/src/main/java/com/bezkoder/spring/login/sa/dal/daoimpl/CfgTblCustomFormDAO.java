package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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

    private void closeQuietly(EntityManager entityManager) {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    /**
     * Generate next form code based on convention prefix
     * Format: PREFIX-0001, PREFIX-0002, etc.
     */
    private String generateNextFormCode(String conventionPrefix, String seedCode, EntityManager entityManager) {
        try {
            String normalizedPrefix = conventionPrefix.trim().toUpperCase(Locale.ROOT);
            String codeQuery = "SELECT f.txtFormCode FROM CfgTblCustomForm f " +
                    "WHERE f.txtFormCode IS NOT NULL " +
                    "AND UPPER(f.txtFormCode) LIKE :prefixPattern " +
                    "AND (f.blIsDeleted = false OR f.blIsDeleted IS NULL)";

            @SuppressWarnings("unchecked")
            List<String> existingCodes = entityManager.createQuery(codeQuery)
                    .setParameter("prefixPattern", normalizedPrefix + "-%")
                    .getResultList();

            int maxNumber = 0;
            int width = 4;

            Integer seedNumber = extractNumericSuffix(seedCode, normalizedPrefix);
            if (seedNumber != null) {
                maxNumber = Math.max(maxNumber, seedNumber);
                width = Math.max(width, getNumericSuffixLength(seedCode, normalizedPrefix));
            }

            if (existingCodes != null) {
                for (String code : existingCodes) {
                    Integer numeric = extractNumericSuffix(code, normalizedPrefix);
                    if (numeric != null) {
                        maxNumber = Math.max(maxNumber, numeric);
                        width = Math.max(width, getNumericSuffixLength(code, normalizedPrefix));
                    }
                }
            }

            int nextNumber = maxNumber + 1;
            return String.format("%s-%0" + width + "d", normalizedPrefix, nextNumber);
        } catch (Exception e) {
            log.error("Error generating form code: " + e.getMessage(), e);
            // Fallback: use timestamp-based code
            return conventionPrefix + "-" + System.currentTimeMillis();
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

    private Set<Integer> extractDepartmentIds(String approvalPipelineJson) {
        if (approvalPipelineJson == null || approvalPipelineJson.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> departmentIds = new HashSet<>();
        try {
            List<Map<String, Object>> pipelineArray = objectMapper.readValue(
                    approvalPipelineJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> pipelineData : pipelineArray) {
                Object deptIdObj = pipelineData.get("serDepartmentId");
                if (deptIdObj instanceof Integer) {
                    departmentIds.add((Integer) deptIdObj);
                } else if (deptIdObj instanceof Number) {
                    departmentIds.add(((Number) deptIdObj).intValue());
                }
            }
        } catch (Exception e) {
            log.error("Error extracting department IDs from approval pipeline JSON", e);
        }

        return departmentIds;
    }

    private Map<Integer, HrTblDepartment> loadDepartmentsForForms(List<CfgTblCustomForm> forms, EntityManager entityManager) {
        Set<Integer> departmentIds = new HashSet<>();

        if (forms != null) {
            for (CfgTblCustomForm form : forms) {
                departmentIds.addAll(extractDepartmentIds(form.getTxtApprovalPipeline()));
            }
        }

        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<HrTblDepartment> departments = entityManager.createQuery(
                        "SELECT d FROM HrTblDepartment d WHERE d.serDepartmentId IN :departmentIds",
                        HrTblDepartment.class)
                .setParameter("departmentIds", departmentIds)
                .setHint("org.hibernate.readOnly", true)
                .getResultList();

        Map<Integer, HrTblDepartment> departmentsById = new HashMap<>();
        for (HrTblDepartment department : departments) {
            departmentsById.put(department.getSerDepartmentId(), department);
        }

        return departmentsById;
    }

    /**
     * Deserialize approval pipeline from JSON string to list of pipeline objects
     */
    private void deserializeApprovalPipeline(CfgTblCustomForm form, Map<Integer, HrTblDepartment> departmentsById) {
        if (form == null) {
            return;
        }
        
        List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelines = new ArrayList<>();
        
        if (form.getTxtApprovalPipeline() != null && !form.getTxtApprovalPipeline().trim().isEmpty()) {
            try {
                // Parse JSON array
                List<Map<String, Object>> pipelineArray = objectMapper.readValue(
                    form.getTxtApprovalPipeline(), 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Convert each map to CfgTblCustomFormApprovalPipeline object
                for (Map<String, Object> pipelineData : pipelineArray) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline = 
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();
                    
                    // Set department ID and fetch department entity
                    Object deptIdObj = pipelineData.get("serDepartmentId");
                    Integer deptId = null;
                    if (deptIdObj != null) {
                        if (deptIdObj instanceof Integer) {
                            deptId = (Integer) deptIdObj;
                        } else if (deptIdObj instanceof Number) {
                            deptId = ((Number) deptIdObj).intValue();
                        }
                        
                        if (deptId != null) {
                            HrTblDepartment dept = departmentsById.get(deptId);
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

    private List<CfgTblCustomForm> fetchCustomForms(boolean activeOnly) {
        EntityManager entityManager = getEntityManager();
        List<CfgTblCustomForm> forms;
        Map<Integer, HrTblDepartment> departmentsById;

        try {
            String jpql =
                    "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                    "LEFT JOIN FETCH f.cfgTblCustomFormFields field " +
                    "WHERE (f.blIsDeleted = false OR f.blIsDeleted IS NULL) " +
                    (activeOnly
                            ? "AND (f.blIsActive = true OR f.blIsActive IS NULL) " +
                              "AND (f.blnStatus = true OR f.blnStatus IS NULL) "
                            : "") +
                    "AND (field.blIsDeleted = false OR field.blIsDeleted IS NULL OR field IS NULL) " +
                    "ORDER BY f.dteCreatedDate DESC";

            forms = entityManager.createQuery(jpql, CfgTblCustomForm.class)
                    .setHint("org.hibernate.readOnly", true)
                    .getResultList();
            departmentsById = loadDepartmentsForForms(forms, entityManager);
        } catch (Exception e) {
            log.error("Error getting {} custom forms: {}", activeOnly ? "active" : "all", e.getMessage(), e);
            throw e;
        } finally {
            closeQuietly(entityManager);
        }

        if (forms != null && !forms.isEmpty()) {
            for (CfgTblCustomForm form : forms) {
                deserializeApprovalPipeline(form, departmentsById);
            }
        }

        return forms;
    }

    @Override
    public List<CfgTblCustomForm> getAllCustomForms() {
        return fetchCustomForms(false);
    }

    @Override
    public CfgTblCustomForm getCustomFormById(Integer formId) {
        EntityManager entityManager = getEntityManager();
        CfgTblCustomForm form;
        Map<Integer, HrTblDepartment> departmentsById;
        try {
            form = entityManager.createQuery(
                    "SELECT DISTINCT f FROM CfgTblCustomForm f " +
                    "LEFT JOIN FETCH f.cfgTblCustomFormFields field " +
                    "WHERE f.serFormId = :formId " +
                    "AND (field.blIsDeleted = false OR field.blIsDeleted IS NULL OR field IS NULL)",
                    CfgTblCustomForm.class)
                    .setParameter("formId", formId)
                    .getSingleResult();
            departmentsById = loadDepartmentsForForms(Collections.singletonList(form), entityManager);
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            log.error("Error getting custom form by ID: " + e.getMessage(), e);
            throw e;
        } finally {
            closeQuietly(entityManager);
        }

        deserializeApprovalPipeline(form, departmentsById);
        return form;
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

            // Generate form code based on convention or provided form code prefix
            String conventionPrefix = null;
            if (customForm.getTxtConventionPrefix() != null && !customForm.getTxtConventionPrefix().trim().isEmpty()) {
                conventionPrefix = customForm.getTxtConventionPrefix().trim().toUpperCase(Locale.ROOT);
            } else if (customForm.getTxtFormCode() != null && customForm.getTxtFormCode().contains("-")) {
                conventionPrefix = customForm.getTxtFormCode()
                        .substring(0, customForm.getTxtFormCode().indexOf("-"))
                        .trim()
                        .toUpperCase(Locale.ROOT);
            }

            if (conventionPrefix != null && !conventionPrefix.isEmpty()) {
                String nextCode = generateNextFormCode(conventionPrefix, customForm.getTxtFormCode(), entityManager);
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

            // Use txtApprovalPipeline from payload if already set (supports mixed department + individual).
            // Otherwise build from cfgTblCustomFormApprovalPipelines entities.
            if (customForm.getTxtApprovalPipeline() == null || customForm.getTxtApprovalPipeline().trim().isEmpty()) {
                if (customForm.getCfgTblCustomFormApprovalPipelines() != null && !customForm.getCfgTblCustomFormApprovalPipelines().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> pipelineArray = new java.util.ArrayList<>();

                    for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline : customForm.getCfgTblCustomFormApprovalPipelines()) {
                        Integer deptId = null;
                        if (pipeline.getSerDepartmentId() != null) {
                            deptId = pipeline.getSerDepartmentId();
                        } else if (pipeline.getHrTblDepartment() != null && pipeline.getHrTblDepartment().getSerDepartmentId() != null) {
                            deptId = pipeline.getHrTblDepartment().getSerDepartmentId();
                        }

                        if (deptId != null) {
                            java.util.Map<String, Object> pipelineData = new java.util.HashMap<>();
                            pipelineData.put("type", "department");
                            pipelineData.put("serDepartmentId", deptId);
                            pipelineData.put("intApprovalOrder", pipeline.getIntApprovalOrder() != null ? pipeline.getIntApprovalOrder() : 0);
                            pipelineArray.add(pipelineData);
                        } else {
                            log.warn("No department ID provided for pipeline, skipping");
                        }
                    }

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
                    String nextCode = generateNextFormCode(newConventionPrefix, customForm.getTxtFormCode(), entityManager);
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

            // Use txtApprovalPipeline from request if already set (supports mixed department + individual).
            // Otherwise build from cfgTblCustomFormApprovalPipelines entities.
            if (customForm.getTxtApprovalPipeline() != null && !customForm.getTxtApprovalPipeline().trim().isEmpty()) {
                existingForm.setTxtApprovalPipeline(customForm.getTxtApprovalPipeline());
                log.info("Using txtApprovalPipeline from request (mixed pipeline support)");
            } else if (customForm.getCfgTblCustomFormApprovalPipelines() != null && !customForm.getCfgTblCustomFormApprovalPipelines().isEmpty()) {
                java.util.List<java.util.Map<String, Object>> pipelineArray = new java.util.ArrayList<>();

                for (com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline : customForm.getCfgTblCustomFormApprovalPipelines()) {
                    Integer deptId = null;
                    if (pipeline.getSerDepartmentId() != null) {
                        deptId = pipeline.getSerDepartmentId();
                    } else if (pipeline.getHrTblDepartment() != null && pipeline.getHrTblDepartment().getSerDepartmentId() != null) {
                        deptId = pipeline.getHrTblDepartment().getSerDepartmentId();
                    }

                    if (deptId != null) {
                        java.util.Map<String, Object> pipelineData = new java.util.HashMap<>();
                        pipelineData.put("type", "department");
                        pipelineData.put("serDepartmentId", deptId);
                        pipelineData.put("intApprovalOrder", pipeline.getIntApprovalOrder() != null ? pipeline.getIntApprovalOrder() : 0);
                        pipelineArray.add(pipelineData);
                    } else {
                        log.warn("No department ID provided for pipeline, skipping");
                    }
                }

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
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : e.getMessage();
            log.error("Error updating custom form: " + errorMessage, e);
            return "Failure: " + errorMessage;
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

    @Override
    public List<CfgTblCustomForm> getActiveCustomForms() {
        return fetchCustomForms(true);
    }
}
