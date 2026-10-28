package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_workflow_threshold_rule")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblWorkflowThresholdRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_workflow_threshold_rule_id")
    private Integer serWorkflowThresholdRuleId;

    @Column(name = "ser_form_id", nullable = false)
    private Integer serFormId;

    @Column(name = "txt_rule_name", nullable = false, length = 200)
    private String txtRuleName;

    @Column(name = "txt_field_key", nullable = false, length = 120)
    private String txtFieldKey;

    @Column(name = "txt_field_label", length = 200)
    private String txtFieldLabel;

    @Column(name = "txt_operator", nullable = false, length = 32)
    private String txtOperator;

    @Column(name = "txt_compare_value", nullable = false, length = 500)
    private String txtCompareValue;

    @Column(name = "txt_routing_action", nullable = false, length = 16)
    private String txtRoutingAction;

    @Column(name = "txt_insert_position", length = 16)
    private String txtInsertPosition;

    @Column(name = "int_approval_step_order", nullable = false)
    private Integer intApprovalStepOrder;

    @Column(name = "txt_target_type", nullable = false, length = 16)
    private String txtTargetType;

    @Column(name = "ser_target_id")
    private Integer serTargetId;

    @Column(name = "txt_target_label", length = 200)
    private String txtTargetLabel;

    @Column(name = "int_priority", nullable = false)
    private Integer intPriority;

    @Column(name = "txt_notes", length = 500)
    private String txtNotes;

    @Column(name = "bl_is_active")
    private Boolean blIsActive;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name = "ser_created_user_id")
    private Integer serCreatedUserId;

    @Column(name = "ser_modified_user_id")
    private Integer serModifiedUserId;

    public Integer getSerWorkflowThresholdRuleId() {
        return serWorkflowThresholdRuleId;
    }

    public void setSerWorkflowThresholdRuleId(Integer serWorkflowThresholdRuleId) {
        this.serWorkflowThresholdRuleId = serWorkflowThresholdRuleId;
    }

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtRuleName() {
        return txtRuleName;
    }

    public void setTxtRuleName(String txtRuleName) {
        this.txtRuleName = txtRuleName;
    }

    public String getTxtFieldKey() {
        return txtFieldKey;
    }

    public void setTxtFieldKey(String txtFieldKey) {
        this.txtFieldKey = txtFieldKey;
    }

    public String getTxtFieldLabel() {
        return txtFieldLabel;
    }

    public void setTxtFieldLabel(String txtFieldLabel) {
        this.txtFieldLabel = txtFieldLabel;
    }

    public String getTxtOperator() {
        return txtOperator;
    }

    public void setTxtOperator(String txtOperator) {
        this.txtOperator = txtOperator;
    }

    public String getTxtCompareValue() {
        return txtCompareValue;
    }

    public void setTxtCompareValue(String txtCompareValue) {
        this.txtCompareValue = txtCompareValue;
    }

    public String getTxtRoutingAction() {
        return txtRoutingAction;
    }

    public void setTxtRoutingAction(String txtRoutingAction) {
        this.txtRoutingAction = txtRoutingAction;
    }

    public String getTxtInsertPosition() {
        return txtInsertPosition;
    }

    public void setTxtInsertPosition(String txtInsertPosition) {
        this.txtInsertPosition = txtInsertPosition;
    }

    public Integer getIntApprovalStepOrder() {
        return intApprovalStepOrder;
    }

    public void setIntApprovalStepOrder(Integer intApprovalStepOrder) {
        this.intApprovalStepOrder = intApprovalStepOrder;
    }

    public String getTxtTargetType() {
        return txtTargetType;
    }

    public void setTxtTargetType(String txtTargetType) {
        this.txtTargetType = txtTargetType;
    }

    public Integer getSerTargetId() {
        return serTargetId;
    }

    public void setSerTargetId(Integer serTargetId) {
        this.serTargetId = serTargetId;
    }

    public String getTxtTargetLabel() {
        return txtTargetLabel;
    }

    public void setTxtTargetLabel(String txtTargetLabel) {
        this.txtTargetLabel = txtTargetLabel;
    }

    public Integer getIntPriority() {
        return intPriority;
    }

    public void setIntPriority(Integer intPriority) {
        this.intPriority = intPriority;
    }

    public String getTxtNotes() {
        return txtNotes;
    }

    public void setTxtNotes(String txtNotes) {
        this.txtNotes = txtNotes;
    }

    public Boolean getBlIsActive() {
        return blIsActive;
    }

    public void setBlIsActive(Boolean blIsActive) {
        this.blIsActive = blIsActive;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }

    public Integer getSerCreatedUserId() {
        return serCreatedUserId;
    }

    public void setSerCreatedUserId(Integer serCreatedUserId) {
        this.serCreatedUserId = serCreatedUserId;
    }

    public Integer getSerModifiedUserId() {
        return serModifiedUserId;
    }

    public void setSerModifiedUserId(Integer serModifiedUserId) {
        this.serModifiedUserId = serModifiedUserId;
    }
}
