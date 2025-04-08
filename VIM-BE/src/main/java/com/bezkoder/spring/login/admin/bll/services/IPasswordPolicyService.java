package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;


public interface IPasswordPolicyService {

	List<CfgTblPasswordPolicy> getAllPasswordPolicy();
	
	List<CfgTblPasswordPolicy> getActivePasswordPolicy();
	
	String addNewPasswordPolicy(CfgTblPasswordPolicy cfgTblPasswordPolicy);

	boolean getPasswordPolicyByProperty(String property, String value, String mode, String oldValue);
	
	String deletePasswordPolicy(List<String> passwordPolicysId);

	String updatePasswordPolicy(CfgTblPasswordPolicy cfgTblPasswordPolicy);
	
	String generatePasswordPolicyNo(String type);
	
	List<CfgTblPasswordPolicy> searchPasswordPolicy(CfgTblPasswordPolicy passwordPolicy);
	

}
