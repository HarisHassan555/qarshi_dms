package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.Provider;


public interface IProviderService {

	List<Provider> getAllProvider();
	
	List<Provider> getActiveProvider();
	
	String addNewProvider(Provider provider);

	boolean getProviderByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProvider(List<String> providersId);

	String updateProvider(Provider provider);
	
	String generateProviderNo(String type);
	
	List<Provider> searchProvider(Provider provider);
	

}
