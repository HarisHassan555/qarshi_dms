package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_uom database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_uom")
@NamedQuery(name="CfgTblUom.findAll", query="SELECT c FROM CfgTblUom c")
public class CfgTblUom implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_uom_id")
	private Integer serUomId;

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

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_unit")
	private String txtUnit;

	@Column(name="txt_unit_base")
	private String txtUnitBase;

	//bi-directional many-to-one association to CfgTblProduct
	@OneToMany(mappedBy="cfgTblUom")
	private List<CfgTblProduct> cfgTblProducts;

	public CfgTblUom() {
	}

	public Integer getSerUomId() {
		return this.serUomId;
	}

	public void setSerUomId(Integer serUomId) {
		this.serUomId = serUomId;
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

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtUnit() {
		return this.txtUnit;
	}

	public void setTxtUnit(String txtUnit) {
		this.txtUnit = txtUnit;
	}

	public String getTxtUnitBase() {
		return this.txtUnitBase;
	}

	public void setTxtUnitBase(String txtUnitBase) {
		this.txtUnitBase = txtUnitBase;
	}

	public List<CfgTblProduct> getCfgTblProducts() {
		return this.cfgTblProducts;
	}

	public void setCfgTblProducts(List<CfgTblProduct> cfgTblProducts) {
		this.cfgTblProducts = cfgTblProducts;
	}

	public CfgTblProduct addCfgTblProduct(CfgTblProduct cfgTblProduct) {
		getCfgTblProducts().add(cfgTblProduct);
		cfgTblProduct.setCfgTblUom(this);

		return cfgTblProduct;
	}

	public CfgTblProduct removeCfgTblProduct(CfgTblProduct cfgTblProduct) {
		getCfgTblProducts().remove(cfgTblProduct);
		cfgTblProduct.setCfgTblUom(null);

		return cfgTblProduct;
	}

}