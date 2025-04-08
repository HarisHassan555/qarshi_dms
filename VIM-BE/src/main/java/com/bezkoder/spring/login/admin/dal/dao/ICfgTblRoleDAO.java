package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;

public interface ICfgTblRoleDAO {

	List<CfgTblRole> getAllRole();

	List<CfgTblRole> getActiveRole();

	List<CfgTblRole> getRoleByProperty(String property, String value, String mode, String oldValue);

	String addNewRole(CfgTblRole CfgTblRole);

	String deleteRole(List<String> customerId);

	String updateRole(CfgTblRole CfgTblRole);

	String generateRoleNo(String type);

	String getRoleById(String customerId);
	
	List<CfgTblRole> searchRole(CfgTblRole role);
}
