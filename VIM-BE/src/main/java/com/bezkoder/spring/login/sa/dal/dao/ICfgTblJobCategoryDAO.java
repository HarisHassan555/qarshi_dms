package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobCategory;

public interface ICfgTblJobCategoryDAO {

	List<CfgTblJobCategory> getAllJobCategory();

	List<CfgTblJobCategory> getActiveJobCategory();

	List<CfgTblJobCategory> getJobCategoryByProperty(String property, String value, String mode, String oldValue);

	String addNewJobCategory(CfgTblJobCategory cfgTblJobCategory);

	String deleteJobCategory(List<String> customerId);

	String updateJobCategory(CfgTblJobCategory cfgTblJobCategory);

	String generateJobCategoryNo(String type);

	String getJobCategoryById(String customerId);
	
	List<CfgTblJobCategory> searchJobCategory(CfgTblJobCategory jobCategory);
}
