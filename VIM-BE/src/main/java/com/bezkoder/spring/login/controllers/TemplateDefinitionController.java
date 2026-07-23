package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    @Transactional
    @RequestMapping(value = "/approveTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> approveTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                          HttpServletResponse response) {
        return handleTemplateDecision(requestBody, "APPROVED", response);
    }

    @Transactional
    @RequestMapping(value = "/rejectTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> rejectTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                         HttpServletResponse response) {
        return handleTemplateDecision(requestBody, "REJECTED", response);
    }

    @Transactional
    @RequestMapping(value = "/sendBackTemplateApplication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackTemplateApplication(@RequestBody Map<String, Object> requestBody,
                                                           HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
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
            appendTemplateHistory(application, currentStep, actorId, "SENT_BACK", remarks, targetLevel + 1);
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(targetLevel);
            application.setSerCurrentApprover(firstApproverId(targetStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("Application sent back successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error sending back template application", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/sendBackTemplateApplicationToInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sendBackTemplateApplicationToInitiator(@RequestBody Map<String, Object> requestBody,
                                                                      HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep currentStep = context.stepAt(safeLevel(application));
            if (!isAuthorizedForTemplateStep(currentStep, actorId)) {
                throw new IllegalArgumentException("You are not authorized to send back this template application");
            }
            appendTemplateHistory(application, currentStep, actorId, "SENT_BACK_TO_INITIATOR", remarks, 1);
            application.setTxtStatus("PENDING");
            application.setIntCurrentApprovalLevel(0);
            application.setSerCurrentApprover(application.getSerSubmittedBy());
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("Application sent back to initiator successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error sending back template application to initiator", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/resubmitTemplateApplicationFromInitiator", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> resubmitTemplateApplicationFromInitiator(@RequestBody Map<String, Object> requestBody,
                                                                        HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
            Integer actorId = toInteger(requestBody.get("userId"));
            String remarks = valueAsString(requestBody.get("remarks"));
            if (application.getSerSubmittedBy() == null || !application.getSerSubmittedBy().equals(actorId)) {
                throw new IllegalArgumentException("Only the initiator can resubmit this template application");
            }
            TemplateWorkflowContext context = buildTemplateWorkflowContext(application);
            TemplateStep firstStep = context.stepAt(0);
            appendTemplateHistory(application, context.initiatorStep(), actorId, "RESUBMITTED_BY_INITIATOR", remarks, 1);
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(0);
            application.setSerCurrentApprover(firstApproverId(firstStep));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("Application resubmitted successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error resubmitting template application", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/requestTemplateApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> requestTemplateApplicationOpinion(@RequestBody Map<String, Object> requestBody,
                                                                 HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
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
            Map<String, Object> appData = parseMap(application.getTxtApplicationData());
            Map<String, Object> opinion = new HashMap<>();
            opinion.put("active", true);
            opinion.put("requestedBy", actorId);
            opinion.put("requestedFrom", opinionUserId);
            opinion.put("returnLevel", safeLevel(application));
            opinion.put("remarks", remarks);
            opinion.put("requestedDate", now().toString());
            appData.put("templateOpinionRequest", opinion);
            application.setTxtApplicationData(writeJson(appData));
            appendTemplateHistory(application, currentStep, actorId, "OPINION_REQUESTED", remarks, safeLevel(application) + 1);
            application.setTxtStatus("OPINION_PENDING");
            application.setSerCurrentApprover(opinionUserId);
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("Application sent for opinion successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error requesting template opinion", ex);
        }
    }

    @Transactional
    @RequestMapping(value = "/submitTemplateApplicationOpinion", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> submitTemplateApplicationOpinion(@RequestBody Map<String, Object> requestBody,
                                                                HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            CfgTblCustomFormApplication application = loadTemplateApplication(toInteger(requestBody.get("applicationId")));
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
                    "reject".equalsIgnoreCase(action) ? "OPINION_REJECTED" : "OPINION_APPROVED", remarks, returnLevel + 1);
            opinion.put("active", false);
            opinion.put("action", action);
            opinion.put("completedDate", now().toString());
            appData.put("templateOpinionRequest", opinion);
            application.setTxtApplicationData(writeJson(appData));
            application.setTxtStatus("IN_PROGRESS");
            application.setIntCurrentApprovalLevel(returnLevel);
            application.setSerCurrentApprover(toInteger(opinion.get("requestedBy")));
            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("Opinion submitted successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error submitting template opinion", ex);
        }
    }

    @RequestMapping(value = "/getAllTemplateDefinitions", method = RequestMethod.GET)
    public List<TemplateDefinition> getAllTemplateDefinitions(HttpServletResponse response) {
        try {
            return entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ORDER BY t.dteCreatedDate DESC",
                    TemplateDefinition.class)
                    .getResultList();
        } catch (Exception ex) {
            logger.error("Error fetching template definitions: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getTemplateDefinitionByFormId", method = RequestMethod.GET)
    public TemplateDefinition getTemplateDefinitionByFormId(@RequestParam Integer formId, HttpServletResponse response) {
        try {
            List<TemplateDefinition> matches = entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false)",
                    TemplateDefinition.class)
                    .setParameter("formId", formId)
                    .setMaxResults(1)
                    .getResultList();
            return matches.isEmpty() ? null : matches.get(0);
        } catch (Exception ex) {
            logger.error("Error fetching template definition: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getMyTemplateApplications", method = RequestMethod.GET)
    public Map<String, Object> getMyTemplateApplications(@RequestParam Integer userId,
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

            String whereClause = "WHERE a.serSubmittedBy = :userId " +
                    "AND (a.blIsDeleted IS NULL OR a.blIsDeleted = false) " +
                    "AND (t.blIsDeleted IS NULL OR t.blIsDeleted = false) ";
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
            countQuery.setParameter("userId", userId);
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
            rowQuery.setParameter("userId", userId);
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
            if (!Boolean.TRUE.equals(all)) {
                whereClause += "AND a.serCurrentApprover = :userId ";
            }
            if (hasSearch) {
                whereClause += "AND (LOWER(COALESCE(a.txtFormCode, '')) LIKE :search " +
                        "OR LOWER(COALESCE(a.txtStatus, '')) LIKE :search " +
                        "OR LOWER(COALESCE(t.txtTemplateName, '')) LIKE :search) ";
            }

            javax.persistence.Query countQuery = entityManager.createQuery(
                    "SELECT COUNT(a.serApplicationId) " +
                            "FROM CfgTblCustomFormApplication a, TemplateDefinition t " +
                            whereClause);
            countQuery.setParameter("pendingStatuses", pendingStatuses);
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
                            "ORDER BY a.dteCreatedDate DESC");
            rowQuery.setParameter("pendingStatuses", pendingStatuses);
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
                if (Boolean.TRUE.equals(all) || !hasUserApprovedTemplateLevel(toInteger(row[0]), userId, toInteger(row[4]))) {
                    items.add(item);
                }
            }

            result.put("items", items);
            result.put("total", total == null ? 0 : total);
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

            definition.setTxtTemplateName(valueAsString(requestBody.get("txtTemplateName")));
            definition.setTxtCodeConvention(valueAsString(requestBody.get("txtCodeConvention")));
            definition.setTxtTemplatePayload(payload);
            definition.setBlIsActive(true);
            definition.setBlIsDeleted(false);
            definition.setBlnStatus(true);
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

    private Map<String, Object> handleTemplateDecision(Map<String, Object> requestBody, String action,
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

            appendTemplateHistory(application, currentStep, actorId, action, remarks, safeLevel(application) + 1);
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
                    application.setIntCurrentApprovalLevel(nextLevel);
                    application.setSerCurrentApprover(firstApproverId(nextStep));
                }
            } else {
                application.setTxtStatus("IN_PROGRESS");
                application.setSerCurrentApprover(nextPendingApproverId(application, currentStep));
            }

            application.setTxtRemarks(remarks);
            touchTemplateApplication(application, actorId);
            entityManager.merge(application);
            return success("REJECTED".equals(action) ? "Application rejected successfully" : "Application approved successfully");
        } catch (Exception ex) {
            return failure(result, response, "Error completing template action", ex);
        }
    }

    private boolean hasUserApprovedTemplateLevel(Integer applicationId, Integer userId, Integer level) {
        if (applicationId == null || userId == null) {
            return false;
        }
        CfgTblCustomFormApplication application = entityManager.find(CfgTblCustomFormApplication.class, applicationId);
        if (application == null) {
            return false;
        }
        int displayLevel = (level == null ? 0 : Math.max(0, level)) + 2;
        for (Map<String, Object> entry : parseHistory(application.getTxtApprovalHistory())) {
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

    private TemplateWorkflowContext buildTemplateWorkflowContext(CfgTblCustomFormApplication application) {
        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        TemplateDefinition definition = findByFormId(application.getSerFormId());
        Map<String, Object> templatePayload = asMap(appData.get("templatePayload"));
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
        for (Map<String, Object> entry : parseHistory(application.getTxtApprovalHistory())) {
            if (!"APPROVED".equalsIgnoreCase(valueAsString(entry.get("action")))) {
                continue;
            }
            Integer level = toInteger(firstObject(entry.get("intApprovalOrder"), entry.get("level")));
            Integer userId = toInteger(firstObject(entry.get("approvedBy"), entry.get("userId")));
            String entryStepId = firstText(entry.get("stepId"), entry.get("pipelineStepId"), entry.get("signatureTargetId"));
            int displayLevel = step.level + 2;
            boolean stepMatches = step.id != null && !step.id.isBlank()
                    ? step.id.equals(entryStepId) || (level != null && (level == displayLevel || level == step.level + 1 || level == step.level))
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
        return application.getIntCurrentApprovalLevel() == null ? 0 : Math.max(0, application.getIntCurrentApprovalLevel());
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
