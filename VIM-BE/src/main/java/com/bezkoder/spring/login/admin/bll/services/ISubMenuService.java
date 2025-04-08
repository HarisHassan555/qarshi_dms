package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;


public interface ISubMenuService {

	List<CfgTblSubMenu> getAllSubMenu();
	
	List<CfgTblSubMenu> getActiveSubMenu();
	
	String addNewSubMenu(CfgTblSubMenu cfgTblSubMenu);

	boolean getSubMenuByProperty(String property, String value, String mode, String oldValue);
	
	String deleteSubMenu(List<String> subMenusId);

	String updateSubMenu(CfgTblSubMenu cfgTblSubMenu);
	
	String generateSubMenuNo(String type);
	
	List<CfgTblSubMenu> searchSubMenu(CfgTblSubMenu subMenu);
	

}
