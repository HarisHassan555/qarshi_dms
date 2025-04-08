package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.ISubMenuRoleService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblSubMenuRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;


@Service
public class SubMenuRoleService implements ISubMenuRoleService {
	
	@Autowired
	private ICfgTblSubMenuRoleDAO citTableSubMenuRoleDAO;

	private Logger logger = LogManager.getLogger(SubMenuService.class);

	public SubMenuRoleService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblSubMenuRole> getAllSubMenuRole() {
		logger.debug("getAllSubMenuRoles()");
		List<CfgTblSubMenuRole> subMenuRoles = citTableSubMenuRoleDAO.getAllSubMenuRole();
		return subMenuRoles;
	}
	
	@Override
	public List<CfgTblSubMenuRole> getActiveSubMenuRole() {
		logger.debug("getActiveSubMenuRoles()");
		List<CfgTblSubMenuRole> subMenuRoles = citTableSubMenuRoleDAO.getActiveSubMenuRole();
		return subMenuRoles;
	}
	
	@Override
	public String generateSubMenuRoleNo(String type) {
		
		return citTableSubMenuRoleDAO.generateSubMenuRoleNo(type);
		
	}
	
	@Override
	public boolean getSubMenuRoleByProperty(String property, String value,String mode, String oldValue) {
		return !citTableSubMenuRoleDAO.getSubMenuRoleByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole) {
				
		return citTableSubMenuRoleDAO.addNewSubMenuRole(cfgTblSubMenuRole);
	}

	@Override
	public String updateSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole) {
		
		return citTableSubMenuRoleDAO.updateSubMenuRole(cfgTblSubMenuRole);
	}

	@Override
	public String deleteSubMenuRole(List<String> subMenuRolesId) {
		// TODO Auto-generated method stub
		return citTableSubMenuRoleDAO.deleteSubMenuRole(subMenuRolesId);
	}
	
	@Override
	public List<CfgTblSubMenuRole> searchSubMenuRole(CfgTblSubMenuRole subMenuRole) {
		// TODO Auto-generated method stub
		return citTableSubMenuRoleDAO.searchSubMenuRole(subMenuRole);
	}
	
	
	@Override
	public String addNewSubMenuRoleinList(List<CfgTblSubMenuRole> lstcfgTblSubMenuRoles) {
		// TODO Auto-generated method stub
		return citTableSubMenuRoleDAO.addNewSubMenuRoleinList(lstcfgTblSubMenuRoles);
	}

}
