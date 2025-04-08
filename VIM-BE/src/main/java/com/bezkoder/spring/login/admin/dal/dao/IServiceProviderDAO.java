package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.ServiceProvider;

public interface IServiceProviderDAO {

	List<ServiceProvider> getAllServiceProvider();

	List<ServiceProvider> getActiveServiceProvider();

	List<ServiceProvider> getServiceProviderByProperty(String property, String value, String mode, String oldValue);

	String addNewServiceProvider(ServiceProvider ServiceProvider);

	String deleteServiceProvider(List<String> customerId);

	String updateServiceProvider(ServiceProvider ServiceProvider);

	String generateServiceProviderNo(String type);

	String getServiceProviderById(String customerId);
	
	List<ServiceProvider> searchServiceProvider(ServiceProvider serviceProvider);
}
