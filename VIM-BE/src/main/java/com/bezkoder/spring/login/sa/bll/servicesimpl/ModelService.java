package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IModelService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblModelDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblModel;


@Service
public class ModelService implements IModelService {
	
	@Autowired
	private ICfgTblModelDAO citTableModelDAO;

	private Logger logger = LogManager.getLogger(ModelService.class);

	public ModelService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblModel> getAllModel() {
		logger.debug("getAllModels()");
		List<CfgTblModel> models = citTableModelDAO.getAllModel();
		return models;
	}
	
	@Override
	public List<CfgTblModel> getActiveModel() {
		logger.debug("getActiveModels()");
		List<CfgTblModel> models = citTableModelDAO.getActiveModel();
		return models;
	}
	
	@Override
	public String generateModelNo(String type) {
		
		return citTableModelDAO.generateModelNo(type);
		
	}
	
	@Override
	public boolean modelExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableModelDAO.getModelByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewModel(CfgTblModel cfgTblModel) {
				
		return citTableModelDAO.addNewModel(cfgTblModel);
	}

	@Override
	public String updateModel(CfgTblModel cfgTblModel) {
		
		return citTableModelDAO.updateModel(cfgTblModel);
	}

	@Override
	public String deleteModel(List<String> modelsId) {
		// TODO Auto-generated method stub
		return citTableModelDAO.deleteModel(modelsId);
	}
	
	@Override
	public List<CfgTblModel> searchModel(CfgTblModel model) {
		// TODO Auto-generated method stub
		return citTableModelDAO.searchModel(model);
	}

}
