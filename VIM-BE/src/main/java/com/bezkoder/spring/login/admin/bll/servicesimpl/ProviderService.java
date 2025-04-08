package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IProviderService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderDAO;
import com.bezkoder.spring.login.admin.dal.entities.Provider;


@Service
public class ProviderService implements IProviderService {
	
	@Autowired
	private IProviderDAO citTableProviderDAO;

	private Logger logger = LogManager.getLogger(ProviderService.class);

	public ProviderService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Provider> getAllProvider() {
		logger.debug("getAllProviders()");
		List<Provider> providers = citTableProviderDAO.getAllProvider();
		return providers;
	}
	
	@Override
	public List<Provider> getActiveProvider() {
		logger.debug("getActiveProviders()");
		List<Provider> providers = citTableProviderDAO.getActiveProvider();
		return providers;
	}
	
	@Override
	public String generateProviderNo(String type) {
		
		return citTableProviderDAO.generateProviderNo(type);
		
	}
	
	@Override
	public boolean getProviderByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProviderDAO.getProviderByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProvider(Provider provider) {
				
		return citTableProviderDAO.addNewProvider(provider);
	}

	@Override
	public String updateProvider(Provider provider) {
		
		return citTableProviderDAO.updateProvider(provider);
	}

	@Override
	public String deleteProvider(List<String> providersId) {
		// TODO Auto-generated method stub
		return citTableProviderDAO.deleteProvider(providersId);
	}
	
	@Override
	public List<Provider> searchProvider(Provider provider) {
		// TODO Auto-generated method stub
		return citTableProviderDAO.searchProvider(provider);
	}

}
