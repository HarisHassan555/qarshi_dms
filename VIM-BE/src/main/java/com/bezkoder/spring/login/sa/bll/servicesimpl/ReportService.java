package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;
import com.bezkoder.spring.login.sa.bll.services.IReportService;
import com.bezkoder.spring.login.sa.dal.dao.IReportDAO;

@Service
public class ReportService implements IReportService {

	@Autowired
	private IReportDAO reportDAO;

	private Logger logger = LogManager.getLogger(ReportService.class);

	public ReportService() {
		// TODO Auto-generated constructor stub
	}

	

	@Override
	public  List<Map<String, Object>> getAttendenceReport(ReportDTO dto) {
		logger.debug("getAttendenceReport()");
		List<Map<String, Object>> ledgers = reportDAO.getAttendenceReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getStockReportatProcess(ReportDTO dto) {
		logger.debug("getStockReportatProcess()");
		List<Map<String, Object>> ledgers = reportDAO.getStockReportatProcess(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getStockLedgerReportatProcess(ReportDTO dto) {
		logger.debug("getStockLedgerReportatProcess()");
		List<Map<String, Object>> ledgers = reportDAO.getStockLedgerReportatProcess(dto);
		return ledgers;
	}

	@Override
	public List<Map<String, Object>> getSaleOrderReport(ReportDTO dto) {
		logger.debug("getSaleOrderReport()");
		List<Map<String, Object>> ledgers = reportDAO.getSaleOrderReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getDCReport(ReportDTO dto) {
		logger.debug("getDCReport()");
		List<Map<String, Object>> ledgers = reportDAO.getDCReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getInvoiceReport(ReportDTO dto) {
		logger.debug("getInvoiceReport()");
		List<Map<String, Object>> ledgers = reportDAO.getInvoiceReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getTranferReport(ReportDTO dto) {
		logger.debug("getTranferReport()");
		List<Map<String, Object>> ledgers = reportDAO.getTranferReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getDisptachReportCustomerWise(ReportDTO dto) {
		logger.debug("getDisptachReportCustomerWise()");
		List<Map<String, Object>> ledgers = reportDAO.getDisptachReportCustomerWise(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getBreakgeReportProcessWise(ReportDTO dto) {
		logger.debug("getBreakgeReportProcessWise()");
		List<Map<String, Object>> ledgers = reportDAO.getBreakgeReportProcessWise(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getProcessDetailReportProcessWise(ReportDTO dto) {
		logger.debug("getProcessDetailReportProcessWise()");
		List<Map<String, Object>> ledgers = reportDAO.getProcessDetailReportProcessWise(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getDPRReport(ReportDTO dto) {
		logger.debug("getDPRReport()");
		List<Map<String, Object>> ledgers = reportDAO.getDPRReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getStockDetailReport(ReportDTO dto) {
		logger.debug("getStockDetailReport()");
		List<Map<String, Object>> ledgers = reportDAO.getStockDetailReport(dto);
		return ledgers;
	}
	

	@Override
	public  List<Map<String, Object>> getDispatchSummaryReport(ReportDTO dto) {
		logger.debug("getDispatchSummaryReport()");
		List<Map<String, Object>> ledgers = reportDAO.getDispatchSummaryReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getWHReceivedReport(ReportDTO dto) {
		logger.debug("getWHReceivedReport()");
		List<Map<String, Object>> ledgers = reportDAO.getWHReceivedReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getWHIssuanceReport(ReportDTO dto) {
		logger.debug("getWHIssuanceReport()");
		List<Map<String, Object>> ledgers = reportDAO.getWHIssuanceReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getSaleInvoiceListReport(ReportDTO dto) {
		logger.debug("getSaleInvoiceListReport()");
		List<Map<String, Object>> ledgers = reportDAO.getSaleInvoiceListReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getConsolidatedSalesSummaryReport(ReportDTO dto) {
		logger.debug("getConsolidatedSalesSummaryReport()");
		List<Map<String, Object>> ledgers = reportDAO.getConsolidatedSalesSummaryReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getDCSummaryReport(ReportDTO dto) {
		logger.debug("getDCSummaryReport()");
		List<Map<String, Object>> ledgers = reportDAO.getDCSummaryReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getDCSummary_groupReport(ReportDTO dto) {
		logger.debug("getDCSummary_groupReport()");
		List<Map<String, Object>> ledgers = reportDAO.getDCSummary_groupReport(dto);
		return ledgers;
	}
	
	@Override
	public  List<Map<String, Object>> getGILPaymentReport(ReportDTO dto) {
		logger.debug("getGILPaymentReport()");
		List<Map<String, Object>> payments = reportDAO.getGILPaymentReport(dto);
		return payments;
	}
	
	@Override
	public List<Map<String, Object>> getJOBCardReport(ReportDTO dto) {
		logger.debug("getJOBCardReport()");
		List<Map<String, Object>> ledgers = reportDAO.getJOBCardReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getTIRReport(ReportDTO dto) {
		logger.debug("getTIRReport()");
		List<Map<String, Object>> ledgers = reportDAO.getTIRReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getToolReport(ReportDTO dto) {
		logger.debug("getTIRReport()");
		List<Map<String, Object>> ledgers = reportDAO.getToolReport(dto);
		return ledgers;
	}
	
	@Override
	public List<Map<String, Object>> getClaimReport(ReportDTO dto) {
		logger.debug("getClaimReport()");
		List<Map<String, Object>> ledgers = reportDAO.getClaimReport(dto);
		return ledgers;
	}
	
}