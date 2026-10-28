package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.WorkflowThresholdRuleDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowThresholdRuleSaveRequest;

import java.util.List;

public interface IWorkflowThresholdRuleService {

    List<WorkflowThresholdRuleDto> getAll(Integer serFormId);

    WorkflowThresholdRuleDto getById(int id);

    WorkflowThresholdRuleDto create(WorkflowThresholdRuleSaveRequest request, int userId);

    WorkflowThresholdRuleDto update(WorkflowThresholdRuleSaveRequest request, int userId);

    String deleteByIds(List<Integer> ids, int userId);
}
