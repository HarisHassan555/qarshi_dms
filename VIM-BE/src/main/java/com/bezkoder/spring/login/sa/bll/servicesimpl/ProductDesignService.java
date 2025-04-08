package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IProductDesignService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductDesignDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;


@Service
public class ProductDesignService implements IProductDesignService {
	
	@Autowired
	private ICfgTblProductDesignDAO citTableProductDesignDAO;

	private Logger logger = LogManager.getLogger(ProductDesignService.class);

	public ProductDesignService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblProductDesign> getAllProductDesign() {
		logger.debug("getAllProductDesigns()");
		List<CfgTblProductDesign> productDesigns = citTableProductDesignDAO.getAllProductDesign();
		return productDesigns;
	}
	
	@Override
	public List<CfgTblProductDesign> getActiveProductDesign() {
		logger.debug("getActiveProductDesigns()");
		List<CfgTblProductDesign> productDesigns = citTableProductDesignDAO.getActiveProductDesign();
		return productDesigns;
	}
	
	@Override
	public String generateProductDesignNo(String type) {
		
		return citTableProductDesignDAO.generateProductDesignNo(type);
		
	}
	
	@Override
	public boolean getProductDesignByProperty(String property, String value,String mode, String oldValue) {
		return !citTableProductDesignDAO.getProductDesignByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProductDesign(CfgTblProductDesign cfgTblProductDesign) {
				
		return citTableProductDesignDAO.addNewProductDesign(cfgTblProductDesign);
	}

	@Override
	public String updateProductDesign(CfgTblProductDesign cfgTblProductDesign) {
		
		return citTableProductDesignDAO.updateProductDesign(cfgTblProductDesign);
	}

	@Override
	public String deleteProductDesign(List<String> productDesignsId) {
		// TODO Auto-generated method stub
		return citTableProductDesignDAO.deleteProductDesign(productDesignsId);
	}
	
	@Override
	public List<CfgTblProductDesign> searchProductDesign(CfgTblProductDesign productDesign) {
		// TODO Auto-generated method stub
		return citTableProductDesignDAO.searchProductDesign(productDesign);
	}

}
