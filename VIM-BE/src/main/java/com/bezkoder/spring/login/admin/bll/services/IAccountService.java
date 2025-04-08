package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.Account;


public interface IAccountService {

	List<Account> getAllAccount();
	
	List<Account> getActiveAccount();
	
	String addNewAccount(Account account);

	boolean getAccountByProperty(String property, String value, String mode, String oldValue);
	
	String deleteAccount(List<String> accountsId);

	String updateAccount(Account account);
	
	String generateAccountNo(String type);
	
	List<Account> searchAccount(Account account);
	

}
