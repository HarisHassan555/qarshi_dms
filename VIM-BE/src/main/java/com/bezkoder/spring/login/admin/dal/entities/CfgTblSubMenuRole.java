package com.bezkoder.spring.login.admin.dal.entities;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_sub_menu_role database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_sub_menu_role")
@NamedQuery(name="CfgTblSubMenuRole.findAll", query="SELECT c FROM CfgTblSubMenuRole c")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "serSubMenuRoleId")
public class CfgTblSubMenuRole implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_sub_menu_role_id")
	private Integer serSubMenuRoleId;

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
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="ser_role_id")
	@JsonIgnore
	private CfgTblRole cfgTblRole;

	//bi-directional many-to-one association to CfgTblSubMenu
	@ManyToOne
	@JoinColumn(name="ser_sub_menu_id")
	private CfgTblSubMenu cfgTblSubMenu;


	@ManyToOne
	@JoinColumn(name="ser_user_id")
	private CfgTblUser cfgTblUser;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bl_is_view")
	private Boolean blIsview;

	@Column(name="bl_is_add")
	private Boolean blIsAdd;

	@Column(name="bl_is_delete")
	private Boolean blIsDelete;


	@Column(name="bl_is_update")
	private Boolean blIsUpdate;

	@Column(name="bl_is_approve")
	private Boolean blIsApprove;


	@Column(name="bl_is_all")
	private Boolean blIsAll;


	@Column(name="bl_is_enabled")
	private Boolean blIsEnabled;

	public CfgTblSubMenuRole() {
	}

	public Integer getSerSubMenuRoleId() {
		return this.serSubMenuRoleId;
	}

	public void setSerSubMenuRoleId(Integer serSubMenuRoleId) {
		this.serSubMenuRoleId = serSubMenuRoleId;
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

	public CfgTblSubMenu getCfgTblSubMenu() {
		return this.cfgTblSubMenu;
	}

	public void setCfgTblSubMenu(CfgTblSubMenu cfgTblSubMenu) {
		this.cfgTblSubMenu = cfgTblSubMenu;
	}

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlIsview() {
		return blIsview;
	}

	public void setBlIsview(Boolean blIsview) {
		this.blIsview = blIsview;
	}

	public Boolean getBlIsAdd() {
		return blIsAdd;
	}

	public void setBlIsAdd(Boolean blIsAdd) {
		this.blIsAdd = blIsAdd;
	}

	public Boolean getBlIsDelete() {
		return blIsDelete;
	}

	public void setBlIsDelete(Boolean blIsDelete) {
		this.blIsDelete = blIsDelete;
	}

	public Boolean getBlIsUpdate() {
		return blIsUpdate;
	}

	public void setBlIsUpdate(Boolean blIsUpdate) {
		this.blIsUpdate = blIsUpdate;
	}

	public Boolean getBlIsApprove() {
		return blIsApprove;
	}

	public void setBlIsApprove(Boolean blIsApprove) {
		this.blIsApprove = blIsApprove;
	}
	

	public Boolean getBlIsAll() {
		return blIsAll;
	}

	public void setBlIsAll(Boolean blIsAll) {
		this.blIsAll = blIsAll;
	}

	public CfgTblUser getCfgTblUser() {
		return cfgTblUser;
	}

	public void setCfgTblUser(CfgTblUser cfgTblUser) {
		this.cfgTblUser = cfgTblUser;
	}

	public Boolean getBlIsEnabled() {
		return blIsEnabled;
	}

	public void setBlIsEnabled(Boolean blIsEnabled) {
		this.blIsEnabled = blIsEnabled;
	}

}