package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.ServiceProvider;


public interface IServiceProviderService {

	List<ServiceProvider> getAllServiceProvider();
	
	List<ServiceProvider> getActiveServiceProvider();
	
	String addNewServiceProvider(ServiceProvider serviceProvider);

	boolean getServiceProviderByProperty(String property, String value, String mode, String oldValue);
	
	String deleteServiceProvider(List<String> serviceProvidersId);

	String updateServiceProvider(ServiceProvider serviceProvider);
	
	String generateServiceProviderNo(String type);
	
	List<ServiceProvider> searchServiceProvider(ServiceProvider serviceProvider);
	

}
