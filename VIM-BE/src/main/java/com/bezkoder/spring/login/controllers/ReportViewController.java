package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@JsonInclude
@RestController
public class ReportViewController {
	private Logger logger = LogManager.getLogger(ReportViewController.class);

	@Autowired
	private ICommonService commonService;
	
	@RequestMapping(value = "/AttDep")
	public ModelAndView ATRDep(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/AttDep");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/AttDesig")
	public ModelAndView AttDesig(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/AttDesig");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/AttAll")
	public ModelAndView AttAll(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/AttAll");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/AttEmp")
	public ModelAndView ATREmp(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/AttEmp");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/AttSum")
	public ModelAndView ATRSum(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/AttSum");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/Reports")
	public ModelAndView Reports(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/ReportMain");
		addPreDefinedFields(model);
		return modelAndView;
	}

	@RequestMapping(value = "/AttReport")
	public ModelAndView AttReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/billingReport");
		addPreDefinedFields(model);
		return modelAndView;
	}

	@RequestMapping(value = "/StockReport")
	public ModelAndView StockReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportWHC")
	public ModelAndView StockReportWHC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportWHC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/WithheldReport")
	public ModelAndView WithheldReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/WithheldReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
		
	@RequestMapping(value = "/LedgerReport")
	public ModelAndView LedgerReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/DCSummaryReport")
	public ModelAndView DCSummaryReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/Monthly_Truck");
		addPreDefinedFields(model);
		return modelAndView;
	}
		
	@RequestMapping(value = "/DispatchReport")
	public ModelAndView DispatchReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/DispatchCWReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	

	@RequestMapping(value = "/StockDetail")
	public ModelAndView StockDetail(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockDetailReport");
		addPreDefinedFields(model);
		return modelAndView;
	}

	@RequestMapping(value = "/SOReport")
	public ModelAndView SOReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("Reports/SOReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/DSR")
	public ModelAndView DSR(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/WHDSR");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/WHReceivec")
	public ModelAndView WHReceivec(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/WHReceive");
		addPreDefinedFields(model);
		return modelAndView;
	}

	@RequestMapping(value = "/WHIssuance")
	public ModelAndView WHIssuance(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/WHIssuance");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	/*@RequestMapping(value = "/StockReportPR")
	public ModelAndView StockReportPR(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportPR");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportPT")
	public ModelAndView StockReportPT(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportPT");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportTemper")
	public ModelAndView StockReportTemper(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportTemper");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportTechno")
	public ModelAndView StockReportTechno(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportTechno");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	
	@RequestMapping(value = "/StockReportPacking")
	public ModelAndView StockReportPacking(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportPacking");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportPRC")
	public ModelAndView StockReportPRC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportPRC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/StockReportTemperC")
	public ModelAndView StockReportTemperC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/StockReportTemperC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	
	
	@RequestMapping(value = "/LedgerReportPR")
	public ModelAndView LedgerReportPR(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportPR");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportPT")
	public ModelAndView LedgerReportPT(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportPT");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportTemper")
	public ModelAndView LedgerReportTemper(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportTemper");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportPacking")
	public ModelAndView LedgerReportPacking(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportPacking");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportTechno")
	public ModelAndView LedgerReportTechno(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportTechno");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportC")
	public ModelAndView LedgerReportC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportTemperC")
	public ModelAndView LedgerReportTemperC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportTemperC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/LedgerReportWHC")
	public ModelAndView LedgerReportWHC(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/LedgerReportWHC");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	
	
	@RequestMapping(value = "/BreakageReport")
	public ModelAndView BreakageReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/BreakageReport");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/BreakageReportPT")
	public ModelAndView BreakageReportPT(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/BreakageReportPT");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/BreakageReportTemper")
	public ModelAndView BreakageReportTemper(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/BreakageReportTemper");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/BreakageReportPacking")
	public ModelAndView BreakageReportPacking(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/BreakageReportPacking");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/ProcessMovementReportPR")
	public ModelAndView ProcessMovementReportPR(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/ProcessMovementReportPR");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/WithheldReport")
	public ModelAndView WithheldReport(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/WithheldReport");
		addPreDefinedFields(model);
		return modelAndView;
	}

	
	@RequestMapping(value = "/DPR")
	public ModelAndView DPR(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/DPR");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	
	@RequestMapping(value = "/SIList")
	public ModelAndView SIList(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/SIList");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/SIConsolidated")
	public ModelAndView SIConsolidated(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/SIConsolidated");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	
	
	
	@RequestMapping(value = "/DR_Town_DesignWise")
	public ModelAndView DR_Town_DesignWise(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/Dispatch_Town_Design");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/Dispatch_Town_Design_WOP")
	public ModelAndView Dispatch_Town_Design_WOP(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/Dispatch_Town_Design_WOP");
		addPreDefinedFields(model);
		return modelAndView;
	}
	
	@RequestMapping(value = "/Monthly_Truck")
	public ModelAndView Monthly_Truck(HttpServletRequest request, Model model, HttpServletResponse response) {
		ModelAndView modelAndView = new ModelAndView("reports/Monthly_Truck");
		addPreDefinedFields(model);
		return modelAndView;
	}
	*/
	private void addPreDefinedFields(Model model) {

		model.addAttribute("navigationMenuRoles", commonService.getNavigationMenuRoles());
		model.addAttribute("userRole", commonService.getCurrentUserRole());
	}

}
