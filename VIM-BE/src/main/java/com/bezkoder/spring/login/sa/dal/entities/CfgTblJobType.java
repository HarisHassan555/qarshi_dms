package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_jobType database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_jobType")
@NamedQuery(name="CfgTblJobType.findAll", query="SELECT c FROM CfgTblJobType c")
public class CfgTblJobType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_jobType_id")
	private Integer serJobTypeId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_isnational")
	private Boolean blnIsnational;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_parent_jobType_id")
	private Integer serParentJobTypeId;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_name")
	private String txtName;



	public CfgTblJobType() {
	}

	public Integer getSerJobTypeId() {
		return this.serJobTypeId;
	}

	public void setSerJobTypeId(Integer serJobTypeId) {
		this.serJobTypeId = serJobTypeId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsnational() {
		return this.blnIsnational;
	}

	public void setBlnIsnational(Boolean blnIsnational) {
		this.blnIsnational = blnIsnational;
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

	public Integer getSerParentJobTypeId() {
		return this.serParentJobTypeId;
	}

	public void setSerParentJobTypeId(Integer serParentJobTypeId) {
		this.serParentJobTypeId = serParentJobTypeId;
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