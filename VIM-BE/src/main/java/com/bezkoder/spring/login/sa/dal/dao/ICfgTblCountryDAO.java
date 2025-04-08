package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCountry;

public interface ICfgTblCountryDAO {

	List<CfgTblCountry> getAllCountry();

	List<CfgTblCountry> getActiveCountry();

	List<CfgTblCountry> getCountryByProperty(String property, String value, String mode, String oldValue);

	String addNewCountry(CfgTblCountry cfgTblCountry);

	String deleteCountry(List<String> customerId);

	String updateCountry(CfgTblCountry cfgTblCountry);

	String generateCountryNo(String type);

	String getCountryById(String customerId);
	
	List<CfgTblCountry> searchCountry(CfgTblCountry country);
}
