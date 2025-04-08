package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblFFSFU;

public interface ISlsTblFFSFUDAO {

	List<SlsTblFFSFU> getAllFFSFU();

	List<SlsTblFFSFU> getActiveFFSFU();

	List<SlsTblFFSFU> getFFSFUByProperty(String property, String value, String mode, String oldValue);

	String addNewFFSFU(SlsTblFFSFU slsTblFFSFU);

	String deleteFFSFU(List<String> customerId);

	String updateFFSFU(SlsTblFFSFU slsTblFFSFU);

	String generateFFSFUNo(String type);

	String getFFSFUById(String id);
	
	List<SlsTblFFSFU> searchFFSFU(SlsTblFFSFU SlsTblFFSFU);
	
	public String serivcetoaddFFSFUFromthread();
}
