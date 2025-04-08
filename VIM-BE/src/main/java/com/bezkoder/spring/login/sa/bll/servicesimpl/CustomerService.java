package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import javax.xml.soap.SOAPException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ICustomerService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomerDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;


@Service
public class CustomerService implements ICustomerService {
	
	@Autowired
	private ICfgTblCustomerDAO citTableCustomerDAO;

	private Logger logger = LogManager.getLogger(CustomerService.class);

	public CustomerService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblCustomer> getAllCustomer() {
		logger.debug("getAllCustomers()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getAllCustomer();
		return customers;
	}
	
	@Override
	public List<CfgTblCustomer> getActiveCustomer() {
		logger.debug("getActiveCustomers()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getActiveCustomer();
		return customers;
	}
	
	@Override
	public String generateCustomerNo(String type) throws SOAPException {
		
		return citTableCustomerDAO.generateCustomerNo(type);
		
	}
	
	@Override
	public boolean getCustomerByProperty(String property, String value,String mode, String oldValue) {
		return !citTableCustomerDAO.getCustomerByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewCustomer(CfgTblCustomer cfgTblCustomer) {
				
		return citTableCustomerDAO.addNewCustomer(cfgTblCustomer);
	}

	@Override
	public String updateCustomer(CfgTblCustomer cfgTblCustomer) {
		
		return citTableCustomerDAO.updateCustomer(cfgTblCustomer);
	}

	@Override
	public String deleteCustomer(List<String> customerId) {
		// TODO Auto-generated method stub
		return citTableCustomerDAO.deleteCustomer(customerId);
	}
	
	@Override
	public List<CfgTblCustomer> searchCustomer(CfgTblCustomer customer) {
		// TODO Auto-generated method stub
		return citTableCustomerDAO.searchCustomer(customer);
	}
	
	@Override
	public List<CfgTblCustomer> getAllDealer() {
		logger.debug("getAllDealer()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getAllDealer();
		return customers;
	}
	
	@Override
	public List<CfgTblCustomer> getActiveDealer() {
		logger.debug("getActiveDealer()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getActiveDealer();
		return customers;
	}
	
	@Override
	public List<CfgTblCustomer> getCustomerWODealer() {
		logger.debug("getActiveDealer()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getCustomerWODealer();
		return customers;
	}
	
	
	@Override
	public List<CfgTblCustomer> getgroupActiveCustomer() {
		logger.debug("getgroupActiveCustomer()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getgroupActiveCustomer();
		return customers;
	}
	

	@Override
	public CfgTblCustomer addNewCustomerFromSapOrder(CfgTblCustomer cfgTblCustomer) {
				
		return citTableCustomerDAO.addNewCustomerFromSapOrder(cfgTblCustomer);
	}
	
	
	
	@Override
	public List<CfgTblCustomer> getAllMainDealer() {
		logger.debug("getAllMainDealer()");
		List<CfgTblCustomer> customers = citTableCustomerDAO.getAllMainDealer();
		return customers;
	}
	
	@Override
	public String updateAndrePostCustomerInSAP(CfgTblCustomer cfgTblCustomer) {
		
		return citTableCustomerDAO.updateAndrePostCustomerInSAP(cfgTblCustomer);
	}
	
}
