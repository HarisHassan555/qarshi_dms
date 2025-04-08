package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ICountryService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCountryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCountry;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CountryService implements ICountryService {
	
	@Autowired
	private ICfgTblCountryDAO citTableCountryDAO;

	private Logger logger = LogManager.getLogger(CountryService.class);

	public CountryService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblCountry> getAllCountry() {
		logger.debug("getAllCountrys()");
		List<CfgTblCountry> countrys = citTableCountryDAO.getAllCountry();
		return countrys;
	}
	
	@Override
	public List<CfgTblCountry> getActiveCountry() {
		logger.debug("getActiveCountrys()");
		List<CfgTblCountry> countrys = citTableCountryDAO.getActiveCountry();
		return countrys;
	}
	
	@Override
	public String generateCountryNo(String type) {
		
		return citTableCountryDAO.generateCountryNo(type);
		
	}
	
	@Override
	public boolean countryExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableCountryDAO.getCountryByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewCountry(CfgTblCountry cfgTblCountry) {
				
		return citTableCountryDAO.addNewCountry(cfgTblCountry);
	}

	@Override
	public String updateCountry(CfgTblCountry cfgTblCountry) {
		
		return citTableCountryDAO.updateCountry(cfgTblCountry);
	}

	@Override
	public String deleteCountry(List<String> countrysId) {
		// TODO Auto-generated method stub
		return citTableCountryDAO.deleteCountry(countrysId);
	}
	
	@Override
	public List<CfgTblCountry> searchCountry(CfgTblCountry country) {
		// TODO Auto-generated method stub
		return citTableCountryDAO.searchCountry(country);
	}

}
