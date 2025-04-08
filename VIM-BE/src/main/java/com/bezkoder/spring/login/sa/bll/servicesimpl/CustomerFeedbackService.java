package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ICustomerFeedbackService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblCustomerFeedbackDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblCustomerFeedback;


@Service
public class CustomerFeedbackService implements ICustomerFeedbackService {
	
	@Autowired
	private ISlsTblCustomerFeedbackDAO citTableCustomerFeedbackDAO;

	private Logger logger = LogManager.getLogger(CustomerFeedbackService.class);

	public CustomerFeedbackService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<SlsTblCustomerFeedback> getAllCustomerFeedback() {
		logger.debug("getAllCustomerFeedbacks()");
		List<SlsTblCustomerFeedback> citys = citTableCustomerFeedbackDAO.getAllCustomerFeedback();
		return citys;
	}
	
	@Override
	public List<SlsTblCustomerFeedback> getActiveCustomerFeedback() {
		logger.debug("getActiveCustomerFeedbacks()");
		List<SlsTblCustomerFeedback> citys = citTableCustomerFeedbackDAO.getActiveCustomerFeedback();
		return citys;
	}
	
	@Override
	public String generateCustomerFeedbackNo(String type) {
		
		return citTableCustomerFeedbackDAO.generateCustomerFeedbackNo(type);
		
	}
	
	@Override
	public boolean getCustomerFeedbackByProperty(String property, String value,String mode, String oldValue) {
		return !citTableCustomerFeedbackDAO.getCustomerFeedbackByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewCustomerFeedback(SlsTblCustomerFeedback slsTblCustomerFeedback) {
				
		return citTableCustomerFeedbackDAO.addNewCustomerFeedback(slsTblCustomerFeedback);
	}

	@Override
	public String updateCustomerFeedback(SlsTblCustomerFeedback slsTblCustomerFeedback) {
		
		return citTableCustomerFeedbackDAO.updateCustomerFeedback(slsTblCustomerFeedback);
	}

	@Override
	public String deleteCustomerFeedback(List<String> citysId) {
		// TODO Auto-generated method stub
		return citTableCustomerFeedbackDAO.deleteCustomerFeedback(citysId);
	}
	
	@Override
	public List<SlsTblCustomerFeedback> searchCustomerFeedback(SlsTblCustomerFeedback city) {
		// TODO Auto-generated method stub
		return citTableCustomerFeedbackDAO.searchCustomerFeedback(city);
	}

}
