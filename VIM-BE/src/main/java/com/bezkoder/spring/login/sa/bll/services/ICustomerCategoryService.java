package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomerCategory;


public interface ICustomerCategoryService {

	List<CfgTblCustomerCategory> getAllCustomerCategory();
	
	List<CfgTblCustomerCategory> getActiveCustomerCategory();
	
	String addNewCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory);

	boolean getCustomerCategoryByProperty(String property, String value, String mode, String oldValue);
	
	String deleteCustomerCategory(List<String> customerCategoryId);

	String updateCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory);
	
	String generateCustomerCategoryNo(String type);
	
	List<CfgTblCustomerCategory> searchCustomerCategory(CfgTblCustomerCategory customerCategory);
	

}
