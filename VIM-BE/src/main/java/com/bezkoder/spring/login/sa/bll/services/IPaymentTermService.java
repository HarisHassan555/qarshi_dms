package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblPaymentTerm;


public interface IPaymentTermService {

	List<CfgTblPaymentTerm> getAllPaymentTerm();
	
	List<CfgTblPaymentTerm> getActivePaymentTerm();
	
	String addNewPaymentTerm(CfgTblPaymentTerm cfgTblPaymentTerm);

	boolean paymentTermExistByProperty(String property, String value, String mode, String oldValue);
	
	String deletePaymentTerm(List<String> paymentTermsId);

	String updatePaymentTerm(CfgTblPaymentTerm cfgTblPaymentTerm);
	
	String generatePaymentTermNo(String type);
	
	List<CfgTblPaymentTerm> searchPaymentTerm(CfgTblPaymentTerm paymentTerm);
	

}
