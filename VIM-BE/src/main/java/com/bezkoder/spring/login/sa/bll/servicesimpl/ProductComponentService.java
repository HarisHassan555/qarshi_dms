package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IProductComponentService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductComponentDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductComponent;


@Service
public class ProductComponentService implements IProductComponentService {
	
	@Autowired
	private ICfgTblProductComponentDAO citTableProductComponentDAO;

	private Logger logger = LogManager.getLogger(ProductComponentService.class);

	public ProductComponentService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblProductComponent> getAllProductComponent() {
		logger.debug("getAllProductComponents()");
		List<CfgTblProductComponent> productComponents = citTableProductComponentDAO.getAllProductComponent();
		return productComponents;
	}
	
	@Override
	public List<CfgTblProductComponent> getActiveProductComponent() {
		logger.debug("getActiveProductComponents()");
		List<CfgTblProductComponent> productComponents = citTableProductComponentDAO.getActiveProductComponent();
		return productComponents;
	}
	
	@Override
	public String generateProductComponentNo(String type) {
		
		return citTableProductComponentDAO.generateProductComponentNo(type);
		
	}
	
	@Override
	public boolean getProductComponentByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProductComponentDAO.getProductComponentByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProductComponentinList(List<CfgTblProductComponent> lstcfgTblProductComponent) {
				
		return citTableProductComponentDAO.addNewProductComponentinList(lstcfgTblProductComponent);
	}
	
	@Override
	public String addNewProductComponent(CfgTblProductComponent cfgTblProductComponent) {
				
		return citTableProductComponentDAO.addNewProductComponent(cfgTblProductComponent);
	}

	@Override
	public String updateProductComponent(CfgTblProductComponent cfgTblProductComponent) {
		
		return citTableProductComponentDAO.updateProductComponent(cfgTblProductComponent);
	}

	@Override
	public String deleteProductComponent(List<String> productComponentsId) {
		// TODO Auto-generated method stub
		return citTableProductComponentDAO.deleteProductComponent(productComponentsId);
	}
	
	@Override
	public List<CfgTblProductComponent> searchProductComponent(CfgTblProductComponent productComponent) {
		// TODO Auto-generated method stub
		return citTableProductComponentDAO.searchProductComponent(productComponent);
	}

}
