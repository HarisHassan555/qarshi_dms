package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;

public interface IProviderAuthenticationDAO {

	List<ProviderAuthentication> getAllProviderAuthentication();

	List<ProviderAuthentication> getActiveProviderAuthentication();

	List<ProviderAuthentication> getProviderAuthenticationByProperty(String property, String value, String mode, String oldValue);



	String deleteProviderAuthentication(List<String> Id);

	String updateProviderAuthentication(ProviderAuthentication ProviderAuthentication);

	String generateProviderAuthenticationNo(String type);

	String getProviderAuthenticationById(String providerAuthenticationId);
	
	List<ProviderAuthentication> searchProviderAuthentication(ProviderAuthentication providerAuthentication);
	
	String addNewProviderAuthenitication(ProviderAuthentication providerAuthentication);

}
