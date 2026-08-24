package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.utility.common.RequestMetadataUtil;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
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
            appendSubmissionHistory(application);
            entityManager.persist(application);
            logTemplateAction("FORM_SUBMIT", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application submitted successfully",
                    buildTemplateApplicationPayload(application, "FORM_SUBMIT", null), null);

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
            result.put("txtApplicationData", application.getTxtApplicationData());
            result.put("txtStatus", application.getTxtStatus());
            result.put("intCurrentApprovalLevel", application.getIntCurrentApprovalLevel());
            result.put("serSubmittedBy", application.getSerSubmittedBy());
            result.put("serCurrentApprover", application.getSerCurrentApprover());
            result.put("txtRemarks", application.getTxtRemarks());
            result.put("txtApprovalHistory", application.getTxtApprovalHistory());
            result.put("txtPriorApprovals", application.getTxtPriorApprovals());
            result.put("currentApproverIds", new ArrayList<>(pendingApproverIdsForCurrentStep(application)));
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
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            int currentLevel = safeLevel(application);
            TemplateStep currentStep = context.stepAt(currentLevel);
            if (!isAuthorizedForTemplateStep(currentStep, actorId)) {
                throw new IllegalArgumentException("You are not authorized to send back this template application");
            }
            int targetLevel = Math.max(0, currentLevel - 1);
            TemplateStep targetStep = context.stepAt(targetLevel);
            appendTemplateHistory(application, currentStep, actorId, "SENT_BACK", remarks, displayLevelForIndex(targetLevel));
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(targetLevel));
            application.setSerCurrentApprover(firstApproverId(targetStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("SEND_BACK", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent back successfully",
                    buildTemplateApplicationPayload(application, "SEND_BACK", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(), "template send-back notification", true);
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
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            if (!isAuthorizedForTemplateStep(currentStep, actorId)) {
                throw new IllegalArgumentException("You are not authorized to send back this template application");
            }
            appendTemplateHistory(application, currentStep, actorId, "SENT_BACK_TO_INITIATOR", remarks, 1);
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("PENDING");
            application.setIntCurrentApprovalLevel(1);
            application.setSerCurrentApprover(application.getSerSubmittedBy());
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            entityManager.flush();
            logTemplateAction("SEND_BACK", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent back to initiator successfully",
                    buildTemplateApplicationPayload(application, "SEND_BACK_TO_INITIATOR", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(),
                    "template send-back-to-initiator notification", true);
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
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep firstStep = context.stepAt(0);
            appendTemplateHistory(application, context.initiatorStep(), actorId, "RESUBMITTED_BY_INITIATOR", remarks, 1);
            application.setTxtApprovalHistory(writeJson(activeTemplateHistory(parseHistory(application.getTxtApprovalHistory()))));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(displayLevelForIndex(0));
            application.setSerCurrentApprover(firstApproverId(firstStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            logTemplateAction("RESUBMIT_INITIATOR", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application resubmitted by initiator",
                    buildTemplateApplicationPayload(application, "RESUBMITTED_BY_INITIATOR", remarks), null);
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
            if (!isAuthorizedForTemplateStep(currentStep, actorId)) {
                throw new IllegalArgumentException("You are not authorized to request opinion for this template application");
            }
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
            entityManager.merge(application);
            logTemplateAction("REQUEST_OPINION", request, application.getSerApplicationId(), "SUCCESS",
                    "Template application sent for opinion successfully",
                    buildTemplateApplicationPayload(application, "OPINION_REQUESTED", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(),
                    "template opinion request notification");
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
            entityManager.merge(application);
            logTemplateAction("SUBMIT_OPINION", request, application.getSerApplicationId(), "SUCCESS",
                    "Template opinion submitted successfully",
                    buildTemplateApplicationPayload(application,
                            "reject".equalsIgnoreCase(action) ? "OPINION_REJECTED" : "OPINION_APPROVED", remarks), null);
            queueTemplatePostApprovalEmails(application.getSerApplicationId(),
                    "template opinion submission notification");
            return success("Opinion submitted successfully");
        } catch (Exception ex) {
            logTemplateAction("SUBMIT_OPINION", request, toInteger(requestBody.get("applicationId")), "FAILURE",
                    ex.getMessage(), buildTemplateRequestPayload(requestBody, "SUBMIT_OPINION"), ex.getMessage());
            return failure(result, response, "Error submitting template opinion", ex);
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
                                                              HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int safePage = page == null || page < 0 ? 0 : page;
            int safePageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
            String searchText = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

            List<TemplateDefinition> definitions = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ORDER BY t.dteCreatedDate DESC",
                    TemplateDefinition.class)
                    .getResultList();

            List<Map<String, Object>> summaries = new ArrayList<>();
            for (TemplateDefinition definition : filterVisibleTemplateDefinitions(definitions, userId)) {
                Map<String, Object> summary = toTemplateDefinitionSummary(definition);
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
                return sanitizeTemplateDefinitionResponse(definition);
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (Exception ex) {
            logger.error("Error fetching template definition: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
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
                        CfgTblCustomFormApplication fullApplication = entityManager.find(
                                CfgTblCustomFormApplication.class,
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

    private TemplateDefinition sanitizeTemplateDefinitionResponse(TemplateDefinition definition) {
        if (definition == null) {
            return null;
        }
        TemplateDefinition copy = new TemplateDefinition();
        copy.setSerTemplateId(definition.getSerTemplateId());
        copy.setSerFormId(definition.getSerFormId());
        copy.setTxtTemplateName(definition.getTxtTemplateName());
        copy.setTxtCodeConvention(definition.getTxtCodeConvention());
        copy.setTxtTemplatePayload(sanitizeTemplatePayloadJson(definition.getTxtTemplatePayload()));
        copy.setBlIsActive(definition.getBlIsActive());
        copy.setBlIsDeleted(definition.getBlIsDeleted());
        copy.setBlnStatus(definition.getBlnStatus());
        copy.setDteCreatedDate(definition.getDteCreatedDate());
        copy.setDteModifiedDate(definition.getDteModifiedDate());
        copy.setSerCreatedUser(definition.getSerCreatedUser());
        copy.setSerModifiedUser(definition.getSerModifiedUser());
        return copy;
    }

    private Map<String, Object> toTemplateDefinitionSummary(TemplateDefinition definition) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Set<Integer> visibleUserIds = extractVisibleUserIds(definition);
        boolean isActive = isTemplateDefinitionActive(definition);

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
                Boolean.FALSE.equals(summary.get("isActive")) ? "inactive" : "active")
                .toLowerCase(Locale.ROOT);
        return haystack.contains(searchText);
    }

    private String sanitizeTemplatePayloadJson(String rawPayload) {
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            return rawPayload;
        }
        try {
            Map<String, Object> payload = parseMap(rawPayload);
            return writeJson(stripAttachmentPayloads(payload, new ArrayList<>()));
        } catch (Exception ex) {
            logger.warn("Failed to sanitize template payload response: " + ex.getMessage());
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
        if (!isTemplateDefinitionActive(definition)) {
            return false;
        }
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
        if (Boolean.FALSE.equals(definition.getBlIsActive()) || Boolean.FALSE.equals(definition.getBlnStatus())) {
            return false;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        return isTemplatePayloadMarkedActive(payload);
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
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            if (!isAuthorizedForTemplateStep(currentStep, actorId)) {
                throw new IllegalArgumentException("You are not authorized to act on this template application");
            }

            appendTemplateHistory(application, currentStep, actorId, action, remarks, displayLevelForIndex(safeLevel(application)));
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
            touchTemplateApplication(application, actorId);
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
        Runnable sendEmails = () -> {
            try {
                if (refreshPdfBeforeEmail) {
                    String refreshStatus = customFormApplicationService.refreshTemplateApplicationPdfFromStage0(applicationId);
                    if (!"Success".equalsIgnoreCase(refreshStatus)) {
                        logger.warn("Failed to refresh template PDF before " + contextLabel + " for applicationId="
                                + applicationId + ": " + refreshStatus);
                    }
                }
                customFormApplicationService.sendTemplatePostApprovalEmails(applicationId);
            } catch (Exception emailEx) {
                logger.warn("Failed to send " + contextLabel + " for applicationId="
                        + applicationId + ": " + emailEx.getMessage(), emailEx);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmails.run();
                }
            });
            return;
        }
        sendEmails.run();
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
    }

    private TemplateWorkflowContext buildTemplateWorkflowContext(CfgTblCustomFormApplication application) {
        return buildTemplateWorkflowContext(application, null);
    }

    private TemplateWorkflowContext buildTemplateWorkflowContext(CfgTblCustomFormApplication application,
                                                                String templatePayloadJson) {
        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        List<TemplateStep> footerSteps = buildFooterWorkflowSteps(appData, application);
        if (!footerSteps.isEmpty()) {
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
        TemplateDefinition definition = findByFormId(application.getSerFormId());
        Map<String, Object> templatePayload = asMap(appData.get("templatePayload"));
        if (templatePayload.isEmpty() && templatePayloadJson != null && !templatePayloadJson.trim().isEmpty()) {
            templatePayload = parseMap(templatePayloadJson);
        }
        if (templatePayload.isEmpty() && definition != null) {
            templatePayload = parseMap(definition.getTxtTemplatePayload());
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
        return item;
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

    private void appendTemplateHistory(CfgTblCustomFormApplication application, TemplateStep step, Integer actorId,
                                       String action, String remarks, Integer targetLevel) {
        CfgTblUser user = actorId != null ? entityManager.find(CfgTblUser.class, actorId) : null;
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
        entry.put("approvedBy", actorId);
        entry.put("userId", actorId);
        entry.put("approverName", user != null ? user.getTxtUserName() : "");
        entry.put("signaturePath", user != null ? user.getTxtSignaturePath() : "");
        entry.put("txtDepartmentName", user != null ? user.getTxtDepartmentName() : "");
        entry.put("userDepartmentName", user != null ? user.getTxtDepartmentName() : "");
        entry.put("designation", user != null ? user.getTxtDesignation() : "");
        entry.put("txtDesignation", user != null ? user.getTxtDesignation() : "");
        entry.put("approvedDate", now().toString());

        List<Map<String, Object>> history = parseHistory(application.getTxtApprovalHistory());
        history.add(entry);
        application.setTxtApprovalHistory(writeJson(history));
        List<Map<String, Object>> prior = parseHistory(application.getTxtPriorApprovals());
        prior.add(new HashMap<>(entry));
        application.setTxtPriorApprovals(writeJson(prior));
    }

    private void appendSubmissionHistory(CfgTblCustomFormApplication application) {
        Integer submittedBy = application.getSerSubmittedBy();
        CfgTblUser user = submittedBy != null ? entityManager.find(CfgTblUser.class, submittedBy) : null;
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
        entry.put("remarks", "Submitted application");
        entry.put("approvedBy", submittedBy);
        entry.put("userId", submittedBy);
        entry.put("approverName", user != null ? user.getTxtUserName() : "Initiator");
        entry.put("signaturePath", user != null ? user.getTxtSignaturePath() : "");
        entry.put("txtDepartmentName", user != null ? user.getTxtDepartmentName() : "");
        entry.put("userDepartmentName", user != null ? user.getTxtDepartmentName() : "");
        entry.put("designation", user != null ? user.getTxtDesignation() : "");
        entry.put("txtDesignation", user != null ? user.getTxtDesignation() : "");
        entry.put("approvedDate", application.getDteCreatedDate() != null ? application.getDteCreatedDate().toString() : now().toString());

        List<Map<String, Object>> history = parseHistory(application.getTxtApprovalHistory());
        history.add(entry);
        application.setTxtApprovalHistory(writeJson(history));
        List<Map<String, Object>> prior = parseHistory(application.getTxtPriorApprovals());
        prior.add(new HashMap<>(entry));
        application.setTxtPriorApprovals(writeJson(prior));
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
