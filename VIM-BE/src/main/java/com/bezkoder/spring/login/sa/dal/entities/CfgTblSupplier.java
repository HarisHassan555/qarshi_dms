package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_supplier database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_supplier")
@NamedQuery(name="CfgTblSupplier.findAll", query="SELECT c FROM CfgTblSupplier c")
public class CfgTblSupplier implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_supplier_id")
	private Integer serSupplierId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Supplier_sql")
	@SequenceGenerator(name="Supplier_sql", sequenceName = "cfg_tbl_supplier_ser_supplier_id_seq", allocationSize=1)
	@Column(name = "ser_supplier_id", updatable = false, nullable = false)
	private Integer serSupplierId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_is_approved")
	private Boolean blnIsApproved;

	public Boolean getBlnStatus() {
		return blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	@Column(name="bln_status")
	private Boolean blnStatus;
	
	@Temporal(TemporalType.DATE)
	@Column(name="dte_approved_date")
	private Date dteApprovedDate;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_credit_limit")
	private BigDecimal numCreditLimit;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_company_name")
	private String txtCompanyName;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_email")
	private String txtEmail;

	@Column(name="txt_fax_no")
	private String txtFaxNo;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_ntn_no")
	private String txtNtnNo;

	@Column(name="txt_supplier_address")
	private String txtSupplierAddress;

	@Column(name="txt_supplier_address2")
	private String txtSupplierAddress2;

	@Column(name="txt_supplier_category")
	private String txtSupplierCategory;

	@Column(name="txt_supplier_code")
	private String txtSupplierCode;

	@Column(name="txt_supplier_mobile_no")
	private String txtSupplierMobileNo;

	@Column(name="txt_supplier_name")
	private String txtSupplierName;

	@Column(name="txt_supplier_phone_no")
	private String txtSupplierPhoneNo;

	@Column(name="txt_type")
	private String txtType;
	
	


	public CfgTblSupplierCategory getCfgTblSupplierCategory() {
		return cfgTblSupplierCategory;
	}

	public void setCfgTblSupplierCategory(CfgTblSupplierCategory cfgTblSupplierCategory) {
		this.cfgTblSupplierCategory = cfgTblSupplierCategory;
	}

	@ManyToOne
	@JoinColumn(name="ser_supplier_category_id")
	private CfgTblSupplierCategory cfgTblSupplierCategory;


	//bi-directional many-to-one association to CfgTblCity
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;

	

	

	public CfgTblSupplier() {
	}

	public Integer getSerSupplierId() {
		return this.serSupplierId;
	}

	public void setSerSupplierId(Integer serSupplierId) {
		this.serSupplierId = serSupplierId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsApproved() {
		return this.blnIsApproved;
	}

	public void setBlnIsApproved(Boolean blnIsApproved) {
		this.blnIsApproved = blnIsApproved;
	}

	public Date getDteApprovedDate() {
		return this.dteApprovedDate;
	}

	public void setDteApprovedDate(Date dteApprovedDate) {
		this.dteApprovedDate = dteApprovedDate;
	}

	public Timestamp getDteCreateddate() {
		return this.dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public BigDecimal getNumCreditLimit() {
		return this.numCreditLimit;
	}

	public void setNumCreditLimit(BigDecimal numCreditLimit) {
		this.numCreditLimit = numCreditLimit;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public String getTxtCompanyName() {
		return this.txtCompanyName;
	}

	public void setTxtCompanyName(String txtCompanyName) {
		this.txtCompanyName = txtCompanyName;
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

	public String getTxtFaxNo() {
		return this.txtFaxNo;
	}

	public void setTxtFaxNo(String txtFaxNo) {
		this.txtFaxNo = txtFaxNo;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtNtnNo() {
		return this.txtNtnNo;
	}

	public void setTxtNtnNo(String txtNtnNo) {
		this.txtNtnNo = txtNtnNo;
	}

	public String getTxtSupplierAddress() {
		return this.txtSupplierAddress;
	}

	public void setTxtSupplierAddress(String txtSupplierAddress) {
		this.txtSupplierAddress = txtSupplierAddress;
	}

	public String getTxtSupplierAddress2() {
		return this.txtSupplierAddress2;
	}

	public void setTxtSupplierAddress2(String txtSupplierAddress2) {
		this.txtSupplierAddress2 = txtSupplierAddress2;
	}

	public String getTxtSupplierCategory() {
		return this.txtSupplierCategory;
	}

	public void setTxtSupplierCategory(String txtSupplierCategory) {
		this.txtSupplierCategory = txtSupplierCategory;
	}

	public String getTxtSupplierCode() {
		return this.txtSupplierCode;
	}

	public void setTxtSupplierCode(String txtSupplierCode) {
		this.txtSupplierCode = txtSupplierCode;
	}

	public String getTxtSupplierMobileNo() {
		return this.txtSupplierMobileNo;
	}

	public void setTxtSupplierMobileNo(String txtSupplierMobileNo) {
		this.txtSupplierMobileNo = txtSupplierMobileNo;
	}

	public String getTxtSupplierName() {
		return this.txtSupplierName;
	}

	public void setTxtSupplierName(String txtSupplierName) {
		this.txtSupplierName = txtSupplierName;
	}

	public String getTxtSupplierPhoneNo() {
		return this.txtSupplierPhoneNo;
	}

	public void setTxtSupplierPhoneNo(String txtSupplierPhoneNo) {
		this.txtSupplierPhoneNo = txtSupplierPhoneNo;
	}

	public String getTxtType() {
		return this.txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public CfgTblCity getCfgTblCity() {
		return this.cfgTblCity;
	}

	public void setCfgTblCity(CfgTblCity cfgTblCity) {
		this.cfgTblCity = cfgTblCity;
	}

}