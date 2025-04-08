package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;

public interface IAccountAuthenticationDAO {

	List<AccountAuthentication> getAllAccountAuthentication();

	List<AccountAuthentication> getActiveAccountAuthentication();

	List<AccountAuthentication> getAccountAuthenticationByProperty(String property, String value, String mode, String oldValue);



	String deleteAccountAuthentication(List<String> Id);

	String updateAccountAuthentication(AccountAuthentication AccountAuthentication);

	String generateAccountAuthenticationNo(String type);

	String getAccountAuthenticationById(String accountAuthenticationId);
	
	List<AccountAuthentication> searchAccountAuthentication(AccountAuthentication accountAuthentication);
	
	String addNewAccountAuthenitication(AccountAuthentication accountAuthentication);

}
