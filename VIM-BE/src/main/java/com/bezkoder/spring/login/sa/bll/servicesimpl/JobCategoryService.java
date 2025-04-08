package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IJobCategoryService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblJobCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobCategory;


@Service
public class JobCategoryService implements IJobCategoryService {
	
	@Autowired
	private ICfgTblJobCategoryDAO citTableJobCategoryDAO;

	private Logger logger = LogManager.getLogger(JobCategoryService.class);

	public JobCategoryService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblJobCategory> getAllJobCategory() {
		logger.debug("getAllJobCategorys()");
		List<CfgTblJobCategory> jobCategorys = citTableJobCategoryDAO.getAllJobCategory();
		return jobCategorys;
	}
	
	@Override
	public List<CfgTblJobCategory> getActiveJobCategory() {
		logger.debug("getActiveJobCategorys()");
		List<CfgTblJobCategory> jobCategorys = citTableJobCategoryDAO.getActiveJobCategory();
		return jobCategorys;
	}
	
	@Override
	public String generateJobCategoryNo(String type) {
		
		return citTableJobCategoryDAO.generateJobCategoryNo(type);
		
	}
	
	@Override
	public boolean jobCategoryExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableJobCategoryDAO.getJobCategoryByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewJobCategory(CfgTblJobCategory cfgTblJobCategory) {
				
		return citTableJobCategoryDAO.addNewJobCategory(cfgTblJobCategory);
	}

	@Override
	public String updateJobCategory(CfgTblJobCategory cfgTblJobCategory) {
		
		return citTableJobCategoryDAO.updateJobCategory(cfgTblJobCategory);
	}

	@Override
	public String deleteJobCategory(List<String> jobCategorysId) {
		// TODO Auto-generated method stub
		return citTableJobCategoryDAO.deleteJobCategory(jobCategorysId);
	}
	
	@Override
	public List<CfgTblJobCategory> searchJobCategory(CfgTblJobCategory jobCategory) {
		// TODO Auto-generated method stub
		return citTableJobCategoryDAO.searchJobCategory(jobCategory);
	}

}
