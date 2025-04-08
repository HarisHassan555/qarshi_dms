package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblDivision;


public interface IDivisionService {

	List<CfgTblDivision> getAllDivision();
	
	List<CfgTblDivision> getActiveDivision();
	
	String addNewDivision(CfgTblDivision cfgTblDivision);

	boolean divisionExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDivision(List<String> divisionsId);

	String updateDivision(CfgTblDivision cfgTblDivision);
	
	String generateDivisionNo(String type);
	
	List<CfgTblDivision> searchDivision(CfgTblDivision division);
	

}
