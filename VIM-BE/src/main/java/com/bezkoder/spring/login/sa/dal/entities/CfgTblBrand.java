package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_brand database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_brand")
@NamedQuery(name="CfgTblBrand.findAll", query="SELECT c FROM CfgTblBrand c")
public class CfgTblBrand implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_brand_id")
	private Integer serBrandId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_brand_ser_brand_id_seq")
	@SequenceGenerator(name = "cfg_tbl_brand_ser_brand_id_seq", sequenceName = "cfg_tbl_brand_ser_brand_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_brand_id")
	private Integer serBrandId;


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_truck_qty")
	private BigDecimal numTruckQty;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_brand_code")
	private String txtBrandCode;

	@Column(name="txt_brand_name")
	private String txtBrandName;
	
	@Column(name="txt_SAP_code")
	private String txtSAPCode;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	//bi-directional many-to-one association to CfgTblProduct
	@OneToMany(mappedBy="cfgTblBrand")
	private List<CfgTblProduct> cfgTblProducts;

	public CfgTblBrand() {
	}

	public Integer getSerBrandId() {
		return this.serBrandId;
	}

	public void setSerBrandId(Integer serBrandId) {
		this.serBrandId = serBrandId;
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

	public BigDecimal getNumTruckQty() {
		return this.numTruckQty;
	}

	public void setNumTruckQty(BigDecimal numTruckQty) {
		this.numTruckQty = numTruckQty;
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

	public String getTxtBrandCode() {
		return this.txtBrandCode;
	}

	public void setTxtBrandCode(String txtBrandCode) {
		this.txtBrandCode = txtBrandCode;
	}

	public String getTxtBrandName() {
		return this.txtBrandName;
	}

	public void setTxtBrandName(String txtBrandName) {
		this.txtBrandName = txtBrandName;
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

	public List<CfgTblProduct> getCfgTblProducts() {
		return this.cfgTblProducts;
	}

	public void setCfgTblProducts(List<CfgTblProduct> cfgTblProducts) {
		this.cfgTblProducts = cfgTblProducts;
	}

	public CfgTblProduct addCfgTblProduct(CfgTblProduct cfgTblProduct) {
		getCfgTblProducts().add(cfgTblProduct);
		cfgTblProduct.setCfgTblBrand(this);

		return cfgTblProduct;
	}

	public CfgTblProduct removeCfgTblProduct(CfgTblProduct cfgTblProduct) {
		getCfgTblProducts().remove(cfgTblProduct);
		cfgTblProduct.setCfgTblBrand(null);

		return cfgTblProduct;
	}

	public String getTxtSAPCode() {
		return txtSAPCode;
	}

	public void setTxtSAPCode(String txtSAPCode) {
		this.txtSAPCode = txtSAPCode;
	}

}