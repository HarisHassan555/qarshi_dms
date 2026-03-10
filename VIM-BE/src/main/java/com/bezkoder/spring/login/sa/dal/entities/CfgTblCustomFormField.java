package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * The persistent class for the cfg_tbl_custom_form_field database table.
 */
@Entity
@Table(name="cfg_tbl_custom_form_field")
@NamedQuery(name="CfgTblCustomFormField.findAll", query="SELECT c FROM CfgTblCustomFormField c")
public class CfgTblCustomFormField implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ser_field_id")
    private Integer serFieldId;

    @Column(name="txt_field_label")
    private String txtFieldLabel;

    @Column(name="txt_field_type")
    private String txtFieldType;

    @Column(name="txt_placeholder")
    private String txtPlaceholder;

    @Column(name="bl_is_required")
    private Boolean blIsRequired;

    @Column(name="int_field_order")
    private Integer intFieldOrder;

    @Lob
    @Column(name="txt_field_options", columnDefinition = "LONGTEXT")
    private String txtFieldOptions;

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
    @JsonBackReference
    private CfgTblCustomForm cfgTblCustomForm;

    public CfgTblCustomFormField() {
    }

    public Integer getSerFieldId() {
        return this.serFieldId;
    }

    public void setSerFieldId(Integer serFieldId) {
        this.serFieldId = serFieldId;
    }

    public String getTxtFieldLabel() {
        return this.txtFieldLabel;
    }

    public void setTxtFieldLabel(String txtFieldLabel) {
        this.txtFieldLabel = txtFieldLabel;
    }

    public String getTxtFieldType() {
        return this.txtFieldType;
    }

    public void setTxtFieldType(String txtFieldType) {
        this.txtFieldType = txtFieldType;
    }

    public String getTxtPlaceholder() {
        return this.txtPlaceholder;
    }

    public void setTxtPlaceholder(String txtPlaceholder) {
        this.txtPlaceholder = txtPlaceholder;
    }

    public Boolean getBlIsRequired() {
        return this.blIsRequired;
    }

    public void setBlIsRequired(Boolean blIsRequired) {
        this.blIsRequired = blIsRequired;
    }

    public Integer getIntFieldOrder() {
        return this.intFieldOrder;
    }

    public void setIntFieldOrder(Integer intFieldOrder) {
        this.intFieldOrder = intFieldOrder;
    }

    public String getTxtFieldOptions() {
        return this.txtFieldOptions;
    }

    public void setTxtFieldOptions(String txtFieldOptions) {
        this.txtFieldOptions = txtFieldOptions;
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
}
