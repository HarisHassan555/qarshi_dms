package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblDefect;


public interface IDefectService {

	List<CfgTblDefect> getAllDefect();
	
	List<CfgTblDefect> getActiveDefect();
	
	String addNewDefect(CfgTblDefect cfgTblDefect);

	boolean defectExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDefect(List<String> defectsId);

	String updateDefect(CfgTblDefect cfgTblDefect);
	
	String generateDefectNo(String type);
	
	List<CfgTblDefect> searchDefect(CfgTblDefect defect);
	

}
