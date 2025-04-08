package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_password_history database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_password_history")
@NamedQuery(name="CfgTblPasswordHistory.findAll", query="SELECT c FROM CfgTblPasswordHistory c")
public class CfgTblPasswordHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="CFG_TBL_PASSWORD_HISTORY_SERPASSWORDHISTORYID_GENERATOR", sequenceName="cfg_tbl_password_history_ser_password_history_id_seq",  initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="CFG_TBL_PASSWORD_HISTORY_SERPASSWORDHISTORYID_GENERATOR")
	@Column(name="ser_password_history_id")
	private Integer serPasswordHistoryId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_change_date")
	private Timestamp dteChangeDate;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_date")
	private Timestamp dteDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="ser_user_id")
	private Integer serUserId;

	@Column(name="txt_password")
	private String txtPassword;

	public CfgTblPasswordHistory() {
	}

	public Integer getSerPasswordHistoryId() {
		return this.serPasswordHistoryId;
	}

	public void setSerPasswordHistoryId(Integer serPasswordHistoryId) {
		this.serPasswordHistoryId = serPasswordHistoryId;
	}

	public Boolean getBlIsActive() {
		return this.blIsActive;
	}

	public void setBlIsActive(Boolean blIsActive) {
		this.blIsActive = blIsActive;
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

	public Timestamp getDteChangeDate() {
		return this.dteChangeDate;
	}

	public void setDteChangeDate(Timestamp dteChangeDate) {
		this.dteChangeDate = dteChangeDate;
	}

	public Timestamp getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Timestamp dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Timestamp getDteDate() {
		return this.dteDate;
	}

	public void setDteDate(Timestamp dteDate) {
		this.dteDate = dteDate;
	}

	public Timestamp getDteModifiedDate() {
		return this.dteModifiedDate;
	}

	public void setDteModifiedDate(Timestamp dteModifiedDate) {
		this.dteModifiedDate = dteModifiedDate;
	}

	public Integer getSerCreatedUser() {
		return this.serCreatedUser;
	}

	public void setSerCreatedUser(Integer serCreatedUser) {
		this.serCreatedUser = serCreatedUser;
	}

	public Integer getSerModifiedUser() {
		return this.serModifiedUser;
	}

	public void setSerModifiedUser(Integer serModifiedUser) {
		this.serModifiedUser = serModifiedUser;
	}

	public Integer getSerUserId() {
		return this.serUserId;
	}

	public void setSerUserId(Integer serUserId) {
		this.serUserId = serUserId;
	}

	public String getTxtPassword() {
		return this.txtPassword;
	}

	public void setTxtPassword(String txtPassword) {
		this.txtPassword = txtPassword;
	}

}