package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;


public interface ISubMenuRoleService {

	List<CfgTblSubMenuRole> getAllSubMenuRole();
	
	List<CfgTblSubMenuRole> getActiveSubMenuRole();
	
	String addNewSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole);

	boolean getSubMenuRoleByProperty(String property, String value, String mode, String oldValue);
	
	String deleteSubMenuRole(List<String> subMenuRolesId);

	String deleteSubMenuRoleBySubMenuIds(List<String> subMenuIds);

	String updateSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole);
	
	String generateSubMenuRoleNo(String type);
	
	List<CfgTblSubMenuRole> searchSubMenuRole(CfgTblSubMenuRole subMenuRole);
	
	String addNewSubMenuRoleinList(List<CfgTblSubMenuRole> lstcfgTblSubMenuRoles);
	

}
