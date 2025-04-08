package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblIncoTerm;


public interface IIncoTermsService {

	List<CfgTblIncoTerm> getAllIncoTerm();
	
	List<CfgTblIncoTerm> getActiveIncoTerm();
	
	String addNewIncoTerm(CfgTblIncoTerm cfgTblIncoTerm);

	boolean incoTermExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteIncoTerm(List<String> incoTermsId);

	String updateIncoTerm(CfgTblIncoTerm cfgTblIncoTerm);
	
	String generateIncoTermNo(String type);
	
	List<CfgTblIncoTerm> searchIncoTerm(CfgTblIncoTerm incoTerm);
	

}
