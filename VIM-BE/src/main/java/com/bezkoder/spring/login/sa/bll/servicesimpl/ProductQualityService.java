package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IProductQualityService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductQualityDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;


@Service
public class ProductQualityService implements IProductQualityService {
	
	@Autowired
	private ICfgTblProductQualityDAO cfgTableProductQualityDAO;

	private Logger logger = LogManager.getLogger(ProductQualityService.class);

	public ProductQualityService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblProductQuality> getAllProductQuality() {
		logger.debug("getAllProductQualitys()");
		List<CfgTblProductQuality> cfgys = cfgTableProductQualityDAO.getAllProductQuality();
		return cfgys;
	}
	
	@Override
	public List<CfgTblProductQuality> getActiveProductQuality() {
		logger.debug("getActiveProductQualitys()");
		List<CfgTblProductQuality> cfgys = cfgTableProductQualityDAO.getActiveProductQuality();
		return cfgys;
	}
	
	@Override
	public String generateProductQualityNo(String type) {
		
		return cfgTableProductQualityDAO.generateProductQualityNo(type);
		
	}
	
	@Override
	public boolean getProductQualityByProperty(String property, String value,String mode, String oldValue) {
		return !cfgTableProductQualityDAO.getProductQualityByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewProductQuality(CfgTblProductQuality cfgTblProductQuality) {
				
		return cfgTableProductQualityDAO.addNewProductQuality(cfgTblProductQuality);
	}

	@Override
	public String updateProductQuality(CfgTblProductQuality cfgTblProductQuality) {
		
		return cfgTableProductQualityDAO.updateProductQuality(cfgTblProductQuality);
	}

	@Override
	public String deleteProductQuality(List<String> cfgTableProductQualityId) {
		// TODO Auto-generated method stub
		return cfgTableProductQualityDAO.deleteProductQuality(cfgTableProductQualityId);
	}
	
	@Override
	public List<CfgTblProductQuality> searchProductQuality(CfgTblProductQuality cfgTableProductQuality) {
		// TODO Auto-generated method stub
		return cfgTableProductQualityDAO.searchProductQuality(cfgTableProductQuality);
	}

}
