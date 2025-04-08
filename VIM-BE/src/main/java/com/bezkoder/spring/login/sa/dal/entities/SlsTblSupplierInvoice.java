package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the sls_tbl_supplier_invoice database table.
 * 
 */
@Entity
@Table(name="sls_tbl_supplier_invoice")
@NamedQuery(name="SlsTblSupplierInvoice.findAll", query="SELECT s FROM SlsTblSupplierInvoice s")
public class SlsTblSupplierInvoice implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_supplier_invoice_id")
	private Integer serSupplierInvoiceId;*/
	

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_supplier_invoice_ser_supplier_invoice_id_seq")
	@SequenceGenerator(name = "sls_tbl_supplier_invoice_ser_supplier_invoice_id_seq", sequenceName = "sls_tbl_supplier_invoice_ser_supplier_invoice_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_supplier_invoice_id")
	private Integer serSupplierInvoiceId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;
	
	public BigDecimal getNumInvoiceAmount() {
		return numInvoiceAmount;
	}

	public void setNumInvoiceAmount(BigDecimal numInvoiceAmount) {
		this.numInvoiceAmount = numInvoiceAmount;
	}

	public BigDecimal getNumInvoiceAmountAftFreight() {
		return numInvoiceAmountAftFreight;
	}

	public void setNumInvoiceAmountAftFreight(BigDecimal numInvoiceAmountAftFreight) {
		this.numInvoiceAmountAftFreight = numInvoiceAmountAftFreight;
	}

	@Column(name="num_invoice_amount")
	private BigDecimal numInvoiceAmount;
	
	@Column(name="num_invoice_amount_aft_freight")
	private BigDecimal numInvoiceAmountAftFreight;

	@Column(name="num_advance_deducted")
	private BigDecimal numAdvanceDeducted;

	@Column(name="num_advance_tax_perc")
	private BigDecimal numAdvanceTaxPerc;
	
	@Column(name="num_total_advance_tax_amount_perc")
	private BigDecimal numTotalAdvanceTaxAmount;

	@Column(name="num_customs_duty")
	private BigDecimal numCustomsDuty;
	
	public BigDecimal getNumTotalQty() {
		return numTotalQty;
	}

	public void setNumTotalQty(BigDecimal numTotalQty) {
		this.numTotalQty = numTotalQty;
	}

	@Column(name="num_total_qty")
	private BigDecimal numTotalQty;

	@Column(name="num_discount")
	private BigDecimal numDiscount;
	
	@Column(name="num_discount_amount")
	private BigDecimal numDiscountAmount;

	@Column(name="num_freight_amount")
	private BigDecimal numFreightAmount;

	@Column(name="num_gst_withhold_amount")
	private BigDecimal numGstWithholdAmount;

	@Column(name="num_income_tax")
	private BigDecimal numIncomeTax;

	@Column(name="num_old_sales_tax_amount")
	private BigDecimal numOldSalesTaxAmount;

	@Column(name="num_paid_amount")
	private BigDecimal numPaidAmount;

	@Column(name="num_sales_tax")
	private BigDecimal numSalesTax;
	
	@Column(name="num_sales_tax_perc")
	private BigDecimal numSalesTaxPerc;
	
	@Column(name="num_amount_after_discount")
	private BigDecimal numAmountAfterDiscount;
	
	public BigDecimal getNumAmountAfterDiscount() {
		return numAmountAfterDiscount;
	}

	public void setNumAmountAfterDiscount(BigDecimal numAmountAfterDiscount) {
		this.numAmountAfterDiscount = numAmountAfterDiscount;
	}

	public BigDecimal getNumSalesTaxPerc() {
		return numSalesTaxPerc;
	}

	public void setNumSalesTaxPerc(BigDecimal numSalesTaxPerc) {
		this.numSalesTaxPerc = numSalesTaxPerc;
	}

	@Column(name="num_shipment_price")
	private BigDecimal numShipmentPrice;

	@Column(name="num_special_salestax")
	private BigDecimal numSpecialSalestax;

	@Column(name="num_stax_tax_perc")
	private BigDecimal numStaxTaxPerc;

	@Column(name="num_tax_amount")
	private BigDecimal numTaxAmount;


	


	@Column(name="num_total_amount")
	private BigDecimal numTotalAmount;

	@Column(name="num_total_amount_after_stax")
	private BigDecimal numTotalAmountAfterStax;

	@Column(name="num_total_without_gstwh_amount")
	private BigDecimal numTotalWithoutGstwhAmount;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_bill_no")
	private String txtBillNo;

	@Column(name="txt_invoice_code")
	private String txtInvoiceCode;

	@Column(name="txt_invoice_status")
	private String txtInvoiceStatus;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_remarks")
	private String txtRemarks;

	@Column(name="txt_salestax_invoice_no")
	private String txtSalestaxInvoiceNo;

	//bi-directional many-to-one association to SlsTblInvoiceDetail
	@OneToMany(mappedBy="slsTblSupplierInvoice")
	private List<SlsTblInvoiceDetail> slsTblInvoiceDetails;

	//bi-directional many-to-one association to CfgTblCustomer
	@ManyToOne
	@JoinColumn(name="ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;

	//bi-directional many-to-one association to CfgTblSupplier
	@ManyToOne
	@JoinColumn(name="ser_supplier_id")
	private CfgTblSupplier cfgTblSupplier;
	

	@ManyToOne
	@JoinColumn(name="ser_issue_id")
	private InvTblIssue invTblIssue;
	
	public InvTblIssue getInvTblIssue() {
		return invTblIssue;
	}

	public void setInvTblIssue(InvTblIssue invTblIssue) {
		this.invTblIssue = invTblIssue;
	}

	

	public BigDecimal getNumDiscountAmount() {
		return numDiscountAmount;
	}

	public void setNumDiscountAmount(BigDecimal numDiscountAmount) {
		this.numDiscountAmount = numDiscountAmount;
	}

	public SlsTblSupplierInvoice() {
	}

	public Integer getSerSupplierInvoiceId() {
		return this.serSupplierInvoiceId;
	}

	public void setSerSupplierInvoiceId(Integer serSupplierInvoiceId) {
		this.serSupplierInvoiceId = serSupplierInvoiceId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
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

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public BigDecimal getNumAdvanceDeducted() {
		return this.numAdvanceDeducted;
	}

	public void setNumAdvanceDeducted(BigDecimal numAdvanceDeducted) {
		this.numAdvanceDeducted = numAdvanceDeducted;
	}

	public BigDecimal getNumAdvanceTaxPerc() {
		return this.numAdvanceTaxPerc;
	}

	public void setNumAdvanceTaxPerc(BigDecimal numAdvanceTaxPerc) {
		this.numAdvanceTaxPerc = numAdvanceTaxPerc;
	}

	public BigDecimal getNumCustomsDuty() {
		return this.numCustomsDuty;
	}

	public void setNumCustomsDuty(BigDecimal numCustomsDuty) {
		this.numCustomsDuty = numCustomsDuty;
	}

	public BigDecimal getNumDiscount() {
		return this.numDiscount;
	}

	public void setNumDiscount(BigDecimal numDiscount) {
		this.numDiscount = numDiscount;
	}

	public BigDecimal getNumFreightAmount() {
		return this.numFreightAmount;
	}

	public void setNumFreightAmount(BigDecimal numFreightAmount) {
		this.numFreightAmount = numFreightAmount;
	}

	public BigDecimal getNumGstWithholdAmount() {
		return this.numGstWithholdAmount;
	}

	public void setNumGstWithholdAmount(BigDecimal numGstWithholdAmount) {
		this.numGstWithholdAmount = numGstWithholdAmount;
	}

	public BigDecimal getNumIncomeTax() {
		return this.numIncomeTax;
	}

	public void setNumIncomeTax(BigDecimal numIncomeTax) {
		this.numIncomeTax = numIncomeTax;
	}

	public BigDecimal getNumOldSalesTaxAmount() {
		return this.numOldSalesTaxAmount;
	}

	public void setNumOldSalesTaxAmount(BigDecimal numOldSalesTaxAmount) {
		this.numOldSalesTaxAmount = numOldSalesTaxAmount;
	}

	public BigDecimal getNumPaidAmount() {
		return this.numPaidAmount;
	}

	public void setNumPaidAmount(BigDecimal numPaidAmount) {
		this.numPaidAmount = numPaidAmount;
	}

	public BigDecimal getNumSalesTax() {
		return this.numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public BigDecimal getNumShipmentPrice() {
		return this.numShipmentPrice;
	}

	public void setNumShipmentPrice(BigDecimal numShipmentPrice) {
		this.numShipmentPrice = numShipmentPrice;
	}

	public BigDecimal getNumSpecialSalestax() {
		return this.numSpecialSalestax;
	}

	public void setNumSpecialSalestax(BigDecimal numSpecialSalestax) {
		this.numSpecialSalestax = numSpecialSalestax;
	}

	public BigDecimal getNumStaxTaxPerc() {
		return this.numStaxTaxPerc;
	}

	public void setNumStaxTaxPerc(BigDecimal numStaxTaxPerc) {
		this.numStaxTaxPerc = numStaxTaxPerc;
	}

	public BigDecimal getNumTaxAmount() {
		return this.numTaxAmount;
	}

	public void setNumTaxAmount(BigDecimal numTaxAmount) {
		this.numTaxAmount = numTaxAmount;
	}

	public BigDecimal getNumTotalAdvanceTaxAmount() {
		return this.numTotalAdvanceTaxAmount;
	}

	public void setNumTotalAdvanceTaxAmount(BigDecimal numTotalAdvanceTaxAmount) {
		this.numTotalAdvanceTaxAmount = numTotalAdvanceTaxAmount;
	}

	public BigDecimal getNumTotalAmount() {
		return this.numTotalAmount;
	}

	public void setNumTotalAmount(BigDecimal numTotalAmount) {
		this.numTotalAmount = numTotalAmount;
	}

	public BigDecimal getNumTotalAmountAfterStax() {
		return this.numTotalAmountAfterStax;
	}

	public void setNumTotalAmountAfterStax(BigDecimal numTotalAmountAfterStax) {
		this.numTotalAmountAfterStax = numTotalAmountAfterStax;
	}

	public BigDecimal getNumTotalWithoutGstwhAmount() {
		return this.numTotalWithoutGstwhAmount;
	}

	public void setNumTotalWithoutGstwhAmount(BigDecimal numTotalWithoutGstwhAmount) {
		this.numTotalWithoutGstwhAmount = numTotalWithoutGstwhAmount;
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

	public String getTxtBillNo() {
		return this.txtBillNo;
	}

	public void setTxtBillNo(String txtBillNo) {
		this.txtBillNo = txtBillNo;
	}

	public String getTxtInvoiceCode() {
		return this.txtInvoiceCode;
	}

	public void setTxtInvoiceCode(String txtInvoiceCode) {
		this.txtInvoiceCode = txtInvoiceCode;
	}

	public String getTxtInvoiceStatus() {
		return this.txtInvoiceStatus;
	}

	public void setTxtInvoiceStatus(String txtInvoiceStatus) {
		this.txtInvoiceStatus = txtInvoiceStatus;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtRemarks() {
		return this.txtRemarks;
	}

	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}

	public String getTxtSalestaxInvoiceNo() {
		return this.txtSalestaxInvoiceNo;
	}

	public void setTxtSalestaxInvoiceNo(String txtSalestaxInvoiceNo) {
		this.txtSalestaxInvoiceNo = txtSalestaxInvoiceNo;
	}

	public List<SlsTblInvoiceDetail> getSlsTblInvoiceDetails() {
		return this.slsTblInvoiceDetails;
	}

	public void setSlsTblInvoiceDetails(List<SlsTblInvoiceDetail> slsTblInvoiceDetails) {
		this.slsTblInvoiceDetails = slsTblInvoiceDetails;
	}

	public SlsTblInvoiceDetail addSlsTblInvoiceDetail(SlsTblInvoiceDetail slsTblInvoiceDetail) {
		getSlsTblInvoiceDetails().add(slsTblInvoiceDetail);
		slsTblInvoiceDetail.setSlsTblSupplierInvoice(this);

		return slsTblInvoiceDetail;
	}

	public SlsTblInvoiceDetail removeSlsTblInvoiceDetail(SlsTblInvoiceDetail slsTblInvoiceDetail) {
		getSlsTblInvoiceDetails().remove(slsTblInvoiceDetail);
		slsTblInvoiceDetail.setSlsTblSupplierInvoice(null);

		return slsTblInvoiceDetail;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return this.cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public CfgTblSupplier getCfgTblSupplier() {
		return this.cfgTblSupplier;
	}

	public void setCfgTblSupplier(CfgTblSupplier cfgTblSupplier) {
		this.cfgTblSupplier = cfgTblSupplier;
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

}