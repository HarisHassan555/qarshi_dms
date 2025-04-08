package com.bezkoder.spring.login.sa.bll.dto;
import java.util.List;

public class NavigationMenuRoles {

	private String menuName;
	private String menuIcon;
	private List<SubMenuDTO> subMenus;

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

	public List<SubMenuDTO> getSubMenus() {
		return subMenus;
	}

	public void setSubMenus(List<SubMenuDTO> subMenus) {
		this.subMenus = subMenus;
	}

}
