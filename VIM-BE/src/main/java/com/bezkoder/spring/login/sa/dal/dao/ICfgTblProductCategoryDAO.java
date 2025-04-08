package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductCategory;

public interface ICfgTblProductCategoryDAO {

	List<CfgTblProductCategory> getAllProductCategory();

	List<CfgTblProductCategory> getActiveProductCategory();

	List<CfgTblProductCategory> getProductCategoryByProperty(String property, String value, String mode, String oldValue);

	String addNewProductCategory(CfgTblProductCategory cfgTblProductCategory);

	String deleteProductCategory(List<String> customerId);

	String updateProductCategory(CfgTblProductCategory cfgTblProductCategory);

	String generateProductCategoryNo(String type);

	String getProductCategoryById(String customerId);
	
	List<CfgTblProductCategory> searchProductCategory(CfgTblProductCategory ProductCategory);
}
