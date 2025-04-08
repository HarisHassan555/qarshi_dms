package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the sls_tbl_claim_Detail database table.
 * 
 */
@Entity
@Table(name="sls_tbl_claim_Detail")
@NamedQuery(name="SlsTblClaimDetail.findAll", query="SELECT i FROM SlsTblClaimDetail i")
public class SlsTblClaimDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_claim_detail_id")
	private Integer serClaimDetailId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sls_tbl_claim_Detail_ser_claim_detail_id_seq")
	@SequenceGenerator(name = "sls_tbl_claim_Detail_ser_claim_detail_id_seq", sequenceName = "sls_tbl_claim_Detail_ser_claim_detail_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_claim_detail_id")
	private Integer serClaimDetailId;
	
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
	
	 @Column(name="num_amount")
	 private BigDecimal numAmount;
	 
	 @Column(name="num_service_amount")
	 private BigDecimal numServiceAmount;
	
	@ManyToOne
	@JoinColumn(name="ser_claim_id")
	private SlsTblClaim slsTblClaim;

	//bi-directional many-to-one association to CfgTblProcess
	@ManyToOne
	@JoinColumn(name="ser_process_id")
	private CfgTblProcess cfgTblProcess;

	//bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name="ser_product_id")
	private CfgTblProduct cfgTblProduct;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;
	
	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	
	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_dealer_id")
	private CfgTblCustomer cfgTblDealer;
	
	@ManyToOne
	@JoinColumn(name = "ser_work_order_id")
	private SlsTblWorkOrder slsTblWorkOrder;

	public Integer getSerClaimDetailId() {
		return serClaimDetailId;
	}

	public void setSerClaimDetailId(Integer serClaimDetailId) {
		this.serClaimDetailId = serClaimDetailId;
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

	public BigDecimal getNumAmount() {
		return numAmount;
	}

	public void setNumAmount(BigDecimal numAmount) {
		this.numAmount = numAmount;
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

	public SlsTblSoVehicleDetail getSlsTblSoVehicleDetail() {
		return slsTblSoVehicleDetail;
	}

	public void setSlsTblSoVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail) {
		this.slsTblSoVehicleDetail = slsTblSoVehicleDetail;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public CfgTblCustomer getCfgTblDealer() {
		return cfgTblDealer;
	}

	public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
		this.cfgTblDealer = cfgTblDealer;
	}

	public SlsTblClaim getSlsTblClaim() {
		return slsTblClaim;
	}

	public void setSlsTblClaim(SlsTblClaim slsTblClaim) {
		this.slsTblClaim = slsTblClaim;
	}

	public SlsTblWorkOrder getSlsTblWorkOrder() {
		return slsTblWorkOrder;
	}

	public void setSlsTblWorkOrder(SlsTblWorkOrder slsTblWorkOrder) {
		this.slsTblWorkOrder = slsTblWorkOrder;
	}

	public BigDecimal getNumServiceAmount() {
		return numServiceAmount;
	}

	public void setNumServiceAmount(BigDecimal numServiceAmount) {
		this.numServiceAmount = numServiceAmount;
	}
	
	

}