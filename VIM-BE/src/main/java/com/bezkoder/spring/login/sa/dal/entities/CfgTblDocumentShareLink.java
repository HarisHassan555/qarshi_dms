package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_document_share_link")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblDocumentShareLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_document_share_id")
    private Integer serDocumentShareId;

    @Column(name = "ser_application_id", nullable = false)
    private Integer serApplicationId;

    @Column(name = "txt_share_token", nullable = false, length = 64)
    private String txtShareToken;

    @Column(name = "ser_created_by_user_id", nullable = false)
    private Integer serCreatedByUserId;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_expires_date")
    private Timestamp dteExpiresDate;

    @Column(name = "bl_revoked")
    private Boolean blRevoked;

    @Column(name = "dte_revoked_date")
    private Timestamp dteRevokedDate;

    @Column(name = "int_view_count")
    private Integer intViewCount;

    @Column(name = "dte_last_viewed_date")
    private Timestamp dteLastViewedDate;

    @Column(name = "txt_scope", nullable = false, length = 32)
    private String txtScope;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    public Integer getSerDocumentShareId() {
        return serDocumentShareId;
    }

    public void setSerDocumentShareId(Integer serDocumentShareId) {
        this.serDocumentShareId = serDocumentShareId;
    }

    public Integer getSerApplicationId() {
        return serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public String getTxtShareToken() {
        return txtShareToken;
    }

    public void setTxtShareToken(String txtShareToken) {
        this.txtShareToken = txtShareToken;
    }

    public Integer getSerCreatedByUserId() {
        return serCreatedByUserId;
    }

    public void setSerCreatedByUserId(Integer serCreatedByUserId) {
        this.serCreatedByUserId = serCreatedByUserId;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteExpiresDate() {
        return dteExpiresDate;
    }

    public void setDteExpiresDate(Timestamp dteExpiresDate) {
        this.dteExpiresDate = dteExpiresDate;
    }

    public Boolean getBlRevoked() {
        return blRevoked;
    }

    public void setBlRevoked(Boolean blRevoked) {
        this.blRevoked = blRevoked;
    }

    public Timestamp getDteRevokedDate() {
        return dteRevokedDate;
    }

    public void setDteRevokedDate(Timestamp dteRevokedDate) {
        this.dteRevokedDate = dteRevokedDate;
    }

    public Integer getIntViewCount() {
        return intViewCount;
    }

    public void setIntViewCount(Integer intViewCount) {
        this.intViewCount = intViewCount;
    }

    public Timestamp getDteLastViewedDate() {
        return dteLastViewedDate;
    }

    public void setDteLastViewedDate(Timestamp dteLastViewedDate) {
        this.dteLastViewedDate = dteLastViewedDate;
    }

    public String getTxtScope() {
        return txtScope;
    }

    public void setTxtScope(String txtScope) {
        this.txtScope = txtScope;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }
}
