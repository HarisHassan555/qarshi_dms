package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobType;


public interface IJobTypeService {

	List<CfgTblJobType> getAllJobType();
	
	List<CfgTblJobType> getActiveJobType();
	
	String addNewJobType(CfgTblJobType cfgTblJobType);

	boolean jobTypeExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteJobType(List<String> jobTypesId);

	String updateJobType(CfgTblJobType cfgTblJobType);
	
	String generateJobTypeNo(String type);
	
	List<CfgTblJobType> searchJobType(CfgTblJobType jobType);
	

}
