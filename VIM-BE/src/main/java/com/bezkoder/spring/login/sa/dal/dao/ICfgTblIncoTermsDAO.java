package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblIncoTerm;

public interface ICfgTblIncoTermsDAO {

	List<CfgTblIncoTerm> getAllIncoTerm();

	List<CfgTblIncoTerm> getActiveIncoTerm();

	List<CfgTblIncoTerm> getIncoTermByProperty(String property, String value, String mode, String oldValue);

	String addNewIncoTerm(CfgTblIncoTerm cfgTblIncoTerm);

	String deleteIncoTerm(List<String> customerId);

	String updateIncoTerm(CfgTblIncoTerm cfgTblIncoTerm);

	String generateIncoTermNo(String type);

	String getIncoTermById(String customerId);
	
	List<CfgTblIncoTerm> searchIncoTerm(CfgTblIncoTerm country);
	
	CfgTblIncoTerm getIncoTermByPK(int IncoTermId);
}
