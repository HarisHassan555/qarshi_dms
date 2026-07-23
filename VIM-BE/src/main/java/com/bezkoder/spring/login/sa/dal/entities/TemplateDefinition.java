package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "tpl_template_definition")
@NamedQuery(name = "TemplateDefinition.findAll", query = "SELECT t FROM TemplateDefinition t")
public class TemplateDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_template_id")
    private Integer serTemplateId;

    @Column(name = "ser_form_id", unique = true, nullable = false)
    private Integer serFormId;

    @Column(name = "txt_template_name")
    private String txtTemplateName;

    @Column(name = "txt_code_convention")
    private String txtCodeConvention;

    @Lob
    @Column(name = "txt_template_payload", columnDefinition = "LONGTEXT", nullable = false)
    private String txtTemplatePayload;

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

    public Integer getSerTemplateId() {
        return serTemplateId;
    }

    public void setSerTemplateId(Integer serTemplateId) {
        this.serTemplateId = serTemplateId;
    }

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtTemplateName() {
        return txtTemplateName;
    }

    public void setTxtTemplateName(String txtTemplateName) {
        this.txtTemplateName = txtTemplateName;
    }

    public String getTxtCodeConvention() {
        return txtCodeConvention;
    }

    public void setTxtCodeConvention(String txtCodeConvention) {
        this.txtCodeConvention = txtCodeConvention;
    }

    public String getTxtTemplatePayload() {
        return txtTemplatePayload;
    }

    public void setTxtTemplatePayload(String txtTemplatePayload) {
        this.txtTemplatePayload = txtTemplatePayload;
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

    public Integer getSerCreatedUser() {
        return serCreatedUser;
    }

    public void setSerCreatedUser(Integer serCreatedUser) {
        this.serCreatedUser = serCreatedUser;
    }

    public Integer getSerModifiedUser() {
        return serModifiedUser;
    }

    public void setSerModifiedUser(Integer serModifiedUser) {
        this.serModifiedUser = serModifiedUser;
    }
}
