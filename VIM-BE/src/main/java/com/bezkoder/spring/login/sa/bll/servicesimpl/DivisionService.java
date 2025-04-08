package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDivisionService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDivisionDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDivision;


@Service
public class DivisionService implements IDivisionService {
	
	@Autowired
	private ICfgTblDivisionDAO citTableDivisionDAO;

	private Logger logger = LogManager.getLogger(DivisionService.class);

	public DivisionService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblDivision> getAllDivision() {
		logger.debug("getAllDivisions()");
		List<CfgTblDivision> divisions = citTableDivisionDAO.getAllDivision();
		return divisions;
	}
	
	@Override
	public List<CfgTblDivision> getActiveDivision() {
		logger.debug("getActiveDivisions()");
		List<CfgTblDivision> divisions = citTableDivisionDAO.getActiveDivision();
		return divisions;
	}
	
	@Override
	public String generateDivisionNo(String type) {
		
		return citTableDivisionDAO.generateDivisionNo(type);
		
	}
	
	@Override
	public boolean divisionExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableDivisionDAO.getDivisionByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDivision(CfgTblDivision cfgTblDivision) {
				
		return citTableDivisionDAO.addNewDivision(cfgTblDivision);
	}

	@Override
	public String updateDivision(CfgTblDivision cfgTblDivision) {
		
		return citTableDivisionDAO.updateDivision(cfgTblDivision);
	}

	@Override
	public String deleteDivision(List<String> divisionsId) {
		// TODO Auto-generated method stub
		return citTableDivisionDAO.deleteDivision(divisionsId);
	}
	
	@Override
	public List<CfgTblDivision> searchDivision(CfgTblDivision division) {
		// TODO Auto-generated method stub
		return citTableDivisionDAO.searchDivision(division);
	}

}
