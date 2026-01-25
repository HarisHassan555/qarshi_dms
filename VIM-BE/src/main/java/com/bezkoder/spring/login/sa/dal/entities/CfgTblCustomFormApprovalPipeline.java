package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The persistent class for the cfg_tbl_custom_form_approval_pipeline database table.
 */
@Entity
@Table(name="cfg_tbl_custom_form_approval_pipeline")
@NamedQuery(name="CfgTblCustomFormApprovalPipeline.findAll", query="SELECT c FROM CfgTblCustomFormApprovalPipeline c")
public class CfgTblCustomFormApprovalPipeline implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ser_approval_pipeline_id")
    private Integer serApprovalPipelineId;

    @Column(name="int_approval_order")
    private Integer intApprovalOrder;

    @Column(name="bl_is_active")
    private Boolean blIsActive;

    @Column(name="bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name="dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name="dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name="ser_created_user")
    private Integer serCreatedUser;

    @Column(name="ser_modified_user")
    private Integer serModifiedUser;

    //bi-directional many-to-one association to CfgTblCustomForm
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ser_form_id")
    @JsonIgnore // Ignore during both serialization and deserialization since we handle it manually
    private CfgTblCustomForm cfgTblCustomForm;

    //bi-directional many-to-one association to HrTblDepartment
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="ser_department_id")
    @JsonIgnoreProperties({"cfgTblUsers", "hrTblEmployees"})
    private HrTblDepartment hrTblDepartment;

    // Transient field to accept serDepartmentId from JSON during deserialization
    @Transient
    private Integer serDepartmentId;

    public CfgTblCustomFormApprovalPipeline() {
    }

    public Integer getSerApprovalPipelineId() {
        return this.serApprovalPipelineId;
    }

    public void setSerApprovalPipelineId(Integer serApprovalPipelineId) {
        this.serApprovalPipelineId = serApprovalPipelineId;
    }

    public Integer getIntApprovalOrder() {
        return this.intApprovalOrder;
    }

    public void setIntApprovalOrder(Integer intApprovalOrder) {
        this.intApprovalOrder = intApprovalOrder;
    }

    public Boolean getBlIsActive() {
        return this.blIsActive;
    }

    public void setBlIsActive(Boolean blIsActive) {
        this.blIsActive = blIsActive;
    }

    public Boolean getBlIsDeleted() {
        return this.blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public Timestamp getDteCreatedDate() {
        return this.dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteModifiedDate() {
        return this.dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }

    public Integer getSerCreatedUser() {
        return this.serCreatedUser;
    }

    public void setSerCreatedUser(Integer serCreatedUser) {
        this.serCreatedUser = serCreatedUser;
    }

    public Integer getSerModifiedUser() {
        return this.serModifiedUser;
    }

    public void setSerModifiedUser(Integer serModifiedUser) {
        this.serModifiedUser = serModifiedUser;
    }

    public CfgTblCustomForm getCfgTblCustomForm() {
        return this.cfgTblCustomForm;
    }

    public void setCfgTblCustomForm(CfgTblCustomForm cfgTblCustomForm) {
        this.cfgTblCustomForm = cfgTblCustomForm;
    }

    public HrTblDepartment getHrTblDepartment() {
        return this.hrTblDepartment;
    }

    public void setHrTblDepartment(HrTblDepartment hrTblDepartment) {
        this.hrTblDepartment = hrTblDepartment;
    }

    public Integer getSerDepartmentId() {
        return serDepartmentId;
    }

    public void setSerDepartmentId(Integer serDepartmentId) {
        this.serDepartmentId = serDepartmentId;
    }
}

