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

    @Column(name = "txt_po_code")
    private String txtPoCode;

    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_data")
    private byte[] blbPdfData;

    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_0")
    private byte[] blbPdfStage0;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_1")
    private byte[] blbPdfStage1;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_2")
    private byte[] blbPdfStage2;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_3")
    private byte[] blbPdfStage3;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_4")
    private byte[] blbPdfStage4;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_5")
    private byte[] blbPdfStage5;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_6")
    private byte[] blbPdfStage6;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_7")
    private byte[] blbPdfStage7;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_8")
    private byte[] blbPdfStage8;
    @Lob
    @JsonIgnore
    @Column(name = "blb_pdf_stage_9")
    private byte[] blbPdfStage9;

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
    @JsonIgnoreProperties({ "cfgTblCustomFormApprovalPipelines" })
    private CfgTblCustomForm cfgTblCustomForm;

    public CfgTblCustomFormApplication() {
    }

    /**
     * Computed flag for JSON API - tells frontend this is a CAPF form so the correct preview is shown.
     * Matches backend isCapfForm logic in CfgTblCustomFormApplicationDAO.
     */
    public Boolean getIsCapfForm() {
        if (cfgTblCustomForm != null) {
            String name = cfgTblCustomForm.getTxtFormName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("capf") || lower.contains("capital assets purchase")) return true;
            }
            String code = cfgTblCustomForm.getTxtFormCode();
            if (code != null && code.toLowerCase().contains("capf")) return true;
        }
        if (txtFormCode != null && txtFormCode.toLowerCase().contains("capf")) return true;
        return false;
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

    public String getTxtPoCode() {
        return txtPoCode;
    }

    public void setTxtPoCode(String txtPoCode) {
        this.txtPoCode = txtPoCode;
    }

    public byte[] getBlbPdfData() {
        return blbPdfData;
    }

    public void setBlbPdfData(byte[] blbPdfData) {
        this.blbPdfData = blbPdfData;
    }

    private static final int MAX_PDF_STAGES = 10;

    public byte[] getBlbPdfForStage(int stage) {
        if (stage < 0 || stage >= MAX_PDF_STAGES) return null;
        switch (stage) {
            case 0: return blbPdfStage0;
            case 1: return blbPdfStage1;
            case 2: return blbPdfStage2;
            case 3: return blbPdfStage3;
            case 4: return blbPdfStage4;
            case 5: return blbPdfStage5;
            case 6: return blbPdfStage6;
            case 7: return blbPdfStage7;
            case 8: return blbPdfStage8;
            case 9: return blbPdfStage9;
            default: return null;
        }
    }

    public void setBlbPdfForStage(int stage, byte[] data) {
        if (stage < 0 || stage >= MAX_PDF_STAGES) return;
        switch (stage) {
            case 0: blbPdfStage0 = data; break;
            case 1: blbPdfStage1 = data; break;
            case 2: blbPdfStage2 = data; break;
            case 3: blbPdfStage3 = data; break;
            case 4: blbPdfStage4 = data; break;
            case 5: blbPdfStage5 = data; break;
            case 6: blbPdfStage6 = data; break;
            case 7: blbPdfStage7 = data; break;
            case 8: blbPdfStage8 = data; break;
            case 9: blbPdfStage9 = data; break;
        }
    }

    public byte[] getBlbPdfStage0() { return blbPdfStage0; }
    public void setBlbPdfStage0(byte[] v) { blbPdfStage0 = v; }
    public byte[] getBlbPdfStage1() { return blbPdfStage1; }
    public void setBlbPdfStage1(byte[] v) { blbPdfStage1 = v; }
    public byte[] getBlbPdfStage2() { return blbPdfStage2; }
    public void setBlbPdfStage2(byte[] v) { blbPdfStage2 = v; }
    public byte[] getBlbPdfStage3() { return blbPdfStage3; }
    public void setBlbPdfStage3(byte[] v) { blbPdfStage3 = v; }
    public byte[] getBlbPdfStage4() { return blbPdfStage4; }
    public void setBlbPdfStage4(byte[] v) { blbPdfStage4 = v; }
    public byte[] getBlbPdfStage5() { return blbPdfStage5; }
    public void setBlbPdfStage5(byte[] v) { blbPdfStage5 = v; }
    public byte[] getBlbPdfStage6() { return blbPdfStage6; }
    public void setBlbPdfStage6(byte[] v) { blbPdfStage6 = v; }
    public byte[] getBlbPdfStage7() { return blbPdfStage7; }
    public void setBlbPdfStage7(byte[] v) { blbPdfStage7 = v; }
    public byte[] getBlbPdfStage8() { return blbPdfStage8; }
    public void setBlbPdfStage8(byte[] v) { blbPdfStage8 = v; }
    public byte[] getBlbPdfStage9() { return blbPdfStage9; }
    public void setBlbPdfStage9(byte[] v) { blbPdfStage9 = v; }

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
