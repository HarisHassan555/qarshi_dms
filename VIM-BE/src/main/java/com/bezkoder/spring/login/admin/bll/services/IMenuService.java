package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.sa.bll.dto.NavigationMenuRoles;


public interface IMenuService {

	List<CfgTblMenu> getAllMenu();
	
	List<CfgTblMenu> getActiveMenu();

	
	String addNewMenu(CfgTblMenu cfgTblMenu);

	boolean getMenuByProperty(String property, String value, String mode, String oldValue);
	
	String deleteMenu(List<String> menusId);

	String updateMenu(CfgTblMenu cfgTblMenu);
	
	String generateMenuNo(String type);
	
	List<CfgTblMenu> searchMenu(CfgTblMenu menu);

	List<NavigationMenuRoles> getAllMenuNew();

	List<CfgTblSubMenuRole> getAllSubMenuRole(Long roleId, Long userId);

}
