package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The persistent class for the cfg_tbl_custom_form_application database table.
 */
@Entity
@Table(name = "cfg_tbl_custom_form_application")
@NamedQuery(name = "CfgTblCustomFormApplication.findAll", query = "SELECT c FROM CfgTblCustomFormApplication c")
public class CfgTblCustomFormApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_application_id")
    private Integer serApplicationId;

    @Column(name = "ser_form_id")
    private Integer serFormId;

    @Column(name = "txt_form_code")
    private String txtFormCode;

    @Column(name = "txt_application_data", columnDefinition = "JSON")
    private String txtApplicationData;

    @Column(name = "txt_status")
    private String txtStatus;

    @Column(name = "int_current_approval_level")
    private Integer intCurrentApprovalLevel;

    @Column(name = "ser_submitted_by")
    private Integer serSubmittedBy;

    @Column(name = "ser_current_approver")
    private Integer serCurrentApprover;

    @Column(name = "txt_remarks", columnDefinition = "TEXT")
    private String txtRemarks;

    @Column(name = "txt_approval_history", columnDefinition = "JSON")
    private String txtApprovalHistory;

    @Column(name = "txt_prior_approvals", columnDefinition = "JSON")
    private String txtPriorApprovals;

    @Column(name = "txt_asset_code")
    private String txtAssetCode;

    @Column(name = "txt_pr_code")
    private String txtPrCode;

    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_data")
    private byte[] blbPdfData;

    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_data_initial")
    private byte[] blbPdfDataInitial;

    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_data_previous")
    private byte[] blbPdfDataPrevious;

    @Column(name = "txt_pdf_name")
    private String txtPdfName;

    @Column(name = "txt_pdf_mime")
    private String txtPdfMime;

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

    @Column(name = "ser_created_user")
    private Integer serCreatedUser;

    @Column(name = "ser_modified_user")
    private Integer serModifiedUser;

    @Transient
    private Boolean deferEmail;

    // bi-directional many-to-one association to CfgTblCustomForm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ser_form_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({ "cfgTblCustomFormFields", "cfgTblCustomFormApprovalPipelines" })
    private CfgTblCustomForm cfgTblCustomForm;

    public CfgTblCustomFormApplication() {
    }

    public Integer getSerApplicationId() {
        return this.serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public Integer getSerFormId() {
        return this.serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtFormCode() {
        return this.txtFormCode;
    }

    public void setTxtFormCode(String txtFormCode) {
        this.txtFormCode = txtFormCode;
    }

    public String getTxtApplicationData() {
        return this.txtApplicationData;
    }

    public void setTxtApplicationData(String txtApplicationData) {
        this.txtApplicationData = txtApplicationData;
    }

    public String getTxtStatus() {
        return this.txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public Integer getIntCurrentApprovalLevel() {
        return this.intCurrentApprovalLevel;
    }

    public void setIntCurrentApprovalLevel(Integer intCurrentApprovalLevel) {
        this.intCurrentApprovalLevel = intCurrentApprovalLevel;
    }

    public Integer getSerSubmittedBy() {
        return this.serSubmittedBy;
    }

    public void setSerSubmittedBy(Integer serSubmittedBy) {
        this.serSubmittedBy = serSubmittedBy;
    }

    public Integer getSerCurrentApprover() {
        return this.serCurrentApprover;
    }

    public void setSerCurrentApprover(Integer serCurrentApprover) {
        this.serCurrentApprover = serCurrentApprover;
    }

    public String getTxtRemarks() {
        return this.txtRemarks;
    }

    public void setTxtRemarks(String txtRemarks) {
        this.txtRemarks = txtRemarks;
    }

    public String getTxtApprovalHistory() {
        return this.txtApprovalHistory;
    }

    public void setTxtApprovalHistory(String txtApprovalHistory) {
        this.txtApprovalHistory = txtApprovalHistory;
    }

    public String getTxtPriorApprovals() {
        return txtPriorApprovals;
    }

    public void setTxtPriorApprovals(String txtPriorApprovals) {
        this.txtPriorApprovals = txtPriorApprovals;
    }

    public String getTxtAssetCode() {
        return txtAssetCode;
    }

    public void setTxtAssetCode(String txtAssetCode) {
        this.txtAssetCode = txtAssetCode;
    }

    public String getTxtPrCode() {
        return txtPrCode;
    }

    public void setTxtPrCode(String txtPrCode) {
        this.txtPrCode = txtPrCode;
    }

    public byte[] getBlbPdfData() {
        return blbPdfData;
    }

    public void setBlbPdfData(byte[] blbPdfData) {
        this.blbPdfData = blbPdfData;
    }

    public byte[] getBlbPdfDataInitial() {
        return blbPdfDataInitial;
    }

    public void setBlbPdfDataInitial(byte[] blbPdfDataInitial) {
        this.blbPdfDataInitial = blbPdfDataInitial;
    }

    public byte[] getBlbPdfDataPrevious() {
        return blbPdfDataPrevious;
    }

    public void setBlbPdfDataPrevious(byte[] blbPdfDataPrevious) {
        this.blbPdfDataPrevious = blbPdfDataPrevious;
    }

    public String getTxtPdfName() {
        return txtPdfName;
    }

    public void setTxtPdfName(String txtPdfName) {
        this.txtPdfName = txtPdfName;
    }

    public String getTxtPdfMime() {
        return txtPdfMime;
    }

    public void setTxtPdfMime(String txtPdfMime) {
        this.txtPdfMime = txtPdfMime;
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

    public Boolean getDeferEmail() {
        return deferEmail;
    }

    public void setDeferEmail(Boolean deferEmail) {
        this.deferEmail = deferEmail;
    }

    public CfgTblCustomForm getCfgTblCustomForm() {
        return this.cfgTblCustomForm;
    }

    public void setCfgTblCustomForm(CfgTblCustomForm cfgTblCustomForm) {
        this.cfgTblCustomForm = cfgTblCustomForm;
    }
}
