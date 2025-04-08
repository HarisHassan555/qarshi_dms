package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_product_component database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_product_component")
@NamedQuery(name="CfgTblProductComponent.findAll", query="SELECT c FROM CfgTblProductComponent c")
public class CfgTblProductComponent implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_product_component_id")
	private Integer serProductComponentId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_perc_waistage")
	private BigDecimal numPercWaistage;

	@Column(name="num_quantity")
	private BigDecimal numQuantity;


	public CfgTblProduct getCfgTblProductChild() {
		return cfgTblProductChild;
	}

	public void setCfgTblProductChild(CfgTblProduct cfgTblProductChild) {
		this.cfgTblProductChild = cfgTblProductChild;
	}

	@ManyToOne
	@JoinColumn(name="ser_child_product_id")
	private CfgTblProduct cfgTblProductChild;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_furnace_id")
	private Integer serFurnaceId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@ManyToOne
	@JoinColumn(name="ser_parent_product_id")
	private CfgTblProduct cfgTblProductParent;

	public CfgTblProduct getCfgTblProductParent() {
		return cfgTblProductParent;
	}

	public void setCfgTblProductParent(CfgTblProduct cfgTblProductParent) {
		this.cfgTblProductParent = cfgTblProductParent;
	}

	@Column(name="ser_product_category_id")
	private Integer serProductCategoryId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_type")
	private String txtType;
	
	public Boolean getBlIsPacking() {
		return blIsPacking;
	}

	public void setBlIsPacking(Boolean blIsPacking) {
		this.blIsPacking = blIsPacking;
	}

	@Column(name="bl_is_packing")
	private Boolean blIsPacking;

	public CfgTblProductComponent() {
	}

	public Integer getSerProductComponentId() {
		return this.serProductComponentId;
	}

	public void setSerProductComponentId(Integer serProductComponentId) {
		this.serProductComponentId = serProductComponentId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
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

	public BigDecimal getNumPercWaistage() {
		return this.numPercWaistage;
	}

	public void setNumPercWaistage(BigDecimal numPercWaistage) {
		this.numPercWaistage = numPercWaistage;
	}

	public BigDecimal getNumQuantity() {
		return this.numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerFurnaceId() {
		return this.serFurnaceId;
	}

	public void setSerFurnaceId(Integer serFurnaceId) {
		this.serFurnaceId = serFurnaceId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}


	public Integer getSerProductCategoryId() {
		return this.serProductCategoryId;
	}

	public void setSerProductCategoryId(Integer serProductCategoryId) {
		this.serProductCategoryId = serProductCategoryId;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtType() {
		return this.txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}
	
	
	@ManyToOne
	@JoinColumn(name="ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;


	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}


}