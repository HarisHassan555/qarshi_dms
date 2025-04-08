package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobCategory;


public interface IJobCategoryService {

	List<CfgTblJobCategory> getAllJobCategory();
	
	List<CfgTblJobCategory> getActiveJobCategory();
	
	String addNewJobCategory(CfgTblJobCategory cfgTblJobCategory);

	boolean jobCategoryExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteJobCategory(List<String> jobCategorysId);

	String updateJobCategory(CfgTblJobCategory cfgTblJobCategory);
	
	String generateJobCategoryNo(String type);
	
	List<CfgTblJobCategory> searchJobCategory(CfgTblJobCategory jobCategory);
	

}
