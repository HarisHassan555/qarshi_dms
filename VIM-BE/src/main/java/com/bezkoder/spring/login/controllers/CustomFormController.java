package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.utility.common.RequestMetadataUtil;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CustomFormController {

    private Logger logger = LogManager.getLogger(CustomFormController.class);

    @Autowired
    private ICustomFormService customFormService;

    @Autowired
    private IAppActivityLogService activityLogService;

    @Autowired
    private ICommonService commonService;

    @RequestMapping(value = "/getAllCustomForms", method = RequestMethod.GET)
    public List<CfgTblCustomForm> getAllCustomForms(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("getAllCustomForms()");
        try {
            List<CfgTblCustomForm> forms = customFormService.getAllCustomForms();
            return forms;
        } catch (Exception ex) {
            logger.error("Error fetching all custom forms: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getCustomFormById", method = RequestMethod.GET)
    public CfgTblCustomForm getCustomFormById(@RequestParam Integer formId, 
                                               HttpServletRequest request, 
                                               HttpServletResponse response) {
        logger.debug("getCustomFormById() - formId: " + formId);
        try {
            CfgTblCustomForm form = customFormService.getCustomFormById(formId);
            return form;
        } catch (Exception ex) {
            logger.error("Error fetching custom form by ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/addNewCustomForm",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> addNewCustomForm(@RequestBody Map<String, Object> requestBody,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        logger.debug("addNewCustomForm()");
        Map<String, Object> result = new HashMap<>();
        try {
            Object pipelinesObj = requestBody.get("cfgTblCustomFormApprovalPipelines");
            if (pipelinesObj == null) {
                pipelinesObj = requestBody.get("approvalPipelines");
            }

            Object txtPipelineObj = requestBody.get("txtApprovalPipeline");
            requestBody.remove("txtApprovalPipeline");
            requestBody.remove("cfgTblCustomFormApprovalPipelines");
            requestBody.remove("approvalPipelines");

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            CfgTblCustomForm customForm = objectMapper.convertValue(requestBody, CfgTblCustomForm.class);

            if (pipelinesObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> pipelinesList = (java.util.List<Map<String, Object>>) pipelinesObj;
                java.util.List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelineEntities =
                    new java.util.ArrayList<>();

                for (Map<String, Object> pipelineMap : pipelinesList) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline =
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();

                    Object deptIdObj = pipelineMap.get("serDepartmentId");
                    if (deptIdObj == null && pipelineMap.get("hrTblDepartment") != null) {
                        Map<String, Object> deptMap = (Map<String, Object>) pipelineMap.get("hrTblDepartment");
                        deptIdObj = deptMap.get("serDepartmentId");
                    }

                    if (deptIdObj != null) {
                        Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj :
                                Integer.parseInt(deptIdObj.toString());
                        pipeline.setSerDepartmentId(deptId);
                    }

                    Object orderObj = pipelineMap.get("intApprovalOrder");
                    if (orderObj != null) {
                        Integer order = orderObj instanceof Integer ? (Integer) orderObj :
                                Integer.parseInt(orderObj.toString());
                        pipeline.setIntApprovalOrder(order);
                    }

                    pipelineEntities.add(pipeline);
                }

                customForm.setCfgTblCustomFormApprovalPipelines(pipelineEntities);
                logger.debug("Manually extracted " + pipelineEntities.size() + " approval pipelines");
            }

            if (txtPipelineObj instanceof String && !((String) txtPipelineObj).trim().isEmpty()) {
                customForm.setTxtApprovalPipeline((String) txtPipelineObj);
                logger.debug("Using txtApprovalPipeline from request (supports mixed pipeline)");
            }

            String status = customFormService.addNewCustomForm(customForm);
            if (status != null && status.startsWith("Success")) {
                logTemplateFormAction("TEMPLATE_CREATE", request, customForm.getSerFormId(), "SUCCESS",
                        "Template form created successfully", buildTemplateFormPayload(customForm, "CREATE"), null);
                result.put("status", "Success");
                result.put("message", "Form created successfully");
                result.put("formId", customForm.getSerFormId());
            } else {
                logTemplateFormAction("TEMPLATE_CREATE", request, customForm.getSerFormId(), "FAILURE",
                        status, buildTemplateFormPayload(customForm, "CREATE"), status);
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to create form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error creating custom form: " + ex.getMessage(), ex);
            ex.printStackTrace();
            logTemplateFormAction("TEMPLATE_CREATE", request, null, "FAILURE",
                    ex.getMessage(), requestBody, ex.getMessage());
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return result;
        }
    }

    @RequestMapping(value = "/updateCustomForm",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateCustomForm(@RequestBody Map<String, Object> requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("updateCustomForm()");
        Map<String, Object> result = new HashMap<>();
        try {
            // Manually extract approval pipelines from raw JSON to avoid deserialization issues
            Object pipelinesObj = requestBody.get("cfgTblCustomFormApprovalPipelines");
            if (pipelinesObj == null) {
                pipelinesObj = requestBody.get("approvalPipelines"); // Try alias
            }
            
            // Extract txtApprovalPipeline if sent (supports mixed department + individual pipeline)
            Object txtPipelineObj = requestBody.get("txtApprovalPipeline");
            requestBody.remove("txtApprovalPipeline");

            // Remove from request body to avoid deserialization issues
            requestBody.remove("cfgTblCustomFormApprovalPipelines");
            requestBody.remove("approvalPipelines");

            // Convert to CfgTblCustomForm using ObjectMapper
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            CfgTblCustomForm customForm = objectMapper.convertValue(requestBody, CfgTblCustomForm.class);
            
            // Manually set approval pipelines if they exist
            if (pipelinesObj != null && pipelinesObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> pipelinesList = (java.util.List<Map<String, Object>>) pipelinesObj;
                java.util.List<com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline> pipelineEntities = 
                    new java.util.ArrayList<>();
                
                for (Map<String, Object> pipelineMap : pipelinesList) {
                    com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline pipeline = 
                        new com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApprovalPipeline();
                    
                    // Extract serDepartmentId (could be direct or nested in hrTblDepartment)
                    Object deptIdObj = pipelineMap.get("serDepartmentId");
                    if (deptIdObj == null && pipelineMap.get("hrTblDepartment") != null) {
                        Map<String, Object> deptMap = (Map<String, Object>) pipelineMap.get("hrTblDepartment");
                        deptIdObj = deptMap.get("serDepartmentId");
                    }
                    
                    if (deptIdObj != null) {
                        Integer deptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : 
                                        Integer.parseInt(deptIdObj.toString());
                        pipeline.setSerDepartmentId(deptId);
                    }
                    
                    // Extract intApprovalOrder
                    Object orderObj = pipelineMap.get("intApprovalOrder");
                    if (orderObj != null) {
                        Integer order = orderObj instanceof Integer ? (Integer) orderObj : 
                                       Integer.parseInt(orderObj.toString());
                        pipeline.setIntApprovalOrder(order);
                    }
                    
                    pipelineEntities.add(pipeline);
                }
                
                customForm.setCfgTblCustomFormApprovalPipelines(pipelineEntities);
                logger.debug("Manually extracted " + pipelineEntities.size() + " approval pipelines");
            }

            // If frontend sent txtApprovalPipeline (mixed department + individual), use it directly
            if (txtPipelineObj != null && txtPipelineObj instanceof String && !((String) txtPipelineObj).trim().isEmpty()) {
                customForm.setTxtApprovalPipeline((String) txtPipelineObj);
                logger.debug("Using txtApprovalPipeline from request (supports mixed pipeline)");
            }

            String status = customFormService.updateCustomForm(customForm);
            if ("Success".equals(status)) {
                logTemplateFormAction("TEMPLATE_UPDATE", request, customForm.getSerFormId(), "SUCCESS",
                        "Template form updated successfully", buildTemplateFormPayload(customForm, "UPDATE"), null);
                result.put("status", "Success");
                result.put("message", "Form updated successfully");
            } else {
                logTemplateFormAction("TEMPLATE_UPDATE", request, customForm.getSerFormId(), "FAILURE",
                        status, buildTemplateFormPayload(customForm, "UPDATE"), status);
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to update form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating custom form: " + ex.getMessage(), ex);
            ex.printStackTrace();
            logTemplateFormAction("TEMPLATE_UPDATE", request, toInteger(requestBody.get("serFormId")), "FAILURE",
                    ex.getMessage(), requestBody, ex.getMessage());
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return result;
        }
    }

    @RequestMapping(value = "/deleteCustomForm", method = RequestMethod.POST)
    public Map<String, Object> deleteCustomForm(@RequestParam Integer formId,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("deleteCustomForm() - formId: " + formId);
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormService.deleteCustomForm(formId);
            if ("Success".equals(status)) {
                logTemplateFormAction("TEMPLATE_DELETE", request, formId, "SUCCESS",
                        "Template form deleted successfully", buildTemplateFormPayload(formId, "DELETE"), null);
                result.put("status", "Success");
                result.put("message", "Form deleted successfully");
            } else {
                logTemplateFormAction("TEMPLATE_DELETE", request, formId, "FAILURE",
                        status, buildTemplateFormPayload(formId, "DELETE"), status);
                result.put("status", "Failure");
                result.put("message", "Failed to delete form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error deleting custom form: " + ex.getMessage(), ex);
            logTemplateFormAction("TEMPLATE_DELETE", request, formId, "FAILURE",
                    ex.getMessage(), buildTemplateFormPayload(formId, "DELETE"), ex.getMessage());
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    private void logTemplateFormAction(String actionType, HttpServletRequest request, Integer formId,
                                       String status, String message, Object payload, String errorMessage) {
        try {
            int userId = commonService.getCurrentLoggedInUser();
            String username = null;
            if (userId > 0) {
                CfgTblUser user = commonService.getCurrentUser(userId);
                if (user != null) {
                    username = user.getTxtUserName();
                }
            }
            Map<String, Object> normalizedPayload = new HashMap<>();
            if (payload instanceof Map) {
                normalizedPayload.putAll((Map<String, Object>) payload);
            } else if (payload != null) {
                normalizedPayload.put("value", payload);
            }
            activityLogService.logActivity(actionType, userId > 0 ? userId : null, username,
                    RequestMetadataUtil.resolveClientIp(request),
                    RequestMetadataUtil.resolveDevice(request),
                    status, message, "TEMPLATE_FORM", formId, normalizedPayload, errorMessage);
        } catch (Exception ex) {
            logger.warn("Failed to log template form action: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildTemplateFormPayload(CfgTblCustomForm customForm, String action) {
        Map<String, Object> payload = new HashMap<>();
        if (customForm != null) {
            payload.put("formId", customForm.getSerFormId());
            payload.put("formCode", customForm.getTxtFormCode());
            payload.put("formName", customForm.getTxtFormName());
            payload.put("conventionPrefix", customForm.getTxtConventionPrefix());
        }
        payload.put("action", action);
        return payload;
    }

    private Map<String, Object> buildTemplateFormPayload(Integer formId, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("formId", formId);
        payload.put("action", action);
        return payload;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    @RequestMapping(value = "/getActiveCustomForms", method = RequestMethod.GET)
    public List<CfgTblCustomForm> getActiveCustomForms(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("getActiveCustomForms()");
        try {
            List<CfgTblCustomForm> forms = customFormService.getActiveCustomForms();
            return forms;
        } catch (Exception ex) {
            logger.error("Error fetching active custom forms: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }
}

