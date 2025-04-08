package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblLead;

public interface ISlsTblLeadDAO {

	List<SlsTblLead> getAllLead();

	List<SlsTblLead> getActiveLead();

	List<SlsTblLead> getLeadByProperty(String property, String value, String mode, String oldValue);

	String addNewLead(SlsTblLead slsTblLead);

	String deleteLead(List<String> customerId);

	String updateLead(SlsTblLead slsTblLead);

	String generateLeadNo(String type);

	String getLeadById(String customerId);
	
	List<SlsTblLead> searchLead(SlsTblLead lead);
}
