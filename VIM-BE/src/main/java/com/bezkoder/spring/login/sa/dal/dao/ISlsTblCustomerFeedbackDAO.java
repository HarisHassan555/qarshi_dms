package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblCustomerFeedback;

public interface ISlsTblCustomerFeedbackDAO {

	List<SlsTblCustomerFeedback> getAllCustomerFeedback();

	List<SlsTblCustomerFeedback> getActiveCustomerFeedback();

	List<SlsTblCustomerFeedback> getCustomerFeedbackByProperty(String property, String value, String mode, String oldValue);

	String addNewCustomerFeedback(SlsTblCustomerFeedback slsTblCustomerFeedback);

	String deleteCustomerFeedback(List<String> customerId);

	String updateCustomerFeedback(SlsTblCustomerFeedback slsTblCustomerFeedback);

	String generateCustomerFeedbackNo(String type);

	String getCustomerFeedbackById(String customerId);
	
	List<SlsTblCustomerFeedback> searchCustomerFeedback(SlsTblCustomerFeedback city);
}
