package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;

public interface ICfgTblSubMenuRoleDAO {

	List<CfgTblSubMenuRole> getAllSubMenuRole();

	List<CfgTblSubMenuRole> getActiveSubMenuRole();

	List<CfgTblSubMenuRole> getSubMenuRoleByProperty(String property, String value, String mode, String oldValue);

	String addNewSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole);

	String deleteSubMenuRole(List<String> subMenuRoleId);

	String deleteSubMenuRoleBySubMenuIds(List<String> subMenuIds);

	String updateSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole);

	String generateSubMenuRoleNo(String type);

	String getSubMenuRoleById(String subMenuRoleId);
	
	List<CfgTblSubMenuRole> searchSubMenuRole(CfgTblSubMenuRole subMenuRole);
	
	String addNewSubMenuRoleinList(List<CfgTblSubMenuRole> lstcfgTblSubMenuRoles);
}
