package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblSalesOrganization;


public interface ISalesOrganizationService {

	List<CfgTblSalesOrganization> getAllSalesOrganization();
	
	List<CfgTblSalesOrganization> getActiveSalesOrganization();
	
	String addNewSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization);

	boolean salesOrganizationExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteSalesOrganization(List<String> salesOrganizationsId);

	String updateSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization);
	
	String generateSalesOrganizationNo(String type);
	
	List<CfgTblSalesOrganization> searchSalesOrganization(CfgTblSalesOrganization salesOrganization);
	

}
