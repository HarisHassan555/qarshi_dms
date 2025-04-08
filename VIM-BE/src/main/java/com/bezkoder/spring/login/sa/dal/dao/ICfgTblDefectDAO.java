package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDefect;

public interface ICfgTblDefectDAO {

	List<CfgTblDefect> getAllDefect();

	List<CfgTblDefect> getActiveDefect();

	List<CfgTblDefect> getDefectByProperty(String property, String value, String mode, String oldValue);

	String addNewDefect(CfgTblDefect cfgTblDefect);

	String deleteDefect(List<String> customerId);

	String updateDefect(CfgTblDefect cfgTblDefect);

	String generateDefectNo(String type);

	String getDefectById(String defectid);
	
	List<CfgTblDefect> searchDefect(CfgTblDefect defect);
}
