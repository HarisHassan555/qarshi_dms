package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.Account;

public interface IAccountDAO {

	List<Account> getAllAccount();

	List<Account> getActiveAccount();

	List<Account> getAccountByProperty(String property, String value, String mode, String oldValue);

	String addNewAccount(Account Account);

	String deleteAccount(List<String> customerId);

	String updateAccount(Account Account);

	String generateAccountNo(String type);

	String getAccountById(String accountId);
	
	List<Account> searchAccount(Account account);
}
