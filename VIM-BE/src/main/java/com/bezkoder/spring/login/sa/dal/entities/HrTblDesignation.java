package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the hr_tbl_designation database table.
 * 
 */
@Entity
@Table(name="hr_tbl_designation")
@NamedQuery(name="HrTblDesignation.findAll", query="SELECT h FROM HrTblDesignation h")
public class HrTblDesignation implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_designation_id")
	private Integer serDesignationId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_designation_code")
	private String txtDesignationCode;

	@Column(name="txt_designation_name")
	private String txtDesignationName;

	//bi-directional many-to-one association to HrTblEmployee
	@OneToMany(mappedBy="hrTblDesignation")
	private List<HrTblEmployee> hrTblEmployees;

	public HrTblDesignation() {
	}

	public Integer getSerDesignationId() {
		return this.serDesignationId;
	}

	public void setSerDesignationId(Integer serDesignationId) {
		this.serDesignationId = serDesignationId;
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

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
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

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtDesignationCode() {
		return this.txtDesignationCode;
	}

	public void setTxtDesignationCode(String txtDesignationCode) {
		this.txtDesignationCode = txtDesignationCode;
	}

	public String getTxtDesignationName() {
		return this.txtDesignationName;
	}

	public void setTxtDesignationName(String txtDesignationName) {
		this.txtDesignationName = txtDesignationName;
	}

	public List<HrTblEmployee> getHrTblEmployees() {
		return this.hrTblEmployees;
	}

	public void setHrTblEmployees(List<HrTblEmployee> hrTblEmployees) {
		this.hrTblEmployees = hrTblEmployees;
	}

	public HrTblEmployee addHrTblEmployee(HrTblEmployee hrTblEmployee) {
		getHrTblEmployees().add(hrTblEmployee);
		hrTblEmployee.setHrTblDesignation(this);

		return hrTblEmployee;
	}

	public HrTblEmployee removeHrTblEmployee(HrTblEmployee hrTblEmployee) {
		getHrTblEmployees().remove(hrTblEmployee);
		hrTblEmployee.setHrTblDesignation(null);

		return hrTblEmployee;
	}

}