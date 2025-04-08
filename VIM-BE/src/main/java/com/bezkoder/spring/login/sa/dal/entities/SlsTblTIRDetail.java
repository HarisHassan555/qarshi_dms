package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the sls_tbl_work_order_Detail database table.
 * 
 */
@Entity
@Table(name="sls_tbl_tir_Detail")
@NamedQuery(name="SlsTblTIRDetail.findAll", query="SELECT i FROM SlsTblTIRDetail i")
public class SlsTblTIRDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_work_order_detail_id")
	private Integer serTIRDetailId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_work_order_Detail_ser_work_order_detail_id_seq")
	@SequenceGenerator(name = "sls_tbl_work_order_Detail_ser_work_order_detail_id_seq", sequenceName = "sls_tbl_work_order_Detail_ser_work_order_detail_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_tir_detail_id")
	private Integer serTIRDetailId;
	
	@Column(name="bl_is_sp")
	private Boolean blIsSP;
	
	@Column(name="bl_is_service")
	private Boolean blIsService;
	
	@Column(name="bl_is_remarks")
	private Boolean blIsRemarks;
	
	@Column(name="num_service_part")
	private Integer numServiceorPart;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_price_per_piece")
	private BigDecimal numPricePerPiece;

	@Column(name="num_quantity")
	private BigDecimal numQuantity;

	@Column(name="num_total_wt")
	private BigDecimal numTotalWt;

	@Column(name="num_unit_wt")
	private BigDecimal numUnitWt;

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
	@JoinColumn(name="ser_tir_id")
	private SlsTblTIR slsTblTIR;
	
	
	
	
	public SlsTblTIRDetail() {
	}

	public Integer getSerTIRDetailId() {
		return serTIRDetailId;
	}

	public void setSerTIRDetailId(Integer serTIRDetailId) {
		this.serTIRDetailId = serTIRDetailId;
	}

	public Boolean getBlIsSP() {
		return blIsSP;
	}

	public void setBlIsSP(Boolean blIsSP) {
		this.blIsSP = blIsSP;
	}

	public Boolean getBlIsService() {
		return blIsService;
	}

	public void setBlIsService(Boolean blIsService) {
		this.blIsService = blIsService;
	}

	public Boolean getBlIsRemarks() {
		return blIsRemarks;
	}

	public void setBlIsRemarks(Boolean blIsRemarks) {
		this.blIsRemarks = blIsRemarks;
	}

	public Integer getNumServiceorPart() {
		return numServiceorPart;
	}

	public void setNumServiceorPart(Integer numServiceorPart) {
		this.numServiceorPart = numServiceorPart;
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

	public BigDecimal getNumPricePerPiece() {
		return numPricePerPiece;
	}

	public void setNumPricePerPiece(BigDecimal numPricePerPiece) {
		this.numPricePerPiece = numPricePerPiece;
	}

	public BigDecimal getNumQuantity() {
		return numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	public BigDecimal getNumTotalWt() {
		return numTotalWt;
	}

	public void setNumTotalWt(BigDecimal numTotalWt) {
		this.numTotalWt = numTotalWt;
	}

	public BigDecimal getNumUnitWt() {
		return numUnitWt;
	}

	public void setNumUnitWt(BigDecimal numUnitWt) {
		this.numUnitWt = numUnitWt;
	}

	public BigDecimal getNumStockAvailabe() {
		return numStockAvailabe;
	}

	public void setNumStockAvailabe(BigDecimal numStockAvailabe) {
		this.numStockAvailabe = numStockAvailabe;
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

	public String getTxtReason() {
		return txtReason;
	}

	public void setTxtReason(String txtReason) {
		this.txtReason = txtReason;
	}

	public String getTxtType() {
		return txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public CfgTblProcess getCfgTblProcess() {
		return cfgTblProcess;
	}

	public void setCfgTblProcess(CfgTblProcess cfgTblProcess) {
		this.cfgTblProcess = cfgTblProcess;
	}

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public CfgTblProductDesign getCfgTblProductDesign() {
		return cfgTblProductDesign;
	}

	public void setCfgTblProductDesign(CfgTblProductDesign cfgTblProductDesign) {
		this.cfgTblProductDesign = cfgTblProductDesign;
	}

	public CfgTblProductQuality getCfgTblProductQuality() {
		return cfgTblProductQuality;
	}

	public void setCfgTblProductQuality(CfgTblProductQuality cfgTblProductQuality) {
		this.cfgTblProductQuality = cfgTblProductQuality;
	}

	public SlsTblTIR getSlsTblTIR() {
		return slsTblTIR;
	}

	public void setSlsTblTIR(SlsTblTIR slsTblTIR) {
		this.slsTblTIR = slsTblTIR;
	}





}