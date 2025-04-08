package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_distribution_channel database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_distribution_channel")
@NamedQuery(name="CfgTblDistributionChannel.findAll", query="SELECT c FROM CfgTblDistributionChannel c")
public class CfgTblDistributionChannel implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="CFG_TBL_DISTRIBUTION_CHANNEL_SERDISTRIBUTIONCHANNELID_GENERATOR", sequenceName="cfg_tbl_distribution_channel_seq")
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="CFG_TBL_DISTRIBUTION_CHANNEL_SERDISTRIBUTIONCHANNELID_GENERATOR")
	@Column(name="ser_distribution_channel_id")
	private Integer serDistributionChannelId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

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

	@Column(name="txt_code")
	private String txtCode;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_name")
	private String txtName;

	public CfgTblDistributionChannel() {
	}

	public Integer getSerDistributionChannelId() {
		return this.serDistributionChannelId;
	}

	public void setSerDistributionChannelId(Integer serDistributionChannelId) {
		this.serDistributionChannelId = serDistributionChannelId;
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

	public String getTxtName() {
		return this.txtName;
	}

	public void setTxtName(String txtName) {
		this.txtName = txtName;
	}

}