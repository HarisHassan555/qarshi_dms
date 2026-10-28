package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.WorkflowThresholdRuleDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowThresholdRuleSaveRequest;
import com.bezkoder.spring.login.sa.bll.services.IWorkflowThresholdRuleService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblWorkflowThresholdRule;
import com.bezkoder.spring.login.sa.dal.entities.WorkflowThresholdRuleRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkflowThresholdRuleService implements IWorkflowThresholdRuleService {

    private static final Logger logger = LogManager.getLogger(WorkflowThresholdRuleService.class);

    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            "equals", "not_equals", "greater_than", "less_than", "contains", "in_list"));
    private static final Set<String> TARGET_TYPES = new HashSet<>(Arrays.asList("department", "role", "individual"));
    private static final Set<String> ROUTING_ACTIONS = new HashSet<>(Arrays.asList("override", "insert"));
    private static final Set<String> INSERT_POSITIONS = new HashSet<>(Arrays.asList("before", "after"));

    @Autowired
    private WorkflowThresholdRuleRepository repository;

    @Override
    public List<WorkflowThresholdRuleDto> getAll(Integer serFormId) {
        List<CfgTblWorkflowThresholdRule> rows;
        if (serFormId != null && serFormId > 0) {
            rows = repository.findBySerFormIdAndBlIsDeletedFalseOrderByIntPriorityAscTxtRuleNameAsc(serFormId);
        } else {
            rows = repository.findByBlIsDeletedFalseOrderByIntPriorityAscSerFormIdAscTxtRuleNameAsc();
        }
        List<WorkflowThresholdRuleDto> result = new ArrayList<>();
        for (CfgTblWorkflowThresholdRule row : rows) {
            result.add(toDto(row));
        }
        return result;
    }

    @Override
    public WorkflowThresholdRuleDto getById(int id) {
        return toDto(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public WorkflowThresholdRuleDto create(WorkflowThresholdRuleSaveRequest request, int userId) {
        validateSaveRequest(request, null);
        CfgTblWorkflowThresholdRule row = new CfgTblWorkflowThresholdRule();
        applySaveRequest(row, request, userId, true);
        row.setSerCreatedUserId(userId > 0 ? userId : null);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        repository.save(row);
        return toDto(row);
    }

    @Override
    @Transactional
    public WorkflowThresholdRuleDto update(WorkflowThresholdRuleSaveRequest request, int userId) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("id is required for update");
        }
        CfgTblWorkflowThresholdRule row = findActiveOrThrow(request.getId());
        validateSaveRequest(request, row.getSerWorkflowThresholdRuleId());
        applySaveRequest(row, request, userId, false);
        repository.save(row);
        return toDto(row);
    }

    @Override
    @Transactional
    public String deleteByIds(List<Integer> ids, int userId) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No rule id(s) provided");
        }
        int deleted = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                continue;
            }
            Optional<CfgTblWorkflowThresholdRule> optional =
                    repository.findBySerWorkflowThresholdRuleIdAndBlIsDeletedFalse(id);
            if (!optional.isPresent()) {
                continue;
            }
            CfgTblWorkflowThresholdRule row = optional.get();
            row.setBlIsDeleted(true);
            row.setBlIsActive(false);
            row.setDteModifiedDate(now);
            if (userId > 0) {
                row.setSerModifiedUserId(userId);
            }
            repository.save(row);
            deleted++;
        }
        if (deleted == 0) {
            return "{\"status\":\"Failure\",\"message\":\"No rules deleted\"}";
        }
        return "{\"status\":\"Success\"}";
    }

    private CfgTblWorkflowThresholdRule findActiveOrThrow(int id) {
        return repository.findBySerWorkflowThresholdRuleIdAndBlIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
    }

    private void validateSaveRequest(WorkflowThresholdRuleSaveRequest request, Integer existingId) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (isBlank(request.getRuleName())) {
            throw new IllegalArgumentException("ruleName is required");
        }
        if (request.getSerFormId() == null || request.getSerFormId() <= 0) {
            throw new IllegalArgumentException("serFormId is required");
        }
        if (isBlank(request.getFieldKey())) {
            throw new IllegalArgumentException("fieldKey is required");
        }
        if (isBlank(request.getOperator()) || !OPERATORS.contains(request.getOperator().trim())) {
            throw new IllegalArgumentException("Invalid operator");
        }
        if (request.getValue() == null) {
            throw new IllegalArgumentException("value is required");
        }
        String routing = normalizeRoutingAction(request.getRoutingAction());
        if (!ROUTING_ACTIONS.contains(routing)) {
            throw new IllegalArgumentException("Invalid routingAction");
        }
        if ("insert".equals(routing)) {
            String pos = normalizeInsertPosition(request.getInsertPosition());
            if (!INSERT_POSITIONS.contains(pos)) {
                throw new IllegalArgumentException("insertPosition must be before or after");
            }
        }
        if (request.getApprovalStepOrder() == null || request.getApprovalStepOrder() < 1) {
            throw new IllegalArgumentException("approvalStepOrder must be >= 1");
        }
        if (isBlank(request.getTargetType()) || !TARGET_TYPES.contains(request.getTargetType().trim())) {
            throw new IllegalArgumentException("Invalid targetType");
        }
        if (request.getSerTargetId() == null || request.getSerTargetId() <= 0) {
            throw new IllegalArgumentException("serTargetId is required");
        }
        if (isBlank(request.getTargetLabel())) {
            throw new IllegalArgumentException("targetLabel is required");
        }
        if (request.getPriority() == null || request.getPriority() < 0) {
            throw new IllegalArgumentException("priority is required");
        }
        if (existingId != null) {
            logger.debug("Updating threshold rule id=" + existingId);
        }
    }

    private void applySaveRequest(CfgTblWorkflowThresholdRule row, WorkflowThresholdRuleSaveRequest request, int userId, boolean isCreate) {
        row.setSerFormId(request.getSerFormId());
        row.setTxtRuleName(request.getRuleName().trim());
        row.setTxtFieldKey(request.getFieldKey().trim());
        row.setTxtFieldLabel(trimToNull(request.getFieldLabel()));
        row.setTxtOperator(request.getOperator().trim());
        row.setTxtCompareValue(request.getValue().trim());
        String routing = normalizeRoutingAction(request.getRoutingAction());
        row.setTxtRoutingAction(routing);
        if ("insert".equals(routing)) {
            row.setTxtInsertPosition(normalizeInsertPosition(request.getInsertPosition()));
        } else {
            row.setTxtInsertPosition(null);
        }
        row.setIntApprovalStepOrder(request.getApprovalStepOrder());
        row.setTxtTargetType(request.getTargetType().trim());
        row.setSerTargetId(request.getSerTargetId());
        row.setTxtTargetLabel(request.getTargetLabel().trim());
        row.setIntPriority(request.getPriority());
        row.setTxtNotes(trimToNull(request.getNotes()));
        row.setBlIsActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        row.setBlIsDeleted(false);
        row.setBlnStatus(true);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        if (userId > 0) {
            row.setSerModifiedUserId(userId);
        }
        if (isCreate && row.getBlIsActive() == null) {
            row.setBlIsActive(true);
        }
    }

    private WorkflowThresholdRuleDto toDto(CfgTblWorkflowThresholdRule row) {
        WorkflowThresholdRuleDto dto = new WorkflowThresholdRuleDto();
        dto.setId(row.getSerWorkflowThresholdRuleId());
        dto.setRuleName(row.getTxtRuleName());
        dto.setSerFormId(row.getSerFormId());
        dto.setFieldKey(row.getTxtFieldKey());
        dto.setFieldLabel(row.getTxtFieldLabel());
        dto.setOperator(row.getTxtOperator());
        dto.setValue(row.getTxtCompareValue());
        dto.setRoutingAction(row.getTxtRoutingAction());
        dto.setInsertPosition(row.getTxtInsertPosition());
        dto.setApprovalStepOrder(row.getIntApprovalStepOrder());
        dto.setTargetType(row.getTxtTargetType());
        dto.setSerTargetId(row.getSerTargetId());
        dto.setTargetLabel(row.getTxtTargetLabel());
        dto.setPriority(row.getIntPriority());
        dto.setActive(Boolean.TRUE.equals(row.getBlIsActive()));
        dto.setNotes(row.getTxtNotes());
        return dto;
    }

    private static String normalizeRoutingAction(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "override";
        }
        return value.trim().toLowerCase();
    }

    private static String normalizeInsertPosition(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "after";
        }
        return value.trim().toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
