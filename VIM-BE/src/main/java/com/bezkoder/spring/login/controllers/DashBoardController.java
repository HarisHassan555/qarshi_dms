package com.bezkoder.spring.login.controllers;/*
package com.bezkoder.spring.login.sa.controller;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.Copies;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;
import com.bezkoder.spring.login.admin.bll.dto.DashBoardDto;
import com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.services.IReportService;
import com.bezkoder.spring.login.sa.dal.dao.IDashBoardDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.util.UtilDateAndTime;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;

@RestController
public class DashBoardController {

	private Logger logger = LogManager.getLogger(DashBoardController.class);

	@Autowired
	private IReportService reportService;
	
	
	@Autowired
	private IDashBoardDAO dashboardDAO;
	

	@Autowired
	private ICommonService commonService;

//String id_128J= " Tariq Glass Industries Limited.\n 128-J Model Town,\n Lahore, Pakistan\n E-Mail: info@tariqglass.com\n Tel: (0092 - 42) 111343434\n Fax: (0092 - 42) 585 7692";
//String id_factory= " abc\n 33 Km Lahore/Sheikhupura   \n Road, Pakistan\n Tel: +92 056 3500635-7";
//String id_118d= " Tariq Glass Industries Limited.\n 118-D Model Town,\n Lahore, Pakistan\n E-Mail: info@tariqfloatglass.com\n Tel: (0092 - 42) 111 000 660\n Fax: (0092 - 42) 3585 7169-70";
//	String id_factory= " 106- Stadium Road Sargodha\n eatelyrestaurant@gmail.com  \n Tel: +92 048 3768750";

	String id_factory = " F-3, Hub Chauki Road, S.I.T.E., Karachi-75730, Pakistan\nPhone: +92-21-111-445-111, 32560083-6 Toll Free: 0800-11190   \nTax No.: +92-21-32566093,2564458\n Email: info@gil.com.pk, sales@gil.com.pk, marketing@gil.com.pk";

//	@RequestMapping(value="/generateServicesReport",method=RequestMethod.GET)
//	public void generateComplaintReport(String dateFrom, String dateTo,String area, HttpServletRequest request, HttpServletResponse response){

	String path = ("" + getClass().getResource("ReportController.class"));

	
//	@RequestMapping(value = "/getClaimReport", method = RequestMethod.GET)
//	public void getClaimReport(int ser_sale_order_id, String Report_name, HttpServletRequest request,
//			HttpServletResponse response) {
//		Map<String, Object> m = new HashMap<String, Object>();
//		m.put("companyis", id_factory);
//		System.out.println("------------path--------:" + path);
//		try {
//			m.put("image", "" + new ClassPathResource("reports/logo.png").getFile());
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//
//		System.out.println("Product list report is  called ");
//
//		try {
//			ReportDTO dto = new ReportDTO();
//			dto.setSer_claim_id(ser_sale_order_id);
//
//			List<Map<String, Object>> claim = reportService.getClaimReport(dto);
//
//			if (Report_name != null && !(Report_name.trim().length() > 0))
//				Report_name = "FreeServiceClaimReport.jasper";
//
//			JRDataSource jdataSource = new JRBeanCollectionDataSource(claim);
//
//			JasperPrint jasperPrint = JasperFillManager.fillReport(
//					new ClassPathResource("reports/" + Report_name).getFile().getAbsolutePath(), m, jdataSource);
//			byte[] byteStream = JasperExportManager.exportReportToPdf(jasperPrint);
//			OutputStream outStream = response.getOutputStream();
//			response.setHeader("Content-Disposition", "filename=myReport.pdf");
//			response.setContentType("application/pdf");
//			response.setContentLength(byteStream.length);
//			outStream.write(byteStream, 0, byteStream.length);
//
//		} catch (JRException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}
	
	
	@RequestMapping(value = "/getTopMostUsedServices", method = RequestMethod.GET)
	public  List<DashBoardDto>  getClaimReport( HttpServletRequest request,	HttpServletResponse response) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("companyis", id_factory);
		System.out.println("------------path--------:" + path);
		try {
			m.put("image", "" + new ClassPathResource("reports/logo.png").getFile());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

//		    m.put("image",path+"logo.JPG");
		System.out.println("Product list report is  called ");
//		    m.put("companyname",companyName());
//		    m.put("image",path+"logo.JPG");

		try {
			DashBoardDto dto = new DashBoardDto();
			
			 List<DashBoardDto> lstRecords= dashboardDAO.getTopMostUsedServices(10);
			 for(DashBoardDto dtoRet : lstRecords)
			 {
				 System.out.println(dtoRet.getSerId() +" - "+dto.getTxtName()+" - "+dto.getCount());
			 }
return lstRecords;
//			dto.setSer_claim_id(ser_sale_order_id);

//			List<Map<String, Object>> claim = reportService.getClaimReport(dto);

//			if (Report_name != null && !(Report_name.trim().length() > 0))
//				Report_name = "FreeServiceClaimReport.jasper";

//			JRDataSource jdataSource = new JRBeanCollectionDataSource(claim);
//
//			JasperPrint jasperPrint = JasperFillManager.fillReport(
//					new ClassPathResource("reports/" + Report_name).getFile().getAbsolutePath(), m, jdataSource);
//			byte[] byteStream = JasperExportManager.exportReportToPdf(jasperPrint);
//			OutputStream outStream = response.getOutputStream();
//			response.setHeader("Content-Disposition", "filename=myReport.pdf");
//			response.setContentType("application/pdf");
//			response.setContentLength(byteStream.length);
//			outStream.write(byteStream, 0, byteStream.length);

		} 
//		catch (JRException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
		catch (Exception e) {
			e.printStackTrace();
			
		}
		return null;

	}
	
	
	@RequestMapping(value = "/getTopMostUsedSpareParts", method = RequestMethod.GET)
	public List<DashBoardDto> getTopMostUsedSpareParts(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getTopMostUsedSpareParts()");
		List<DashBoardDto> spareParts = dashboardDAO.getTopMostUsedSpareParts(10);
		
		
		return spareParts;
	}
	
	
	@RequestMapping(value = "/getRevenueGenerationFromPartsMonthNDealerWise", method = RequestMethod.GET)
	public List<DashBoardRevenueDto>  getRevenueGenerationFromPartsMonthNDealerWise( String dte_date_from, String dte_date_to, 
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getTopMostUsedSpareParts()");
		Date dfrom = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		Date dTo = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_to);
		
		List<DashBoardRevenueDto> spareParts = dashboardDAO.getRevenueGenerationFromPartsMonthNDealerWise(dfrom, dTo);
		
		
		return spareParts;
	}
	
	@RequestMapping(value = "/getRevenueGenerationFromServiceMonthlyNDealerWise", method = RequestMethod.GET)
	public List<DashBoardRevenueDto>  getRevenueGenerationFromServiceMonthlyNDealerWise( String dte_date_from, String dte_date_to, 
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getRevenueGenerationFromServiceMonthlyNDealerWise()");
		Date dfrom = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		Date dTo = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		
		List<DashBoardRevenueDto> spareParts = dashboardDAO.getRevenueGenerationFromServiceMonthlyNDealerWise(dfrom, dTo);
		
		
		return spareParts;
	}
	
	
	@RequestMapping(value = "/getTopDefectWisePhenomenCount", method = RequestMethod.GET)
	public List<DashBoardDto>  getTopDefectWisePhenomenCount( String dte_date_from, String dte_date_to, 
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getTopDefectWisePhenomenCount()");
		Date dfrom = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		Date dTo = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		
		List<DashBoardDto> spareParts = dashboardDAO.getTopDefectWisePhenomenCount(10);
		
		
		return spareParts;
	}
	
	
	@RequestMapping(value = "/getWarrantyCostMonthWise", method = RequestMethod.GET)
	public List<DashBoardRevenueDto>  getWarrantyCostMonthWise( String dte_date_from, String dte_date_to, 
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getWarrantyCostMonthWise()");
		Date dfrom = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		Date dTo = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		
		List<DashBoardRevenueDto> spareParts = dashboardDAO.getWarrantyCostMonthWise(dfrom, dTo);
		
		
		return spareParts;
	}
	
	@RequestMapping(value = "/getCustomerRetension", method = RequestMethod.GET)
	public List<DashBoardRevenueDto>  getCustomerRetension( String dte_date_from, String dte_date_to, 
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getWarrantyCostMonthWise()");
		Date dfrom = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		Date dTo = UtilDateAndTime.ddmmyyyyStringToDate(dte_date_from);
		
		List<DashBoardRevenueDto> spareParts = dashboardDAO.getCustomerRetension(dfrom, dTo);
		
		
		return spareParts;
	}
	
	
	
	
	
	@RequestMapping(value="/searchDBData",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleOrder> searchSaleOrderAction(@RequestBody ReportDTO dto,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
//		List<SlsTblSaleOrder> saleOrders = saleOrderService.searchSaleOrder(slsTblSaleOrder);
		return null;
		
	}
	
}
*/
