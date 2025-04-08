package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IProviderAccountService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderAccountDAO;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAccount;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;


@Service
public class ProviderAccountService implements IProviderAccountService {
	
	@Autowired
	private IProviderAccountDAO citTableProviderAccountDAO;

	private Logger logger = LogManager.getLogger(ProviderAccountService.class);

	public ProviderAccountService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ProviderAccount> getAllProviderAccount() {
		logger.debug("getAllProviderAccounts()");
		List<ProviderAccount> providerAccounts = citTableProviderAccountDAO.getAllProviderAccount();
		return providerAccounts;
	}
	
	@Override
	public List<ProviderAccount> getActiveProviderAccount() {
		logger.debug("getActiveProviderAccounts()");
		List<ProviderAccount> providerAccounts = citTableProviderAccountDAO.getActiveProviderAccount();
		return providerAccounts;
	}
	
	@Override
	public String generateProviderAccountNo(String type) {
		
		return citTableProviderAccountDAO.generateProviderAccountNo(type);
		
	}
	
	@Override
	public boolean getProviderAccountByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProviderAccountDAO.getProviderAccountByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProviderAccount(ProviderAccount providerAccount) {
				
		return citTableProviderAccountDAO.addNewProviderAccount(providerAccount);
	}

	@Override
	public String updateProviderAccount(ProviderAccount providerAccount) {
		
		return citTableProviderAccountDAO.updateProviderAccount(providerAccount);
	}

	@Override
	public String deleteProviderAccount(List<String> providerAccountsId) {
		// TODO Auto-generated method stub
		return citTableProviderAccountDAO.deleteProviderAccount(providerAccountsId);
	}
	
	@Override
	public List<ProviderAccount> searchProviderAccount(ProviderAccount providerAccount) {
		// TODO Auto-generated method stub
		return citTableProviderAccountDAO.searchProviderAccount(providerAccount);
	}
	
	@Override
	public String addNewProviderAuthenitication(ProviderAuthentication providerAccount) {
				
		return citTableProviderAccountDAO.addNewProviderAuthenitication(providerAccount);
	}
//	ProviderAuthentication

}
