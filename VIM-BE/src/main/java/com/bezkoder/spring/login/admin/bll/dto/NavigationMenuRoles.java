package com.bezkoder.spring.login.admin.bll.dto;

import java.util.TreeMap;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;


public class NavigationMenuRoles {

	private String menuName;
	private String menuIcon;
	private String menuRoles;
	private TreeMap<CfgTblSubMenu, String> subMenuRoles = new TreeMap<CfgTblSubMenu, String>();
	
	
	public String getMenuName() {
		return menuName;
	}
	public void setMenuName(String menuName) {
		this.menuName = menuName;
	}
	public String getMenuIcon() {
		return menuIcon;
	}
	public void setMenuIcon(String menuIcon) {
		this.menuIcon = menuIcon;
	}
	public String getMenuRoles() {
		return menuRoles;
	}
	public void setMenuRoles(String menuRoles) {
		this.menuRoles = menuRoles;
	}
	public TreeMap<CfgTblSubMenu, String> getSubMenuRoles() {
		return subMenuRoles;
	}
	public void setSubMenuRoles(TreeMap<CfgTblSubMenu, String> subMenuRoles) {
		this.subMenuRoles = subMenuRoles;
	}
	
}
