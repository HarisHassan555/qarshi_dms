package com.bezkoder.spring.login.sa.dal.entities;


import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the cit_zone3_setup database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_zone3")
@NamedQuery(name="CfgTblZone3.findAll", query="SELECT c FROM CfgTblZone3 c")
public class CfgTblZone3 implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_zone3_id")
	private int serZone3Id;

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
	@JoinColumn(name="ser_zone2_id",nullable = true)
	private CfgTblZone2 cfgTblZone2;
	
	@Lob
	@Column(name="txt_zone3_code")
	private String txtZone3Code;

	@Lob
	@Column(name="txt_zone3_name")
	private String txtZone3Name;

	@Lob
	@Column(name="txt_description")
	private String txtDescription;
	
	//bi-directional many-to-one association to HrTblEmployee
//	@OneToMany(mappedBy="citZone3Setup1")
//	private List<HrTblEmployee> hrTblEmployees1;
//
//	//bi-directional many-to-one association to HrTblEmployee
//	@OneToMany(mappedBy="citZone3Setup2")
//	private List<HrTblEmployee> hrTblEmployees2;

	public CfgTblZone3() {
	}

	public int getSerZone3Id() {
		return this.serZone3Id;
	}

	public void setSerZone3Id(int serZone3Id) {
		this.serZone3Id = serZone3Id;
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

	

	public CfgTblZone2 getCfgTblZone2() {
		return cfgTblZone2;
	}

	public void setCfgTblZone2(CfgTblZone2 cfgTblZone2) {
		this.cfgTblZone2 = cfgTblZone2;
	}

	public String getTxtZone3Code() {
		return this.txtZone3Code;
	}

	public void setTxtZone3Code(String txtZone3Code) {
		this.txtZone3Code = txtZone3Code;
	}

	public String getTxtZone3Name() {
		return this.txtZone3Name;
	}

	public void setTxtZone3Name(String txtZone3Name) {
		this.txtZone3Name = txtZone3Name;
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
//		hrTblEmployees1.setCitZone3Setup1(this);
//
//		return hrTblEmployees1;
//	}
//
//	public HrTblEmployee removeHrTblEmployees1(HrTblEmployee hrTblEmployees1) {
//		getHrTblEmployees1().remove(hrTblEmployees1);
//		hrTblEmployees1.setCitZone3Setup1(null);
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
//		hrTblEmployees2.setCitZone3Setup2(this);
//
//		return hrTblEmployees2;
//	}
//
//	public HrTblEmployee removeHrTblEmployees2(HrTblEmployee hrTblEmployees2) {
//		getHrTblEmployees2().remove(hrTblEmployees2);
//		hrTblEmployees2.setCitZone3Setup2(null);
//
//		return hrTblEmployees2;
//	}



}