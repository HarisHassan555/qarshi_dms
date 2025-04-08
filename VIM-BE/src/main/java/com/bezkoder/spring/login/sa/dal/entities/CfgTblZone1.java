package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_Zone1_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_Zone1")
@NamedQuery(name="CfgTblZone1.findAll", query="SELECT c FROM CfgTblZone1 c")
public class CfgTblZone1 implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_Zone1_id")
	private int serZone1Id;

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

	@Column(name="txt_Zone1_code")
	private String txtZone1Code;

	@Column(name="txt_Zone1_name")
	private String txtZone1Name;

	//bi-directional many-to-one association to CfgTblZone
//	@OneToMany(mappedBy="cfgTblZone1")
//	private List<CfgTblZone1> cfgTblZones;

	public CfgTblZone1() {
	}

	public int getSerZone1Id() {
		return this.serZone1Id;
	}

	public void setSerZone1Id(int serZone1Id) {
		this.serZone1Id = serZone1Id;
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

	public String getTxtZone1Code() {
		return this.txtZone1Code;
	}

	public void setTxtZone1Code(String txtZone1Code) {
		this.txtZone1Code = txtZone1Code;
	}

	public String getTxtZone1Name() {
		return this.txtZone1Name;
	}

	public void setTxtZone1Name(String txtZone1Name) {
		this.txtZone1Name = txtZone1Name;
	}



//	public CfgTblZone addCfgTblZone(CfgTblZone cfgTblZone) {
//		getCfgTblZones().add(cfgTblZone);
//		cfgTblZone.setCitZone1Setup(this);
//
//		return cfgTblZone;
//	}
//
//	public CfgTblZone removeCfgTblZone(CfgTblZone cfgTblZone) {
//		getCfgTblZones().remove(cfgTblZone);
//		cfgTblZone.setCitZone1Setup(null);
//
//		return cfgTblZone;
//	}

}