package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IBrandService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblBrandDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBrand;


@Service
public class BrandService implements IBrandService {
	
	@Autowired
	private ICfgTblBrandDAO citTableBrandDAO;

	private Logger logger = LogManager.getLogger(BrandService.class);

	public BrandService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblBrand> getAllBrand() {
		logger.debug("getAllBrands()");
		List<CfgTblBrand> citys = citTableBrandDAO.getAllBrand();
		return citys;
	}
	
	@Override
	public List<CfgTblBrand> getActiveBrand() {
		logger.debug("getActiveBrands()");
		List<CfgTblBrand> citys = citTableBrandDAO.getActiveBrand();
		return citys;
	}
	
	@Override
	public String generateBrandNo(String type) {
		
		return citTableBrandDAO.generateBrandNo(type);
		
	}
	
	@Override
	public boolean getBrandByProperty(String property, String value,String mode, String oldValue) {
		return !citTableBrandDAO.getBrandByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewBrand(CfgTblBrand cfgTblBrand) {
				
		return citTableBrandDAO.addNewBrand(cfgTblBrand);
	}

	@Override
	public String updateBrand(CfgTblBrand cfgTblBrand) {
		
		return citTableBrandDAO.updateBrand(cfgTblBrand);
	}

	@Override
	public String deleteBrand(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableBrandDAO.deleteBrand(citysId);
	}
	
	@Override
	public List<CfgTblBrand> searchBrand(CfgTblBrand city) {
		// TODO Auto-generated method stub
		return citTableBrandDAO.searchBrand(city);
	}

}
