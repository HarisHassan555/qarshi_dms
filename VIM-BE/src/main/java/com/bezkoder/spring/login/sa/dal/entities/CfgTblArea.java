package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_area_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_area")
@NamedQuery(name="CfgTblArea.findAll", query="SELECT c FROM CfgTblArea c")
public class CfgTblArea implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_area_id")
	private int serAreaId;

	@Column(name="bl_is_deleted")
	private byte blIsDeleted;

	@Column(name="bl_isbranch")
	private int blIsbranch;

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

	@Lob
	@Column(name="Ser_parent_id",nullable = true)
	private String ser_parent_id=null;

	@PrePersist
	void preInsert() {
	   if (this.ser_parent_id == null)
	       this.ser_parent_id = "";
	}

	@ManyToOne
	@JoinColumn(name="ser_zone_id",nullable = true)
	private CfgTblZone cfgTblZone;
	
	@Lob
	@Column(name="txt_area_code")
	private String txtAreaCode;

	@Lob
	@Column(name="txt_area_name")
	private String txtAreaName;

	@Lob
	@Column(name="txt_description")
	private String txtDescription;
	
	//bi-directional many-to-one association to HrTblEmployee
//	@OneToMany(mappedBy="citAreaSetup1")
//	private List<HrTblEmployee> hrTblEmployees1;
//
//	//bi-directional many-to-one association to HrTblEmployee
//	@OneToMany(mappedBy="citAreaSetup2")
//	private List<HrTblEmployee> hrTblEmployees2;

	public CfgTblArea() {
	}

	public int getSerAreaId() {
		return this.serAreaId;
	}

	public void setSerAreaId(int serAreaId) {
		this.serAreaId = serAreaId;
	}

	public byte getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(byte blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public int getBlIsbranch() {
		return this.blIsbranch;
	}

	public void setBlIsbranch(int blIsbranch) {
		this.blIsbranch = blIsbranch;
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

	public String getSer_parent_id() {
		return this.ser_parent_id;
	}

	public void setSer_parent_id(String ser_parent_id) {
		if( ser_parent_id==null){
			ser_parent_id=null;
		}else{
		this.ser_parent_id = ser_parent_id;
	}
}
	public CfgTblZone getCfgTblZone() {
		return this.cfgTblZone;
	}

	public void setCfgTblZone(CfgTblZone cfgTblZone) {
		this.cfgTblZone = cfgTblZone;
	}

	public String getTxtAreaCode() {
		return this.txtAreaCode;
	}

	public void setTxtAreaCode(String txtAreaCode) {
		this.txtAreaCode = txtAreaCode;
	}

	public String getTxtAreaName() {
		return this.txtAreaName;
	}

	public void setTxtAreaName(String txtAreaName) {
		this.txtAreaName = txtAreaName;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}
	
//	public List<HrTblEmployee> getHrTblEmployees1() {
//		return this.hrTblEmployees1;
//	}
//
//	public void setHrTblEmployees1(List<HrTblEmployee> hrTblEmployees1) {
//		this.hrTblEmployees1 = hrTblEmployees1;
//	}

//	public HrTblEmployee addHrTblEmployees1(HrTblEmployee hrTblEmployees1) {
//		getHrTblEmployees1().add(hrTblEmployees1);
//		hrTblEmployees1.setCitAreaSetup1(this);
//
//		return hrTblEmployees1;
//	}
//
//	public HrTblEmployee removeHrTblEmployees1(HrTblEmployee hrTblEmployees1) {
//		getHrTblEmployees1().remove(hrTblEmployees1);
//		hrTblEmployees1.setCitAreaSetup1(null);
//
//		return hrTblEmployees1;
//	}

//	public List<HrTblEmployee> getHrTblEmployees2() {
//		return this.hrTblEmployees2;
//	}
//
//	public void setHrTblEmployees2(List<HrTblEmployee> hrTblEmployees2) {
//		this.hrTblEmployees2 = hrTblEmployees2;
//	}

//	public HrTblEmployee addHrTblEmployees2(HrTblEmployee hrTblEmployees2) {
//		getHrTblEmployees2().add(hrTblEmployees2);
//		hrTblEmployees2.setCitAreaSetup2(this);
//
//		return hrTblEmployees2;
//	}
//
//	public HrTblEmployee removeHrTblEmployees2(HrTblEmployee hrTblEmployees2) {
//		getHrTblEmployees2().remove(hrTblEmployees2);
//		hrTblEmployees2.setCitAreaSetup2(null);
//
//		return hrTblEmployees2;
//	}



}