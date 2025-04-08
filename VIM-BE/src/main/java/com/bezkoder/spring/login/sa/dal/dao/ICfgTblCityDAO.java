package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;

public interface ICfgTblCityDAO {

	List<CfgTblCity> getAllCity();

	List<CfgTblCity> getActiveCity();

	List<CfgTblCity> getCityByProperty(String property, String value, String mode, String oldValue);

	String addNewCity(CfgTblCity cfgTblCity);

	String deleteCity(List<String> customerId);

	String updateCity(CfgTblCity cfgTblCity);

	String generateCityNo(String type);

	String getCityById(String customerId);
	
	List<CfgTblCity> searchCity(CfgTblCity city);
}
