package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.IPasswordPolicyService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;
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
public class PasswordPolicyController {

	private Logger logger = LogManager.getLogger(PasswordPolicyController.class);

	@Autowired
	private IPasswordPolicyService passwordPolicyService;

	@RequestMapping(value = "/getAllPasswordPolicy", method = RequestMethod.GET)
	public List<CfgTblPasswordPolicy> getAllPasswordPolicyAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllPasswordPolicyes()");
		List<CfgTblPasswordPolicy> passwordPolicys = passwordPolicyService.getAllPasswordPolicy();
		return passwordPolicys;
	}

	@RequestMapping(value = "/getActivePasswordPolicy", method = RequestMethod.GET)
	public List<CfgTblPasswordPolicy> getActivePasswordPolicy(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActivePasswordPolicy()");
		List<CfgTblPasswordPolicy> passwordPolicys = passwordPolicyService.getActivePasswordPolicy();
		return passwordPolicys;
	}

	@RequestMapping(value = "/generatePasswordPolicyNo", method = RequestMethod.GET)
	public String generatePasswordPolicyNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return passwordPolicyService.generatePasswordPolicyNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewPasswordPolicy", method = RequestMethod.GET)
	public CfgTblPasswordPolicy getNewPasswordPolicyAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblPasswordPolicy passwordPolicy = new CfgTblPasswordPolicy();
		return passwordPolicy;
	}

	@RequestMapping(value = "/addNewPasswordPolicy", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewPasswordPolicyAction(@RequestBody CfgTblPasswordPolicy citTblPasswordPolicy, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return passwordPolicyService.addNewPasswordPolicy(citTblPasswordPolicy);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deletePasswordPolicy", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deletePasswordPolicyAction(@RequestBody String passwordPolicyesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : passwordPolicyesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return passwordPolicyService.deletePasswordPolicy(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updatePasswordPolicy", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updatePasswordPolicyAction(@RequestBody CfgTblPasswordPolicy citTblPasswordPolicy, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return passwordPolicyService.updatePasswordPolicy(citTblPasswordPolicy);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	@RequestMapping(value = "/passwordPolicyExistByProperty", method = RequestMethod.POST)
	public String passwordPolicyExistByPropertyAction(@RequestParam String property, @RequestParam String value,
			@RequestParam String mode, @RequestParam String customer, @RequestParam String oldValue,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			return passwordPolicyService.getPasswordPolicyByProperty(property, value, mode, oldValue) ? "true" : "false";
			// passwordPolicyExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}

}
