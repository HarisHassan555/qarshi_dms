package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IRoleService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;


@Service
public class RoleService implements IRoleService {
	
	@Autowired
	private ICfgTblRoleDAO citTableRoleDAO;

	private Logger logger = LogManager.getLogger(RoleService.class);

	public RoleService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblRole> getAllRole() {
		logger.debug("getAllRoles()");
		List<CfgTblRole> roles = citTableRoleDAO.getAllRole();
		return roles;
	}
	
	@Override
	public List<CfgTblRole> getActiveRole() {
		logger.debug("getActiveRoles()");
		List<CfgTblRole> roles = citTableRoleDAO.getActiveRole();
		return roles;
	}
	
	@Override
	public String generateRoleNo(String type) {
		
		return citTableRoleDAO.generateRoleNo(type);
		
	}
	
	@Override
	public boolean getRoleByProperty(String property, String value,String mode, String oldValue) {
		return !citTableRoleDAO.getRoleByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewRole(CfgTblRole cfgTblRole) {
				
		return citTableRoleDAO.addNewRole(cfgTblRole);
	}

	@Override
	public String updateRole(CfgTblRole cfgTblRole) {
		
		return citTableRoleDAO.updateRole(cfgTblRole);
	}

	@Override
	public String deleteRole(List<String> rolesId) {
		// TODO Auto-generated method stub
		return citTableRoleDAO.deleteRole(rolesId);
	}
	
	@Override
	public List<CfgTblRole> searchRole(CfgTblRole role) {
		// TODO Auto-generated method stub
		return citTableRoleDAO.searchRole(role);
	}

}
