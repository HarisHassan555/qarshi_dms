package com.bezkoder.spring.login.admin.dal.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_menu database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_menu")
@NamedQuery(name="CfgTblMenu.findAll", query="SELECT c FROM CfgTblMenu c")
public class CfgTblMenu implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_menu_id")
	private Integer serMenuId;

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

	@Column(name="txt_menu_icons")
	private String txtMenuIcons;

	@Column(name="txt_menu_name")
	private String txtMenuName;

	//bi-directional many-to-one association to CfgTblSubMenu
//	@OneToMany(mappedBy="cfgTblMenu")
	
	@OneToMany(mappedBy="cfgTblMenu",fetch=FetchType.EAGER)
//	@OrderBy("subMenuOrder")
	@JsonManagedReference
	private List<CfgTblSubMenu> cfgTblSubMenus;

	public CfgTblMenu() {
	}

	public Integer getSerMenuId() {
		return this.serMenuId;
	}

	public void setSerMenuId(Integer serMenuId) {
		this.serMenuId = serMenuId;
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

	public String getTxtMenuIcons() {
		return this.txtMenuIcons;
	}

	public void setTxtMenuIcons(String txtMenuIcons) {
		this.txtMenuIcons = txtMenuIcons;
	}

	public String getTxtMenuName() {
		return this.txtMenuName;
	}

	public void setTxtMenuName(String txtMenuName) {
		this.txtMenuName = txtMenuName;
	}

	public List<CfgTblSubMenu> getCfgTblSubMenus() {
		return this.cfgTblSubMenus;
	}

	public void setCfgTblSubMenus(List<CfgTblSubMenu> cfgTblSubMenus) {
		this.cfgTblSubMenus = cfgTblSubMenus;
	}

	public CfgTblSubMenu addCfgTblSubMenus(CfgTblSubMenu cfgTblSubMenus) {
		getCfgTblSubMenus().add(cfgTblSubMenus);
		cfgTblSubMenus.setCfgTblMenu(this);

		return cfgTblSubMenus;
	}

	public CfgTblSubMenu removeCfgTblSubMenus(CfgTblSubMenu cfgTblSubMenus) {
		getCfgTblSubMenus().remove(cfgTblSubMenus);
		cfgTblSubMenus.setCfgTblMenu(null);

		return cfgTblSubMenus;
	}
	
	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

}