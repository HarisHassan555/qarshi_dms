package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IProductService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;


@Service
public class ProductService implements IProductService {
	
	@Autowired
	private ICfgTblProductDAO citTableProductDAO;

	private Logger logger = LogManager.getLogger(ProductService.class);

	public ProductService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblProduct> getAllProduct() {
		logger.debug("getAllProducts()");
		List<CfgTblProduct> products = citTableProductDAO.getAllProduct();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getActiveProduct() {
		logger.debug("getActiveProducts()");
		List<CfgTblProduct> products = citTableProductDAO.getActiveProduct();
		return products;
	}
	
	@Override
	public String generateProductNo(String type) {
		
		return citTableProductDAO.generateProductNo(type);
		
	}
	
	@Override
	public boolean getProductByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProductDAO.getProductByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProduct(CfgTblProduct cfgTblProduct) {
				
		return citTableProductDAO.addNewProduct(cfgTblProduct);
	}

	@Override
	public String updateProduct(CfgTblProduct cfgTblProduct) {
		
		return citTableProductDAO.updateProduct(cfgTblProduct);
	}

	@Override
	public String deleteProduct(List<String> productId) {
		// TODO Auto-generated method stub
		return citTableProductDAO.deleteProduct(productId);
	}
	
	@Override
	public List<CfgTblProduct> searchProduct(CfgTblProduct product) {
		// TODO Auto-generated method stub
		return citTableProductDAO.searchProduct(product);
	}
	
	@Override
	public List<CfgTblProduct> getAllPacking() {
		logger.debug("getAllPacking()");
		List<CfgTblProduct> products = citTableProductDAO.getAllPacking();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllInventory() {
		logger.debug("getAllInventory()");
		List<CfgTblProduct> products = citTableProductDAO.getAllInventory();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllSalesItem() {
		logger.debug("getAllSalesItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllSalesItem();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllPurchaseItem() {
		logger.debug("getAllPurchaseItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllPurchaseItem();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllImportItem() {
		logger.debug("getAllImportItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllImportItem();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllSetItem() {
		logger.debug("getAllSetItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllSetItem();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllComponentItem() {
		logger.debug("getAllComponentItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllComponentItem();
		return products;
	}
	
	@Override
	public List<CfgTblProduct> getAllProductionItem() {
		logger.debug("getAllComponentItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllProductionItem();
		return products;
	}
	
	
	@Override
	public List<CfgTblProduct> getAllProductionItem(String priceGroup) {
		logger.debug("getAllComponentItem()");
		List<CfgTblProduct> products = citTableProductDAO.getAllProductionItem(priceGroup);
		return products;
	}
	

	
}
