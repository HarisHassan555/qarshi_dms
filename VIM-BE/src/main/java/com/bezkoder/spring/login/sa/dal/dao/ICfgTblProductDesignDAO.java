package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;

public interface ICfgTblProductDesignDAO {

	List<CfgTblProductDesign> getAllProductDesign();

	List<CfgTblProductDesign> getActiveProductDesign();

	List<CfgTblProductDesign> getProductDesignByProperty(String property, String value, String mode, String oldValue);

	String addNewProductDesign(CfgTblProductDesign cfgTblProductDesign);

	String deleteProductDesign(List<String> customerId);

	String updateProductDesign(CfgTblProductDesign cfgTblProductDesign);

	String generateProductDesignNo(String type);

	String getProductDesignById(String customerId);
	
	List<CfgTblProductDesign> searchProductDesign(CfgTblProductDesign ProductDesign);
}
