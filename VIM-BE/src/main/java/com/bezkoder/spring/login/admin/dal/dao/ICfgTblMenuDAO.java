package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;

public interface ICfgTblMenuDAO {

	List<CfgTblMenu> getAllMenu();

	List<CfgTblMenu> getActiveMenu();

	List<CfgTblMenu> getMenuByProperty(String property, String value, String mode, String oldValue);

	String addNewMenu(CfgTblMenu CfgTblMenu);

	String deleteMenu(List<String> menuId);

	String updateMenu(CfgTblMenu CfgTblMenu);

	String generateMenuNo(String type);

	String getMenuById(String menuId);
	
	List<CfgTblMenu> searchMenu(CfgTblMenu menu);

}
