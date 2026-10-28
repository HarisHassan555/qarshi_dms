package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_user_delegation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblUserDelegation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_delegation_id")
    private Integer serDelegationId;

    @Column(name = "ser_delegate_user_id", nullable = false)
    private Integer serDelegateUserId;

    @Column(name = "ser_on_behalf_user_id", nullable = false)
    private Integer serOnBehalfUserId;

    @Column(name = "dte_valid_from", nullable = false)
    private Date dteValidFrom;

    @Column(name = "dte_valid_to", nullable = false)
    private Date dteValidTo;

    @Column(name = "txt_scope", nullable = false, length = 512)
    private String txtScope;

    @Column(name = "txt_notes", length = 2000)
    private String txtNotes;

    @Column(name = "txt_attachment_path", length = 512)
    private String txtAttachmentPath;

    @Column(name = "txt_attachment_name", length = 255)
    private String txtAttachmentName;

    @Column(name = "txt_status", nullable = false, length = 32)
    private String txtStatus;

    @Column(name = "dte_revoked_date")
    private Timestamp dteRevokedDate;

    @Column(name = "ser_revoked_by_user_id")
    private Integer serRevokedByUserId;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @Column(name = "ser_created_user_id")
    private Integer serCreatedUserId;

    @Column(name = "ser_modified_user_id")
    private Integer serModifiedUserId;

    public Integer getSerDelegationId() {
        return serDelegationId;
    }

    public void setSerDelegationId(Integer serDelegationId) {
        this.serDelegationId = serDelegationId;
    }

    public Integer getSerDelegateUserId() {
        return serDelegateUserId;
    }

    public void setSerDelegateUserId(Integer serDelegateUserId) {
        this.serDelegateUserId = serDelegateUserId;
    }

    public Integer getSerOnBehalfUserId() {
        return serOnBehalfUserId;
    }

    public void setSerOnBehalfUserId(Integer serOnBehalfUserId) {
        this.serOnBehalfUserId = serOnBehalfUserId;
    }

    public Date getDteValidFrom() {
        return dteValidFrom;
    }

    public void setDteValidFrom(Date dteValidFrom) {
        this.dteValidFrom = dteValidFrom;
    }

    public Date getDteValidTo() {
        return dteValidTo;
    }

    public void setDteValidTo(Date dteValidTo) {
        this.dteValidTo = dteValidTo;
    }

    public String getTxtScope() {
        return txtScope;
    }

    public void setTxtScope(String txtScope) {
        this.txtScope = txtScope;
    }

    public String getTxtNotes() {
        return txtNotes;
    }

    public void setTxtNotes(String txtNotes) {
        this.txtNotes = txtNotes;
    }

    public String getTxtAttachmentPath() {
        return txtAttachmentPath;
    }

    public void setTxtAttachmentPath(String txtAttachmentPath) {
        this.txtAttachmentPath = txtAttachmentPath;
    }

    public String getTxtAttachmentName() {
        return txtAttachmentName;
    }

    public void setTxtAttachmentName(String txtAttachmentName) {
        this.txtAttachmentName = txtAttachmentName;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public Timestamp getDteRevokedDate() {
        return dteRevokedDate;
    }

    public void setDteRevokedDate(Timestamp dteRevokedDate) {
        this.dteRevokedDate = dteRevokedDate;
    }

    public Integer getSerRevokedByUserId() {
        return serRevokedByUserId;
    }

    public void setSerRevokedByUserId(Integer serRevokedByUserId) {
        this.serRevokedByUserId = serRevokedByUserId;
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
