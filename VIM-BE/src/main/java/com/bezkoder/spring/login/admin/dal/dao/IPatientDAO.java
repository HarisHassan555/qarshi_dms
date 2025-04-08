package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.Patient;

public interface IPatientDAO {

	List<Patient> getAllPatient();

	List<Patient> getActivePatient();

	List<Patient> getPatientByProperty(String property, String value, String mode, String oldValue);

	String addNewPatient(Patient Patient);

	String deletePatient(List<String> patientId);

	String updatePatient(Patient Patient);

	String generatePatientNo(String type);

	String getPatientById(String patientId);
	
	List<Patient> searchPatient(Patient patient);
}
