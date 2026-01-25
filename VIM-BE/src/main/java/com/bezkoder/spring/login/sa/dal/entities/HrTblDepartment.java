package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;


/**
 * The persistent class for the hr_tbl_department database table.
 * 
 */
@Entity
@Table(name="hr_tbl_department")
@NamedQuery(name="HrTblDepartment.findAll", query="SELECT h FROM HrTblDepartment h")
public class HrTblDepartment implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_department_id")
	private Integer serDepartmentId;

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

	@Column(name="ser_parent_department_id")
	private Integer serParentDepartmentId;

	@Column(name="ser_department_head_id")
	private Integer serDepartmentHeadId;

	//bi-directional many-to-one association to CfgTblUser (Department Head)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ser_department_head_id", insertable = false, updatable = false)
	@JsonIgnoreProperties({"txtPassword", "cfgTblUserRoles", "cfgTblManager", "cfgTblPasswordPolicy", "cfgTblCustomer", "hrTblDepartment"})
	private CfgTblUser departmentHead;

	@Column(name="txt_department_code")
	private String txtDepartmentCode;

	@Column(name="txt_department_name")
	private String txtDepartmentName;

	@Column(name="txt_description")
	private String txtDescription;

	//bi-directional many-to-one association to HrTblEmployee
	@OneToMany(mappedBy="hrTblDepartment", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<HrTblEmployee> hrTblEmployees;

	//bi-directional many-to-one association to CfgTblUser
	@OneToMany(mappedBy="hrTblDepartment", fetch = FetchType.LAZY)
	@JsonManagedReference
	@JsonIgnoreProperties({"txtPassword", "cfgTblUserRoles", "cfgTblManager", "cfgTblPasswordPolicy", "cfgTblCustomer", "hrTblDepartment"})
	private List<CfgTblUser> cfgTblUsers;

	public HrTblDepartment() {
	}

	public Integer getSerDepartmentId() {
		return this.serDepartmentId;
	}

	public void setSerDepartmentId(Integer serDepartmentId) {
		this.serDepartmentId = serDepartmentId;
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

	public Integer getSerParentDepartmentId() {
		return this.serParentDepartmentId;
	}

	public void setSerParentDepartmentId(Integer serParentDepartmentId) {
		this.serParentDepartmentId = serParentDepartmentId;
	}

	public Integer getSerDepartmentHeadId() {
		return this.serDepartmentHeadId;
	}

	public void setSerDepartmentHeadId(Integer serDepartmentHeadId) {
		this.serDepartmentHeadId = serDepartmentHeadId;
	}

	public CfgTblUser getDepartmentHead() {
		return this.departmentHead;
	}

	public void setDepartmentHead(CfgTblUser departmentHead) {
		this.departmentHead = departmentHead;
	}

	public String getTxtDepartmentCode() {
		return this.txtDepartmentCode;
	}

	public void setTxtDepartmentCode(String txtDepartmentCode) {
		this.txtDepartmentCode = txtDepartmentCode;
	}

	public String getTxtDepartmentName() {
		return this.txtDepartmentName;
	}

	public void setTxtDepartmentName(String txtDepartmentName) {
		this.txtDepartmentName = txtDepartmentName;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public List<HrTblEmployee> getHrTblEmployees() {
		return this.hrTblEmployees;
	}

	public void setHrTblEmployees(List<HrTblEmployee> hrTblEmployees) {
		this.hrTblEmployees = hrTblEmployees;
	}

	public HrTblEmployee addHrTblEmployee(HrTblEmployee hrTblEmployee) {
		getHrTblEmployees().add(hrTblEmployee);
		hrTblEmployee.setHrTblDepartment(this);

		return hrTblEmployee;
	}

	public HrTblEmployee removeHrTblEmployee(HrTblEmployee hrTblEmployee) {
		getHrTblEmployees().remove(hrTblEmployee);
		hrTblEmployee.setHrTblDepartment(null);

		return hrTblEmployee;
	}

	public List<CfgTblUser> getCfgTblUsers() {
		return this.cfgTblUsers;
	}

	public void setCfgTblUsers(List<CfgTblUser> cfgTblUsers) {
		this.cfgTblUsers = cfgTblUsers;
	}

	public CfgTblUser addCfgTblUser(CfgTblUser cfgTblUser) {
		getCfgTblUsers().add(cfgTblUser);
		cfgTblUser.setHrTblDepartment(this);

		return cfgTblUser;
	}

	public CfgTblUser removeCfgTblUser(CfgTblUser cfgTblUser) {
		getCfgTblUsers().remove(cfgTblUser);
		cfgTblUser.setHrTblDepartment(null);

		return cfgTblUser;
	}

}