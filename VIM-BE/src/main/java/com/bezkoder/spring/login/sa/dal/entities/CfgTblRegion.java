package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_region_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_region")
@NamedQuery(name="CfgTblRegion.findAll", query="SELECT c FROM CfgTblRegion c")
public class CfgTblRegion implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_region_id")
	private int serRegionId;

	private byte bl_Status;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_created_date")
	private Date dteCreatedDate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_modified_date")
	private Date dteModifiedDate;

	@Column(name="ser_created_user_id")
	private int serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private int serModifiedUserId;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_region_code")
	private String txtRegionCode;

	@Column(name="txt_region_name")
	private String txtRegionName;

	//bi-directional many-to-one association to CfgTblZone
	@OneToMany(mappedBy="cfgTblRegion")
	private List<CfgTblZone> cfgTblZones;

	public CfgTblRegion() {
	}

	public int getSerRegionId() {
		return this.serRegionId;
	}

	public void setSerRegionId(int serRegionId) {
		this.serRegionId = serRegionId;
	}

	public byte getBl_Status() {
		return this.bl_Status;
	}

	public void setBl_Status(byte bl_Status) {
		this.bl_Status = bl_Status;
	}

	public Date getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Date dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Date getDteModifiedDate() {
		return this.dteModifiedDate;
	}

	public void setDteModifiedDate(Date dteModifiedDate) {
		this.dteModifiedDate = dteModifiedDate;
	}

	public int getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(int serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public int getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(int serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtRegionCode() {
		return this.txtRegionCode;
	}

	public void setTxtRegionCode(String txtRegionCode) {
		this.txtRegionCode = txtRegionCode;
	}

	public String getTxtRegionName() {
		return this.txtRegionName;
	}

	public void setTxtRegionName(String txtRegionName) {
		this.txtRegionName = txtRegionName;
	}



//	public CfgTblZone addCfgTblZone(CfgTblZone cfgTblZone) {
//		getCfgTblZones().add(cfgTblZone);
//		cfgTblZone.setCitRegionSetup(this);
//
//		return cfgTblZone;
//	}
//
//	public CfgTblZone removeCfgTblZone(CfgTblZone cfgTblZone) {
//		getCfgTblZones().remove(cfgTblZone);
//		cfgTblZone.setCitRegionSetup(null);
//
//		return cfgTblZone;
//	}

}