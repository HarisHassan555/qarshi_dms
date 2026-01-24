package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import java.math.BigDecimal;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import javax.persistence.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_user database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_user")
@NamedQuery(name="CfgTblUser.findAll", query="SELECT c FROM CfgTblUser c")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfgTblUser implements Serializable {
	private static final long serialVersionUID = 1L;

	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_User_ser_user_id_seq")
	@SequenceGenerator(name = "cfg_tbl_User_ser_user_id_seq", sequenceName = "cfg_tbl_User_ser_user_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_user_id")
	private Integer serUserId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="num_sub_menu_order")
	private Integer numSubMenuOrder;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="txt_address")
	private String txtAddress;

	@Column(name="txt_cnic")
	private String txtCnic;

	@Column(name="txt_contact_no")
	private String txtContactNo;

	@Column(name="txt_password")
	private String txtPassword;

	@Column(name="txt_user_name")
	private String txtUserName;
	
	@Column(name="txt_role")
	private String txtrole;

	public String getTxtrole() {
		return txtrole;
	}

	public void setTxtrole(String txtrole) {
		this.txtrole = txtrole;
	}

	//bi-directional many-to-one association to CfgTblUserRole
	@OneToMany(mappedBy="cfgTblUser",fetch = FetchType.EAGER)
	private List<CfgTblUserRole> cfgTblUserRoles;

	public CfgTblUser() {
	}

	public Integer getSerUserId() {
		return this.serUserId;
	}

	public void setSerUserId(Integer serUserId) {
		this.serUserId = serUserId;
	}

	public Boolean getBlIsActive() {
		return this.blIsActive;
	}

	public void setBlIsActive(Boolean blIsActive) {
		this.blIsActive = blIsActive;
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

	public Integer getNumSubMenuOrder() {
		return this.numSubMenuOrder;
	}

	public void setNumSubMenuOrder(Integer numSubMenuOrder) {
		this.numSubMenuOrder = numSubMenuOrder;
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

	public String getTxtPassword() {
		return this.txtPassword;
	}

	public void setTxtPassword(String txtPassword) {
		this.txtPassword = txtPassword;
	}

	public String getTxtUserName() {
		return this.txtUserName;
	}

	public void setTxtUserName(String txtUserName) {
		this.txtUserName = txtUserName;
	}

	public List<CfgTblUserRole> getCfgTblUserRoles() {
		return this.cfgTblUserRoles;
	}

	public void setCfgTblUserRoles(List<CfgTblUserRole> cfgTblUserRoles) {
		this.cfgTblUserRoles = cfgTblUserRoles;
	}

	public CfgTblUserRole addCfgTblUserRole(CfgTblUserRole cfgTblUserRole) {
		getCfgTblUserRoles().add(cfgTblUserRole);
		cfgTblUserRole.setCfgTblUser(this);

		return cfgTblUserRole;
	}

	public CfgTblUserRole removeCfgTblUserRole(CfgTblUserRole cfgTblUserRole) {
		getCfgTblUserRoles().remove(cfgTblUserRole);
		cfgTblUserRole.setCfgTblUser(null);

		return cfgTblUserRole;
	}
	
	
	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	@Column(name="ser_group_id")
	private Integer serGroupId;
	

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	
	public CfgTblRole getCfgTblRole() {
		return cfgTblRole;
	}

	public void setCfgTblRole(CfgTblRole cfgTblRole) {
		this.cfgTblRole = cfgTblRole;
	}

	@ManyToOne
	@JoinColumn(name = "ser_role_id")
	private CfgTblRole cfgTblRole;
	
	
	public Boolean getBlIsPasswordChang() {
		return blIsPasswordChang;
	}

	public void setBlIsPasswordChang(Boolean blIsPasswordChang) {
		this.blIsPasswordChang = blIsPasswordChang;
	}

	@Column(name="bl_is_PasswordChang")
	private Boolean blIsPasswordChang;



	/*@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ser_manager_id")*/
	@ManyToOne
	@JoinColumn(name = "manager_id")
	private CfgTblUser cfgTblManager;

	public CfgTblUser getCfgTblManager() {
		return cfgTblManager;
	}

	public void setCfgTblManager(CfgTblUser cfgTblManager) {
		this.cfgTblManager = cfgTblManager;
	}

	
	public CfgTblPasswordPolicy getCfgTblPasswordPolicy() {
		return cfgTblPasswordPolicy;
	}

	public void setCfgTblPasswordPolicy(CfgTblPasswordPolicy cfgTblPasswordPolicy) {
		this.cfgTblPasswordPolicy = cfgTblPasswordPolicy;
	}

	public Date getDteExpiryDate() {
		return dteExpiryDate;
	}

	public void setDteExpiryDate(Date dteExpiryDate) {
		this.dteExpiryDate = dteExpiryDate;
	}

	public BigDecimal getNumAttempt() {
		return numAttempt;
	}

	public void setNumAttempt(BigDecimal numAttempt) {
		this.numAttempt = numAttempt;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ser_password_policy_id")
	private CfgTblPasswordPolicy cfgTblPasswordPolicy;
	
	@Column(name="dte_expiry_date")
	private Date dteExpiryDate;
	
	@Column(name="num_attempt")
	private BigDecimal numAttempt;
	
	
	@ManyToOne
	@JoinColumn(name="ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;



	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	@Column(name="bl_is_group_customer")
	private Boolean blIsGroupCustomer;

	public Boolean getBlIsGroupCustomer() {
		return blIsGroupCustomer;
	}

	public void setBlIsGroupCustomer(Boolean blIsGroupCustomer) {
		this.blIsGroupCustomer = blIsGroupCustomer;
	}

	//bi-directional many-to-one association to HrTblDepartment
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ser_department_id")
	@JsonBackReference
	private HrTblDepartment hrTblDepartment;

	public HrTblDepartment getHrTblDepartment() {
		return hrTblDepartment;
	}

	public void setHrTblDepartment(HrTblDepartment hrTblDepartment) {
		this.hrTblDepartment = hrTblDepartment;
	}
}