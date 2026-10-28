package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_email_template")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblEmailTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_email_template_id")
    private Integer serEmailTemplateId;

    @Column(name = "txt_template_code", nullable = false, length = 64)
    private String txtTemplateCode;

    @Column(name = "txt_template_name", nullable = false, length = 200)
    private String txtTemplateName;

    @Column(name = "txt_description", length = 500)
    private String txtDescription;

    @Column(name = "txt_subject", nullable = false, length = 500)
    private String txtSubject;

    @Column(name = "txt_body_html", nullable = false, columnDefinition = "LONGTEXT")
    private String txtBodyHtml;

    @Column(name = "ser_email_layout_id")
    private Integer serEmailLayoutId;

    @Column(name = "txt_placeholder_help", columnDefinition = "LONGTEXT")
    private String txtPlaceholderHelp;

    @Column(name = "txt_category", length = 64)
    private String txtCategory;

    @Column(name = "int_sort_order", nullable = false)
    private int intSortOrder = 100;

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

    public Integer getSerEmailTemplateId() {
        return serEmailTemplateId;
    }

    public void setSerEmailTemplateId(Integer serEmailTemplateId) {
        this.serEmailTemplateId = serEmailTemplateId;
    }

    public String getTxtTemplateCode() {
        return txtTemplateCode;
    }

    public void setTxtTemplateCode(String txtTemplateCode) {
        this.txtTemplateCode = txtTemplateCode;
    }

    public String getTxtTemplateName() {
        return txtTemplateName;
    }

    public void setTxtTemplateName(String txtTemplateName) {
        this.txtTemplateName = txtTemplateName;
    }

    public String getTxtDescription() {
        return txtDescription;
    }

    public void setTxtDescription(String txtDescription) {
        this.txtDescription = txtDescription;
    }

    public String getTxtSubject() {
        return txtSubject;
    }

    public void setTxtSubject(String txtSubject) {
        this.txtSubject = txtSubject;
    }

    public String getTxtBodyHtml() {
        return txtBodyHtml;
    }

    public void setTxtBodyHtml(String txtBodyHtml) {
        this.txtBodyHtml = txtBodyHtml;
    }

    public Integer getSerEmailLayoutId() {
        return serEmailLayoutId;
    }

    public void setSerEmailLayoutId(Integer serEmailLayoutId) {
        this.serEmailLayoutId = serEmailLayoutId;
    }

    public String getTxtPlaceholderHelp() {
        return txtPlaceholderHelp;
    }

    public void setTxtPlaceholderHelp(String txtPlaceholderHelp) {
        this.txtPlaceholderHelp = txtPlaceholderHelp;
    }

    public String getTxtCategory() {
        return txtCategory;
    }

    public void setTxtCategory(String txtCategory) {
        this.txtCategory = txtCategory;
    }

    public int getIntSortOrder() {
        return intSortOrder;
    }

    public void setIntSortOrder(int intSortOrder) {
        this.intSortOrder = intSortOrder;
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
