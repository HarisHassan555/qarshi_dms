package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;


public interface IAccountAuthenticationService {

	List<AccountAuthentication> getAllAccountAuthentication();
	
	List<AccountAuthentication> getActiveAccountAuthentication();
	

	boolean getAccountAuthenticationByProperty(String property, String value, String mode, String oldValue);
	
	String deleteAccountAuthentication(List<String> accountAuthenticationsId);

	String updateAccountAuthentication(AccountAuthentication accountAuthentication);
	
	String generateAccountAuthenticationNo(String type);
	
	List<AccountAuthentication> searchAccountAuthentication(AccountAuthentication accountAuthentication);
	
	 String addNewAccountAuthenitication(AccountAuthentication accountAuthentication) ;

}
