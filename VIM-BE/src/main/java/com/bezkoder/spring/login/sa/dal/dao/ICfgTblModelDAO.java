package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblModel;

public interface ICfgTblModelDAO {

	List<CfgTblModel> getAllModel();

	List<CfgTblModel> getActiveModel();

	List<CfgTblModel> getModelByProperty(String property, String value, String mode, String oldValue);

	String addNewModel(CfgTblModel cfgTblModel);

	String deleteModel(List<String> customerId);

	String updateModel(CfgTblModel cfgTblModel);

	String generateModelNo(String type);

	String getModelById(String customerId);
	
	List<CfgTblModel> searchModel(CfgTblModel model);
}
