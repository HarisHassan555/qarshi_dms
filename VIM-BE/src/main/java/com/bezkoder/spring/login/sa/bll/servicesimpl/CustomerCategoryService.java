package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ICustomerCategoryService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomerCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomerCategory;


@Service
public class CustomerCategoryService implements ICustomerCategoryService {
	
	@Autowired
	private ICfgTblCustomerCategoryDAO citTableCustomerCategoryDAO;

	private Logger logger = LogManager.getLogger(CustomerCategoryService.class);

	public CustomerCategoryService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblCustomerCategory> getAllCustomerCategory() {
		logger.debug("getAllCustomerCategorys()");
		List<CfgTblCustomerCategory> citys = citTableCustomerCategoryDAO.getAllCustomerCategory();
		return citys;
	}
	
	@Override
	public List<CfgTblCustomerCategory> getActiveCustomerCategory() {
		logger.debug("getActiveCustomerCategorys()");
		List<CfgTblCustomerCategory> citys = citTableCustomerCategoryDAO.getActiveCustomerCategory();
		return citys;
	}
	
	@Override
	public String generateCustomerCategoryNo(String type) {
		
		return citTableCustomerCategoryDAO.generateCustomerCategoryNo(type);
		
	}
	
	@Override
	public boolean getCustomerCategoryByProperty(String property, String value,String mode, String oldValue) {
		return !citTableCustomerCategoryDAO.getCustomerCategoryByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory) {
				
		return citTableCustomerCategoryDAO.addNewCustomerCategory(cfgTblCustomerCategory);
	}

	@Override
	public String updateCustomerCategory(CfgTblCustomerCategory cfgTblCustomerCategory) {
		
		return citTableCustomerCategoryDAO.updateCustomerCategory(cfgTblCustomerCategory);
	}

	@Override
	public String deleteCustomerCategory(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableCustomerCategoryDAO.deleteCustomerCategory(citysId);
	}
	
	@Override
	public List<CfgTblCustomerCategory> searchCustomerCategory(CfgTblCustomerCategory city) {
		// TODO Auto-generated method stub
		return citTableCustomerCategoryDAO.searchCustomerCategory(city);
	}

}
