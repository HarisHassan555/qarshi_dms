package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The persistent class for the cfg_tbl_custom_form database table.
 */
@Entity
@Table(name="cfg_tbl_custom_form")
@NamedQuery(name="CfgTblCustomForm.findAll", query="SELECT c FROM CfgTblCustomForm c")
public class CfgTblCustomForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ser_form_id")
    private Integer serFormId;

    @Column(name="txt_form_name")
    private String txtFormName;

    @Column(name="txt_form_code")
    private String txtFormCode;

    @Column(name="txt_convention_prefix")
    private String txtConventionPrefix;

    @Column(name="txt_form_description")
    private String txtFormDescription;

    @Column(name="txt_approval_pipeline", columnDefinition = "JSON")
    @JsonIgnore // Don't expose JSON string in API responses, only expose the deserialized list
    private String txtApprovalPipeline;

    @Column(name="bl_is_active")
    private Boolean blIsActive;

    @Column(name="bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name="bln_status")
    private Boolean blnStatus;

    @Column(name="dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name="dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name="ser_created_user")
    private Integer serCreatedUser;

    @Column(name="ser_modified_user")
    private Integer serModifiedUser;

    //bi-directional one-to-many association to CfgTblCustomFormField
    @OneToMany(mappedBy="cfgTblCustomForm", cascade=CascadeType.ALL, fetch=FetchType.EAGER, orphanRemoval=true)
    @JsonManagedReference
    @JsonIgnoreProperties({"cfgTblCustomForm"})
    @OrderBy("intFieldOrder ASC")
    private List<CfgTblCustomFormField> cfgTblCustomFormFields;

    // Approval pipeline stored as JSON array - transient for deserialization
    @Transient
    private List<CfgTblCustomFormApprovalPipeline> cfgTblCustomFormApprovalPipelines;

    public CfgTblCustomForm() {
    }

    public Integer getSerFormId() {
        return this.serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtFormName() {
        return this.txtFormName;
    }

    public void setTxtFormName(String txtFormName) {
        this.txtFormName = txtFormName;
    }

    public String getTxtFormCode() {
        return this.txtFormCode;
    }

    public void setTxtFormCode(String txtFormCode) {
        this.txtFormCode = txtFormCode;
    }

    public String getTxtConventionPrefix() {
        return this.txtConventionPrefix;
    }

    public void setTxtConventionPrefix(String txtConventionPrefix) {
        this.txtConventionPrefix = txtConventionPrefix;
    }

    public String getTxtFormDescription() {
        return this.txtFormDescription;
    }

    public void setTxtFormDescription(String txtFormDescription) {
        this.txtFormDescription = txtFormDescription;
    }

    public String getTxtApprovalPipeline() {
        return this.txtApprovalPipeline;
    }

    public void setTxtApprovalPipeline(String txtApprovalPipeline) {
        this.txtApprovalPipeline = txtApprovalPipeline;
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

    public Boolean getBlnStatus() {
        return this.blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
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

    public List<CfgTblCustomFormField> getCfgTblCustomFormFields() {
        return this.cfgTblCustomFormFields;
    }

    public void setCfgTblCustomFormFields(List<CfgTblCustomFormField> cfgTblCustomFormFields) {
        this.cfgTblCustomFormFields = cfgTblCustomFormFields;
    }

    public CfgTblCustomFormField addCfgTblCustomFormField(CfgTblCustomFormField cfgTblCustomFormField) {
        getCfgTblCustomFormFields().add(cfgTblCustomFormField);
        cfgTblCustomFormField.setCfgTblCustomForm(this);
        return cfgTblCustomFormField;
    }

    public CfgTblCustomFormField removeCfgTblCustomFormField(CfgTblCustomFormField cfgTblCustomFormField) {
        getCfgTblCustomFormFields().remove(cfgTblCustomFormField);
        cfgTblCustomFormField.setCfgTblCustomForm(null);
        return cfgTblCustomFormField;
    }

    public List<CfgTblCustomFormApprovalPipeline> getCfgTblCustomFormApprovalPipelines() {
        return this.cfgTblCustomFormApprovalPipelines;
    }

    public void setCfgTblCustomFormApprovalPipelines(List<CfgTblCustomFormApprovalPipeline> cfgTblCustomFormApprovalPipelines) {
        this.cfgTblCustomFormApprovalPipelines = cfgTblCustomFormApprovalPipelines;
    }

    public CfgTblCustomFormApprovalPipeline addCfgTblCustomFormApprovalPipeline(CfgTblCustomFormApprovalPipeline cfgTblCustomFormApprovalPipeline) {
        getCfgTblCustomFormApprovalPipelines().add(cfgTblCustomFormApprovalPipeline);
        cfgTblCustomFormApprovalPipeline.setCfgTblCustomForm(this);
        return cfgTblCustomFormApprovalPipeline;
    }

    public CfgTblCustomFormApprovalPipeline removeCfgTblCustomFormApprovalPipeline(CfgTblCustomFormApprovalPipeline cfgTblCustomFormApprovalPipeline) {
        getCfgTblCustomFormApprovalPipelines().remove(cfgTblCustomFormApprovalPipeline);
        cfgTblCustomFormApprovalPipeline.setCfgTblCustomForm(null);
        return cfgTblCustomFormApprovalPipeline;
    }
}

