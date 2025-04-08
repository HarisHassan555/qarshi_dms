package com.bezkoder.spring.login.sa.dal.dao;

import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.HrTblJob;

public interface IHrTblJobDao {
	
	List<HrTblJob> getAllJobs();

	HrTblJob addNewJob(HrTblJob job);

	String deleteJob(List<HrTblJob> listJobs);

	HrTblJob updateJob(HrTblJob job);

	HrTblJob getJobById(HrTblJob job);
	
}
