package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblCustomerFeedback;


public interface ICustomerFeedbackService {

	List<SlsTblCustomerFeedback> getAllCustomerFeedback();
	
	List<SlsTblCustomerFeedback> getActiveCustomerFeedback();
	
	String addNewCustomerFeedback(SlsTblCustomerFeedback cfgTblCustomerFeedback);

	boolean getCustomerFeedbackByProperty(String property, String value, String mode, String oldValue);
	
	String deleteCustomerFeedback(List<String> citysId);

	String updateCustomerFeedback(SlsTblCustomerFeedback cfgTblCustomerFeedback);
	
	String generateCustomerFeedbackNo(String type);
	
	List<SlsTblCustomerFeedback> searchCustomerFeedback(SlsTblCustomerFeedback city);
	

}
