package com.bezkoder.spring.login.sa.bll.dto;

/**
 * Matches Angular {@code WorkflowPipelineThresholdRule}.
 */
public class WorkflowThresholdRuleDto {

    private Integer id;
    private String ruleName;
    private Integer serFormId;
    private String fieldKey;
    private String fieldLabel;
    private String operator;
    private String value;
    private String routingAction;
    private String insertPosition;
    private Integer approvalStepOrder;
    private String targetType;
    private Integer serTargetId;
    private String targetLabel;
    private Integer priority;
    private Boolean active;
    private String notes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRoutingAction() {
        return routingAction;
    }

    public void setRoutingAction(String routingAction) {
        this.routingAction = routingAction;
    }

    public String getInsertPosition() {
        return insertPosition;
    }

    public void setInsertPosition(String insertPosition) {
        this.insertPosition = insertPosition;
    }

    public Integer getApprovalStepOrder() {
        return approvalStepOrder;
    }

    public void setApprovalStepOrder(Integer approvalStepOrder) {
        this.approvalStepOrder = approvalStepOrder;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getSerTargetId() {
        return serTargetId;
    }

    public void setSerTargetId(Integer serTargetId) {
        this.serTargetId = serTargetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
