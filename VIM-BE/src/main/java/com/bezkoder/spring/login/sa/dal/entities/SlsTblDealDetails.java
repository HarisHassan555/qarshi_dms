package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name="sls_tbl_deal_details")
@NamedQuery(name="SlsTblDealDetails.findAll", query="SELECT s FROM SlsTblDealDetails s")
public class SlsTblDealDetails implements Serializable {
	private static final long serialVersionUID = 1L;

/*	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_so_detail_id")
	private Integer serSoDetailId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_deal_details_ser_deal_details_id_seq")
	@SequenceGenerator(name = "sls_tbl_deal_details_ser_deal_details_id_seq", sequenceName = "sls_tbl_deal_details_ser_deal_details_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_deal_detail_id")
	private Integer serDealDetailId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_approved_quantity")
	private BigDecimal numApprovedQuantity;

	@Column(name="num_balance")
	private BigDecimal numBalance;

	@Column(name="num_bonus")
	private BigDecimal numBonus;
	
	public BigDecimal getNumIssueQty() {
		return numIssueQty;
	}

	public void setNumIssueQty(BigDecimal numIssueQty) {
		this.numIssueQty = numIssueQty;
	}

	@Column(name="num_issue_qty")
	private BigDecimal numIssueQty;

	@Column(name="num_item_price")
	private BigDecimal numItemPrice;

	@Column(name="num_quantity")
	private BigDecimal numQuantity;
	
	@Column(name="num_excise_duty")
	private BigDecimal numExciseDuty;
	
	@Column(name="num_net_amount")
	private BigDecimal numNetAmount;

	@Column(name="num_sales_tax")
	private BigDecimal numSalesTax;

	@Column(name="num_stock_availabe")
	private BigDecimal numStockAvailabe;

	@Column(name="num_total_wt")
	private BigDecimal numTotalWt;

	@Column(name="num_unit_wt")
	private BigDecimal numUnitWt;
	
	@Column(name="num_amount_after_discount")
	private BigDecimal numAmountAfterDiscount;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_item_approve_status")
	private String txtItemApproveStatus;

	@Column(name="txt_item_issue_status")
	private String txtItemIssueStatus;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_remarks")
	private String txtRemarks;

	@Column(name="txt_status")
	private String txtStatus;

	
	@Column(name="num_total_price")
	private BigDecimal numTotalPrice;

	@Column(name="num_seq")
	private BigDecimal numSeq;
	
	//bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name="ser_product_id")
	private CfgTblProduct cfgTblProduct;

	//bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ser_deal_id")
	//@JsonBackReference("deals_details")
	@JsonIgnoreProperties("slsTblDealDetails")
	private SlsTblDeal slsTblDeal;


	@Column(name="sap_line_item_no")
	private String sapLineItem;


	@Column(name="sap_line_item_description")
	private String sapLineItemDescription;

	public String getSapLineItemDescription() {
		return sapLineItemDescription;
	}

	public void setSapLineItemDescription(String sapLineItemDescription) {
		this.sapLineItemDescription = sapLineItemDescription;
	}



	public String getSapLineItem() {
		return sapLineItem;
	}

	public void setSapLineItem(String sapLineItem) {
		this.sapLineItem = sapLineItem;
	}


	
	public BigDecimal getNumAmountAfterDiscount() {
		return numAmountAfterDiscount;
	}

	public void setNumAmountAfterDiscount(BigDecimal numAmountAfterDiscount) {
		this.numAmountAfterDiscount = numAmountAfterDiscount;
	}

	

	
	
	public CfgTblProductQuality getCfgTblProductQuality() {
		return cfgTblProductQuality;
	}

	public void setCfgTblProductQuality(CfgTblProductQuality cfgTblProductQuality) {
		this.cfgTblProductQuality = cfgTblProductQuality;
	}

	public CfgTblProductDesign getCfgTblProductDesign() {
		return cfgTblProductDesign;
	}

	public void setCfgTblProductDesign(CfgTblProductDesign cfgTblProductDesign) {
		this.cfgTblProductDesign = cfgTblProductDesign;
	}

		//bi-directional many-to-one association to CfgTblProduct
		@ManyToOne
		@JoinColumn(name="ser_product_quality_id")
		private CfgTblProductQuality cfgTblProductQuality;
		
		@ManyToOne
		@JoinColumn(name="ser_product_design_id")
		private CfgTblProductDesign cfgTblProductDesign;

	public SlsTblDealDetails() {
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

	public BigDecimal getNumApprovedQuantity() {
		return this.numApprovedQuantity;
	}

	public void setNumApprovedQuantity(BigDecimal numApprovedQuantity) {
		this.numApprovedQuantity = numApprovedQuantity;
	}

	public BigDecimal getNumBalance() {
		return this.numBalance;
	}

	public void setNumBalance(BigDecimal numBalance) {
		this.numBalance = numBalance;
	}

	public BigDecimal getNumBonus() {
		return this.numBonus;
	}

	public void setNumBonus(BigDecimal numBonus) {
		this.numBonus = numBonus;
	}

	public BigDecimal getNumItemPrice() {
		return this.numItemPrice;
	}

	public void setNumItemPrice(BigDecimal numItemPrice) {
		this.numItemPrice = numItemPrice;
	}

	public BigDecimal getNumQuantity() {
		return this.numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	public BigDecimal getNumStockAvailabe() {
		return this.numStockAvailabe;
	}

	public void setNumStockAvailabe(BigDecimal numStockAvailabe) {
		this.numStockAvailabe = numStockAvailabe;
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

	public String getTxtItemApproveStatus() {
		return this.txtItemApproveStatus;
	}

	public void setTxtItemApproveStatus(String txtItemApproveStatus) {
		this.txtItemApproveStatus = txtItemApproveStatus;
	}

	public String getTxtItemIssueStatus() {
		return this.txtItemIssueStatus;
	}

	public void setTxtItemIssueStatus(String txtItemIssueStatus) {
		this.txtItemIssueStatus = txtItemIssueStatus;
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

	public String getTxtStatus() {
		return this.txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	

	public CfgTblProduct getCfgTblProduct() {
		return this.cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	
	
	
	public SlsTblDeal getSlsTblDeal() {
		return slsTblDeal;
	}

	public void setSlsTblDeal(SlsTblDeal slsTblDeal) {
		this.slsTblDeal = slsTblDeal;
	}

	@Temporal(TemporalType.DATE)
	@Column(name="dte_schedule_date")
	private Date dteScheduleDate;

	public Date getDteScheduleDate() {
		return dteScheduleDate;
	}

	public void setDteScheduleDate(Date dteScheduleDate) {
		this.dteScheduleDate = dteScheduleDate;
	}

//	slsTblSaleItemSchedule
	
	//bi-directional many-to-one association to SlsTblSoDetail
	@OneToMany(mappedBy="slsTblSoDetail",fetch = FetchType.EAGER)
	@JsonManagedReference("dealDetails")
	private List<SlsTblSaleItemSchedule> slsTblSaleItemSchedule;

	public List<SlsTblSaleItemSchedule> getSlsTblSaleItemSchedule() {
		return slsTblSaleItemSchedule;
	}

	public void setSlsTblSaleItemSchedule(List<SlsTblSaleItemSchedule> slsTblSaleItemSchedule) {
		this.slsTblSaleItemSchedule = slsTblSaleItemSchedule;
	}

	public Integer getSerDealDetailId() {
		return serDealDetailId;
	}

	public void setSerDealDetailId(Integer serDealDetailId) {
		this.serDealDetailId = serDealDetailId;
	}

	public BigDecimal getNumExciseDuty() {
		return numExciseDuty;
	}

	public void setNumExciseDuty(BigDecimal numExciseDuty) {
		this.numExciseDuty = numExciseDuty;
	}

	public BigDecimal getNumNetAmount() {
		return numNetAmount;
	}

	public void setNumNetAmount(BigDecimal numNetAmount) {
		this.numNetAmount = numNetAmount;
	}

	public BigDecimal getNumSalesTax() {
		return numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public BigDecimal getNumTotalPrice() {
		return numTotalPrice;
	}

	public void setNumTotalPrice(BigDecimal numTotalPrice) {
		this.numTotalPrice = numTotalPrice;
	}

	public BigDecimal getNumSeq() {
		return numSeq;
	}

	public void setNumSeq(BigDecimal numSeq) {
		this.numSeq = numSeq;
	}
	
	
	
//	//bi-directional many-to-one association to SlsTblSoDetail
//	@OneToMany(mappedBy="slsTblSaleOrder")
//	private List<SlsTblSoDetail> slsTblSoDetails;
}