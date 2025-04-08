package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_product_category database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_product_category")
@NamedQuery(name="CfgTblProductCategory.findAll", query="SELECT c FROM CfgTblProductCategory c")
public class CfgTblProductCategory implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_product_category_id")
	private Integer serProductCategoryId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_salec_tax")
	private BigDecimal numSalecTax;
	
	@Column(name="txt_SAP_code")
	private String txtSAPCode;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_parent_category_id")
	private Integer serParentCategoryId;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_product_category_code")
	private String txtProductCategoryCode;

	@Column(name="txt_product_category_name")
	private String txtProductCategoryName;

	//bi-directional many-to-one association to CfgTblProduct
	@OneToMany(mappedBy="cfgTblProductCategory")
	private List<CfgTblProduct> cfgTblProducts;

	public CfgTblProductCategory() {
	}

	public Integer getSerProductCategoryId() {
		return this.serProductCategoryId;
	}

	public void setSerProductCategoryId(Integer serProductCategoryId) {
		this.serProductCategoryId = serProductCategoryId;
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

	public BigDecimal getNumSalecTax() {
		return this.numSalecTax;
	}

	public void setNumSalecTax(BigDecimal numSalecTax) {
		this.numSalecTax = numSalecTax;
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

	public Integer getSerParentCategoryId() {
		return this.serParentCategoryId;
	}

	public void setSerParentCategoryId(Integer serParentCategoryId) {
		this.serParentCategoryId = serParentCategoryId;
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

	public String getTxtProductCategoryCode() {
		return this.txtProductCategoryCode;
	}

	public void setTxtProductCategoryCode(String txtProductCategoryCode) {
		this.txtProductCategoryCode = txtProductCategoryCode;
	}

	public String getTxtProductCategoryName() {
		return this.txtProductCategoryName;
	}

	public void setTxtProductCategoryName(String txtProductCategoryName) {
		this.txtProductCategoryName = txtProductCategoryName;
	}

	public String getTxtSAPCode() {
		return txtSAPCode;
	}

	public void setTxtSAPCode(String txtSAPCode) {
		this.txtSAPCode = txtSAPCode;
	}

	
}