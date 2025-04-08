package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IProviderAuthenticationService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderAuthenticationDAO;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;


@Service
public class ProviderAuthenticationService implements IProviderAuthenticationService {
	
	@Autowired
	private IProviderAuthenticationDAO citTableProviderAuthenticationDAO;

	private Logger logger = LogManager.getLogger(ProviderAuthenticationService.class);

	public ProviderAuthenticationService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ProviderAuthentication> getAllProviderAuthentication() {
		logger.debug("getAllProviderAuthentications()");
		List<ProviderAuthentication> providerAuthentications = citTableProviderAuthenticationDAO.getAllProviderAuthentication();
		return providerAuthentications;
	}
	
	@Override
	public List<ProviderAuthentication> getActiveProviderAuthentication() {
		logger.debug("getActiveProviderAuthentications()");
		List<ProviderAuthentication> providerAuthentications = citTableProviderAuthenticationDAO.getActiveProviderAuthentication();
		return providerAuthentications;
	}
	
	@Override
	public String generateProviderAuthenticationNo(String type) {
		
		return citTableProviderAuthenticationDAO.generateProviderAuthenticationNo(type);
		
	}
	
	@Override
	public boolean getProviderAuthenticationByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProviderAuthenticationDAO.getProviderAuthenticationByProperty(property, value,mode,oldValue).isEmpty();
	}

	@Override
	public String updateProviderAuthentication(ProviderAuthentication providerAuthentication) {
		
		return citTableProviderAuthenticationDAO.updateProviderAuthentication(providerAuthentication);
	}

	@Override
	public String deleteProviderAuthentication(List<String> providerAuthenticationsId) {
		// TODO Auto-generated method stub
		return citTableProviderAuthenticationDAO.deleteProviderAuthentication(providerAuthenticationsId);
	}
	
	@Override
	public List<ProviderAuthentication> searchProviderAuthentication(ProviderAuthentication providerAuthentication) {
		// TODO Auto-generated method stub
		return citTableProviderAuthenticationDAO.searchProviderAuthentication(providerAuthentication);
	}
	
	@Override
	public String addNewProviderAuthenitication(ProviderAuthentication providerAuthentication) {
				
		return citTableProviderAuthenticationDAO.addNewProviderAuthenitication(providerAuthentication);
	}
//	ProviderAuthentication

}
