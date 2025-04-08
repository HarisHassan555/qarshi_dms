package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.HrTblJob;

public interface IJobService {
	
	List<HrTblJob> getAllJobs();

	HrTblJob addNewJob(HrTblJob job);

	String deleteJob(List<HrTblJob> job);

	HrTblJob updateJob(HrTblJob job);

	HrTblJob getJobById(HrTblJob hrTblEmployeeType);
	
}
