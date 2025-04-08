package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblBank;


public interface IBankService {

	List<CfgTblBank> getAllBank();
	
	List<CfgTblBank> getActiveBank();
	
	String addNewBank(CfgTblBank cfgTblBank);

	boolean getBankByProperty(String property, String value, String mode, String oldValue);
	
	String deleteBank(List<String> bankId);

	String updateBank(CfgTblBank cfgTblBank);
	
	String generateBankNo(String type);
	
	List<CfgTblBank> searchBank(CfgTblBank bank);
	

}
