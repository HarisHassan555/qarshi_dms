package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;


/**
 * The persistent class for the sls_tbl_invoice_detail database table.
 * 
 */
@Entity
@Table(name="sls_tbl_invoice_detail")
@NamedQuery(name="SlsTblInvoiceDetail.findAll", query="SELECT s FROM SlsTblInvoiceDetail s")
public class SlsTblInvoiceDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_invoice_detail_id")
	private Integer serInvoiceDetailId;
*/

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_invoice_detail_ser_invoice_detail_id_seq")
	@SequenceGenerator(name = "sls_tbl_invoice_detail_ser_invoice_detail_id_seq", sequenceName = "sls_tbl_invoice_detail_ser_invoice_detail_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_invoice_detail_id")
	private Integer serInvoiceDetailId;
	
	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_advance_tax_amount")
	private BigDecimal numAdvanceTaxAmount;

	@Column(name="num_market_retail_price")
	private BigDecimal numMarketRetailPrice;

	@Column(name="num_old_qty")
	private BigDecimal numOldQty;

	@Column(name="num_per_piece_price")
	private BigDecimal numPerPiecePrice;

	@Column(name="num_quantity")
	private Integer numQuantity;

	@Column(name="num_tax_amount")
	private BigDecimal numTaxAmount;

	@Column(name="num_total_price")
	private BigDecimal numTotalPrice;

	@Column(name="num_total_price_after_tax")
	private BigDecimal numTotalPriceAfterTax;

	@Column(name="num_total_wt")
	private BigDecimal numTotalWt;

	@Column(name="num_unit_wt")
	private BigDecimal numUnitWt;

	@Column(name="num_weight")
	private BigDecimal numWeight;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	public InvTblIssueDetail getInvTblIssueDetail() {
		return invTblIssueDetail;
	}

	public void setInvTblIssueDetail(InvTblIssueDetail invTblIssueDetail) {
		this.invTblIssueDetail = invTblIssueDetail;
	}
	
	@ManyToOne
	@JoinColumn(name="ser_issue_detail_id")
	private InvTblIssueDetail invTblIssueDetail;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_remarks")
	private String txtRemarks;

	//bi-directional many-to-one association to CfgTblProcess
	@ManyToOne
	@JoinColumn(name="ser_process_id")
	private CfgTblProcess cfgTblProcess;

	//bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name="ser_product_id")
	private CfgTblProduct cfgTblProduct;

	//bi-directional many-to-one association to CfgTblProductDesign
	@ManyToOne
	@JoinColumn(name="ser_product_design_id")
	private CfgTblProductDesign cfgTblProductDesign;

	//bi-directional many-to-one association to CfgTblProductQuality
	@ManyToOne
	@JoinColumn(name="ser_product_quality_id")
	private CfgTblProductQuality cfgTblProductQuality;

	//bi-directional many-to-one association to SlsTblSupplierInvoice
	@ManyToOne
	@JoinColumn(name="ser_supplier_invoice_id")
	private SlsTblSupplierInvoice slsTblSupplierInvoice;

	public SlsTblInvoiceDetail() {
	}

	public Integer getSerInvoiceDetailId() {
		return this.serInvoiceDetailId;
	}

	public void setSerInvoiceDetailId(Integer serInvoiceDetailId) {
		this.serInvoiceDetailId = serInvoiceDetailId;
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

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public BigDecimal getNumAdvanceTaxAmount() {
		return this.numAdvanceTaxAmount;
	}

	public void setNumAdvanceTaxAmount(BigDecimal numAdvanceTaxAmount) {
		this.numAdvanceTaxAmount = numAdvanceTaxAmount;
	}

	public BigDecimal getNumMarketRetailPrice() {
		return this.numMarketRetailPrice;
	}

	public void setNumMarketRetailPrice(BigDecimal numMarketRetailPrice) {
		this.numMarketRetailPrice = numMarketRetailPrice;
	}

	public BigDecimal getNumOldQty() {
		return this.numOldQty;
	}

	public void setNumOldQty(BigDecimal numOldQty) {
		this.numOldQty = numOldQty;
	}

	public BigDecimal getNumPerPiecePrice() {
		return this.numPerPiecePrice;
	}

	public void setNumPerPiecePrice(BigDecimal numPerPiecePrice) {
		this.numPerPiecePrice = numPerPiecePrice;
	}

	public Integer getNumQuantity() {
		return this.numQuantity;
	}

	public void setNumQuantity(Integer numQuantity) {
		this.numQuantity = numQuantity;
	}

	public BigDecimal getNumTaxAmount() {
		return this.numTaxAmount;
	}

	public void setNumTaxAmount(BigDecimal numTaxAmount) {
		this.numTaxAmount = numTaxAmount;
	}

	public BigDecimal getNumTotalPrice() {
		return this.numTotalPrice;
	}

	public void setNumTotalPrice(BigDecimal numTotalPrice) {
		this.numTotalPrice = numTotalPrice;
	}

	public BigDecimal getNumTotalPriceAfterTax() {
		return this.numTotalPriceAfterTax;
	}

	public void setNumTotalPriceAfterTax(BigDecimal numTotalPriceAfterTax) {
		this.numTotalPriceAfterTax = numTotalPriceAfterTax;
	}

	public BigDecimal getNumTotalWt() {
		return this.numTotalWt;
	}

	public void setNumTotalWt(BigDecimal numTotalWt) {
		this.numTotalWt = numTotalWt;
	}

	public BigDecimal getNumUnitWt() {
		return this.numUnitWt;
	}

	public void setNumUnitWt(BigDecimal numUnitWt) {
		this.numUnitWt = numUnitWt;
	}

	public BigDecimal getNumWeight() {
		return this.numWeight;
	}

	public void setNumWeight(BigDecimal numWeight) {
		this.numWeight = numWeight;
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

	public CfgTblProcess getCfgTblProcess() {
		return this.cfgTblProcess;
	}

	public void setCfgTblProcess(CfgTblProcess cfgTblProcess) {
		this.cfgTblProcess = cfgTblProcess;
	}

	public CfgTblProduct getCfgTblProduct() {
		return this.cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public CfgTblProductDesign getCfgTblProductDesign() {
		return this.cfgTblProductDesign;
	}

	public void setCfgTblProductDesign(CfgTblProductDesign cfgTblProductDesign) {
		this.cfgTblProductDesign = cfgTblProductDesign;
	}

	public CfgTblProductQuality getCfgTblProductQuality() {
		return this.cfgTblProductQuality;
	}

	public void setCfgTblProductQuality(CfgTblProductQuality cfgTblProductQuality) {
		this.cfgTblProductQuality = cfgTblProductQuality;
	}

	public SlsTblSupplierInvoice getSlsTblSupplierInvoice() {
		return this.slsTblSupplierInvoice;
	}

	public void setSlsTblSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice) {
		this.slsTblSupplierInvoice = slsTblSupplierInvoice;
	}

}