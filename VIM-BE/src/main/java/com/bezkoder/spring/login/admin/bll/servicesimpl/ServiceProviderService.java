package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IServiceProviderService;
import com.bezkoder.spring.login.admin.dal.dao.IServiceProviderDAO;
import com.bezkoder.spring.login.admin.dal.entities.ServiceProvider;


@Service
public class ServiceProviderService implements IServiceProviderService {
	
	@Autowired
	private IServiceProviderDAO citTableServiceProviderDAO;

	private Logger logger = LogManager.getLogger(ServiceProviderService.class);

	public ServiceProviderService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ServiceProvider> getAllServiceProvider() {
		logger.debug("getAllServiceProviders()");
		List<ServiceProvider> serviceProviders = citTableServiceProviderDAO.getAllServiceProvider();
		return serviceProviders;
	}
	
	@Override
	public List<ServiceProvider> getActiveServiceProvider() {
		logger.debug("getActiveServiceProviders()");
		List<ServiceProvider> serviceProviders = citTableServiceProviderDAO.getActiveServiceProvider();
		return serviceProviders;
	}
	
	@Override
	public String generateServiceProviderNo(String type) {
		
		return citTableServiceProviderDAO.generateServiceProviderNo(type);
		
	}
	
	@Override
	public boolean getServiceProviderByProperty(String property, String value,String mode, String oldValue) {
		return !citTableServiceProviderDAO.getServiceProviderByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewServiceProvider(ServiceProvider serviceProvider) {
				
		return citTableServiceProviderDAO.addNewServiceProvider(serviceProvider);
	}

	@Override
	public String updateServiceProvider(ServiceProvider serviceProvider) {
		
		return citTableServiceProviderDAO.updateServiceProvider(serviceProvider);
	}

	@Override
	public String deleteServiceProvider(List<String> serviceProvidersId) {
		// TODO Auto-generated method stub
		return citTableServiceProviderDAO.deleteServiceProvider(serviceProvidersId);
	}
	
	@Override
	public List<ServiceProvider> searchServiceProvider(ServiceProvider serviceProvider) {
		// TODO Auto-generated method stub
		return citTableServiceProviderDAO.searchServiceProvider(serviceProvider);
	}

}
