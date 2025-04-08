package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomerCategory;

public interface ICfgTblCustomerCategoryDAO {

	List<CfgTblCustomerCategory> getAllCustomerCategory();

	List<CfgTblCustomerCategory> getActiveCustomerCategory();

	List<CfgTblCustomerCategory> getCustomerCategoryByProperty(String property, String value, String mode, String oldValue);

	String addNewCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory);

	String deleteCustomerCategory(List<String> customerId);

	String updateCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory);

	String generateCustomerCategoryNo(String type);

	String getCustomerCategoryById(String customerId);
	
	List<CfgTblCustomerCategory> searchCustomerCategory(CfgTblCustomerCategory CustomerCategory);
}
