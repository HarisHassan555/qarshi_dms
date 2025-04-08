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

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.login.admin.bll.services.IAccountService;
import com.bezkoder.spring.login.admin.dal.entities.Account;

@RestController
public class AccountController {

	private Logger logger = LogManager.getLogger(AccountController.class);

	@Autowired
	private IAccountService accountService;

	@RequestMapping(value = "/getAllAccount", method = RequestMethod.GET)
	public List<Account> getAllAccountAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllAccountes()");
		List<Account> accounts = accountService.getAllAccount();
		return accounts;
	}

	@RequestMapping(value = "/getActiveAccount", method = RequestMethod.GET)
	public List<Account> getActiveAccount(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveAccount()");
		List<Account> accounts = accountService.getActiveAccount();
		return accounts;
	}

	@RequestMapping(value = "/generateAccountNo", method = RequestMethod.GET)
	public String generateAccountNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return accountService.generateAccountNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewAccount", method = RequestMethod.GET)
	public Account getNewAccountAction(HttpServletRequest request, HttpServletResponse response) {
		Account account = new Account();
		return account;
	}

	@RequestMapping(value = "/addNewAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewAccountAction(@RequestBody Account citTblAccount, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return accountService.addNewAccount(citTblAccount);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteAccountAction(@RequestBody String accountesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : accountesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return accountService.deleteAccount(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateAccount", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateAccountAction(@RequestBody Account citTblAccount, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return accountService.updateAccount(citTblAccount);
		} catch (Exception ex) {
			return "Failure";
		}
	}


}
*/
