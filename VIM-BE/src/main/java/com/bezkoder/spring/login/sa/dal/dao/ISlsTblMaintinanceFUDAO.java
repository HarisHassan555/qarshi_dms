package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblMaintinanceFU;

public interface ISlsTblMaintinanceFUDAO {

	List<SlsTblMaintinanceFU> getAllMaintinanceFU();

	List<SlsTblMaintinanceFU> getActiveMaintinanceFU();

	List<SlsTblMaintinanceFU> getMaintinanceFUByProperty(String property, String value, String mode, String oldValue);

	String addNewMaintinanceFU(SlsTblMaintinanceFU slsTblMaintinanceFU);

	String deleteMaintinanceFU(List<String> customerId);

	String updateMaintinanceFU(SlsTblMaintinanceFU slsTblMaintinanceFU);

	String generateMaintinanceFUNo(String type);

	String getMaintinanceFUById(String id);
	
	List<SlsTblMaintinanceFU> searchMaintinanceFU(SlsTblMaintinanceFU SlsTblMaintinanceFU);
	
	public String serivcetoaddMaintinanceFUFromthread();
}
