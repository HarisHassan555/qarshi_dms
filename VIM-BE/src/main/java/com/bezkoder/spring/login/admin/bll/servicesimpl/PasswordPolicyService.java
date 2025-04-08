package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IPasswordPolicyService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblPasswordPolicyDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;


@Service
public class PasswordPolicyService implements IPasswordPolicyService {
	
	@Autowired
	private ICfgTblPasswordPolicyDAO citTablePasswordPolicyDAO;

	private Logger logger = LogManager.getLogger(PasswordPolicyService.class);

	public PasswordPolicyService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblPasswordPolicy> getAllPasswordPolicy() {
		logger.debug("getAllPasswordPolicys()");
		List<CfgTblPasswordPolicy> passwordPolicys = citTablePasswordPolicyDAO.getAllPasswordPolicy();
		return passwordPolicys;
	}
	
	@Override
	public List<CfgTblPasswordPolicy> getActivePasswordPolicy() {
		logger.debug("getActivePasswordPolicys()");
		List<CfgTblPasswordPolicy> passwordPolicys = citTablePasswordPolicyDAO.getActivePasswordPolicy();
		return passwordPolicys;
	}
	
	@Override
	public String generatePasswordPolicyNo(String type) {
		
		return citTablePasswordPolicyDAO.generatePasswordPolicyNo(type);
		
	}
	
	@Override
	public boolean getPasswordPolicyByProperty(String property, String value,String mode, String oldValue) {
		return !citTablePasswordPolicyDAO.getPasswordPolicyByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewPasswordPolicy(CfgTblPasswordPolicy cfgTblPasswordPolicy) {
				
		return citTablePasswordPolicyDAO.addNewPasswordPolicy(cfgTblPasswordPolicy);
	}

	@Override
	public String updatePasswordPolicy(CfgTblPasswordPolicy cfgTblPasswordPolicy) {
		
		return citTablePasswordPolicyDAO.updatePasswordPolicy(cfgTblPasswordPolicy);
	}

	@Override
	public String deletePasswordPolicy(List<String> passwordPolicysId) {
		// TODO Auto-generated method stub
		return citTablePasswordPolicyDAO.deletePasswordPolicy(passwordPolicysId);
	}
	
	@Override
	public List<CfgTblPasswordPolicy> searchPasswordPolicy(CfgTblPasswordPolicy passwordPolicy) {
		// TODO Auto-generated method stub
		return citTablePasswordPolicyDAO.searchPasswordPolicy(passwordPolicy);
	}

}
