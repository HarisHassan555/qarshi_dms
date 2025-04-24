package com.bezkoder.spring.login.admin.dal.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.io.Serializable;




import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_sub_menu database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_sub_menu")
@NamedQuery(name="CfgTblSubMenu.findAll", query="SELECT c FROM CfgTblSubMenu c")
public class CfgTblSubMenu implements Serializable,Comparable<CfgTblSubMenu> {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_sub_menu_id")
	private Integer serSubMenuId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="int_sub_menu_order")
	private Integer intSubMenuOrder;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="txt_sub_menu_name")
	private String txtSubMenuName;

	@Column(name="txt_sub_menu_url")
	private String txtSubMenuUrl;

	//bi-directional many-to-one association to CfgTblMenu
	@ManyToOne (fetch=FetchType.LAZY)
	@JoinColumn(name="ser_menu_id")
	@JsonBackReference
	private CfgTblMenu cfgTblMenu;

	//bi-directional many-to-one association to CfgTblSubMenuRole
	/*@OneToMany(mappedBy="cfgTblSubMenu")
	private List<CfgTblSubMenuRole> cfgTblSubMenuRoles;*/
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(
		name="cfg_tbl_sub_menu_role" 
		, joinColumns={
			@JoinColumn(name="ser_sub_menu_id")
			}
		, inverseJoinColumns={
			@JoinColumn(name="ser_role_id")
			}
		)
	private List<CfgTblRole> cfgTblRole;

	public List<CfgTblRole> getCfgTblRole() {
		return cfgTblRole;
	}

	public void setCfgTblRole(List<CfgTblRole> cfgTblRole) {
		this.cfgTblRole = cfgTblRole;
	}

	public CfgTblSubMenu() {
	}

	public Integer getSerSubMenuId() {
		return this.serSubMenuId;
	}

	public void setSerSubMenuId(Integer serSubMenuId) {
		this.serSubMenuId = serSubMenuId;
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

	public Integer getIntSubMenuOrder() {
		return this.intSubMenuOrder;
	}

	public void setIntSubMenuOrder(Integer intSubMenuOrder) {
		this.intSubMenuOrder = intSubMenuOrder;
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

	public String getTxtSubMenuName() {
		return this.txtSubMenuName;
	}

	public void setTxtSubMenuName(String txtSubMenuName) {
		this.txtSubMenuName = txtSubMenuName;
	}

	public String getTxtSubMenuUrl() {
		return this.txtSubMenuUrl;
	}

	public void setTxtSubMenuUrl(String txtSubMenuUrl) {
		this.txtSubMenuUrl = txtSubMenuUrl;
	}

	public CfgTblMenu getCfgTblMenu() {
		return this.cfgTblMenu;
	}

	public void setCfgTblMenu(CfgTblMenu cfgTblMenu) {
		this.cfgTblMenu = cfgTblMenu;
	}

	
	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	
	
	@Column(name="bl_is_view")
	private Boolean blIsview;
	
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


	@Column(name="bl_is_add")
	private Boolean blIsAdd;
	
	@Column(name="bl_is_delete")
	private Boolean blIsDelete;

	@Column(name="bl_is_update")
	private Boolean blIsUpdate;
	
	@Column(name="bl_is_approve")
	private Boolean blIsApprove;
	

	@Override
	public int compareTo(CfgTblSubMenu subMenu) {
			if(subMenu.getIntSubMenuOrder()>this.intSubMenuOrder){
				return -1;
			}
			else if(subMenu.getIntSubMenuOrder()<this.intSubMenuOrder){
				return 1;
			}
		return 0;
	}

	private Integer serSubMenuRoleId;

	public Integer getSerSubMenuRoleId() {
		return serSubMenuRoleId;
	}

	public void setSerSubMenuRoleId(Integer serSubMenuRoleId) {
		this.serSubMenuRoleId = serSubMenuRoleId;
	}
	
}