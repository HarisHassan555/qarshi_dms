package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_currency database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_currency")
@NamedQuery(name="CfgTblCurrency.findAll", query="SELECT c FROM CfgTblCurrency c")
public class CfgTblCurrency implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_currency_ser_currency_id_seq")
	@SequenceGenerator(name = "cfg_tbl_currency_ser_currency_id_seq", sequenceName = "cfg_tbl_currency_ser_currency_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_currency_id")
	private Integer serCurrencyId;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="is_parent_currency")
	private Boolean isParentCurrency;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_code")
	private String txtCode;

	@Column(name="txt_country")
	private String txtCountry;

	@Column(name="txt_currency_sign")
	private String txtCurrencySign;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_name")
	private String txtName;

	public CfgTblCurrency() {
	}

	public Integer getSerCurrencyId() {
		return this.serCurrencyId;
	}

	public void setSerCurrencyId(Integer serCurrencyId) {
		this.serCurrencyId = serCurrencyId;
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

	public Boolean getIsParentCurrency() {
		return this.isParentCurrency;
	}

	public void setIsParentCurrency(Boolean isParentCurrency) {
		this.isParentCurrency = isParentCurrency;
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

	public String getTxtCode() {
		return this.txtCode;
	}

	public void setTxtCode(String txtCode) {
		this.txtCode = txtCode;
	}

	public String getTxtCountry() {
		return this.txtCountry;
	}

	public void setTxtCountry(String txtCountry) {
		this.txtCountry = txtCountry;
	}

	public String getTxtCurrencySign() {
		return this.txtCurrencySign;
	}

	public void setTxtCurrencySign(String txtCurrencySign) {
		this.txtCurrencySign = txtCurrencySign;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtName() {
		return this.txtName;
	}

	public void setTxtName(String txtName) {
		this.txtName = txtName;
	}

}