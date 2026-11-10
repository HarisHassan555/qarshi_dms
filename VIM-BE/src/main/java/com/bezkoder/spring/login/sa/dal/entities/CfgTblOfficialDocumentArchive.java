package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_official_document_archive")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblOfficialDocumentArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_official_document_id")
    private Integer serOfficialDocumentId;

    @Column(name = "txt_title", length = 500)
    private String txtTitle;

    @Column(name = "txt_summary", columnDefinition = "TEXT")
    private String txtSummary;

    @Column(name = "txt_label", length = 80)
    private String txtLabel;

    @Column(name = "txt_original_file_name", length = 500)
    private String txtOriginalFileName;

    @Column(name = "txt_stored_path", length = 500)
    private String txtStoredPath;

    @Column(name = "txt_mime_type", length = 120)
    private String txtMimeType;

    @Column(name = "int_page_count")
    private Integer intPageCount;

    @Column(name = "bl_ai_analyzed")
    private Boolean blAiAnalyzed;

    @Column(name = "ser_uploaded_by_user_id")
    private Integer serUploadedByUserId;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    public Integer getSerOfficialDocumentId() {
        return serOfficialDocumentId;
    }

    public void setSerOfficialDocumentId(Integer serOfficialDocumentId) {
        this.serOfficialDocumentId = serOfficialDocumentId;
    }

    public String getTxtTitle() {
        return txtTitle;
    }

    public void setTxtTitle(String txtTitle) {
        this.txtTitle = txtTitle;
    }

    public String getTxtSummary() {
        return txtSummary;
    }

    public void setTxtSummary(String txtSummary) {
        this.txtSummary = txtSummary;
    }

    public String getTxtLabel() {
        return txtLabel;
    }

    public void setTxtLabel(String txtLabel) {
        this.txtLabel = txtLabel;
    }

    public String getTxtOriginalFileName() {
        return txtOriginalFileName;
    }

    public void setTxtOriginalFileName(String txtOriginalFileName) {
        this.txtOriginalFileName = txtOriginalFileName;
    }

    public String getTxtStoredPath() {
        return txtStoredPath;
    }

    public void setTxtStoredPath(String txtStoredPath) {
        this.txtStoredPath = txtStoredPath;
    }

    public String getTxtMimeType() {
        return txtMimeType;
    }

    public void setTxtMimeType(String txtMimeType) {
        this.txtMimeType = txtMimeType;
    }

    public Integer getIntPageCount() {
        return intPageCount;
    }

    public void setIntPageCount(Integer intPageCount) {
        this.intPageCount = intPageCount;
    }

    public Boolean getBlAiAnalyzed() {
        return blAiAnalyzed;
    }

    public void setBlAiAnalyzed(Boolean blAiAnalyzed) {
        this.blAiAnalyzed = blAiAnalyzed;
    }

    public Integer getSerUploadedByUserId() {
        return serUploadedByUserId;
    }

    public void setSerUploadedByUserId(Integer serUploadedByUserId) {
        this.serUploadedByUserId = serUploadedByUserId;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }
}
