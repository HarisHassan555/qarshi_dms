package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the sls_tbl_sale_order database table.
 * 
 */
@Entity
@Table(name="sls_tbl_sale_order")
@NamedQuery(name="SlsTblSaleOrder.findAll", query="SELECT s FROM SlsTblSaleOrder s")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlsTblSaleOrder implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_sale_order_id")
	private Integer serSaleOrderId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_sale_order_ser_sale_order_id_seq")
	@SequenceGenerator(name = "sls_tbl_sale_order_ser_sale_order_id_seq", sequenceName = "sls_tbl_sale_order_ser_sale_order_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_sale_order_id")
	private Integer serSaleOrderId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	

	@Column(name="bl_is_sales")
	private Boolean blIsSales;

	@Column(name="bln_demand_completion_status")
	private Boolean blnDemandCompletionStatus;

	@Column(name="bln_is_approved")
	private Boolean blnIsApproved;

	@Column(name="bln_is_completed")
	private Boolean blnIsCompleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name="dte_delivery_date")
	private Date dteDeliveryDate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_due_date")
	private Date dteDueDate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_excise_duty")
	private BigDecimal numExciseDuty;

	@Column(name="num_net_amount")
	private BigDecimal numNetAmount;
	
	@Column(name="num_amount")
	private BigDecimal numAmount;

	private Integer priority;

	@Column(name="ser_approvedby_id")
	private Integer serApprovedbyId;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_preparedby_id")
	private Integer serPreparedbyId;

	@Column(name="txt_billing_address")
	private String txtBillingAddress;

	@Column(name="txt_dealer")
	private String txtDealer;
	
	@Column(name="txt_customer")
	private String txtCustomer;
	
	
	@Column(name="txt_product")
	private String txtProduct;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_destination")
	private String txtDestination;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_paymnet_terms")
	private String txtPaymnetTerms;

	@Column(name="txt_price_terms")
	private String txtPriceTerms;

	@Column(name="txt_receive_status")
	private String txtReceiveStatus;

	@Column(name="txt_sale_order_no")
	private String txtSaleOrderNo;

	@Column(name="txt_ship_by")
	private String txtShipBy;

	@Column(name="txt_shipping_address1")
	private String txtShippingAddress1;

	@Column(name="txt_shipping_address2")
	private String txtShippingAddress2;

	@Column(name="txt_shipping_address3")
	private String txtShippingAddress3;
	
	@Column(name="txt_delivery_time")
	private String txtDeliveryTime;
	
	@Column(name="txt_vehicle_type")
	private String txtVehicleType;
	
	@Column(name="dte_rsm_approval")
	private Timestamp dteRSMApproval;
	
	@Column(name="dte_final_approval")
	private Timestamp dteFinalApproval;
	
	@Column(name="num_quantity")
	private BigDecimal numQuantity;
	
	@Column(name="num_price")
	private BigDecimal numPrice;
	
	@ManyToOne
	@JoinColumn(name="ser_product_id")
	private CfgTblProduct cfgTblProduct;
	
	@Column(name="bln_from_SAP")
	private Boolean blnFromSAP;
	
	@Column(name="bln_is_incoterm")
	private Boolean blnIsIncoTerm;
	
	@Column(name="num_discount")
	 private BigDecimal numDiscount;
	
	 @Column(name="num_discount_amount")
	private BigDecimal numDiscountAmount;
	 
	 @Column(name="num_amount_after_discount")
	 private BigDecimal numAmountAfterDiscount;
	 
	 @Column(name="num_fex")
	 private BigDecimal numFED;
	
	 @Column(name="num_fed_amount")
	private BigDecimal numFEDAmount;
	 
	 @Column(name="num_amount_after_fed")
	 private BigDecimal numAmountAfterFED;
	 
	 @Column(name="num_sales_Tax")
	 private BigDecimal numSalesTax;
	
	 @Column(name="num_sales_tax_amount")
	private BigDecimal numSalesTaxAmount;
	 
	 @Column(name="num_amount_after_st")
	 private BigDecimal numAmountAfterST;
	 
	 @Column(name="num_cvt")
	 private BigDecimal numCVT;
	
	 @Column(name="num_cvt_amount")
	private BigDecimal numCVTAmount;
	 
	 @Column(name="num_amount_after_cvt")
	 private BigDecimal numAmountAfterCVT;
	 
	 @Column(name="num_freight")
	 private BigDecimal numFreight;
	 
	 @Column(name="num_tax_on_freight")
	 private BigDecimal numTaxOnFreight;
	
	 @Column(name="num_tof_amount")
	private BigDecimal numTOFAmount;
	 
	 @Column(name="num_gross_value")
		private BigDecimal numGrossValue;
	 
	 @Column(name="num_advance_tax")
		private BigDecimal numAvanceTax;
	 
	 @Column(name="num_invoice_amount")
		private BigDecimal numInvoiceAmount;
	 
	 @Column(name="num_fed_on_freight")
		private BigDecimal numFEDOnFreight;
	 
	 @Column(name="num_freight_amount_afterTax")
		private BigDecimal numFreightAmountafterTax;
	 
	 @Column(name="num_commission")
		private BigDecimal numCommission;
	 
	 @Column(name="num_fabrication")
	 private BigDecimal numFabrication;
	 
	 
	 	 
	 @Column(name="num_Total")
	 private BigDecimal numTotal;
	 
		@Column(name="txt_price_group")
		private String txtPriceGroup;
		
		@Column(name="txt_xml_sent")
		private String txtXMSent;
		
		@Column(name="txt_return_msg")
		private String txtReturnMsg;
		
		@Column(name="txt_xml_receive")
		private String txtXMReceive;
		
		@Column(name="txt_cancel_reason")
		private String txtCancelReason;
		
		@Column(name="bln_is_issued")
		private Boolean blnIsIssued;
		
		@Column(name="bln_is_received")
		private Boolean blnIsReceived;
		
		@Temporal(TemporalType.DATE)
		@Column(name="dte_battery_date")
		private Date dteBatteryDate;
		
		@Column(name="txt_chassis_no")
		private String txtChassisNo;
		
		@Column(name="txt_gatepass_no")
		private String txtGatePassNo;
		
		@Column(name="txt_warrnty_no")
		private String txtWarrantyNo;
		
		@Column(name="txt_customer_reamrks")
		private String txtCustomerRemarks;
		
		@Column(name="txt_engine_no")
		private String txtEngineNo;
		
		@Column(name="txt_registration_no")
		private String txtRegistrationNo;
		
		@Column(name="txt_delivery_part_no")
		private String txtDeliveryPartNo;
		
		@Temporal(TemporalType.DATE)
		@Column(name="dte_delivery_date_actual")
		private Date dteDeliveryDateActual;
		
		@Column(name="num_qty_actual")
		private BigDecimal numQtyActual;
		
		@Column(name="txt_reason_for_Cancel")
		private String txtReasonforCancel;
		
		@Column(name="txt_cheque_no")
		private String txtChequeNo;
		
		@Column(name="customer_pic",columnDefinition="mediumblob")
		private byte[] customer_pic;
	
		
		/*@JsonIgnoreProperties(value={"slsTblSaleOrder"})
		@OneToMany(fetch=FetchType.LAZY,mappedBy="slsTblSaleOrder")*/
		@OneToMany(mappedBy = "slsTblSaleOrder", fetch = FetchType.LAZY)
		@JsonIgnore
		private List<SOPaymentDocument> paymentDocuments=new ArrayList<SOPaymentDocument>();


	@Column(name="txtSoapReturnType")
	private String txtSoapReturnType;

	@Column(name="txtSoapResponseMsg")
	private String txtSoapResponseMsg;


	@Column(name="num_level")
	private Integer numLevel;

	@Column(name="ser_approvedby_id1")
	private Integer serApprovedbyId1;

	@Column(name="ser_approvedby_id2")
	private Integer serApprovedbyId2;

	@Column(name="ser_approvedby_id3")
	private Integer serApprovedbyId3;

	@Column(name="ser_approvedby_id4")
	private Integer serApprovedbyId4;

	@Column(name="ser_approvedby_id5")
	private Integer serApprovedbyId5;

	@Column(name="ser_approvedby_id6")
	private Integer serApprovedbyId6;

	@Column(name="txt_status1")
	private String txtStatus1;

	@Column(name="txt_status2")
	private String txtStatus2;


	@Column(name="txt_status3")
	private String txtStatus3;

	@Column(name="txt_status4")
	private String txtStatus4;

	@Column(name="txt_status5")
	private String txtStatus5;

	@Column(name="txt_status6")
	private String txtStatus6;

	@Column(name="txt_status")
	private String txtStatus;


	@Column(name="txt_color")
	private String txtColor;

	@Column(name="txt_order_type")
	private String txtpOrderType;

	@Column(name="txt_payment_term")
	private String txtPaymentTerm;

	@Column(name="txt_Level")
	private String txtLevel;

	@Column(name="bl_is_dealer_adjustment")
	private Boolean blIsDealerAdjustment;

	@Column(name="dte_approveddate1")
	private Timestamp dteApproveddate1;

	@Column(name="dte_approveddate2")
	private Timestamp dteApproveddate2;

	@Column(name="dte_approveddate3")
	private Timestamp dteApproveddate3;

	@Column(name="dte_approveddate4")
	private Timestamp dteApproveddate4;

	@Column(name="dte_approveddate5")
	private Timestamp dteApproveddate5;


	@Column(name="txt_issue_code")
	private String txtIssueCode;


	@Column(name="dte_issue_date")
	private Date dteIssuedate;

/*	@Column(name="dte_issue_time")
	private String dteIssueTime;*/

	@Column(name="notes")
	private String notes;


	public String getTxtoriginalStatus() {
		return txtoriginalStatus;
	}

	public void setTxtoriginalStatus(String txtoriginalStatus) {
		this.txtoriginalStatus = txtoriginalStatus;
	}

	@Column(name="txt_orginal_status")
	private String txtoriginalStatus;


	/*public String getDteIssueTime() {
		return dteIssueTime;
	}*/

	public String getTxtSapInvoiceNo() {
		return txtSapInvoiceNo;
	}

	public void setTxtSapInvoiceNo(String txtSapInvoiceNo) {
		this.txtSapInvoiceNo = txtSapInvoiceNo;
	}

	@Column(name="txt_SapInvoiceNo")
	private String txtSapInvoiceNo;



	@Column(name="fbr_invoice_date")
	private String fbrinvoiceDate;

	@Column(name="fbr_invoice_no")
	private String fbrinvoiceno;

	public String getFbrinvoiceDate() {
		return fbrinvoiceDate;
	}

	public void setFbrinvoiceDate(String fbrinvoiceDate) {
		this.fbrinvoiceDate = fbrinvoiceDate;
	}

	public String getFbrinvoiceno() {
		return fbrinvoiceno;
	}

	public void setFbrinvoiceno(String fbrinvoiceno) {
		this.fbrinvoiceno = fbrinvoiceno;
	}

	public String getTimeString() {
		return timeString;
	}

	public void setTimeString(String timeString) {
		this.timeString = timeString;
	}

	@Column(name="time_string")
	private String timeString;

	/*public void setDteIssueTime(String dteIssueTime) {
		this.dteIssueTime = dteIssueTime;
	}*/

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	//bi-directional many-to-one association to CfgTblCity
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;

	//bi-directional many-to-one association to CfgTblCustomer
	@ManyToOne
	@JoinColumn(name="ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;


	//bi-directional many-to-one association to SlsTblSoDetail
	@OneToMany(mappedBy="slsTblSaleOrder", fetch = FetchType.LAZY)
	@JsonIgnore
	//@JsonProperty
	private List<SlsTblSoDetail> slsTblSoDetails;

	@Column(name="num_amount_received")
	private BigDecimal numAmountReceived;

	@Column(name="num_remaining_balance")
	private BigDecimal numRemainingBalance;

	//bi-directional many-to-one association to CfgTblCustomer
	@ManyToOne
	@JoinColumn(name="ser_deal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private SlsTblDeal slsTblDeal;


	public Boolean getBlIsVendor() {
		return blIsVendor;
	}

	public void setBlIsVendor(Boolean blIsVendor) {
		this.blIsVendor = blIsVendor;
	}

	@Column(name="bl_is_vendor")
	private Boolean blIsVendor;

	public String getTxtSoapReturnType() {
		return txtSoapReturnType;
	}

	public void setTxtSoapReturnType(String txtSoapReturnType) {
		this.txtSoapReturnType = txtSoapReturnType;
	}

	public String getTxtSoapResponseMsg() {
		return txtSoapResponseMsg;
	}

	public void setTxtSoapResponseMsg(String txtSoapResponseMsg) {
		this.txtSoapResponseMsg = txtSoapResponseMsg;
	}

	public String getTxtVehicleType() {
		return txtVehicleType;
	}

	public void setTxtVehicleType(String txtVehicleType) {
		this.txtVehicleType = txtVehicleType;
	}

	public String getTxtDeliveryTime() {
		return txtDeliveryTime;
	}

	public void setTxtDeliveryTime(String txtDeliveryTime) {
		this.txtDeliveryTime = txtDeliveryTime;
	}
	

	
	public String getTxtIssueCode() {
		return txtIssueCode;
	}

	public void setTxtIssueCode(String txtIssueCode) {
		this.txtIssueCode = txtIssueCode;
	}

	public Date getDteIssuedate() {
		return dteIssuedate;
	}

	public void setDteIssuedate(Date dteIssuedate) {
		this.dteIssuedate = dteIssuedate;
	}



	public SlsTblSaleOrder() {
	}

	public Integer getSerSaleOrderId() {
		return this.serSaleOrderId;
	}

	public void setSerSaleOrderId(Integer serSaleOrderId) {
		this.serSaleOrderId = serSaleOrderId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnDemandCompletionStatus() {
		return this.blnDemandCompletionStatus;
	}

	public void setBlnDemandCompletionStatus(Boolean blnDemandCompletionStatus) {
		this.blnDemandCompletionStatus = blnDemandCompletionStatus;
	}

	public Boolean getBlnIsApproved() {
		return this.blnIsApproved;
	}

	public void setBlnIsApproved(Boolean blnIsApproved) {
		this.blnIsApproved = blnIsApproved;
	}

	public Boolean getBlnIsCompleted() {
		return this.blnIsCompleted;
	}

	public void setBlnIsCompleted(Boolean blnIsCompleted) {
		this.blnIsCompleted = blnIsCompleted;
	}

	public Timestamp getDteCreateddate() {
		return this.dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Date getDteDate() {
		return this.dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public Date getDteDueDate() {
		return this.dteDueDate;
	}

	public void setDteDueDate(Date dteDueDate) {
		this.dteDueDate = dteDueDate;
	}

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public BigDecimal getNumExciseDuty() {
		return this.numExciseDuty;
	}

	public void setNumExciseDuty(BigDecimal numExciseDuty) {
		this.numExciseDuty = numExciseDuty;
	}

	public BigDecimal getNumFreight() {
		return this.numFreight;
	}

	public void setNumFreight(BigDecimal numFreight) {
		this.numFreight = numFreight;
	}

	public BigDecimal getNumSalesTax() {
		return this.numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public Integer getPriority() {
		return this.priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Integer getSerApprovedbyId() {
		return this.serApprovedbyId;
	}

	public void setSerApprovedbyId(Integer serApprovedbyId) {
		this.serApprovedbyId = serApprovedbyId;
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

	public Integer getSerPreparedbyId() {
		return this.serPreparedbyId;
	}

	public void setSerPreparedbyId(Integer serPreparedbyId) {
		this.serPreparedbyId = serPreparedbyId;
	}

	public String getTxtBillingAddress() {
		return this.txtBillingAddress;
	}

	public void setTxtBillingAddress(String txtBillingAddress) {
		this.txtBillingAddress = txtBillingAddress;
	}

	public String getTxtDealer() {
		return this.txtDealer;
	}

	public void setTxtDealer(String txtDealer) {
		this.txtDealer = txtDealer;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtDestination() {
		return this.txtDestination;
	}

	public void setTxtDestination(String txtDestination) {
		this.txtDestination = txtDestination;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtPaymnetTerms() {
		return this.txtPaymnetTerms;
	}

	public void setTxtPaymnetTerms(String txtPaymnetTerms) {
		this.txtPaymnetTerms = txtPaymnetTerms;
	}

	public String getTxtPriceTerms() {
		return this.txtPriceTerms;
	}

	public void setTxtPriceTerms(String txtPriceTerms) {
		this.txtPriceTerms = txtPriceTerms;
	}

	public String getTxtReceiveStatus() {
		return this.txtReceiveStatus;
	}

	public void setTxtReceiveStatus(String txtReceiveStatus) {
		this.txtReceiveStatus = txtReceiveStatus;
	}

	public String getTxtSaleOrderNo() {
		return this.txtSaleOrderNo;
	}

	public void setTxtSaleOrderNo(String txtSaleOrderNo) {
		this.txtSaleOrderNo = txtSaleOrderNo;
	}

	public String getTxtShipBy() {
		return this.txtShipBy;
	}

	public void setTxtShipBy(String txtShipBy) {
		this.txtShipBy = txtShipBy;
	}

	public String getTxtShippingAddress1() {
		return this.txtShippingAddress1;
	}

	public void setTxtShippingAddress1(String txtShippingAddress1) {
		this.txtShippingAddress1 = txtShippingAddress1;
	}

	public String getTxtShippingAddress2() {
		return this.txtShippingAddress2;
	}

	public void setTxtShippingAddress2(String txtShippingAddress2) {
		this.txtShippingAddress2 = txtShippingAddress2;
	}

	public String getTxtShippingAddress3() {
		return this.txtShippingAddress3;
	}

	public void setTxtShippingAddress3(String txtShippingAddress3) {
		this.txtShippingAddress3 = txtShippingAddress3;
	}

	public String getTxtStatus() {
		return this.txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	

	public CfgTblCity getCfgTblCity() {
		return this.cfgTblCity;
	}

	public void setCfgTblCity(CfgTblCity cfgTblCity) {
		this.cfgTblCity = cfgTblCity;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return this.cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public List<SlsTblSoDetail> getSlsTblSoDetails() {
		return this.slsTblSoDetails;
	}

	public void setSlsTblSoDetails(List<SlsTblSoDetail> slsTblSoDetails) {
		this.slsTblSoDetails = slsTblSoDetails;
	}

	public SlsTblSoDetail addSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		getSlsTblSoDetails().add(slsTblSoDetail);
		slsTblSoDetail.setSlsTblSaleOrder(this);

		return slsTblSoDetail;
	}

	public SlsTblSoDetail removeSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		getSlsTblSoDetails().remove(slsTblSoDetail);
		slsTblSoDetail.setSlsTblSaleOrder(null);

		return slsTblSoDetail;
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


	private String dte_date_from;
	
	
	private String dte_date_to;
	
	
	public Boolean getBlIsComplementry() {
		return blIsComplementry;
	}

	public void setBlIsComplementry(Boolean blIsComplementry) {
		this.blIsComplementry = blIsComplementry;
	}

	public Boolean getBlIsSplit() {
		return blIsSplit;
	}

	public void setBlIsSplit(Boolean blIsSplit) {
		this.blIsSplit = blIsSplit;
	}

	@Column(name="bl_is_complementry")
	private Boolean blIsComplementry;
	
	@Column(name="bl_is_split")
	private Boolean blIsSplit;
	
	
	//bi-directional many-to-one association to CfgTblCustomer
		@ManyToOne
		@JoinColumn(name="ser_dealer_id")
		private CfgTblCustomer cfgTblDealer;

		public CfgTblCustomer getCfgTblDealer() {
			return cfgTblDealer;
		}

		public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
			this.cfgTblDealer = cfgTblDealer;
		}
		
		@ManyToOne
		@JoinColumn(name="ser_dealer_id_one")
		private CfgTblCustomer cfgTblDealerOne;
		
		public CfgTblCustomer getCfgTblDealerOne() {
			return cfgTblDealerOne;
		}

		public void setCfgTblDealerOne(CfgTblCustomer cfgTblDealerOne) {
			this.cfgTblDealerOne = cfgTblDealerOne;
		}

		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_distribution_channel_id")
		private CfgTblDistributionChannel cfgTblDistributionChannel;
		
		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_division_id")
		private CfgTblDivision cfgTblDivision;
		
		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_document_type_id")
		private CfgTblDocumentType cfgTblDocumentType;
		
		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_inco_terms_id")
		private CfgTblIncoTerm cfgTblIncoTerm;
		
		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_payment_terms_id")
		private CfgTblPaymentTerm cfgTblPaymentTerm;
		
		//bi-directional many-to-one association to CfgTblCity
		@ManyToOne
		@JoinColumn(name="ser_sales_organization_id")
		private CfgTblSalesOrganization cfgTblSalesOrganization;

		public CfgTblDistributionChannel getCfgTblDistributionChannel() {
			return cfgTblDistributionChannel;
		}

		public void setCfgTblDistributionChannel(CfgTblDistributionChannel cfgTblDistributionChannel) {
			this.cfgTblDistributionChannel = cfgTblDistributionChannel;
		}

		public CfgTblDivision getCfgTblDivision() {
			return cfgTblDivision;
		}

		public void setCfgTblDivision(CfgTblDivision cfgTblDivision) {
			this.cfgTblDivision = cfgTblDivision;
		}

		public CfgTblDocumentType getCfgTblDocumentType() {
			return cfgTblDocumentType;
		}

		public void setCfgTblDocumentType(CfgTblDocumentType cfgTblDocumentType) {
			this.cfgTblDocumentType = cfgTblDocumentType;
		}

		public CfgTblIncoTerm getCfgTblIncoTerm() {
			return cfgTblIncoTerm;
		}

		public void setCfgTblIncoTerm(CfgTblIncoTerm cfgTblIncoTerm) {
			this.cfgTblIncoTerm = cfgTblIncoTerm;
		}

		public CfgTblPaymentTerm getCfgTblPaymentTerm() {
			return cfgTblPaymentTerm;
		}

		public void setCfgTblPaymentTerm(CfgTblPaymentTerm cfgTblPaymentTerm) {
			this.cfgTblPaymentTerm = cfgTblPaymentTerm;
		}

		public CfgTblSalesOrganization getCfgTblSalesOrganization() {
			return cfgTblSalesOrganization;
		}

		public void setCfgTblSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization) {
			this.cfgTblSalesOrganization = cfgTblSalesOrganization;
		}
		
		
		
		//bi-directional many-to-one association to CfgTblCity
				@ManyToOne
				@JoinColumn(name="ser_Employee_id")
				private HrTblEmployee hrTblEmployee;
				
				@Column(name="txt_po_no")
				private String txtPONo;
				
				@Temporal(TemporalType.DATE)
				@Column(name="dte_po_date")
				private Date dtePODate;





				public HrTblEmployee getHrTblEmployee() {
					return hrTblEmployee;
				}

				public void setHrTblEmployee(HrTblEmployee hrTblEmployee) {
					this.hrTblEmployee = hrTblEmployee;
				}

				public String getTxtPONo() {
					return txtPONo;
				}

				public void setTxtPONo(String txtPONo) {
					this.txtPONo = txtPONo;
				}

				public Date getDtePODate() {
					return dtePODate;
				}

				public void setDtePODate(Date dtePODate) {
					this.dtePODate = dtePODate;
				}
				
				
				
				@Column(name="txt_sap_no")
				private String txtSapNo;

				public String getTxtSapNo() {
					return txtSapNo;
				}

				public void setTxtSapNo(String txtSapNo) {
					this.txtSapNo = txtSapNo;
				}
				
				

				@Lob
				@Column(name="txt_image")
				private byte[] txtImage;
				
				@Column(name="profile_pic",columnDefinition="mediumblob")
				private byte[] profile_pic;
				
				@Transient
				private MultipartFile upload_profile_pic;

				public byte[] getTxtImage() {
					return txtImage;
				}

				public void setTxtImage(byte[] txtImage) {
					this.txtImage = txtImage;
				}

				public byte[] getProfile_pic() {
					return profile_pic;
				}

				public void setProfile_pic(byte[] profile_pic) {
					this.profile_pic = profile_pic;
				}

				public MultipartFile getUpload_profile_pic() {
					return upload_profile_pic;
				}

				public void setUpload_profile_pic(MultipartFile upload_profile_pic) {
					this.upload_profile_pic = upload_profile_pic;
				}
				
				
				@Column(name="txt_image_name")
				private String txtImageName;
				
				@Column(name="txt_image_type")
				private String txtImageType;

				public String getTxtImageName() {
					return txtImageName;
				}

				public void setTxtImageName(String txtImageName) {
					this.txtImageName = txtImageName;
				}

				public String getTxtImageType() {
					return txtImageType;
				}

				public void setTxtImageType(String txtImageType) {
					this.txtImageType = txtImageType;
				}

				
				@Column(name="txt_dc_status")
				private String txtDCStatus;
				
				@Column(name="txt_invoice_status")
				private String txtInvoiceStatus;
				
				@Column(name="txt_dc_no")
				private String txtDCNo;
				
				@Column(name="txt_invoice_no")
				private String txtInvoiceNo;
				
				@Column(name="txt_dc_date")
				private String txtDCDate;
				
				@Column(name="txt_invoice_Date")
				private String txtInvoiceDate;
				
				@Column(name="txt_order_approval_date")
				private String txtOrderapprovalDate;

				public String getTxtDCStatus() {
					return txtDCStatus;
				}

				public void setTxtDCStatus(String txtDCStatus) {
					this.txtDCStatus = txtDCStatus;
				}

				public String getTxtInvoiceStatus() {
					return txtInvoiceStatus;
				}

				public void setTxtInvoiceStatus(String txtInvoiceStatus) {
					this.txtInvoiceStatus = txtInvoiceStatus;
				}

				public String getTxtDCNo() {
					return txtDCNo;
				}

				public void setTxtDCNo(String txtDCNo) {
					this.txtDCNo = txtDCNo;
				}

				public String getTxtInvoiceNo() {
					return txtInvoiceNo;
				}

				public void setTxtInvoiceNo(String txtInvoiceNo) {
					this.txtInvoiceNo = txtInvoiceNo;
				}

				public String getTxtDCDate() {
					return txtDCDate;
				}

				public void setTxtDCDate(String txtDCDate) {
					this.txtDCDate = txtDCDate;
				}

				public String getTxtInvoiceDate() {
					return txtInvoiceDate;
				}

				public void setTxtInvoiceDate(String txtInvoiceDate) {
					this.txtInvoiceDate = txtInvoiceDate;
				}

				public Timestamp getDteRSMApproval() {
					return dteRSMApproval;
				}

				public void setDteRSMApproval(Timestamp dteRSMApproval) {
					this.dteRSMApproval = dteRSMApproval;
				}

				public Timestamp getDteFinalApproval() {
					return dteFinalApproval;
				}

				public void setDteFinalApproval(Timestamp dteFinalApproval) {
					this.dteFinalApproval = dteFinalApproval;
				}

				public BigDecimal getNumQuantity() {
					return numQuantity;
				}

				public void setNumQuantity(BigDecimal numQuantity) {
					this.numQuantity = numQuantity;
				}

				public CfgTblProduct getCfgTblProduct() {
					return cfgTblProduct;
				}

				public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
					this.cfgTblProduct = cfgTblProduct;
				}

				public String getTxtOrderapprovalDate() {
					return txtOrderapprovalDate;
				}

				public void setTxtOrderapprovalDate(String txtOrderapprovalDate) {
					this.txtOrderapprovalDate = txtOrderapprovalDate;
				}

				
				@Column(name="txt_dc_qty")
				private String txtDCQty;


				public String getTxtDCQty() {
					return txtDCQty;
				}

				public void setTxtDCQty(String txtDCQty) {
					this.txtDCQty = txtDCQty;
				}

				public BigDecimal getNumPrice() {
					return numPrice;
				}

				public void setNumPrice(BigDecimal numPrice) {
					this.numPrice = numPrice;
				}

				public BigDecimal getNumNetAmount() {
					return numNetAmount;
				}

				public void setNumNetAmount(BigDecimal numNetAmount) {
					this.numNetAmount = numNetAmount;
				}

				public Boolean getBlnFromSAP() {
					return blnFromSAP;
				}

				public void setBlnFromSAP(Boolean blnFromSAP) {
					this.blnFromSAP = blnFromSAP;
				}

				public Boolean getBlnIsIncoTerm() {
					return blnIsIncoTerm;
				}

				public void setBlnIsIncoTerm(Boolean blnIsIncoTerm) {
					this.blnIsIncoTerm = blnIsIncoTerm;
				}

				public String getTxtCustomer() {
					return txtCustomer;
				}

				public void setTxtCustomer(String txtCustomer) {
					this.txtCustomer = txtCustomer;
				}

				public String getTxtProduct() {
					return txtProduct;
				}

				public void setTxtProduct(String txtProduct) {
					this.txtProduct = txtProduct;
				}

				public BigDecimal getNumAmount() {
					return numAmount;
				}

				public void setNumAmount(BigDecimal numAmount) {
					this.numAmount = numAmount;
				}

				public BigDecimal getNumAmountReceived() {
					return numAmountReceived;
				}

				public void setNumAmountReceived(BigDecimal numAmountReceived) {
					this.numAmountReceived = numAmountReceived;
				}

				public BigDecimal getNumRemainingBalance() {
					return numRemainingBalance;
				}

				public void setNumRemainingBalance(BigDecimal numRemainingBalance) {
					this.numRemainingBalance = numRemainingBalance;
				}

				public BigDecimal getNumDiscount() {
					return numDiscount;
				}

				public void setNumDiscount(BigDecimal numDiscount) {
					this.numDiscount = numDiscount;
				}

				public BigDecimal getNumDiscountAmount() {
					return numDiscountAmount;
				}

				public void setNumDiscountAmount(BigDecimal numDiscountAmount) {
					this.numDiscountAmount = numDiscountAmount;
				}

				public BigDecimal getNumAmountAfterDiscount() {
					return numAmountAfterDiscount;
				}

				public void setNumAmountAfterDiscount(BigDecimal numAmountAfterDiscount) {
					this.numAmountAfterDiscount = numAmountAfterDiscount;
				}

				public BigDecimal getNumFED() {
					return numFED;
				}

				public void setNumFED(BigDecimal numFED) {
					this.numFED = numFED;
				}

				public BigDecimal getNumFEDAmount() {
					return numFEDAmount;
				}

				public void setNumFEDAmount(BigDecimal numFEDAmount) {
					this.numFEDAmount = numFEDAmount;
				}

				public BigDecimal getNumAmountAfterFED() {
					return numAmountAfterFED;
				}

				public void setNumAmountAfterFED(BigDecimal numAmountAfterFED) {
					this.numAmountAfterFED = numAmountAfterFED;
				}

				public BigDecimal getNumSalesTaxAmount() {
					return numSalesTaxAmount;
				}

				public void setNumSalesTaxAmount(BigDecimal numSalesTaxAmount) {
					this.numSalesTaxAmount = numSalesTaxAmount;
				}

				public BigDecimal getNumAmountAfterST() {
					return numAmountAfterST;
				}

				public void setNumAmountAfterST(BigDecimal numAmountAfterST) {
					this.numAmountAfterST = numAmountAfterST;
				}

				public BigDecimal getNumCVT() {
					return numCVT;
				}

				public void setNumCVT(BigDecimal numCVT) {
					this.numCVT = numCVT;
				}

				public BigDecimal getNumCVTAmount() {
					return numCVTAmount;
				}

				public void setNumCVTAmount(BigDecimal numCVTAmount) {
					this.numCVTAmount = numCVTAmount;
				}

				public BigDecimal getNumAmountAfterCVT() {
					return numAmountAfterCVT;
				}

				public void setNumAmountAfterCVT(BigDecimal numAmountAfterCVT) {
					this.numAmountAfterCVT = numAmountAfterCVT;
				}

				public BigDecimal getNumTaxOnFreight() {
					return numTaxOnFreight;
				}

				public void setNumTaxOnFreight(BigDecimal numTaxOnFreight) {
					this.numTaxOnFreight = numTaxOnFreight;
				}

				public BigDecimal getNumTOFAmount() {
					return numTOFAmount;
				}

				public void setNumTOFAmount(BigDecimal numTOFAmount) {
					this.numTOFAmount = numTOFAmount;
				}

				public BigDecimal getNumGrossValue() {
					return numGrossValue;
				}

				public void setNumGrossValue(BigDecimal numGrossValue) {
					this.numGrossValue = numGrossValue;
				}

				public BigDecimal getNumAvanceTax() {
					return numAvanceTax;
				}

				public void setNumAvanceTax(BigDecimal numAvanceTax) {
					this.numAvanceTax = numAvanceTax;
				}

				public BigDecimal getNumTotal() {
					return numTotal;
				}

				public void setNumTotal(BigDecimal numTotal) {
					this.numTotal = numTotal;
				}

				public Date getDteDeliveryDate() {
					return dteDeliveryDate;
				}

				public void setDteDeliveryDate(Date dteDeliveryDate) {
					this.dteDeliveryDate = dteDeliveryDate;
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

				public String getTxtLevel() {
					return txtLevel;
				}

				public void setTxtLevel(String txtLevel) {
					this.txtLevel = txtLevel;
				}

				public Integer getNumLevel() {
					return numLevel;
				}

				public void setNumLevel(Integer numLevel) {
					this.numLevel = numLevel;
				}
			
				
				
				
				@Column(name="txt_slip_no")
				private String txtSlipNo;
				
				@Column(name="txt_payment_method")
				private String txtPaymentMethod;

				@Column(name="num_payment_received")
				private BigDecimal numPaymentReceived;


				public String getTxtSlipNo() {
					return txtSlipNo;
				}

				public void setTxtSlipNo(String txtSlipNo) {
					this.txtSlipNo = txtSlipNo;
				}

				public String getTxtPaymentMethod() {
					return txtPaymentMethod;
				}

				public void setTxtPaymentMethod(String txtPaymentMethod) {
					this.txtPaymentMethod = txtPaymentMethod;
				}

				public BigDecimal getNumPaymentReceived() {
					return numPaymentReceived;
				}

				public void setNumPaymentReceived(BigDecimal numPaymentReceived) {
					this.numPaymentReceived = numPaymentReceived;
				}

				public String getTxtpOrderType() {
					return txtpOrderType;
				}

				public void setTxtpOrderType(String txtpOrderType) {
					this.txtpOrderType = txtpOrderType;
				}

				public String getTxtPaymentTerm() {
					return txtPaymentTerm;
				}

				public void setTxtPaymentTerm(String txtPaymentTerm) {
					this.txtPaymentTerm = txtPaymentTerm;
				}

				public Boolean getBlIsDealerAdjustment() {
					return blIsDealerAdjustment;
				}

				public void setBlIsDealerAdjustment(Boolean blIsDealerAdjustment) {
					this.blIsDealerAdjustment = blIsDealerAdjustment;
				}

				

				public SlsTblDeal getSlsTblDeal() {
					return slsTblDeal;
				}

				public void setSlsTblDeal(SlsTblDeal slsTblDeal) {
					this.slsTblDeal = slsTblDeal;
				}

				public String getTxtPriceGroup() {
					return txtPriceGroup;
				}

				public void setTxtPriceGroup(String txtPriceGroup) {
					this.txtPriceGroup = txtPriceGroup;
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

				public String getTxtCancelReason() {
					return txtCancelReason;
				}

				public void setTxtCancelReason(String txtCancelReason) {
					this.txtCancelReason = txtCancelReason;
				}
				
				
				@Column(name="txt_error_msg_from_Sap")
				private String txtErrorMsgFromSap;
				
				@Column(name="bl_is_posted_to_Sap")
				private Boolean blIsPOSTEDToSAP;
				
				@Column(name="txtType")
				private String txtType;
				
				@Column(name="bl_is_gal")
				private Boolean blIsGAL;
				
				

				
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

				public Boolean getBlnIsIssued() {
					return blnIsIssued;
				}

				public void setBlnIsIssued(Boolean blnIsIssued) {
					this.blnIsIssued = blnIsIssued;
				}

				public Boolean getBlnIsReceived() {
					return blnIsReceived;
				}

				public void setBlnIsReceived(Boolean blnIsReceived) {
					this.blnIsReceived = blnIsReceived;
				}

				public Date getDteBatteryDate() {
					return dteBatteryDate;
				}

				public void setDteBatteryDate(Date dteBatteryDate) {
					this.dteBatteryDate = dteBatteryDate;
				}

				public String getTxtChassisNo() {
					return txtChassisNo;
				}

				public void setTxtChassisNo(String txtChassisNo) {
					this.txtChassisNo = txtChassisNo;
				}

				public String getTxtCustomerRemarks() {
					return txtCustomerRemarks;
				}

				public void setTxtCustomerRemarks(String txtCustomerRemarks) {
					this.txtCustomerRemarks = txtCustomerRemarks;
				}

				public byte[] getCustomer_pic() {
					return customer_pic;
				}

				public void setCustomer_pic(byte[] customer_pic) {
					this.customer_pic = customer_pic;
				}

				public String getTxtGatePassNo() {
					return txtGatePassNo;
				}

				public void setTxtGatePassNo(String txtGatePassNo) {
					this.txtGatePassNo = txtGatePassNo;
				}

				public String getTxtWarrantyNo() {
					return txtWarrantyNo;
				}

				public void setTxtWarrantyNo(String txtWarrantyNo) {
					this.txtWarrantyNo = txtWarrantyNo;
				}

				public String getTxtEngineNo() {
					return txtEngineNo;
				}

				public void setTxtEngineNo(String txtEngineNo) {
					this.txtEngineNo = txtEngineNo;
				}

				public String getTxtRegistrationNo() {
					return txtRegistrationNo;
				}

				public void setTxtRegistrationNo(String txtRegistrationNo) {
					this.txtRegistrationNo = txtRegistrationNo;
				}

				public String getTxtDeliveryPartNo() {
					return txtDeliveryPartNo;
				}

				public void setTxtDeliveryPartNo(String txtDeliveryPartNo) {
					this.txtDeliveryPartNo = txtDeliveryPartNo;
				}

				public Date getDteDeliveryDateActual() {
					return dteDeliveryDateActual;
				}

				public void setDteDeliveryDateActual(Date dteDeliveryDateActual) {
					this.dteDeliveryDateActual = dteDeliveryDateActual;
				}

				public BigDecimal getNumQtyActual() {
					return numQtyActual;
				}

				public void setNumQtyActual(BigDecimal numQtyActual) {
					this.numQtyActual = numQtyActual;
				}

				public String getTxtReasonforCancel() {
					return txtReasonforCancel;
				}

				public void setTxtReasonforCancel(String txtReasonforCancel) {
					this.txtReasonforCancel = txtReasonforCancel;
				}

				public BigDecimal getNumInvoiceAmount() {
					return numInvoiceAmount;
				}

				public void setNumInvoiceAmount(BigDecimal numInvoiceAmount) {
					this.numInvoiceAmount = numInvoiceAmount;
				}

				public BigDecimal getNumFEDOnFreight() {
					return numFEDOnFreight;
				}

				public void setNumFEDOnFreight(BigDecimal numFEDOnFreight) {
					this.numFEDOnFreight = numFEDOnFreight;
				}

				public BigDecimal getNumFreightAmountafterTax() {
					return numFreightAmountafterTax;
				}

				public void setNumFreightAmountafterTax(BigDecimal numFreightAmountafterTax) {
					this.numFreightAmountafterTax = numFreightAmountafterTax;
				}

				public BigDecimal getNumCommission() {
					return numCommission;
				}

				public void setNumCommission(BigDecimal numCommission) {
					this.numCommission = numCommission;
				}

				public Boolean getBlIsSales() {
					return blIsSales;
				}

				public void setBlIsSales(Boolean blIsSales) {
					this.blIsSales = blIsSales;
				}

				public BigDecimal getNumFabrication() {
					return numFabrication;
				}

				public void setNumFabrication(BigDecimal numFabrication) {
					this.numFabrication = numFabrication;
				}

				public String getTxtColor() {
					return txtColor;
				}

				public void setTxtColor(String txtColor) {
					this.txtColor = txtColor;
				}

				public String getTxtChequeNo() {
					return txtChequeNo;
				}

				public void setTxtChequeNo(String txtChequeNo) {
					this.txtChequeNo = txtChequeNo;
				}

				public List<SOPaymentDocument> getPaymentDocuments() {
					return paymentDocuments;
				}

				public void setPaymentDocuments(List<SOPaymentDocument> paymentDocuments) {
					this.paymentDocuments = paymentDocuments;
				}

				public Integer getSerApprovedbyId4() {
					return serApprovedbyId4;
				}

				public void setSerApprovedbyId4(Integer serApprovedbyId4) {
					this.serApprovedbyId4 = serApprovedbyId4;
				}

				public Integer getSerApprovedbyId5() {
					return serApprovedbyId5;
				}

				public void setSerApprovedbyId5(Integer serApprovedbyId5) {
					this.serApprovedbyId5 = serApprovedbyId5;
				}

				public String getTxtStatus4() {
					return txtStatus4;
				}

				public void setTxtStatus4(String txtStatus4) {
					this.txtStatus4 = txtStatus4;
				}

				public String getTxtStatus5() {
					return txtStatus5;
				}

				public void setTxtStatus5(String txtStatus5) {
					this.txtStatus5 = txtStatus5;
				}

				public Timestamp getDteApproveddate4() {
					return dteApproveddate4;
				}

				public void setDteApproveddate4(Timestamp dteApproveddate4) {
					this.dteApproveddate4 = dteApproveddate4;
				}

				public Timestamp getDteApproveddate5() {
					return dteApproveddate5;
				}

				public void setDteApproveddate5(Timestamp dteApproveddate5) {
					this.dteApproveddate5 = dteApproveddate5;
				}

	@Column(name = "txt_marketing_status")
	private String txtMarketingRemarks;

	@Column(name = "txt_audit_status")
	private String txtAuditRemarks;

	@Column(name = "txt_tax_status")
	private String txtTaxRemarks;

	@Column(name = "txt_finance_status")
	private String txtFinanceRemarks;

	@Column(name = "txt_procurement_status")
	private String txtProcurementRemarks;


	@Column(name = "txt_marketing_head_approved")
	private String txtMarketingHeadApproved;

	@Column(name = "txt_audit_head_approved")
	private String txtAuditHeadApproved;

	@Column(name = "txt_tax_head_approved")
	private String txtTaxHeadApproved;

	@Column(name = "txt_finance_head_approved")
	private String txtFinanceHeadApproved;

	@Column(name = "txt_payment_status")
	private String txtPaymentRemarks;

	@Column(name = "txt_procurement_head_approved")
	private String txtProcurementHeadApproved;


	@Column(name = "txt_payment_head_approved")
	private String txtPaymentHeadApproved;
	// Getters and Setters for each field

	public String getTxtMarketingRemarks() {
		return txtMarketingRemarks;
	}

	public void setTxtMarketingRemarks(String txtMarketingRemarks) {
		this.txtMarketingRemarks = txtMarketingRemarks;
	}

	public String getTxtAuditRemarks() {
		return txtAuditRemarks;
	}

	public void setTxtAuditRemarks(String txtAuditRemarks) {
		this.txtAuditRemarks = txtAuditRemarks;
	}

	public String getTxtTaxRemarks() {
		return txtTaxRemarks;
	}

	public void setTxtTaxRemarks(String txtTaxRemarks) {
		this.txtTaxRemarks = txtTaxRemarks;
	}

	public String getTxtFinanceRemarks() {
		return txtFinanceRemarks;
	}

	public void setTxtFinanceRemarks(String txtFinanceRemarks) {
		this.txtFinanceRemarks = txtFinanceRemarks;
	}

	public String getTxtProcurementRemarks() {
		return txtProcurementRemarks;
	}

	public void setTxtProcurementRemarks(String txtProcurementRemarks) {
		this.txtProcurementRemarks = txtProcurementRemarks;
	}

	public String getTxtMarketingHeadApproved() {
		return txtMarketingHeadApproved;
	}

	public void setTxtMarketingHeadApproved(String txtMarketingHeadApproved) {
		this.txtMarketingHeadApproved = txtMarketingHeadApproved;
	}

	public String getTxtAuditHeadApproved() {
		return txtAuditHeadApproved;
	}

	public void setTxtAuditHeadApproved(String txtAuditHeadApproved) {
		this.txtAuditHeadApproved = txtAuditHeadApproved;
	}

	public String getTxtTaxHeadApproved() {
		return txtTaxHeadApproved;
	}

	public void setTxtTaxHeadApproved(String txtTaxHeadApproved) {
		this.txtTaxHeadApproved = txtTaxHeadApproved;
	}

	public String getTxtFinanceHeadApproved() {
		return txtFinanceHeadApproved;
	}

	public void setTxtFinanceHeadApproved(String txtFinanceHeadApproved) {
		this.txtFinanceHeadApproved = txtFinanceHeadApproved;
	}

	public String getTxtProcurementHeadApproved() {
		return txtProcurementHeadApproved;
	}

	public void setTxtProcurementHeadApproved(String txtProcurementHeadApproved) {
		this.txtProcurementHeadApproved = txtProcurementHeadApproved;
	}

	public Integer getSerApprovedbyId6() {
		return serApprovedbyId6;
	}

	public void setSerApprovedbyId6(Integer serApprovedbyId6) {
		this.serApprovedbyId6 = serApprovedbyId6;
	}

	public String getTxtStatus6() {
		return txtStatus6;
	}

	public void setTxtStatus6(String txtStatus6) {
		this.txtStatus6 = txtStatus6;
	}

	public String getTxtPaymentRemarks() {
		return txtPaymentRemarks;
	}

	public void setTxtPaymentRemarks(String txtPaymentRemarks) {
		this.txtPaymentRemarks = txtPaymentRemarks;
	}

	public String getTxtPaymentHeadApproved() {
		return txtPaymentHeadApproved;
	}

	public void setTxtPaymentHeadApproved(String txtPaymentHeadApproved) {
		this.txtPaymentHeadApproved = txtPaymentHeadApproved;
	}

	@Column(name="ser_approvedby_name1")
	private String serApprovedbyName1;

	@Column(name="ser_approvedby_name2")
	private String serApprovedbyName2;

	@Column(name="ser_approvedby_name3")
	private String serApprovedbyName3;

	@Column(name="ser_approvedby_name4")
	private String serApprovedbyName4;

	@Column(name="ser_approvedby_name5")
	private String serApprovedbyName5;

	@Column(name="ser_approvedby_name6")
	private String serApprovedbyName6;

	public String getSerApprovedbyName1() {
		return serApprovedbyName1;
	}

	public void setSerApprovedbyName1(String serApprovedbyName1) {
		this.serApprovedbyName1 = serApprovedbyName1;
	}

	public String getSerApprovedbyName2() {
		return serApprovedbyName2;
	}

	public void setSerApprovedbyName2(String serApprovedbyName2) {
		this.serApprovedbyName2 = serApprovedbyName2;
	}

	public String getSerApprovedbyName3() {
		return serApprovedbyName3;
	}

	public void setSerApprovedbyName3(String serApprovedbyName3) {
		this.serApprovedbyName3 = serApprovedbyName3;
	}

	public String getSerApprovedbyName4() {
		return serApprovedbyName4;
	}

	public void setSerApprovedbyName4(String serApprovedbyName4) {
		this.serApprovedbyName4 = serApprovedbyName4;
	}

	public String getSerApprovedbyName5() {
		return serApprovedbyName5;
	}

	public void setSerApprovedbyName5(String serApprovedbyName5) {
		this.serApprovedbyName5 = serApprovedbyName5;
	}

	public String getSerApprovedbyName6() {
		return serApprovedbyName6;
	}

	public void setSerApprovedbyName6(String serApprovedbyName6) {
		this.serApprovedbyName6 = serApprovedbyName6;
	}
}
