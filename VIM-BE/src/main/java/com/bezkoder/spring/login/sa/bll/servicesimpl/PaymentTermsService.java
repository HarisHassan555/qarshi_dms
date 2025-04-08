package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IPaymentTermService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblPaymentTermsDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPaymentTerm;


@Service
public class PaymentTermsService implements IPaymentTermService {
	
	@Autowired
	private ICfgTblPaymentTermsDAO citTablePaymentTermDAO;

	private Logger logger = LogManager.getLogger(PaymentTermsService.class);

	public PaymentTermsService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblPaymentTerm> getAllPaymentTerm() {
		logger.debug("getAllPaymentTerms()");
		List<CfgTblPaymentTerm> paymentTerms = citTablePaymentTermDAO.getAllPaymentTerms();
		return paymentTerms;
	}
	
	@Override
	public List<CfgTblPaymentTerm> getActivePaymentTerm() {
		logger.debug("getActivePaymentTerms()");
		List<CfgTblPaymentTerm> paymentTerms = citTablePaymentTermDAO.getActivePaymentTerms();
		return paymentTerms;
	}
	
	@Override
	public String generatePaymentTermNo(String type) {
		
		return citTablePaymentTermDAO.generatePaymentTermsNo(type);
		
	}
	
	@Override
	public boolean paymentTermExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTablePaymentTermDAO.getPaymentTermsByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewPaymentTerm(CfgTblPaymentTerm cfgTblPaymentTerm) {
				
		return citTablePaymentTermDAO.addNewPaymentTerms(cfgTblPaymentTerm);
	}

	@Override
	public String updatePaymentTerm(CfgTblPaymentTerm cfgTblPaymentTerm) {
		
		return citTablePaymentTermDAO.updatePaymentTerms(cfgTblPaymentTerm);
	}

	@Override
	public String deletePaymentTerm(List<String> paymentTermsId) {
		// TODO Auto-generated method stub
		return citTablePaymentTermDAO.deletePaymentTerms(paymentTermsId);
	}
	
	@Override
	public List<CfgTblPaymentTerm> searchPaymentTerm(CfgTblPaymentTerm paymentTerm) {
		// TODO Auto-generated method stub
		return citTablePaymentTermDAO.searchPaymentTerms(paymentTerm);
	}

}
