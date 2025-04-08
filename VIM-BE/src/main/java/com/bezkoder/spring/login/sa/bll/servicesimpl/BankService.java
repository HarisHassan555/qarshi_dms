package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IBankService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblBankDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBank;


@Service
public class BankService implements IBankService {
	
	@Autowired
	private ICfgTblBankDAO citTableBankDAO;

	private Logger logger = LogManager.getLogger(BankService.class);

	public BankService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblBank> getAllBank() {
		logger.debug("getAllBanks()");
		List<CfgTblBank> citys = citTableBankDAO.getAllBank();
		return citys;
	}
	
	@Override
	public List<CfgTblBank> getActiveBank() {
		logger.debug("getActiveBanks()");
		List<CfgTblBank> citys = citTableBankDAO.getActiveBank();
		return citys;
	}
	
	@Override
	public String generateBankNo(String type) {
		
		return citTableBankDAO.generateBankNo(type);
		
	}
	
	@Override
	public boolean getBankByProperty(String property, String value,String mode, String oldValue) {
		return !citTableBankDAO.getBankByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewBank(CfgTblBank cfgTblBank) {
				
		return citTableBankDAO.addNewBank(cfgTblBank);
	}

	@Override
	public String updateBank(CfgTblBank cfgTblBank) {
		
		return citTableBankDAO.updateBank(cfgTblBank);
	}

	@Override
	public String deleteBank(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableBankDAO.deleteBank(citysId);
	}
	
	@Override
	public List<CfgTblBank> searchBank(CfgTblBank city) {
		// TODO Auto-generated method stub
		return citTableBankDAO.searchBank(city);
	}

}
