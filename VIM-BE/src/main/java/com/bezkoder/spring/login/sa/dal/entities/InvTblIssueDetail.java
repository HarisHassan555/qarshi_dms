package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the inv_tbl_issue_detail database table.
 * 
 */
@Entity
@Table(name="inv_tbl_issue_detail")
@NamedQuery(name="InvTblIssueDetail.findAll", query="SELECT i FROM InvTblIssueDetail i")
public class InvTblIssueDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_issue_detail_id")
	private Integer serIssueDetailId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_tbl_issue_detail_ser_issue_detail_id_seq")
	@SequenceGenerator(name = "inv_tbl_issue_detail_ser_issue_detail_id_seq", sequenceName = "inv_tbl_issue_detail_ser_issue_detail_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_issue_detail_id")
	private Integer serIssueDetailId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_no_of_bags")
	private BigDecimal numNoOfBags;

	@Column(name="num_price_per_piece")
	private BigDecimal numPricePerPiece;

	@Column(name="num_qty_in_bags")
	private BigDecimal numQtyInBags;

	@Column(name="num_quantity")
	private BigDecimal numQuantity;

	@Column(name="num_total_wt")
	private BigDecimal numTotalWt;

	@Column(name="num_unit_wt")
	private BigDecimal numUnitWt;
	


	public BigDecimal getNumStockAvailabe() {
		return numStockAvailabe;
	}

	public void setNumStockAvailabe(BigDecimal numStockAvailabe) {
		this.numStockAvailabe = numStockAvailabe;
	}

	@Column(name="num_stock_availabe")
	private BigDecimal numStockAvailabe;
	

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_reason")
	private String txtReason;

	@Column(name="txt_type")
	private String txtType;

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

	//bi-directional many-to-one association to InvTblIssue
	@ManyToOne
	@JoinColumn(name="ser_issue_id")
	private InvTblIssue invTblIssue;
	
	

	//bi-directional many-to-one association to SlsTblSoDetail
	@ManyToOne
	@JoinColumn(name="ser_so_detail_id")
	private SlsTblSoDetail slsTblSoDetail;



	public InvTblIssueDetail() {
	}

	public Integer getSerIssueDetailId() {
		return this.serIssueDetailId;
	}

	public void setSerIssueDetailId(Integer serIssueDetailId) {
		this.serIssueDetailId = serIssueDetailId;
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

	public BigDecimal getNumNoOfBags() {
		return this.numNoOfBags;
	}

	public void setNumNoOfBags(BigDecimal numNoOfBags) {
		this.numNoOfBags = numNoOfBags;
	}

	public BigDecimal getNumPricePerPiece() {
		return this.numPricePerPiece;
	}

	public void setNumPricePerPiece(BigDecimal numPricePerPiece) {
		this.numPricePerPiece = numPricePerPiece;
	}

	public BigDecimal getNumQtyInBags() {
		return this.numQtyInBags;
	}

	public void setNumQtyInBags(BigDecimal numQtyInBags) {
		this.numQtyInBags = numQtyInBags;
	}

	public BigDecimal getNumQuantity() {
		return this.numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
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

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtReason() {
		return this.txtReason;
	}

	public void setTxtReason(String txtReason) {
		this.txtReason = txtReason;
	}

	public String getTxtType() {
		return this.txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
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

	public InvTblIssue getInvTblIssue() {
		return this.invTblIssue;
	}

	public void setInvTblIssue(InvTblIssue invTblIssue) {
		this.invTblIssue = invTblIssue;
	}

	public SlsTblSoDetail getSlsTblSoDetail() {
		return this.slsTblSoDetail;
	}

	public void setSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		this.slsTblSoDetail = slsTblSoDetail;
	}



}