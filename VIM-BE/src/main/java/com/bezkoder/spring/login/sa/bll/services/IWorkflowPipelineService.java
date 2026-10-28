package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDetailDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineSaveRequest;

import java.util.List;

public interface IWorkflowPipelineService {

    List<WorkflowPipelineDto> getAll();

    WorkflowPipelineDetailDto getById(int id);

    WorkflowPipelineDetailDto create(WorkflowPipelineSaveRequest request, int userId);

    WorkflowPipelineDetailDto update(WorkflowPipelineSaveRequest request, int userId);

    String deleteByIds(List<Integer> ids, int userId);

    WorkflowPipelineDetailDto duplicate(int sourceId, int userId);

    boolean existsByCode(String code, Integer excludeId);
}
