package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    
    @Autowired
    private com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO customFormApplicationDAO;

    @RequestMapping(value = "/getAllApplications", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getAllApplications(HttpServletRequest request,
            HttpServletResponse response) {
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
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsByFormId(formId);
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
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsByUserId(userId);
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

    @RequestMapping(value = "/submitApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to submit application");
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

    @RequestMapping(value = "/updateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to update application");
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

    @RequestMapping(value = "/updateApplicationPdf", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateApplicationPdf(@RequestParam Integer applicationId,
            @RequestParam("pdf") MultipartFile pdf,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("updateApplicationPdf() - applicationId: " + applicationId);
        Map<String, Object> result = new HashMap<>();
        try {
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }
            if (pdf == null || pdf.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "PDF file is required");
                return result;
            }

            String pdfName = pdf.getOriginalFilename();
            String pdfMime = pdf.getContentType();
            String status = customFormApplicationService.updateApplicationPdf(applicationId, pdf.getBytes(), pdfName,
                    pdfMime);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application PDF updated successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to update application PDF");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating application PDF: " + ex.getMessage(), ex);
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
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsByStatus(status);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications by status: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getApplicationsByStatusAndUserId", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsByStatusAndUserId(@RequestParam String status,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getApplicationsByStatusAndUserId() - status: " + status + ", userId: " + userId);
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsByStatusAndUserId(status, userId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications by status and user ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getApplicationsApprovedByUser", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getApplicationsApprovedByUser(@RequestParam String status,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getApplicationsApprovedByUser() - status: " + status + ", userId: " + userId);
        try {
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsApprovedByUser(status, userId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching approved applications: " + ex.getMessage(), ex);
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
            List<CfgTblCustomFormApplication> applications = customFormApplicationService
                    .getApplicationsPendingApprovalForDepartmentHead(departmentHeadUserId);
            return applications;
        } catch (Exception ex) {
            logger.error("Error fetching applications pending approval: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getAllApplicationsPendingApproval", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getAllApplicationsPendingApproval(HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getAllApplicationsPendingApproval()");
        try {
            return customFormApplicationService.getAllApplicationsPendingApproval();
        } catch (Exception ex) {
            logger.error("Error fetching all applications pending approval: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/approveApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> approveApplication(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("approveApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = (Integer) requestBody.get("applicationId");
            String remarks = (String) requestBody.get("remarks");
            Integer approverUserId = null;
            if (requestBody.get("approverUserId") != null) {
                approverUserId = (Integer) requestBody.get("approverUserId");
            }
            String approvedVia = requestBody.get("approvedVia") != null ? String.valueOf(requestBody.get("approvedVia"))
                    : "SYSTEM";

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            String approvedIp = resolveClientIp(request);
            String status = customFormApplicationService.approveApplication(applicationId, remarks, approverUserId,
                    approvedVia, approvedIp);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application approved successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to approve application");
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

    @RequestMapping(value = "/rejectApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to reject application");
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

    /**
     * GET endpoint for email-based approval (accessed via email link)
     * This allows users to approve applications directly from email
     */
    @RequestMapping(value = "/approveApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String approveApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("approveApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            String approvedIp = resolveClientIp(request);
            String status = customFormApplicationService.approveApplication(applicationId, "Approved via email", userId,
                    "EMAIL", approvedIp);
            if ("Success".equals(status)) {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Application Approved</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".success{color:#27ae60;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='success'>✓ Application Approved Successfully</div>" +
                        "<div class='message'>The application has been approved. You can close this window.</div></div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Approval Failed</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='error'>✗ Approval Failed</div>" +
                        "<div class='message'>"
                        + (status != null && status.startsWith("Failure:") ? status.substring(8)
                                : "Failed to approve application")
                        +
                        "</div></div></body></html>";
            }
        } catch (Exception ex) {
            logger.error("Error approving application from email: " + ex.getMessage(), ex);
            return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Error</title>" +
                    "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                    ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                    +
                    ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                    ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                    "<div class='container'><div class='error'>✗ Error</div>" +
                    "<div class='message'>An error occurred: " + ex.getMessage() + "</div></div></body></html>";
        }
    }

    /**
     * GET endpoint for email-based rejection (accessed via email link)
     * This allows users to reject applications directly from email
     */
    @RequestMapping(value = "/rejectApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String rejectApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("rejectApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            String status = customFormApplicationService.rejectApplication(applicationId, "Rejected via email");
            if ("Success".equals(status)) {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Application Rejected</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".success{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='success'>✗ Application Rejected</div>" +
                        "<div class='message'>The application has been rejected. You can close this window.</div></div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Rejection Failed</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='error'>✗ Rejection Failed</div>" +
                        "<div class='message'>"
                        + (status != null && status.startsWith("Failure:") ? status.substring(8)
                                : "Failed to reject application")
                        +
                        "</div></div></body></html>";
            }
        } catch (Exception ex) {
            logger.error("Error rejecting application from email: " + ex.getMessage(), ex);
            return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Error</title>" +
                    "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                    ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                    +
                    ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                    ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                    "<div class='container'><div class='error'>✗ Error</div>" +
                    "<div class='message'>An error occurred: " + ex.getMessage() + "</div></div></body></html>";
        }
    }

    @RequestMapping(value = "/sendBackApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackApplication(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackApplication()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = (Integer) requestBody.get("applicationId");
            String remarks = (String) requestBody.get("remarks");

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Remarks are required when sending back an application");
                return result;
            }

            String status = customFormApplicationService.sendBackApplication(applicationId, remarks);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application sent back successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to send back application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error sending back application: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/sendBackToInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackToInitiator(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackToInitiator()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = (Integer) requestBody.get("applicationId");
            String remarks = (String) requestBody.get("remarks");

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Remarks are required when sending back an application to initiator");
                return result;
            }

            String status = customFormApplicationService.sendBackApplicationToInitiator(applicationId, remarks);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application sent back to initiator successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to send back application to initiator");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error sending back application to initiator: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    /**
     * GET endpoint for email-based send back (accessed via email link)
     * This allows users to send back applications directly from email
     */
    @RequestMapping(value = "/sendBackApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            // Use DAO directly to pass userId for email-based send back
            com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO dao = 
                (com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO) customFormApplicationDAO;
            String status = dao.sendBackApplication(applicationId, "Sent back via email", userId);
            if ("Success".equals(status)) {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Application Sent Back</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".success{color:#f39c12;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='success'>✓ Application Sent Back</div>" +
                        "<div class='message'>The application has been sent back. You can close this window.</div></div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Send Back Failed</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='error'>Send Back Failed</div>" +
                        "<div class='message'>"
                        + (status != null && status.startsWith("Failure:") ? status.substring(8)
                                : "Failed to send back application")
                        +
                        "</div></div></body></html>";
            }
        } catch (Exception ex) {
            logger.error("Error sending back application from email: " + ex.getMessage(), ex);
            return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Error</title>" +
                    "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                    ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                    +
                    ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                    ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                    "<div class='container'><div class='error'>Error</div>" +
                    "<div class='message'>An error occurred: " + ex.getMessage() + "</div></div></body></html>";
        }
    }

    /**
     * GET endpoint for email-based send back to initiator (accessed via email link)
     */
    @RequestMapping(value = "/sendBackToInitiatorFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackToInitiatorFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackToInitiatorFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            // Use DAO directly to pass userId for email-based send back
            com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO dao = 
                (com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO) customFormApplicationDAO;
            String status = dao.sendBackApplicationToInitiator(applicationId, "Sent back to initiator via email", userId);
            if ("Success".equals(status)) {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Application Sent Back to Initiator</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".success{color:#f39c12;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='success'>✓ Application Sent Back to Initiator</div>" +
                        "<div class='message'>The application has been sent back to the initiator. You can close this window.</div></div></body></html>";
            } else {
                return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Send Back Failed</title>" +
                        "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                        ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                        +
                        ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                        ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                        "<div class='container'><div class='error'>Send Back Failed</div>" +
                        "<div class='message'>"
                        + (status != null && status.startsWith("Failure:") ? status.substring(8)
                                : "Failed to send back application to initiator")
                        +
                        "</div></div></body></html>";
            }
        } catch (Exception ex) {
            logger.error("Error sending back application to initiator from email: " + ex.getMessage(), ex);
            return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Error</title>" +
                    "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}" +
                    ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                    +
                    ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}" +
                    ".message{color:#333;font-size:16px;line-height:1.6}</style></head><body>" +
                    "<div class='container'><div class='error'>Error</div>" +
                    "<div class='message'>An error occurred: " + ex.getMessage() + "</div></div></body></html>";
        }
    }


    @RequestMapping(value = "/sendSubmissionEmails", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendSubmissionEmails(@RequestParam Integer applicationId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendSubmissionEmails() - applicationId: " + applicationId);
        Map<String, Object> result = new HashMap<>();
        try {
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            String status = customFormApplicationService.sendSubmissionEmailsForApplication(applicationId);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Submission emails sent successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to send submission emails");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error sending submission emails: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @PostMapping(value = "/assignAssetCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> assignAssetCode(@RequestParam Integer applicationId,
            @RequestParam String assetCode,
            @RequestParam(required = false) Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("assignAssetCode() - applicationId: " + applicationId + ", userId: " + userId);
        Map<String, Object> result = new HashMap<>();
        try {
            String ip = resolveClientIp(request);
            String status = customFormApplicationService.assignAssetCode(applicationId, assetCode, userId, ip);
            if ("Success".equalsIgnoreCase(status)) {
                result.put("status", "Success");
                result.put("message", "Asset code saved and application approved");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : status);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            logger.error("Error assigning asset code: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    @PostMapping(value = "/assignPrCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> assignPrCode(@RequestParam Integer applicationId,
            @RequestParam String prCode,
            @RequestParam(required = false) Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("assignPrCode() - applicationId: " + applicationId + ", userId: " + userId);
        Map<String, Object> result = new HashMap<>();
        try {
            String ip = resolveClientIp(request);
            String status = customFormApplicationService.assignPrCode(applicationId, prCode, userId, ip);
            if ("Success".equalsIgnoreCase(status)) {
                result.put("status", "Success");
                result.put("message", "PR code saved successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : status);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            logger.error("Error assigning PR code: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null)
            return "";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty() && !"unknown".equalsIgnoreCase(forwarded.trim())) {
            String[] ips = forwarded.split(",");
            if (ips.length > 0 && ips[0] != null) {
                return ips[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(realIp.trim())) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }
}
