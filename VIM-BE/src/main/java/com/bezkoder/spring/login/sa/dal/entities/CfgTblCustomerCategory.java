package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_customer_category database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_customer_category")
@NamedQuery(name="CfgTblCustomerCategory.findAll", query="SELECT c FROM CfgTblCustomerCategory c")
public class CfgTblCustomerCategory implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_customer_category_ser_customer_category_id_seq")
	@SequenceGenerator(name = "cfg_tbl_customer_category_ser_customer_category_id_seq", sequenceName = "cfg_tbl_customer_category_ser_customer_category_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_customer_category_id")
	private Integer serCustomerCategoryId;

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

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_parent_category_id")
	private Integer serParentCategoryId;

	@Column(name="txt_customer_category_code")
	private String txtCustomerCategoryCode;

	@Column(name="txt_customer_category_name")
	private String txtCustomerCategoryName;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	//bi-directional many-to-one association to CfgTblCustomer
	@OneToMany(mappedBy="cfgTblCustomerCategory")
	private List<CfgTblCustomer> cfgTblCustomers;

	public CfgTblCustomerCategory() {
	}

	public Integer getSerCustomerCategoryId() {
		return this.serCustomerCategoryId;
	}

	public void setSerCustomerCategoryId(Integer serCustomerCategoryId) {
		this.serCustomerCategoryId = serCustomerCategoryId;
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

	public String getTxtCustomerCategoryCode() {
		return this.txtCustomerCategoryCode;
	}

	public void setTxtCustomerCategoryCode(String txtCustomerCategoryCode) {
		this.txtCustomerCategoryCode = txtCustomerCategoryCode;
	}

	public String getTxtCustomerCategoryName() {
		return this.txtCustomerCategoryName;
	}

	public void setTxtCustomerCategoryName(String txtCustomerCategoryName) {
		this.txtCustomerCategoryName = txtCustomerCategoryName;
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

	public List<CfgTblCustomer> getCfgTblCustomers() {
		return this.cfgTblCustomers;
	}

	public void setCfgTblCustomers(List<CfgTblCustomer> cfgTblCustomers) {
		this.cfgTblCustomers = cfgTblCustomers;
	}

	public CfgTblCustomer addCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		getCfgTblCustomers().add(cfgTblCustomer);
		cfgTblCustomer.setCfgTblCustomerCategory(this);

		return cfgTblCustomer;
	}

	public CfgTblCustomer removeCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		getCfgTblCustomers().remove(cfgTblCustomer);
		cfgTblCustomer.setCfgTblCustomerCategory(null);

		return cfgTblCustomer;
	}

}