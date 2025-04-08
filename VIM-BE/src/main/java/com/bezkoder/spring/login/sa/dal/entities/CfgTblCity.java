package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;
import java.math.BigDecimal;

/**
 * The persistent class for the cfg_tbl_city database table.
 */
@Entity
@Table(name = "cfg_tbl_city")
@NamedQuery(name = "CfgTblCity.findAll", query = "SELECT c FROM CfgTblCity c")
public class CfgTblCity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_city_ser_city_id_seq")
	@SequenceGenerator(name = "cfg_tbl_city_ser_city_id_seq", sequenceName = "cfg_tbl_city_ser_city_id_seq", initialValue = 1, allocationSize = 1)
	@Column(name = "ser_city_id")
	private Integer serCityId;

	@Column(name = "bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name = "bln_status")
	private Boolean blnStatus;

	@Column(name = "txt_city_code")
	private String txtCityCode;

	@Column(name = "txt_city_name")
	private String txtCityName;

	@Column(name = "num_freight")
	private BigDecimal numFreight;

	@ManyToOne
	@JoinColumn(name = "ser_country_id")
	private CfgTblCountry cfgTblCountry;

	public CfgTblCity() {}

	// Getters and Setters

	public Integer getSerCityId() {
		return this.serCityId;
	}

	public void setSerCityId(Integer serCityId) {
		this.serCityId = serCityId;
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

	public String getTxtCityCode() {
		return this.txtCityCode;
	}

	public void setTxtCityCode(String txtCityCode) {
		this.txtCityCode = txtCityCode;
	}

	public String getTxtCityName() {
		return this.txtCityName;
	}

	public void setTxtCityName(String txtCityName) {
		this.txtCityName = txtCityName;
	}

	public BigDecimal getNumFreight() {
		return this.numFreight;
	}

	public void setNumFreight(BigDecimal numFreight) {
		this.numFreight = numFreight;
	}

	public CfgTblCountry getCfgTblCountry() {
		return this.cfgTblCountry;
	}

	public void setCfgTblCountry(CfgTblCountry cfgTblCountry) {
		this.cfgTblCountry = cfgTblCountry;
	}
}