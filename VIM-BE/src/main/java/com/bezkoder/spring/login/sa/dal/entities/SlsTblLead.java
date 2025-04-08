package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the sls_tbl_lead database table.
 * 
 */
@Entity
@Table(name="sls_tbl_lead")
@NamedQuery(name="SlsTblLead.findAll", query="SELECT c FROM SlsTblLead c")
public class SlsTblLead implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_lead_id")
	private Integer serLeadId;
	

	@Column(name="txt_cnic_no")
	private String txtCnicNo;

	@Column(name="txt_lead_code")
	private String txtLeadCode;

	@Column(name="txt_lead_name")
	private String txtLeadName;

	@Column(name="txt_display_address")
	private String txtDisplayAddress;

	@Column(name="txt_email_address")
	private String txtEmailAddress;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_isnational")
	private Boolean blnIsnational;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_parent_lead_id")
	private Integer serParentCountryId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_name")
	private String txtName;
	
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
	
	@Column(name="txt_expirty_date")
	private String txtExpiryDate;

	@Column(name="txt_phone_no")
	private String txtPhoneNo;
	
	@Column(name="txt_phone_no2")
	private String txtPhoneNo2;
	
	@Column(name="txt_status")
	private String txtStatus;
	
	@Column(name="txt_interest")
	private String txtInterest;

	@Column(name="bln_commercial")
	private Boolean blnCommercial;
	

	@Column(name="bln_passanger")
	private Boolean blnPassanger;

	@Column(name="txt_shipping_address")
	private String txtShippingAddress;
	
	
	@Column(name="txt_product")
	private String txtProduct;
	
	@Column(name="txt_remarks")
	private String txtRemarks;
	
	@Column(name="bl_is_test_drive_taken")
	private Boolean blIsTestDriveTaken;


	@Column(name="bl_is_dealer")
	private Boolean blIsDealer;
	
	//bi-directional many-to-one association to CfgTblCity
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;

	//bi-directional many-to-one association to CfgTblCountry
	@ManyToOne
	@JoinColumn(name="ser_country_id")
	private CfgTblCountry cfgTblCountry;
	
	
	//bi-directional many-to-one association to CfgTblCountry
		@ManyToOne
		@JoinColumn(name="ser_source_id")
		private CfgTblSource cfgTblSource;


		@ManyToOne
		@JoinColumn(name="ser_customer_id")
		private CfgTblCustomer cfgTblCustomer;
		
		
	//bi-directional many-to-one association to cfgTblArea
		@ManyToOne
		@JoinColumn(name="ser_area_id")
		private CfgTblArea cfgTblArea;
		
		
		@Column(name="bl_is_from_gal_team")
		private Boolean blIsFromGALTeam;	
		
		@Column(name="bl_is_from_marketting")
		private Boolean blIsFromMarketting;	
		
		@Column(name="bl_is_showroom_walkin")
		private Boolean blIsShowrommWalkIn;	
		
		@Column(name="bl_is_dealer_ref")
		private Boolean blIsDealerRef;
		
		@Column(name="bl_is_natural_traffic")
		private Boolean blIsNaturalTrafic;
		
		

	public SlsTblLead() {
	}



	public Integer getSerLeadId() {
		return serLeadId;
	}



	public void setSerLeadId(Integer serLeadId) {
		this.serLeadId = serLeadId;
	}



	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}



	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}



	public Boolean getBlnIsnational() {
		return blnIsnational;
	}



	public void setBlnIsnational(Boolean blnIsnational) {
		this.blnIsnational = blnIsnational;
	}



	public Boolean getBlnStatus() {
		return blnStatus;
	}



	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}



	public Timestamp getDteCreateddate() {
		return dteCreateddate;
	}



	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}



	public Timestamp getDteModifieddate() {
		return dteModifieddate;
	}



	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}



	public Integer getSerCreatedUserId() {
		return serCreatedUserId;
	}



	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}



	public Integer getSerModifiedUserId() {
		return serModifiedUserId;
	}



	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}



	public Integer getSerParentCountryId() {
		return serParentCountryId;
	}



	public void setSerParentCountryId(Integer serParentCountryId) {
		this.serParentCountryId = serParentCountryId;
	}



	public String getTxtMachineIp() {
		return txtMachineIp;
	}



	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}



	public String getTxtName() {
		return txtName;
	}



	public void setTxtName(String txtName) {
		this.txtName = txtName;
	}



	public String getTxtCnicNo() {
		return txtCnicNo;
	}



	public void setTxtCnicNo(String txtCnicNo) {
		this.txtCnicNo = txtCnicNo;
	}



	public String getTxtLeadCode() {
		return txtLeadCode;
	}



	public void setTxtLeadCode(String txtLeadCode) {
		this.txtLeadCode = txtLeadCode;
	}



	public String getTxtLeadName() {
		return txtLeadName;
	}



	public void setTxtLeadName(String txtLeadName) {
		this.txtLeadName = txtLeadName;
	}



	public String getTxtDisplayAddress() {
		return txtDisplayAddress;
	}



	public void setTxtDisplayAddress(String txtDisplayAddress) {
		this.txtDisplayAddress = txtDisplayAddress;
	}



	public String getTxtEmailAddress() {
		return txtEmailAddress;
	}



	public void setTxtEmailAddress(String txtEmailAddress) {
		this.txtEmailAddress = txtEmailAddress;
	}



	public String getTxtMobileNo() {
		return txtMobileNo;
	}



	public void setTxtMobileNo(String txtMobileNo) {
		this.txtMobileNo = txtMobileNo;
	}



	public String getTxtNtnNo() {
		return txtNtnNo;
	}



	public void setTxtNtnNo(String txtNtnNo) {
		this.txtNtnNo = txtNtnNo;
	}



	public String getTxtSTR() {
		return txtSTR;
	}



	public void setTxtSTR(String txtSTR) {
		this.txtSTR = txtSTR;
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



	public String getTxtExpiryDate() {
		return txtExpiryDate;
	}



	public void setTxtExpiryDate(String txtExpiryDate) {
		this.txtExpiryDate = txtExpiryDate;
	}



	public String getTxtPhoneNo() {
		return txtPhoneNo;
	}



	public void setTxtPhoneNo(String txtPhoneNo) {
		this.txtPhoneNo = txtPhoneNo;
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



	public String getTxtShippingAddress() {
		return txtShippingAddress;
	}



	public void setTxtShippingAddress(String txtShippingAddress) {
		this.txtShippingAddress = txtShippingAddress;
	}



	public CfgTblCity getCfgTblCity() {
		return cfgTblCity;
	}



	public void setCfgTblCity(CfgTblCity cfgTblCity) {
		this.cfgTblCity = cfgTblCity;
	}



	public CfgTblCountry getCfgTblCountry() {
		return cfgTblCountry;
	}



	public void setCfgTblCountry(CfgTblCountry cfgTblCountry) {
		this.cfgTblCountry = cfgTblCountry;
	}




	public CfgTblArea getCfgTblArea() {
		return cfgTblArea;
	}



	public void setCfgTblArea(CfgTblArea cfgTblArea) {
		this.cfgTblArea = cfgTblArea;
	}



	public static long getSerialversionuid() {
		return serialVersionUID;
	}



	public String getTxtProduct() {
		return txtProduct;
	}



	public void setTxtProduct(String txtProduct) {
		this.txtProduct = txtProduct;
	}



	public String getTxtRemarks() {
		return txtRemarks;
	}



	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}



	public Boolean getBlIsDealer() {
		return blIsDealer;
	}



	public void setBlIsDealer(Boolean blIsDealer) {
		this.blIsDealer = blIsDealer;
	}



	public Boolean getBlIsFromGALTeam() {
		return blIsFromGALTeam;
	}



	public void setBlIsFromGALTeam(Boolean blIsFromGALTeam) {
		this.blIsFromGALTeam = blIsFromGALTeam;
	}



	public Boolean getBlIsFromMarketting() {
		return blIsFromMarketting;
	}



	public void setBlIsFromMarketting(Boolean blIsFromMarketting) {
		this.blIsFromMarketting = blIsFromMarketting;
	}



	public Boolean getBlIsShowrommWalkIn() {
		return blIsShowrommWalkIn;
	}



	public void setBlIsShowrommWalkIn(Boolean blIsShowrommWalkIn) {
		this.blIsShowrommWalkIn = blIsShowrommWalkIn;
	}



	public Boolean getBlIsDealerRef() {
		return blIsDealerRef;
	}



	public void setBlIsDealerRef(Boolean blIsDealerRef) {
		this.blIsDealerRef = blIsDealerRef;
	}



	public Boolean getBlIsNaturalTrafic() {
		return blIsNaturalTrafic;
	}



	public void setBlIsNaturalTrafic(Boolean blIsNaturalTrafic) {
		this.blIsNaturalTrafic = blIsNaturalTrafic;
	}



	public CfgTblSource getCfgTblSource() {
		return cfgTblSource;
	}



	public void setCfgTblSource(CfgTblSource cfgTblSource) {
		this.cfgTblSource = cfgTblSource;
	}



	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}



	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}



	public String getTxtStatus() {
		return txtStatus;
	}



	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}



	public String getTxtInterest() {
		return txtInterest;
	}



	public void setTxtInterest(String txtInterest) {
		this.txtInterest = txtInterest;
	}



	public Boolean getBlIsTestDriveTaken() {
		return blIsTestDriveTaken;
	}



	public void setBlIsTestDriveTaken(Boolean blIsTestDriveTaken) {
		this.blIsTestDriveTaken = blIsTestDriveTaken;
	}

	
	
		
}