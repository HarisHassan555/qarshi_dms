package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;

public interface ICfgTblSubMenuDAO {

	List<CfgTblSubMenu> getAllSubMenu();

	List<CfgTblSubMenu> getActiveSubMenu();

	List<CfgTblSubMenu> getSubMenuByProperty(String property, String value, String mode, String oldValue);

	String addNewSubMenu(CfgTblSubMenu CfgTblSubMenu);

	String deleteSubMenu(List<String> subMenuId);

	String updateSubMenu(CfgTblSubMenu CfgTblSubMenu);

	String generateSubMenuNo(String type);

	String getSubMenuById(String subMenuId);
	
	List<CfgTblSubMenu> searchSubMenu(CfgTblSubMenu subMenu);
}
