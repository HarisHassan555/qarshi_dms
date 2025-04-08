package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;


public interface IRoleService {

	List<CfgTblRole> getAllRole();
	
	List<CfgTblRole> getActiveRole();
	
	String addNewRole(CfgTblRole cfgTblRole);

	boolean getRoleByProperty(String property, String value, String mode, String oldValue);
	
	String deleteRole(List<String> rolesId);

	String updateRole(CfgTblRole cfgTblRole);
	
	String generateRoleNo(String type);
	
	List<CfgTblRole> searchRole(CfgTblRole role);
	

}
