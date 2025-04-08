package com.bezkoder.spring.login.controllers;/*
package com.bezkoder.spring.login.admin.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.*;

import com.bezkoder.spring.login.admin.bll.services.IAccountAuthenticationService;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;

@RestController
@CrossOrigin( origins = "*" )
public class AccountAuthenticationController {

	private Logger logger = LogManager.getLogger(AccountAuthenticationController.class);

	@Autowired
	private IAccountAuthenticationService accountAuthenticationService;

	@RequestMapping(value = "/getAllAccountAuthentication", method = RequestMethod.GET)
	public List<AccountAuthentication> getAllAccountAuthenticationAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllAccountAuthenticationes()");
		List<AccountAuthentication> accountAuthentications = accountAuthenticationService.getAllAccountAuthentication();
		return accountAuthentications;
	}

	@RequestMapping(value = "/getActiveAccountAuthentication", method = RequestMethod.GET)
	public List<AccountAuthentication> getActiveAccountAuthentication(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveAccountAuthentication()");
		List<AccountAuthentication> accountAuthentications = accountAuthenticationService.getActiveAccountAuthentication();
		return accountAuthentications;
	}

	@RequestMapping(value = "/generateAccountAuthenticationNo", method = RequestMethod.GET)
	public String generateAccountAuthenticationNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return accountAuthenticationService.generateAccountAuthenticationNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewAccountAuthentication", method = RequestMethod.GET)
	public AccountAuthentication getNewAccountAuthenticationAction(HttpServletRequest request, HttpServletResponse response) {
		AccountAuthentication accountAuthentication = new AccountAuthentication();
		return accountAuthentication;
	}

	
	
	@RequestMapping(value = "/addNewAccountAuthenitication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewAccountAuthenitication(@RequestBody AccountAuthentication citTblAccountAuthentication, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return accountAuthenticationService.addNewAccountAuthenitication(citTblAccountAuthentication);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteAccountAuthentication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteAccountAuthenticationAction(@RequestBody String accountAuthenticationesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : accountAuthenticationesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return accountAuthenticationService.deleteAccountAuthentication(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateAccountAuthentication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateAccountAuthenticationAction(@RequestBody AccountAuthentication citTblAccountAuthentication, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return accountAuthenticationService.updateAccountAuthentication(citTblAccountAuthentication);
		} catch (Exception ex) {
			return "Failure";
		}
	}


}
*/
