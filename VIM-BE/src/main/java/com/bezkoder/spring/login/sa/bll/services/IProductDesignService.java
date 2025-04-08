package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;


public interface IProductDesignService {

	List<CfgTblProductDesign> getAllProductDesign();
	
	List<CfgTblProductDesign> getActiveProductDesign();
	
	String addNewProductDesign(CfgTblProductDesign cfgTblProductDesign);

	boolean getProductDesignByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProductDesign(List<String> productDesignId);

	String updateProductDesign(CfgTblProductDesign cfgTblProductDesign);
	
	String generateProductDesignNo(String type);
	
	List<CfgTblProductDesign> searchProductDesign(CfgTblProductDesign productDesign);
	

}
