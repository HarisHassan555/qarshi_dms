package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import javax.xml.soap.SOAPException;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;

public interface ICfgTblCustomerDAO {

	List<CfgTblCustomer> getAllCustomer();

	List<CfgTblCustomer> getActiveCustomer();

	List<CfgTblCustomer> getCustomerByProperty(String property, String value, String mode, String oldValue);

	String addNewCustomer(CfgTblCustomer cfgTblCustomer);

	String deleteCustomer(List<String> customer);

	String updateCustomer(CfgTblCustomer cfgTblCustomer);

	String generateCustomerNo(String type) throws SOAPException;

	String getCustomerById(String customer);
	
	List<CfgTblCustomer> searchCustomer(CfgTblCustomer Customer);
	
	List<CfgTblCustomer> getAllDealer();

	List<CfgTblCustomer> getActiveDealer();
	
	List<CfgTblCustomer> getCustomerWODealer();
	
	List<CfgTblCustomer> getgroupActiveCustomer();
	
	CfgTblCustomer addNewCustomerFromSapOrder(CfgTblCustomer CfgTblCustomer);
	
	List<CfgTblCustomer> getAllMainDealer();
	
	 String updateAndrePostCustomerInSAP(CfgTblCustomer CfgTblCustomer);
	 
	 CfgTblCustomer addNewCustomerRC(CfgTblCustomer CfgTblCustomer);
	 
	 CfgTblCustomer getCustomerBycode(String CustomerId);
}
