package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAccount;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;

public interface IProviderAccountDAO {

	List<ProviderAccount> getAllProviderAccount();

	List<ProviderAccount> getActiveProviderAccount();

	List<ProviderAccount> getProviderAccountByProperty(String property, String value, String mode, String oldValue);

	String addNewProviderAccount(ProviderAccount ProviderAccount);

	String deleteProviderAccount(List<String> customerId);

	String updateProviderAccount(ProviderAccount ProviderAccount);

	String generateProviderAccountNo(String type);

	String getProviderAccountById(String providerAccountId);
	
	List<ProviderAccount> searchProviderAccount(ProviderAccount providerAccount);
	
	String addNewProviderAuthenitication(ProviderAuthentication providerAccount);

}
