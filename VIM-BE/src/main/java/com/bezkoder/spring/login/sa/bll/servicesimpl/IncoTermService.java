package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.dao.ICfgTblIncoTermsDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblIncoTerm;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IIncoTermsService;



@Service
public class IncoTermService implements IIncoTermsService {
	
	@Autowired
	private ICfgTblIncoTermsDAO citTableIncoTermDAO;

	private Logger logger = LogManager.getLogger(IncoTermService.class);

	public IncoTermService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblIncoTerm> getAllIncoTerm() {
		logger.debug("getAllIncoTerms()");
		List<CfgTblIncoTerm> incoTerms = citTableIncoTermDAO.getAllIncoTerm();
		return incoTerms;
	}
	
	@Override
	public List<CfgTblIncoTerm> getActiveIncoTerm() {
		logger.debug("getActiveIncoTerms()");
		List<CfgTblIncoTerm> incoTerms = citTableIncoTermDAO.getActiveIncoTerm();
		return incoTerms;
	}
	
	@Override
	public String generateIncoTermNo(String type) {
		
		return citTableIncoTermDAO.generateIncoTermNo(type);
		
	}
	
	@Override
	public boolean incoTermExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableIncoTermDAO.getIncoTermByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewIncoTerm(CfgTblIncoTerm cfgTblIncoTerm) {
				
		return citTableIncoTermDAO.addNewIncoTerm(cfgTblIncoTerm);
	}

	@Override
	public String updateIncoTerm(CfgTblIncoTerm cfgTblIncoTerm) {
		
		return citTableIncoTermDAO.updateIncoTerm(cfgTblIncoTerm);
	}

	@Override
	public String deleteIncoTerm(List<String> incoTermsId) {
		// TODO Auto-generated method stub
		return citTableIncoTermDAO.deleteIncoTerm(incoTermsId);
	}
	
	@Override
	public List<CfgTblIncoTerm> searchIncoTerm(CfgTblIncoTerm incoTerm) {
		// TODO Auto-generated method stub
		return citTableIncoTermDAO.searchIncoTerm(incoTerm);
	}

}
