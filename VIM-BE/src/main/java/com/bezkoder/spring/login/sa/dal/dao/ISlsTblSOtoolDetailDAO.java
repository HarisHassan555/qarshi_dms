package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblToolDetail;

public interface ISlsTblSOtoolDetailDAO {

	List<SlsTblToolDetail> getAllToolDetail();

	List<SlsTblToolDetail> getActiveToolDetail();

	List<SlsTblToolDetail> getToolDetailByProperty(String property, String value, String mode, String oldValue);

	String addNewToolDetail(SlsTblToolDetail slsTblToolDetail);

	String deleteToolDetail(List<String> customerId);

	String updateToolDetail(SlsTblToolDetail slsTblToolDetail);

	String generateToolDetailNo(String type);

	String getToolDetailById(String customerId);
	
	List<SlsTblToolDetail> searchToolDetail(SlsTblToolDetail toolDetail);
}
