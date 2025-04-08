package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBrand;

public interface ICfgTblBrandDAO {

	List<CfgTblBrand> getAllBrand();

	List<CfgTblBrand> getActiveBrand();

	List<CfgTblBrand> getBrandByProperty(String property, String value, String mode, String oldValue);

	String addNewBrand(CfgTblBrand cfgTblBrand);

	String deleteBrand(List<String> customerId);

	String updateBrand(CfgTblBrand cfgTblBrand);

	String generateBrandNo(String type);

	String getBrandById(String customerId);
	
	List<CfgTblBrand> searchBrand(CfgTblBrand Brand);
}
