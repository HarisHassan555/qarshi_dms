package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IAccountService;
import com.bezkoder.spring.login.admin.dal.dao.IAccountDAO;
import com.bezkoder.spring.login.admin.dal.entities.Account;


@Service
public class AccountService implements IAccountService {
	
	@Autowired
	private IAccountDAO citTableAccountDAO;

	private Logger logger = LogManager.getLogger(AccountService.class);

	public AccountService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Account> getAllAccount() {
		logger.debug("getAllAccounts()");
		List<Account> accounts = citTableAccountDAO.getAllAccount();
		return accounts;
	}
	
	@Override
	public List<Account> getActiveAccount() {
		logger.debug("getActiveAccounts()");
		List<Account> accounts = citTableAccountDAO.getActiveAccount();
		return accounts;
	}
	
	@Override
	public String generateAccountNo(String type) {
		
		return citTableAccountDAO.generateAccountNo(type);
		
	}
	
	@Override
	public boolean getAccountByProperty(String property, String value,String mode, String oldValue) {
		return !citTableAccountDAO.getAccountByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewAccount(Account account) {
				
		return citTableAccountDAO.addNewAccount(account);
	}

	@Override
	public String updateAccount(Account account) {
		
		return citTableAccountDAO.updateAccount(account);
	}

	@Override
	public String deleteAccount(List<String> accountsId) {
		// TODO Auto-generated method stub
		return citTableAccountDAO.deleteAccount(accountsId);
	}
	
	@Override
	public List<Account> searchAccount(Account account) {
		// TODO Auto-generated method stub
		return citTableAccountDAO.searchAccount(account);
	}

}
