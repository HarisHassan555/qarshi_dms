package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import javax.xml.soap.SOAPException;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;


public interface ICustomerService {

	List<CfgTblCustomer> getAllCustomer();
	
	List<CfgTblCustomer> getActiveCustomer();
	
	String addNewCustomer(CfgTblCustomer cfgTblCustomer);

	boolean getCustomerByProperty(String property, String value, String mode, String oldValue);
	
	String deleteCustomer(List<String> productId);

	String updateCustomer(CfgTblCustomer cfgTblCustomer);
	
	String generateCustomerNo(String type) throws SOAPException;
	
	List<CfgTblCustomer> searchCustomer(CfgTblCustomer product);
	
	List<CfgTblCustomer> getAllDealer();

	List<CfgTblCustomer> getActiveDealer();
	
	List<CfgTblCustomer> getCustomerWODealer();
	
	List<CfgTblCustomer> getgroupActiveCustomer();
	
	CfgTblCustomer addNewCustomerFromSapOrder(CfgTblCustomer CfgTblCustomer);
	
	List<CfgTblCustomer> getAllMainDealer();
	
	 String updateAndrePostCustomerInSAP(CfgTblCustomer CfgTblCustomer);

}
