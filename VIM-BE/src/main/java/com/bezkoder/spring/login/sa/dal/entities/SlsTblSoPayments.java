package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name="sls_tbl_so_payments")
@NamedQuery(name="SlsTblSoPayments.findAll", query="SELECT s FROM SlsTblSoPayments s")
public class SlsTblSoPayments implements Serializable {
	private static final long serialVersionUID = 1L;

/*	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_so_detail_id")
	private Integer serSoDetailId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slsTblSoPayments_ser_so_payment_id_seq")
	@SequenceGenerator(name = "slsTblSoPayments_ser_so_payment_id_seq", sequenceName = "slsTblSoPayments_ser_so_payment_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_so_payment_id")
	private Integer serSoPaymentId;
	
	@Column(name="txt_slip_no")
	private String txtSlipNo;
	
	@Column(name="txt_payment_method")
	private String txtPaymentMethod;

	@Column(name="num_payment_received")
	private BigDecimal numPaymentReceived;

	@Column(name="num_balance")
	private BigDecimal numBalance;
	

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;
	
	@Column(name="num_opening_balance")
	private BigDecimal numOpeningBalance;
	

	

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bl_is_dealer_adjustment")
	private Boolean blIsDealerAdjustment;
	
	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_remarks")
	private String txtRemarks;
	
	@Column(name="txt_cheque_no")
	private String txtChequeNo;

	@Column(name="txt_status")
	private String txtStatus;
	
	@Column(name="txt_saleOrder_no")
	private String txtSaleOrderNo;
	
	@Column(name="ser_group_id")
	private Integer serGroupId;
	
	
	@Column(name="txt_payment_term")
	private String txtPaymentTerm;
	
	@Column(name="txt_withheld")
	private String txtWithheld;
	
	@Column(name="txt_stamp_duty")
	private String txtStampDuty;
	
	@Column(name="txt_late_delivery")
	private String txtLateDelivery;
	
 
	@Column(name="num_net_payment")
	private BigDecimal numNetPayment;
	
	@Column(name="num_st")
	private BigDecimal numST;
	
	@Column(name="txt_xml_sent")
	private String txtXMSent;
	
	@Column(name="txt_return_msg")
	private String txtReturnMsg;
	
	@Column(name="txt_xml_receive")
	private String txtXMReceive;
	
	@Column(name="bl_is_advance")
	private Boolean blIsAdvance;



	//bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name="ser_sale_order_id")
	private SlsTblSaleOrder slsTblSaleOrder;
	
	

	//bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name="ser_bank_id")
	private CfgTblBank cfgTblBank;
	
	
	//bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name="ser_deal_id")
	private SlsTblDeal slsTblDeal;
	
	/*
	 * @JsonIgnoreProperties(value={"slsTblSoPayments"})
	 * 
	 * @OneToMany(fetch=FetchType.LAZY,mappedBy="slsTblSoPayments") private
	 * List<SOPaymentDocument> paymentDocuments=new ArrayList<SOPaymentDocument>();
	 */
	
	public SlsTblSoPayments() {
	}

	public Integer getSerSoPaymentId() {
		return serSoPaymentId;
	}

	public void setSerSoPaymentId(Integer serSoPaymentId) {
		this.serSoPaymentId = serSoPaymentId;
	}

	public String getTxtSlipNo() {
		return txtSlipNo;
	}

	public void setTxtSlipNo(String txtSlipNo) {
		this.txtSlipNo = txtSlipNo;
	}

	public BigDecimal getNumPaymentReceived() {
		return numPaymentReceived;
	}

	public void setNumPaymentReceived(BigDecimal numPaymentReceived) {
		this.numPaymentReceived = numPaymentReceived;
	}

	public BigDecimal getNumBalance() {
		return numBalance;
	}

	public void setNumBalance(BigDecimal numBalance) {
		this.numBalance = numBalance;
	}

	public Date getDteDate() {
		return dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public BigDecimal getNumOpeningBalance() {
		return numOpeningBalance;
	}

	public void setNumOpeningBalance(BigDecimal numOpeningBalance) {
		this.numOpeningBalance = numOpeningBalance;
	}

	public String getTxtPaymentMethod() {
		return txtPaymentMethod;
	}

	public void setTxtPaymentMethod(String txtPaymentMethod) {
		this.txtPaymentMethod = txtPaymentMethod;
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

	public SlsTblSaleOrder getSlsTblSaleOrder() {
		return slsTblSaleOrder;
	}

	public void setSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		this.slsTblSaleOrder = slsTblSaleOrder;
	}

	public String getTxtSaleOrderNo() {
		return txtSaleOrderNo;
	}

	public void setTxtSaleOrderNo(String txtSaleOrderNo) {
		this.txtSaleOrderNo = txtSaleOrderNo;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	/*
	 * public List<SOPaymentDocument> getPaymentDocuments() { return
	 * paymentDocuments; }
	 * 
	 * public void setPaymentDocuments(List<SOPaymentDocument> paymentDocuments) {
	 * this.paymentDocuments = paymentDocuments; }
	 */

	public Boolean getBlIsDealerAdjustment() {
		return blIsDealerAdjustment;
	}

	public void setBlIsDealerAdjustment(Boolean blIsDealerAdjustment) {
		this.blIsDealerAdjustment = blIsDealerAdjustment;
	}

	public String getTxtPaymentTerm() {
		return txtPaymentTerm;
	}

	public void setTxtPaymentTerm(String txtPaymentTerm) {
		this.txtPaymentTerm = txtPaymentTerm;
	}

	public String getTxtWithheld() {
		return txtWithheld;
	}

	public void setTxtWithheld(String txtWithheld) {
		this.txtWithheld = txtWithheld;
	}

	public String getTxtStampDuty() {
		return txtStampDuty;
	}

	public void setTxtStampDuty(String txtStampDuty) {
		this.txtStampDuty = txtStampDuty;
	}

	public String getTxtLateDelivery() {
		return txtLateDelivery;
	}

	public void setTxtLateDelivery(String txtLateDelivery) {
		this.txtLateDelivery = txtLateDelivery;
	}

	public SlsTblDeal getSlsTblDeal() {
		return slsTblDeal;
	}

	public void setSlsTblDeal(SlsTblDeal slsTblDeal) {
		this.slsTblDeal = slsTblDeal;
	}

	public BigDecimal getNumNetPayment() {
		return numNetPayment;
	}

	public void setNumNetPayment(BigDecimal numNetPayment) {
		this.numNetPayment = numNetPayment;
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

	public Boolean getBlIsAdvance() {
		return blIsAdvance;
	}

	public void setBlIsAdvance(Boolean blIsAdvance) {
		this.blIsAdvance = blIsAdvance;
	}

	public CfgTblBank getCfgTblBank() {
		return cfgTblBank;
	}

	public void setCfgTblBank(CfgTblBank cfgTblBank) {
		this.cfgTblBank = cfgTblBank;
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

	public String getTxtChequeNo() {
		return txtChequeNo;
	}

	public void setTxtChequeNo(String txtChequeNo) {
		this.txtChequeNo = txtChequeNo;
	}

	public BigDecimal getNumST() {
		return numST;
	}

	public void setNumST(BigDecimal numST) {
		this.numST = numST;
	}
	
	@Column(name="num_level")
	private Integer numLevel;
	
	@Column(name="ser_approvedby_id1")
	private Integer serApprovedbyId1;
	
	@Column(name="ser_approvedby_id2")
	private Integer serApprovedbyId2;
	
	@Column(name="txt_status1")
	private String txtStatus1;
	
	@Column(name="txt_status2")
	private String txtStatus2;
	

	@Column(name="dte_approveddate1")
	private Timestamp dteApproveddate1;
	
	@Column(name="dte_approveddate2")
	private Timestamp dteApproveddate2;

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
	
	@Column(name="txt_Level")
	private String txtLevel;

	@Column(name="txt_order_approval_date")
	private String txtOrderapprovalDate;

	public String getTxtOrderapprovalDate() {
		return txtOrderapprovalDate;
	}

	public void setTxtOrderapprovalDate(String txtOrderapprovalDate) {
		this.txtOrderapprovalDate = txtOrderapprovalDate;
	}

	public String getTxtLevel() {
		return txtLevel;
	}

	public void setTxtLevel(String txtLevel) {
		this.txtLevel = txtLevel;
	}
	
	
}