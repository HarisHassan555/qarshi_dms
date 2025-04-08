package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;

public interface ICfgTblPasswordPolicyDAO {

	List<CfgTblPasswordPolicy> getAllPasswordPolicy();

	List<CfgTblPasswordPolicy> getActivePasswordPolicy();

	List<CfgTblPasswordPolicy> getPasswordPolicyByProperty(String property, String value, String mode, String oldValue);

	String addNewPasswordPolicy(CfgTblPasswordPolicy CfgTblPasswordPolicy);

	String deletePasswordPolicy(List<String> customerId);

	String updatePasswordPolicy(CfgTblPasswordPolicy CfgTblPasswordPolicy);

	String generatePasswordPolicyNo(String type);

	String getPasswordPolicyById(String customerId);
	
	List<CfgTblPasswordPolicy> searchPasswordPolicy(CfgTblPasswordPolicy role);
}
