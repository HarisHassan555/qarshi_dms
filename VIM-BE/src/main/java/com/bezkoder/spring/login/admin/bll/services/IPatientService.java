package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.Patient;


public interface IPatientService {

	List<Patient> getAllPatient();
	
	List<Patient> getActivePatient();
	
	String addNewPatient(Patient patient);

	boolean getPatientByProperty(String property, String value, String mode, String oldValue);
	
	String deletePatient(List<String> patientsId);

	String updatePatient(Patient patient);
	
	String generatePatientNo(String type);
	
	List<Patient> searchPatient(Patient patient);
	

}
