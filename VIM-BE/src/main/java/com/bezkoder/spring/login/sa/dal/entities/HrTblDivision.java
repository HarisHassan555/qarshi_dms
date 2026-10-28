package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "hr_tbl_division")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HrTblDivision implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_division_id")
    private Integer serDivisionId;

    @Column(name = "ser_subarea_id", nullable = false)
    private Integer serSubareaId;

    @Column(name = "txt_division_name", nullable = false)
    private String txtDivisionName;

    @Column(name = "txt_division_code", length = 64)
    private String txtDivisionCode;

    @Column(name = "bln_status")
    private Boolean blnStatus;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ser_subarea_id", insertable = false, updatable = false)
    private HrTblSubarea hrTblSubarea;

    public Integer getSerDivisionId() {
        return serDivisionId;
    }

    public void setSerDivisionId(Integer serDivisionId) {
        this.serDivisionId = serDivisionId;
    }

    public Integer getSerSubareaId() {
        return serSubareaId;
    }

    public void setSerSubareaId(Integer serSubareaId) {
        this.serSubareaId = serSubareaId;
    }

    public String getTxtDivisionName() {
        return txtDivisionName;
    }

    public void setTxtDivisionName(String txtDivisionName) {
        this.txtDivisionName = txtDivisionName;
    }

    public String getTxtDivisionCode() {
        return txtDivisionCode;
    }

    public void setTxtDivisionCode(String txtDivisionCode) {
        this.txtDivisionCode = txtDivisionCode;
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

    public HrTblSubarea getHrTblSubarea() {
        return hrTblSubarea;
    }

    public void setHrTblSubarea(HrTblSubarea hrTblSubarea) {
        this.hrTblSubarea = hrTblSubarea;
    }
}
