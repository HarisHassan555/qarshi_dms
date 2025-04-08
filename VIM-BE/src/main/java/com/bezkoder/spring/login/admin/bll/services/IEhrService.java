package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.Ehr;


public interface IEhrService {

	List<Ehr> getAllEhr();
	
	List<Ehr> getActiveEhr();
	
	String addNewEhr(Ehr ehr);

	boolean getEhrByProperty(String property, String value, String mode, String oldValue);
	
	String deleteEhr(List<String> ehrsId);

	String updateEhr(Ehr ehr);
	
	String generateEhrNo(String type);
	
	List<Ehr> searchEhr(Ehr ehr);
	

}
