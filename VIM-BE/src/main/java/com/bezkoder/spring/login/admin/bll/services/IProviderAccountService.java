package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.ProviderAccount;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;


public interface IProviderAccountService {

	List<ProviderAccount> getAllProviderAccount();
	
	List<ProviderAccount> getActiveProviderAccount();
	
	String addNewProviderAccount(ProviderAccount providerAccount);

	boolean getProviderAccountByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProviderAccount(List<String> providerAccountsId);

	String updateProviderAccount(ProviderAccount providerAccount);
	
	String generateProviderAccountNo(String type);
	
	List<ProviderAccount> searchProviderAccount(ProviderAccount providerAccount);
	
	 String addNewProviderAuthenitication(ProviderAuthentication providerAccount) ;

}
