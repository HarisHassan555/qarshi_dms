package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IJobService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblJobDao;
import com.bezkoder.spring.login.sa.dal.entities.HrTblJob;

@Service
public class JobService implements IJobService {

	@Autowired
	IHrTblJobDao jobDao;
	
	@Override
	public List<HrTblJob> getAllJobs() {
 		return jobDao.getAllJobs();
	}

	@Override
	public HrTblJob addNewJob(HrTblJob job) {
 		return jobDao.addNewJob(job);
	}

	@Override
	public String deleteJob(List<HrTblJob> listJobs) {
 		return jobDao.deleteJob(listJobs);
	}

	@Override
	public HrTblJob updateJob(HrTblJob job) {
 		return updateJob(job);
	}

	@Override
	public HrTblJob getJobById(HrTblJob job) {
 		return getJobById(job);
	}

}
