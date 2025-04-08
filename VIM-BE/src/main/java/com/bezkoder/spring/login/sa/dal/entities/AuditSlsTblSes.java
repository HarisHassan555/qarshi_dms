package com.bezkoder.spring.login.sa.dal.entities;


import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "audit_sls_tbl_ses")
public class AuditSlsTblSes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer auditId;

    @Column(name = "ser_sale_order_id")
    private Integer serSaleOrderId;

    @Column(name = "procurement_No")
    private String procurementNo;

    public String getProcurementNo() {
        return procurementNo;
    }

    public void setProcurementNo(String procurementNo) {
        this.procurementNo = procurementNo;
    }

    @Lob
    @Column(name = "txt_request")
    private String txtRequest;

    @Lob
    @Column(name = "txt_response")
    private String txtResponse;


    @Column(name = "txt_msgType")
    private String msgType;

    public String getMsgType() {
        return msgType;
    }

    @Column(name="dte_createddate")
    private Timestamp dteCreateddate;


    @Column(name="ser_created_user_id")
    private Integer serCreatedUserId;

    public Integer getSerCreatedUserId() {
        return serCreatedUserId;
    }

    public void setSerCreatedUserId(Integer serCreatedUserId) {
        this.serCreatedUserId = serCreatedUserId;
    }

    public Timestamp getDteCreateddate() {
        return dteCreateddate;
    }

    public void setDteCreateddate(Timestamp dteCreateddate) {
        this.dteCreateddate = dteCreateddate;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Integer getAuditId() {
        return auditId;
    }

    public void setAuditId(Integer auditId) {
        this.auditId = auditId;
    }

    public Integer getSerSaleOrderId() {
        return serSaleOrderId;
    }

    public void setSerSaleOrderId(Integer serSaleOrderId) {
        this.serSaleOrderId = serSaleOrderId;
    }

    public String getTxtRequest() {
        return txtRequest;
    }

    public void setTxtRequest(String txtRequest) {
        this.txtRequest = txtRequest;
    }

    public String getTxtResponse() {
        return txtResponse;
    }

    public void setTxtResponse(String txtResponse) {
        this.txtResponse = txtResponse;
    }

    /* @Column(name = "txt_deal_no")
    private String txtDealNo;*/

   /* @Column(name = "txt_sap_no")
    private String txtSapNo;*/

    /*@Column(name = "dte_changed_date")
    private Timestamp dteChangedDate;


    @Column(name = "txt_Level_audit")
    private String txtLevel;

    @Column(name = "txt_nextLevel")
    private String txtNextLevel;

    @Column(name = "txt_status")
    private String txtStatus;

    @Column(name = "txt_remarks")
    private String txtRemarks;

    @Column(name = "txt_department")
    private String txtDepartment;


    @Column(name="ser_approvedby_id1")
    private Integer serApprovedbyId;



    @Column(name="approvedby_name")
    private String approvedNAME;


    public Boolean getBlIsVendor() {
        return blIsVendor;
    }

    public void setBlIsVendor(Boolean blIsVendor) {
        this.blIsVendor = blIsVendor;
    }

    @Column(name="bl_is_vendor")
    private Boolean blIsVendor;

    public Integer getSerApprovedbyId() {
        return serApprovedbyId;
    }

    public void setSerApprovedbyId(Integer serApprovedbyId) {
        this.serApprovedbyId = serApprovedbyId;
    }



    public String getTxtLevel() {
        return txtLevel;
    }

    public void setTxtLevel(String txtLevel) {
        this.txtLevel = txtLevel;
    }

    public String getTxtDepartment() {
        return txtDepartment;
    }

    public void setTxtDepartment(String txtDepartment) {
        this.txtDepartment = txtDepartment;
    }

    // Getters and Setters
    public Integer getAuditId() {
        return auditId;
    }

    public void setAuditId(Integer auditId) {
        this.auditId = auditId;
    }

    public Integer getSerSaleOrderId() {
        return serSaleOrderId;
    }

    public void setSerSaleOrderId(Integer serSaleOrderId) {
        this.serSaleOrderId = serSaleOrderId;
    }

    public String getTxtDealNo() {
        return txtDealNo;
    }

    public void setTxtDealNo(String txtDealNo) {
        this.txtDealNo = txtDealNo;
    }

    public String getTxtSapNo() {
        return txtSapNo;
    }

    public void setTxtSapNo(String txtSapNo) {
        this.txtSapNo = txtSapNo;
    }


    public Timestamp getDteChangedDate() {
        return dteChangedDate;
    }

    public void setDteChangedDate(Timestamp dteChangedDate) {
        this.dteChangedDate = dteChangedDate;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public String getTxtRemarks() {
        return txtRemarks;
    }

    public void setTxtRemarks(String txtRemarks) {
        this.txtRemarks = txtRemarks;
    }

    public String getTxtNextLevel() {
        return txtNextLevel;
    }

    public void setTxtNextLevel(String txtNextLevel) {
        this.txtNextLevel = txtNextLevel;
    }

    public String getApprovedNAME() {
        return approvedNAME;
    }

    public void setApprovedNAME(String approvedNAME) {
        this.approvedNAME = approvedNAME;
    }*/
}
