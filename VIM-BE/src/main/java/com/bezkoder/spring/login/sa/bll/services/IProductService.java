package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;


public interface IProductService {

	List<CfgTblProduct> getAllProduct();
	
	List<CfgTblProduct> getActiveProduct();
	
	String addNewProduct(CfgTblProduct cfgTblProduct);

	boolean getProductByProperty(String property, String value, String mode, String oldValue);
	
	String deleteProduct(List<String> productId);

	String updateProduct(CfgTblProduct cfgTblProduct);
	
	String generateProductNo(String type);
	
	List<CfgTblProduct> searchProduct(CfgTblProduct product);
	
    List<CfgTblProduct> getAllPacking();
	
	List<CfgTblProduct> getAllInventory();
	
	List<CfgTblProduct> getAllSalesItem();
	
	List<CfgTblProduct> getAllPurchaseItem();
	
	List<CfgTblProduct> getAllImportItem();
	
	List<CfgTblProduct> getAllSetItem();
	
	List<CfgTblProduct> getAllComponentItem();
	
	 List<CfgTblProduct> getAllProductionItem();
	 
	 List<CfgTblProduct> getAllProductionItem(String priceGroup);
	

}
