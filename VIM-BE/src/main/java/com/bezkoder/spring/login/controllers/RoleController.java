package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.IRoleService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
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
public class RoleController {

	private Logger logger = LogManager.getLogger(RoleController.class);

	@Autowired
	private IRoleService roleService;

	@RequestMapping(value = "/getAllRole", method = RequestMethod.GET)
	public List<CfgTblRole> getAllRoleAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllRolees()");
		List<CfgTblRole> roles = roleService.getAllRole();
		return roles;
	}

	@RequestMapping(value = "/getActiveRole", method = RequestMethod.GET)
	public List<CfgTblRole> getActiveRole(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveRole()");
		List<CfgTblRole> roles = roleService.getActiveRole();
		return roles;
	}

	@RequestMapping(value = "/generateRoleNo", method = RequestMethod.GET)
	public String generateRoleNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return roleService.generateRoleNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewRole", method = RequestMethod.GET)
	public CfgTblRole getNewRoleAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblRole role = new CfgTblRole();
		return role;
	}

	@RequestMapping(value = "/addNewRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewRoleAction(@RequestBody CfgTblRole citTblRole, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return roleService.addNewRole(citTblRole);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteRoleAction(@RequestBody String roleesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : roleesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return roleService.deleteRole(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateRole", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateRoleAction(@RequestBody CfgTblRole citTblRole, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return roleService.updateRole(citTblRole);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	@RequestMapping(value = "/roleExistByProperty", method = RequestMethod.POST)
	public String roleExistByPropertyAction(@RequestParam String property, @RequestParam String value,
			@RequestParam String mode, @RequestParam String customer, @RequestParam String oldValue,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			return roleService.getRoleByProperty(property, value, mode, oldValue) ? "true" : "false";
			// roleExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}

}
