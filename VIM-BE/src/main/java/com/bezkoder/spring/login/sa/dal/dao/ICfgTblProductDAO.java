package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.admin.bll.dto.SPDTO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;

public interface ICfgTblProductDAO {

	List<CfgTblProduct> getAllProduct();

	List<CfgTblProduct> getActiveProduct();

	List<CfgTblProduct> getProductByProperty(String property, String value, String mode, String oldValue);

	String addNewProduct(CfgTblProduct cfgTblProduct);

	String deleteProduct(List<String> product);

	String updateProduct(CfgTblProduct cfgTblProduct);

	String generateProductNo(String type);

	String getProductById(String product);
	
	List<CfgTblProduct> searchProduct(CfgTblProduct Product);
	
	List<CfgTblProduct> getAllPacking();
	
	List<CfgTblProduct> getAllInventory();
	
	List<CfgTblProduct> getAllSalesItem();
	
	List<CfgTblProduct> getAllPurchaseItem();
	
	List<CfgTblProduct> getAllImportItem();
	
	List<CfgTblProduct> getAllSetItem();
	
	List<CfgTblProduct> getAllComponentItem();
	
	 List<CfgTblProduct> getAllProductionItem();
	 
	 List<CfgTblProduct> getAllProductionItem(String priceGroup);
	
	 List<CfgTblProduct> getAllSpareParts();
	 
	 List<SPDTO> searchProductSP(CfgTblProduct Product);
}
