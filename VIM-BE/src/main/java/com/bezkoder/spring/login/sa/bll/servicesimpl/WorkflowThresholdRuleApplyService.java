package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.services.IWorkflowThresholdRuleApplyService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblWorkflowThresholdRule;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import com.bezkoder.spring.login.sa.dal.entities.WorkflowThresholdRuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkflowThresholdRuleApplyService implements IWorkflowThresholdRuleApplyService {

    private static final Logger logger = LogManager.getLogger(WorkflowThresholdRuleApplyService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WorkflowThresholdRuleRepository ruleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void applyActiveRulesOnSubmit(CfgTblCustomFormApplication application) {
        if (application == null || application.getSerFormId() == null) {
            return;
        }
        List<CfgTblWorkflowThresholdRule> rules = ruleRepository
                .findBySerFormIdAndBlIsDeletedFalseOrderByIntPriorityAscTxtRuleNameAsc(application.getSerFormId())
                .stream()
                .filter(this::isRuleActive)
                .collect(Collectors.toList());

        Map<String, Object> appData = parseMap(application.getTxtApplicationData());
        Map<String, Object> templatePayload = asMap(appData.get("templatePayload"));
        if (templatePayload.isEmpty()) {
            return;
        }

        boolean hasFooterSelections = hasFooterSelections(appData);
        if (rules.isEmpty() && !hasFooterSelections) {
            return;
        }

        resetPipelineFromTemplateDefinition(templatePayload, application.getSerFormId());

        List<Map<String, Object>> pipeline = deepCopyPipeline(asListOfMaps(templatePayload.get("pipeline")));
        if (pipeline.isEmpty() && !hasFooterSelections) {
            return;
        }
        ensureInitiatorStepPresent(pipeline);

        List<Integer> appliedRuleIds = new ArrayList<>();
        for (CfgTblWorkflowThresholdRule rule : rules) {
            if (!ruleMatches(rule, appData, templatePayload)) {
                continue;
            }
            if (applyRule(pipeline, rule)) {
                appliedRuleIds.add(rule.getSerWorkflowThresholdRuleId());
            }
        }

        int footerStepsAppended = appendFooterFieldsToPipeline(pipeline, appData);
        if (!appliedRuleIds.isEmpty() || footerStepsAppended > 0) {
            renumberPipelineOrders(pipeline);
        }

        if (appliedRuleIds.isEmpty() && footerStepsAppended == 0) {
            return;
        }

        templatePayload.put("pipeline", pipeline);
        appData.put("templatePayload", templatePayload);
        if (!appliedRuleIds.isEmpty()) {
            appData.put("thresholdRulesApplied", appliedRuleIds);
        }
        if (footerStepsAppended > 0) {
            appData.put("workflowPipelineMergedFooter", true);
        }
        try {
            application.setTxtApplicationData(objectMapper.writeValueAsString(appData));
        } catch (Exception ex) {
            logger.error("Failed to persist threshold rule pipeline changes: " + ex.getMessage(), ex);
            throw new IllegalStateException("Could not apply workflow threshold rules");
        }
    }

    private boolean isRuleActive(CfgTblWorkflowThresholdRule rule) {
        return rule != null && !Boolean.FALSE.equals(rule.getBlIsActive());
    }

    private void resetPipelineFromTemplateDefinition(Map<String, Object> templatePayload, Integer serFormId) {
        if (serFormId == null || serFormId <= 0) {
            return;
        }
        List<TemplateDefinition> matches = entityManager.createQuery(
                        "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId",
                        TemplateDefinition.class)
                .setParameter("formId", serFormId)
                .setMaxResults(1)
                .getResultList();
        if (matches.isEmpty()) {
            return;
        }
        Map<String, Object> definitionPayload = parseMap(matches.get(0).getTxtTemplatePayload());
        List<Map<String, Object>> basePipeline = asListOfMaps(definitionPayload.get("pipeline"));
        if (!basePipeline.isEmpty()) {
            templatePayload.put("pipeline", deepCopyPipeline(basePipeline));
        }
    }

    private boolean hasFooterSelections(Map<String, Object> appData) {
        return countFooterApproverSteps(appData) > 0;
    }

    /**
     * Appends individual footer approvers after threshold rules so routing order is:
     * template base (e.g. initiator) → rule inserts/overrides → footer-selected users.
     */
    private int appendFooterFieldsToPipeline(List<Map<String, Object>> pipeline, Map<String, Object> appData) {
        List<Map<String, Object>> footerFields = new ArrayList<>(asListOfMaps(appData.get("footerFields")));
        if (footerFields.isEmpty()) {
            return 0;
        }
        footerFields.sort(Comparator.comparingInt(section -> {
            Integer order = toInteger(section.get("order"));
            return order != null ? order : Integer.MAX_VALUE;
        }));

        int appended = 0;
        for (int sectionIndex = 0; sectionIndex < footerFields.size(); sectionIndex++) {
            Map<String, Object> section = footerFields.get(sectionIndex);
            String sectionKey = valueAsString(section.get("key"));
            if (sectionKey != null && "prepared_by".equalsIgnoreCase(sectionKey)) {
                continue;
            }
            String sectionLabel = valueAsString(section.get("label"));
            List<Map<String, Object>> users = asListOfMaps(section.get("users"));
            for (int userIndex = 0; userIndex < users.size(); userIndex++) {
                Map<String, Object> user = users.get(userIndex);
                Integer userId = toInteger(firstPresent(user.get("serUserId"), user.get("userId"), user.get("id")));
                if (userId == null || userId <= 0) {
                    continue;
                }
                String userName = valueAsString(firstPresent(user.get("txtUserName"), user.get("userName"), user.get("name")));
                String stepId = String.format(Locale.ROOT, "%s-%d-%d-%d",
                        sectionKey != null ? sectionKey : "footer",
                        sectionIndex + 1,
                        userId,
                        userIndex + 1);

                Map<String, Object> step = new LinkedHashMap<>();
                step.put("id", stepId);
                step.put("type", "individual");
                step.put("approvalMode", "OR");
                step.put("fieldPermissionsConfigured", true);
                step.put("fieldPermissions", new ArrayList<>());
                step.put("footerPipeline", true);
                step.put("name", userName != null ? userName
                        : (sectionLabel != null ? sectionLabel : "Footer approver"));
                step.put("users", Collections.singletonList(new LinkedHashMap<>(user)));
                step.put("userIds", Collections.singletonList(userId));
                pipeline.add(step);
                appended++;
            }
        }
        return appended;
    }

    private int countFooterApproverSteps(Map<String, Object> appData) {
        int count = 0;
        for (Map<String, Object> section : asListOfMaps(appData.get("footerFields"))) {
            String sectionKey = valueAsString(section.get("key"));
            if (sectionKey != null && "prepared_by".equalsIgnoreCase(sectionKey)) {
                continue;
            }
            for (Map<String, Object> user : asListOfMaps(section.get("users"))) {
                Integer userId = toInteger(firstPresent(user.get("serUserId"), user.get("userId"), user.get("id")));
                if (userId != null && userId > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private void ensureInitiatorStepPresent(List<Map<String, Object>> pipeline) {
        for (Map<String, Object> step : pipeline) {
            if ("initiator".equalsIgnoreCase(valueAsString(step.get("type")))) {
                return;
            }
        }
        Map<String, Object> initiator = new LinkedHashMap<>();
        initiator.put("id", "initiator");
        initiator.put("name", "Initiator");
        initiator.put("type", "initiator");
        initiator.put("approvalMode", "OR");
        initiator.put("fieldPermissionsConfigured", true);
        initiator.put("fieldPermissions", new ArrayList<>());
        pipeline.add(0, initiator);
    }

    private Object firstPresent(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            return value;
        }
        return null;
    }

    private boolean ruleMatches(CfgTblWorkflowThresholdRule rule, Map<String, Object> appData,
                                Map<String, Object> templatePayload) {
        String actual = resolveFieldValue(rule.getTxtFieldKey(), rule.getTxtFieldLabel(), appData, templatePayload);
        if (actual == null) {
            return false;
        }
        String expected = rule.getTxtCompareValue() != null ? rule.getTxtCompareValue().trim() : "";
        String operator = rule.getTxtOperator() != null ? rule.getTxtOperator().trim() : "";
        switch (operator) {
            case "equals":
                return compareAsString(actual, expected);
            case "not_equals":
                return !compareAsString(actual, expected);
            case "contains":
                return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
            case "greater_than":
            case "less_than":
                return compareNumeric(actual, expected, operator);
            case "in_list":
                return valueInList(actual, expected);
            default:
                return false;
        }
    }

    private boolean compareAsString(String actual, String expected) {
        return actual.trim().equalsIgnoreCase(expected.trim());
    }

    private boolean compareNumeric(String actual, String expected, String operator) {
        try {
            double a = Double.parseDouble(actual.trim());
            double b = Double.parseDouble(expected.trim());
            return "greater_than".equals(operator) ? a > b : a < b;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean valueInList(String actual, String expected) {
        String normalizedActual = actual.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(expected.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedActual::equals);
    }

    private String resolveFieldValue(String fieldKey, String fieldLabel, Map<String, Object> appData,
                                     Map<String, Object> templatePayload) {
        if (fieldKey == null || fieldKey.trim().isEmpty()) {
            return null;
        }
        String key = fieldKey.trim();
        Map<String, Object> templateValues = asMap(appData.get("templateValues"));

        String fromTemplateValues = valueAsString(templateValues.get(key));
        if (fromTemplateValues != null) {
            return fromTemplateValues;
        }
        String fromTopLevel = valueAsString(appData.get(key));
        if (fromTopLevel != null) {
            return fromTopLevel;
        }

        for (Map<String, Object> field : asListOfMaps(templatePayload.get("fields"))) {
            String id = valueAsString(field.get("id"));
            String label = valueAsString(field.get("label"));
            String slugFromLabel = slugifyFieldKey(label);
            boolean matchesKey = key.equals(id) || key.equals(slugFromLabel)
                    || (fieldLabel != null && fieldLabel.trim().equalsIgnoreCase(label != null ? label.trim() : ""));
            if (!matchesKey) {
                continue;
            }
            if (id != null) {
                String byId = valueAsString(templateValues.get(id));
                if (byId != null) {
                    return byId;
                }
                String byIdTop = valueAsString(appData.get(id));
                if (byIdTop != null) {
                    return byIdTop;
                }
            }
        }
        return null;
    }

    private String slugifyFieldKey(String label) {
        if (label == null) {
            return "";
        }
        return label.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private boolean applyRule(List<Map<String, Object>> pipeline, CfgTblWorkflowThresholdRule rule) {
        String routing = rule.getTxtRoutingAction() != null ? rule.getTxtRoutingAction().trim().toLowerCase(Locale.ROOT) : "";
        if ("insert".equals(routing)) {
            return applyInsert(pipeline, rule);
        }
        if ("override".equals(routing)) {
            return applyOverride(pipeline, rule);
        }
        return false;
    }

    private boolean applyOverride(List<Map<String, Object>> pipeline, CfgTblWorkflowThresholdRule rule) {
        int stepIndex = findStepIndexByOrder(pipeline, rule.getIntApprovalStepOrder());
        if (stepIndex < 0) {
            return false;
        }
        Map<String, Object> step = pipeline.get(stepIndex);
        if ("initiator".equalsIgnoreCase(valueAsString(step.get("type")))) {
            return false;
        }
        clearApproverFields(step);
        populateStepTarget(step, rule);
        step.put("thresholdRuleId", rule.getSerWorkflowThresholdRuleId());
        step.put("thresholdRuleOverride", true);
        return true;
    }

    private boolean applyInsert(List<Map<String, Object>> pipeline, CfgTblWorkflowThresholdRule rule) {
        int anchorIndex = findStepIndexByOrder(pipeline, rule.getIntApprovalStepOrder());
        if (anchorIndex < 0) {
            return false;
        }
        String position = rule.getTxtInsertPosition() != null ? rule.getTxtInsertPosition().trim().toLowerCase(Locale.ROOT) : "after";
        int insertIndex = "before".equals(position) ? anchorIndex : anchorIndex + 1;
        Map<String, Object> newStep = new LinkedHashMap<>();
        newStep.put("id", "threshold-" + UUID.randomUUID().toString().replace("-", ""));
        newStep.put("name", buildInsertedStepName(rule));
        newStep.put("approvalMode", "OR");
        newStep.put("fieldPermissionsConfigured", true);
        newStep.put("fieldPermissions", new ArrayList<>());
        newStep.put("thresholdRuleId", rule.getSerWorkflowThresholdRuleId());
        newStep.put("thresholdRuleInsert", true);
        populateStepTarget(newStep, rule);
        pipeline.add(insertIndex, newStep);
        renumberPipelineOrders(pipeline);
        return true;
    }

    private String buildInsertedStepName(CfgTblWorkflowThresholdRule rule) {
        if (rule.getTxtTargetLabel() != null && !rule.getTxtTargetLabel().trim().isEmpty()) {
            return rule.getTxtTargetLabel().trim();
        }
        if (rule.getTxtRuleName() != null && !rule.getTxtRuleName().trim().isEmpty()) {
            return rule.getTxtRuleName().trim();
        }
        return "Threshold approval";
    }

    private void clearApproverFields(Map<String, Object> step) {
        step.remove("users");
        step.remove("userIds");
        step.remove("serUserId");
        step.remove("userId");
        step.remove("serDepartmentId");
        step.remove("departmentId");
        step.remove("hrTblDepartment");
        step.remove("serRoleId");
        step.remove("cfgTblRole");
        step.remove("dynamicTarget");
        step.remove("dynamicTargets");
        step.remove("txtUserName");
        step.remove("roleName");
        step.remove("departmentName");
        step.remove("txtDepartmentName");
    }

    private void populateStepTarget(Map<String, Object> step, CfgTblWorkflowThresholdRule rule) {
        String targetType = rule.getTxtTargetType() != null ? rule.getTxtTargetType().trim().toLowerCase(Locale.ROOT) : "";
        Integer targetId = rule.getSerTargetId();
        String targetLabel = rule.getTxtTargetLabel() != null ? rule.getTxtTargetLabel().trim() : "";

        if ("department".equals(targetType) && targetId != null) {
            step.put("type", "department");
            step.put("serDepartmentId", targetId);
            HrTblDepartment department = entityManager.find(HrTblDepartment.class, targetId);
            if (department != null) {
                Map<String, Object> dept = new LinkedHashMap<>();
                dept.put("serDepartmentId", department.getSerDepartmentId());
                dept.put("txtDepartmentName", department.getTxtDepartmentName());
                step.put("hrTblDepartment", dept);
                if (targetLabel.isEmpty() && department.getTxtDepartmentName() != null) {
                    targetLabel = department.getTxtDepartmentName();
                }
            }
            if (!targetLabel.isEmpty()) {
                step.put("name", targetLabel);
            }
            return;
        }

        if ("role".equals(targetType) && targetId != null) {
            step.put("type", "role");
            step.put("serRoleId", targetId);
            CfgTblRole role = entityManager.find(CfgTblRole.class, targetId);
            if (role != null) {
                Map<String, Object> roleMap = new LinkedHashMap<>();
                roleMap.put("serRoleId", role.getSerRoleId());
                roleMap.put("txtRoleName", role.getTxtRoleName());
                step.put("cfgTblRole", roleMap);
                if (targetLabel.isEmpty() && role.getTxtRoleName() != null) {
                    targetLabel = role.getTxtRoleName();
                }
            }
            if (!targetLabel.isEmpty()) {
                step.put("name", targetLabel);
            }
            return;
        }

        if ("individual".equals(targetType) && targetId != null) {
            step.put("type", "individual");
            CfgTblUser user = entityManager.find(CfgTblUser.class, targetId);
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("serUserId", targetId);
            if (user != null) {
                userMap.put("txtUserName", user.getTxtUserName());
                if (targetLabel.isEmpty() && user.getTxtUserName() != null) {
                    targetLabel = user.getTxtUserName();
                }
            }
            List<Map<String, Object>> users = new ArrayList<>();
            users.add(userMap);
            step.put("users", users);
            step.put("userIds", Arrays.asList(targetId));
            if (!targetLabel.isEmpty()) {
                step.put("name", targetLabel);
            }
        }
    }

    private int findStepIndexByOrder(List<Map<String, Object>> pipeline, Integer targetOrder) {
        if (targetOrder == null || targetOrder < 1) {
            return -1;
        }
        for (int i = 0; i < pipeline.size(); i++) {
            int order = resolveStepOrder(pipeline.get(i), i);
            if (order == targetOrder) {
                return i;
            }
        }
        return -1;
    }

    private int resolveStepOrder(Map<String, Object> step, int index) {
        Integer explicit = toInteger(step.get("order"));
        if (explicit != null && explicit > 0) {
            return explicit;
        }
        return index + 1;
    }

    private void renumberPipelineOrders(List<Map<String, Object>> pipeline) {
        for (int i = 0; i < pipeline.size(); i++) {
            pipeline.get(i).put("order", i + 1);
        }
    }

    private List<Map<String, Object>> deepCopyPipeline(List<Map<String, Object>> pipeline) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(pipeline),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            logger.warn("Pipeline deep copy failed, using shallow list: " + ex.getMessage());
            return new ArrayList<>(pipeline);
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
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

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : text;
        }
        return String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
