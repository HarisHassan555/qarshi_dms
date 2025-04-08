package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.*;
import java.util.Date;
import java.sql.Timestamp;


/**
 * The persistent class for the hr_tbl_employee database table.
 * 
 */
@Entity
@Table(name="hr_tbl_employee")
@NamedQuery(name="HrTblEmployee.findAll", query="SELECT h FROM HrTblEmployee h")
public class HrTblEmployee implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_employee_id")
	private Integer serEmployeeId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_cnic_expired_date")
	private Date dteCnicExpiredDate;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	
	@Column(name="dte_dob")
	private Date dteDob;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_joining_date")
	private Date dteJoiningDate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_licence_expired_date")
	private Date dteLicenceExpiredDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="ser_parent_department_id")
	private Integer serParentDepartmentId;

	@Column(name="txt_address")
	private String txtAddress;

	@Column(name="txt_cnic")
	private String txtCnic;

	@Column(name="txt_contact_no")
	private String txtContactNo;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_email")
	private String txtEmail;

	@Column(name="txt_employee_code")
	private String txtEmployeeCode;

	@Column(name="txt_employee_name")
	private String txtEmployeeName;

	@Column(name="txt_job_type")
	private String txtJobType;

	@Column(name="txt_marital_status")
	private String txtMaritalStatus;

	@Column(name="txt_mobile_no")
	private String txtMobileNo;

	@Column(name="txt_so_do_wo")
	private String txtSoDoWo;

	//bi-directional many-to-one association to HrTblDepartment
	@ManyToOne
	@JoinColumn(name="ser_department_id")
	private HrTblDepartment hrTblDepartment;

	//bi-directional many-to-one association to HrTblDesignation
	@ManyToOne
	@JoinColumn(name="ser_designation_id")
	private HrTblDesignation hrTblDesignation;

	//bi-directional many-to-one association to SlsTblLedger
/*	@OneToMany(mappedBy="hrTblEmployee")
	private List<SlsTblLedger> slsTblLedgers;*/
	
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;
	
	@ManyToOne
	@JoinColumn(name="ser_supplier_id")
	private CfgTblSupplier cfgTblSupplier;
	
	
	
	@Column(name="txt_account_no")
	private String txtAccountNo;
	
	@Column(name="txt_account_title")
	private String txtAccountTitle;
	
	@Column(name="txt_iban")
	private String txtIBAN;
	
	@ManyToOne
	@JoinColumn(name="ser_job_id")
	private HrTblJob job;
	
	@Column(name="txt_status")
	private String txtStatus;
	
	//bi-directional many-to-one association to cfgTblArea
	@ManyToOne
	@JoinColumn(name="ser_area_id")
	private CfgTblArea cfgTblArea;

	public String getTxtAccountNo() {
		return txtAccountNo;
	}

	public void setTxtAccountNo(String txtAccountNo) {
		this.txtAccountNo = txtAccountNo;
	}

	public String getTxtAccountTitle() {
		return txtAccountTitle;
	}

	public void setTxtAccountTitle(String txtAccountTitle) {
		this.txtAccountTitle = txtAccountTitle;
	}

	public String getTxtIBAN() {
		return txtIBAN;
	}

	public void setTxtIBAN(String txtIBAN) {
		this.txtIBAN = txtIBAN;
	}


	
	
	public CfgTblSupplier getCfgTblSupplier() {
		return cfgTblSupplier;
	}

	public void setCfgTblSupplier(CfgTblSupplier cfgTblSupplier) {
		this.cfgTblSupplier = cfgTblSupplier;
	}

	

	public CfgTblCity getCfgTblCity() {
		return cfgTblCity;
	}

	public void setCfgTblCity(CfgTblCity cfgTblCity) {
		this.cfgTblCity = cfgTblCity;
	}

	public HrTblEmployee() {
	}

	public Integer getSerEmployeeId() {
		return this.serEmployeeId;
	}

	public void setSerEmployeeId(Integer serEmployeeId) {
		this.serEmployeeId = serEmployeeId;
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

	public Date getDteCnicExpiredDate() {
		return this.dteCnicExpiredDate;
	}

	public void setDteCnicExpiredDate(Date dteCnicExpiredDate) {
		this.dteCnicExpiredDate = dteCnicExpiredDate;
	}

	public Timestamp getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Timestamp dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Date getDteDob() {
		return this.dteDob;
	}

	public void setDteDob(Date dteDob) {
		this.dteDob = dteDob;
	}

	public Date getDteJoiningDate() {
		return this.dteJoiningDate;
	}

	public void setDteJoiningDate(Date dteJoiningDate) {
		this.dteJoiningDate = dteJoiningDate;
	}

	public Date getDteLicenceExpiredDate() {
		return this.dteLicenceExpiredDate;
	}

	public void setDteLicenceExpiredDate(Date dteLicenceExpiredDate) {
		this.dteLicenceExpiredDate = dteLicenceExpiredDate;
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

	public String getTxtAddress() {
		return this.txtAddress;
	}

	public void setTxtAddress(String txtAddress) {
		this.txtAddress = txtAddress;
	}

	public String getTxtCnic() {
		return this.txtCnic;
	}

	public void setTxtCnic(String txtCnic) {
		this.txtCnic = txtCnic;
	}

	public String getTxtContactNo() {
		return this.txtContactNo;
	}

	public void setTxtContactNo(String txtContactNo) {
		this.txtContactNo = txtContactNo;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtEmail() {
		return this.txtEmail;
	}

	public void setTxtEmail(String txtEmail) {
		this.txtEmail = txtEmail;
	}

	public String getTxtEmployeeCode() {
		return this.txtEmployeeCode;
	}

	public void setTxtEmployeeCode(String txtEmployeeCode) {
		this.txtEmployeeCode = txtEmployeeCode;
	}

	public String getTxtEmployeeName() {
		return this.txtEmployeeName;
	}

	public void setTxtEmployeeName(String txtEmployeeName) {
		this.txtEmployeeName = txtEmployeeName;
	}

	public String getTxtJobType() {
		return this.txtJobType;
	}

	public void setTxtJobType(String txtJobType) {
		this.txtJobType = txtJobType;
	}

	public String getTxtMaritalStatus() {
		return this.txtMaritalStatus;
	}

	public void setTxtMaritalStatus(String txtMaritalStatus) {
		this.txtMaritalStatus = txtMaritalStatus;
	}

	public String getTxtMobileNo() {
		return this.txtMobileNo;
	}

	public void setTxtMobileNo(String txtMobileNo) {
		this.txtMobileNo = txtMobileNo;
	}

	public String getTxtSoDoWo() {
		return this.txtSoDoWo;
	}

	public void setTxtSoDoWo(String txtSoDoWo) {
		this.txtSoDoWo = txtSoDoWo;
	}

	public HrTblDepartment getHrTblDepartment() {
		return this.hrTblDepartment;
	}

	public void setHrTblDepartment(HrTblDepartment hrTblDepartment) {
		this.hrTblDepartment = hrTblDepartment;
	}

	public HrTblDesignation getHrTblDesignation() {
		return this.hrTblDesignation;
	}

	public void setHrTblDesignation(HrTblDesignation hrTblDesignation) {
		this.hrTblDesignation = hrTblDesignation;
	}

/*	public List<SlsTblLedger> getSlsTblLedgers() {
		return this.slsTblLedgers;
	}

	public void setSlsTblLedgers(List<SlsTblLedger> slsTblLedgers) {
		this.slsTblLedgers = slsTblLedgers;
	}*/

	/*public SlsTblLedger addSlsTblLedger(SlsTblLedger slsTblLedger) {
		getSlsTblLedgers().add(slsTblLedger);
		slsTblLedger.setHrTblEmployee(this);

		return slsTblLedger;
	}

	public SlsTblLedger removeSlsTblLedger(SlsTblLedger slsTblLedger) {
		getSlsTblLedgers().remove(slsTblLedger);
		slsTblLedger.setHrTblEmployee(null);

		return slsTblLedger;
	}*/

	

	@Column(name="txt_father_name")
	private String txtFatherName;



	public String getTxtFatherName() {
		return txtFatherName;
	}

	public void setTxtFatherName(String txtFatherName) {
		this.txtFatherName = txtFatherName;
	}
	
	@ManyToOne
	@JoinColumn(name="ser_shift_info_id")
	private HrTblShiftInfo hrTblShiftInfo;



	public HrTblShiftInfo getHrTblShiftInfo() {
		return hrTblShiftInfo;
	}

	public void setHrTblShiftInfo(HrTblShiftInfo hrTblShiftInfo) {
		this.hrTblShiftInfo = hrTblShiftInfo;
	}
	
	
	@Column(name="num_rate")
	private BigDecimal numRate;



	public BigDecimal getNumRate() {
		return numRate;
	}

	public void setNumRate(BigDecimal numRate) {
		this.numRate = numRate;
	}
	
	public HrTblJob getJob() {
		return job;
	}

	public void setJob(HrTblJob job) {
		this.job = job;
	}
	

	public String getTxtStatus() {
		return txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	public CfgTblArea getCfgTblArea() {
		return cfgTblArea;
	}

	public void setCfgTblArea(CfgTblArea cfgTblArea) {
		this.cfgTblArea = cfgTblArea;
	}

}