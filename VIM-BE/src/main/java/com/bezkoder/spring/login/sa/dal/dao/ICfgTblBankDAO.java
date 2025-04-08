package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBank;

public interface ICfgTblBankDAO {

	List<CfgTblBank> getAllBank();

	List<CfgTblBank> getActiveBank();

	List<CfgTblBank> getBankByProperty(String property, String value, String mode, String oldValue);

	String addNewBank(CfgTblBank cfgTblBank);

	String deleteBank(List<String> customerId);

	String updateBank(CfgTblBank cfgTblBank);

	String generateBankNo(String type);

	String getBankById(String customerId);
	
	List<CfgTblBank> searchBank(CfgTblBank Bank);
}
