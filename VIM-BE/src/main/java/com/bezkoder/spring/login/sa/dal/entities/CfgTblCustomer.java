package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_customer database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_customer")
@NamedQuery(name="CfgTblCustomer.findAll", query="SELECT c FROM CfgTblCustomer c")

public class CfgTblCustomer implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_customer_id")
	private Integer serCustomerId;*/
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_customer_ser_customer_id_seq")
	@SequenceGenerator(name = "cfg_tbl_customer_ser_customer_id_seq", sequenceName = "cfg_tbl_customer_ser_customer_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_customer_id")
	private Integer serCustomerId;
	

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_is_filer")
	private Boolean blnIsFiler;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_discount")
	private BigDecimal numDiscount;

	@Column(name="num_excise_duty")
	private BigDecimal numExciseDuty;

	@Column(name="num_sales_tax")
	private BigDecimal numSalesTax;
	
	@Column(name="num_fed")
	private BigDecimal numFED;
	
	public BigDecimal getNumFurtherTax() {
		return numFurtherTax;
	}

	public void setNumFurtherTax(BigDecimal numFurtherTax) {
		this.numFurtherTax = numFurtherTax;
	}

	@Column(name="num_further_tax")
	private BigDecimal numFurtherTax;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_billing_address")
	private String txtBillingAddress;

	@Column(name="txt_business_name")
	private String txtBusinessName;

	@Column(name="txt_cnic_no")
	private String txtCnicNo;

	@Column(name="txt_customer_code")
	private String txtCustomerCode;
	
	@Column(name="txt_invoice_name")
	private String txtInvoiceName;

	@Column(name="txt_customer_name")
	private String txtCustomerName;

	@Column(name="txt_display_address")
	private String txtDisplayAddress;

	@Column(name="txt_email_address")
	private String txtEmailAddress;

	@Column(name="txt_gst_name_on_invoice")
	private String txtGstNameOnInvoice;

	@Column(name="txt_gst_number")
	private String txtGstNumber;

	@Column(name="txt_is_filer")
	private String txtIsFiler;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_mobile_no")
	private String txtMobileNo;

	@Column(name="txt_ntn_no")
	private String txtNtnNo;
	
	@Column(name="txt_str")
	private String txtSTR;
	
	@Column(name="txt_ftn")
	private String txtFTN;
	
	@Column(name="txt_province")
	private String txtProvince;
	
	@Column(name="txt_fname")
	private String txtFName;
	
	@Column(name="dte_expirty_date")
	private Date dteExpiryDate;
	
	@Column(name="dte_dob")
	private Date dteDOB;
	
	@Column(name="txt_expirty_date")
	private String txtExpiryDate;

	@Column(name="txt_phone_no")
	private String txtPhoneNo;
	
	@Column(name="txt_phone_no2")
	private String txtPhoneNo2;
	

	@Column(name="bln_commercial")
	private Boolean blnCommercial;
	

	@Column(name="bln_passanger")
	private Boolean blnPassanger;
	
	public Boolean getBlnIsGst() {
		return blnIsGst;
	}

	public void setBlnIsGst(Boolean blnIsGst) {
		this.blnIsGst = blnIsGst;
	}
	
	

	

	@Column(name="bln_is_gst")
	private Boolean blnIsGst;

	@Column(name="txt_shipping_address")
	private String txtShippingAddress;

	//bi-directional many-to-one association to CfgTblCity
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;

	//bi-directional many-to-one association to CfgTblCountry
	@ManyToOne
	@JoinColumn(name="ser_country_id")
	private CfgTblCountry cfgTblCountry;

	//bi-directional many-to-one association to CfgTblCustomerCategory
	@ManyToOne
	@JoinColumn(name="ser_customer_category_id")
	private CfgTblCustomerCategory cfgTblCustomerCategory;

	//bi-directional many-to-one association to SlsTblSaleOrder
/*	@OneToMany(mappedBy="cfgTblCustomer")*/
	@OneToMany(mappedBy = "cfgTblCustomer", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<SlsTblSaleOrder> slsTblSaleOrders;

	//bi-directional many-to-one association to cfgTblArea
		@ManyToOne
		@JoinColumn(name="ser_area_id")
		private CfgTblArea cfgTblArea;
		
		
		@ManyToOne
		@JoinColumn(name="ser_region_id")
		private CfgTblRegion cfgTblRegion;
		
		
		
		//bi-directional many-to-one association to cfgTblArea
				@ManyToOne
				@JoinColumn(name="ser_zone1_id")
				private CfgTblZone1 cfgTblZone1;
				
			
				//bi-directional many-to-one association to cfgTblArea
				@ManyToOne
				@JoinColumn(name="ser_zone2_id")
				private CfgTblZone2 cfgTblZone2;
				
				//bi-directional many-to-one association to cfgTblArea
				@ManyToOne
				@JoinColumn(name="ser_zone3_id")
				private CfgTblZone3 cfgTblZone3;
		
		
		@Column(name="txt_xml_sent")
		private String txtXMSent;
		
		@Column(name="txt_return_msg")
		private String txtReturnMsg;
		
		@Column(name="txt_xml_receive")
		private String txtXMReceive;


	public CfgTblCustomer() {
	}

	public Integer getSerCustomerId() {
		return this.serCustomerId;
	}

	public void setSerCustomerId(Integer serCustomerId) {
		this.serCustomerId = serCustomerId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsFiler() {
		return this.blnIsFiler;
	}

	public void setBlnIsFiler(Boolean blnIsFiler) {
		this.blnIsFiler = blnIsFiler;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
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

	public BigDecimal getNumDiscount() {
		return this.numDiscount;
	}

	public void setNumDiscount(BigDecimal numDiscount) {
		this.numDiscount = numDiscount;
	}

	public BigDecimal getNumExciseDuty() {
		return this.numExciseDuty;
	}

	public void setNumExciseDuty(BigDecimal numExciseDuty) {
		this.numExciseDuty = numExciseDuty;
	}

	public BigDecimal getNumSalesTax() {
		return this.numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerGroupId() {
		return this.serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public String getTxtBillingAddress() {
		return this.txtBillingAddress;
	}

	public void setTxtBillingAddress(String txtBillingAddress) {
		this.txtBillingAddress = txtBillingAddress;
	}

	public String getTxtBusinessName() {
		return this.txtBusinessName;
	}

	public void setTxtBusinessName(String txtBusinessName) {
		this.txtBusinessName = txtBusinessName;
	}

	public String getTxtCnicNo() {
		return this.txtCnicNo;
	}

	public void setTxtCnicNo(String txtCnicNo) {
		this.txtCnicNo = txtCnicNo;
	}

	public String getTxtCustomerCode() {
		return this.txtCustomerCode;
	}

	public void setTxtCustomerCode(String txtCustomerCode) {
		this.txtCustomerCode = txtCustomerCode;
	}

	public String getTxtCustomerName() {
		return this.txtCustomerName;
	}

	public void setTxtCustomerName(String txtCustomerName) {
		this.txtCustomerName = txtCustomerName;
	}

	public String getTxtDisplayAddress() {
		return this.txtDisplayAddress;
	}

	public void setTxtDisplayAddress(String txtDisplayAddress) {
		this.txtDisplayAddress = txtDisplayAddress;
	}

	public String getTxtEmailAddress() {
		return this.txtEmailAddress;
	}

	public void setTxtEmailAddress(String txtEmailAddress) {
		this.txtEmailAddress = txtEmailAddress;
	}

	public String getTxtGstNameOnInvoice() {
		return this.txtGstNameOnInvoice;
	}

	public void setTxtGstNameOnInvoice(String txtGstNameOnInvoice) {
		this.txtGstNameOnInvoice = txtGstNameOnInvoice;
	}

	public String getTxtGstNumber() {
		return this.txtGstNumber;
	}

	public void setTxtGstNumber(String txtGstNumber) {
		this.txtGstNumber = txtGstNumber;
	}

	public String getTxtIsFiler() {
		return this.txtIsFiler;
	}

	public void setTxtIsFiler(String txtIsFiler) {
		this.txtIsFiler = txtIsFiler;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtMobileNo() {
		return this.txtMobileNo;
	}

	public void setTxtMobileNo(String txtMobileNo) {
		this.txtMobileNo = txtMobileNo;
	}

	public String getTxtNtnNo() {
		return this.txtNtnNo;
	}

	public void setTxtNtnNo(String txtNtnNo) {
		this.txtNtnNo = txtNtnNo;
	}

	public String getTxtPhoneNo() {
		return this.txtPhoneNo;
	}

	public void setTxtPhoneNo(String txtPhoneNo) {
		this.txtPhoneNo = txtPhoneNo;
	}

	public String getTxtShippingAddress() {
		return this.txtShippingAddress;
	}

	public void setTxtShippingAddress(String txtShippingAddress) {
		this.txtShippingAddress = txtShippingAddress;
	}

	public CfgTblCity getCfgTblCity() {
		return this.cfgTblCity;
	}

	public void setCfgTblCity(CfgTblCity cfgTblCity) {
		this.cfgTblCity = cfgTblCity;
	}

	public CfgTblCountry getCfgTblCountry() {
		return this.cfgTblCountry;
	}

	public void setCfgTblCountry(CfgTblCountry cfgTblCountry) {
		this.cfgTblCountry = cfgTblCountry;
	}

	public CfgTblCustomerCategory getCfgTblCustomerCategory() {
		return this.cfgTblCustomerCategory;
	}

	public void setCfgTblCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory) {
		this.cfgTblCustomerCategory = cfgTblCustomerCategory;
	}

	public List<SlsTblSaleOrder> getSlsTblSaleOrders() {
		return this.slsTblSaleOrders;
	}

	public void setSlsTblSaleOrders(List<SlsTblSaleOrder> slsTblSaleOrders) {
		this.slsTblSaleOrders = slsTblSaleOrders;
	}

	public SlsTblSaleOrder addSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		getSlsTblSaleOrders().add(slsTblSaleOrder);
		slsTblSaleOrder.setCfgTblCustomer(this);

		return slsTblSaleOrder;
	}

	public SlsTblSaleOrder removeSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		getSlsTblSaleOrders().remove(slsTblSaleOrder);
		slsTblSaleOrder.setCfgTblCustomer(null);

		return slsTblSaleOrder;
	}

	
	public Boolean getBlIsDealer() {
		return blIsDealer;
	}

	public void setBlIsDealer(Boolean blIsDealer) {
		this.blIsDealer = blIsDealer;
	}

	@Column(name="bl_is_dealer")
	private Boolean blIsDealer;
	
	
	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	@ManyToOne
	@JoinColumn(name="ser_parent_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	
	public String getTxtSapNo() {
		return txtSapNo;
	}

	public void setTxtSapNo(String txtSapNo) {
		this.txtSapNo = txtSapNo;
	}

	@Column(name="txt_sap_no")
	private String txtSapNo;
	
	
	public String getTxtUserName() {
		return txtUserName;
	}

	public void setTxtUserName(String txtUserName) {
		this.txtUserName = txtUserName;
	}

	@Column(name="txt_user_name")
	private String txtUserName;
	
	

	

	@Column(name="bl_is_group")
	private Boolean blIsGroup;

	public Boolean getBlIsGroup() {
		return blIsGroup;
	}

	public void setBlIsGroup(Boolean blIsGroup) {
		this.blIsGroup = blIsGroup;
	}
	
	@Column(name="bl_is_account_exist")
	private Boolean blIsAccountExist;
	
	

	public Boolean getBlIsAccountExist() {
		return blIsAccountExist;
	}

	public void setBlIsAccountExist(Boolean blIsAccountExist) {
		this.blIsAccountExist = blIsAccountExist;
	}

	public Boolean getBlIsLabsa() {
		return blIsLabsa;
	}

	public void setBlIsLabsa(Boolean blIsLabsa) {
		this.blIsLabsa = blIsLabsa;
	}

	@Column(name="bl_is_labsa")
	private Boolean blIsLabsa;

	@ManyToOne
	@JoinColumn(name="ser_group_customer_id")
	private CfgTblCustomer cfgTblGroupCustomer;

	public CfgTblCustomer getCfgTblGroupCustomer() {
		return cfgTblGroupCustomer;
	}

	public void setCfgTblGroupCustomer(CfgTblCustomer cfgTblGroupCustomer) {
		this.cfgTblGroupCustomer = cfgTblGroupCustomer;
	}
	
	

	@ManyToOne
	@JoinColumn(name="ser_employee_id")
	private HrTblEmployee hrTblEmployee;

	public HrTblEmployee getHrTblEmployee() {
		return hrTblEmployee;
	}

	public void setHrTblEmployee(HrTblEmployee hrTblEmployee) {
		this.hrTblEmployee = hrTblEmployee;
	}
	
	
	@Column(name="bln_is_export")
	private Boolean blnIsExport;
	
	
	@Column(name="txt_division")
	private String txtDivision;

	public Boolean getBlnIsExport() {
		return blnIsExport;
	}

	public void setBlnIsExport(Boolean blnIsExport) {
		this.blnIsExport = blnIsExport;
	}

	public String getTxtDivision() {
		return txtDivision;
	}

	public void setTxtDivision(String txtDivision) {
		this.txtDivision = txtDivision;
	}
	
	
	@ManyToOne
	@JoinColumn(name="ser_inco_terms_id")
	private CfgTblIncoTerm cfgTblIncoTerm;
	
	@ManyToOne
	@JoinColumn(name="ser_division_id")
	private CfgTblDivision cfgTblDivision;

	public CfgTblIncoTerm getCfgTblIncoTerm() {
		return cfgTblIncoTerm;
	}

	public void setCfgTblIncoTerm(CfgTblIncoTerm cfgTblIncoTerm) {
		this.cfgTblIncoTerm = cfgTblIncoTerm;
	}
	
	
	@Column(name="txt_designation")
	private String txtDesignation;
	
	
	@Column(name="txt_hod")
	private String txtHOD;
	
	@Column(name="txt_hod_mobile")
	private String txtHODMobile;
	
	@Column(name="txt_hod_landline")
	private String txtHODLandLine;
	
	@Column(name="txt_hod_email_address")
	private String txtHODEmailAddress;

	public String getTxtDesignation() {
		return txtDesignation;
	}

	public void setTxtDesignation(String txtDesignation) {
		this.txtDesignation = txtDesignation;
	}

	public String getTxtHOD() {
		return txtHOD;
	}

	public void setTxtHOD(String txtHOD) {
		this.txtHOD = txtHOD;
	}

	public String getTxtHODMobile() {
		return txtHODMobile;
	}

	public void setTxtHODMobile(String txtHODMobile) {
		this.txtHODMobile = txtHODMobile;
	}

	public String getTxtHODLandLine() {
		return txtHODLandLine;
	}

	public void setTxtHODLandLine(String txtHODLandLine) {
		this.txtHODLandLine = txtHODLandLine;
	}

	public String getTxtHODEmailAddress() {
		return txtHODEmailAddress;
	}

	public void setTxtHODEmailAddress(String txtHODEmailAddress) {
		this.txtHODEmailAddress = txtHODEmailAddress;
	}

	public CfgTblArea getCfgTblArea() {
		return cfgTblArea;
	}

	public void setCfgTblArea(CfgTblArea cfgTblArea) {
		this.cfgTblArea = cfgTblArea;
	}

	public String getTxtSTR() {
		return txtSTR;
	}

	public void setTxtSTR(String txtSTR) {
		this.txtSTR = txtSTR;
	}



	public String getTxtFName() {
		return txtFName;
	}

	public void setTxtFName(String txtFName) {
		this.txtFName = txtFName;
	}



	public Date getDteExpiryDate() {
		return dteExpiryDate;
	}

	public void setDteExpiryDate(Date dteExpiryDate) {
		this.dteExpiryDate = dteExpiryDate;
	}

	public String getTxtPhoneNo2() {
		return txtPhoneNo2;
	}

	public void setTxtPhoneNo2(String txtPhoneNo2) {
		this.txtPhoneNo2 = txtPhoneNo2;
	}

	public Boolean getBlnCommercial() {
		return blnCommercial;
	}

	public void setBlnCommercial(Boolean blnCommercial) {
		this.blnCommercial = blnCommercial;
	}

	public Boolean getBlnPassanger() {
		return blnPassanger;
	}

	public void setBlnPassanger(Boolean blnPassanger) {
		this.blnPassanger = blnPassanger;
	}

	public String getTxtExpiryDate() {
		return txtExpiryDate;
	}

	public void setTxtExpiryDate(String txtExpiryDate) {
		this.txtExpiryDate = txtExpiryDate;
	}

	public CfgTblDivision getCfgTblDivision() {
		return cfgTblDivision;
	}

	public void setCfgTblDivision(CfgTblDivision cfgTblDivision) {
		this.cfgTblDivision = cfgTblDivision;
	}

	public String getTxtFTN() {
		return txtFTN;
	}

	public void setTxtFTN(String txtFTN) {
		this.txtFTN = txtFTN;
	}

	public String getTxtProvince() {
		return txtProvince;
	}

	public void setTxtProvince(String txtProvince) {
		this.txtProvince = txtProvince;
	}

	public String getTxtXMSent() {
		return txtXMSent;
	}

	public void setTxtXMSent(String txtXMSent) {
		this.txtXMSent = txtXMSent;
	}

	public String getTxtReturnMsg() {
		return txtReturnMsg;
	}

	public void setTxtReturnMsg(String txtReturnMsg) {
		this.txtReturnMsg = txtReturnMsg;
	}

	public String getTxtXMReceive() {
		return txtXMReceive;
	}

	public void setTxtXMReceive(String txtXMReceive) {
		this.txtXMReceive = txtXMReceive;
	}

	
	
	@Column(name="txt_error_msg_from_Sap")
	private String txtErrorMsgFromSap;
	
	@Column(name="bl_is_posted_to_Sap")
	private Boolean blIsPOSTEDToSAP;

	public String getTxtErrorMsgFromSap() {
		return txtErrorMsgFromSap;
	}

	public void setTxtErrorMsgFromSap(String txtErrorMsgFromSap) {
		this.txtErrorMsgFromSap = txtErrorMsgFromSap;
	}

	public Boolean getBlIsPOSTEDToSAP() {
		return blIsPOSTEDToSAP;
	}

	public void setBlIsPOSTEDToSAP(Boolean blIsPOSTEDToSAP) {
		this.blIsPOSTEDToSAP = blIsPOSTEDToSAP;
	}

	public String getTxtInvoiceName() {
		return txtInvoiceName;
	}

	public void setTxtInvoiceName(String txtInvoiceName) {
		this.txtInvoiceName = txtInvoiceName;
	}

	public CfgTblZone1 getCfgTblZone1() {
		return cfgTblZone1;
	}

	public void setCfgTblZone1(CfgTblZone1 cfgTblZone1) {
		this.cfgTblZone1 = cfgTblZone1;
	}

	public CfgTblZone2 getCfgTblZone2() {
		return cfgTblZone2;
	}

	public void setCfgTblZone2(CfgTblZone2 cfgTblZone2) {
		this.cfgTblZone2 = cfgTblZone2;
	}

	public CfgTblZone3 getCfgTblZone3() {
		return cfgTblZone3;
	}

	public void setCfgTblZone3(CfgTblZone3 cfgTblZone3) {
		this.cfgTblZone3 = cfgTblZone3;
	}

	public Date getDteDOB() {
		return dteDOB;
	}

	public void setDteDOB(Date dteDOB) {
		this.dteDOB = dteDOB;
	}

	public BigDecimal getNumFED() {
		return numFED;
	}

	public void setNumFED(BigDecimal numFED) {
		this.numFED = numFED;
	}

	public CfgTblRegion getCfgTblRegion() {
		return cfgTblRegion;
	}

	public void setCfgTblRegion(CfgTblRegion cfgTblRegion) {
		this.cfgTblRegion = cfgTblRegion;
	}
	
	
	
}