package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IEhrService;
import com.bezkoder.spring.login.admin.dal.dao.IEhrDAO;
import com.bezkoder.spring.login.admin.dal.entities.Ehr;


@Service
public class EhrService implements IEhrService {
	
	@Autowired
	private IEhrDAO TableEhrDAO;

	private Logger logger = LogManager.getLogger(EhrService.class);

	public EhrService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Ehr> getAllEhr() {
		logger.debug("getAllEhrs()");
		List<Ehr> ehrs = TableEhrDAO.getAllEhr();
		return ehrs;
	}
	
	@Override
	public List<Ehr> getActiveEhr() {
		logger.debug("getActiveEhrs()");
		List<Ehr> ehrs = TableEhrDAO.getActiveEhr();
		return ehrs;
	}
	
	@Override
	public String generateEhrNo(String type) {
		
		return TableEhrDAO.generateEhrNo(type);
		
	}
	
	@Override
	public boolean getEhrByProperty(String property, String value,String mode, String oldValue) {
		return !TableEhrDAO.getEhrByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewEhr(Ehr ehr) {
				
		return TableEhrDAO.addNewEhr(ehr);
	}

	@Override
	public String updateEhr(Ehr ehr) {
		
		return TableEhrDAO.updateEhr(ehr);
	}

	@Override
	public String deleteEhr(List<String> ehrsId) {
		// TODO Auto-generated method stub
		return TableEhrDAO.deleteEhr(ehrsId);
	}
	
	@Override
	public List<Ehr> searchEhr(Ehr ehr) {
		// TODO Auto-generated method stub
		return TableEhrDAO.searchEhr(ehr);
	}

}
