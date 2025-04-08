package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IJobTypeService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblJobTypeDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobType;


@Service
public class JobTypeService implements IJobTypeService {
	
	@Autowired
	private ICfgTblJobTypeDAO citTableJobTypeDAO;

	private Logger logger = LogManager.getLogger(JobTypeService.class);

	public JobTypeService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblJobType> getAllJobType() {
		logger.debug("getAllJobTypes()");
		List<CfgTblJobType> jobTypes = citTableJobTypeDAO.getAllJobType();
		return jobTypes;
	}
	
	@Override
	public List<CfgTblJobType> getActiveJobType() {
		logger.debug("getActiveJobTypes()");
		List<CfgTblJobType> jobTypes = citTableJobTypeDAO.getActiveJobType();
		return jobTypes;
	}
	
	@Override
	public String generateJobTypeNo(String type) {
		
		return citTableJobTypeDAO.generateJobTypeNo(type);
		
	}
	
	@Override
	public boolean jobTypeExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableJobTypeDAO.getJobTypeByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewJobType(CfgTblJobType cfgTblJobType) {
				
		return citTableJobTypeDAO.addNewJobType(cfgTblJobType);
	}

	@Override
	public String updateJobType(CfgTblJobType cfgTblJobType) {
		
		return citTableJobTypeDAO.updateJobType(cfgTblJobType);
	}

	@Override
	public String deleteJobType(List<String> jobTypesId) {
		// TODO Auto-generated method stub
		return citTableJobTypeDAO.deleteJobType(jobTypesId);
	}
	
	@Override
	public List<CfgTblJobType> searchJobType(CfgTblJobType jobType) {
		// TODO Auto-generated method stub
		return citTableJobTypeDAO.searchJobType(jobType);
	}

}
