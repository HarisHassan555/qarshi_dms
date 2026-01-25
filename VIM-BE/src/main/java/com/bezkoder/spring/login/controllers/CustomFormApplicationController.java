package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
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
public class CustomFormApplicationController {

    private Logger logger = LogManager.getLogger(CustomFormApplicationController.class);

    @Autowired
    private ICustomFormApplicationService customFormApplicationService;

    @RequestMapping(value = "/getAllApplications", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getAllApplications(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("getAllApplications()");
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService.getAllApplications();
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching all applications: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getApplicationsByFormId", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsByFormId(@RequestParam Integer formId,
                                                                      HttpServletRequest request,
                                                                      HttpServletResponse response) {
        logger.debug("getApplicationsByFormId() - formId: " + formId);
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService.getApplicationsByFormId(formId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications by form ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getApplicationsByUserId", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsByUserId(@RequestParam Integer userId,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {
        logger.debug("getApplicationsByUserId() - userId: " + userId);
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService.getApplicationsByUserId(userId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications by user ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getApplicationById", method = RequestMethod.GET)
    public CfgTblCustomFormApplication getApplicationById(@RequestParam Integer applicationId,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        logger.debug("getApplicationById() - applicationId: " + applicationId);
        try {
            CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
            return application;
        } catch (Exception ex) {
            logger.error("Error fetching application by ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/submitApplication",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> submitApplication(@RequestBody CfgTblCustomFormApplication application,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        logger.debug("submitApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormApplicationService.submitApplication(application);
            if (status != null && status.startsWith("Success")) {
                result.put("status", "Success");
                result.put("message", "Application submitted successfully");
                result.put("applicationId", application.getSerApplicationId());
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to submit application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error submitting application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/updateApplication",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateApplication(@RequestBody CfgTblCustomFormApplication application,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("updateApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormApplicationService.updateApplication(application);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application updated successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to update application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/deleteApplication", method = RequestMethod.POST)
    public Map<String, Object> deleteApplication(@RequestParam Integer applicationId,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("deleteApplication() - applicationId: " + applicationId);
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormApplicationService.deleteApplication(applicationId);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application deleted successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", "Failed to delete application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error deleting application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/getApplicationsByStatus", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsByStatus(@RequestParam String status,
                                                                    HttpServletRequest request,
                                                                    HttpServletResponse response) {
        logger.debug("getApplicationsByStatus() - status: " + status);
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService.getApplicationsByStatus(status);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications by status: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getNextApplicationCode", method = RequestMethod.GET)
    public Map<String, Object> getNextApplicationCode(@RequestParam Integer formId,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        logger.debug("getNextApplicationCode() - formId: " + formId);
        Map<String, Object> result = new HashMap<>();
        try {
            String nextCode = customFormApplicationService.getNextApplicationCode(formId);
            if (nextCode != null) {
                result.put("status", "Success");
                result.put("code", nextCode);
            } else {
                result.put("status", "Failure");
                result.put("message", "Could not generate application code");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error generating next application code: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/getApplicationsPendingApproval", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsPendingApproval(@RequestParam Integer departmentHeadUserId,
                                                                           HttpServletRequest request,
                                                                           HttpServletResponse response) {
        logger.debug("getApplicationsPendingApproval() - departmentHeadUserId: " + departmentHeadUserId);
        try {
            List<CfgTblCustomFormApplication> applications = 
                customFormApplicationService.getApplicationsPendingApprovalForDepartmentHead(departmentHeadUserId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications pending approval: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/approveApplication",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> approveApplication(@RequestBody Map<String, Object> requestBody,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        logger.debug("approveApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = (Integer) requestBody.get("applicationId");
            String remarks = (String) requestBody.get("remarks");
            
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }
            
            String status = customFormApplicationService.approveApplication(applicationId, remarks);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application approved successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to approve application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error approving application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/rejectApplication",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> rejectApplication(@RequestBody Map<String, Object> requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("rejectApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = (Integer) requestBody.get("applicationId");
            String remarks = (String) requestBody.get("remarks");
            
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }
            
            String status = customFormApplicationService.rejectApplication(applicationId, remarks);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application rejected successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to reject application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error rejecting application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }
}

