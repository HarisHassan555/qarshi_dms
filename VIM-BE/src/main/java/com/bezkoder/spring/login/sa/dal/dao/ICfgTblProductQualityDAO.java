package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;

public interface ICfgTblProductQualityDAO {

	List<CfgTblProductQuality> getAllProductQuality();

	List<CfgTblProductQuality> getActiveProductQuality();

	List<CfgTblProductQuality> getProductQualityByProperty(String property, String value, String mode, String oldValue);

	String addNewProductQuality(CfgTblProductQuality cfgTblProductQuality);

	String deleteProductQuality(List<String> qualityId);

	String updateProductQuality(CfgTblProductQuality cfgTblProductQuality);

	String generateProductQualityNo(String type);

	String getProductQualityById(String qualityId);
	
	List<CfgTblProductQuality> searchProductQuality(CfgTblProductQuality ProductQuality);
}
