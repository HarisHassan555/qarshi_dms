package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_zone2_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_zone2")
@NamedQuery(name="CfgTblZone2.findAll", query="SELECT c FROM CfgTblZone c")
public class CfgTblZone2 implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_zone2_id")
	private int serZone2Id;

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

	private int serZone1Id;

	
	@Column(name="txt_description")
	private String txtDescription;

	
	@Column(name="txt_zone2_code")
	private String txtZone2Code;

	
	@Column(name="txt_zone2_name")
	private String txtZone2Name;

	//bi-directional many-to-one association to CfgTblArea
	@OneToMany(mappedBy="cfgTblZone2")
	private List<CfgTblZone3> cfgTblZone3;

	//bi-directional many-to-one association to CfgTblZone1
	@ManyToOne
	@JoinColumn(name="ser_zone1_id")
	private CfgTblZone1 cfgTblZone1;

	public CfgTblZone2() {
	}

	public int getSerZone2Id() {
		return serZone2Id;
	}

	public void setSerZone2Id(int serZone2Id) {
		this.serZone2Id = serZone2Id;
	}

	public byte getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(byte blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public byte getBl_Status() {
		return bl_Status;
	}

	public void setBl_Status(byte bl_Status) {
		this.bl_Status = bl_Status;
	}

	public Date getDteCreatedDate() {
		return dteCreatedDate;
	}

	public void setDteCreatedDate(Date dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Date getDteModifiedDate() {
		return dteModifiedDate;
	}

	public void setDteModifiedDate(Date dteModifiedDate) {
		this.dteModifiedDate = dteModifiedDate;
	}

	public int getSerCreatedUserId() {
		return serCreatedUserId;
	}

	public void setSerCreatedUserId(int serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public int getSerModifiedUserId() {
		return serModifiedUserId;
	}

	public void setSerModifiedUserId(int serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public int getSerZone1Id() {
		return serZone1Id;
	}

	public void setSerZone1Id(int serZone1Id) {
		this.serZone1Id = serZone1Id;
	}

	public String getTxtDescription() {
		return txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtZone2Code() {
		return txtZone2Code;
	}

	public void setTxtZone2Code(String txtZone2Code) {
		this.txtZone2Code = txtZone2Code;
	}

	public String getTxtZone2Name() {
		return txtZone2Name;
	}

	public void setTxtZone2Name(String txtZone2Name) {
		this.txtZone2Name = txtZone2Name;
	}

	public List<CfgTblZone3> getCfgTblZone3() {
		return cfgTblZone3;
	}

	public void setCfgTblZone3(List<CfgTblZone3> cfgTblZone3) {
		this.cfgTblZone3 = cfgTblZone3;
	}

	public CfgTblZone1 getCfgTblZone1() {
		return cfgTblZone1;
	}

	public void setCfgTblZone1(CfgTblZone1 cfgTblZone1) {
		this.cfgTblZone1 = cfgTblZone1;
	}

	

}