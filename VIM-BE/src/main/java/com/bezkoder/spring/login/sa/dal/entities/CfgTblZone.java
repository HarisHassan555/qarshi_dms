package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_zone_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_zone")
@NamedQuery(name="CfgTblZone.findAll", query="SELECT c FROM CfgTblZone c")
public class CfgTblZone implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_zone_id")
	private int serZoneId;

	@Column(name="bl_is_deleted")
	private byte blIsDeleted;

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

	
	@Column(name="txt_zone_code")
	private String txtZoneCode;

	
	@Column(name="txt_zone_name")
	private String txtZoneName;

	//bi-directional many-to-one association to CfgTblArea
	@OneToMany(mappedBy="cfgTblZone")
	private List<CfgTblArea> cfgTblAreas;

	//bi-directional many-to-one association to CfgTblRegion
	@ManyToOne
	@JoinColumn(name="ser_region_id")
	private CfgTblRegion cfgTblRegion;

	public CfgTblZone() {
	}

	public int getSerZoneId() {
		return this.serZoneId;
	}

	public void setSerZoneId(int serZoneId) {
		this.serZoneId = serZoneId;
	}

	public byte getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(byte blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
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

	public String getTxtZoneCode() {
		return this.txtZoneCode;
	}

	public void setTxtZoneCode(String txtZoneCode) {
		this.txtZoneCode = txtZoneCode;
	}

	public String getTxtZoneName() {
		return this.txtZoneName;
	}

	public void setTxtZoneName(String txtZoneName) {
		this.txtZoneName = txtZoneName;
	}

	public List<CfgTblArea> getCfgTblAreas() {
		return this.cfgTblAreas;
	}

	public void setCfgTblAreas(List<CfgTblArea> cfgTblAreas) {
		this.cfgTblAreas = cfgTblAreas;
	}

//	public CfgTblArea addCfgTblArea(CfgTblArea cfgTblArea) {
//		getCfgTblAreas().add(cfgTblArea);
//		cfgTblArea.setCfgTblZone(this);
//
//		return cfgTblArea;
//	}
//
//	public CfgTblArea removeCfgTblArea(CfgTblArea cfgTblArea) {
//		getCfgTblAreas().remove(cfgTblArea);
//		cfgTblArea.setCfgTblZone(null);
//
//		return cfgTblArea;
//	}

	public CfgTblRegion getCfgTblRegion() {
		return this.cfgTblRegion;
	}

	public void setCfgTblRegion(CfgTblRegion cfgTblRegion) {
		this.cfgTblRegion = cfgTblRegion;
	}

}