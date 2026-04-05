package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ISubMenuService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblMenuDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin( origins = "*" )
public class SubMenuController {

	private Logger logger = LogManager.getLogger(SubMenuController.class);

	@Autowired
	private ISubMenuService subMenuService;

	@Autowired
	private ICfgTblMenuDAO citTableMenuDAO;


	@RequestMapping(value = "/getAllSubMenu", method = RequestMethod.GET)
	public List<CfgTblSubMenu> getAllSubMenuAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSubMenues()");
		List<CfgTblSubMenu> subMenus = subMenuService.getAllSubMenu();
		return subMenus;
	}

	@RequestMapping(value = "/getActiveSubMenu", method = RequestMethod.GET)
	public List<CfgTblSubMenu> getActiveSubMenu(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveSubMenu()");
		List<CfgTblSubMenu> subMenus = subMenuService.getActiveSubMenu();
		return subMenus;
	}

	

	@RequestMapping(value = "/getNewSubMenu", method = RequestMethod.GET)
	public CfgTblSubMenu getNewSubMenuAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblSubMenu subMenu = new CfgTblSubMenu();
		return subMenu;
	}

	@RequestMapping(value = "/addNewSubMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewSubMenuAction(@RequestBody CfgTblSubMenu citTblSubMenu, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return subMenuService.addNewSubMenu(citTblSubMenu);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteSubMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteSubMenuAction(@RequestBody String subMenuesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : subMenuesId.split(",")) {
				String clean = id.replace("\"", "").trim();
				if (clean.isEmpty()) {
					continue;
				}
				idList.add(clean);
			}
			return subMenuService.deleteSubMenu(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateSubMenu", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSubMenuAction(@RequestBody CfgTblSubMenu citTblSubMenu, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return subMenuService.updateSubMenu(citTblSubMenu);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	@RequestMapping(value = "/getMenuBySubmenuId", method = RequestMethod.GET)
	public String getMenuBySubmenuId(@RequestParam Long Id,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllMenues()");
		List<CfgTblSubMenu> menus = subMenuService.getAllSubMenu();
		for (CfgTblSubMenu menu : menus) {
			if (menu.getSerSubMenuId().equals(Id.intValue())) {
				// Retrieve the menu ID
				Integer menuId = menu.getCfgTblMenu().getSerMenuId();

				// Create an instance of CfgTblMenu
				CfgTblMenu cfgTblMenu = new CfgTblMenu();
				cfgTblMenu.setSerMenuId(menuId);

				// Fetch the menu from the DAO
				List<CfgTblMenu> menuList = citTableMenuDAO.searchMenu(cfgTblMenu);

				// Ensure the list is not empty before accessing the first element
				if (!menuList.isEmpty()) {
					cfgTblMenu = menuList.get(0);
					return cfgTblMenu.getTxtMenuName();
				} else {
					// Handle the case where no menu was found
					return null; // or throw an exception, or return a default value
				}
			}

		}
		return null;
	}
	

}
