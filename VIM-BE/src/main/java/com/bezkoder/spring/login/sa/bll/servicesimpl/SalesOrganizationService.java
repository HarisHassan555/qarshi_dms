package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ISalesOrganizationService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblSalesOrganizationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSalesOrganization;


@Service
public class SalesOrganizationService implements ISalesOrganizationService {
	
	@Autowired
	private ICfgTblSalesOrganizationDAO citTableSalesOrganizationDAO;

	private Logger logger = LogManager.getLogger(SalesOrganizationService.class);

	public SalesOrganizationService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblSalesOrganization> getAllSalesOrganization() {
		logger.debug("getAllSalesOrganizations()");
		List<CfgTblSalesOrganization> salesOrganizations = citTableSalesOrganizationDAO.getAllSalesOrganization();
		return salesOrganizations;
	}
	
	@Override
	public List<CfgTblSalesOrganization> getActiveSalesOrganization() {
		logger.debug("getActiveSalesOrganizations()");
		List<CfgTblSalesOrganization> salesOrganizations = citTableSalesOrganizationDAO.getActiveSalesOrganization();
		return salesOrganizations;
	}
	
	@Override
	public String generateSalesOrganizationNo(String type) {
		
		return citTableSalesOrganizationDAO.generateSalesOrganizationNo(type);
		
	}
	
	@Override
	public boolean salesOrganizationExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableSalesOrganizationDAO.getSalesOrganizationByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization) {
				
		return citTableSalesOrganizationDAO.addNewSalesOrganization(cfgTblSalesOrganization);
	}

	@Override
	public String updateSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization) {
		
		return citTableSalesOrganizationDAO.updateSalesOrganization(cfgTblSalesOrganization);
	}

	@Override
	public String deleteSalesOrganization(List<String> salesOrganizationsId) {
		// TODO Auto-generated method stub
		return citTableSalesOrganizationDAO.deleteSalesOrganization(salesOrganizationsId);
	}
	
	@Override
	public List<CfgTblSalesOrganization> searchSalesOrganization(CfgTblSalesOrganization salesOrganization) {
		// TODO Auto-generated method stub
		return citTableSalesOrganizationDAO.searchSalesOrganization(salesOrganization);
	}

}
