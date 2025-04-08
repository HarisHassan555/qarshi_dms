package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobType;

public interface ICfgTblJobTypeDAO {

	List<CfgTblJobType> getAllJobType();

	List<CfgTblJobType> getActiveJobType();

	List<CfgTblJobType> getJobTypeByProperty(String property, String value, String mode, String oldValue);

	String addNewJobType(CfgTblJobType cfgTblJobType);

	String deleteJobType(List<String> customerId);

	String updateJobType(CfgTblJobType cfgTblJobType);

	String generateJobTypeNo(String type);

	String getJobTypeById(String customerId);
	
	List<CfgTblJobType> searchJobType(CfgTblJobType jobType);
}
