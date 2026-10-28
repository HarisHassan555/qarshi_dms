package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_background_job")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblBackgroundJob implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_background_job_id")
    private Long serBackgroundJobId;

    @Column(name = "txt_job_type", nullable = false, length = 64)
    private String txtJobType;

    @Column(name = "txt_idempotency_key", nullable = false, length = 191, unique = true)
    private String txtIdempotencyKey;

    @Column(name = "txt_payload", columnDefinition = "LONGTEXT")
    private String txtPayload;

    @Column(name = "txt_status", nullable = false, length = 32)
    private String txtStatus;

    @Column(name = "int_attempts", nullable = false)
    private int intAttempts;

    @Column(name = "int_max_attempts", nullable = false)
    private int intMaxAttempts;

    @Column(name = "dte_run_after", nullable = false)
    private Timestamp dteRunAfter;

    @Column(name = "dte_locked_at")
    private Timestamp dteLockedAt;

    @Column(name = "txt_locked_by", length = 128)
    private String txtLockedBy;

    @Column(name = "txt_last_error", columnDefinition = "LONGTEXT")
    private String txtLastError;

    @Column(name = "dte_started_at")
    private Timestamp dteStartedAt;

    @Column(name = "dte_completed_at")
    private Timestamp dteCompletedAt;

    @Column(name = "ser_created_user_id")
    private Integer serCreatedUserId;

    @Column(name = "bl_is_deleted", nullable = false)
    private boolean blIsDeleted;

    @Column(name = "bln_status", nullable = false)
    private boolean blnStatus;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    public Long getSerBackgroundJobId() {
        return serBackgroundJobId;
    }

    public void setSerBackgroundJobId(Long serBackgroundJobId) {
        this.serBackgroundJobId = serBackgroundJobId;
    }

    public String getTxtJobType() {
        return txtJobType;
    }

    public void setTxtJobType(String txtJobType) {
        this.txtJobType = txtJobType;
    }

    public String getTxtIdempotencyKey() {
        return txtIdempotencyKey;
    }

    public void setTxtIdempotencyKey(String txtIdempotencyKey) {
        this.txtIdempotencyKey = txtIdempotencyKey;
    }

    public String getTxtPayload() {
        return txtPayload;
    }

    public void setTxtPayload(String txtPayload) {
        this.txtPayload = txtPayload;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public int getIntAttempts() {
        return intAttempts;
    }

    public void setIntAttempts(int intAttempts) {
        this.intAttempts = intAttempts;
    }

    public int getIntMaxAttempts() {
        return intMaxAttempts;
    }

    public void setIntMaxAttempts(int intMaxAttempts) {
        this.intMaxAttempts = intMaxAttempts;
    }

    public Timestamp getDteRunAfter() {
        return dteRunAfter;
    }

    public void setDteRunAfter(Timestamp dteRunAfter) {
        this.dteRunAfter = dteRunAfter;
    }

    public Timestamp getDteLockedAt() {
        return dteLockedAt;
    }

    public void setDteLockedAt(Timestamp dteLockedAt) {
        this.dteLockedAt = dteLockedAt;
    }

    public String getTxtLockedBy() {
        return txtLockedBy;
    }

    public void setTxtLockedBy(String txtLockedBy) {
        this.txtLockedBy = txtLockedBy;
    }

    public String getTxtLastError() {
        return txtLastError;
    }

    public void setTxtLastError(String txtLastError) {
        this.txtLastError = txtLastError;
    }

    public Timestamp getDteStartedAt() {
        return dteStartedAt;
    }

    public void setDteStartedAt(Timestamp dteStartedAt) {
        this.dteStartedAt = dteStartedAt;
    }

    public Timestamp getDteCompletedAt() {
        return dteCompletedAt;
    }

    public void setDteCompletedAt(Timestamp dteCompletedAt) {
        this.dteCompletedAt = dteCompletedAt;
    }

    public Integer getSerCreatedUserId() {
        return serCreatedUserId;
    }

    public void setSerCreatedUserId(Integer serCreatedUserId) {
        this.serCreatedUserId = serCreatedUserId;
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
}
