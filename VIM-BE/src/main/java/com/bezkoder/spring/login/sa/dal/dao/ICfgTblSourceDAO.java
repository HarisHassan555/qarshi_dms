package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSource;

public interface ICfgTblSourceDAO {

	List<CfgTblSource> getAllSource();

	List<CfgTblSource> getActiveSource();

	List<CfgTblSource> getSourceByProperty(String property, String value, String mode, String oldValue);

	String addNewSource(CfgTblSource cfgTblSource);

	String deleteSource(List<String> customerId);

	String updateSource(CfgTblSource cfgTblSource);

	String generateSourceNo(String type);

	String getSourceById(String customerId);
	
	List<CfgTblSource> searchSource(CfgTblSource Source);
}
