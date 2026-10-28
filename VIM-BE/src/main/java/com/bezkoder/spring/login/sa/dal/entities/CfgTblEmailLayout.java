package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_email_layout")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblEmailLayout implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_email_layout_id")
    private Integer serEmailLayoutId;

    @Column(name = "txt_layout_code", nullable = false, length = 64)
    private String txtLayoutCode;

    @Column(name = "txt_layout_name", nullable = false, length = 200)
    private String txtLayoutName;

    @Column(name = "txt_description", length = 500)
    private String txtDescription;

    @Column(name = "txt_html_wrapper", nullable = false, columnDefinition = "LONGTEXT")
    private String txtHtmlWrapper;

    @Column(name = "bl_is_active", nullable = false)
    private boolean blIsActive = true;

    @Column(name = "bl_is_deleted", nullable = false)
    private boolean blIsDeleted;

    @Column(name = "bln_status", nullable = false)
    private boolean blnStatus = true;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name = "ser_created_user_id")
    private Integer serCreatedUserId;

    @Column(name = "ser_modified_user_id")
    private Integer serModifiedUserId;

    public Integer getSerEmailLayoutId() {
        return serEmailLayoutId;
    }

    public void setSerEmailLayoutId(Integer serEmailLayoutId) {
        this.serEmailLayoutId = serEmailLayoutId;
    }

    public String getTxtLayoutCode() {
        return txtLayoutCode;
    }

    public void setTxtLayoutCode(String txtLayoutCode) {
        this.txtLayoutCode = txtLayoutCode;
    }

    public String getTxtLayoutName() {
        return txtLayoutName;
    }

    public void setTxtLayoutName(String txtLayoutName) {
        this.txtLayoutName = txtLayoutName;
    }

    public String getTxtDescription() {
        return txtDescription;
    }

    public void setTxtDescription(String txtDescription) {
        this.txtDescription = txtDescription;
    }

    public String getTxtHtmlWrapper() {
        return txtHtmlWrapper;
    }

    public void setTxtHtmlWrapper(String txtHtmlWrapper) {
        this.txtHtmlWrapper = txtHtmlWrapper;
    }

    public boolean isBlIsActive() {
        return blIsActive;
    }

    public void setBlIsActive(boolean blIsActive) {
        this.blIsActive = blIsActive;
    }

    public boolean isBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public boolean isBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(boolean blnStatus) {
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
