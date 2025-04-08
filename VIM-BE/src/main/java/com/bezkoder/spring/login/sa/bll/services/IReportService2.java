package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;
import java.util.Map;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;



public interface IReportService2 {

		 
	 List<Map<String, Object>> getAttendenceReport(ReportDTO dto);
	 
	 List<Map<String, Object>> getStockReportatProcess(ReportDTO dto);
		
		List<Map<String, Object>> getStockLedgerReportatProcess(ReportDTO dto);
		
		 List<Map<String, Object>> getSaleOrderReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getDCReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getInvoiceReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getTranferReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getDisptachReportCustomerWise(ReportDTO dto);
		 
		 List<Map<String, Object>> getBreakgeReportProcessWise(ReportDTO dto);
		 
		 List<Map<String, Object>> getProcessDetailReportProcessWise(ReportDTO dto);
		 
		 List<Map<String, Object>> getDPRReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getStockDetailReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getDispatchSummaryReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getWHReceivedReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getWHIssuanceReport(ReportDTO dto);
		 
	     List<Map<String, Object>> getSaleInvoiceListReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getConsolidatedSalesSummaryReport(ReportDTO dto);
		 
		 public List<Map<String, Object>> getDCSummaryReport(ReportDTO dto);
		 
		 public List<Map<String, Object>> getDCSummary_groupReport(ReportDTO dto);
		 
		 public List<Map<String, Object>> getGILPaymentReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getJOBCardReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getTIRReport(ReportDTO dto);
		 
		 List<Map<String, Object>> getToolReport(ReportDTO dto);
	
	}
