package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IProductCategoryService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductCategory;


@Service
public class ProductCategoryService implements IProductCategoryService {
	
	@Autowired
	private ICfgTblProductCategoryDAO citTableProductCategoryDAO;

	private Logger logger = LogManager.getLogger(ProductCategoryService.class);

	public ProductCategoryService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblProductCategory> getAllProductCategory() {
		logger.debug("getAllProductCategorys()");
		List<CfgTblProductCategory> citys = citTableProductCategoryDAO.getAllProductCategory();
		return citys;
	}
	
	@Override
	public List<CfgTblProductCategory> getActiveProductCategory() {
		logger.debug("getActiveProductCategorys()");
		List<CfgTblProductCategory> citys = citTableProductCategoryDAO.getActiveProductCategory();
		return citys;
	}
	
	@Override
	public String generateProductCategoryNo(String type) {
		
		return citTableProductCategoryDAO.generateProductCategoryNo(type);
		
	}
	
	@Override
	public boolean getProductCategoryByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProductCategoryDAO.getProductCategoryByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProductCategory(CfgTblProductCategory cfgTblProductCategory) {
				
		return citTableProductCategoryDAO.addNewProductCategory(cfgTblProductCategory);
	}

	@Override
	public String updateProductCategory(CfgTblProductCategory cfgTblProductCategory) {
		
		return citTableProductCategoryDAO.updateProductCategory(cfgTblProductCategory);
	}

	@Override
	public String deleteProductCategory(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableProductCategoryDAO.deleteProductCategory(citysId);
	}
	
	@Override
	public List<CfgTblProductCategory> searchProductCategory(CfgTblProductCategory city) {
		// TODO Auto-generated method stub
		return citTableProductCategoryDAO.searchProductCategory(city);
	}

}
