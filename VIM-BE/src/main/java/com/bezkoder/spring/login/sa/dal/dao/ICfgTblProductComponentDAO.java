package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductComponent;

public interface ICfgTblProductComponentDAO {

	List<CfgTblProductComponent> getAllProductComponent();

	List<CfgTblProductComponent> getActiveProductComponent();

	List<CfgTblProductComponent> getProductComponentByProperty(String property, String value, String mode, String oldValue);
	
	String addNewProductComponentinList(List<CfgTblProductComponent> lstcfgTblProductComponent);

	String addNewProductComponent(CfgTblProductComponent cfgTblProductComponent);

	String deleteProductComponent(List<String> productComponentId);

	String updateProductComponent(CfgTblProductComponent cfgTblProductComponent);

	String generateProductComponentNo(String type);

	String getProductComponentById(String productComponentId);
	
	List<CfgTblProductComponent> searchProductComponent(CfgTblProductComponent productComponent);
}
