package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDefectService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDefectDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDefect;


@Service
public class DefectService implements IDefectService {
	
	@Autowired
	private ICfgTblDefectDAO citTableDefectDAO;

	private Logger logger = LogManager.getLogger(DefectService.class);

	public DefectService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblDefect> getAllDefect() {
		logger.debug("getAllDefects()");
		List<CfgTblDefect> defects = citTableDefectDAO.getAllDefect();
		return defects;
	}
	
	@Override
	public List<CfgTblDefect> getActiveDefect() {
		logger.debug("getActiveDefects()");
		List<CfgTblDefect> defects = citTableDefectDAO.getActiveDefect();
		return defects;
	}
	
	@Override
	public String generateDefectNo(String type) {
		
		return citTableDefectDAO.generateDefectNo(type);
		
	}
	
	@Override
	public boolean defectExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableDefectDAO.getDefectByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDefect(CfgTblDefect cfgTblDefect) {
				
		return citTableDefectDAO.addNewDefect(cfgTblDefect);
	}

	@Override
	public String updateDefect(CfgTblDefect cfgTblDefect) {
		
		return citTableDefectDAO.updateDefect(cfgTblDefect);
	}

	@Override
	public String deleteDefect(List<String> defectsId) {
		// TODO Auto-generated method stub
		return citTableDefectDAO.deleteDefect(defectsId);
	}
	
	@Override
	public List<CfgTblDefect> searchDefect(CfgTblDefect defect) {
		// TODO Auto-generated method stub
		return citTableDefectDAO.searchDefect(defect);
	}

}
