package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name = "sls_tbl_tir")
@NamedQuery(name = "SlsTblTIR.findAll", query = "SELECT s FROM SlsTblTIR s")
public class SlsTblTIR implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_so_detail_id") private Integer serSoDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SlsTblTIR_ser_tir_id_seq")
	@SequenceGenerator(name = "SlsTblTIR_ser_tir_id_seq", sequenceName = "SlsTblTIR_ser_tir_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_tir_id")
	private Integer serTIRId;

	@Column(name = "txt_tir_no")
	private String txtTIRNo;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Column(name = "txt_driver")
	private String txtDriver;

	@Column(name = "txt_driver_phone_no")
	private String txtDriverPhoneNo;

	@Column(name = "txt_warranty_type")
	private String txtWarrantyType;

	@Column(name = "txt_estimated_ref")
	private String txtEstimatedRef;

	@Column(name = "dte_timein")
	private Timestamp dteTimeIn;

	@Column(name = "dte_timeout")
	private Timestamp dteTimeOut;

	@Column(name = "dte_promisetime")
	private Timestamp dtePromiseTime;

	@Column(name = "num_previous_millage")
	private BigDecimal numPreviousMillage;

	@Column(name = "num_current_millage")
	private BigDecimal numCurrentMillage;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_invoice_date")
	private Date dteInvoiceDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_date")
	private Date dteDate;

	@Column(name = "txt_remarks")
	private String txtRemarks;

	@Column(name = "txt_status")
	private String txtStatus;

	@Column(name = "bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name = "dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name = "dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name = "ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name = "ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name = "txt_machine_ip")
	private String txtMachineIp;

	@Column(name="tim_delivery_time")
	private Time timDeliveryTime;
	
	@Column(name = "txt_complain")
	private String txtComplain;
	
	@Column(name = "txt_detail_of_rapair")
	private String txtDetailOfRepair;
	
	@Column(name = "txt_repair_history")
	private String txtRepairHistory;
	
	@Column(name = "txt_cause_of_failure")
	private String txtCauseOfFailure;
	
	@Column(name = "txt_Other_information")
	private String txtOtherInformation;
	
	@Column(name = "txt_pass_information")
	private String txtPassInformation;
	
	@Column(name = "txt_caption1")
	private String txtCaption1;
	
	@Column(name = "txt_caption2")
	private String txtCaption2;
	
	@Column(name="num_level")
	private Integer numLevel;
	
	@Column(name="ser_approvedby_id1")
	private Integer serApprovedbyId1;
	
	@Column(name="ser_approvedby_id2")
	private Integer serApprovedbyId2;
	
	@Column(name="ser_approvedby_id3")
	private Integer serApprovedbyId3;

	@Column(name="txt_status1")
	private String txtStatus1;
	
	@Column(name="txt_status2")
	private String txtStatus2;
	
	
	@Column(name="txt_status3")
	private String txtStatus3;
	
	@Column(name="txt_Level")
	private String txtLevel;
	
	@Column(name = "txt_link")
	private String txtLink;
	
	

	@Column(name="dte_approveddate1")
	private Timestamp dteApproveddate1;
	
	@Column(name="dte_approveddate2")
	private Timestamp dteApproveddate2;
	
	@Column(name="dte_approveddate3")
	private Timestamp dteApproveddate3;
	
	
	
	@OneToMany(mappedBy = "slsTblTIR")
	private List<SlsTblTIRDetail> SlsTblTIRDetail;

	@OneToMany(mappedBy = "slsTblTIR")
	private List<SlsTblTIRDetail> SlsTblTIRDetailservices;
	
	@JsonIgnoreProperties(value={"slsTblTIR"})
	@OneToMany(fetch=FetchType.LAZY,mappedBy="slsTblTIR")
	private List<SlsTblTIRDocument> TIRDocument=new ArrayList<SlsTblTIRDocument>();


	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_product_id")
	private CfgTblProduct cfgTblProduct;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_model_id")
	private CfgTblModel cfgTblModel;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_color_id")
	private CfgTblColor cfgTblColor;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	
	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_dealer_id")
	private CfgTblCustomer cfgTblDealer;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_jobCategory_id")
	private CfgTblJobCategory cfgTblJobCategory;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_jobType_id")
	private CfgTblJobType cfgTblJobType;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;
	
	@ManyToOne
	@JoinColumn(name = "ser_phenomenon_id")
	private CfgTblPhenomenon cfgTblPhenomenon;
	
	@ManyToOne
	@JoinColumn(name = "ser_trouble_id")
	private CfgTblTrouble cfgTblTrouble;
	
	@ManyToOne
	@JoinColumn(name = "ser_defect_id")
	private CfgTblDefect cfgTblDefect;
	
    private String dte_date_from;
	
	
	private String dte_date_to;
	
	@Column(name="txtType")
	private String txtType;
	
	@Column(name="bl_is_gal")
	private Boolean blIsGAL;

	public Integer getSerTIRId() {
		return serTIRId;
	}

	public void setSerTIRId(Integer serTIRId) {
		this.serTIRId = serTIRId;
	}

	public String getTxtTIRNo() {
		return txtTIRNo;
	}

	public void setTxtTIRNo(String txtTIRNo) {
		this.txtTIRNo = txtTIRNo;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public String getTxtDriver() {
		return txtDriver;
	}

	public void setTxtDriver(String txtDriver) {
		this.txtDriver = txtDriver;
	}

	public String getTxtDriverPhoneNo() {
		return txtDriverPhoneNo;
	}

	public void setTxtDriverPhoneNo(String txtDriverPhoneNo) {
		this.txtDriverPhoneNo = txtDriverPhoneNo;
	}

	public String getTxtWarrantyType() {
		return txtWarrantyType;
	}

	public void setTxtWarrantyType(String txtWarrantyType) {
		this.txtWarrantyType = txtWarrantyType;
	}

	public String getTxtEstimatedRef() {
		return txtEstimatedRef;
	}

	public void setTxtEstimatedRef(String txtEstimatedRef) {
		this.txtEstimatedRef = txtEstimatedRef;
	}

	public Timestamp getDteTimeIn() {
		return dteTimeIn;
	}

	public void setDteTimeIn(Timestamp dteTimeIn) {
		this.dteTimeIn = dteTimeIn;
	}

	public Timestamp getDteTimeOut() {
		return dteTimeOut;
	}

	public void setDteTimeOut(Timestamp dteTimeOut) {
		this.dteTimeOut = dteTimeOut;
	}

	public Timestamp getDtePromiseTime() {
		return dtePromiseTime;
	}

	public void setDtePromiseTime(Timestamp dtePromiseTime) {
		this.dtePromiseTime = dtePromiseTime;
	}

	public BigDecimal getNumPreviousMillage() {
		return numPreviousMillage;
	}

	public void setNumPreviousMillage(BigDecimal numPreviousMillage) {
		this.numPreviousMillage = numPreviousMillage;
	}

	public BigDecimal getNumCurrentMillage() {
		return numCurrentMillage;
	}

	public void setNumCurrentMillage(BigDecimal numCurrentMillage) {
		this.numCurrentMillage = numCurrentMillage;
	}

	public Date getDteInvoiceDate() {
		return dteInvoiceDate;
	}

	public void setDteInvoiceDate(Date dteInvoiceDate) {
		this.dteInvoiceDate = dteInvoiceDate;
	}

	public Date getDteDate() {
		return dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public String getTxtRemarks() {
		return txtRemarks;
	}

	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}

	public String getTxtStatus() {
		return txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
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

	public String getTxtMachineIp() {
		return txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public List<SlsTblTIRDetail> getSlsTblTIRDetail() {
		return SlsTblTIRDetail;
	}

	public void setSlsTblTIRDetail(List<SlsTblTIRDetail> SlsTblTIRDetail) {
		this.SlsTblTIRDetail = SlsTblTIRDetail;
	}

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public CfgTblModel getCfgTblModel() {
		return cfgTblModel;
	}

	public void setCfgTblModel(CfgTblModel cfgTblModel) {
		this.cfgTblModel = cfgTblModel;
	}


	public CfgTblColor getCfgTblColor() {
		return cfgTblColor;
	}

	public void setCfgTblColor(CfgTblColor cfgTblColor) {
		this.cfgTblColor = cfgTblColor;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public CfgTblJobCategory getCfgTblJobCategory() {
		return cfgTblJobCategory;
	}

	public void setCfgTblJobCategory(CfgTblJobCategory cfgTblJobCategory) {
		this.cfgTblJobCategory = cfgTblJobCategory;
	}

	public CfgTblJobType getCfgTblJobType() {
		return cfgTblJobType;
	}

	public void setCfgTblJobType(CfgTblJobType cfgTblJobType) {
		this.cfgTblJobType = cfgTblJobType;
	}

	public SlsTblSoVehicleDetail getSlsTblSoVehicleDetail() {
		return slsTblSoVehicleDetail;
	}

	public void setSlsTblSoVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail) {
		this.slsTblSoVehicleDetail = slsTblSoVehicleDetail;
	}

	public List<SlsTblTIRDetail> getSlsTblTIRDetailservices() {
		return SlsTblTIRDetailservices;
	}

	public void setSlsTblTIRDetailservices(List<SlsTblTIRDetail> SlsTblTIRDetailservices) {
		this.SlsTblTIRDetailservices = SlsTblTIRDetailservices;
	}

	public Time getTimDeliveryTime() {
		return timDeliveryTime;
	}

	public void setTimDeliveryTime(Time timDeliveryTime) {
		this.timDeliveryTime = timDeliveryTime;
	}

	public String getDte_date_from() {
		return dte_date_from;
	}

	public void setDte_date_from(String dte_date_from) {
		this.dte_date_from = dte_date_from;
	}

	public String getDte_date_to() {
		return dte_date_to;
	}

	public void setDte_date_to(String dte_date_to) {
		this.dte_date_to = dte_date_to;
	}

	public String getTxtComplain() {
		return txtComplain;
	}

	public void setTxtComplain(String txtComplain) {
		this.txtComplain = txtComplain;
	}

	public String getTxtDetailOfRepair() {
		return txtDetailOfRepair;
	}

	public void setTxtDetailOfRepair(String txtDetailOfRepair) {
		this.txtDetailOfRepair = txtDetailOfRepair;
	}

	public String getTxtRepairHistory() {
		return txtRepairHistory;
	}

	public void setTxtRepairHistory(String txtRepairHistory) {
		this.txtRepairHistory = txtRepairHistory;
	}

	public String getTxtCauseOfFailure() {
		return txtCauseOfFailure;
	}

	public void setTxtCauseOfFailure(String txtCauseOfFailure) {
		this.txtCauseOfFailure = txtCauseOfFailure;
	}

	public String getTxtOtherInformation() {
		return txtOtherInformation;
	}

	public void setTxtOtherInformation(String txtOtherInformation) {
		this.txtOtherInformation = txtOtherInformation;
	}

	public String getTxtPassInformation() {
		return txtPassInformation;
	}

	public void setTxtPassInformation(String txtPassInformation) {
		this.txtPassInformation = txtPassInformation;
	}

	public String getTxtCaption1() {
		return txtCaption1;
	}

	public void setTxtCaption1(String txtCaption1) {
		this.txtCaption1 = txtCaption1;
	}

	public String getTxtCaption2() {
		return txtCaption2;
	}

	public void setTxtCaption2(String txtCaption2) {
		this.txtCaption2 = txtCaption2;
	}

	public CfgTblPhenomenon getCfgTblPhenomenon() {
		return cfgTblPhenomenon;
	}

	public void setCfgTblPhenomenon(CfgTblPhenomenon cfgTblPhenomenon) {
		this.cfgTblPhenomenon = cfgTblPhenomenon;
	}

	public CfgTblTrouble getCfgTblTrouble() {
		return cfgTblTrouble;
	}

	public void setCfgTblTrouble(CfgTblTrouble cfgTblTrouble) {
		this.cfgTblTrouble = cfgTblTrouble;
	}

	public CfgTblDefect getCfgTblDefect() {
		return cfgTblDefect;
	}

	public void setCfgTblDefect(CfgTblDefect cfgTblDefect) {
		this.cfgTblDefect = cfgTblDefect;
	}

	public List<SlsTblTIRDocument> getTIRDocument() {
		return TIRDocument;
	}

	public void setTIRDocument(List<SlsTblTIRDocument> tIRDocument) {
		TIRDocument = tIRDocument;
	}

	public Integer getNumLevel() {
		return numLevel;
	}

	public void setNumLevel(Integer numLevel) {
		this.numLevel = numLevel;
	}

	public Integer getSerApprovedbyId1() {
		return serApprovedbyId1;
	}

	public void setSerApprovedbyId1(Integer serApprovedbyId1) {
		this.serApprovedbyId1 = serApprovedbyId1;
	}

	public Integer getSerApprovedbyId2() {
		return serApprovedbyId2;
	}

	public void setSerApprovedbyId2(Integer serApprovedbyId2) {
		this.serApprovedbyId2 = serApprovedbyId2;
	}

	public Integer getSerApprovedbyId3() {
		return serApprovedbyId3;
	}

	public void setSerApprovedbyId3(Integer serApprovedbyId3) {
		this.serApprovedbyId3 = serApprovedbyId3;
	}

	public String getTxtStatus1() {
		return txtStatus1;
	}

	public void setTxtStatus1(String txtStatus1) {
		this.txtStatus1 = txtStatus1;
	}

	public String getTxtStatus2() {
		return txtStatus2;
	}

	public void setTxtStatus2(String txtStatus2) {
		this.txtStatus2 = txtStatus2;
	}

	public String getTxtStatus3() {
		return txtStatus3;
	}

	public void setTxtStatus3(String txtStatus3) {
		this.txtStatus3 = txtStatus3;
	}

	public CfgTblCustomer getCfgTblDealer() {
		return cfgTblDealer;
	}

	public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
		this.cfgTblDealer = cfgTblDealer;
	}

	public String getTxtLevel() {
		return txtLevel;
	}

	public void setTxtLevel(String txtLevel) {
		this.txtLevel = txtLevel;
	}

	@Column(name="pic1",columnDefinition="mediumblob")
	private byte[] pic1;
	
	@Column(name="pic2",columnDefinition="mediumblob")
	private byte[] pic2;
	
	@Column(name="pic3",columnDefinition="mediumblob")
	private byte[] pic3;

	public byte[] getPic1() {
		return pic1;
	}

	public void setPic1(byte[] pic1) {
		this.pic1 = pic1;
	}

	public byte[] getPic2() {
		return pic2;
	}

	public void setPic2(byte[] pic2) {
		this.pic2 = pic2;
	}

	public byte[] getPic3() {
		return pic3;
	}

	public void setPic3(byte[] pic3) {
		this.pic3 = pic3;
	}

	public Timestamp getDteApproveddate1() {
		return dteApproveddate1;
	}

	public void setDteApproveddate1(Timestamp dteApproveddate1) {
		this.dteApproveddate1 = dteApproveddate1;
	}

	public Timestamp getDteApproveddate2() {
		return dteApproveddate2;
	}

	public void setDteApproveddate2(Timestamp dteApproveddate2) {
		this.dteApproveddate2 = dteApproveddate2;
	}

	public Timestamp getDteApproveddate3() {
		return dteApproveddate3;
	}

	public void setDteApproveddate3(Timestamp dteApproveddate3) {
		this.dteApproveddate3 = dteApproveddate3;
	}

	public String getTxtType() {
		return txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public Boolean getBlIsGAL() {
		return blIsGAL;
	}

	public void setBlIsGAL(Boolean blIsGAL) {
		this.blIsGAL = blIsGAL;
	}

	public String getTxtLink() {
		return txtLink;
	}

	public void setTxtLink(String txtLink) {
		this.txtLink = txtLink;
	}
	
	

}