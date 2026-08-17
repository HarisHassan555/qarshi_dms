package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.utility.common.RequestMetadataUtil;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private IAppActivityLogService activityLogService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateDefinitionController templateDefinitionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @RequestMapping(value = "/getDepartmentApplications", method = RequestMethod.GET)
    public List<CfgTblCustomFormApplication> getDepartmentApplications(@RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getDepartmentApplications() - userId: " + userId);
        try {
            return customFormApplicationService.getDepartmentApplications(userId);
        } catch (Exception ex) {
            logger.error("Error fetching department applications: " + ex.getMessage(), ex);
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

    @RequestMapping(value = "/downloadTemplateApplicationPdf", method = RequestMethod.GET)
    public ResponseEntity<byte[]> downloadTemplateApplicationPdf(@RequestParam Integer applicationId,
            HttpServletRequest request) {
        logger.debug("downloadTemplateApplicationPdf() - applicationId: " + applicationId);
        try {
            if (applicationId == null) {
                return ResponseEntity.badRequest().build();
            }
            CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] pdfBytes = application.getBlbPdfData();
            if (pdfBytes == null || pdfBytes.length == 0) {
                pdfBytes = customFormApplicationService.resolveDownloadablePdf(applicationId);
            }
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String fileName = application.getTxtPdfName();
            if (fileName == null || fileName.trim().isEmpty()) {
                String code = application.getTxtFormCode() != null && !application.getTxtFormCode().trim().isEmpty()
                        ? application.getTxtFormCode().trim()
                        : "template-application-" + applicationId;
                fileName = code + ".pdf";
            } else if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName = fileName + ".pdf";
            }

            String pdfMime = application.getTxtPdfMime();
            MediaType mediaType;
            try {
                mediaType = (pdfMime != null && !pdfMime.trim().isEmpty())
                        ? MediaType.parseMediaType(pdfMime)
                        : MediaType.APPLICATION_PDF;
            } catch (Exception ignore) {
                mediaType = MediaType.APPLICATION_PDF;
            }

            String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName.replace("\"", "") + "\"; filename*=UTF-8''" + encodedFilename)
                    .body(pdfBytes);
        } catch (Exception ex) {
            logger.error("Error downloading application PDF: " + ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            String logStatus = status != null && status.startsWith("Success") ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = new HashMap<>();
            payload.put("applicationId", application.getSerApplicationId());
            payload.put("formId", application.getSerFormId());
            payload.put("formCode", application.getTxtFormCode());
            logFormAction("FORM_SUBMIT", request, application.getSerApplicationId(),
                    logStatus, status, payload, logStatus.equals("FAILURE") ? status : null);
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
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = new HashMap<>();
            payload.put("applicationId", application.getSerApplicationId());
            payload.put("formId", application.getSerFormId());
            payload.put("formCode", application.getTxtFormCode());
            logFormAction("UPDATE", request, application.getSerApplicationId(),
                    logStatus, status, payload, logStatus.equals("FAILURE") ? status : null);
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
            @RequestParam(required = false, defaultValue = "false") boolean refreshCapfSignatures,
            @RequestParam(required = false, defaultValue = "false") boolean suppressEditNotification,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("updateApplicationPdf() - applicationId: " + applicationId + ", refreshCapfSignatures="
                + refreshCapfSignatures + ", suppressEditNotification=" + suppressEditNotification);
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
                    pdfMime, refreshCapfSignatures, suppressEditNotification);
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
            boolean deferEmail = Boolean.TRUE.equals(requestBody.get("deferEmail"))
                    || "true".equalsIgnoreCase(String.valueOf(requestBody.get("deferEmail")));
            String approvedVia = requestBody.get("approvedVia") != null ? String.valueOf(requestBody.get("approvedVia"))
                    : "SYSTEM";
            if (deferEmail) {
                approvedVia = "TEMPLATE_PORTAL_DEFER_EMAIL";
            }

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Comments or remarks are required when approving an application");
                return result;
            }

            String approvedIp = resolveClientIp(request);
            String status = customFormApplicationService.approveApplication(applicationId, remarks.trim(), approverUserId,
                    approvedVia, approvedIp);
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, approvedVia, approvedIp, "APPROVE");
            logFormAction("APPROVE", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application approved" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
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

            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Comments or remarks are required when rejecting an application");
                return result;
            }

            String status = customFormApplicationService.rejectApplication(applicationId, remarks.trim());
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM", resolveClientIp(request), "REJECT");
            logFormAction("REJECT", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application rejected" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
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
    @RequestMapping(value = "/approveApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String approveApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("approveApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        return renderEmailActionCommentForm("Approve Application",
                "Enter your comments (required), then confirm approval.",
                "/approveApplicationFromEmail", applicationId, userId, request);
    }

    @RequestMapping(value = "/approveApplicationFromEmail", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String approveApplicationFromEmailSubmit(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("approveApplicationFromEmailSubmit() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            if (remarks == null || remarks.trim().isEmpty()) {
                return renderEmailActionErrorPage("Comments required",
                        "Please enter comments before approving. Use your browser back button to return to the form.");
            }
            String finalRemarks = remarks.trim();
            if (isTemplateBuilderApplication(applicationId)) {
                Map<String, Object> requestBody = buildTemplateEmailActionRequest(applicationId, userId, finalRemarks);
                Map<String, Object> result = templateDefinitionController.approveTemplateApplication(requestBody,
                        response);
                if ("Success".equals(result.get("status"))) {
                    String pdfRefreshStatus = getCustomFormApplicationDaoImpl()
                            .refreshTemplateApplicationPdfFromStage0(applicationId);
                    if (!"Success".equals(pdfRefreshStatus)) {
                        logger.warn("Template email approval succeeded but PDF refresh failed for applicationId="
                                + applicationId + ": " + pdfRefreshStatus);
                    }
                    try {
                        customFormApplicationService.sendTemplatePostApprovalEmails(applicationId);
                    } catch (Exception emailEx) {
                        logger.warn("Template email approval succeeded but follow-up emails failed for applicationId="
                                + applicationId + ": " + emailEx.getMessage(), emailEx);
                    }
                    return renderEmailActionResultPage("Application Approved", "The application has been approved.");
                }
                return renderEmailActionErrorPage("Approval Failed",
                        String.valueOf(result.getOrDefault("message", "Failed to approve application")));
            }
            String approvedIp = resolveClientIp(request);
            String status = customFormApplicationService.approveApplication(applicationId, finalRemarks, userId, "EMAIL",
                    approvedIp);
            if ("Success".equals(status)) {
                return renderEmailActionResultPage("Application Approved", "The application has been approved.");
            }
            return renderEmailActionErrorPage("Approval Failed",
                    status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to approve application");
        } catch (Exception ex) {
            logger.error("Error approving application from email: " + ex.getMessage(), ex);
            return renderEmailActionErrorPage("Error", "An error occurred: " + ex.getMessage());
        }
    }

    @RequestMapping(value = "/rejectApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String rejectApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("rejectApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        return renderEmailActionCommentForm("Reject Application",
                "Enter your comments (required), then confirm rejection.",
                "/rejectApplicationFromEmail", applicationId, userId, request);
    }

    @RequestMapping(value = "/rejectApplicationFromEmail", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String rejectApplicationFromEmailSubmit(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("rejectApplicationFromEmailSubmit() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            if (remarks == null || remarks.trim().isEmpty()) {
                return renderEmailActionErrorPage("Comments required",
                        "Please enter comments before rejecting. Use your browser back button to return to the form.");
            }
            String finalRemarks = remarks.trim();
            if (isTemplateBuilderApplication(applicationId)) {
                Map<String, Object> requestBody = buildTemplateEmailActionRequest(applicationId, userId, finalRemarks);
                Map<String, Object> result = templateDefinitionController.rejectTemplateApplication(requestBody,
                        response);
                if ("Success".equals(result.get("status"))) {
                    return renderEmailActionResultPage("Application Rejected", "The application has been rejected.");
                }
                return renderEmailActionErrorPage("Rejection Failed",
                        String.valueOf(result.getOrDefault("message", "Failed to reject application")));
            }
            String status = getCustomFormApplicationDaoImpl().rejectApplication(applicationId, finalRemarks, userId);
            if ("Success".equals(status)) {
                return renderEmailActionResultPage("Application Rejected", "The application has been rejected.");
            }
            return renderEmailActionErrorPage("Rejection Failed",
                    status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to reject application");
        } catch (Exception ex) {
            logger.error("Error rejecting application from email: " + ex.getMessage(), ex);
            return renderEmailActionErrorPage("Error", "An error occurred: " + ex.getMessage());
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
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM", resolveClientIp(request), "SEND_BACK");
            logFormAction("SEND_BACK", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application sent back" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
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
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM", resolveClientIp(request), "SEND_BACK_INITIATOR");
            logFormAction("SEND_BACK", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application sent back to initiator" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
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

    @RequestMapping(value = "/resubmitApplicationFromInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> resubmitApplicationFromInitiator(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("resubmitApplicationFromInitiator()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = getIntegerValue(requestBody.get("applicationId"));
            Integer userId = getIntegerValue(requestBody.get("userId"));
            String remarks = requestBody.get("remarks") != null ? String.valueOf(requestBody.get("remarks")) : "";

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            String status = customFormApplicationService.resubmitApplicationFromInitiator(applicationId,
                    remarks != null ? remarks.trim() : "", userId);
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM", resolveClientIp(request),
                    "RESUBMIT_INITIATOR");
            logFormAction("RESUBMIT_INITIATOR", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application resubmitted by initiator" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application resubmitted successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to resubmit application");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error resubmitting application from initiator: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/requestApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> requestApplicationOpinion(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("requestApplicationOpinion()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = getIntegerValue(requestBody.get("applicationId"));
            Integer opinionUserId = getIntegerValue(requestBody.get("opinionUserId"));
            String remarks = requestBody.get("remarks") != null ? String.valueOf(requestBody.get("remarks")) : "";

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }
            if (opinionUserId == null || opinionUserId <= 0) {
                result.put("status", "Failure");
                result.put("message", "Opinion user is required");
                return result;
            }
            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Remarks are required when requesting an opinion");
                return result;
            }

            String status = customFormApplicationService.requestApplicationOpinion(applicationId, opinionUserId,
                    remarks.trim());
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM",
                    resolveClientIp(request), "REQUEST_OPINION");
            payload.put("opinionUserId", opinionUserId);
            logFormAction("REQUEST_OPINION", request, applicationId, logStatus,
                    "Success".equals(status) ? "Application sent for opinion" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application sent for opinion successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to request opinion");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error requesting application opinion: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/submitApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> submitApplicationOpinion(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("submitApplicationOpinion()");
        Map<String, Object> result = new HashMap<>();
        try {
            Integer applicationId = getIntegerValue(requestBody.get("applicationId"));
            String action = requestBody.get("action") != null ? String.valueOf(requestBody.get("action")) : "";
            String remarks = requestBody.get("remarks") != null ? String.valueOf(requestBody.get("remarks")) : "";

            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }
            if (remarks == null || remarks.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Remarks are required when submitting an opinion");
                return result;
            }

            String status = customFormApplicationService.submitApplicationOpinion(applicationId, action, remarks.trim());
            String logStatus = "Success".equals(status) ? "SUCCESS" : "FAILURE";
            Map<String, Object> payload = buildApprovalPayload(applicationId, remarks, "SYSTEM",
                    resolveClientIp(request), "SUBMIT_OPINION");
            payload.put("opinionAction", action);
            logFormAction("SUBMIT_OPINION", request, applicationId, logStatus,
                    "Success".equals(status) ? "Opinion submitted" : status, payload,
                    logStatus.equals("FAILURE") ? status : null);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Opinion submitted successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to submit opinion");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error submitting application opinion: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }
    @RequestMapping(value = "/sendBackApplicationFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackApplicationFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackApplicationFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        return renderEmailActionCommentForm("Send Back Application",
                "Enter your comments (required), then confirm send back.", "/sendBackApplicationFromEmail",
                applicationId, userId, request);
    }

    @RequestMapping(value = "/sendBackApplicationFromEmail", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackApplicationFromEmailSubmit(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug(
                "sendBackApplicationFromEmailSubmit() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            if (remarks == null || remarks.trim().isEmpty()) {
                return renderEmailActionErrorPage("Comments required",
                        "Please enter comments before sending back. Use your browser back button to return to the form.");
            }
            String finalRemarks = remarks.trim();
            if (isTemplateBuilderApplication(applicationId)) {
                Map<String, Object> requestBody = buildTemplateEmailActionRequest(applicationId, userId, finalRemarks);
                Map<String, Object> result = templateDefinitionController.sendBackTemplateApplication(requestBody,
                        response);
                if ("Success".equals(result.get("status"))) {
                    return renderEmailActionResultPage("Application Sent Back", "The application has been sent back.");
                }
                return renderEmailActionErrorPage("Send Back Failed",
                        String.valueOf(result.getOrDefault("message", "Failed to send back application")));
            }
            String status = getCustomFormApplicationDaoImpl().sendBackApplication(applicationId, finalRemarks, userId);
            if ("Success".equals(status)) {
                return renderEmailActionResultPage("Application Sent Back", "The application has been sent back.");
            }
            return renderEmailActionErrorPage("Send Back Failed",
                    status != null && status.startsWith("Failure:") ? status.substring(8)
                            : "Failed to send back application");
        } catch (Exception ex) {
            logger.error("Error sending back application from email: " + ex.getMessage(), ex);
            return renderEmailActionErrorPage("Error", "An error occurred: " + ex.getMessage());
        }
    }

    @RequestMapping(value = "/sendBackToInitiatorFromEmail", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackToInitiatorFromEmail(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackToInitiatorFromEmail() - applicationId: " + applicationId + ", userId: " + userId);
        return renderEmailActionCommentForm("Send Back To Initiator",
                "Enter your comments (required), then confirm send back to initiator.", "/sendBackToInitiatorFromEmail",
                applicationId, userId, request);
    }

    @RequestMapping(value = "/sendBackToInitiatorFromEmail", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String sendBackToInitiatorFromEmailSubmit(@RequestParam Integer applicationId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendBackToInitiatorFromEmailSubmit() - applicationId: " + applicationId + ", userId: " + userId);
        try {
            if (remarks == null || remarks.trim().isEmpty()) {
                return renderEmailActionErrorPage("Comments required",
                        "Please enter comments before sending back to initiator. Use your browser back button to return to the form.");
            }
            String finalRemarks = remarks.trim();
            if (isTemplateBuilderApplication(applicationId)) {
                Map<String, Object> requestBody = buildTemplateEmailActionRequest(applicationId, userId, finalRemarks);
                Map<String, Object> result = templateDefinitionController
                        .sendBackTemplateApplicationToInitiator(requestBody, response);
                if ("Success".equals(result.get("status"))) {
                    return renderEmailActionResultPage("Application Sent Back To Initiator",
                            "The application has been sent back to the initiator.");
                }
                return renderEmailActionErrorPage("Send Back Failed",
                        String.valueOf(result.getOrDefault("message",
                                "Failed to send back application to initiator")));
            }
            String status = getCustomFormApplicationDaoImpl().sendBackApplicationToInitiator(applicationId, finalRemarks,
                    userId);
            if ("Success".equals(status)) {
                return renderEmailActionResultPage("Application Sent Back To Initiator",
                        "The application has been sent back to the initiator.");
            }
            return renderEmailActionErrorPage("Send Back Failed",
                    status != null && status.startsWith("Failure:") ? status.substring(8)
                            : "Failed to send back application to initiator");
        } catch (Exception ex) {
            logger.error("Error sending back application to initiator from email: " + ex.getMessage(), ex);
            return renderEmailActionErrorPage("Error", "An error occurred: " + ex.getMessage());
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

    @RequestMapping(value = "/sendTemplatePostApprovalEmails", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendTemplatePostApprovalEmails(@RequestParam Integer applicationId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendTemplatePostApprovalEmails() - applicationId: " + applicationId);
        Map<String, Object> result = new HashMap<>();
        try {
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            String status = customFormApplicationService.sendTemplatePostApprovalEmails(applicationId);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Approval emails sent successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to send approval emails");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error sending template post-approval emails: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/sendTemplatePostApprovalEmailsWithPdfs", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendTemplatePostApprovalEmailsWithPdfs(@RequestParam Integer applicationId,
            @RequestParam(value = "initiatorPdf", required = false) MultipartFile initiatorPdf,
            @RequestParam(value = "approverPdf", required = false) MultipartFile approverPdf,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendTemplatePostApprovalEmailsWithPdfs() - applicationId: " + applicationId);
        Map<String, Object> result = new HashMap<>();
        try {
            if (applicationId == null) {
                result.put("status", "Failure");
                result.put("message", "Application ID is required");
                return result;
            }

            byte[] initiatorBytes = initiatorPdf != null && !initiatorPdf.isEmpty() ? initiatorPdf.getBytes() : null;
            byte[] approverBytes = approverPdf != null && !approverPdf.isEmpty() ? approverPdf.getBytes() : null;
            String pdfName = approverPdf != null && approverPdf.getOriginalFilename() != null
                    ? approverPdf.getOriginalFilename()
                    : (initiatorPdf != null ? initiatorPdf.getOriginalFilename() : "template-application.pdf");
            String pdfMime = approverPdf != null && approverPdf.getContentType() != null
                    ? approverPdf.getContentType()
                    : (initiatorPdf != null ? initiatorPdf.getContentType() : "application/pdf");

            String status = customFormApplicationService.sendTemplatePostApprovalEmails(applicationId, initiatorBytes,
                    approverBytes, pdfName, pdfMime);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Approval emails sent successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8)
                        : "Failed to send approval emails");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error sending template post-approval emails with PDFs: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/sendTemplateTestEmailPdf", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendTemplateTestEmailPdf(@RequestParam String recipients,
            @RequestParam String subject,
            @RequestParam String bodyHtml,
            @RequestParam("pdf") MultipartFile pdf,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("sendTemplateTestEmailPdf()");
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.List<String> recipientEmails = new java.util.ArrayList<>();
            try {
                List<String> parsedRecipients = objectMapper.readValue(recipients, new TypeReference<List<String>>() {
                });
                if (parsedRecipients != null) {
                    for (String item : parsedRecipients) {
                        String email = item != null ? item.trim() : "";
                        if (!email.isEmpty() && !recipientEmails.contains(email)) {
                            recipientEmails.add(email);
                        }
                    }
                }
            } catch (Exception parseEx) {
                String[] parts = recipients != null ? recipients.split(",") : new String[0];
                for (String item : parts) {
                    String email = item != null ? item.trim() : "";
                    if (!email.isEmpty() && !recipientEmails.contains(email)) {
                        recipientEmails.add(email);
                    }
                }
            }

            if (recipientEmails.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "No admin recipient email found");
                return result;
            }

            if (pdf == null || pdf.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Template PDF attachment is required");
                return result;
            }

            String attachmentName = pdf.getOriginalFilename() != null && !pdf.getOriginalFilename().trim().isEmpty()
                    ? pdf.getOriginalFilename()
                    : "template-form.pdf";
            emailService.sendHtmlEmailWithAttachment(
                    recipientEmails,
                    subject != null && !subject.trim().isEmpty() ? subject : "Template test email",
                    bodyHtml != null && !bodyHtml.trim().isEmpty() ? bodyHtml : "<p>Attached is the filled template form PDF.</p>",
                    pdf.getBytes(),
                    attachmentName,
                    pdf.getContentType() != null ? pdf.getContentType() : "application/pdf");
            result.put("status", "Success");
            result.put("message", "Template test email PDF sent successfully");
            result.put("recipients", recipientEmails);
            return result;
        } catch (Exception ex) {
            logger.error("Error sending template test email PDF: " + ex.getMessage(), ex);
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

    @PostMapping(value = "/assignPoCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> assignPoCode(@RequestParam Integer applicationId,
            @RequestParam String poCode,
            @RequestParam(required = false) Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("assignPoCode() - applicationId: " + applicationId + ", userId: " + userId);
        Map<String, Object> result = new HashMap<>();
        try {
            String ip = resolveClientIp(request);
            String status = customFormApplicationService.assignPoCode(applicationId, poCode, userId, ip);
            if ("Success".equalsIgnoreCase(status)) {
                result.put("status", "Success");
                result.put("message", "PO code saved successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : status);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            logger.error("Error assigning PO code: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    private com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO getCustomFormApplicationDaoImpl() {
        return (com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomFormApplicationDAO) customFormApplicationDAO;
    }

    private String renderEmailActionCommentForm(String title, String subtitle, String actionPath, Integer applicationId,
            Integer userId, HttpServletRequest request) {
        String terminalStateMessage = getEmailActionTerminalStateMessage(applicationId);
        if (terminalStateMessage != null) {
            return renderEmailActionErrorPage("Action Not Available", terminalStateMessage);
        }
        String contextPath = request != null ? request.getContextPath() : "";
        String actionUrl = contextPath + actionPath;
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + escapeHtml(title) + "</title>"
                + "<style>body{font-family:Arial,sans-serif;background:#f5f5f5;padding:30px}"
                + ".container{background:white;padding:28px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:560px;margin:0 auto}"
                + "h2{margin:0 0 10px 0;color:#2c3e50;font-size:24px}.subtitle{color:#555;margin-bottom:18px}"
                + "label{display:block;color:#2c3e50;font-weight:600;margin-bottom:8px}"
                + "textarea{width:100%;min-height:110px;padding:10px;border:1px solid #d5d5d5;border-radius:6px;resize:vertical;font-size:14px;font-family:Arial,sans-serif;box-sizing:border-box}"
                + ".hint{color:#888;font-size:12px;margin-top:8px}"
                + "button{margin-top:18px;background:#1f6feb;color:#fff;border:none;border-radius:6px;padding:10px 18px;font-size:14px;cursor:pointer}"
                + "button:hover{background:#1558b0}</style></head><body><div class='container'>"
                + "<h2>" + escapeHtml(title) + "</h2>"
                + "<div class='subtitle'>" + escapeHtml(subtitle) + "</div>"
                + "<form method='post' action='" + escapeHtml(actionUrl) + "'>"
                + "<input type='hidden' name='applicationId' value='" + applicationId + "'/>"
                + "<input type='hidden' name='userId' value='" + userId + "'/>"
                + "<label for='remarks'>Comments / remarks <span style='color:#c0392b'>*</span></label>"
                + "<textarea id='remarks' name='remarks' maxlength='2000' required placeholder='Enter comments (required)'></textarea>"
                + "<div class='hint'>Required. Max 2000 characters.</div>"
                + "<button type='submit'>Submit</button></form></div></body></html>";
    }

    private String renderEmailActionResultPage(String title, String message) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + escapeHtml(title) + "</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}"
                + ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                + ".success{color:#27ae60;font-size:24px;margin-bottom:20px}.message{color:#333;font-size:16px;line-height:1.6}</style></head><body>"
                + "<div class='container'><div class='success'>" + escapeHtml(title) + "</div>"
                + "<div class='message'>" + escapeHtml(message) + " You can close this window.</div></div></body></html>";
    }

    private String renderEmailActionErrorPage(String title, String message) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + escapeHtml(title) + "</title>"
                + "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f5f5f5}"
                + ".container{background:white;padding:30px;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);max-width:500px;margin:0 auto}"
                + ".error{color:#e74c3c;font-size:24px;margin-bottom:20px}.message{color:#333;font-size:16px;line-height:1.6}</style></head><body>"
                + "<div class='container'><div class='error'>" + escapeHtml(title) + "</div>"
                + "<div class='message'>" + escapeHtml(message) + "</div></div></body></html>";
    }

    private String getEmailActionTerminalStateMessage(Integer applicationId) {
        if (applicationId == null) {
            return "Application ID is required.";
        }
        try {
            CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
            if (application == null) {
                return "Application not found.";
            }
            String status = application.getTxtStatus() != null ? application.getTxtStatus().trim() : "";
            if ("REJECTED".equalsIgnoreCase(status)) {
                return "This application has already been rejected. No further actions are allowed.";
            }
            if ("APPROVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                return "This application has already been completed. No further actions are allowed.";
            }
            return null;
        } catch (Exception ex) {
            logger.warn("Unable to validate email action state for applicationId=" + applicationId + ": "
                    + ex.getMessage());
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        return RequestMetadataUtil.resolveClientIp(request);
    }

    private void logFormAction(String actionType, HttpServletRequest request, Integer applicationId,
            String status, String message, Map<String, Object> payload, String errorMessage) {
        try {
            int userId = commonService.getCurrentLoggedInUser();
            String username = null;
            if (userId > 0) {
                com.bezkoder.spring.login.admin.dal.entities.CfgTblUser user = commonService.getCurrentUser(userId);
                if (user != null) {
                    username = user.getTxtUserName();
                }
            }
            activityLogService.logActivity(actionType, userId > 0 ? userId : null, username,
                    RequestMetadataUtil.resolveClientIp(request),
                    RequestMetadataUtil.resolveDevice(request),
                    status, message, "APPLICATION", applicationId, payload, errorMessage);
        } catch (Exception e) {
            logger.warn("Failed to log form action: " + e.getMessage());
        }
    }

    private Map<String, Object> buildTemplateEmailActionRequest(Integer applicationId, Integer userId, String remarks) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("applicationId", applicationId);
        requestBody.put("userId", userId);
        requestBody.put("approverUserId", userId);
        requestBody.put("remarks", remarks != null ? remarks : "");
        return requestBody;
    }

    private boolean isTemplateBuilderApplication(Integer applicationId) {
        if (applicationId == null) {
            return false;
        }
        try {
            CfgTblCustomFormApplication application = customFormApplicationService.getApplicationById(applicationId);
            if (application == null || application.getTxtApplicationData() == null) {
                return false;
            }
            String rawJson = String.valueOf(application.getTxtApplicationData()).trim();
            if (rawJson.isEmpty()) {
                return false;
            }
            Map<String, Object> appData = objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {
            });
            return appData.get("templatePayload") instanceof Map;
        } catch (Exception ex) {
            logger.warn("Failed to detect template-builder application for email action, applicationId="
                    + applicationId + ": " + ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildApprovalPayload(Integer applicationId, String remarks, String approvedVia,
            String approvedIp, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", applicationId);
        payload.put("remarks", remarks != null ? remarks : "");
        payload.put("approvedVia", approvedVia != null ? approvedVia : "SYSTEM");
        payload.put("approvedIp", approvedIp != null ? approvedIp : "");
        payload.put("action", action);
        payload.put("targetType", "APPROVAL_HISTORY");

        try {
            CfgTblCustomFormApplication app = customFormApplicationService.getApplicationById(applicationId);
            if (app != null && app.getTxtApprovalHistory() != null && !app.getTxtApprovalHistory().trim().isEmpty()) {
                List<Map<String, Object>> history = objectMapper.readValue(app.getTxtApprovalHistory(),
                        new TypeReference<List<Map<String, Object>>>() {
                        });
                if (!history.isEmpty()) {
                    int lastIndex = history.size() - 1;
                    Map<String, Object> lastEntry = history.get(lastIndex);
                    payload.put("historyIndex", lastIndex);
                    if (lastEntry.get("level") != null) {
                        payload.put("level", lastEntry.get("level"));
                    }
                    if (lastEntry.get("role") != null) {
                        payload.put("role", lastEntry.get("role"));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not enrich approval payload: " + e.getMessage());
        }
        return payload;
    }
}
