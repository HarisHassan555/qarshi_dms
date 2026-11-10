package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Data-scope rows for the All Applications audit page.
 * Table: all_application_page_permission
 */
@Entity
@Table(name = "all_application_page_permission")
@NamedQuery(
        name = "AllApplicationPagePermission.findAll",
        query = "SELECT p FROM AllApplicationPagePermission p"
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AllApplicationPagePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_permission_id")
    private Integer serPermissionId;

    @Column(name = "ser_role_id", nullable = false)
    private Integer serRoleId;

    /** Optional viewer user; null = applies to whole role. */
    @Column(name = "ser_user_id")
    private Integer serUserId;

    @Column(name = "ser_department_id")
    private Integer serDepartmentId;

    @Column(name = "ser_submitter_user_id")
    private Integer serSubmitterUserId;

    @Column(name = "ser_approver_user_id")
    private Integer serApproverUserId;

    @Column(name = "ser_form_id")
    private Integer serFormId;

    @Column(name = "ser_application_id")
    private Integer serApplicationId;

    @Column(name = "bl_is_enabled")
    private Boolean blIsEnabled;

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

    public Integer getSerPermissionId() {
        return serPermissionId;
    }

    public void setSerPermissionId(Integer serPermissionId) {
        this.serPermissionId = serPermissionId;
    }

    public Integer getSerRoleId() {
        return serRoleId;
    }

    public void setSerRoleId(Integer serRoleId) {
        this.serRoleId = serRoleId;
    }

    public Integer getSerUserId() {
        return serUserId;
    }

    public void setSerUserId(Integer serUserId) {
        this.serUserId = serUserId;
    }

    public Integer getSerDepartmentId() {
        return serDepartmentId;
    }

    public void setSerDepartmentId(Integer serDepartmentId) {
        this.serDepartmentId = serDepartmentId;
    }

    public Integer getSerSubmitterUserId() {
        return serSubmitterUserId;
    }

    public void setSerSubmitterUserId(Integer serSubmitterUserId) {
        this.serSubmitterUserId = serSubmitterUserId;
    }

    public Integer getSerApproverUserId() {
        return serApproverUserId;
    }

    public void setSerApproverUserId(Integer serApproverUserId) {
        this.serApproverUserId = serApproverUserId;
    }

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public Integer getSerApplicationId() {
        return serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public Boolean getBlIsEnabled() {
        return blIsEnabled;
    }

    public void setBlIsEnabled(Boolean blIsEnabled) {
        this.blIsEnabled = blIsEnabled;
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
