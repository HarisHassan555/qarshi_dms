package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDetailDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineSaveRequest;
import com.bezkoder.spring.login.sa.bll.services.IWorkflowPipelineService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblWorkflowPipeline;
import com.bezkoder.spring.login.sa.dal.entities.WorkflowPipelineRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkflowPipelineService implements IWorkflowPipelineService {

    private static final Logger logger = LogManager.getLogger(WorkflowPipelineService.class);
    private static final String DEFAULT_PIPELINE_TYPE = "STANDARD";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private WorkflowPipelineRepository workflowPipelineRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<WorkflowPipelineDto> getAll() {
        List<WorkflowPipelineDto> result = new ArrayList<>();
        for (CfgTblWorkflowPipeline row : workflowPipelineRepository.findByBlIsDeletedFalseOrderByDteModifiedDateDescTxtNameAsc()) {
            result.add(toListDto(row));
        }
        return result;
    }

    @Override
    public WorkflowPipelineDetailDto getById(int id) {
        CfgTblWorkflowPipeline row = findActiveOrThrow(id);
        return toDetailDto(row);
    }

    @Override
    @Transactional
    public WorkflowPipelineDetailDto create(WorkflowPipelineSaveRequest request, int userId) {
        validateSaveRequest(request, null);
        CfgTblWorkflowPipeline row = new CfgTblWorkflowPipeline();
        applySaveRequest(row, request, userId, true);
        row.setSerCreatedUserId(userId > 0 ? userId : null);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        workflowPipelineRepository.save(row);
        return toDetailDto(row);
    }

    @Override
    @Transactional
    public WorkflowPipelineDetailDto update(WorkflowPipelineSaveRequest request, int userId) {
        if (request.getSerWorkflowPipelineId() == null || request.getSerWorkflowPipelineId() <= 0) {
            throw new IllegalArgumentException("serWorkflowPipelineId is required for update");
        }
        CfgTblWorkflowPipeline row = findActiveOrThrow(request.getSerWorkflowPipelineId());
        validateSaveRequest(request, row.getSerWorkflowPipelineId());
        applySaveRequest(row, request, userId, false);
        workflowPipelineRepository.save(row);
        return toDetailDto(row);
    }

    @Override
    @Transactional
    public String deleteByIds(List<Integer> ids, int userId) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No pipeline id(s) provided");
        }
        int deleted = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                continue;
            }
            Optional<CfgTblWorkflowPipeline> optional = workflowPipelineRepository.findBySerWorkflowPipelineIdAndBlIsDeletedFalse(id);
            if (!optional.isPresent()) {
                continue;
            }
            CfgTblWorkflowPipeline row = optional.get();
            row.setBlIsDeleted(true);
            row.setBlIsActive(false);
            row.setDteModifiedDate(now);
            if (userId > 0) {
                row.setSerModifiedUserId(userId);
            }
            workflowPipelineRepository.save(row);
            deleted++;
        }
        if (deleted == 0) {
            return "{\"status\":\"Failure\",\"message\":\"No pipelines deleted\"}";
        }
        return "{\"status\":\"Success\"}";
    }

    @Override
    @Transactional
    public WorkflowPipelineDetailDto duplicate(int sourceId, int userId) {
        CfgTblWorkflowPipeline source = findActiveOrThrow(sourceId);
        WorkflowPipelineSaveRequest request = new WorkflowPipelineSaveRequest();
        request.setName(buildDuplicateName(source.getTxtName()));
        request.setCode(buildDuplicateCode(source.getTxtCode()));
        request.setDescription(source.getTxtDescription());
        request.setPipelineType(source.getTxtPipelineType());
        request.setActive(Boolean.TRUE.equals(source.getBlIsActive()));
        request.setPipeline(parsePipelineSteps(source.getTxtPipelineJson()));
        return create(request, userId);
    }

    private CfgTblWorkflowPipeline findActiveOrThrow(int id) {
        return workflowPipelineRepository.findBySerWorkflowPipelineIdAndBlIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow pipeline not found: " + id));
    }

    private void validateSaveRequest(WorkflowPipelineSaveRequest request, Integer excludeId) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String name = trimToNull(request.getName());
        if (name == null) {
            throw new IllegalArgumentException("Name is required");
        }
        List<Map<String, Object>> steps = request.getPipeline();
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Pipeline must contain at least one step");
        }
        String code = trimToNull(request.getCode());
        if (code != null) {
            int exclude = excludeId != null ? excludeId : -1;
            if (workflowPipelineRepository.existsByTxtCodeIgnoreCaseAndBlIsDeletedFalseAndSerWorkflowPipelineIdNot(code, exclude)) {
                throw new IllegalArgumentException("Pipeline code already exists: " + code);
            }
        }
        serializePipelineSteps(steps);
    }

    private void applySaveRequest(CfgTblWorkflowPipeline row, WorkflowPipelineSaveRequest request, int userId, boolean isCreate) {
        row.setTxtName(trimToNull(request.getName()));
        row.setTxtCode(trimToNull(request.getCode()));
        row.setTxtDescription(trimToNull(request.getDescription()));
        String pipelineType = trimToNull(request.getPipelineType());
        row.setTxtPipelineType(pipelineType != null ? pipelineType : DEFAULT_PIPELINE_TYPE);
        List<Map<String, Object>> steps = request.getPipeline();
        String json = serializePipelineSteps(steps);
        row.setTxtPipelineJson(json);
        row.setIntStepCount(countNonInitiatorSteps(steps));
        row.setBlIsActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        row.setBlIsDeleted(false);
        row.setBlnStatus(true);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        if (userId > 0) {
            row.setSerModifiedUserId(userId);
        }
        if (isCreate) {
            if (row.getBlIsActive() == null) {
                row.setBlIsActive(true);
            }
        }
    }

    private String serializePipelineSteps(List<Map<String, Object>> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid pipeline JSON");
        }
    }

    private List<Map<String, Object>> parsePipelineSteps(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            logger.warn("Failed to parse pipeline JSON: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    private int countNonInitiatorSteps(List<Map<String, Object>> steps) {
        if (steps == null) {
            return 0;
        }
        int count = 0;
        for (Map<String, Object> step : steps) {
            if (step == null) {
                continue;
            }
            Object type = step.get("type");
            if (type != null && "initiator".equalsIgnoreCase(String.valueOf(type))) {
                continue;
            }
            count++;
        }
        return count;
    }

    private WorkflowPipelineDto toListDto(CfgTblWorkflowPipeline row) {
        WorkflowPipelineDto dto = new WorkflowPipelineDto();
        dto.setId(row.getSerWorkflowPipelineId());
        dto.setName(row.getTxtName());
        dto.setCode(row.getTxtCode());
        dto.setDescription(row.getTxtDescription());
        dto.setPipelineType(row.getTxtPipelineType());
        dto.setStepCount(row.getIntStepCount() != null ? row.getIntStepCount() : 0);
        dto.setActive(Boolean.TRUE.equals(row.getBlIsActive()));
        if (row.getDteModifiedDate() != null) {
            dto.setModifiedDate(DATE_FMT.format(row.getDteModifiedDate()));
        } else if (row.getDteCreatedDate() != null) {
            dto.setModifiedDate(DATE_FMT.format(row.getDteCreatedDate()));
        }
        return dto;
    }

    private WorkflowPipelineDetailDto toDetailDto(CfgTblWorkflowPipeline row) {
        WorkflowPipelineDetailDto dto = new WorkflowPipelineDetailDto();
        WorkflowPipelineDto list = toListDto(row);
        dto.setId(list.getId());
        dto.setName(list.getName());
        dto.setCode(list.getCode());
        dto.setDescription(list.getDescription());
        dto.setPipelineType(list.getPipelineType());
        dto.setStepCount(list.getStepCount());
        dto.setActive(list.getActive());
        dto.setModifiedDate(list.getModifiedDate());
        dto.setPipeline(parsePipelineSteps(row.getTxtPipelineJson()));
        return dto;
    }

    private String buildDuplicateName(String name) {
        String base = name != null ? name.trim() : "Pipeline";
        String candidate = base + " (copy)";
        if (candidate.length() > 200) {
            candidate = candidate.substring(0, 200);
        }
        return candidate;
    }

    private String buildDuplicateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String base = code.trim();
        String suffix = "-COPY";
        String candidate = base + suffix;
        if (candidate.length() > 50) {
            candidate = base.substring(0, Math.max(1, 50 - suffix.length())) + suffix;
        }
        int n = 2;
        while (workflowPipelineRepository.findByTxtCodeIgnoreCaseAndBlIsDeletedFalse(candidate).isPresent()) {
            String extra = "-" + n;
            candidate = base;
            if (candidate.length() + extra.length() > 50) {
                candidate = candidate.substring(0, 50 - extra.length());
            }
            candidate = candidate + extra;
            n++;
        }
        return candidate;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public boolean existsByCode(String code, Integer excludeId) {
        String trimmed = trimToNull(code);
        if (trimmed == null) {
            return false;
        }
        int exclude = excludeId != null && excludeId > 0 ? excludeId : -1;
        return workflowPipelineRepository.existsByTxtCodeIgnoreCaseAndBlIsDeletedFalseAndSerWorkflowPipelineIdNot(trimmed, exclude);
    }

    /** Default pipeline for new records (initiator only). */
    public static List<Map<String, Object>> defaultPipelineSteps() {
        Map<String, Object> initiator = new LinkedHashMap<>();
        initiator.put("id", "initiator");
        initiator.put("name", "Initiator");
        initiator.put("type", "initiator");
        initiator.put("order", 1);
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(initiator);
        return steps;
    }
}
