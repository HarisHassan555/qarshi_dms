package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblComplaint;

public interface ISlsTblComplaintDAO {

	List<SlsTblComplaint> getAllComplaint();

	List<SlsTblComplaint> getActiveComplaint();

	List<SlsTblComplaint> getComplaintByProperty(String property, String value, String mode, String oldValue);

	String addNewComplaint(SlsTblComplaint slsTblComplaint);

	String deleteComplaint(List<String> customerId);

	String updateComplaint(SlsTblComplaint slsTblComplaint);

	String generateComplaintNo(String type);

	String getComplaintById(String id);
	
	List<SlsTblComplaint> searchComplaint(SlsTblComplaint SlsTblComplaint);
	
	public String serivcetoaddComplaintFromthread();
}
