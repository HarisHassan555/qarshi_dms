package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductComponent;


public interface IProductComponentService {

	List<CfgTblProductComponent> getAllProductComponent();
	
	List<CfgTblProductComponent> getActiveProductComponent();
	
	String addNewProductComponentinList(List<CfgTblProductComponent> lstcfgTblProductComponent);
	
	String addNewProductComponent(CfgTblProductComponent cfgTblProductComponent);

	boolean getProductComponentByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProductComponent(List<String> productComponentId);

	String updateProductComponent(CfgTblProductComponent cfgTblProductComponent);
	
	String generateProductComponentNo(String type);
	
	List<CfgTblProductComponent> searchProductComponent(CfgTblProductComponent productComponent);
	

}
