package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IAccountAuthenticationService;
import com.bezkoder.spring.login.admin.dal.dao.IAccountAuthenticationDAO;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;


@Service
public class AccountAuthenticationService implements IAccountAuthenticationService {
	
	@Autowired
	private IAccountAuthenticationDAO citTableAccountAuthenticationDAO;

	private Logger logger = LogManager.getLogger(AccountAuthenticationService.class);

	public AccountAuthenticationService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<AccountAuthentication> getAllAccountAuthentication() {
		logger.debug("getAllAccountAuthentications()");
		List<AccountAuthentication> accountAuthentications = citTableAccountAuthenticationDAO.getAllAccountAuthentication();
		return accountAuthentications;
	}
	
	@Override
	public List<AccountAuthentication> getActiveAccountAuthentication() {
		logger.debug("getActiveAccountAuthentications()");
		List<AccountAuthentication> accountAuthentications = citTableAccountAuthenticationDAO.getActiveAccountAuthentication();
		return accountAuthentications;
	}
	
	@Override
	public String generateAccountAuthenticationNo(String type) {
		
		return citTableAccountAuthenticationDAO.generateAccountAuthenticationNo(type);
		
	}
	
	@Override
	public boolean getAccountAuthenticationByProperty(String property, String value,String mode, String oldValue) {
		return !citTableAccountAuthenticationDAO.getAccountAuthenticationByProperty(property, value,mode,oldValue).isEmpty();
	}

	@Override
	public String updateAccountAuthentication(AccountAuthentication accountAuthentication) {
		
		return citTableAccountAuthenticationDAO.updateAccountAuthentication(accountAuthentication);
	}

	@Override
	public String deleteAccountAuthentication(List<String> accountAuthenticationsId) {
		// TODO Auto-generated method stub
		return citTableAccountAuthenticationDAO.deleteAccountAuthentication(accountAuthenticationsId);
	}
	
	@Override
	public List<AccountAuthentication> searchAccountAuthentication(AccountAuthentication accountAuthentication) {
		// TODO Auto-generated method stub
		return citTableAccountAuthenticationDAO.searchAccountAuthentication(accountAuthentication);
	}
	
	@Override
	public String addNewAccountAuthenitication(AccountAuthentication accountAuthentication) {
				
		return citTableAccountAuthenticationDAO.addNewAccountAuthenitication(accountAuthentication);
	}
//	AccountAuthentication

}
