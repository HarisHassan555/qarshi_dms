package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblBrand;


public interface IBrandService {

	List<CfgTblBrand> getAllBrand();
	
	List<CfgTblBrand> getActiveBrand();
	
	String addNewBrand(CfgTblBrand cfgTblBrand);

	boolean getBrandByProperty(String property, String value, String mode, String oldValue);
	
	String deleteBrand(List<String> brandId);

	String updateBrand(CfgTblBrand cfgTblBrand);
	
	String generateBrandNo(String type);
	
	List<CfgTblBrand> searchBrand(CfgTblBrand brand);
	

}
