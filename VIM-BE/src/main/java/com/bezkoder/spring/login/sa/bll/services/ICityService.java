package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;


public interface ICityService {

	List<CfgTblCity> getAllCity();
	
	List<CfgTblCity> getActiveCity();
	
	String addNewCity(CfgTblCity cfgTblCity);

	boolean getCityByProperty(String property, String value, String mode, String oldValue);
	
	String deleteCity(List<String> citysId);

	String updateCity(CfgTblCity cfgTblCity);
	
	String generateCityNo(String type);
	
	List<CfgTblCity> searchCity(CfgTblCity city);
	

}
