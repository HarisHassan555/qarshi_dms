package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.bezkoder.spring.login.admin.dal.entities.*;
import com.bezkoder.spring.login.sa.bll.dto.NavigationMenuRoles;
import com.bezkoder.spring.login.sa.bll.dto.SubMenuDTO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSubMenuRoleRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblMenuDAO;

import javax.persistence.*;;


@Service
public class MenuService implements IMenuService {
	
	@Autowired
	private ICfgTblMenuDAO citTableMenuDAO;

	private Logger logger = LogManager.getLogger(MenuService.class);

	@Autowired
	private CfgTblSubMenuRoleRepository cfgTblSubMenuRoleRepository;

	public MenuService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblMenu> getAllMenu() {
		logger.debug("getAllMenus()");
		List<CfgTblMenu> menus = citTableMenuDAO.getAllMenu();
		return menus;
	}
	
	@Override
	public List<CfgTblMenu> getActiveMenu() {
		logger.debug("getActiveMenus()");
		List<CfgTblMenu> menus = citTableMenuDAO.getActiveMenu();
		return menus;
	}
	
	@Override
	public String generateMenuNo(String type) {
		
		return citTableMenuDAO.generateMenuNo(type);
		
	}
	
	@Override
	public boolean getMenuByProperty(String property, String value,String mode, String oldValue) {
		return !citTableMenuDAO.getMenuByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewMenu(CfgTblMenu cfgTblMenu) {
				
		return citTableMenuDAO.addNewMenu(cfgTblMenu);
	}

	@Override
	public String updateMenu(CfgTblMenu cfgTblMenu) {
		
		return citTableMenuDAO.updateMenu(cfgTblMenu);
	}

	@Override
	public String deleteMenu(List<String> menusId) {
		// TODO Auto-generated method stub
		return citTableMenuDAO.deleteMenu(menusId);
	}
	
	@Override
	public List<CfgTblMenu> searchMenu(CfgTblMenu menu) {
		// TODO Auto-generated method stub
		return citTableMenuDAO.searchMenu(menu);
	}

	@Override
	public List<NavigationMenuRoles> getAllMenuNew() {
		/*List<CfgTblMenu> menus = citTableMenuDAO.getAllMenu();*/
		return populateNavigationMenus();

	}

	public List<NavigationMenuRoles> populateNavigationMenus() {
		List<CfgTblMenu> menuList = citTableMenuDAO.getAllMenu();
		List<NavigationMenuRoles> navigationMenuDTOs = new ArrayList<>();

		for (CfgTblMenu menu : menuList) {
			NavigationMenuRoles navMenuDTO = new NavigationMenuRoles();
			navMenuDTO.setMenuName(menu.getTxtMenuName());
			navMenuDTO.setMenuIcon(menu.getTxtMenuIcons());
			List<SubMenuDTO> subMenuDTOs = new ArrayList<>();
			for (CfgTblSubMenu subMenu : menu.getCfgTblSubMenus()) {
				SubMenuDTO subMenuDTO = new SubMenuDTO();
				subMenuDTO.setSubMenuId(subMenu.getSerSubMenuId());
				subMenuDTO.setSubMenuName(subMenu.getTxtSubMenuName());
				subMenuDTO.setSubMenuAction(subMenu.getTxtSubMenuUrl());
				subMenuDTO.setRoles(getRolesString(subMenu.getCfgTblRole()));
				subMenuDTO.setSubmenuOrder(subMenu.getIntSubMenuOrder());
				subMenuDTOs.add(subMenuDTO);
			}
			navMenuDTO.setSubMenus(subMenuDTOs);
			navigationMenuDTOs.add(navMenuDTO);
		}

		return navigationMenuDTOs;
	}

	private String getRolesString(List<CfgTblRole> roles) {
		return roles.stream()
				.map(role -> "ROLE_" + role.getTxtRoleName().toUpperCase())
				.collect(Collectors.joining(","));
	}


	@Override
	public List<CfgTblSubMenuRole> getAllSubMenuRole(Long roleId,Long userId) {

		CfgTblRole cfgTblRole = new CfgTblRole();
		cfgTblRole.setSerRoleId(Math.toIntExact(roleId));
		CfgTblUser cfgTblUser = new CfgTblUser();
		cfgTblUser.setSerUserId(Math.toIntExact(userId));
		return cfgTblSubMenuRoleRepository.findCfgTblSubMenuRoleByCfgTblRoleAndCfgTblUser(cfgTblRole,cfgTblUser);
	}



}
