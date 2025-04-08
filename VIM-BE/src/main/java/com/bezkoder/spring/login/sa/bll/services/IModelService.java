package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblModel;


public interface IModelService {

	List<CfgTblModel> getAllModel();
	
	List<CfgTblModel> getActiveModel();
	
	String addNewModel(CfgTblModel cfgTblModel);

	boolean modelExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteModel(List<String> modelsId);

	String updateModel(CfgTblModel cfgTblModel);
	
	String generateModelNo(String type);
	
	List<CfgTblModel> searchModel(CfgTblModel model);
	

}
