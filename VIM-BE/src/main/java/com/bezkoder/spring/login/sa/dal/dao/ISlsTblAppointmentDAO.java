package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblAppointment;

public interface ISlsTblAppointmentDAO {

	List<SlsTblAppointment> getAllAppointment();

	List<SlsTblAppointment> getActiveAppointment();

	List<SlsTblAppointment> getAppointmentByProperty(String property, String value, String mode, String oldValue);

	String addNewAppointment(SlsTblAppointment slsTblAppointment);

	String deleteAppointment(List<String> customerId);

	String updateAppointment(SlsTblAppointment slsTblAppointment);

	String generateAppointmentNo(String type);

	String getAppointmentById(String id);
	
	List<SlsTblAppointment> searchAppointment(SlsTblAppointment SlsTblAppointment);
	
	public String serivcetoaddAppointmentFromthread();
}
