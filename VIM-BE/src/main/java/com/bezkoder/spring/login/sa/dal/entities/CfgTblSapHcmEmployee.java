package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_sap_hcm_employee")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblSapHcmEmployee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_sap_hcm_employee_id")
    private Integer serSapHcmEmployeeId;

    @Column(name = "txt_employee_id", nullable = false, length = 32)
    private String txtEmployeeId;

    @Column(name = "txt_emp_name")
    private String txtEmpName;

    @Column(name = "txt_department_name")
    private String txtDepartmentName;

    @Column(name = "txt_division")
    private String txtDivision;

    @Column(name = "txt_designation")
    private String txtDesignation;

    @Column(name = "txt_email")
    private String txtEmail;

    @Column(name = "txt_pay_scale", length = 32)
    private String txtPayScale;

    @Column(name = "txt_subarea")
    private String txtSubarea;

    @Column(name = "txt_company_name")
    private String txtCompanyName;

    @Column(name = "txt_hiring_date", length = 32)
    private String txtHiringDate;

    @Column(name = "txt_confirmation_date", length = 32)
    private String txtConfirmationDate;

    @Column(name = "txt_retirement_date", length = 32)
    private String txtRetirementDate;

    @Column(name = "txt_separation_date", length = 32)
    private String txtSeparationDate;

    @Column(name = "txt_rehiring_date", length = 32)
    private String txtRehiringDate;

    @Column(name = "txt_annual_inc_type", length = 64)
    private String txtAnnualIncType;

    @Column(name = "txt_active", length = 64)
    private String txtActive;

    @Column(name = "txt_report_to", length = 32)
    private String txtReportTo;

    @Column(name = "txt_report_to_name")
    private String txtReportToName;

    @Column(name = "txt_picture_path", length = 512)
    private String txtPicturePath;

    @Column(name = "bl_has_picture")
    private Boolean blHasPicture;

    /** JSON snapshot without picture; LONGTEXT avoids Hibernate LOB → MySQL JSON binary charset error. */
    @Column(name = "txt_record_json", columnDefinition = "LONGTEXT")
    private String txtRecordJson;

    @Column(name = "dte_last_synced_date")
    private Timestamp dteLastSyncedDate;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    public Integer getSerSapHcmEmployeeId() {
        return serSapHcmEmployeeId;
    }

    public void setSerSapHcmEmployeeId(Integer serSapHcmEmployeeId) {
        this.serSapHcmEmployeeId = serSapHcmEmployeeId;
    }

    public String getTxtEmployeeId() {
        return txtEmployeeId;
    }

    public void setTxtEmployeeId(String txtEmployeeId) {
        this.txtEmployeeId = txtEmployeeId;
    }

    public String getTxtEmpName() {
        return txtEmpName;
    }

    public void setTxtEmpName(String txtEmpName) {
        this.txtEmpName = txtEmpName;
    }

    public String getTxtDepartmentName() {
        return txtDepartmentName;
    }

    public void setTxtDepartmentName(String txtDepartmentName) {
        this.txtDepartmentName = txtDepartmentName;
    }

    public String getTxtDivision() {
        return txtDivision;
    }

    public void setTxtDivision(String txtDivision) {
        this.txtDivision = txtDivision;
    }

    public String getTxtDesignation() {
        return txtDesignation;
    }

    public void setTxtDesignation(String txtDesignation) {
        this.txtDesignation = txtDesignation;
    }

    public String getTxtEmail() {
        return txtEmail;
    }

    public void setTxtEmail(String txtEmail) {
        this.txtEmail = txtEmail;
    }

    public String getTxtPayScale() {
        return txtPayScale;
    }

    public void setTxtPayScale(String txtPayScale) {
        this.txtPayScale = txtPayScale;
    }

    public String getTxtSubarea() {
        return txtSubarea;
    }

    public void setTxtSubarea(String txtSubarea) {
        this.txtSubarea = txtSubarea;
    }

    public String getTxtCompanyName() {
        return txtCompanyName;
    }

    public void setTxtCompanyName(String txtCompanyName) {
        this.txtCompanyName = txtCompanyName;
    }

    public String getTxtHiringDate() {
        return txtHiringDate;
    }

    public void setTxtHiringDate(String txtHiringDate) {
        this.txtHiringDate = txtHiringDate;
    }

    public String getTxtConfirmationDate() {
        return txtConfirmationDate;
    }

    public void setTxtConfirmationDate(String txtConfirmationDate) {
        this.txtConfirmationDate = txtConfirmationDate;
    }

    public String getTxtRetirementDate() {
        return txtRetirementDate;
    }

    public void setTxtRetirementDate(String txtRetirementDate) {
        this.txtRetirementDate = txtRetirementDate;
    }

    public String getTxtSeparationDate() {
        return txtSeparationDate;
    }

    public void setTxtSeparationDate(String txtSeparationDate) {
        this.txtSeparationDate = txtSeparationDate;
    }

    public String getTxtRehiringDate() {
        return txtRehiringDate;
    }

    public void setTxtRehiringDate(String txtRehiringDate) {
        this.txtRehiringDate = txtRehiringDate;
    }

    public String getTxtAnnualIncType() {
        return txtAnnualIncType;
    }

    public void setTxtAnnualIncType(String txtAnnualIncType) {
        this.txtAnnualIncType = txtAnnualIncType;
    }

    public String getTxtActive() {
        return txtActive;
    }

    public void setTxtActive(String txtActive) {
        this.txtActive = txtActive;
    }

    public String getTxtReportTo() {
        return txtReportTo;
    }

    public void setTxtReportTo(String txtReportTo) {
        this.txtReportTo = txtReportTo;
    }

    public String getTxtReportToName() {
        return txtReportToName;
    }

    public void setTxtReportToName(String txtReportToName) {
        this.txtReportToName = txtReportToName;
    }

    public String getTxtPicturePath() {
        return txtPicturePath;
    }

    public void setTxtPicturePath(String txtPicturePath) {
        this.txtPicturePath = txtPicturePath;
    }

    public Boolean getBlHasPicture() {
        return blHasPicture;
    }

    public void setBlHasPicture(Boolean blHasPicture) {
        this.blHasPicture = blHasPicture;
    }

    public String getTxtRecordJson() {
        return txtRecordJson;
    }

    public void setTxtRecordJson(String txtRecordJson) {
        this.txtRecordJson = txtRecordJson;
    }

    public Timestamp getDteLastSyncedDate() {
        return dteLastSyncedDate;
    }

    public void setDteLastSyncedDate(Timestamp dteLastSyncedDate) {
        this.dteLastSyncedDate = dteLastSyncedDate;
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

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }
}
