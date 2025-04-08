package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.ISubMenuService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblSubMenuDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;


@Service
public class SubMenuService implements ISubMenuService {
	
	@Autowired
	private ICfgTblSubMenuDAO citTableSubMenuDAO;

	private Logger logger = LogManager.getLogger(SubMenuService.class);

	public SubMenuService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblSubMenu> getAllSubMenu() {
		logger.debug("getAllSubMenus()");
		List<CfgTblSubMenu> subMenus = citTableSubMenuDAO.getAllSubMenu();
		return subMenus;
	}
	
	@Override
	public List<CfgTblSubMenu> getActiveSubMenu() {
		logger.debug("getActiveSubMenus()");
		List<CfgTblSubMenu> subMenus = citTableSubMenuDAO.getActiveSubMenu();
		return subMenus;
	}
	
	@Override
	public String generateSubMenuNo(String type) {
		
		return citTableSubMenuDAO.generateSubMenuNo(type);
		
	}
	
	@Override
	public boolean getSubMenuByProperty(String property, String value,String mode, String oldValue) {
		return !citTableSubMenuDAO.getSubMenuByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewSubMenu(CfgTblSubMenu cfgTblSubMenu) {
				
		return citTableSubMenuDAO.addNewSubMenu(cfgTblSubMenu);
	}

	@Override
	public String updateSubMenu(CfgTblSubMenu cfgTblSubMenu) {
		
		return citTableSubMenuDAO.updateSubMenu(cfgTblSubMenu);
	}

	@Override
	public String deleteSubMenu(List<String> subMenusId) {
		// TODO Auto-generated method stub
		return citTableSubMenuDAO.deleteSubMenu(subMenusId);
	}
	
	@Override
	public List<CfgTblSubMenu> searchSubMenu(CfgTblSubMenu subMenu) {
		// TODO Auto-generated method stub
		return citTableSubMenuDAO.searchSubMenu(subMenu);
	}

}
