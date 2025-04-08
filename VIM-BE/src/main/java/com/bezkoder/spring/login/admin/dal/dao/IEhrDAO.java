package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.Ehr;

public interface IEhrDAO {

	List<Ehr> getAllEhr();

	List<Ehr> getActiveEhr();

	List<Ehr> getEhrByProperty(String property, String value, String mode, String oldValue);

	String addNewEhr(Ehr Ehr);

	String deleteEhr(List<String> customerId);

	String updateEhr(Ehr Ehr);

	String generateEhrNo(String type);

	String getEhrById(String ehrId);
	
	List<Ehr> searchEhr(Ehr ehr);
}
