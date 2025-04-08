package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDivision;

public interface ICfgTblDivisionDAO {

	List<CfgTblDivision> getAllDivision();

	List<CfgTblDivision> getActiveDivision();

	List<CfgTblDivision> getDivisionByProperty(String property, String value, String mode, String oldValue);

	String addNewDivision(CfgTblDivision cfgTblDivision);

	String deleteDivision(List<String> customerId);

	String updateDivision(CfgTblDivision cfgTblDivision);

	String generateDivisionNo(String type);

	String getDivisionById(String customerId);
	
	List<CfgTblDivision> searchDivision(CfgTblDivision country);
}
