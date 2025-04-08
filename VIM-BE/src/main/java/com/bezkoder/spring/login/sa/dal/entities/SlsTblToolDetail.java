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
@Table(name = "sls_tbl_tool_detail")
@NamedQuery(name = "SlsTblToolDetail.findAll", query = "SELECT i FROM SlsTblToolDetail i")
public class SlsTblToolDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_issue_detail_id") private Integer serIssueDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_tbl_issue_detail_ser_issue_detail_id_seq")
	@SequenceGenerator(name = "inv_tbl_issue_detail_ser_issue_detail_id_seq", sequenceName = "inv_tbl_issue_detail_ser_issue_detail_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_tool_detail_id")
	private Integer sertoolDetailId;

	@Column(name = "bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name = "dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name = "dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name = "num_quantity")
	private BigDecimal numQuantity;

	@Column(name = "num_quantity_receive")
	private BigDecimal numQuantityReceive;

	@Column(name = "bl_is_standard")
	private Boolean blIsstandard;

	@Column(name = "ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name = "ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name = "txt_machine_ip")
	private String txtMachineIp;

	@Column(name = "txt_reason")
	private String txtReason;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_product_id")
	private CfgTblProduct cfgTblProduct;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_sale_order_id")
	private SlsTblSaleOrder slsTblSaleOrder;

	// bi-directional many-to-one association to SlsTblSoDetail
	@ManyToOne
	@JoinColumn(name = "ser_so_detail_id")
	private SlsTblSoDetail slsTblSoDetail;

	public SlsTblToolDetail() {
	}

	public Integer getSertoolDetailId() {
		return sertoolDetailId;
	}

	public void setSertoolDetailId(Integer sertoolDetailId) {
		this.sertoolDetailId = sertoolDetailId;
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

	public BigDecimal getNumQuantity() {
		return numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	public BigDecimal getNumQuantityReceive() {
		return numQuantityReceive;
	}

	public void setNumQuantityReceive(BigDecimal numQuantityReceive) {
		this.numQuantityReceive = numQuantityReceive;
	}

	public Boolean getBlIsstandard() {
		return blIsstandard;
	}

	public void setBlIsstandard(Boolean blIsstandard) {
		this.blIsstandard = blIsstandard;
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

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public SlsTblSaleOrder getSlsTblSaleOrder() {
		return slsTblSaleOrder;
	}

	public void setSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		this.slsTblSaleOrder = slsTblSaleOrder;
	}

	public SlsTblSoDetail getSlsTblSoDetail() {
		return slsTblSoDetail;
	}

	public void setSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		this.slsTblSoDetail = slsTblSoDetail;
	}

}