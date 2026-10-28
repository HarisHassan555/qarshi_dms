package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.utility.common.IpGeolocationService;
import com.bezkoder.spring.login.admin.utility.common.RequestMetadataUtil;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationOnBehalfListItemDto;
import com.bezkoder.spring.login.sa.bll.services.IUserDelegationService;
import com.bezkoder.spring.login.sa.bll.services.IWorkflowThresholdRuleApplyService;
import com.bezkoder.spring.login.notification.bll.TemplateWorkflowNotificationService;
import com.bezkoder.spring.login.sa.bll.servicesimpl.TemplateEmailDispatchService;
import com.bezkoder.spring.login.sa.bll.servicesimpl.TemplateWorkflowCommandService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
public class TemplateDefinitionController {
    private static final int DEFAULT_MAX_ATTACHMENT_MB = 5;
    private static final String ALL_APPLICATIONS_SUBMENU_URL = "all-applications";

    private final Logger logger = LogManager.getLogger(TemplateDefinitionController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ICustomFormApplicationService customFormApplicationService;

    @Autowired
    private IAppActivityLogService activityLogService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private TemplateEmailDispatchService templateEmailDispatchService;

    @Autowired
    private TemplateWorkflowCommandService templateWorkflowCommandService;

    @Autowired
    private TemplateWorkflowNotificationService templateWorkflowNotificationService;

    @Autowired
    private IpGeolocationService ipGeolocationService;

    @Autowired
    private IUserDelegationService userDelegationService;

    @Autowired
    private IWorkflowThresholdRuleApplyService workflowThresholdRuleApplyService;

    @Autowired
    private IMenuService menuService;

    @Transactional
    @RequestMapping(value = "/saveTemplateApplicationDraft", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> saveTemplateApplicationDraft(@RequestBody CfgTblCustomFormApplication application,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (application == null || application.getSerFormId() == null) {
                throw new IllegalArgumentException("Template form ID is required");
            }
            TemplateDefinition definition = findByFormId(application.getSerFormId());
            if (definition == null) {
                throw new IllegalArgumentException("Template definition not found for this form");
            }
            int actorUserId = commonService.getCurrentLoggedInUser();
            if (actorUserId <= 0) {
                throw new IllegalArgumentException("You must be logged in to save a draft");
            }

            enrichTemplateApplicationPayload(application, definition);
            applySubmitInitiatorAndDelegate(application, definition, actorUserId);
            assertTemplateAttachmentFieldSizeLimit(application.getTxtApplicationData(), actorUserId);

            Integer applicationId = application.getSerApplicationId();
            CfgTblCustomFormApplication target;
            if (applicationId != null && applicationId > 0) {
                target = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
                if (target == null) {
                    throw new IllegalArgumentException("Application not found");
                }
                assertCanEditTemplateDraft(target, actorUserId);
                if (!isDraftStatus(target.getTxtStatus())) {
                    throw new IllegalArgumentException("Only draft applications can be updated as draft");
                }
                target.setTxtApplicationData(application.getTxtApplicationData());
                if (application.getSerSubmittedBy() != null && application.getSerSubmittedBy() > 0) {
                    target.setSerSubmittedBy(application.getSerSubmittedBy());
                }
                if (target.getTxtFormCode() == null || target.getTxtFormCode().trim().isEmpty()) {
                    target.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, application.getTxtFormCode()));
                }
                target.setTxtStatus("DRAFT");
                target.setIntCurrentApprovalLevel(0);
                target.setSerCurrentApprover(null);
                target.setDteModifiedDate(now());
                target.setSerModifiedUser(actorUserId);
                application = target;
            } else {
                if (application.getSerSubmittedBy() == null) {
                    application.setSerSubmittedBy(actorUserId);
                }
                assertCanEditTemplateDraft(application, actorUserId);
                if (application.getTxtFormCode() == null || application.getTxtFormCode().trim().isEmpty()) {
                    application.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, null));
                } else {
                    application.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, application.getTxtFormCode()));
                }
                application.setBlIsActive(application.getBlIsActive() == null ? true : application.getBlIsActive());
                application.setBlIsDeleted(application.getBlIsDeleted() == null ? false : application.getBlIsDeleted());
                application.setBlnStatus(application.getBlnStatus() == null ? true : application.getBlnStatus());
                application.setDteCreatedDate(application.getDteCreatedDate() == null ? now() : application.getDteCreatedDate());
                application.setDteModifiedDate(now());
                application.setSerCreatedUser(application.getSerCreatedUser() == null ? actorUserId : application.getSerCreatedUser());
                application.setSerModifiedUser(actorUserId);
                application.setTxtStatus("DRAFT");
                application.setIntCurrentApprovalLevel(0);
                application.setSerCurrentApprover(null);
                entityManager.persist(application);
            }

            logTemplateAction("FORM_DRAFT_SAVE", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application saved as draft",
                    buildTemplateApplicationPayload(application, "FORM_DRAFT_SAVE", null), null);

            result.put("status", "Success");
            result.put("message", "Draft saved successfully");
            result.put("applicationId", application.getSerApplicationId());
            result.put("formCode", application.getTxtFormCode());
            return result;
        } catch (IllegalArgumentException ex) {
            logTemplateAction("FORM_DRAFT_SAVE", request, application != null ? application.getSerApplicationId() : null,
                    "FAILURE", ex.getMessage(),
                    buildTemplateApplicationPayload(application, "FORM_DRAFT_SAVE", null), ex.getMessage());
            logger.warn("Draft save rejected: " + ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logTemplateAction("FORM_DRAFT_SAVE", request, application != null ? application.getSerApplicationId() : null,
                    "FAILURE", ex.getMessage(),
                    buildTemplateApplicationPayload(application, "FORM_DRAFT_SAVE", null), ex.getMessage());
            logger.error("Error saving template application draft: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @Transactional
    @RequestMapping(value = "/submitTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> submitTemplateApplication(@RequestBody CfgTblCustomFormApplication application,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (application == null || application.getSerFormId() == null) {
                throw new IllegalArgumentException("Template form ID is required");
            }
            TemplateDefinition definition = findByFormId(application.getSerFormId());
            if (definition == null) {
                throw new IllegalArgumentException("Template definition not found for this form");
            }

            Integer existingApplicationId = application.getSerApplicationId();
            if (existingApplicationId != null && existingApplicationId > 0) {
                CfgTblCustomFormApplication existing = entityManager.find(CfgTblCustomFormApplication.class, existingApplicationId);
                if (existing != null && isDraftStatus(existing.getTxtStatus())) {
                    return submitTemplateApplicationFromDraft(application, existing, definition, request, response);
                }
                throw new IllegalArgumentException("Application cannot be submitted from its current state");
            }

            enrichTemplateApplicationPayload(application, definition);
            workflowThresholdRuleApplyService.applyActiveRulesOnSubmit(application);
            int actorUserId = commonService.getCurrentLoggedInUser();
            applySubmitInitiatorAndDelegate(application, definition, actorUserId);
            assertTemplateAttachmentFieldSizeLimit(application.getTxtApplicationData(), actorUserId);
            if (application.getTxtFormCode() == null || application.getTxtFormCode().trim().isEmpty()) {
                application.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, null));
            } else {
                application.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, application.getTxtFormCode()));
            }
            application.setBlIsActive(application.getBlIsActive() == null ? true : application.getBlIsActive());
            application.setBlIsDeleted(application.getBlIsDeleted() == null ? false : application.getBlIsDeleted());
            application.setBlnStatus(application.getBlnStatus() == null ? true : application.getBlnStatus());
            application.setDteCreatedDate(application.getDteCreatedDate() == null ? now() : application.getDteCreatedDate());
            application.setDteModifiedDate(now());
            application.setSerModifiedUser(application.getSerCreatedUser());

            applyTemplateSubmitWorkflowState(application, request);
            entityManager.persist(application);
            entityManager.flush();
            logTemplateAction("FORM_SUBMIT", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application submitted successfully",
                    buildTemplateApplicationPayload(application, "FORM_SUBMIT", null), null);
            templateWorkflowNotificationService.notifySubmitted(application, actorUserId);

            result.put("status", "Success");
            result.put("message", "Template application submitted successfully");
            result.put("applicationId", application.getSerApplicationId());
            result.put("formCode", application.getTxtFormCode());
            return result;
        } catch (Exception ex) {
            logTemplateAction("FORM_SUBMIT", request, application != null ? application.getSerApplicationId() : null,
                    "FAILURE", ex.getMessage(),
                    buildTemplateApplicationPayload(application, "FORM_SUBMIT", null), ex.getMessage());
            logger.error("Error submitting template application: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getTemplateApplicationById", method = RequestMethod.GET)
    public Map<String, Object> getTemplateApplicationById(@RequestParam Integer applicationId,
                                                          @RequestParam(required = false) Integer userId,
                                                          HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(applicationId);
            CfgTblUser submittedBy = application.getSerSubmittedBy() != null
                    ? entityManager.find(CfgTblUser.class, application.getSerSubmittedBy())
                    : null;
            result.put("serApplicationId", application.getSerApplicationId());
            result.put("serFormId", application.getSerFormId());
            result.put("txtFormCode", application.getTxtFormCode());
            result.put("txtApplicationData", sanitizeApplicationDataJson(application.getTxtApplicationData()));
            result.put("txtStatus", application.getTxtStatus());
            result.put("intCurrentApprovalLevel", application.getIntCurrentApprovalLevel());
            result.put("serSubmittedBy", application.getSerSubmittedBy());
            result.put("serCurrentApprover", application.getSerCurrentApprover());
            result.put("txtRemarks", application.getTxtRemarks());
            result.put("txtApprovalHistory", application.getTxtApprovalHistory());
            result.put("txtPriorApprovals", application.getTxtPriorApprovals());
            result.put("currentApproverIds", new ArrayList<>(pendingApproverIdsForCurrentStep(application)));
            if (userId != null && userId > 0) {
                TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
                String workflowUserNotice = resolveOrAlreadyApprovedMessage(application, context, userId);
                if (workflowUserNotice != null) {
                    result.put("workflowUserNotice", workflowUserNotice);
                }
            }
            result.put("txtPdfName", application.getTxtPdfName());
            result.put("txtPdfMime", application.getTxtPdfMime());
            result.put("dteCreatedDate", application.getDteCreatedDate());
            result.put("dteModifiedDate", application.getDteModifiedDate());
            result.put("serCreatedUser", application.getSerCreatedUser());
            result.put("serModifiedUser", application.getSerModifiedUser());
            result.put("submittedByUserName", submittedBy != null ? submittedBy.getTxtUserName() : "");
            result.put("submittedDepartmentName", submittedBy != null ? submittedBy.getTxtDepartmentName() : "");
            result.put("isTemplateBuilderApplication", true);
            result.put("isCapfForm", false);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching template application: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getTemplateApprovalLocationByIp", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTemplateApprovalLocationByIp(@RequestParam("ip") String ip,
                                                               HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (ip == null || ip.trim().isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "IP address is required");
                return result;
            }
            if (IpGeolocationService.isNonPublicIp(ip.trim())) {
                result.put("status", "Failure");
                result.put("message", "Location is not available for local or private network addresses");
                return result;
            }
            return ipGeolocationService.resolve(ip.trim())
                    .map((geo) -> {
                        Map<String, Object> success = new HashMap<>();
                        success.put("status", "Success");
                        success.put("latitude", geo.latitude);
                        success.put("longitude", geo.longitude);
                        if (geo.label != null && !geo.label.isEmpty()) {
                            success.put("locationLabel", geo.label);
                        }
                        return success;
                    })
                    .orElseGet(() -> {
                        result.put("status", "Failure");
                        result.put("message", "Could not resolve location for this IP address");
                        return result;
                    });
        } catch (Exception ex) {
            return failure(result, response, "Error resolving approval location", ex);
        }
    }

    private static final String PORTAL_APPLICATION_PDF_FIELD_ID = "__application_pdf__";

    @RequestMapping(value = "/getCompletedTemplatePortalAttachments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCompletedTemplatePortalAttachments(@RequestParam(defaultValue = "0") Integer page,
                                                                     @RequestParam(defaultValue = "50") Integer pageSize,
                                                                     @RequestParam(required = false) String search,
                                                                     HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 50 : Math.max(1, Math.min(pageSize, 200));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

            // Light metadata only — do not select large JSON/PDF blobs in the ordered list query.
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createQuery(
                            "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, a.dteCreatedDate, " +
                                    "t.txtTemplateName, a.txtPdfName, " +
                                    "CASE WHEN a.blbPdfData IS NULL THEN false ELSE true END " +
                                    "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                                    "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                                    "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) " +
                                    "AND t.serFormId = a.serFormId " +
                                    "AND (a.txtStatus = 'COMPLETED' OR a.txtStatus = 'APPROVED' " +
                                    "OR a.txtStatus = 'completed' OR a.txtStatus = 'approved') " +
                                    "ORDER BY a.dteCreatedDate DESC")
                    .getResultList();

            List<Map<String, Object>> matchedItems = new ArrayList<>();
            for (Object[] row : rows) {
                Integer applicationId = toInteger(row[0]);
                if (applicationId == null || applicationId <= 0) {
                    continue;
                }
                String formCode = row[2] == null ? "" : String.valueOf(row[2]);
                String status = row[3] == null ? "" : String.valueOf(row[3]);
                if (!isCompletedPortalApplicationStatus(status)) {
                    continue;
                }
                Object createdDate = row[4];
                String templateName = row[5] == null ? "" : String.valueOf(row[5]);
                String pdfName = row[6] == null ? "" : String.valueOf(row[6]).trim();
                boolean hasPdf = Boolean.TRUE.equals(row[7]) || "true".equalsIgnoreCase(String.valueOf(row[7]));

                if (hasPdf) {
                    Map<String, Object> pdfItem = buildCompletedApplicationPdfMeta(
                            applicationId, formCode, status, createdDate, templateName, pdfName);
                    if (matchesPortalAttachmentSearch(pdfItem, formCode, templateName, searchText)) {
                        matchedItems.add(pdfItem);
                    }
                }

                String applicationDataRaw = getApplicationJsonById(applicationId);
                for (Map<String, Object> attachmentMeta : extractCompletedPortalAttachmentMetas(
                        applicationId, formCode, status, createdDate, templateName, applicationDataRaw)) {
                    if (matchesPortalAttachmentSearch(attachmentMeta, formCode, templateName, searchText)) {
                        matchedItems.add(attachmentMeta);
                    }
                }
            }

            int total = matchedItems.size();
            int fromIndex = Math.min(safePage * safePageSize, total);
            int toIndex = Math.min(fromIndex + safePageSize, total);
            result.put("status", "Success");
            result.put("items", matchedItems.subList(fromIndex, toIndex));
            result.put("total", total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching completed portal attachments: " + ex.getMessage(), ex);
            // Fallback without CASE/blob-null check if the dialect rejects it.
            try {
                return getCompletedTemplatePortalAttachmentsFallback(page, pageSize, search, response);
            } catch (Exception fallbackEx) {
                logger.error("Fallback completed portal attachments also failed: " + fallbackEx.getMessage(), fallbackEx);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                result.put("status", "Failure");
                result.put("items", java.util.Collections.emptyList());
                result.put("total", 0);
                result.put("message", ex.getMessage());
                return result;
            }
        }
    }

    private Map<String, Object> getCompletedTemplatePortalAttachmentsFallback(Integer page,
                                                                              Integer pageSize,
                                                                              String search,
                                                                              HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        int safePage = page == null || page < 0 ? 0 : page;
        int safePageSize = pageSize == null ? 50 : Math.max(1, Math.min(pageSize, 200));
        String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, a.dteCreatedDate, " +
                                "t.txtTemplateName, a.txtPdfName " +
                                "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                                "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                                "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) " +
                                "AND t.serFormId = a.serFormId " +
                                "AND (a.txtStatus = 'COMPLETED' OR a.txtStatus = 'APPROVED' " +
                                "OR a.txtStatus = 'completed' OR a.txtStatus = 'approved') " +
                                "ORDER BY a.dteCreatedDate DESC")
                .getResultList();

        List<Map<String, Object>> matchedItems = new ArrayList<>();
        for (Object[] row : rows) {
            Integer applicationId = toInteger(row[0]);
            if (applicationId == null || applicationId <= 0) {
                continue;
            }
            String formCode = row[2] == null ? "" : String.valueOf(row[2]);
            String status = row[3] == null ? "" : String.valueOf(row[3]);
            if (!isCompletedPortalApplicationStatus(status)) {
                continue;
            }
            Object createdDate = row[4];
            String templateName = row[5] == null ? "" : String.valueOf(row[5]);
            String pdfName = row[6] == null ? "" : String.valueOf(row[6]).trim();

            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
            boolean hasPdf = application != null && application.getBlbPdfData() != null && application.getBlbPdfData().length > 0;
            if (!hasPdf && application != null) {
                byte[] stage0 = application.getBlbPdfForStage(0);
                hasPdf = stage0 != null && stage0.length > 0;
            }
            if (hasPdf) {
                Map<String, Object> pdfItem = buildCompletedApplicationPdfMeta(
                        applicationId, formCode, status, createdDate, templateName, pdfName);
                if (matchesPortalAttachmentSearch(pdfItem, formCode, templateName, searchText)) {
                    matchedItems.add(pdfItem);
                }
            }

            String applicationDataRaw = application != null ? application.getTxtApplicationData() : getApplicationJsonById(applicationId);
            for (Map<String, Object> attachmentMeta : extractCompletedPortalAttachmentMetas(
                    applicationId, formCode, status, createdDate, templateName, applicationDataRaw)) {
                if (matchesPortalAttachmentSearch(attachmentMeta, formCode, templateName, searchText)) {
                    matchedItems.add(attachmentMeta);
                }
            }
        }

        int total = matchedItems.size();
        int fromIndex = Math.min(safePage * safePageSize, total);
        int toIndex = Math.min(fromIndex + safePageSize, total);
        result.put("status", "Success");
        result.put("items", matchedItems.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        return result;
    }

    @RequestMapping(value = "/getCompletedTemplatePortalAttachmentContent", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCompletedTemplatePortalAttachmentContent(@RequestParam Integer applicationId,
                                                                            @RequestParam String fieldId,
                                                                            @RequestParam(defaultValue = "0") Integer attachmentIndex,
                                                                            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (applicationId == null || applicationId <= 0) {
                throw new IllegalArgumentException("applicationId is required");
            }
            if (fieldId == null || fieldId.trim().isEmpty()) {
                throw new IllegalArgumentException("fieldId is required");
            }

            CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
            if (application == null || Boolean.TRUE.equals(application.getBlIsDeleted())) {
                throw new IllegalArgumentException("Application not found");
            }
            if (!isCompletedPortalApplicationStatus(String.valueOf(application.getTxtStatus()))) {
                throw new IllegalArgumentException("Only completed/approved applications can be used for portal attachments");
            }

            Map<String, Object> payload;
            if (PORTAL_APPLICATION_PDF_FIELD_ID.equals(fieldId.trim())) {
                payload = resolveCompletedApplicationPdfPayload(application);
            } else {
                if (attachmentIndex == null || attachmentIndex < 0) {
                    throw new IllegalArgumentException("attachmentIndex is required");
                }
                payload = resolvePortalAttachmentPayload(
                        application.getTxtApplicationData(), fieldId.trim(), attachmentIndex);
            }
            if (payload == null || payload.isEmpty()) {
                throw new IllegalArgumentException("Attachment content was not found");
            }

            result.put("status", "Success");
            result.put("attachment", payload);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching completed portal attachment content: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private Map<String, Object> buildCompletedApplicationPdfMeta(Integer applicationId,
                                                                 String formCode,
                                                                 String status,
                                                                 Object createdDate,
                                                                 String templateName,
                                                                 String pdfName) {
        String safeFormCode = formCode == null || formCode.trim().isEmpty() ? ("APP-" + applicationId) : formCode.trim();
        String fileName = pdfName != null && !pdfName.trim().isEmpty()
                ? pdfName.trim()
                : (safeFormCode + ".pdf");
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        Map<String, Object> item = new HashMap<>();
        item.put("applicationId", applicationId);
        item.put("formCode", formCode);
        item.put("status", status);
        item.put("createdDate", createdDate);
        item.put("templateName", templateName);
        item.put("fieldId", PORTAL_APPLICATION_PDF_FIELD_ID);
        item.put("fieldLabel", "Completed Document");
        item.put("attachmentIndex", 0);
        item.put("fileName", fileName);
        item.put("mimeType", "application/pdf");
        item.put("sourceType", "APPLICATION_PDF");
        return item;
    }

    private Map<String, Object> resolveCompletedApplicationPdfPayload(CfgTblCustomFormApplication application) {
        if (application == null) {
            return null;
        }
        byte[] pdfBytes = application.getBlbPdfData();
        if (pdfBytes == null || pdfBytes.length == 0) {
            pdfBytes = application.getBlbPdfForStage(0);
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }
        String formCode = application.getTxtFormCode();
        String safeFormCode = formCode == null || formCode.trim().isEmpty()
                ? ("APP-" + application.getSerApplicationId())
                : formCode.trim();
        String fileName = application.getTxtPdfName();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = safeFormCode + ".pdf";
        } else if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            fileName = fileName.trim() + ".pdf";
        } else {
            fileName = fileName.trim();
        }
        String mimeType = application.getTxtPdfMime();
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/pdf";
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(pdfBytes);
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName);
        payload.put("mimeType", mimeType);
        payload.put("base64", base64);
        payload.put("dataUrl", "data:" + mimeType + ";base64," + base64);
        return payload;
    }

    private boolean matchesPortalAttachmentSearch(Map<String, Object> item,
                                                  String formCode,
                                                  String templateName,
                                                  String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String haystack = String.valueOf(item.getOrDefault("fileName", "")).toLowerCase(Locale.ROOT)
                + " " + (formCode == null ? "" : formCode.toLowerCase(Locale.ROOT))
                + " " + (templateName == null ? "" : templateName.toLowerCase(Locale.ROOT))
                + " " + String.valueOf(item.getOrDefault("fieldLabel", "")).toLowerCase(Locale.ROOT);
        return haystack.contains(searchText);
    }

    private boolean isCompletedPortalApplicationStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "COMPLETED".equals(normalized) || "APPROVED".equals(normalized);
    }

    @Transactional
    @RequestMapping(value = "/approveTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> approveTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        return handleTemplateDecision(requestBody, "APPROVED", request, response);
    }

    @Transactional
    @RequestMapping(value = "/rejectTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> rejectTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        return handleTemplateDecision(requestBody, "REJECTED", request, response);
    }

    @Transactional
    @RequestMapping(value = "/sendBackTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            int currentLevel = safeLevel(application);
            TemplateStep currentStep = context.stepAt(currentLevel);
            TemplateWorkflowActor actor = resolveTemplateWorkflowActor(requestBody, application, context, currentStep,
                    "You are not authorized to send back this template application");
            ensureActorHasUploadedSignature(actor.performedBy);
            int targetLevel = Math.max(0, currentLevel - 1);
            TemplateStep targetStep = context.stepAt(targetLevel);
            appendTemplateHistory(application, currentStep, actor.performedBy, actor.onBehalfOf, "SENT_BACK", remarks, displayLevelForIndex(targetLevel));
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(targetLevel));
            application.setSerCurrentApprover(firstApproverId(targetStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actor.performedBy);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("SEND_BACK", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent back successfully",
                    buildTemplateApplicationPayload(application, "SEND_BACK", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(), "template send-back notification", true);
            templateWorkflowNotificationService.notifySentBack(application, actor.performedBy, remarks);
            return success("Application sent back successfully");
        } catch (Exception ex) {
            logTemplateAction("SEND_BACK", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "SEND_BACK"), ex.getMessage());
            return failure(result, response, "Error sending back template application", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/sendBackTemplateApplicationToInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackTemplateApplicationToInitiator(@RequestBody Map<String, Object> requestBody,
                                                                      HttpServletRequest request,
                                                                      HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            TemplateWorkflowActor actor = resolveTemplateWorkflowActor(requestBody, application, context, currentStep,
                    "You are not authorized to send back this template application");
            ensureActorHasUploadedSignature(actor.performedBy);
            appendTemplateHistory(application, currentStep, actor.performedBy, actor.onBehalfOf, "SENT_BACK_TO_INITIATOR", remarks, 1);
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("PENDING");
            application.setIntCurrentApprovalLevel(1);
            application.setSerCurrentApprover(application.getSerSubmittedBy());
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actor.performedBy);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("SEND_BACK", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent back to initiator successfully",
                    buildTemplateApplicationPayload(application, "SEND_BACK_TO_INITIATOR", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(),
                    "template send-back-to-initiator notification", true);
            templateWorkflowNotificationService.notifySentBackToInitiator(application, actor.performedBy, remarks);
            return success("Application sent back to initiator successfully");
        } catch (Exception ex) {
            logTemplateAction("SEND_BACK", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "SEND_BACK_TO_INITIATOR"), ex.getMessage());
            return failure(result, response, "Error sending back template application to initiator", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/resubmitTemplateApplicationFromInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> resubmitTemplateApplicationFromInitiator(@RequestBody Map<String, Object> requestBody,
                                                                        HttpServletRequest request,
                                                                        HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            if (application.getSerSubmittedBy() == null || !application.getSerSubmittedBy().equals(actorId)) {
                throw new IllegalArgumentException("Only the initiator can resubmit this template application");
            }
            ensureActorHasUploadedSignature(actorId);
            workflowThresholdRuleApplyService.applyActiveRulesOnSubmit(application);
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep firstStep = context.stepAt(0);
            String clientIp = RequestMetadataUtil.resolveClientIp(request);
            appendTemplateHistory(application, context.initiatorStep(), actorId, "RESUBMITTED_BY_INITIATOR", remarks, 1,
                    clientIp, "SYSTEM");
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(0));
            application.setSerCurrentApprover(firstApproverId(firstStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("RESUBMIT_INITIATOR", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application resubmitted by initiator",
                    buildTemplateApplicationPayload(application, "RESUBMITTED_BY_INITIATOR", remarks), null);
            templateWorkflowNotificationService.notifyResubmitted(application, actorId, remarks);
            return success("Application resubmitted successfully");
        } catch (Exception ex) {
            logTemplateAction("RESUBMIT_INITIATOR", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "RESUBMITTED_BY_INITIATOR"), ex.getMessage());
            return failure(result, response, "Error resubmitting template application", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/requestTemplateApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> requestTemplateApplicationOpinion(@RequestBody Map<String, Object> requestBody,
                                                                 HttpServletRequest request,
                                                                 HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            Integer actorId = toInteger(requestBody.get("userId"));
            Integer opinionUserId = toInteger(requestBody.get("opinionUserId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            assertActorAuthorizedForTemplateStep(application, context, currentStep, actorId,
                    "You are not authorized to request opinion for this template application");
            ensureActorHasUploadedSignature(actorId);
            if (opinionUserId == null || opinionUserId <= 0) {
                throw new IllegalArgumentException("Opinion user is required");
            }
            CfgTblUser requester = actorId != null ? entityManager.find(CfgTblUser.class, actorId) : null;
            CfgTblUser opinionUser = entityManager.find(CfgTblUser.class, opinionUserId);
            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> opinion = new HashMap<>();
            opinion.put("active", true);
            opinion.put("requestedBy", actorId);
            opinion.put("requestedByName", requester != null ? requester.getTxtUserName() : "");
            opinion.put("requestedFrom", opinionUserId);
            opinion.put("requestedFromName", opinionUser != null ? opinionUser.getTxtUserName() : "");
            opinion.put("returnLevel", safeLevel(application));
            opinion.put("remarks", remarks);
            opinion.put("requestedDate", now().toString());
            appData.put("templateOpinionRequest", opinion);
            application.setTxtApplicationData(writeJson(appData));
            appendTemplateHistory(application, currentStep, actorId, "OPINION_REQUESTED", remarks, displayLevelForIndex(safeLevel(application)));
            application.setTxtStatus("OPINION_PENDING");
            application.setSerCurrentApprover(opinionUserId);
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            logTemplateAction("REQUEST_OPINION", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent for opinion successfully",
                    buildTemplateApplicationPayload(application, "OPINION_REQUESTED", remarks), null);
            templateWorkflowCommandService.mergeAndQueuePostApprovalEmails(application,
                    "template opinion request notification", false);
            templateWorkflowNotificationService.notifyOpinionRequested(application, actorId, opinionUserId, remarks);
            return success("Application sent for opinion successfully");
        } catch (Exception ex) {
            logTemplateAction("REQUEST_OPINION", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "OPINION_REQUESTED"), ex.getMessage());
            return failure(result, response, "Error requesting template opinion", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/submitTemplateApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> submitTemplateApplicationOpinion(@RequestBody Map<String, Object> requestBody,
                                                                HttpServletRequest request,
                                                                HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            String action = valueAsString(requestBody.get("action"));
            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> opinion = asMap(appData.get("templateOpinionRequest"));
            if (!Boolean.TRUE.equals(opinion.get("active")) || !actorId.equals(toInteger(opinion.get("requestedFrom")))) {
                throw new IllegalArgumentException("No active opinion request is assigned to this user");
            }
            ensureActorHasUploadedSignature(actorId);
            int returnLevel = toInteger(opinion.get("returnLevel")) != null ? toInteger(opinion.get("returnLevel")) : safeLevel(application);
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            appendTemplateHistory(application, context.stepAt(returnLevel), actorId,
                    "reject".equalsIgnoreCase(action) ? "OPINION_REJECTED" : "OPINION_APPROVED", remarks, displayLevelForIndex(returnLevel));
            opinion.put("active", false);
            opinion.put("action", action);
            opinion.put("completedDate", now().toString());
            appData.put("templateOpinionRequest", opinion);
            application.setTxtApplicationData(writeJson(appData));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(returnLevel));
            application.setSerCurrentApprover(toInteger(opinion.get("requestedBy")));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            logTemplateAction("SUBMIT_OPINION", request, application.getSerApplicationId(), "SUCCESS",
                    "Template opinion submitted successfully",
                    buildTemplateApplicationPayload(application,
                            "reject".equalsIgnoreCase(action) ? "OPINION_REJECTED" : "OPINION_APPROVED", remarks), null);
            templateWorkflowCommandService.mergeAndQueuePostApprovalEmails(application,
                    "template opinion submission notification", false);
            templateWorkflowNotificationService.notifyOpinionSubmitted(application, actorId, action, remarks);
            return success("Opinion submitted successfully");
        } catch (Exception ex) {
            logTemplateAction("SUBMIT_OPINION", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "SUBMIT_OPINION"), ex.getMessage());
            return failure(result, response, "Error submitting template opinion", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/holdTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> holdTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            String reason = valueAsString(requestBody.get("remarks"));
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Hold reason is required");
            }
            String status = valueAsString(application.getTxtStatus()).trim().toUpperCase(Locale.ROOT);
            if (!"IN_PROGRESS".equals(status) && !"PENDING".equals(status)) {
                throw new IllegalStateException("Only in-progress or pending template applications can be put on Hold");
            }
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            TemplateWorkflowActor actor = resolveTemplateWorkflowActor(requestBody, application, context, currentStep,
                    "You are not authorized to hold this template application");

            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> holdState = new HashMap<>();
            holdState.put("active", true);
            holdState.put("previousStatus", application.getTxtStatus());
            holdState.put("previousLevel", application.getIntCurrentApprovalLevel());
            holdState.put("previousApprover", application.getSerCurrentApprover());
            holdState.put("reason", reason.trim());
            holdState.put("heldBy", actor.performedBy);
            holdState.put("heldDate", now().toString());
            appData.put("templateHoldState", holdState);
            application.setTxtApplicationData(writeJson(appData));

            appendTemplateHistory(application, currentStep, actor.performedBy, actor.onBehalfOf, "HOLD", reason.trim(),
                    displayLevelForIndex(safeLevel(application)));
            application.setTxtStatus("HOLD");
            application.setTxtRemarks(reason.trim());
            touchTemplateApplication(application, actor.performedBy);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("HOLD", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application put on Hold",
                    buildTemplateApplicationPayload(application, "HOLD", reason.trim()), null);
            templateWorkflowNotificationService.notifyHold(application, actor.performedBy, reason.trim());
            return success("Application put on Hold successfully");
        } catch (Exception ex) {
            logTemplateAction("HOLD", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "HOLD"), ex.getMessage());
            return failure(result, response, "Error holding template application", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/unholdTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> unholdTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            Integer actorId = toInteger(requestBody.get("userId"));
            if (actorId == null || actorId <= 0) {
                actorId = commonService.getCurrentLoggedInUser();
            }
            String remarks = valueAsString(requestBody.get("remarks"));
            String status = valueAsString(application.getTxtStatus()).trim().toUpperCase(Locale.ROOT);
            if (!"HOLD".equals(status)) {
                throw new IllegalStateException("Application is not on Hold");
            }

            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> holdState = asMap(appData.get("templateHoldState"));
            if (!Boolean.TRUE.equals(holdState.get("active"))
                    && !"true".equalsIgnoreCase(String.valueOf(holdState.get("active")))) {
                throw new IllegalStateException("No active Hold state found for this application");
            }

            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            int resumeLevelIndex = safeLevel(application);
            TemplateStep currentStep = context.stepAt(resumeLevelIndex);
            Integer heldBy = toInteger(holdState.get("heldBy"));
            boolean isHolder = heldBy != null && actorId != null && heldBy.equals(actorId);
            TemplateWorkflowActor actor = isHolder
                    ? new TemplateWorkflowActor(actorId, null)
                    : resolveTemplateWorkflowActor(requestBody, application, context, currentStep,
                    "You are not authorized to unhold this template application");

            String previousStatus = valueAsString(holdState.get("previousStatus"));
            if (previousStatus == null || previousStatus.trim().isEmpty()) {
                previousStatus = "IN_PROGRESS";
            }
            Integer previousLevel = toInteger(holdState.get("previousLevel"));
            Integer previousApprover = toInteger(holdState.get("previousApprover"));

            String unholdRemarks = (remarks != null && !remarks.trim().isEmpty())
                    ? remarks.trim()
                    : "Application taken off Hold";
            appendTemplateHistory(application, currentStep, actor.performedBy, actor.onBehalfOf, "UNHOLD", unholdRemarks,
                    displayLevelForIndex(resumeLevelIndex));

            holdState.put("active", false);
            holdState.put("unheldBy", actor.performedBy);
            holdState.put("unheldDate", now().toString());
            holdState.put("unholdRemarks", unholdRemarks);
            appData.put("templateHoldState", holdState);
            application.setTxtApplicationData(writeJson(appData));
            application.setTxtStatus(previousStatus.trim().toUpperCase(Locale.ROOT));
            if (previousLevel != null) {
                application.setIntCurrentApprovalLevel(previousLevel);
            }
            application.setSerCurrentApprover(previousApprover);
            application.setTxtRemarks(unholdRemarks);
            touchTemplateApplication(application, actor.performedBy);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("UNHOLD", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application taken off Hold",
                    buildTemplateApplicationPayload(application, "UNHOLD", unholdRemarks), null);
            templateWorkflowNotificationService.notifyUnhold(application, actor.performedBy, unholdRemarks);
            return success("Application taken off Hold successfully");
        } catch (Exception ex) {
            logTemplateAction("UNHOLD", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "UNHOLD"), ex.getMessage());
            return failure(result, response, "Error unholding template application", ex);
        }
    }

    @RequestMapping(value = "/getAllTemplateDefinitions", method = RequestMethod.GET)
    public List<TemplateDefinition> getAllTemplateDefinitions(@RequestParam(required = false) Integer userId,
                                                              HttpServletResponse response) {
        try {
            List<TemplateDefinition> definitions = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ORDER BY t.dteCreatedDate DESC",
                    TemplateDefinition.class)
                    .getResultList();
            return filterVisibleTemplateDefinitions(definitions, userId);
        } catch (Exception ex) {
            logger.error("Error fetching template definitions: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getTemplateDefinitionSummaries", method = RequestMethod.GET)
    public Map<String, Object> getTemplateDefinitionSummaries(@RequestParam(required = false) Integer userId,
                                                              @RequestParam(defaultValue = "0") Integer page,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(required = false) String search,
                                                              @RequestParam(defaultValue = "false") Boolean draftsOnly,
                                                              HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            boolean draftListRequested = Boolean.TRUE.equals(draftsOnly);
            if (draftListRequested && !isAdminUser(userId)) {
                result.put("items", new ArrayList<>());
                result.put("total", 0);
                result.put("page", safePage);
                result.put("pageSize", safePageSize);
                return result;
            }

            List<TemplateDefinition> definitions = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ORDER BY t.dteCreatedDate DESC",
                    TemplateDefinition.class)
                    .getResultList();

            List<Map<String, Object>> summaries = new ArrayList<>();
            for (TemplateDefinition definition : filterVisibleTemplateDefinitions(definitions, userId)) {
                boolean isDraft = isTemplateDefinitionDraft(definition);
                if (draftListRequested && !isDraft) {
                    continue;
                }
                if (!draftListRequested && isDraft) {
                    continue;
                }
                Map<String, Object> summary = toTemplateDefinitionSummary(definition, userId);
                if (matchesTemplateSummarySearch(summary, searchText)) {
                    summaries.add(summary);
                }
            }

            int total = summaries.size();
            int fromIndex = Math.min(safePage * safePageSize, total);
            int toIndex = Math.min(fromIndex + safePageSize, total);

            result.put("items", summaries.subList(fromIndex, toIndex));
            result.put("total", total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching template definition summaries: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            result.put("items", new ArrayList<>());
            result.put("total", 0);
            result.put("page", 0);
            result.put("pageSize", 10);
            return result;
        }
    }

    @RequestMapping(value = "/getTemplateDefinitionByFormId", method = RequestMethod.GET)
    public TemplateDefinition getTemplateDefinitionByFormId(@RequestParam Integer formId,
                                                            @RequestParam(required = false) Integer userId,
                                                            @RequestParam(required = false, defaultValue = "true") Boolean includeBackground,
                                                            HttpServletResponse response) {
        try {
            List<TemplateDefinition> matches = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false)",
                    TemplateDefinition.class)
                    .setParameter("formId", formId)
                    .setMaxResults(1)
                    .getResultList();
            if (matches.isEmpty()) {
                return null;
            }
            TemplateDefinition definition = matches.get(0);
            if (canAccessTemplateDefinition(definition, userId)) {
                TemplateDefinition sanitized = sanitizeTemplateDefinitionResponse(definition, Boolean.TRUE.equals(includeBackground));
                applyListingAccessMetadata(sanitized, definition, userId);
                return sanitized;
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (Exception ex) {
            logger.error("Error fetching template definition: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getTemplateDefinitionBackground", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTemplateDefinitionBackground(@RequestParam Integer formId,
                                                               @RequestParam(required = false) Integer userId,
                                                               HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<TemplateDefinition> matches = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false)",
                    TemplateDefinition.class)
                    .setParameter("formId", formId)
                    .setMaxResults(1)
                    .getResultList();
            if (matches.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Template not found");
                return result;
            }
            TemplateDefinition definition = matches.get(0);
            if (!canAccessTemplateDefinition(definition, userId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "Access denied");
                return result;
            }
            Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
            Object background = payload.get("background");
            result.put("status", "Success");
            result.put("background", background);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching template background: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getMyTemplateApplications", method = RequestMethod.GET)
    public Map<String, Object> getMyTemplateApplications(@RequestParam Integer userId,
                                                         @RequestParam(defaultValue = "false") Boolean all,
                                                         @RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String search,
                                                         HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase();
            boolean hasSearch = !searchText.isEmpty();

            String whereClause = "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                    "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ";
            if (!Boolean.TRUE.equals(all)) {
                whereClause += "AND a.serSubmittedBy = :userId ";
            }
            if (hasSearch) {
                whereClause += "AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :search " +
                        "OR LOWER(COALESCE(a.txtStatus, '')) LIKE :search " +
                        "OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :search) ";
            }

            javax.persistence.Query countQuery = entityManager.createQuery(
                    "SELECT COUNT(a.serApplicationId) " +
                            "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                            whereClause +
                            "AND t.serFormId = a.serFormId");
            if (!Boolean.TRUE.equals(all)) {
                countQuery.setParameter("userId", userId);
            }
            if (hasSearch) {
                countQuery.setParameter("search", "%" + searchText + "%");
            }
            Long total = (Long) countQuery.getSingleResult();

            javax.persistence.Query rowQuery = entityManager.createQuery(
                    "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, " +
                            "a.intCurrentApprovalLevel, a.serSubmittedBy, a.dteCreatedDate, t.txtTemplateName " +
                            "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                            whereClause +
                            "AND t.serFormId = a.serFormId " +
                            "ORDER BY a.dteCreatedDate DESC");
            if (!Boolean.TRUE.equals(all)) {
                rowQuery.setParameter("userId", userId);
            }
            if (hasSearch) {
                rowQuery.setParameter("search", "%" + searchText + "%");
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = rowQuery
                    .setFirstResult(safePage * safePageSize)
                    .setMaxResults(safePageSize)
                    .getResultList();

            java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("serApplicationId", row[0]);
                item.put("serFormId", row[1]);
                item.put("txtFormCode", row[2]);
                item.put("txtStatus", row[3]);
                item.put("intCurrentApprovalLevel", row[4]);
                item.put("serSubmittedBy", row[5]);
                item.put("dteCreatedDate", row[6]);
                item.put("templateName", row[7]);
                items.add(item);
            }

            result.put("items", items);
            result.put("total", total == null ? 0 : total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching my template applications: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("items", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getAuditAllTemplateApplications", method = RequestMethod.GET)
    public Map<String, Object> getAuditAllTemplateApplications(@RequestParam Integer userId,
                                                               @RequestParam(defaultValue = "0") Integer page,
                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                               @RequestParam(required = false) String search,
                                                               @RequestParam(required = false) Integer departmentId,
                                                               @RequestParam(required = false) String departmentIds,
                                                               @RequestParam(required = false) Integer serFormId,
                                                               @RequestParam(required = false) String serFormIds,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String statuses,
                                                               @RequestParam(required = false) String submittedByUserIds,
                                                               @RequestParam(required = false) String dateFrom,
                                                               @RequestParam(required = false) String dateTo,
                                                               HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int actorId = userId != null && userId > 0 ? userId : commonService.getCurrentLoggedInUser();
            if (!hasAllApplicationsAuditAccess(actorId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to view all applications.");
                result.put("items", java.util.Collections.emptyList());
                result.put("total", 0);
                return result;
            }

            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            boolean hasSearch = !searchText.isEmpty();

            List<Integer> departmentIdFilter = mergeIntegerCsvFilter(departmentIds, departmentId);
            List<Integer> serFormIdFilter = mergeIntegerCsvFilter(serFormIds, serFormId);
            List<String> statusFilter = mergeStatusCsvFilter(statuses, status);
            List<Integer> submittedByFilter = splitIntegerCsvParam(submittedByUserIds);
            String fromDate = normalizeDateParam(dateFrom);
            String toDate = normalizeDateParam(dateTo);

            StringBuilder whereClause = new StringBuilder(
                    "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ");
            if (!departmentIdFilter.isEmpty()) {
                whereClause.append("AND u.hrTblDepartment.serDepartmentId IN :departmentIds ");
            }
            if (!serFormIdFilter.isEmpty()) {
                whereClause.append("AND a.serFormId IN :serFormIds ");
            }
            if (!statusFilter.isEmpty()) {
                whereClause.append("AND UPPER(COALESCE(a.txtStatus, '')) IN :statusFilters ");
            }
            if (!submittedByFilter.isEmpty()) {
                whereClause.append("AND a.serSubmittedBy IN :submittedByUserIds ");
            }
            if (fromDate != null) {
                whereClause.append("AND a.dteCreatedDate >= :auditFromDate ");
            }
            if (toDate != null) {
                whereClause.append("AND a.dteCreatedDate <= :auditToDate ");
            }
            if (hasSearch) {
                whereClause.append("AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :search " +
                        "OR LOWER(COALESCE(a.txtStatus, '')) LIKE :search " +
                        "OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :search " +
                        "OR LOWER(COALESCE(u.txtUserName, '')) LIKE :search " +
                        "OR LOWER(COALESCE(u.txtDepartmentName, '')) LIKE :search) ");
            }

            String where = whereClause.toString();
            javax.persistence.Query countQuery = entityManager.createQuery(
                    "SELECT COUNT(a.serApplicationId) " +
                            "FROM CfgTblCustomFormApplication a " +
                            "INNER JOIN TemplateDefinition t ON t.serFormId = a.serFormId " +
                            "LEFT JOIN CfgTblUser u ON u.serUserId = a.serSubmittedBy " +
                            where);
            bindAuditAllApplicationsParams(countQuery, departmentIdFilter, serFormIdFilter, statusFilter,
                    submittedByFilter, fromDate, toDate, hasSearch, searchText);

            Long total = (Long) countQuery.getSingleResult();

            javax.persistence.Query rowQuery = entityManager.createQuery(
                    "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, " +
                            "a.intCurrentApprovalLevel, a.serSubmittedBy, a.dteCreatedDate, t.txtTemplateName, " +
                            "COALESCE(u.txtUserName, ''), COALESCE(u.txtDepartmentName, '') " +
                            "FROM CfgTblCustomFormApplication a " +
                            "INNER JOIN TemplateDefinition t ON t.serFormId = a.serFormId " +
                            "LEFT JOIN CfgTblUser u ON u.serUserId = a.serSubmittedBy " +
                            where +
                            "ORDER BY a.dteCreatedDate DESC");
            bindAuditAllApplicationsParams(rowQuery, departmentIdFilter, serFormIdFilter, statusFilter,
                    submittedByFilter, fromDate, toDate, hasSearch, searchText);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = rowQuery
                    .setFirstResult(safePage * safePageSize)
                    .setMaxResults(safePageSize)
                    .getResultList();

            List<Map<String, Object>> items = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("serApplicationId", row[0]);
                item.put("serFormId", row[1]);
                item.put("txtFormCode", row[2]);
                item.put("txtStatus", row[3]);
                item.put("intCurrentApprovalLevel", row[4]);
                item.put("serSubmittedBy", row[5]);
                item.put("dteCreatedDate", row[6]);
                item.put("templateName", row[7]);
                item.put("submittedByUserName", row[8]);
                item.put("submittedDepartmentName", row[9]);
                items.add(item);
            }

            result.put("status", "Success");
            result.put("items", items);
            result.put("total", total == null ? 0 : total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching audit all template applications: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("items", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getAuditAllApplicationsFilterOptions", method = RequestMethod.GET)
    public Map<String, Object> getAuditAllApplicationsFilterOptions(@RequestParam Integer userId,
                                                                    HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int actorId = userId != null && userId > 0 ? userId : commonService.getCurrentLoggedInUser();
            if (!hasAllApplicationsAuditAccess(actorId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to view all applications.");
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Object[]> departmentRows = entityManager.createQuery(
                    "SELECT DISTINCT d.serDepartmentId, d.txtDepartmentName " +
                            "FROM CfgTblCustomFormApplication a " +
                            "INNER JOIN CfgTblUser u ON u.serUserId = a.serSubmittedBy " +
                            "INNER JOIN HrTblDepartment d ON d.serDepartmentId = u.hrTblDepartment.serDepartmentId " +
                            "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false) " +
                            "ORDER BY d.txtDepartmentName ASC")
                    .getResultList();
            List<Map<String, Object>> departments = new ArrayList<>();
            for (Object[] row : departmentRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("serDepartmentId", row[0]);
                item.put("txtDepartmentName", row[1]);
                departments.add(item);
            }

            @SuppressWarnings("unchecked")
            List<Object[]> templateRows = entityManager.createQuery(
                    "SELECT DISTINCT t.serFormId, t.txtTemplateName " +
                            "FROM CfgTblCustomFormApplication a " +
                            "INNER JOIN TemplateDefinition t ON t.serFormId = a.serFormId " +
                            "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) " +
                            "ORDER BY t.txtTemplateName ASC")
                    .getResultList();
            List<Map<String, Object>> templates = new ArrayList<>();
            for (Object[] row : templateRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("serFormId", row[0]);
                item.put("templateName", row[1]);
                templates.add(item);
            }

            @SuppressWarnings("unchecked")
            List<String> statuses = entityManager.createQuery(
                    "SELECT DISTINCT UPPER(COALESCE(a.txtStatus, '')) " +
                            "FROM CfgTblCustomFormApplication a " +
                            "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND COALESCE(a.txtStatus, '') <> '' " +
                            "ORDER BY 1 ASC",
                    String.class)
                    .getResultList();

            @SuppressWarnings("unchecked")
            List<Object[]> submitterRows = entityManager.createQuery(
                    "SELECT DISTINCT u.serUserId, COALESCE(u.txtUserName, CONCAT('User ', u.serUserId)) " +
                            "FROM CfgTblCustomFormApplication a " +
                            "INNER JOIN CfgTblUser u ON u.serUserId = a.serSubmittedBy " +
                            "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND (u.blIsDeleted IS NULL OR u.blIsDeleted = false) " +
                            "ORDER BY 2 ASC")
                    .getResultList();
            List<Map<String, Object>> submitters = new ArrayList<>();
            for (Object[] row : submitterRows) {
                Map<String, Object> item = new HashMap<>();
                item.put("serUserId", row[0]);
                item.put("txtUserName", row[1]);
                submitters.add(item);
            }

            result.put("status", "Success");
            result.put("departments", departments);
            result.put("templates", templates);
            result.put("statuses", statuses);
            result.put("submitters", submitters);
            return result;
        } catch (Exception ex) {
            logger.error("Error loading audit all applications filter options: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private void bindAuditAllApplicationsParams(javax.persistence.Query query,
                                                List<Integer> departmentIds,
                                                List<Integer> serFormIds,
                                                List<String> statusFilters,
                                                List<Integer> submittedByUserIds,
                                                String fromDate,
                                                String toDate,
                                                boolean hasSearch,
                                                String searchText) {
        if (departmentIds != null && !departmentIds.isEmpty()) {
            query.setParameter("departmentIds", departmentIds);
        }
        if (serFormIds != null && !serFormIds.isEmpty()) {
            query.setParameter("serFormIds", serFormIds);
        }
        if (statusFilters != null && !statusFilters.isEmpty()) {
            query.setParameter("statusFilters", statusFilters);
        }
        if (submittedByUserIds != null && !submittedByUserIds.isEmpty()) {
            query.setParameter("submittedByUserIds", submittedByUserIds);
        }
        if (fromDate != null) {
            query.setParameter("auditFromDate", Timestamp.valueOf(fromDate + " 00:00:00"));
        }
        if (toDate != null) {
            query.setParameter("auditToDate", Timestamp.valueOf(toDate + " 23:59:59"));
        }
        if (hasSearch) {
            query.setParameter("search", "%" + searchText + "%");
        }
    }

    private List<Integer> mergeIntegerCsvFilter(String csv, Integer legacySingle) {
        List<Integer> values = splitIntegerCsvParam(csv);
        if (values.isEmpty() && legacySingle != null && legacySingle > 0) {
            values.add(legacySingle);
        }
        return values;
    }

    private List<String> mergeStatusCsvFilter(String csv, String legacySingle) {
        List<String> values = new ArrayList<>();
        for (String part : splitCsvParam(csv)) {
            values.add(part.toUpperCase(Locale.ROOT));
        }
        if (values.isEmpty() && legacySingle != null && !legacySingle.trim().isEmpty()) {
            values.add(legacySingle.trim().toUpperCase(Locale.ROOT));
        }
        return values;
    }

    private List<Integer> splitIntegerCsvParam(String value) {
        List<Integer> ids = new ArrayList<>();
        for (String part : splitCsvParam(value)) {
            try {
                int parsed = Integer.parseInt(part);
                if (parsed > 0 && !ids.contains(parsed)) {
                    ids.add(parsed);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return ids;
    }

    private boolean hasAllApplicationsAuditAccess(int userId) {
        if (userId <= 0) {
            return false;
        }
        CfgTblUser user = commonService.getCurrentUser(userId);
        if (user == null || user.getCfgTblRole() == null) {
            return false;
        }
        if (isAdminRoleName(user.getCfgTblRole().getTxtRoleName())) {
            return true;
        }
        Long roleId = Long.valueOf(user.getCfgTblRole().getSerRoleId());
        List<CfgTblSubMenuRole> permissions = menuService.getAllSubMenuRole(roleId, Long.valueOf(userId));
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        for (CfgTblSubMenuRole permission : permissions) {
            if (!isSubMenuPermissionEnabled(permission)) {
                continue;
            }
            CfgTblSubMenu subMenu = permission.getCfgTblSubMenu();
            if (subMenu == null || !matchesAllApplicationsSubMenuUrl(subMenu.getTxtSubMenuUrl())) {
                continue;
            }
            if (Boolean.TRUE.equals(permission.getBlIsview()) || Boolean.TRUE.equals(permission.getBlIsNewView())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllApplicationsSubMenuUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.trim().replaceAll("^/+", "").toLowerCase(Locale.ROOT);
        return ALL_APPLICATIONS_SUBMENU_URL.equals(normalized);
    }

    private boolean isSubMenuPermissionEnabled(CfgTblSubMenuRole permission) {
        if (permission == null || Boolean.TRUE.equals(permission.getBlIsDeleted())) {
            return false;
        }
        if (!Boolean.TRUE.equals(permission.getBlIsEnabled())) {
            return false;
        }
        return Boolean.TRUE.equals(permission.getBlIsActive()) || Boolean.TRUE.equals(permission.getBlnStatus());
    }

    private boolean isAdminRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("ADMIN");
    }

    @RequestMapping(value = "/searchVelocityReports", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> searchVelocityReports(@RequestParam(defaultValue = "0") Integer page,
                                                     @RequestParam(defaultValue = "50") Integer pageSize,
                                                     @RequestParam(required = false) String search,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(required = false) String statuses,
                                                     @RequestParam(required = false) String types,
                                                     @RequestParam(required = false) String departments,
                                                     @RequestParam(required = false) String uploadedBy,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo,
                                                     @RequestParam(required = false) String amountMin,
                                                     @RequestParam(required = false) String amountMax,
                                                     @RequestParam(defaultValue = "true") Boolean all,
                                                     HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        long started = System.currentTimeMillis();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 50 : Math.max(1, Math.min(pageSize, 200));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            String sortKey = sort == null || sort.trim().isEmpty() ? "relevance" : sort.trim().toLowerCase(Locale.ROOT);

            Set<String> statusFilter = splitCsvParam(statuses);
            Set<String> typeFilter = splitCsvParam(types);
            Set<String> departmentFilter = splitCsvParam(departments);
            Set<String> uploadedByFilter = splitCsvParam(uploadedBy);

            Double minAmount = parseDoubleParam(amountMin);
            Double maxAmount = parseDoubleParam(amountMax);
            String fromDate = normalizeDateParam(dateFrom);
            String toDate = normalizeDateParam(dateTo);

            int actorUserId = commonService.getCurrentLoggedInUser();
            boolean scopeAll = Boolean.TRUE.equals(all);

            VelocityReportQuery listQuery = buildVelocityReportQuery(
                    scopeAll, actorUserId, searchText, statusFilter, typeFilter,
                    departmentFilter, uploadedByFilter, fromDate, toDate);
            Long totalCount = countVelocityReportRows(listQuery);
            int total = totalCount == null ? 0 : totalCount.intValue();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = (List<Object[]>) createVelocityReportListQuery(listQuery, sortKey)
                    .setFirstResult(safePage * safePageSize)
                    .setMaxResults(safePageSize)
                    .getResultList();

            List<Map<String, Object>> pageItems = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = mapVelocityReportRow(row);
                if (item == null) {
                    continue;
                }
                double amount = extractReportAmount(null, String.valueOf(item.getOrDefault("text", "")));
                item.put("amount", amount);
                if (minAmount != null && amount < minAmount) {
                    continue;
                }
                if (maxAmount != null && amount > maxAmount) {
                    continue;
                }
                pageItems.add(item);
            }

            Map<String, Map<String, Integer>> facetCounts = loadVelocityReportFacetCounts(
                    scopeAll, actorUserId, searchText, statusFilter, typeFilter,
                    departmentFilter, uploadedByFilter, fromDate, toDate);

            result.put("status", "Success");
            result.put("items", pageItems);
            result.put("total", total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            result.put("facetCounts", facetCounts);
            result.put("elapsedMs", System.currentTimeMillis() - started);
            return result;
        } catch (Exception ex) {
            logger.error("Error searching velocity reports: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("items", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("message", ex.getMessage());
            result.put("elapsedMs", System.currentTimeMillis() - started);
            return result;
        }
    }

    @RequestMapping(value = "/getTemplatePendingApprovals", method = RequestMethod.GET)
    public Map<String, Object> getTemplatePendingApprovals(@RequestParam Integer userId,
                                                           @RequestParam(defaultValue = "false") Boolean all,
                                                           @RequestParam(defaultValue = "0") Integer page,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) String search,
                                                           HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase();

            List<String> pendingStatuses = java.util.Arrays.asList(
                    "PENDING",
                    "IN_PROGRESS",
                    "HOLD",
                    "OPINION_PENDING",
                    "CEO_PENDING",
                    "ASSET_PENDING",
                    "PR_PENDING",
                    "PO_PENDING",
                    "PO_VENDOR_TE_PENDING");
            boolean hasSearch = !searchText.isEmpty();

            String whereClause = "WHERE a.txtStatus IN :pendingStatuses " +
                    "AND (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                    "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) " +
                    "AND t.serFormId = a.serFormId ";
            if (hasSearch) {
                whereClause += "AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :search " +
                        "OR LOWER(COALESCE(a.txtStatus, '')) LIKE :search " +
                        "OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :search) ";
            }

            if (Boolean.TRUE.equals(all)) {
                javax.persistence.Query countQuery = entityManager.createQuery(
                        "SELECT COUNT(a.serApplicationId) " +
                                "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                                whereClause);
                countQuery.setParameter("pendingStatuses", pendingStatuses);
                if (hasSearch) {
                    countQuery.setParameter("search", "%" + searchText + "%");
                }
                Long total = (Long) countQuery.getSingleResult();

                javax.persistence.Query rowQuery = entityManager.createQuery(
                        "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, " +
                                "a.intCurrentApprovalLevel, a.serSubmittedBy, a.serCurrentApprover, " +
                                "a.dteCreatedDate, t.txtTemplateName " +
                                "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                                whereClause +
                                "ORDER BY a.dteCreatedDate DESC");
                rowQuery.setParameter("pendingStatuses", pendingStatuses);
                if (hasSearch) {
                    rowQuery.setParameter("search", "%" + searchText + "%");
                }

                @SuppressWarnings("unchecked")
                List<Object[]> rows = rowQuery
                        .setFirstResult(safePage * safePageSize)
                        .setMaxResults(safePageSize)
                        .getResultList();

                java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
                for (Object[] row : rows) {
                    Map<String, Object> item = toPendingApprovalListItem(row);
                    if (item != null) {
                        items.add(item);
                    }
                }

                result.put("items", items);
                result.put("total", total == null ? 0 : total);
                result.put("page", safePage);
                result.put("pageSize", safePageSize);
                return result;
            }

            final int targetOffset = safePage * safePageSize;
            final int batchSize = Math.max(safePageSize * 4, 50);
            final Map<Integer, String> templatePayloadCache = new HashMap<>();
            final List<Map<String, Object>> items = new ArrayList<>();
            int total = 0;
            int offset = 0;

            while (true) {
                javax.persistence.Query rowQuery = entityManager.createQuery(
                        "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, " +
                                "a.intCurrentApprovalLevel, a.serSubmittedBy, a.serCurrentApprover, " +
                                "a.dteCreatedDate, t.txtTemplateName " +
                                "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                                whereClause +
                                "ORDER BY a.dteCreatedDate DESC");
                rowQuery.setParameter("pendingStatuses", pendingStatuses);
                if (hasSearch) {
                    rowQuery.setParameter("search", "%" + searchText + "%");
                }

                @SuppressWarnings("unchecked")
                List<Object[]> rows = rowQuery
                        .setFirstResult(offset)
                        .setMaxResults(batchSize)
                        .getResultList();

                if (rows.isEmpty()) {
                    break;
                }

                for (Object[] row : rows) {
                    CfgTblCustomFormApplication application = toPendingApprovalCandidate(row);
                    if (application == null) {
                        continue;
                    }

                    boolean isMatch = isDirectPendingApprover(application, userId);
                    if (!isMatch) {
                        CfgTblCustomFormApplication fullApplication = loadApplicationWorkflowContextById(
                                application.getSerApplicationId());
                        String templatePayload = getTemplatePayloadByFormId(
                                application.getSerFormId(),
                                templatePayloadCache);
                        isMatch = isUserPendingForCurrentTemplateStep(fullApplication, userId, templatePayload);
                    }

                    if (!isMatch) {
                        continue;
                    }

                    if (total >= targetOffset && items.size() < safePageSize) {
                        Map<String, Object> item = toPendingApprovalListItem(row);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    total++;
                }

                if (rows.size() < batchSize) {
                    break;
                }
                offset += rows.size();
            }

            result.put("items", items);
            result.put("total", total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching template pending approvals: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("items", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getTemplateApprovedApplications", method = RequestMethod.GET)
    public Map<String, Object> getTemplateApprovedApplications(@RequestParam Integer userId,
                                                               @RequestParam(defaultValue = "0") Integer page,
                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                               @RequestParam(required = false) String search,
                                                               HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createQuery(
                    "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, " +
                            "a.intCurrentApprovalLevel, a.serSubmittedBy, a.dteCreatedDate, a.txtApprovalHistory, " +
                            "t.txtTemplateName " +
                            "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                            "WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                            "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) " +
                            "AND t.serFormId = a.serFormId " +
                            "ORDER BY a.dteCreatedDate DESC")
                    .getResultList();

            List<Map<String, Object>> matchedItems = new ArrayList<>();
            for (Object[] row : rows) {
                String txtFormCode = row[2] == null ? "" : String.valueOf(row[2]);
                String txtStatus = row[3] == null ? "" : String.valueOf(row[3]);
                String templateName = row[8] == null ? "" : String.valueOf(row[8]);

                if (!hasUserApprovedTemplateHistory(row[7], userId)) {
                    continue;
                }

                if (!searchText.isEmpty()) {
                    String haystack = (txtFormCode + " " + txtStatus + " " + templateName).toLowerCase(Locale.ROOT);
                    if (!haystack.contains(searchText)) {
                        continue;
                    }
                }

                Map<String, Object> item = new HashMap<>();
                item.put("serApplicationId", row[0]);
                item.put("serFormId", row[1]);
                item.put("txtFormCode", row[2]);
                item.put("txtStatus", row[3]);
                item.put("intCurrentApprovalLevel", row[4]);
                item.put("serSubmittedBy", row[5]);
                item.put("dteCreatedDate", row[6]);
                item.put("myApprovalDate", getLatestTemplateApprovalDate(row[7], userId));
                item.put("templateName", row[8]);
                matchedItems.add(item);
            }

            int total = matchedItems.size();
            int fromIndex = Math.min(safePage * safePageSize, total);
            int toIndex = Math.min(fromIndex + safePageSize, total);

            result.put("items", matchedItems.subList(fromIndex, toIndex));
            result.put("total", total);
            result.put("page", safePage);
            result.put("pageSize", safePageSize);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching template approved applications: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("items", java.util.Collections.emptyList());
            result.put("total", 0);
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private Map<Integer, String> loadTemplateNamesByFormId() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                "SELECT t.serFormId, t.txtTemplateName FROM TemplateDefinition t " +
                        "WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false)")
                .getResultList();
        Map<Integer, String> names = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                names.put(toInteger(row[0]), row[1] != null ? String.valueOf(row[1]) : "Template");
            }
        }
        return names;
    }

    private boolean containsIgnoreCase(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    @Transactional
    @RequestMapping(value = "/saveTemplateDefinition",
            method = RequestMethod.POST,
            headers = "Accept=application/json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> saveTemplateDefinition(@RequestBody Map<String, Object> requestBody,
                                                       HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer formId = toInteger(requestBody.get("serFormId"));
            if (formId == null) {
                throw new IllegalArgumentException("serFormId is required");
            }

            String payload = valueAsString(requestBody.get("txtTemplatePayload"));
            if (payload == null || payload.trim().isEmpty()) {
                throw new IllegalArgumentException("txtTemplatePayload is required");
            }

            TemplateDefinition definition = findByFormId(formId);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Integer userId = toInteger(requestBody.get("serModifiedUser"));

            if (definition == null) {
                definition = new TemplateDefinition();
                definition.setSerFormId(formId);
                definition.setDteCreatedDate(now);
                definition.setSerCreatedUser(userId);
            }

            Map<String, Object> payloadMap = parseMap(payload);
            boolean isActive = isTemplatePayloadMarkedActive(payloadMap);

            definition.setTxtTemplateName(valueAsString(requestBody.get("txtTemplateName")));
            definition.setTxtCodeConvention(valueAsString(requestBody.get("txtCodeConvention")));
            definition.setTxtTemplatePayload(payload);
            definition.setBlIsActive(isActive);
            definition.setBlIsDeleted(false);
            definition.setBlnStatus(isActive);
            definition.setDteModifiedDate(now);
            definition.setSerModifiedUser(userId);

            TemplateDefinition saved = entityManager.merge(definition);
            entityManager.flush();

            result.put("status", "Success");
            result.put("message", "Template definition saved successfully");
            result.put("templateId", saved.getSerTemplateId());
            result.put("formId", saved.getSerFormId());
            return result;
        } catch (Exception ex) {
            logger.error("Error saving template definition: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return result;
        }
    }

    private TemplateDefinition findByFormId(Integer formId) {
        List<TemplateDefinition> matches = entityManager.createQuery(
                "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId",
                TemplateDefinition.class)
                .setParameter("formId", formId)
                .setMaxResults(1)
                .getResultList();
        return matches.isEmpty() ? null : matches.get(0);
    }

    private Map<String, Object> resolveApplicationTemplatePayload(Map<String, Object> appData,
                                                                  String templatePayloadJson,
                                                                  TemplateDefinition definition) {
        Map<String, Object> templatePayload = asMap(appData.get("templatePayload"));
        if (templatePayload.isEmpty() && templatePayloadJson != null && !templatePayloadJson.trim().isEmpty()) {
            templatePayload = parseMap(templatePayloadJson);
        }
        if (templatePayload.isEmpty() && definition != null) {
            templatePayload = parseMap(definition.getTxtTemplatePayload());
        }
        return templatePayload;
    }

    private boolean hasTemplateApprovalPipeline(Map<String, Object> templatePayload) {
        if (templatePayload == null || templatePayload.isEmpty()) {
            return false;
        }
        for (Map<String, Object> rawStep : asListOfMaps(templatePayload.get("pipeline"))) {
            String type = valueAsString(rawStep.get("type"));
            if (type != null && !type.isEmpty() && !"initiator".equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private TemplateDefinition sanitizeTemplateDefinitionResponse(TemplateDefinition definition, boolean includeBackground) {
        if (definition == null) {
            return null;
        }
        TemplateDefinition copy = new TemplateDefinition();
        copy.setSerTemplateId(definition.getSerTemplateId());
        copy.setSerFormId(definition.getSerFormId());
        copy.setTxtTemplateName(definition.getTxtTemplateName());
        copy.setTxtCodeConvention(definition.getTxtCodeConvention());
        copy.setTxtTemplatePayload(sanitizeTemplatePayloadJson(definition.getTxtTemplatePayload(), includeBackground));
        copy.setBlIsActive(definition.getBlIsActive());
        copy.setBlIsDeleted(definition.getBlIsDeleted());
        copy.setBlnStatus(definition.getBlnStatus());
        copy.setDteCreatedDate(definition.getDteCreatedDate());
        copy.setDteModifiedDate(definition.getDteModifiedDate());
        copy.setSerCreatedUser(definition.getSerCreatedUser());
        copy.setSerModifiedUser(definition.getSerModifiedUser());
        return copy;
    }

    private void applyListingAccessMetadata(TemplateDefinition target, TemplateDefinition definition, Integer userId) {
        if (target == null || definition == null || userId == null || userId <= 0 || definition.getSerFormId() == null) {
            return;
        }
        ListingAccessMetadata metadata = resolveListingAccessMetadata(definition, userId);
        target.setHasOwnAccess(metadata.hasOwnAccess);
        target.setHasDelegatedAccess(metadata.hasDelegatedAccess);
        target.setDelegationOnBehalfOf(metadata.onBehalfParties);
        target.setListingAccess(metadata.listingAccess);
    }

    private void putListingAccessMetadata(Map<String, Object> summary, TemplateDefinition definition, Integer userId) {
        if (summary == null || definition == null || userId == null || userId <= 0 || definition.getSerFormId() == null) {
            return;
        }
        ListingAccessMetadata metadata = resolveListingAccessMetadata(definition, userId);
        summary.put("hasOwnAccess", metadata.hasOwnAccess);
        summary.put("hasDelegatedAccess", metadata.hasDelegatedAccess);
        summary.put("delegationOnBehalfOf", metadata.onBehalfParties);
        summary.put("listingAccess", metadata.listingAccess);
    }

    private ListingAccessMetadata resolveListingAccessMetadata(TemplateDefinition definition, Integer userId) {
        boolean hasOwnAccess = hasPersonalTemplateVisibility(definition, userId);
        boolean hasDelegatedAccess = userDelegationService.canDelegateAccessTemplateDefinition(userId, definition);
        List<Map<String, Object>> onBehalfParties = new ArrayList<>();
        if (hasDelegatedAccess) {
            for (UserDelegationOnBehalfListItemDto party : userDelegationService.getDelegationOnBehalfForTemplate(
                    userId, definition.getSerFormId())) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("serOnBehalfUserId", party.getSerOnBehalfUserId());
                row.put("onBehalfUserName", party.getOnBehalfUserName());
                onBehalfParties.add(row);
            }
        }
        String listingAccess;
        if (hasOwnAccess && hasDelegatedAccess) {
            listingAccess = "BOTH";
        } else if (hasDelegatedAccess) {
            listingAccess = "DELEGATION";
        } else if (hasOwnAccess) {
            listingAccess = "OWN";
        } else {
            listingAccess = "NONE";
        }
        return new ListingAccessMetadata(hasOwnAccess, hasDelegatedAccess, onBehalfParties, listingAccess);
    }

    private static final class ListingAccessMetadata {
        private final boolean hasOwnAccess;
        private final boolean hasDelegatedAccess;
        private final List<Map<String, Object>> onBehalfParties;
        private final String listingAccess;

        private ListingAccessMetadata(boolean hasOwnAccess,
                                      boolean hasDelegatedAccess,
                                      List<Map<String, Object>> onBehalfParties,
                                      String listingAccess) {
            this.hasOwnAccess = hasOwnAccess;
            this.hasDelegatedAccess = hasDelegatedAccess;
            this.onBehalfParties = onBehalfParties;
            this.listingAccess = listingAccess;
        }
    }

    private Map<String, Object> toTemplateDefinitionSummary(TemplateDefinition definition, Integer userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Set<Integer> visibleUserIds = extractVisibleUserIds(definition);
        boolean isActive = isTemplateDefinitionActive(definition);
        boolean isDraft = isTemplateDefinitionDraft(definition);

        summary.put("serTemplateId", definition.getSerTemplateId());
        summary.put("serFormId", definition.getSerFormId());
        summary.put("txtTemplateName", definition.getTxtTemplateName());
        summary.put("txtCodeConvention", definition.getTxtCodeConvention());
        summary.put("blIsActive", definition.getBlIsActive());
        summary.put("blIsDeleted", definition.getBlIsDeleted());
        summary.put("blnStatus", definition.getBlnStatus());
        summary.put("dteCreatedDate", definition.getDteCreatedDate());
        summary.put("dteModifiedDate", definition.getDteModifiedDate());
        summary.put("serCreatedUser", definition.getSerCreatedUser());
        summary.put("serModifiedUser", definition.getSerModifiedUser());
        summary.put("visibilityUserIds", new ArrayList<>(visibleUserIds));
        summary.put("visibleUserIds", new ArrayList<>(visibleUserIds));
        summary.put("selectedUsers", new ArrayList<>(visibleUserIds));
        summary.put("isActive", isActive);
        summary.put("isDraft", isDraft);
        putListingAccessMetadata(summary, definition, userId);
        return summary;
    }

    private boolean matchesTemplateSummarySearch(Map<String, Object> summary, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                String.valueOf(summary.get("serFormId")),
                String.valueOf(summary.get("txtTemplateName")),
                String.valueOf(summary.get("txtCodeConvention")),
                Boolean.TRUE.equals(summary.get("isDraft")) ? "draft" : "",
                Boolean.FALSE.equals(summary.get("isActive")) ? "inactive" : "active")
                .toLowerCase(Locale.ROOT);
        return haystack.contains(searchText);
    }

    private String sanitizeTemplatePayloadJson(String rawPayload) {
        return sanitizeTemplatePayloadJson(rawPayload, true);
    }

    private String sanitizeTemplatePayloadJson(String rawPayload, boolean includeBackground) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return rawPayload;
        }
        try {
            Map<String, Object> payload = parseMap(rawPayload);
            if (!includeBackground) {
                payload = slimBackgroundInTemplatePayload(payload);
            }
            return writeJson(stripAttachmentPayloads(payload, new ArrayList<>()));
        } catch (Exception ex) {
            logger.warn("Failed to sanitize template payload response: " + ex.getMessage());
            return rawPayload;
        }
    }

    private Map<String, Object> slimBackgroundInTemplatePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        Object backgroundValue = payload.get("background");
        if (!(backgroundValue instanceof Map<?, ?>)) {
            return payload;
        }
        Map<?, ?> source = (Map<?, ?>) backgroundValue;
        Map<String, Object> slim = new LinkedHashMap<>();
        copyIfPresent(source, slim, "name");
        copyIfPresent(source, slim, "type");
        copyIfPresent(source, slim, "mimeType");
        copyIfPresent(source, slim, "pageCount");
        slim.put("deferred", true);
        payload.put("background", slim);
        return payload;
    }

    private static void copyIfPresent(Map<?, ?> source, Map<String, Object> target, String key) {
        if (source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    private String sanitizeApplicationDataJson(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return rawPayload;
        }
        try {
            Map<String, Object> payload = parseMap(rawPayload);
            Map<String, Object> templatePayload = asMap(payload.get("templatePayload"));
            if (!templatePayload.isEmpty()) {
                Map<String, Object> slimTemplatePayload = new LinkedHashMap<>();
                if (templatePayload.get("pipeline") != null) {
                    slimTemplatePayload.put("pipeline", templatePayload.get("pipeline"));
                }
                if (templatePayload.get("fields") != null) {
                    slimTemplatePayload.put("fields", templatePayload.get("fields"));
                }
                if (templatePayload.get("page") != null) {
                    slimTemplatePayload.put("page", templatePayload.get("page"));
                }
                payload.put("templatePayload", slimTemplatePayload);
            }
            return writeJson(stripAttachmentPayloads(payload, new ArrayList<>()));
        } catch (Exception ex) {
            logger.warn("Failed to sanitize application data response: " + ex.getMessage());
            return rawPayload;
        }
    }

    private Object stripAttachmentPayloads(Object value, List<String> path) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> source = (Map<?, ?>) value;
            if (isBackgroundPayloadPath(path)) {
                return value;
            }
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = entry.getKey() != null ? String.valueOf(entry.getKey()) : null;
                Object child = entry.getValue();
                if (isAttachmentPayloadKey(key)) {
                    continue;
                }
                List<String> childPath = new ArrayList<>(path);
                if (key != null && !key.trim().isEmpty()) {
                    childPath.add(key.trim());
                }
                sanitized.put(key, stripAttachmentPayloads(child, childPath));
            }
            return sanitized;
        }
        if (value instanceof List<?>) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : (List<?>) value) {
                sanitized.add(stripAttachmentPayloads(item, path));
            }
            return sanitized;
        }
        return value;
    }

    private boolean isBackgroundPayloadPath(List<String> path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String lastSegment = path.get(path.size() - 1);
        return "background".equalsIgnoreCase(lastSegment);
    }

    private boolean isAttachmentPayloadKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("base64")
                || normalized.equals("dataurl")
                || normalized.equals("filebase64")
                || normalized.equals("filedata")
                || normalized.equals("content")
                || normalized.equals("data");
    }

    private List<TemplateDefinition> filterVisibleTemplateDefinitions(List<TemplateDefinition> definitions, Integer userId) {
        if (definitions == null || definitions.isEmpty()) {
            return definitions;
        }
        if (isAdminUser(userId)) {
            return definitions;
        }
        List<TemplateDefinition> visible = new ArrayList<>();
        for (TemplateDefinition definition : definitions) {
            if (canAccessTemplateDefinition(definition, userId)) {
                visible.add(definition);
            }
        }
        return visible;
    }

    private boolean canAccessTemplateDefinition(TemplateDefinition definition, Integer userId) {
        if (definition == null) {
            return false;
        }
        if (isAdminUser(userId)) {
            return true;
        }
        if (isTemplateDefinitionDraft(definition)) {
            return false;
        }
        if (!isTemplateDefinitionActive(definition)) {
            return false;
        }
        if (hasPersonalTemplateVisibility(definition, userId)) {
            return true;
        }
        return userId != null && userDelegationService.canDelegateAccessTemplateDefinition(userId, definition);
    }

    private boolean hasPersonalTemplateVisibility(TemplateDefinition definition, Integer userId) {
        Set<Integer> visibleUserIds = extractVisibleUserIds(definition);
        if (visibleUserIds.isEmpty()) {
            return true;
        }
        return userId != null && visibleUserIds.contains(userId);
    }

    private Set<Integer> extractVisibleUserIds(TemplateDefinition definition) {
        Set<Integer> userIds = new LinkedHashSet<>();
        if (definition == null || definition.getTxtTemplatePayload() == null || definition.getTxtTemplatePayload().trim().isEmpty()) {
            return userIds;
        }

        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        addVisibleUserIds(userIds, payload.get("visibleUserIds"));
        addVisibleUserIds(userIds, payload.get("visibilityUserIds"));
        addVisibleUserIds(userIds, payload.get("selectedUsers"));
        return userIds;
    }

    private void addVisibleUserIds(Set<Integer> target, Object value) {
        if (!(value instanceof List)) {
            return;
        }
        for (Object item : (List<?>) value) {
            Integer userId = toInteger(item);
            if (userId != null && userId > 0) {
                target.add(userId);
            }
        }
    }

    private boolean isTemplateDefinitionActive(TemplateDefinition definition) {
        if (definition == null) {
            return false;
        }
        if (isTemplateDefinitionDraft(definition)) {
            return false;
        }
        if (Boolean.FALSE.equals(definition.getBlIsActive()) || Boolean.FALSE.equals(definition.getBlnStatus())) {
            return false;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        return isTemplatePayloadMarkedActive(payload);
    }

    private boolean isTemplateDefinitionDraft(TemplateDefinition definition) {
        if (definition == null) {
            return false;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        if (payload == null || payload.isEmpty()) {
            return false;
        }
        Object draftFlag = payload.get("isDraft");
        return draftFlag instanceof Boolean
                ? (Boolean) draftFlag
                : draftFlag != null && "true".equalsIgnoreCase(String.valueOf(draftFlag).trim());
    }

    private boolean isTemplatePayloadMarkedActive(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return true;
        }
        Object explicitIsActive = payload.get("isActive");
        if (explicitIsActive != null) {
            return !isFalseValue(explicitIsActive);
        }
        Object explicitStatus = payload.get("blnStatus");
        if (explicitStatus != null) {
            return !isFalseValue(explicitStatus);
        }
        Object explicitActiveFlag = payload.get("blIsActive");
        if (explicitActiveFlag != null) {
            return !isFalseValue(explicitActiveFlag);
        }
        return true;
    }

    private boolean isFalseValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return !((Boolean) value);
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized);
    }

    private boolean isAdminUser(Integer userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        CfgTblUser user = entityManager.find(CfgTblUser.class, userId);
        if (user == null) {
            return false;
        }
        CfgTblRole role = user.getCfgTblRole();
        String roleName = role != null ? valueAsString(role.getTxtRoleName()) : null;
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ADMIN")
                || normalized.equals("ROLE_ADMIN")
                || normalized.equals("SUPER ADMIN")
                || normalized.equals("ROLE_SUPER ADMIN")
                || normalized.contains("ADMIN");
    }

    private Map<String, Object> handleTemplateDecision(Map<String, Object> requestBody, String action,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            ensureTemplateApplicationActionable(application);
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            TemplateWorkflowActor actor = resolveTemplateWorkflowActor(requestBody, application, context, currentStep,
                    "You are not authorized to act on this template application");
            ensureActorHasUploadedSignature(actor.performedBy);

            String approvedIp = RequestMetadataUtil.resolveClientIp(request);
            String approvedVia = resolveTemplateApprovalVia(requestBody);
            appendTemplateHistory(application, currentStep, actor.performedBy, actor.onBehalfOf, action, remarks,
                    displayLevelForIndex(safeLevel(application)), approvedIp, approvedVia);
            if ("REJECTED".equals(action)) {
                application.setTxtStatus("REJECTED");
                application.setSerCurrentApprover(null);
            } else if (isTemplateStepComplete(application, currentStep)) {
                int nextLevel = safeLevel(application) + 1;
                if (nextLevel >= context.steps.size()) {
                    application.setTxtStatus("COMPLETED");
                    application.setSerCurrentApprover(null);
                } else {
                    TemplateStep nextStep = context.stepAt(nextLevel);
                    application.setTxtStatus("IN_PROGRESS");
                    application.setIntCurrentApprovalLevel(displayLevelForIndex(nextLevel));
                    application.setSerCurrentApprover(firstApproverId(nextStep));
                }
            } else {
                application.setTxtStatus("IN_PROGRESS");
                application.setSerCurrentApprover(nextPendingApproverId(application, currentStep));
            }

            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actor.performedBy);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("REJECTED".equals(action) ? "REJECT" : "APPROVE", request,
                    application.getSerApplicationId(), "SUCCESS",
                    "REJECTED".equals(action) ? "Template application rejected successfully"
                            : "Template application approved successfully",
                    buildTemplateApplicationPayload(application, action, remarks), null);
            if ("REJECTED".equals(action)) {
                queueTemplatePostApprovalEmails(application.getSerApplicationId(),
                        "template rejection notification", true);
                templateWorkflowNotificationService.notifyRejected(application, actor.performedBy, remarks);
            } else {
                templateWorkflowNotificationService.notifyApproved(application, actor.performedBy, remarks);
                if ("APPROVED".equals(action)) {
                    if (isEmailTemplateWorkflowAction(requestBody)) {
                        templateWorkflowCommandService.mergeAndQueuePostApprovalEmails(application,
                                "template email approval notification", true);
                    } else if (!isPortalDeferredEmailWorkflow(requestBody)) {
                        templateWorkflowCommandService.mergeAndQueuePostApprovalEmails(application,
                                "template approval notification", true);
                    }
                    // Portal approval (deferEmail): PDF + queue via updateApplicationPdf?queuePostApprovalEmails=true
                }
            }
            return success("REJECTED".equals(action) ? "Application rejected successfully" : "Application approved successfully");
        } catch (Exception ex) {
            logTemplateAction("REJECTED".equals(action) ? "REJECT" : "APPROVE", request,
                    toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, action), ex.getMessage());
            return failure(result, response, "Error completing template action", ex);
        }
    }

    private void logTemplateAction(String actionType, HttpServletRequest request, Integer applicationId,
                                   String status, String message, Map<String, Object> payload, String errorMessage) {
        try {
            int userId = commonService.getCurrentLoggedInUser();
            String username = null;
            if (userId > 0) {
                CfgTblUser user = commonService.getCurrentUser(userId);
                if (user != null) {
                    username = user.getTxtUserName();
                }
            }
            activityLogService.logActivity(actionType, userId > 0 ? userId : null, username,
                    RequestMetadataUtil.resolveClientIp(request),
                    RequestMetadataUtil.resolveDevice(request),
                    status, message, "APPLICATION", applicationId, payload, errorMessage);
        } catch (Exception ex) {
            logger.warn("Failed to log template action: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildTemplateApplicationPayload(CfgTblCustomFormApplication application,
                                                                String action,
                                                                String remarks) {
        Map<String, Object> payload = new HashMap<>();
        if (application == null) {
            return payload;
        }
        payload.put("applicationId", application.getSerApplicationId());
        payload.put("formId", application.getSerFormId());
        payload.put("formCode", application.getTxtFormCode());
        payload.put("status", application.getTxtStatus());
        payload.put("approvalLevel", application.getIntCurrentApprovalLevel());
        payload.put("action", action);
        if (remarks != null) {
            payload.put("remarks", remarks);
        }
        return payload;
    }

    private Map<String, Object> buildTemplateRequestPayload(Map<String, Object> requestBody, String action) {
        Map<String, Object> payload = new HashMap<>();
        if (requestBody != null) {
            payload.put("applicationId", toInteger(requestBody.get("applicationId")));
            payload.put("userId", toInteger(requestBody.get("userId")));
            payload.put("remarks", valueAsString(requestBody.get("remarks")));
            payload.put("opinionUserId", toInteger(requestBody.get("opinionUserId")));
        }
        payload.put("action", action);
        return payload;
    }

    private void queueTemplatePostApprovalEmails(Integer applicationId, String contextLabel) {
        queueTemplatePostApprovalEmails(applicationId, contextLabel, false);
    }

    private void queueTemplatePostApprovalEmails(Integer applicationId, String contextLabel, boolean refreshPdfBeforeEmail) {
        if (applicationId == null) {
            return;
        }
        templateEmailDispatchService.queuePostApprovalEmails(applicationId, contextLabel, refreshPdfBeforeEmail);
    }

    private boolean hasUserApprovedTemplateLevel(Integer applicationId, Integer userId, Integer level) {
        if (applicationId == null || userId == null) {
            return false;
        }
        CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
        if (application == null) {
            return false;
        }
        int displayLevel = level == null || level <= 0 ? displayLevelForIndex(0) : level;
        for (Map<String, Object> entry : activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))) {
            String action = valueAsString(entry.get("action"));
            Integer approvedBy = toInteger(firstObject(entry.get("approvedBy"), entry.get("userId")));
            Integer entryLevel = toInteger(entry.get("level"));
            if ("APPROVED".equalsIgnoreCase(action)
                    && userId.equals(approvedBy)
                    && entryLevel != null
                    && entryLevel.equals(displayLevel)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserPendingForCurrentTemplateStep(Integer applicationId, Integer userId) {
        if (applicationId == null || userId == null || userId <= 0) {
            return false;
        }
        CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
        return isUserPendingForCurrentTemplateStep(application, userId);
    }

    private boolean isUserPendingForCurrentTemplateStep(CfgTblCustomFormApplication application, Integer userId) {
        if (application == null) {
            return false;
        }

        Integer currentApprover = application.getSerCurrentApprover();
        if (currentApprover != null && currentApprover.equals(userId)) {
            return true;
        }

        return pendingApproverIdsForCurrentStep(application).contains(userId);
    }

    private boolean isUserPendingForCurrentTemplateStep(CfgTblCustomFormApplication application,
                                                        Integer userId,
                                                        String templatePayloadJson) {
        if (application == null) {
            return false;
        }

        Integer currentApprover = application.getSerCurrentApprover();
        if (currentApprover != null && currentApprover.equals(userId)) {
            return true;
        }

        return pendingApproverIdsForCurrentStep(application, templatePayloadJson).contains(userId);
    }

    private Set<Integer> pendingApproverIdsForCurrentStep(CfgTblCustomFormApplication application) {
        Set<Integer> pendingIds = new LinkedHashSet<>();
        if (application == null) {
            return pendingIds;
        }

        String status = valueAsString(application.getTxtStatus()).toUpperCase(Locale.ROOT);
        if ("REJECTED".equals(status) || "APPROVED".equals(status) || "COMPLETED".equals(status)) {
            return pendingIds;
        }

        if ("OPINION_PENDING".equals(status)) {
            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> opinionRequest = asMap(appData.get("templateOpinionRequest"));
            if (Boolean.TRUE.equals(opinionRequest.get("active"))
                    || "true".equalsIgnoreCase(valueAsString(opinionRequest.get("active")))) {
                addIfPresent(pendingIds, toInteger(opinionRequest.get("requestedFrom")));
                if (!pendingIds.isEmpty()) {
                    return pendingIds;
                }
            }
        }

        Integer storedLevel = application.getIntCurrentApprovalLevel();
        if (storedLevel != null && storedLevel <= 1) {
            addIfPresent(pendingIds, application.getSerCurrentApprover() != null
                    ? application.getSerCurrentApprover()
                    : application.getSerSubmittedBy());
            return pendingIds;
        }

        TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
        TemplateStep currentStep = context.stepAt(safeLevel(application));
        if (currentStep == null || currentStep.approverIds.isEmpty()) {
            return pendingIds;
        }

        Set<Integer> approvedIds = approvedIdsForStep(application, currentStep);
        if ("AND".equalsIgnoreCase(currentStep.approvalMode)) {
            currentStep.approverIds.stream()
                    .filter((approverId) -> !approvedIds.contains(approverId))
                    .forEach(pendingIds::add);
            return pendingIds;
        }

        if (approvedIds.isEmpty()) {
            pendingIds.addAll(currentStep.approverIds);
        }
        return pendingIds;
    }

    private Set<Integer> pendingApproverIdsForCurrentStep(CfgTblCustomFormApplication application, String templatePayloadJson) {
        Set<Integer> pendingIds = new LinkedHashSet<>();
        if (application == null) {
            return pendingIds;
        }

        String status = valueAsString(application.getTxtStatus()).toUpperCase(Locale.ROOT);
        if ("REJECTED".equals(status) || "APPROVED".equals(status) || "COMPLETED".equals(status)) {
            return pendingIds;
        }

        if ("OPINION_PENDING".equals(status)) {
            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> opinionRequest = asMap(appData.get("templateOpinionRequest"));
            if (Boolean.TRUE.equals(opinionRequest.get("active"))
                    || "true".equalsIgnoreCase(valueAsString(opinionRequest.get("active")))) {
                addIfPresent(pendingIds, toInteger(opinionRequest.get("requestedFrom")));
                if (!pendingIds.isEmpty()) {
                    return pendingIds;
                }
            }
        }

        Integer storedLevel = application.getIntCurrentApprovalLevel();
        if (storedLevel != null && storedLevel <= 1) {
            addIfPresent(pendingIds, application.getSerCurrentApprover() != null
                    ? application.getSerCurrentApprover()
                    : application.getSerSubmittedBy());
            return pendingIds;
        }

        TemplateWorkflowContext context = buildTemplateWorkflowContext(application, templatePayloadJson);
        TemplateStep currentStep = context.stepAt(safeLevel(application));
        if (currentStep == null || currentStep.approverIds.isEmpty()) {
            return pendingIds;
        }

        Set<Integer> approvedIds = approvedIdsForStep(application, currentStep);
        if ("AND".equalsIgnoreCase(currentStep.approvalMode)) {
            currentStep.approverIds.stream()
                    .filter((approverId) -> !approvedIds.contains(approverId))
                    .forEach(pendingIds::add);
            return pendingIds;
        }

        if (approvedIds.isEmpty()) {
            pendingIds.addAll(currentStep.approverIds);
        }
        return pendingIds;
    }

    private boolean hasUserApprovedTemplateHistory(Object rawHistory, Integer userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        for (Map<String, Object> entry : activeTemplateHistory(parseHistory(valueAsString(rawHistory)))) {
            Integer approvedBy = toInteger(firstObject(entry.get("approvedBy"), entry.get("approverUserId"), entry.get("userId")));
            String action = valueAsString(firstObject(entry.get("action"), entry.get("status")));
            if (approvedBy != null
                    && approvedBy.equals(userId)
                    && "APPROVED".equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    private Object getLatestTemplateApprovalDate(Object rawHistory, Integer userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        Object approvedDate = null;
        for (Map<String, Object> entry : activeTemplateHistory(parseHistory(valueAsString(rawHistory)))) {
            Integer approvedBy = toInteger(firstObject(entry.get("approvedBy"), entry.get("approverUserId"), entry.get("userId")));
            String action = valueAsString(firstObject(entry.get("action"), entry.get("status")));
            if (approvedBy != null
                    && approvedBy.equals(userId)
                    && "APPROVED".equalsIgnoreCase(action)) {
                approvedDate = firstObject(
                        entry.get("approvedDate"),
                        entry.get("approvedAt"),
                        entry.get("date"),
                        entry.get("timestamp"),
                        entry.get("dteCreatedDate"));
            }
        }
        return approvedDate;
    }

    private boolean isDirectPendingApprover(CfgTblCustomFormApplication application, Integer userId) {
        if (application == null || userId == null || userId <= 0) {
            return false;
        }
        Integer currentApprover = application.getSerCurrentApprover();
        return currentApprover != null && currentApprover.equals(userId);
    }

    private String getTemplatePayloadByFormId(Integer formId, Map<Integer, String> cache) {
        if (formId == null) {
            return null;
        }
        if (cache.containsKey(formId)) {
            return cache.get(formId);
        }

        List<String> payloads = entityManager.createQuery(
                        "SELECT t.txtTemplatePayload FROM TemplateDefinition t WHERE t.serFormId = :formId",
                        String.class)
                .setParameter("formId", formId)
                .setMaxResults(1)
                .getResultList();
        String payload = payloads.isEmpty() ? null : payloads.get(0);
        cache.put(formId, payload);
        return payload;
    }

    private CfgTblCustomFormApplication loadTemplateApplication(Integer applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID is required");
        }
        CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
        if (application == null || application.getSerFormId() == null) {
            throw new IllegalArgumentException("Template application not found");
        }
        TemplateDefinition template = findByFormId(application.getSerFormId());
        if (template == null) {
            throw new IllegalArgumentException("Template definition not found for this application");
        }
        return application;
    }

    private void ensureTemplateApplicationActionable(CfgTblCustomFormApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Template application not found");
        }
        String status = valueAsString(application.getTxtStatus()).trim().toUpperCase(Locale.ROOT);
        if ("REJECTED".equals(status)) {
            throw new IllegalStateException("Application is already rejected. No further actions are allowed.");
        }
        if ("APPROVED".equals(status) || "COMPLETED".equals(status)) {
            throw new IllegalStateException("Application is already completed. No further actions are allowed.");
        }
        if ("HOLD".equals(status)) {
            throw new IllegalStateException(
                    "Application is on Hold. Unhold it before approve, reject, send back, or other workflow actions.");
        }
    }

    private TemplateWorkflowContext buildTemplateWorkflowContext(CfgTblCustomFormApplication application) {
        return buildTemplateWorkflowContext(application, null);
    }

    private TemplateWorkflowContext buildTemplateWorkflowContext(CfgTblCustomFormApplication application,
                                                                String templatePayloadJson) {
        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        TemplateDefinition definition = findByFormId(application.getSerFormId());
        Map<String, Object> templatePayload = resolveApplicationTemplatePayload(appData, templatePayloadJson, definition);
        List<TemplateStep> footerSteps = buildFooterWorkflowSteps(appData, application);
        if (!footerSteps.isEmpty() && !hasTemplateApprovalPipeline(templatePayload)) {
            TemplateWorkflowContext footerContext = new TemplateWorkflowContext();
            for (TemplateStep step : footerSteps) {
                step.level = footerContext.steps.size();
                footerContext.steps.add(step);
            }
            footerContext.initiator = new TemplateStep();
            footerContext.initiator.id = "initiator";
            footerContext.initiator.name = "Initiator";
            footerContext.initiator.type = "initiator";
            if (application.getSerSubmittedBy() != null) {
                footerContext.initiator.approverIds.add(application.getSerSubmittedBy());
            }
            return footerContext;
        }
        List<Map<String, Object>> rawSteps = asListOfMaps(templatePayload.get("pipeline"));
        TemplateWorkflowContext context = new TemplateWorkflowContext();
        for (Map<String, Object> rawStep : rawSteps) {
            TemplateStep step = buildTemplateStep(rawStep, application);
            if ("initiator".equalsIgnoreCase(step.type)) {
                context.initiator = step;
            } else if (!step.approverIds.isEmpty()) {
                step.level = context.steps.size();
                context.steps.add(step);
            }
        }
        if (context.initiator == null) {
            context.initiator = new TemplateStep();
            context.initiator.id = "initiator";
            context.initiator.name = "Initiator";
            context.initiator.type = "initiator";
            if (application.getSerSubmittedBy() != null) {
                context.initiator.approverIds.add(application.getSerSubmittedBy());
            }
        }
        return context;
    }

    private CfgTblCustomFormApplication toPendingApprovalCandidate(Object[] row) {
        if (row == null || row.length < 8) {
            return null;
        }
        CfgTblCustomFormApplication application = new CfgTblCustomFormApplication();
        application.setSerApplicationId(toInteger(row[0]));
        application.setSerFormId(toInteger(row[1]));
        application.setTxtFormCode(row[2] != null ? String.valueOf(row[2]) : null);
        application.setTxtStatus(row[3] != null ? String.valueOf(row[3]) : null);
        application.setIntCurrentApprovalLevel(toInteger(row[4]));
        application.setSerSubmittedBy(toInteger(row[5]));
        application.setSerCurrentApprover(toInteger(row[6]));
        application.setDteCreatedDate(row[7] instanceof Timestamp ? (Timestamp) row[7] : null);
        return application;
    }

    private Map<String, Object> toPendingApprovalListItem(Object[] row) {
        CfgTblCustomFormApplication application = toPendingApprovalCandidate(row);
        if (application == null) {
            return null;
        }

        Map<String, Object> item = new HashMap<>();
        item.put("serApplicationId", application.getSerApplicationId());
        item.put("serFormId", application.getSerFormId());
        item.put("txtFormCode", application.getTxtFormCode());
        item.put("txtStatus", application.getTxtStatus());
        item.put("intCurrentApprovalLevel", application.getIntCurrentApprovalLevel());
        item.put("serSubmittedBy", application.getSerSubmittedBy());
        item.put("dteCreatedDate", application.getDteCreatedDate());
        item.put("templateName", row.length > 8 ? row[8] : null);
        String applicationJson = getApplicationJsonById(application.getSerApplicationId());
        item.put("applicationTitle", resolveTemplateApplicationTitle(applicationJson));
        return item;
    }

    private String getApplicationJsonById(Integer applicationId) {
        if (applicationId == null) {
            return null;
        }
        List<String> results = entityManager.createQuery(
                        "SELECT a.txtApplicationData FROM CfgTblCustomFormApplication a WHERE a.serApplicationId = :id",
                        String.class)
                .setParameter("id", applicationId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private CfgTblCustomFormApplication loadApplicationWorkflowContextById(Integer applicationId) {
        if (applicationId == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT a.serApplicationId, a.serFormId, a.txtApplicationData, a.txtStatus, " +
                                "a.intCurrentApprovalLevel, a.serSubmittedBy, a.serCurrentApprover, a.txtApprovalHistory " +
                                "FROM CfgTblCustomFormApplication a WHERE a.serApplicationId = :id")
                .setParameter("id", applicationId)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        CfgTblCustomFormApplication application = new CfgTblCustomFormApplication();
        application.setSerApplicationId(toInteger(row[0]));
        application.setSerFormId(toInteger(row[1]));
        application.setTxtApplicationData(row[2] != null ? String.valueOf(row[2]) : null);
        application.setTxtStatus(row[3] != null ? String.valueOf(row[3]) : null);
        application.setIntCurrentApprovalLevel(toInteger(row[4]));
        application.setSerSubmittedBy(toInteger(row[5]));
        application.setSerCurrentApprover(toInteger(row[6]));
        application.setTxtApprovalHistory(row[7] != null ? String.valueOf(row[7]) : null);
        return application;
    }

    private String resolveTemplateApplicationTitle(String txtApplicationData) {
        Map<String, Object> data = parseMap(txtApplicationData);
        Map<String, Object> titleField = findTemplateTitleFieldDefinition(data);
        if (titleField != null) {
            for (Object candidate : templateTitleValueCandidates(titleField, data)) {
                String normalized = normalizeTemplateTitleDisplayValue(candidate);
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        for (Object candidate : directTemplateTitleCandidates(data)) {
            String normalized = normalizeTemplateTitleDisplayValue(candidate);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    private Map<String, Object> findTemplateTitleFieldDefinition(Map<String, Object> data) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.addAll(asListOfMaps(asMap(data.get("templatePayload")).get("fields")));
        fields.addAll(asListOfMaps(data.get("fields")));
        for (Map<String, Object> field : fields) {
            if (field == null || field.isEmpty()) {
                continue;
            }
            String label = templateFieldLabel(field);
            if ("title".equals(normalizeTemplateFieldLabel(label))) {
                return field;
            }
        }
        return null;
    }

    private List<Object> templateTitleValueCandidates(Map<String, Object> field, Map<String, Object> data) {
        List<Object> candidates = new ArrayList<>();
        String label = templateFieldLabel(field);
        String id = firstText(field.get("id"), field.get("fieldId"));
        String slug = slugifyTemplateFieldLabel(label);
        Map<String, Object> templateValues = asMap(data.get("templateValues"));
        if (!id.isEmpty()) {
            candidates.add(templateValues.get(id));
            candidates.add(data.get(id));
        }
        if (!label.isEmpty()) {
            candidates.add(data.get(label));
            candidates.add(templateValues.get(label));
        }
        if (!slug.isEmpty()) {
            candidates.add(data.get(slug));
            candidates.add(templateValues.get(slug));
        }
        return candidates;
    }

    private List<Object> directTemplateTitleCandidates(Map<String, Object> data) {
        Map<String, Object> templateValues = asMap(data.get("templateValues"));
        List<Object> candidates = new ArrayList<>();
        candidates.add(data.get("Title"));
        candidates.add(data.get("title"));
        candidates.add(templateValues.get("Title"));
        candidates.add(templateValues.get("title"));
        return candidates;
    }

    private String templateFieldLabel(Map<String, Object> field) {
        return firstText(field.get("label"), field.get("txtFieldLabel"), field.get("name"), field.get("key"));
    }

    private String normalizeTemplateFieldLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String slugifyTemplateFieldLabel(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizeTemplateTitleDisplayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map || value instanceof List) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String getApplicationDataById(Integer applicationId) {
        if (applicationId == null) {
            return null;
        }
        return getApplicationJsonById(applicationId);
    }

    private List<TemplateStep> buildFooterWorkflowSteps(Map<String, Object> appData,
                                                        CfgTblCustomFormApplication application) {
        List<TemplateStep> steps = new ArrayList<>();
        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        for (Map<String, Object> field : footerFields) {
            String label = firstText(field.get("label"), field.get("key"), "Approver");
            String key = firstText(field.get("key"), "footer_" + (steps.size() + 1));
            if ("prepared_by".equalsIgnoreCase(key)) {
                continue;
            }
            TemplateStep step = new TemplateStep();
            step.id = key;
            step.name = label;
            step.type = "individual";
            step.approvalMode = "AND".equalsIgnoreCase(firstText(field.get("approvalMode"), field.get("condition")))
                    ? "AND"
                    : "OR";
            for (Map<String, Object> user : asListOfMaps(field.get("users"))) {
                Integer approverId = toInteger(firstObject(user.get("serUserId"), user.get("userId"), user.get("id")));
                if (approverId == null) {
                    continue;
                }
                addIfPresent(step.approverIds, approverId);
            }
            if (!step.approverIds.isEmpty()) {
                steps.add(step);
            }
        }
        return steps;
    }

    private List<Map<String, Object>> extractFooterFields(Map<String, Object> appData) {
        if (appData == null || appData.isEmpty()) {
            return new ArrayList<>();
        }
        Object obj = appData.get("footerFields");
        if (!(obj instanceof List)) {
            obj = appData.get("individual_pipeline_footer");
        }
        if (!(obj instanceof List)) {
            obj = appData.get("field_footer");
        }
        if (!(obj instanceof List)) {
            obj = appData.get("dynamicFooter");
        }
        if (!(obj instanceof List)) {
            obj = appData.get("footer");
        }
        List<Map<String, Object>> fields = asListOfMaps(obj);
        fields.sort((left, right) -> Integer.compare(
                toInteger(left != null ? left.get("order") : null) != null ? toInteger(left.get("order")) : Integer.MAX_VALUE,
                toInteger(right != null ? right.get("order") : null) != null ? toInteger(right.get("order")) : Integer.MAX_VALUE));
        return fields;
    }

    private TemplateStep buildTemplateStep(Map<String, Object> rawStep, CfgTblCustomFormApplication application) {
        TemplateStep step = new TemplateStep();
        step.id = valueAsString(rawStep.get("id"));
        step.name = firstText(rawStep.get("name"), rawStep.get("stageName"), rawStep.get("departmentName"), rawStep.get("txtDepartmentName"), "Step");
        step.type = firstText(rawStep.get("type"), "department");
        step.approvalMode = "AND".equalsIgnoreCase(valueAsString(rawStep.get("approvalMode"))) ? "AND" : "OR";

        if ("individual".equalsIgnoreCase(step.type)) {
            for (Map<String, Object> user : asListOfMaps(rawStep.get("users"))) {
                addIfPresent(step.approverIds, toInteger(firstObject(user.get("serUserId"), user.get("userId"), user.get("id"))));
            }
            for (Object userIdValue : asList(rawStep.get("userIds"))) {
                addIfPresent(step.approverIds, toInteger(userIdValue));
            }
            addIfPresent(step.approverIds, toInteger(firstObject(rawStep.get("serUserId"), rawStep.get("userId"))));
            if ("initiator".equalsIgnoreCase(valueAsString(rawStep.get("dynamicTarget")))
                    || asList(rawStep.get("dynamicTargets")).contains("initiator")) {
                addIfPresent(step.approverIds, application.getSerSubmittedBy());
            }
        } else if ("role".equalsIgnoreCase(step.type)) {
            Integer roleId = toInteger(firstObject(rawStep.get("serRoleId"), nested(rawStep.get("cfgTblRole"), "serRoleId")));
            if (roleId != null) {
                for (CfgTblUser user : entityManager.createQuery(
                        "SELECT u FROM CfgTblUser u WHERE u.cfgTblRole.serRoleId = :roleId AND (u.blIsDeleted IS NULL OR u.blIsDeleted = false)",
                        CfgTblUser.class).setParameter("roleId", roleId).getResultList()) {
                    addIfPresent(step.approverIds, user.getSerUserId());
                }
            }
        } else if ("initiator".equalsIgnoreCase(step.type)) {
            addIfPresent(step.approverIds, application.getSerSubmittedBy());
        } else {
            Integer departmentId = toInteger(firstObject(rawStep.get("serDepartmentId"), rawStep.get("departmentId"), nested(rawStep.get("hrTblDepartment"), "serDepartmentId")));
            if ("initiator_hod".equalsIgnoreCase(valueAsString(rawStep.get("dynamicTarget")))) {
                departmentId = getUserDepartmentId(application.getSerSubmittedBy());
            }
            for (Integer hodId : getDepartmentHeadIds(departmentId)) {
                addIfPresent(step.approverIds, hodId);
            }
        }
        return step;
    }

    private boolean isAuthorizedForTemplateStep(TemplateStep step, Integer actorId) {
        return step != null && actorId != null && step.approverIds.contains(actorId);
    }

    public String resolveWorkflowUserNotice(Integer applicationId, Integer userId) {
        if (applicationId == null || userId == null || userId <= 0) {
            return null;
        }
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(applicationId);
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            String orMessage = resolveOrAlreadyApprovedMessage(application, context, userId);
            if (orMessage != null) {
                return orMessage;
            }
            if (hasUserApprovedTemplateHistory(application.getTxtApprovalHistory(), userId)) {
                return "Already approved by you.";
            }
            return null;
        } catch (Exception ex) {
            logger.warn("Failed to resolve template workflow user notice for applicationId=" + applicationId
                    + ", userId=" + userId + ": " + ex.getMessage());
            return null;
        }
    }

    private void assertActorAuthorizedForTemplateStep(CfgTblCustomFormApplication application,
            TemplateWorkflowContext context,
            TemplateStep currentStep,
            Integer actorId,
            String defaultMessage) {
        if (isAuthorizedForTemplateStep(currentStep, actorId)) {
            return;
        }
        String orMessage = resolveOrAlreadyApprovedMessage(application, context, actorId);
        if (orMessage != null) {
            throw new IllegalArgumentException(orMessage);
        }
        throw new IllegalArgumentException(defaultMessage);
    }

    private static final class TemplateWorkflowActor {
        private final int performedBy;
        private final Integer onBehalfOf;

        private TemplateWorkflowActor(int performedBy, Integer onBehalfOf) {
            this.performedBy = performedBy;
            this.onBehalfOf = onBehalfOf;
        }
    }

    private TemplateWorkflowActor resolveTemplateWorkflowActor(Map<String, Object> requestBody,
            CfgTblCustomFormApplication application,
            TemplateWorkflowContext context,
            TemplateStep currentStep,
            String defaultMessage) {
        Integer performedBy = toInteger(requestBody.get("userId"));
        if (performedBy == null || performedBy <= 0) {
            performedBy = commonService.getCurrentLoggedInUser();
        }
        if (performedBy == null || performedBy <= 0) {
            throw new IllegalArgumentException("You must be logged in to perform this action");
        }

        Integer onBehalfOf = toInteger(requestBody.get("onBehalfOfUserId"));
        if (onBehalfOf != null && (onBehalfOf <= 0 || onBehalfOf.equals(performedBy))) {
            onBehalfOf = null;
        }

        if (isAuthorizedForTemplateStep(currentStep, performedBy)) {
            return new TemplateWorkflowActor(performedBy, null);
        }

        if (onBehalfOf != null && isAdminUser(performedBy)) {
            if (!isAuthorizedForTemplateStep(currentStep, onBehalfOf)
                    && !pendingApproverIdsForCurrentStep(application).contains(onBehalfOf)) {
                throw new IllegalArgumentException("Selected approver is not assigned to the current workflow step");
            }
            String orMessage = resolveOrAlreadyApprovedMessage(application, context, onBehalfOf);
            if (orMessage != null) {
                throw new IllegalArgumentException(orMessage);
            }
            return new TemplateWorkflowActor(performedBy, onBehalfOf);
        }

        assertActorAuthorizedForTemplateStep(application, context, currentStep, performedBy, defaultMessage);
        return new TemplateWorkflowActor(performedBy, null);
    }

    private String resolveOrAlreadyApprovedMessage(CfgTblCustomFormApplication application,
            TemplateWorkflowContext context,
            Integer actorId) {
        if (application == null || context == null || actorId == null || actorId <= 0) {
            return null;
        }
        int currentIdx = safeLevel(application);

        for (int idx = 0; idx < context.steps.size(); idx++) {
            TemplateStep step = context.stepAt(idx);
            if (step == null || !"OR".equalsIgnoreCase(step.approvalMode) || step.approverIds.isEmpty()) {
                continue;
            }
            if (!step.approverIds.contains(actorId)) {
                continue;
            }
            Set<Integer> approvedIds = approvedIdsForStep(application, step);
            if (approvedIds.isEmpty()) {
                continue;
            }
            boolean orStepClosed = idx < currentIdx
                    || (idx == currentIdx && !pendingApproverIdsForCurrentStep(application).contains(actorId));
            if (!orStepClosed) {
                continue;
            }
            if (approvedIds.contains(actorId)) {
                return "Already approved by you.";
            }
            String approverName = resolveOrApproverDisplayName(application, step, approvedIds, actorId);
            return "Already approved by " + approverName + ".";
        }
        return null;
    }

    private String resolveOrApproverDisplayName(CfgTblCustomFormApplication application,
            TemplateStep step,
            Set<Integer> approvedIds,
            Integer excludeActorId) {
        List<Map<String, Object>> history = activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()));
        for (int index = history.size() - 1; index >= 0; index--) {
            Map<String, Object> entry = history.get(index);
            if (!"APPROVED".equalsIgnoreCase(valueAsString(entry.get("action")))) {
                continue;
            }
            Integer userId = toInteger(firstObject(entry.get("approvedBy"), entry.get("userId")));
            if (userId == null || !approvedIds.contains(userId)) {
                continue;
            }
            if (excludeActorId != null && excludeActorId.equals(userId)) {
                continue;
            }
            if (!historyEntryMatchesTemplateStep(entry, step)) {
                continue;
            }
            return resolveUserDisplayName(userId, entry);
        }

        for (Integer approvedUserId : approvedIds) {
            if (approvedUserId == null || (excludeActorId != null && excludeActorId.equals(approvedUserId))) {
                continue;
            }
            return resolveUserDisplayName(approvedUserId, null);
        }
        return "another user";
    }

    private boolean historyEntryMatchesTemplateStep(Map<String, Object> entry, TemplateStep step) {
        if (entry == null || step == null) {
            return false;
        }
        Integer level = toInteger(firstObject(entry.get("intApprovalOrder"), entry.get("level")));
        String entryStepId = firstText(entry.get("stepId"), entry.get("pipelineStepId"), entry.get("signatureTargetId"));
        int displayLevel = step.level + 2;
        boolean hasConcreteStepId = step.id != null && !step.id.isBlank();
        boolean hasConcreteEntryStepId = entryStepId != null && !entryStepId.isBlank();
        if (hasConcreteStepId && hasConcreteEntryStepId) {
            return step.id.equals(entryStepId);
        }
        return level != null && (level == displayLevel || level == step.level + 1 || level == step.level);
    }

    private String resolveUserDisplayName(Integer userId, Map<String, Object> historyEntry) {
        if (historyEntry != null) {
            String historyName = firstText(historyEntry.get("approverName"),
                    historyEntry.get("txtUserName"),
                    historyEntry.get("userName"));
            if (historyName != null && !historyName.isBlank()) {
                return historyName;
            }
        }
        if (userId != null && userId > 0) {
            CfgTblUser user = entityManager.find(CfgTblUser.class, userId);
            if (user != null && user.getTxtUserName() != null && !user.getTxtUserName().isBlank()) {
                return user.getTxtUserName();
            }
        }
        return "another user";
    }

    private static final String SIGNATURE_REQUIRED_MESSAGE =
            "Before approving the document, kindly upload your signature.";

    private void ensureActorHasUploadedSignature(Integer actorId) {
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("User not authenticated");
        }
        CfgTblUser user = entityManager.find(CfgTblUser.class, actorId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        String signaturePath = user.getTxtSignaturePath();
        if (!com.bezkoder.spring.login.admin.utility.common.SignatureStorageUtil.hasStoredSignatureFile(signaturePath)) {
            throw new IllegalArgumentException(SIGNATURE_REQUIRED_MESSAGE);
        }
    }

    private boolean isTemplateStepComplete(CfgTblCustomFormApplication application, TemplateStep step) {
        if (step == null || step.approverIds.isEmpty()) {
            return true;
        }
        if (!"AND".equalsIgnoreCase(step.approvalMode)) {
            return true;
        }
        Set<Integer> approvedIds = approvedIdsForStep(application, step);
        return approvedIds.containsAll(step.approverIds);
    }

    private Integer nextPendingApproverId(CfgTblCustomFormApplication application, TemplateStep step) {
        Set<Integer> approvedIds = approvedIdsForStep(application, step);
        for (Integer approverId : step.approverIds) {
            if (!approvedIds.contains(approverId)) {
                return approverId;
            }
        }
        return firstApproverId(step);
    }

    private Integer firstApproverId(TemplateStep step) {
        return step == null || step.approverIds.isEmpty() ? null : step.approverIds.iterator().next();
    }

    private Set<Integer> approvedIdsForStep(CfgTblCustomFormApplication application, TemplateStep step) {
        Set<Integer> ids = new HashSet<>();
        for (Map<String, Object> entry : activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))) {
            if (!"APPROVED".equalsIgnoreCase(valueAsString(entry.get("action")))) {
                continue;
            }
            Integer level = toInteger(firstObject(entry.get("intApprovalOrder"), entry.get("level")));
            Integer userId = toInteger(firstObject(entry.get("approvedBy"), entry.get("userId")));
            String entryStepId = firstText(entry.get("stepId"), entry.get("pipelineStepId"), entry.get("signatureTargetId"));
            int displayLevel = step.level + 2;
            boolean hasConcreteStepId = step.id != null && !step.id.isBlank();
            boolean hasConcreteEntryStepId = entryStepId != null && !entryStepId.isBlank();
            boolean stepMatches = hasConcreteStepId && hasConcreteEntryStepId
                    ? step.id.equals(entryStepId)
                    : level != null && (level == displayLevel || level == step.level + 1 || level == step.level);
            if (stepMatches && userId != null) {
                ids.add(userId);
            }
        }
        return ids;
    }

    private String resolveTemplateApprovalVia(Map<String, Object> requestBody) {
        if (requestBody == null) {
            return "SYSTEM";
        }
        String approvedVia = valueAsString(requestBody.get("approvedVia"));
        if (approvedVia != null && !approvedVia.trim().isEmpty()) {
            return approvedVia.trim();
        }
        boolean deferEmail = Boolean.TRUE.equals(requestBody.get("deferEmail"))
                || "true".equalsIgnoreCase(String.valueOf(requestBody.get("deferEmail")));
        return deferEmail ? "TEMPLATE_PORTAL_DEFER_EMAIL" : "SYSTEM";
    }

    private boolean isEmailTemplateWorkflowAction(Map<String, Object> requestBody) {
        if (requestBody == null) {
            return false;
        }
        String approvedVia = valueAsString(requestBody.get("approvedVia"));
        return approvedVia != null && "EMAIL".equalsIgnoreCase(approvedVia.trim());
    }

    private boolean isPortalDeferredEmailWorkflow(Map<String, Object> requestBody) {
        if (requestBody == null) {
            return false;
        }
        return Boolean.TRUE.equals(requestBody.get("deferEmail"))
                || "true".equalsIgnoreCase(String.valueOf(requestBody.get("deferEmail")));
    }

    private void appendTemplateHistory(CfgTblCustomFormApplication application, TemplateStep step, Integer actorId,
                                       String action, String remarks, Integer targetLevel) {
        appendTemplateHistory(application, step, actorId, null, action, remarks, targetLevel, null, null);
    }

    private void appendTemplateHistory(CfgTblCustomFormApplication application, TemplateStep step, Integer performedBy,
                                       Integer onBehalfOf, String action, String remarks, Integer targetLevel) {
        appendTemplateHistory(application, step, performedBy, onBehalfOf, action, remarks, targetLevel, null, null);
    }

    private void appendTemplateHistory(CfgTblCustomFormApplication application, TemplateStep step, Integer actorId,
                                       String action, String remarks, Integer targetLevel, String approvedIp,
                                       String approvedVia) {
        appendTemplateHistory(application, step, actorId, null, action, remarks, targetLevel, approvedIp, approvedVia);
    }

    private void appendTemplateHistory(CfgTblCustomFormApplication application, TemplateStep step, Integer performedBy,
                                       Integer onBehalfOf, String action, String remarks, Integer targetLevel,
                                       String approvedIp, String approvedVia) {
        Integer workflowApproverId = onBehalfOf != null ? onBehalfOf : performedBy;
        CfgTblUser workflowUser = workflowApproverId != null ? entityManager.find(CfgTblUser.class, workflowApproverId) : null;
        CfgTblUser performedUser = performedBy != null ? entityManager.find(CfgTblUser.class, performedBy) : null;
        boolean delegated = onBehalfOf != null && performedBy != null && !onBehalfOf.equals(performedBy);
        Map<String, Object> entry = new HashMap<>();
        Integer displayLevel = step != null
                ? ("initiator".equalsIgnoreCase(step.type) ? 1 : step.level + 2)
                : targetLevel;
        entry.put("action", action);
        entry.put("level", displayLevel);
        entry.put("intApprovalOrder", displayLevel);
        entry.put("toLevel", targetLevel);
        entry.put("stepId", step != null ? step.id : "");
        entry.put("pipelineStepId", step != null ? step.id : "");
        entry.put("signatureTargetId", step != null ? step.id : "");
        entry.put("stepType", step != null ? step.type : "");
        entry.put("approvalMode", step != null ? step.approvalMode : "");
        entry.put("role", step != null ? step.name : "");
        entry.put("departmentName", step != null ? step.name : "");
        entry.put("remarks", remarks != null ? remarks : "");
        entry.put("approvedBy", workflowApproverId);
        entry.put("userId", workflowApproverId);
        entry.put("approverName", workflowUser != null ? workflowUser.getTxtUserName() : "");
        if (delegated) {
            entry.put("performedBy", performedBy);
            entry.put("performedByName", performedUser != null ? performedUser.getTxtUserName() : "");
            entry.put("onBehalfOf", onBehalfOf);
            entry.put("onBehalfOfName", workflowUser != null ? workflowUser.getTxtUserName() : "");
            entry.put("signaturePath", performedUser != null ? performedUser.getTxtSignaturePath() : "");
            entry.put("txtDepartmentName", performedUser != null ? performedUser.getTxtDepartmentName() : "");
            entry.put("userDepartmentName", performedUser != null ? performedUser.getTxtDepartmentName() : "");
            entry.put("designation", performedUser != null ? performedUser.getTxtDesignation() : "");
            entry.put("txtDesignation", performedUser != null ? performedUser.getTxtDesignation() : "");
        } else {
            entry.put("signaturePath", workflowUser != null ? workflowUser.getTxtSignaturePath() : "");
            entry.put("txtDepartmentName", workflowUser != null ? workflowUser.getTxtDepartmentName() : "");
            entry.put("userDepartmentName", workflowUser != null ? workflowUser.getTxtDepartmentName() : "");
            entry.put("designation", workflowUser != null ? workflowUser.getTxtDesignation() : "");
            entry.put("txtDesignation", workflowUser != null ? workflowUser.getTxtDesignation() : "");
        }
        entry.put("approvedDate", now().toString());
        if (approvedIp != null && !approvedIp.trim().isEmpty()) {
            entry.put("approvedIp", approvedIp.trim());
        }
        if (approvedVia != null && !approvedVia.trim().isEmpty()) {
            entry.put("approvedVia", approvedVia.trim());
        }
        applyApprovalGeoToEntry(entry, approvedIp);

        List<Map<String, Object>> history = parseHistory(application.getTxtApprovalHistory());
        history.add(entry);
        application.setTxtApprovalHistory(writeJson(history));
        List<Map<String, Object>> prior = parseHistory(application.getTxtPriorApprovals());
        prior.add(new HashMap<>(entry));
        application.setTxtPriorApprovals(writeJson(prior));
    }

    private void appendSubmissionHistory(CfgTblCustomFormApplication application, HttpServletRequest request) {
        Integer initiatorId = application.getSerSubmittedBy();
        CfgTblUser initiator = initiatorId != null ? entityManager.find(CfgTblUser.class, initiatorId) : null;
        Integer delegateId = resolveSubmittedByDelegateUserId(application);
        boolean delegatedSubmit = delegateId != null && delegateId > 0
                && initiatorId != null && initiatorId > 0
                && !delegateId.equals(initiatorId);
        CfgTblUser delegate = delegatedSubmit ? entityManager.find(CfgTblUser.class, delegateId) : null;
        Map<String, Object> entry = new HashMap<>();
        entry.put("action", "SUBMITTED");
        entry.put("status", "SUBMITTED");
        entry.put("level", 1);
        entry.put("intApprovalOrder", 1);
        entry.put("stepId", "initiator");
        entry.put("pipelineStepId", "initiator");
        entry.put("signatureTargetId", "initiator");
        entry.put("stepType", "initiator");
        entry.put("role", "Submission");
        entry.put("departmentName", "Submission");
        String initiatorName = initiator != null ? initiator.getTxtUserName() : "Initiator";
        String delegateName = delegate != null ? delegate.getTxtUserName() : "";
        if (delegatedSubmit) {
            entry.put("remarks", "Submitted by " + valueAsString(delegateName) + " on behalf of " + valueAsString(initiatorName));
            entry.put("delegated", true);
            entry.put("performedBy", delegateId);
            entry.put("performedByName", delegateName);
            entry.put("onBehalfOf", initiatorId);
            entry.put("onBehalfOfName", initiatorName);
        } else {
            entry.put("remarks", "Submitted application");
        }
        entry.put("approvedBy", initiatorId);
        entry.put("userId", initiatorId);
        entry.put("approverName", initiatorName);
        entry.put("signaturePath", initiator != null ? initiator.getTxtSignaturePath() : "");
        entry.put("txtDepartmentName", initiator != null ? initiator.getTxtDepartmentName() : "");
        entry.put("userDepartmentName", initiator != null ? initiator.getTxtDepartmentName() : "");
        entry.put("designation", initiator != null ? initiator.getTxtDesignation() : "");
        entry.put("txtDesignation", initiator != null ? initiator.getTxtDesignation() : "");
        entry.put("approvedDate", application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : now().toString());
        String clientIp = RequestMetadataUtil.resolveClientIp(request);
        if (clientIp != null && !clientIp.trim().isEmpty()) {
            entry.put("approvedIp", clientIp.trim());
            entry.put("approvedVia", "SYSTEM");
            applyApprovalGeoToEntry(entry, clientIp.trim());
        }

        List<Map<String, Object>> history = parseHistory(application.getTxtApprovalHistory());
        history.add(entry);
        application.setTxtApprovalHistory(writeJson(history));
        List<Map<String, Object>> prior = parseHistory(application.getTxtPriorApprovals());
        prior.add(new HashMap<>(entry));
        application.setTxtPriorApprovals(writeJson(prior));
    }

    private void applyApprovalGeoToEntry(Map<String, Object> entry, String approvedIp) {
        if (approvedIp == null || approvedIp.trim().isEmpty()) {
            return;
        }
        ipGeolocationService.resolve(approvedIp.trim()).ifPresent((geo) -> {
            entry.put("approvalLatitude", geo.latitude);
            entry.put("approvalLongitude", geo.longitude);
            if (geo.label != null && !geo.label.isEmpty()) {
                entry.put("approvalLocationLabel", geo.label);
            }
        });
    }

    private List<Map<String, Object>> activeTemplateHistory(List<Map<String, Object>> history) {
        List<Map<String, Object>> active = new ArrayList<>();
        for (Map<String, Object> entry : history) {
            String action = valueAsString(entry.get("action")).toUpperCase(Locale.ROOT);
            if ("SENT_BACK_TO_INITIATOR".equals(action)) {
                active.removeIf(existing -> !entryBelongsToTemplateSubmission(existing));
                active.add(entry);
                continue;
            }
            if ("SENT_BACK".equals(action)) {
                Integer toLevel = toInteger(firstObject(entry.get("toLevel"), entry.get("targetLevel"), entry.get("returnLevel")));
                int resetLevel = toLevel == null || toLevel <= 0 ? 1 : toLevel;
                active.removeIf(existing -> {
                    if (entryBelongsToTemplateSubmission(existing)) {
                        return false;
                    }
                    Integer existingLevel = toInteger(firstObject(existing.get("intApprovalOrder"), existing.get("level")));
                    return existingLevel != null && existingLevel >= resetLevel;
                });
                active.add(entry);
                continue;
            }
            if ("RESUBMITTED_BY_INITIATOR".equals(action)) {
                active.removeIf(existing -> !entryBelongsToTemplateSubmission(existing));
                active.add(entry);
                continue;
            }
            active.add(entry);
        }
        return active;
    }

    private boolean entryBelongsToTemplateSubmission(Map<String, Object> entry) {
        String action = valueAsString(entry.get("action")).toUpperCase(Locale.ROOT);
        String stepId = firstText(entry.get("stepId"), entry.get("pipelineStepId"), entry.get("signatureTargetId"));
        String role = valueAsString(firstObject(entry.get("role"), entry.get("departmentName"))).trim().toLowerCase(Locale.ROOT);
        Integer level = toInteger(firstObject(entry.get("intApprovalOrder"), entry.get("level")));
        return "SUBMITTED".equals(action)
                || "initiator".equalsIgnoreCase(stepId)
                || "submission".equals(role)
                || (level != null && level == 1 && "resubmitted_by_initiator".equalsIgnoreCase(action));
    }

    private void applySubmitInitiatorAndDelegate(CfgTblCustomFormApplication application,
                                                 TemplateDefinition definition,
                                                 int actorUserId) {
        if (application == null || definition == null || actorUserId <= 0) {
            throw new IllegalArgumentException("You must be logged in to submit this application");
        }
        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        Integer delegateFromPayload = toInteger(appData.get("submittedByDelegateUserId"));
        Integer requestedInitiator = application.getSerSubmittedBy();
        if (requestedInitiator == null || requestedInitiator <= 0) {
            requestedInitiator = toInteger(appData.get("onBehalfOfUserId"));
        }
        boolean onBehalfMode = "on_behalf".equalsIgnoreCase(valueAsString(appData.get("fillAccessMode")))
                || (delegateFromPayload != null && delegateFromPayload > 0
                && requestedInitiator != null && requestedInitiator > 0
                && !delegateFromPayload.equals(requestedInitiator));
        if (onBehalfMode) {
            if (delegateFromPayload == null || delegateFromPayload <= 0) {
                delegateFromPayload = actorUserId;
            }
            if (!delegateFromPayload.equals(actorUserId)) {
                throw new IllegalArgumentException("Invalid delegation submit context");
            }
            if (requestedInitiator == null || requestedInitiator <= 0) {
                throw new IllegalArgumentException("On-behalf user is required");
            }
            if (!canDelegateSubmitOnBehalf(actorUserId, requestedInitiator, definition)) {
                throw new IllegalArgumentException(
                        "You are not delegated to submit this form on behalf of the selected user");
            }
            application.setSerSubmittedBy(requestedInitiator);
            appData.put("fillAccessMode", "on_behalf");
            appData.put("onBehalfOfUserId", requestedInitiator);
            appData.put("submittedByDelegateUserId", actorUserId);
        } else {
            application.setSerSubmittedBy(actorUserId);
            appData.put("fillAccessMode", "self");
            appData.remove("onBehalfOfUserId");
            appData.remove("submittedByDelegateUserId");
        }
        application.setTxtApplicationData(writeJson(appData));
        if (application.getSerCreatedUser() == null) {
            application.setSerCreatedUser(actorUserId);
        }
    }

    private boolean canDelegateSubmitOnBehalf(Integer delegateUserId, Integer onBehalfUserId, TemplateDefinition definition) {
        if (delegateUserId == null || delegateUserId <= 0 || onBehalfUserId == null || onBehalfUserId <= 0
                || definition == null || definition.getSerFormId() == null) {
            return false;
        }
        for (UserDelegationOnBehalfListItemDto party : userDelegationService.getDelegationOnBehalfForTemplate(
                delegateUserId, definition.getSerFormId())) {
            if (party != null && onBehalfUserId.equals(party.getSerOnBehalfUserId())) {
                return true;
            }
        }
        return false;
    }

    private Integer resolveSubmittedByDelegateUserId(CfgTblCustomFormApplication application) {
        if (application == null || application.getTxtApplicationData() == null) {
            return null;
        }
        return toInteger(parseMap(application.getTxtApplicationData()).get("submittedByDelegateUserId"));
    }

    private void enrichTemplateApplicationPayload(CfgTblCustomFormApplication application, TemplateDefinition definition) {
        if (application.getTxtApplicationData() == null || application.getTxtApplicationData().trim().isEmpty()) {
            application.setTxtApplicationData("{}");
        }
        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        if (asMap(appData.get("templatePayload")).isEmpty()) {
            appData.put("templatePayload", parseMap(definition.getTxtTemplatePayload()));
            application.setTxtApplicationData(writeJson(appData));
        }
        if (application.getSerSubmittedBy() == null) {
            application.setSerSubmittedBy(toInteger(appData.get("submittedBy")));
        }
        if (application.getSerCreatedUser() == null) {
            application.setSerCreatedUser(application.getSerSubmittedBy());
        }
        CfgTblUser submittedByUser = application.getSerSubmittedBy() != null
                ? entityManager.find(CfgTblUser.class, application.getSerSubmittedBy())
                : null;
        if (submittedByUser != null) {
            Integer submittedDepartmentId = submittedByUser.getHrTblDepartment() != null
                    ? submittedByUser.getHrTblDepartment().getSerDepartmentId()
                    : null;
            String submittedDepartmentName = submittedByUser.getTxtDepartmentName();
            if ((submittedDepartmentName == null || submittedDepartmentName.trim().isEmpty())
                    && submittedByUser.getHrTblDepartment() != null) {
                submittedDepartmentName = submittedByUser.getHrTblDepartment().getTxtDepartmentName();
            }
            if (submittedDepartmentId != null && appData.get("submittedDepartmentId") == null
                    && appData.get("serSubmittedDepartmentId") == null
                    && appData.get("submitterDepartmentId") == null) {
                appData.put("submittedDepartmentId", submittedDepartmentId);
            }
            if (submittedDepartmentName != null && !submittedDepartmentName.trim().isEmpty()
                    && appData.get("submittedDepartmentName") == null
                    && appData.get("submitterDepartmentName") == null
                    && appData.get("txtSubmittedDepartmentName") == null
                    && appData.get("txtDepartmentName") == null
                    && appData.get("departmentName") == null
                    && appData.get("submittedByDepartmentName") == null) {
                appData.put("submittedDepartmentName", submittedDepartmentName.trim());
            }
            application.setTxtApplicationData(writeJson(appData));
        }
    }

    private void applyTemplateSubmitWorkflowState(CfgTblCustomFormApplication application, HttpServletRequest request) {
        TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
        TemplateStep firstStep = context.stepAt(0);
        if (firstStep == null || firstStep.approverIds.isEmpty()) {
            application.setTxtStatus("COMPLETED");
            application.setIntCurrentApprovalLevel(1);
            application.setSerCurrentApprover(null);
        } else {
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(0));
            application.setSerCurrentApprover(firstApproverId(firstStep));
        }
        appendSubmissionHistory(application, request);
    }

    private Map<String, Object> submitTemplateApplicationFromDraft(CfgTblCustomFormApplication incoming,
                                                                   CfgTblCustomFormApplication existing,
                                                                   TemplateDefinition definition,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        int actorUserId = commonService.getCurrentLoggedInUser();
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("You must be logged in to submit this application");
        }
        assertCanEditTemplateDraft(existing, actorUserId);

        existing.setTxtApplicationData(incoming.getTxtApplicationData());
        if (incoming.getSerSubmittedBy() != null && incoming.getSerSubmittedBy() > 0) {
            existing.setSerSubmittedBy(incoming.getSerSubmittedBy());
        }
        enrichTemplateApplicationPayload(existing, definition);
        workflowThresholdRuleApplyService.applyActiveRulesOnSubmit(existing);
        applySubmitInitiatorAndDelegate(existing, definition, actorUserId);
        assertTemplateAttachmentFieldSizeLimit(existing.getTxtApplicationData(), actorUserId);
        if (existing.getTxtFormCode() == null || existing.getTxtFormCode().trim().isEmpty()) {
            existing.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, incoming.getTxtFormCode()));
        } else {
            existing.setTxtFormCode(resolveUniqueTemplateApplicationCode(definition, existing.getTxtFormCode()));
        }
        existing.setDteModifiedDate(now());
        existing.setSerModifiedUser(actorUserId);
        applyTemplateSubmitWorkflowState(existing, request);
        entityManager.merge(existing);
        entityManager.flush();

        logTemplateAction("FORM_SUBMIT", request, existing.getSerApplicationId(), "SUCCESS",
                "Template application submitted from draft",
                buildTemplateApplicationPayload(existing, "FORM_SUBMIT", null), null);
        templateWorkflowNotificationService.notifySubmitted(existing, actorUserId);

        result.put("status", "Success");
        result.put("message", "Template application submitted successfully");
        result.put("applicationId", existing.getSerApplicationId());
        result.put("formCode", existing.getTxtFormCode());
        return result;
    }

    private boolean isDraftStatus(String status) {
        return status != null && "DRAFT".equalsIgnoreCase(status.trim());
    }

    private void assertCanEditTemplateDraft(CfgTblCustomFormApplication application, int actorUserId) {
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("You are not authorized to edit this draft");
        }
        if (isAdminUser(actorUserId)) {
            return;
        }
        Integer submittedBy = application.getSerSubmittedBy();
        if (submittedBy != null && submittedBy.equals(actorUserId)) {
            return;
        }
        Integer delegateUserId = resolveSubmittedByDelegateUserId(application);
        if (delegateUserId != null && delegateUserId.equals(actorUserId)) {
            return;
        }
        throw new IllegalArgumentException("You are not authorized to edit this draft");
    }

    private String resolveUniqueTemplateApplicationCode(TemplateDefinition definition, String requestedCode) {
        String candidate = requestedCode != null ? requestedCode.trim().toUpperCase(Locale.ROOT) : null;
        if (candidate == null || candidate.isEmpty()) {
            candidate = generateNextTemplateApplicationCode(definition);
        }
        int attempts = 0;
        while (templateApplicationCodeExists(candidate) && attempts < 1000) {
            candidate = incrementTemplateCode(candidate);
            if (candidate == null || candidate.isEmpty()) {
                candidate = generateNextTemplateApplicationCode(definition);
            }
            attempts++;
        }
        return candidate != null && !candidate.isEmpty() ? candidate : "TMP-" + System.currentTimeMillis();
    }

    private String generateNextTemplateApplicationCode(TemplateDefinition definition) {
        String convention = definition != null ? valueAsString(definition.getTxtCodeConvention()).trim().toUpperCase(Locale.ROOT) : "";
        String prefix = "TMP";
        int width = 4;
        if (!convention.isEmpty()) {
            int hyphen = convention.lastIndexOf("-");
            if (hyphen > 0 && hyphen < convention.length() - 1 && convention.substring(hyphen + 1).matches("0+")) {
                prefix = convention.substring(0, hyphen);
                width = Math.max(1, convention.length() - hyphen - 1);
            } else {
                prefix = convention;
            }
        }
        String pattern = prefix + "-%";
        @SuppressWarnings("unchecked")
        List<String> codes = entityManager.createQuery(
                        "SELECT a.txtFormCode FROM CfgTblCustomFormApplication a " +
                                "WHERE a.txtFormCode IS NOT NULL AND UPPER(a.txtFormCode) LIKE :pattern " +
                                "AND (a.blIsDeleted IS NULL OR a.blIsDeleted = false)")
                .setParameter("pattern", pattern)
                .getResultList();
        int max = 0;
        for (String code : codes) {
            Integer suffix = extractTemplateCodeSuffix(code, prefix);
            if (suffix != null) {
                max = Math.max(max, suffix);
            }
        }
        return String.format("%s-%0" + width + "d", prefix, max + 1);
    }

    private boolean templateApplicationCodeExists(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        Long count = (Long) entityManager.createQuery(
                        "SELECT COUNT(a.serApplicationId) FROM CfgTblCustomFormApplication a " +
                                "WHERE UPPER(a.txtFormCode) = :code AND (a.blIsDeleted IS NULL OR a.blIsDeleted = false)")
                .setParameter("code", code.trim().toUpperCase(Locale.ROOT))
                .getSingleResult();
        return count != null && count > 0;
    }

    private String incrementTemplateCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim().toUpperCase(Locale.ROOT);
        int hyphen = trimmed.lastIndexOf("-");
        if (hyphen <= 0 || hyphen >= trimmed.length() - 1) {
            return null;
        }
        String numberPart = trimmed.substring(hyphen + 1);
        if (!numberPart.matches("\\d+")) {
            return null;
        }
        int next = Integer.parseInt(numberPart) + 1;
        return String.format("%s-%0" + numberPart.length() + "d", trimmed.substring(0, hyphen), next);
    }

    private Integer extractTemplateCodeSuffix(String code, String prefix) {
        if (code == null || prefix == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT) + "-";
        if (!normalized.startsWith(normalizedPrefix)) {
            return null;
        }
        String suffix = normalized.substring(normalizedPrefix.length());
        if (!suffix.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void touchTemplateApplication(CfgTblCustomFormApplication application, Integer actorId) {
        application.setDteModifiedDate(now());
        application.setSerModifiedUser(actorId);
    }

    private Integer getUserDepartmentId(Integer userId) {
        if (userId == null) {
            return null;
        }
        CfgTblUser user = entityManager.find(CfgTblUser.class, userId);
        return user != null && user.getHrTblDepartment() != null ? user.getHrTblDepartment().getSerDepartmentId() : null;
    }

    private Set<Integer> getDepartmentHeadIds(Integer departmentId) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (departmentId == null) {
            return ids;
        }
        HrTblDepartment department = entityManager.find(HrTblDepartment.class, departmentId);
        if (department != null && department.getSerDepartmentHeadId() != null) {
            for (String item : department.getSerDepartmentHeadId().split(",")) {
                addIfPresent(ids, toInteger(item));
            }
        }
        return ids;
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Success");
        result.put("message", message);
        return result;
    }

    private Map<String, Object> failure(Map<String, Object> result, HttpServletResponse response, String prefix, Exception ex) {
        logger.error(prefix + ": " + ex.getMessage(), ex);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        result.put("status", "Failure");
        result.put("message", ex.getMessage());
        return result;
    }

    private int safeLevel(CfgTblCustomFormApplication application) {
        Integer storedLevel = application.getIntCurrentApprovalLevel();
        if (storedLevel == null) {
            return 0;
        }
        if (storedLevel <= 1) {
            return 0;
        }
        return Math.max(0, storedLevel - 2);
    }

    private int displayLevelForIndex(int index) {
        return Math.max(0, index) + 2;
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private Map<String, Object> parseMap(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private void assertTemplateAttachmentFieldSizeLimit(String applicationDataRaw, int userId) {
        String message = validateTemplateAttachmentFieldSizeLimit(applicationDataRaw, userId);
        if (message != null) {
            throw new IllegalArgumentException(message);
        }
    }

    private String validateTemplateAttachmentFieldSizeLimit(String applicationDataRaw, int userId) {
        if (applicationDataRaw == null || applicationDataRaw.trim().isEmpty()) {
            return null;
        }
        int maxMb = resolveUserMaxAttachmentMb(userId);
        long maxBytes = maxMb * 1024L * 1024L;
        Map<String, Object> appData = parseMap(applicationDataRaw);
        Map<String, Object> values = asMap(appData.get("templateValues"));
        if (values.isEmpty()) {
            values = appData;
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if ("templateValues".equals(key) || "footerFields".equals(key) || "templatePayload".equals(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (!(value instanceof List<?>)) {
                continue;
            }
            List<?> list = (List<?>) value;
            if (list.isEmpty() || !(list.get(0) instanceof Map<?, ?>)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> firstAttachment = (Map<String, Object>) list.get(0);
            if (!looksLikeAttachmentPayload(firstAttachment)) {
                continue;
            }
            long fieldBytes = 0L;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?>)) {
                    continue;
                }
                fieldBytes += estimateAttachmentBytesFromPayloadMap((Map<?, ?>) item);
            }
            if (fieldBytes > maxBytes) {
                double actualMb = fieldBytes / (1024.0 * 1024.0);
                return String.format(Locale.US,
                        "These files are about %.2f MB. Your account allows up to %d MB per attachment field. Please remove some files or choose smaller ones.",
                        actualMb, maxMb);
            }
        }
        return null;
    }

    private int resolveUserMaxAttachmentMb(int userId) {
        if (userId <= 0) {
            return DEFAULT_MAX_ATTACHMENT_MB;
        }
        // Use a separate EntityManager via commonService so a missing DB column
        // (num_max_attachment_mb) cannot mark this request's transaction rollback-only.
        try {
            CfgTblUser user = commonService.getCurrentUser(userId);
            if (user != null && user.getNumMaxAttachmentMb() != null && user.getNumMaxAttachmentMb() > 0) {
                return user.getNumMaxAttachmentMb();
            }
        } catch (Exception ex) {
            logger.warn("Could not resolve max attachment MB for user " + userId + ": " + ex.getMessage());
        }
        return DEFAULT_MAX_ATTACHMENT_MB;
    }

    private long estimateAttachmentBytesFromPayloadMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return 0L;
        }
        Object encodedObj = map.get("base64");
        if (encodedObj == null || String.valueOf(encodedObj).trim().isEmpty()) {
            encodedObj = map.get("dataUrl");
        }
        if (encodedObj == null) {
            return 0L;
        }
        String payload = String.valueOf(encodedObj).trim();
        if (payload.startsWith("data:")) {
            int commaIndex = payload.indexOf(',');
            if (commaIndex < 0 || commaIndex >= payload.length() - 1) {
                return 0L;
            }
            payload = payload.substring(commaIndex + 1);
        }
        payload = payload.replaceAll("\\s+", "");
        if (payload.isEmpty()) {
            return 0L;
        }
        int padding = 0;
        if (payload.endsWith("==")) {
            padding = 2;
        } else if (payload.endsWith("=")) {
            padding = 1;
        }
        long length = payload.length();
        long decoded = (length * 3L) / 4L - padding;
        return Math.max(0L, decoded);
    }

    private List<Map<String, Object>> extractCompletedPortalAttachmentMetas(Integer applicationId,
                                                                             String formCode,
                                                                             String status,
                                                                             Object createdDate,
                                                                             String templateName,
                                                                             String applicationDataRaw) {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> appData = parseMap(applicationDataRaw);
        if (appData.isEmpty()) {
            return items;
        }

        Map<String, String> attachmentFieldLabels = new HashMap<>();
        for (Map<String, Object> field : asListOfMaps(asMap(appData.get("templatePayload")).get("fields"))) {
            if (field == null || field.isEmpty()) {
                continue;
            }
            if (!"attachment".equalsIgnoreCase(valueAsString(field.get("type")))) {
                continue;
            }
            String fieldId = valueAsString(field.get("id"));
            if (fieldId == null || fieldId.trim().isEmpty()) {
                continue;
            }
            String label = valueAsString(field.get("label"));
            attachmentFieldLabels.put(fieldId.trim(),
                    label != null && !label.trim().isEmpty() ? label.trim() : "Attachment");
        }

        Map<String, Object> valueRoots = new LinkedHashMap<>();
        valueRoots.put("root", appData);
        valueRoots.put("templateValues", asMap(appData.get("templateValues")));

        Set<String> emittedKeys = new HashSet<>();
        for (Map.Entry<String, Object> rootEntry : valueRoots.entrySet()) {
            Map<String, Object> valuesMap = asMap(rootEntry.getValue());
            for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
                String fieldId = entry.getKey();
                if (fieldId == null || fieldId.trim().isEmpty()) {
                    continue;
                }
                if ("templateValues".equals(fieldId)
                        || "footerFields".equals(fieldId)
                        || "templatePayload".equals(fieldId)
                        || "templateOpinionRequest".equals(fieldId)) {
                    continue;
                }
                List<Map<String, Object>> attachments = asListOfMaps(entry.getValue());
                if (attachments.isEmpty() && entry.getValue() instanceof Map) {
                    Map<String, Object> single = asMap(entry.getValue());
                    if (!single.isEmpty()) {
                        attachments = new ArrayList<>();
                        attachments.add(single);
                    }
                }
                for (int index = 0; index < attachments.size(); index++) {
                    Map<String, Object> attachment = attachments.get(index);
                    if (!looksLikeAttachmentPayload(attachment)) {
                        continue;
                    }
                    String fileName = firstNonBlank(
                            valueAsString(attachment.get("fileName")),
                            valueAsString(attachment.get("name")));
                    if (fileName == null || fileName.trim().isEmpty()) {
                        continue;
                    }
                    String mimeType = firstNonBlank(
                            valueAsString(attachment.get("mimeType")),
                            valueAsString(attachment.get("type")),
                            "application/octet-stream");
                    if (!isAllowedPortalAttachmentMimeOrName(mimeType, fileName)) {
                        continue;
                    }
                    String emitKey = applicationId + "|" + fieldId + "|" + index;
                    if (!emittedKeys.add(emitKey)) {
                        continue;
                    }
                    Map<String, Object> item = new HashMap<>();
                    item.put("applicationId", applicationId);
                    item.put("formCode", formCode);
                    item.put("status", status);
                    item.put("createdDate", createdDate);
                    item.put("templateName", templateName);
                    item.put("fieldId", fieldId);
                    item.put("fieldLabel", attachmentFieldLabels.getOrDefault(fieldId, "Attachment"));
                    item.put("attachmentIndex", index);
                    item.put("fileName", fileName.trim());
                    item.put("mimeType", mimeType);
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Map<String, Object> resolvePortalAttachmentPayload(String applicationDataRaw,
                                                               String fieldId,
                                                               int attachmentIndex) {
        Map<String, Object> appData = parseMap(applicationDataRaw);
        if (appData.isEmpty()) {
            return null;
        }
        Object rawValue = appData.get(fieldId);
        if (rawValue == null) {
            rawValue = asMap(appData.get("templateValues")).get(fieldId);
        }
        List<Map<String, Object>> attachments = asListOfMaps(rawValue);
        if (attachments.isEmpty() && rawValue instanceof Map) {
            attachments = new ArrayList<>();
            attachments.add(asMap(rawValue));
        }
        if (attachmentIndex < 0 || attachmentIndex >= attachments.size()) {
            return null;
        }
        Map<String, Object> source = attachments.get(attachmentIndex);
        if (!looksLikeAttachmentPayload(source)) {
            return null;
        }
        String fileName = firstNonBlank(valueAsString(source.get("fileName")), valueAsString(source.get("name")));
        String mimeType = firstNonBlank(
                valueAsString(source.get("mimeType")),
                valueAsString(source.get("type")),
                "application/octet-stream");
        String dataUrl = valueAsString(source.get("dataUrl"));
        String base64 = valueAsString(source.get("base64"));
        if ((base64 == null || base64.trim().isEmpty()) && dataUrl != null && dataUrl.contains(",")) {
            base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
        }
        if (fileName == null || fileName.trim().isEmpty() || base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        if (!isAllowedPortalAttachmentMimeOrName(mimeType, fileName)) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName.trim());
        payload.put("mimeType", mimeType);
        payload.put("base64", base64.trim());
        if (dataUrl != null && !dataUrl.trim().isEmpty()) {
            payload.put("dataUrl", dataUrl.trim());
        } else {
            payload.put("dataUrl", "data:" + mimeType + ";base64," + base64.trim());
        }
        return payload;
    }

    private boolean looksLikeAttachmentPayload(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String fileName = firstNonBlank(valueAsString(value.get("fileName")), valueAsString(value.get("name")));
        String base64 = valueAsString(value.get("base64"));
        String dataUrl = valueAsString(value.get("dataUrl"));
        return fileName != null && !fileName.trim().isEmpty()
                && ((base64 != null && !base64.trim().isEmpty()) || (dataUrl != null && !dataUrl.trim().isEmpty()));
    }

    private boolean isAllowedPortalAttachmentMimeOrName(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        if ("application/pdf".equals(mime)
                || "image/webp".equals(mime)
                || "image/png".equals(mime)
                || "image/jpeg".equals(mime)
                || "image/jpg".equals(mime)) {
            return true;
        }
        String name = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";
        return "pdf".equals(ext) || "webp".equals(ext) || "png".equals(ext) || "jpeg".equals(ext) || "jpg".equals(ext);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private List<Map<String, Object>> parseHistory(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(value instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private Object nested(Object parent, String key) {
        return asMap(parent).get(key);
    }

    private Object firstObject(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(Object... values) {
        Object value = firstObject(values);
        return value == null ? "" : String.valueOf(value);
    }

    private void addIfPresent(Set<Integer> ids, Integer id) {
        if (id != null && id > 0) {
            ids.add(id);
        }
    }

    private static final String VELOCITY_REPORT_JOIN =
            " FROM CfgTblCustomFormApplication a " +
                    "INNER JOIN TemplateDefinition t ON t.serFormId = a.serFormId " +
                    "LEFT JOIN CfgTblUser u ON u.serUserId = a.serSubmittedBy ";

    private static class VelocityReportQuery {
        private final StringBuilder where = new StringBuilder();
        private final Map<String, Object> params = new HashMap<>();

        private VelocityReportQuery() {
            where.append("WHERE (a.blIsDeleted IS NULL OR a.blIsDeleted = false) ")
                    .append("AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ");
        }
    }

    private VelocityReportQuery buildVelocityReportQuery(boolean scopeAll,
                                                         int actorUserId,
                                                         String searchText,
                                                         Set<String> statusFilter,
                                                         Set<String> typeFilter,
                                                         Set<String> departmentFilter,
                                                         Set<String> uploadedByFilter,
                                                         String fromDate,
                                                         String toDate) {
        VelocityReportQuery query = new VelocityReportQuery();
        if (!scopeAll) {
            query.where.append("AND a.serSubmittedBy = :actorUserId ");
            query.params.put("actorUserId", actorUserId);
        }
        appendVelocityReportSearchToWhere(query, searchText);
        if (!typeFilter.isEmpty()) {
            query.where.append("AND t.txtTemplateName IN :reportTypes ");
            query.params.put("reportTypes", typeFilter);
        }
        Set<String> rawStatuses = expandStatusFilterForSql(statusFilter);
        if (!rawStatuses.isEmpty()) {
            query.where.append("AND UPPER(COALESCE(a.txtStatus, '')) IN :reportStatuses ");
            query.params.put("reportStatuses", rawStatuses);
        }
        if (!departmentFilter.isEmpty()) {
            query.where.append("AND COALESCE(u.txtDepartmentName, '') IN :reportDepartments ");
            query.params.put("reportDepartments", departmentFilter);
        }
        if (!uploadedByFilter.isEmpty()) {
            query.where.append("AND COALESCE(u.txtUserName, CONCAT('User ', a.serSubmittedBy)) IN :reportUploaders ");
            query.params.put("reportUploaders", uploadedByFilter);
        }
        if (fromDate != null) {
            query.where.append("AND a.dteCreatedDate >= :reportFromDate ");
            query.params.put("reportFromDate", Timestamp.valueOf(fromDate + " 00:00:00"));
        }
        if (toDate != null) {
            query.where.append("AND a.dteCreatedDate <= :reportToDate ");
            query.params.put("reportToDate", Timestamp.valueOf(toDate + " 23:59:59"));
        }
        return query;
    }

    private void appendVelocityReportSearchToWhere(VelocityReportQuery query, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        java.util.regex.Matcher phraseMatcher = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(searchText);
        int phraseIndex = 0;
        while (phraseMatcher.find()) {
            String phrase = phraseMatcher.group(1).toLowerCase(Locale.ROOT);
            if (phrase.isEmpty()) {
                continue;
            }
            String param = "reportPhrase" + phraseIndex++;
            query.where.append("AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtStatus, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtRemarks, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtApplicationData, '')) LIKE :").append(param)
                    .append(") ");
            query.params.put(param, "%" + phrase + "%");
        }
        String remaining = searchText.replaceAll("\"[^\"]+\"", " ").trim();
        if (remaining.isEmpty()) {
            return;
        }
        String[] tokens = remaining.split("\\s+");
        int tokenIndex = 0;
        for (String token : tokens) {
            if (token.isEmpty() || token.startsWith("-")) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon > 0 && colon < token.length() - 1) {
                token = token.substring(colon + 1);
            }
            if (token.isEmpty()) {
                continue;
            }
            String param = "reportTok" + tokenIndex++;
            query.where.append("AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtStatus, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtRemarks, '')) LIKE :").append(param)
                    .append(" OR LOWER(COALESCE(a.txtApplicationData, '')) LIKE :").append(param)
                    .append(") ");
            query.params.put(param, "%" + token.toLowerCase(Locale.ROOT) + "%");
        }
    }

    private Set<String> expandStatusFilterForSql(Set<String> statusFilter) {
        Set<String> expanded = new LinkedHashSet<>();
        for (String status : statusFilter) {
            if (status == null || status.trim().isEmpty()) {
                continue;
            }
            String lower = status.trim().toLowerCase(Locale.ROOT);
            if ("approved".equals(lower)) {
                expanded.add("APPROVED");
                expanded.add("COMPLETED");
            } else if ("rejected".equals(lower)) {
                expanded.add("REJECTED");
            } else if ("draft".equals(lower)) {
                expanded.add("DRAFT");
            } else if ("pending".equals(lower)) {
                expanded.add("PENDING");
                expanded.add("OPINION_PENDING");
                expanded.add("CEO_PENDING");
                expanded.add("ASSET_PENDING");
                expanded.add("PR_PENDING");
                expanded.add("PO_PENDING");
                expanded.add("PO_VENDOR_TE_PENDING");
                expanded.add("HOLD");
            } else if ("in progress".equals(lower)) {
                expanded.add("IN_PROGRESS");
            } else {
                expanded.add(status.trim().toUpperCase(Locale.ROOT));
            }
        }
        return expanded;
    }

    private void bindVelocityReportParams(javax.persistence.Query query, VelocityReportQuery reportQuery) {
        for (Map.Entry<String, Object> entry : reportQuery.params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private Long countVelocityReportRows(VelocityReportQuery reportQuery) {
        javax.persistence.Query countQuery = entityManager.createQuery(
                "SELECT COUNT(a.serApplicationId) " + VELOCITY_REPORT_JOIN + reportQuery.where);
        bindVelocityReportParams(countQuery, reportQuery);
        return (Long) countQuery.getSingleResult();
    }

    private javax.persistence.Query createVelocityReportListQuery(VelocityReportQuery reportQuery, String sortKey) {
        String orderBy = " ORDER BY a.dteCreatedDate DESC";
        if ("date-asc".equals(sortKey)) {
            orderBy = " ORDER BY a.dteCreatedDate ASC";
        }
        String jpql = "SELECT a.serApplicationId, a.serFormId, a.txtFormCode, a.txtStatus, a.serSubmittedBy, " +
                "a.dteCreatedDate, t.txtTemplateName, a.txtPdfName, a.txtRemarks, " +
                "CASE WHEN a.blbPdfData IS NULL THEN false ELSE true END, " +
                "COALESCE(u.txtUserName, ''), COALESCE(u.txtDepartmentName, '') " +
                VELOCITY_REPORT_JOIN + reportQuery.where + orderBy;
        javax.persistence.Query listQuery = entityManager.createQuery(jpql);
        bindVelocityReportParams(listQuery, reportQuery);
        return listQuery;
    }

    private Map<String, Object> mapVelocityReportRow(Object[] row) {
        Integer applicationId = toInteger(row[0]);
        if (applicationId == null || applicationId <= 0) {
            return null;
        }
        String formCode = row[2] == null ? "" : String.valueOf(row[2]).trim();
        String status = row[3] == null ? "" : String.valueOf(row[3]).trim();
        Integer submittedBy = toInteger(row[4]);
        String templateName = row[6] == null ? "Template" : String.valueOf(row[6]).trim();
        String pdfName = row[7] == null ? "" : String.valueOf(row[7]).trim();
        String remarks = row[8] == null ? "" : String.valueOf(row[8]);
        boolean hasPdf = Boolean.TRUE.equals(row[9]) || "true".equalsIgnoreCase(String.valueOf(row[9]));
        String submitterName = row[10] == null || String.valueOf(row[10]).trim().isEmpty()
                ? resolveUserDisplayName(null, submittedBy)
                : String.valueOf(row[10]).trim();
        String departmentName = row[11] == null ? "" : String.valueOf(row[11]).trim();
        String uploadedAt = formatReportDate(row[5]);
        String filename = resolveReportFilename(formCode, applicationId, pdfName, hasPdf);
        String snippet = buildReportListSnippet(formCode, templateName, remarks);
        String displayStatus = humanizeReportStatus(status);

        Map<String, Object> item = new HashMap<>();
        item.put("id", "app-" + applicationId);
        item.put("serApplicationId", applicationId);
        item.put("serFormId", toInteger(row[1]));
        item.put("filename", filename);
        item.put("mimeType", hasPdf ? "application/pdf" : "application/octet-stream");
        item.put("type", templateName);
        item.put("templateName", templateName);
        item.put("department", departmentName);
        item.put("uploadedBy", submitterName);
        item.put("uploadedAt", uploadedAt);
        item.put("amount", 0D);
        item.put("status", displayStatus);
        item.put("txtStatus", status);
        item.put("text", snippet);
        item.put("hasPdf", hasPdf);
        item.put("txtFormCode", formCode);
        return item;
    }

    private Map<String, Map<String, Integer>> loadVelocityReportFacetCounts(boolean scopeAll,
                                                                            int actorUserId,
                                                                            String searchText,
                                                                            Set<String> statusFilter,
                                                                            Set<String> typeFilter,
                                                                            Set<String> departmentFilter,
                                                                            Set<String> uploadedByFilter,
                                                                            String fromDate,
                                                                            String toDate) {
        Map<String, Map<String, Integer>> facetCounts = new HashMap<>();
        VelocityReportQuery typeFacetQuery = buildVelocityReportQuery(
                scopeAll, actorUserId, searchText, statusFilter, java.util.Collections.emptySet(),
                departmentFilter, uploadedByFilter, fromDate, toDate);
        facetCounts.put("type", runVelocityReportFacetGroup(
                "SELECT t.txtTemplateName, COUNT(a.serApplicationId) " + VELOCITY_REPORT_JOIN + typeFacetQuery.where
                        + " GROUP BY t.txtTemplateName ORDER BY COUNT(a.serApplicationId) DESC",
                typeFacetQuery, 40));

        VelocityReportQuery statusFacetQuery = buildVelocityReportQuery(
                scopeAll, actorUserId, searchText, java.util.Collections.emptySet(), typeFilter,
                departmentFilter, uploadedByFilter, fromDate, toDate);
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> rawStatusCounts = runVelocityReportFacetGroup(
                "SELECT a.txtStatus, COUNT(a.serApplicationId) " + VELOCITY_REPORT_JOIN + statusFacetQuery.where
                        + " GROUP BY a.txtStatus ORDER BY COUNT(a.serApplicationId) DESC",
                statusFacetQuery, 0);
        for (Map.Entry<String, Integer> entry : rawStatusCounts.entrySet()) {
            String label = humanizeReportStatus(entry.getKey());
            statusCounts.put(label, statusCounts.getOrDefault(label, 0) + entry.getValue());
        }
        facetCounts.put("status", statusCounts);

        VelocityReportQuery departmentFacetQuery = buildVelocityReportQuery(
                scopeAll, actorUserId, searchText, statusFilter, typeFilter,
                java.util.Collections.emptySet(), uploadedByFilter, fromDate, toDate);
        facetCounts.put("department", runVelocityReportFacetGroup(
                "SELECT COALESCE(u.txtDepartmentName, ''), COUNT(a.serApplicationId) " + VELOCITY_REPORT_JOIN
                        + departmentFacetQuery.where
                        + " GROUP BY COALESCE(u.txtDepartmentName, '') ORDER BY COUNT(a.serApplicationId) DESC",
                departmentFacetQuery, 40));

        VelocityReportQuery uploaderFacetQuery = buildVelocityReportQuery(
                scopeAll, actorUserId, searchText, statusFilter, typeFilter,
                departmentFilter, java.util.Collections.emptySet(), fromDate, toDate);
        facetCounts.put("uploadedBy", runVelocityReportFacetGroup(
                "SELECT COALESCE(u.txtUserName, CONCAT('User ', a.serSubmittedBy)), COUNT(a.serApplicationId) "
                        + VELOCITY_REPORT_JOIN + uploaderFacetQuery.where
                        + " GROUP BY COALESCE(u.txtUserName, CONCAT('User ', a.serSubmittedBy)) "
                        + "ORDER BY COUNT(a.serApplicationId) DESC",
                uploaderFacetQuery, 25));
        return facetCounts;
    }

    private Map<String, Integer> runVelocityReportFacetGroup(String jpql,
                                                             VelocityReportQuery reportQuery,
                                                             int maxEntries) {
        javax.persistence.Query query = entityManager.createQuery(jpql);
        bindVelocityReportParams(query, reportQuery);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] == null ? "" : String.valueOf(row[0]).trim();
            if (key.isEmpty()) {
                continue;
            }
            Integer count = toInteger(row[1]);
            counts.put(key, count == null ? 0 : count);
            if (maxEntries > 0 && counts.size() >= maxEntries) {
                break;
            }
        }
        return counts;
    }

    private Set<String> splitCsvParam(String value) {
        Set<String> set = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) {
            return set;
        }
        for (String part : value.split(",")) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private Double parseDoubleParam(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeDateParam(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Integer, CfgTblUser> loadUsersByIds(Set<Integer> userIds) {
        Map<Integer, CfgTblUser> usersById = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return usersById;
        }
        @SuppressWarnings("unchecked")
        List<CfgTblUser> users = entityManager.createQuery(
                        "SELECT u FROM CfgTblUser u WHERE u.serUserId IN :ids")
                .setParameter("ids", userIds)
                .getResultList();
        for (CfgTblUser user : users) {
            if (user != null && user.getSerUserId() != null) {
                usersById.put(user.getSerUserId(), user);
            }
        }
        return usersById;
    }

    private String resolveUserDisplayName(CfgTblUser user, Integer userId) {
        if (user != null && user.getTxtUserName() != null && !user.getTxtUserName().trim().isEmpty()) {
            return user.getTxtUserName().trim();
        }
        return userId == null || userId <= 0 ? "Unknown" : ("User " + userId);
    }

    private String resolveUserDepartmentName(CfgTblUser user) {
        if (user == null) {
            return "";
        }
        String direct = user.getTxtDepartmentName();
        if (direct != null && !direct.trim().isEmpty()) {
            return direct.trim();
        }
        HrTblDepartment department = user.getHrTblDepartment();
        if (department != null && department.getTxtDepartmentName() != null) {
            return department.getTxtDepartmentName().trim();
        }
        return "";
    }

    private String formatReportDate(Object createdDate) {
        if (createdDate instanceof Timestamp) {
            return ((Timestamp) createdDate).toLocalDateTime().toLocalDate().toString();
        }
        if (createdDate == null) {
            return "";
        }
        String text = String.valueOf(createdDate).trim();
        if (text.length() >= 10) {
            return text.substring(0, 10);
        }
        return text;
    }

    private String resolveReportFilename(String formCode, Integer applicationId, String pdfName, boolean hasPdf) {
        if (pdfName != null && !pdfName.trim().isEmpty()) {
            return pdfName.trim();
        }
        String safeCode = formCode == null || formCode.trim().isEmpty()
                ? ("APP-" + applicationId)
                : formCode.trim();
        return hasPdf ? safeCode + ".pdf" : safeCode;
    }

    private String buildReportTextSnippet(String remarks, String applicationDataJson) {
        StringBuilder builder = new StringBuilder();
        if (remarks != null && !remarks.trim().isEmpty()) {
            builder.append(remarks.trim()).append(' ');
        }
        if (applicationDataJson != null && !applicationDataJson.trim().isEmpty()) {
            String flattened = applicationDataJson
                    .replaceAll("[\"{}\\[\\]:,]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            builder.append(flattened);
        }
        String snippet = builder.toString().trim();
        if (snippet.length() > 600) {
            return snippet.substring(0, 600);
        }
        return snippet;
    }

    private String buildReportListSnippet(String formCode, String templateName, String remarks) {
        StringBuilder builder = new StringBuilder();
        if (formCode != null && !formCode.trim().isEmpty()) {
            builder.append(formCode.trim());
        }
        if (templateName != null && !templateName.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(templateName.trim());
        }
        if (remarks != null && !remarks.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" — ");
            }
            builder.append(remarks.trim());
        }
        String snippet = builder.toString().trim();
        if (snippet.length() > 320) {
            return snippet.substring(0, 320) + "…";
        }
        return snippet;
    }

    private double extractReportAmount(String applicationDataJson, String remarks) {
        String combined = (applicationDataJson == null ? "" : applicationDataJson) + " " + (remarks == null ? "" : remarks);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)(?:amount|total|grand\\s*total|price|value)\\D{0,12}(\\d{1,12}(?:\\.\\d+)?)")
                .matcher(combined);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0D;
            }
        }
        return 0D;
    }

    private String humanizeReportStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Pending";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized) || "COMPLETED".equals(normalized)) {
            return "Approved";
        }
        if ("REJECTED".equals(normalized)) {
            return "Rejected";
        }
        if ("DRAFT".equals(normalized)) {
            return "Draft";
        }
        if (normalized.contains("PENDING")) {
            return "Pending";
        }
        if ("IN_PROGRESS".equals(normalized)) {
            return "In Progress";
        }
        return status.trim();
    }

    private boolean matchesReportSearchText(String haystack, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        if (haystack == null || haystack.isEmpty()) {
            return false;
        }
        String remaining = searchText;
        java.util.regex.Matcher phraseMatcher = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(remaining);
        while (phraseMatcher.find()) {
            String phrase = phraseMatcher.group(1).toLowerCase(Locale.ROOT);
            if (!phrase.isEmpty() && !haystack.contains(phrase)) {
                return false;
            }
        }
        remaining = remaining.replaceAll("\"[^\"]+\"", " ").trim();
        if (remaining.isEmpty()) {
            return true;
        }
        String[] tokens = remaining.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty() || token.startsWith("-")) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon > 0 && colon < token.length() - 1) {
                token = token.substring(colon + 1);
            }
            if (!token.isEmpty() && !haystack.contains(token.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private void sortVelocityReportItems(List<Map<String, Object>> items, String sortKey, String searchText) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if ("date-asc".equals(sortKey)) {
            items.sort((left, right) -> String.valueOf(left.getOrDefault("uploadedAt", ""))
                    .compareTo(String.valueOf(right.getOrDefault("uploadedAt", ""))));
            return;
        }
        if ("date-desc".equals(sortKey)) {
            items.sort((left, right) -> String.valueOf(right.getOrDefault("uploadedAt", ""))
                    .compareTo(String.valueOf(left.getOrDefault("uploadedAt", ""))));
            return;
        }
        if ("amount-asc".equals(sortKey)) {
            items.sort((left, right) -> Double.compare(toDouble(left.get("amount")), toDouble(right.get("amount"))));
            return;
        }
        if ("amount-desc".equals(sortKey)) {
            items.sort((left, right) -> Double.compare(toDouble(right.get("amount")), toDouble(left.get("amount"))));
            return;
        }
        if (searchText != null && !searchText.isEmpty()) {
            items.sort((left, right) -> {
                String leftName = String.valueOf(left.getOrDefault("filename", ""));
                String rightName = String.valueOf(right.getOrDefault("filename", ""));
                return leftName.compareToIgnoreCase(rightName);
            });
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0D;
        }
    }

    private Map<String, Map<String, Integer>> buildVelocityReportFacetCounts(List<Map<String, Object>> items) {
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Map<String, Integer> departmentCounts = new LinkedHashMap<>();
        Map<String, Integer> uploadedByCounts = new LinkedHashMap<>();
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            incrementFacetCount(typeCounts, String.valueOf(item.getOrDefault("type", "")));
            incrementFacetCount(departmentCounts, String.valueOf(item.getOrDefault("department", "")));
            incrementFacetCount(uploadedByCounts, String.valueOf(item.getOrDefault("uploadedBy", "")));
            incrementFacetCount(statusCounts, String.valueOf(item.getOrDefault("status", "")));
        }
        Map<String, Map<String, Integer>> facetCounts = new HashMap<>();
        facetCounts.put("type", typeCounts);
        facetCounts.put("department", departmentCounts);
        facetCounts.put("uploadedBy", uploadedByCounts);
        facetCounts.put("status", statusCounts);
        return facetCounts;
    }

    private void incrementFacetCount(Map<String, Integer> counts, String value) {
        String key = value == null ? "" : value.trim();
        if (key.isEmpty()) {
            return;
        }
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private static class TemplateWorkflowContext {
        private TemplateStep initiator;
        private final List<TemplateStep> steps = new ArrayList<>();

        private TemplateStep stepAt(int level) {
            if (steps.isEmpty()) {
                return initiatorStep();
            }
            return steps.get(Math.max(0, Math.min(level, steps.size() - 1)));
        }

        private TemplateStep initiatorStep() {
            return initiator != null ? initiator : new TemplateStep();
        }
    }

    private static class TemplateStep {
        private String id;
        private String name;
        private String type;
        private String approvalMode = "OR";
        private int level = 0;
        private final Set<Integer> approverIds = new LinkedHashSet<>();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : Integer.parseInt(text);
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }
}
