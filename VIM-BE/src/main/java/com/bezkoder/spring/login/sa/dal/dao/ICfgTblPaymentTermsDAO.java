package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPaymentTerm;

public interface ICfgTblPaymentTermsDAO {

	List<CfgTblPaymentTerm> getAllPaymentTerms();

	List<CfgTblPaymentTerm> getActivePaymentTerms();

	List<CfgTblPaymentTerm> getPaymentTermsByProperty(String property, String value, String mode, String oldValue);

	String addNewPaymentTerms(CfgTblPaymentTerm cfgTblPaymentTerms);

	String deletePaymentTerms(List<String> customerId);

	String updatePaymentTerms(CfgTblPaymentTerm cfgTblPaymentTerms);

	String generatePaymentTermsNo(String type);

	String getPaymentTermsById(String customerId);
	
	List<CfgTblPaymentTerm> searchPaymentTerms(CfgTblPaymentTerm paymentTerms);
}
