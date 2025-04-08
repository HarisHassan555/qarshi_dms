package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import java.util.Map;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;



public interface IDBDAO {


	List<Map<String, Object>> getJobCardDAO(ReportDTO dto);
	
	List<Map<String, Object>> getJobCardSummaryDAO(ReportDTO dto);
	
	List<Map<String, Object>> getJobCardSummaryMonthwise(ReportDTO dto);
	
	
	
	public List<Map<String, Object>> getComplaintsSummaryMonthwise(ReportDTO dto);
	
	public List<Map<String, Object>> getComplaintsSummaryDepartmentwise(ReportDTO dto);
	
	public List<Map<String, Object>> getComplaintsSummaryStatuswise(ReportDTO dto);
	
	public List<Map<String, Object>> getComplaintsSummaryDealerwise(ReportDTO dto);
	
	
	
	public List<Map<String, Object>> getClaimSummary(ReportDTO dto);
	
	public List<Map<String, Object>> getClaimSummarybyApproval(ReportDTO dto);
	
	public List<Map<String, Object>> getClaimSummarybyMonth(ReportDTO dto) ;
	
}
