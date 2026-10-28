package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;
import java.util.Map;

public class WorkflowPipelineSaveRequest {

    private Integer serWorkflowPipelineId;
    private String name;
    private String code;
    private String description;
    private String pipelineType;
    private Boolean active;
    private List<Map<String, Object>> pipeline;

    public Integer getSerWorkflowPipelineId() {
        return serWorkflowPipelineId;
    }

    public void setSerWorkflowPipelineId(Integer serWorkflowPipelineId) {
        this.serWorkflowPipelineId = serWorkflowPipelineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPipelineType() {
        return pipelineType;
    }

    public void setPipelineType(String pipelineType) {
        this.pipelineType = pipelineType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Map<String, Object>> getPipeline() {
        return pipeline;
    }

    public void setPipeline(List<Map<String, Object>> pipeline) {
        this.pipeline = pipeline;
    }
}
