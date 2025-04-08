package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IPatientService;
import com.bezkoder.spring.login.admin.dal.dao.IPatientDAO;
import com.bezkoder.spring.login.admin.dal.entities.Patient;


@Service
public class PatientService implements IPatientService {
	
	@Autowired
	private IPatientDAO citTablePatientDAO;

	private Logger logger = LogManager.getLogger(PatientService.class);

	public PatientService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Patient> getAllPatient() {
		logger.debug("getAllPatients()");
		List<Patient> patients = citTablePatientDAO.getAllPatient();
		return patients;
	}
	
	@Override
	public List<Patient> getActivePatient() {
		logger.debug("getActivePatients()");
		List<Patient> patients = citTablePatientDAO.getActivePatient();
		return patients;
	}
	
	@Override
	public String generatePatientNo(String type) {
		
		return citTablePatientDAO.generatePatientNo(type);
		
	}
	
	@Override
	public boolean getPatientByProperty(String property, String value,String mode, String oldValue) {
		return !citTablePatientDAO.getPatientByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewPatient(Patient patient) {
				
		return citTablePatientDAO.addNewPatient(patient);
	}

	@Override
	public String updatePatient(Patient patient) {
		
		return citTablePatientDAO.updatePatient(patient);
	}

	@Override
	public String deletePatient(List<String> patientsId) {
		// TODO Auto-generated method stub
		return citTablePatientDAO.deletePatient(patientsId);
	}
	
	@Override
	public List<Patient> searchPatient(Patient patient) {
		// TODO Auto-generated method stub
		return citTablePatientDAO.searchPatient(patient);
	}

}
