package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.sa.bll.dto.NavigationMenuRoles;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin( origins = "*" )
@RestController
public class MenuController {

	private Logger logger = LogManager.getLogger(MenuController.class);

	@Autowired
	private IMenuService menuService;

	@RequestMapping(value = "/getAllMenu", method = RequestMethod.GET)
	public List<CfgTblMenu> getAllMenuAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllMenues()");
		List<CfgTblMenu> menus = menuService.getAllMenu();
		return menus;
	}

	@RequestMapping(value = "/getActiveMenu", method = RequestMethod.GET)
	public List<CfgTblMenu> getActiveMenu(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveMenu()");
		List<CfgTblMenu> menus = menuService.getActiveMenu();
		return menus;
	}

	

	@RequestMapping(value = "/getNewMenu", method = RequestMethod.GET)
	public CfgTblMenu getNewMenuAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblMenu menu = new CfgTblMenu();
		return menu;
	}

	@RequestMapping(value = "/addNewMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewMenuAction(@RequestBody CfgTblMenu citTblMenu, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return menuService.addNewMenu(citTblMenu);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteMenuAction(@RequestBody String menuesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : menuesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return menuService.deleteMenu(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateMenuAction(@RequestBody CfgTblMenu citTblMenu, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return menuService.updateMenu(citTblMenu);
		} catch (Exception ex) {
			return "Failure";
		}
	}



	@GetMapping("/allMenu")
	public List<NavigationMenuRoles> getAllMenu() throws JsonProcessingException {
		return  menuService.getAllMenuNew();

	}


	@GetMapping("/allSubMenuRole")
	public List<CfgTblSubMenuRole> getAllSubMenuRole(@RequestParam Long roleId,
													 @RequestParam Long userId) throws JsonProcessingException {
		return  menuService.getAllSubMenuRole(roleId,userId);

	}

	/*@RequestMapping(value = "/getMenuBySubmenuId", method = RequestMethod.GET)
	public String getMenuBySubmenuId(@RequestParam Long Id,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllMenues()");
		List<CfgTblMenu> menus = menuService.getAllMenu();
		for (CfgTblMenu menu : menus) {
			for (CfgTblSubMenu subMenu : menu.getCfgTblSubMenus()) {
				if (subMenu.getSerSubMenuId().equals(Id)) {
					return menu.getTxtMenuName();
				}
			}
		}
		return null;
	}*/

	/*@RequestMapping(value = "/getAllSubMenu", method = RequestMethod.GET)
	public List<CfgTblSubMenu> getAllSubMenuAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSubMenues()");
		List<CfgTblSubMenu> subMenus = subMenuService.getAllSubMenu();
		return subMenus;
	}*/
}
