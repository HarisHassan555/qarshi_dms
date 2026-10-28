package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "hr_tbl_subarea")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HrTblSubarea implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_subarea_id")
    private Integer serSubareaId;

    @Column(name = "ser_organization_id", nullable = false)
    private Integer serOrganizationId;

    @Column(name = "txt_subarea_name", nullable = false)
    private String txtSubareaName;

    @Column(name = "txt_subarea_code", length = 64)
    private String txtSubareaCode;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ser_organization_id", insertable = false, updatable = false)
    private HrTblOrganization hrTblOrganization;

    public Integer getSerSubareaId() {
        return serSubareaId;
    }

    public void setSerSubareaId(Integer serSubareaId) {
        this.serSubareaId = serSubareaId;
    }

    public Integer getSerOrganizationId() {
        return serOrganizationId;
    }

    public void setSerOrganizationId(Integer serOrganizationId) {
        this.serOrganizationId = serOrganizationId;
    }

    public String getTxtSubareaName() {
        return txtSubareaName;
    }

    public void setTxtSubareaName(String txtSubareaName) {
        this.txtSubareaName = txtSubareaName;
    }

    public String getTxtSubareaCode() {
        return txtSubareaCode;
    }

    public void setTxtSubareaCode(String txtSubareaCode) {
        this.txtSubareaCode = txtSubareaCode;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
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

    public HrTblOrganization getHrTblOrganization() {
        return hrTblOrganization;
    }

    public void setHrTblOrganization(HrTblOrganization hrTblOrganization) {
        this.hrTblOrganization = hrTblOrganization;
    }
}
