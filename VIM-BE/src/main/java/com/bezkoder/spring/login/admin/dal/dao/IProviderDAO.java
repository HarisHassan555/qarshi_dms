package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.Provider;

public interface IProviderDAO {

	List<Provider> getAllProvider();

	List<Provider> getActiveProvider();

	List<Provider> getProviderByProperty(String property, String value, String mode, String oldValue);

	String addNewProvider(Provider Provider);

	String deleteProvider(List<String> customerId);

	String updateProvider(Provider Provider);

	String generateProviderNo(String type);

	String getProviderById(String providerId);
	
	List<Provider> searchProvider(Provider provider);
}
