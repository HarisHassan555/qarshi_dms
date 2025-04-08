package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the sls_tbl_sale_order database table.
 * 
 */

@Entity
@Table(name="sls_tbl_deal")
@NamedQuery(name="SlsTblDeal.findAll", query="SELECT s FROM SlsTblDeal  s")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlsTblDeal implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_sale_order_id")
	private Integer serSaleOrderId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_deal_ser_deal_id_seq")
	@SequenceGenerator(name = "sls_tbl_deal_ser_deal_id_seq", sequenceName = "sls_tbl_deal_ser_deal_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_deal_id")
	private Integer serDealId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_deal_status")
	private Boolean blnDealCompletionStatus;

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
	@Column(name="dte_start_date")
	private Date dteStartDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name="dte_end_date")
	private Date dteEndDate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_due_date")
	private Date dteDueDate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_excise_duty")
	private BigDecimal numExciseDuty;

	@Column(name="num_net_amount")
	private BigDecimal numNetAmount;


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

	@Column(name="txt_deal_no")
	private String txtDealNo;
	
	@Column(name="txt_deal_name")
	private String txtDealName;
	

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
	
	@Column(name="num_total_price")
	private BigDecimal numTotalPrice;
	
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

	@Column(name="num_sales_Tax_Sap")
	private String numSalesTaxSap;

	public String getNumSalesTaxSap() {
		return numSalesTaxSap;
	}

	public void setNumSalesTaxSap(String numSalesTaxSap) {
		this.numSalesTaxSap = numSalesTaxSap;
	}

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
	 
	 @Column(name="num_Total")
		private BigDecimal numTotal;
		
		@Column(name="num_amount")
		private BigDecimal numAmount;
		
		@Column(name="txtType")
		private String txtType;
		
		@Column(name="bl_is_gal")
		private Boolean blIsGAL;


	@Column(name="txtSoapReturnType")
	private String txtSoapReturnType;

	@Column(name="txtSoapResponseMsg")
	private String txtSoapResponseMsg;

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

	@Column(name="txt_status")
	private String txtStatus;
	
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

	@Column(name="txt_issue_code")
	private String txtIssueCode;

	
	@Column(name="dte_issue_date")
	private Date dteIssuedate;



	@Column(name="num_amount_received")
	private BigDecimal numAmountReceived;
	

	@Column(name="num_remaining_balance")
	private BigDecimal numRemainingBalance;

	//bi-directional many-to-one association to CfgTblCity
	@ManyToOne
	@JoinColumn(name="ser_city_id")
	private CfgTblCity cfgTblCity;

	//bi-directional many-to-one association to CfgTblCustomer
	@ManyToOne
	@JoinColumn(name="ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	
	
	//bi-directional many-to-one association to SlsTblSoDetail
	@OneToMany(mappedBy="slsTblDeal",fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JsonIgnoreProperties("slsTblDeal")
	private List<SlsTblDealDetails> slsTblDealDetails;
	

	public SlsTblDeal() {
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

				public Integer getSerDealId() {
					return serDealId;
				}

				public void setSerDealId(Integer serDealId) {
					this.serDealId = serDealId;
				}

				public Boolean getBlnDealCompletionStatus() {
					return blnDealCompletionStatus;
				}

				public void setBlnDealCompletionStatus(Boolean blnDealCompletionStatus) {
					this.blnDealCompletionStatus = blnDealCompletionStatus;
				}

				public Date getDteStartDate() {
					return dteStartDate;
				}

				public void setDteStartDate(Date dteStartDate) {
					this.dteStartDate = dteStartDate;
				}

				public Date getDteEndDate() {
					return dteEndDate;
				}

				public void setDteEndDate(Date dteEndDate) {
					this.dteEndDate = dteEndDate;
				}

				public String getTxtDealNo() {
					return txtDealNo;
				}

				public void setTxtDealNo(String txtDealNo) {
					this.txtDealNo = txtDealNo;
				}

				public String getTxtDealName() {
					return txtDealName;
				}

				public void setTxtDealName(String txtDealName) {
					this.txtDealName = txtDealName;
				}

				public List<SlsTblDealDetails> getSlsTblDealDetails() {
					return slsTblDealDetails;
				}

				public void setSlsTblDealDetails(List<SlsTblDealDetails> slsTblDealDetails) {
					this.slsTblDealDetails = slsTblDealDetails;
				}

				public BigDecimal getNumAmount() {
					return numAmount;
				}

				public void setNumAmount(BigDecimal numAmount) {
					this.numAmount = numAmount;
				}

				public BigDecimal getNumTotalPrice() {
					return numTotalPrice;
				}

				public void setNumTotalPrice(BigDecimal numTotalPrice) {
					this.numTotalPrice = numTotalPrice;
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
			
				 @Column(name="num_commission")
					private BigDecimal numCommission;
				 
				 @Column(name="num_fabrication")
				 private BigDecimal numFabrication;


				public BigDecimal getNumCommission() {
					return numCommission;
				}

				public void setNumCommission(BigDecimal numCommission) {
					this.numCommission = numCommission;
				}

				public BigDecimal getNumFabrication() {
					return numFabrication;
				}

				public void setNumFabrication(BigDecimal numFabrication) {
					this.numFabrication = numFabrication;
				}
				

				
}