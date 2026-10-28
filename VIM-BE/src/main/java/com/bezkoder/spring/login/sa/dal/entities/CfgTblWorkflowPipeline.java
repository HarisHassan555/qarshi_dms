package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_workflow_pipeline")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblWorkflowPipeline implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_workflow_pipeline_id")
    private Integer serWorkflowPipelineId;

    @Column(name = "txt_name", nullable = false, length = 200)
    private String txtName;

    @Column(name = "txt_code", length = 50)
    private String txtCode;

    @Column(name = "txt_description", length = 500)
    private String txtDescription;

    @Column(name = "txt_pipeline_type", nullable = false, length = 32)
    private String txtPipelineType;

    @Lob
    @Column(name = "txt_pipeline_json", nullable = false, columnDefinition = "LONGTEXT")
    private String txtPipelineJson;

    @Column(name = "int_step_count", nullable = false)
    private Integer intStepCount;

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

    public Integer getSerWorkflowPipelineId() {
        return serWorkflowPipelineId;
    }

    public void setSerWorkflowPipelineId(Integer serWorkflowPipelineId) {
        this.serWorkflowPipelineId = serWorkflowPipelineId;
    }

    public String getTxtName() {
        return txtName;
    }

    public void setTxtName(String txtName) {
        this.txtName = txtName;
    }

    public String getTxtCode() {
        return txtCode;
    }

    public void setTxtCode(String txtCode) {
        this.txtCode = txtCode;
    }

    public String getTxtDescription() {
        return txtDescription;
    }

    public void setTxtDescription(String txtDescription) {
        this.txtDescription = txtDescription;
    }

    public String getTxtPipelineType() {
        return txtPipelineType;
    }

    public void setTxtPipelineType(String txtPipelineType) {
        this.txtPipelineType = txtPipelineType;
    }

    public String getTxtPipelineJson() {
        return txtPipelineJson;
    }

    public void setTxtPipelineJson(String txtPipelineJson) {
        this.txtPipelineJson = txtPipelineJson;
    }

    public Integer getIntStepCount() {
        return intStepCount;
    }

    public void setIntStepCount(Integer intStepCount) {
        this.intStepCount = intStepCount;
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
