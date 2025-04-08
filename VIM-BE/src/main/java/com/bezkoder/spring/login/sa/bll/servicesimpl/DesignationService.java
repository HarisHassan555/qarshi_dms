package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDesignationService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblDesignationDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDesignation;


@Service
public class DesignationService implements IDesignationService {
	
	@Autowired
	private IHrTblDesignationDAO hrTableDesignationDAO;

	private Logger logger = LogManager.getLogger(DesignationService.class);

	public DesignationService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<HrTblDesignation> getAllDesignations() {
		logger.debug("getAllDesignations()");
		List<HrTblDesignation> Designations = hrTableDesignationDAO.getAllDesignations();
		return Designations;
	}
	
	@Override
	public List<HrTblDesignation> getActiveDesignations() {
		logger.debug("getActiveDesignations()");
		List<HrTblDesignation> Designations = hrTableDesignationDAO.getActiveDesignations();
		return Designations;
	}
	
	@Override
	public String generateDesignationNo(String type) {
		
		return hrTableDesignationDAO.generateDesignationNo(type);
		
	}
	
	@Override
	public boolean DesignationExistByProperty(String property, String value,String mode, String oldValue) {
		return !hrTableDesignationDAO.getDesignationByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDesignation(HrTblDesignation HrTblDesignation) {
				
		return hrTableDesignationDAO.addNewDesignation(HrTblDesignation);
	}

	@Override
	public String updateDesignation(HrTblDesignation HrTblDesignation) {
		
		return hrTableDesignationDAO.updateDesignation(HrTblDesignation);
	}

	@Override
	public String deleteDesignations(List<String> designationId) {
		// TODO Auto-generated method stub
		return hrTableDesignationDAO.deleteDesignations(designationId);
	}
	
	@Override
	public List<HrTblDesignation> searchDesignation(HrTblDesignation designation) {
		// TODO Auto-generated method stub
		return hrTableDesignationDAO.searchDesignation(designation);
	}

}
