package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductCategory;


public interface IProductCategoryService {

	List<CfgTblProductCategory> getAllProductCategory();
	
	List<CfgTblProductCategory> getActiveProductCategory();
	
	String addNewProductCategory(CfgTblProductCategory cfgTblProductCategory);

	boolean getProductCategoryByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProductCategory(List<String> productCategoryId);

	String updateProductCategory(CfgTblProductCategory cfgTblProductCategory);
	
	String generateProductCategoryNo(String type);
	
	List<CfgTblProductCategory> searchProductCategory(CfgTblProductCategory productCategory);
	

}
