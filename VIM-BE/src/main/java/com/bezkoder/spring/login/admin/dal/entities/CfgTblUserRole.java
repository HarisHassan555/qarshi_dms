package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_user_role database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_user_role")
@NamedQuery(name="CfgTblUserRole.findAll", query="SELECT c FROM CfgTblUserRole c")
public class CfgTblUserRole implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_user_role_id")
	private Integer serUserRoleId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	//bi-directional many-to-one association to CfgTblRole
	@ManyToOne
	@JoinColumn(name="ser_role_id")
	private CfgTblRole cfgTblRole;

	//bi-directional many-to-one association to CfgTblUser
	@ManyToOne
	@JoinColumn(name="ser_user_id")
	private CfgTblUser cfgTblUser;

	public CfgTblUserRole() {
	}

	public Integer getSerUserRoleId() {
		return this.serUserRoleId;
	}

	public void setSerUserRoleId(Integer serUserRoleId) {
		this.serUserRoleId = serUserRoleId;
	}

	public Boolean getBlIsActive() {
		return this.blIsActive;
	}

	public void setBlIsActive(Boolean blIsActive) {
		this.blIsActive = blIsActive;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public Timestamp getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Timestamp dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
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

	public CfgTblRole getCfgTblRole() {
		return this.cfgTblRole;
	}

	public void setCfgTblRole(CfgTblRole cfgTblRole) {
		this.cfgTblRole = cfgTblRole;
	}

	public CfgTblUser getCfgTblUser() {
		return this.cfgTblUser;
	}

	public void setCfgTblUser(CfgTblUser cfgTblUser) {
		this.cfgTblUser = cfgTblUser;
	}

}