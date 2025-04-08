package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;


public interface IProductQualityService {

	List<CfgTblProductQuality> getAllProductQuality();
	
	List<CfgTblProductQuality> getActiveProductQuality();
	
	String addNewProductQuality(CfgTblProductQuality cfgTblProductQuality);

	boolean getProductQualityByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProductQuality(List<String> productQualityId);

	String updateProductQuality(CfgTblProductQuality cfgTblProductQuality);
	
	String generateProductQualityNo(String type);
	
	List<CfgTblProductQuality> searchProductQuality(CfgTblProductQuality productQuality);
	

}
