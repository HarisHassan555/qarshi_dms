package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCountry;


public interface ICountryService {

	List<CfgTblCountry> getAllCountry();
	
	List<CfgTblCountry> getActiveCountry();
	
	String addNewCountry(CfgTblCountry cfgTblCountry);

	boolean countryExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteCountry(List<String> countrysId);

	String updateCountry(CfgTblCountry cfgTblCountry);
	
	String generateCountryNo(String type);
	
	List<CfgTblCountry> searchCountry(CfgTblCountry country);
	

}
