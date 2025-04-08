package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ISubMenuRoleService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
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
public class SubMenuRoleController {

	private Logger logger = LogManager.getLogger(SubMenuRoleController.class);

	@Autowired
	private ISubMenuRoleService subMenuRoleService;

	@RequestMapping(value = "/getAllSubMenuRole", method = RequestMethod.GET)
	public List<CfgTblSubMenuRole> getAllSubMenuRoleAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSubMenuRolees()");
		List<CfgTblSubMenuRole> subMenuRoles = subMenuRoleService.getAllSubMenuRole();
		return subMenuRoles;
	}

	@RequestMapping(value = "/getActiveSubMenuRole", method = RequestMethod.GET)
	public List<CfgTblSubMenuRole> getActiveSubMenuRole(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveSubMenuRole()");
		List<CfgTblSubMenuRole> subMenuRoles = subMenuRoleService.getActiveSubMenuRole();
		return subMenuRoles;
	}

	

	@RequestMapping(value = "/getNewSubMenuRole", method = RequestMethod.GET)
	public CfgTblSubMenuRole getNewSubMenuRoleAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblSubMenuRole subMenuRole = new CfgTblSubMenuRole();
		return subMenuRole;
	}

	@RequestMapping(value = "/addNewSubMenuRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewSubMenuRoleAction(@RequestBody CfgTblSubMenuRole citTblSubMenuRole, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return subMenuRoleService.addNewSubMenuRole(citTblSubMenuRole);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/addNewSubMenuRoleinList/{userId}/{roleId}", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewSubMenuRoleinList(@PathVariable("userId") String userId,
										  @PathVariable("roleId") String roleId,@RequestBody List<CfgTblSubMenuRole> lstcfgTblSubMenuRole,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewSubMenuRoleinList()");
		try {
			if(lstcfgTblSubMenuRole!=null)
			System.out.println("----Role List---------"+lstcfgTblSubMenuRole.size());
			for (CfgTblSubMenuRole subMenuRole : lstcfgTblSubMenuRole) {
				CfgTblUser cfgTblUser = new CfgTblUser();
				cfgTblUser.setSerUserId(Integer.parseInt(userId));
				CfgTblRole cfgTblRole = new CfgTblRole();
				cfgTblRole.setSerRoleId(Integer.parseInt(roleId));
				subMenuRole.setCfgTblRole(cfgTblRole);
				subMenuRole.setCfgTblUser(cfgTblUser);
			}
			System.out.println(userId + "" + roleId);
			if(lstcfgTblSubMenuRole!=null && lstcfgTblSubMenuRole.size() >0)
			{

				 subMenuRoleService.addNewSubMenuRoleinList(lstcfgTblSubMenuRole);
			}
			
			return "{\"status\":\"Success\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteSubMenuRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteSubMenuRoleAction(@RequestBody String subMenuRoleesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : subMenuRoleesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return subMenuRoleService.deleteSubMenuRole(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateSubMenuRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSubMenuRoleAction(@RequestBody CfgTblSubMenuRole citTblSubMenuRole, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return subMenuRoleService.updateSubMenuRole(citTblSubMenuRole);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	

}
