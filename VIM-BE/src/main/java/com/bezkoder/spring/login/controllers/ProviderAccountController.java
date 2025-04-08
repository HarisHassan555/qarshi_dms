package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.IProviderAccountService;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAccount;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ProviderAccountController {

	private Logger logger = LogManager.getLogger(ProviderAccountController.class);

	@Autowired
	private IProviderAccountService providerAccountService;

	@RequestMapping(value = "/getAllProviderAccount", method = RequestMethod.GET)
	public List<ProviderAccount> getAllProviderAccountAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProviderAccountes()");
		List<ProviderAccount> providerAccounts = providerAccountService.getAllProviderAccount();
		return providerAccounts;
	}

	@RequestMapping(value = "/getActiveProviderAccount", method = RequestMethod.GET)
	public List<ProviderAccount> getActiveProviderAccount(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveProviderAccount()");
		List<ProviderAccount> providerAccounts = providerAccountService.getActiveProviderAccount();
		return providerAccounts;
	}

	@RequestMapping(value = "/generateProviderAccountNo", method = RequestMethod.GET)
	public String generateProviderAccountNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return providerAccountService.generateProviderAccountNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewProviderAccount", method = RequestMethod.GET)
	public ProviderAccount getNewProviderAccountAction(HttpServletRequest request, HttpServletResponse response) {
		ProviderAccount providerAccount = new ProviderAccount();
		return providerAccount;
	}

	@RequestMapping(value = "/addNewProviderAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProviderAccountAction(@RequestBody ProviderAccount citTblProviderAccount, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return providerAccountService.addNewProviderAccount(citTblProviderAccount);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
/*	@RequestMapping(value = "/addNewProviderAuthenitication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProviderAuthenitication(@RequestBody ProviderAuthentication citTblProviderAccount, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return providerAccountService.addNewProviderAuthenitication(citTblProviderAccount);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}*/

	@RequestMapping(value = "/deleteProviderAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteProviderAccountAction(@RequestBody String providerAccountesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : providerAccountesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return providerAccountService.deleteProviderAccount(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateProviderAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateProviderAccountAction(@RequestBody ProviderAccount citTblProviderAccount, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return providerAccountService.updateProviderAccount(citTblProviderAccount);
		} catch (Exception ex) {
			return "Failure";
		}
	}


}
