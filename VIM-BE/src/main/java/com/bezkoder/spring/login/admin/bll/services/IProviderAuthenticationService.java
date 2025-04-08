package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;


public interface IProviderAuthenticationService {

	List<ProviderAuthentication> getAllProviderAuthentication();
	
	List<ProviderAuthentication> getActiveProviderAuthentication();
	

	boolean getProviderAuthenticationByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProviderAuthentication(List<String> providerAuthenticationsId);

	String updateProviderAuthentication(ProviderAuthentication providerAuthentication);
	
	String generateProviderAuthenticationNo(String type);
	
	List<ProviderAuthentication> searchProviderAuthentication(ProviderAuthentication providerAuthentication);
	
	 String addNewProviderAuthenitication(ProviderAuthentication providerAuthentication) ;

}
