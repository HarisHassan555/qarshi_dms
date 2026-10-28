package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "hr_tbl_organization")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HrTblOrganization implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_organization_id")
    private Integer serOrganizationId;

    @Column(name = "logo", length = 512)
    private String logo;

    @Column(name = "txt_organization_name", nullable = false)
    private String txtOrganizationName;

    @Column(name = "txt_organization_code", length = 64)
    private String txtOrganizationCode;

    @Column(name = "txt_address", length = 512)
    private String txtAddress;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    public Integer getSerOrganizationId() {
        return serOrganizationId;
    }

    public void setSerOrganizationId(Integer serOrganizationId) {
        this.serOrganizationId = serOrganizationId;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getTxtOrganizationName() {
        return txtOrganizationName;
    }

    public void setTxtOrganizationName(String txtOrganizationName) {
        this.txtOrganizationName = txtOrganizationName;
    }

    public String getTxtOrganizationCode() {
        return txtOrganizationCode;
    }

    public void setTxtOrganizationCode(String txtOrganizationCode) {
        this.txtOrganizationCode = txtOrganizationCode;
    }

    public String getTxtAddress() {
        return txtAddress;
    }

    public void setTxtAddress(String txtAddress) {
        this.txtAddress = txtAddress;
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
}
