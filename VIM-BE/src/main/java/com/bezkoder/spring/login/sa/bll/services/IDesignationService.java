package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.HrTblDesignation;


public interface IDesignationService {

	List<HrTblDesignation> getAllDesignations();
	
	List<HrTblDesignation> getActiveDesignations();
	
	String addNewDesignation(HrTblDesignation cfgTblDesignation);

	boolean DesignationExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDesignations(List<String> DesignationsId);

	String updateDesignation(HrTblDesignation cfgTblDesignation);
	
	String generateDesignationNo(String type);
	
	List<HrTblDesignation> searchDesignation(HrTblDesignation designation);
	

}
