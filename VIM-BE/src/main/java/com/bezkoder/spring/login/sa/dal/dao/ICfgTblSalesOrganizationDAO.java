package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSalesOrganization;

public interface ICfgTblSalesOrganizationDAO {

	List<CfgTblSalesOrganization> getAllSalesOrganization();

	List<CfgTblSalesOrganization> getActiveSalesOrganization();

	List<CfgTblSalesOrganization> getSalesOrganizationByProperty(String property, String value, String mode, String oldValue);

	String addNewSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization);

	String deleteSalesOrganization(List<String> customerId);

	String updateSalesOrganization(CfgTblSalesOrganization cfgTblSalesOrganization);

	String generateSalesOrganizationNo(String type);

	String getSalesOrganizationById(String customerId);
	
	List<CfgTblSalesOrganization> searchSalesOrganization(CfgTblSalesOrganization salesOrganization);
}
