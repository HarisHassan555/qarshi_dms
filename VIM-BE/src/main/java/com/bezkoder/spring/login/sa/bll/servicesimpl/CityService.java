package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ICityService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCityDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;


@Service
public class CityService implements ICityService {
	
	@Autowired
	private ICfgTblCityDAO citTableCityDAO;

	private Logger logger = LogManager.getLogger(CityService.class);

	public CityService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblCity> getAllCity() {
		logger.debug("getAllCitys()");
		List<CfgTblCity> citys = citTableCityDAO.getAllCity();
		return citys;
	}
	
	@Override
	public List<CfgTblCity> getActiveCity() {
		logger.debug("getActiveCitys()");
		List<CfgTblCity> citys = citTableCityDAO.getActiveCity();
		return citys;
	}
	
	@Override
	public String generateCityNo(String type) {
		
		return citTableCityDAO.generateCityNo(type);
		
	}
	
	@Override
	public boolean getCityByProperty(String property, String value,String mode, String oldValue) {
		return !citTableCityDAO.getCityByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewCity(CfgTblCity cfgTblCity) {
				
		return citTableCityDAO.addNewCity(cfgTblCity);
	}

	@Override
	public String updateCity(CfgTblCity cfgTblCity) {
		
		return citTableCityDAO.updateCity(cfgTblCity);
	}

	@Override
	public String deleteCity(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableCityDAO.deleteCity(citysId);
	}
	
	@Override
	public List<CfgTblCity> searchCity(CfgTblCity city) {
		// TODO Auto-generated method stub
		return citTableCityDAO.searchCity(city);
	}

}
