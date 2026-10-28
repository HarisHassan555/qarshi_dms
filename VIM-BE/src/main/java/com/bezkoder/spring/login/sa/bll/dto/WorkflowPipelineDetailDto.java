package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;
import java.util.Map;

public class WorkflowPipelineDetailDto extends WorkflowPipelineDto {

    private List<Map<String, Object>> pipeline;

    public List<Map<String, Object>> getPipeline() {
        return pipeline;
    }

    public void setPipeline(List<Map<String, Object>> pipeline) {
        this.pipeline = pipeline;
    }
}
