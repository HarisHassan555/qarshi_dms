package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IPaymentTermService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPaymentTerm;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class PaymentTermController {

	private Logger logger = LogManager.getLogger(PaymentTermController.class);

	@Autowired
	private IPaymentTermService paymentTermService;
	
	

	/*@RequestMapping(value = "/getAllCountries", method = RequestMethod.GET)
	public List<CfgTblPaymentTerm> getAllCountriesAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllPaymentTermes()");
		List<CfgTblPaymentTerm> paymentTerms = paymentTermService.getAllPaymentTerm();
		return paymentTerms;
	}
*/
	@RequestMapping(value = "/generatePaymentTermNo", method = RequestMethod.GET)
	public String generatePaymentTermNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return paymentTermService.generatePaymentTermNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllPaymentTerm", method = RequestMethod.GET)
	public List<CfgTblPaymentTerm> getAllPaymentTerm(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllPaymentTermes()");
		List<CfgTblPaymentTerm> paymentTerms = paymentTermService.getAllPaymentTerm();
		return paymentTerms;
	}
	
	@RequestMapping(value = "/getActivePaymentTerm", method = RequestMethod.GET)
	public List<CfgTblPaymentTerm> getActivePaymentTerm(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActivePaymentTerm()");
		List<CfgTblPaymentTerm> paymentTerms = paymentTermService.getActivePaymentTerm();
		return paymentTerms;
	}
	
	
	@RequestMapping(value = "/getNewPaymentTerm", method = RequestMethod.GET)
	public CfgTblPaymentTerm getNewPaymentTermAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblPaymentTerm paymentTerm = new CfgTblPaymentTerm();
		return paymentTerm;
	}

	@RequestMapping(value = "/addNewPaymentTerm", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewPaymentTermAction(@RequestBody CfgTblPaymentTerm citTblPaymentTerm, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return paymentTermService.addNewPaymentTerm(citTblPaymentTerm);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deletePaymentTerm", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deletePaymentTermAction(@RequestBody String paymentTermesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : paymentTermesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return paymentTermService.deletePaymentTerm(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updatePaymentTerm", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updatePaymentTermAction(@RequestBody CfgTblPaymentTerm citTblPaymentTerm, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return paymentTermService.updatePaymentTerm(citTblPaymentTerm);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/paymentTermExistByProperty", method = RequestMethod.POST)
	public String paymentTermExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return paymentTermService.paymentTermExistByProperty(property,mode,customer,oldValue)?"true":"false";
//					paymentTermExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="paymentTermBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("paymentTerm/bulk_paymentTerm");
		
			if (file.isEmpty()) {
				
				redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
	            
		    }
		 
		    String rootPath = request.getSession().getServletContext().getRealPath("/");
		    File dir = new File(rootPath + File.separator + "uploadedfile");
		    if (!dir.exists()) {
		        dir.mkdirs();
		    }
		 
		    File serverFile = new File(dir.getAbsolutePath() + File.separator + file.getOriginalFilename());
		
		   try {
		        try (InputStream is = file.getInputStream();
		                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile))) {
		            int i;
		            //write file to server
		            while ((i = is.read()) != -1) {
		                stream.write(i);
		            }
		            stream.flush();
		        }
		    } catch (IOException e) {
		    	 System.out.println("error while reading excel and put to db : " + e.getMessage());
		    }
		 
		   
		    try {
	
		    	FileInputStream excelFile = new FileInputStream(new File(serverFile.getPath().toString()));
	            Workbook workbook = new XSSFWorkbook(excelFile);
	            Sheet datatypeSheet = workbook.getSheetAt(0);
	            List<String[]> rows = new ArrayList<String[]>();
	            Row row;
	            for(int i=0; i<=datatypeSheet.getLastRowNum(); i++){
	            	 String[] tmpRows = new String[8];
	            	row = datatypeSheet.getRow(i);
	            	if(row.getRowNum()!=0){
	            	tmpRows[0]         =  row.getCell(0).getStringCellValue();
	            	tmpRows[1]      = row.getCell(1).getStringCellValue();
	            	tmpRows[2]        = row.getCell(2).getStringCellValue();
	            	tmpRows[3]   = row.getCell(3).getStringCellValue();
	            	int column4  =(int) row.getCell(4).getNumericCellValue();
	            	tmpRows[4]    = String.valueOf(column4);
	            	int column5=(int) row.getCell(5).getNumericCellValue();
	            	tmpRows[5]      = String.valueOf(column5);
	            	tmpRows[6]         = row.getCell(6).getStringCellValue();
	            	tmpRows[7]         = row.getCell(7).getStringCellValue();
	            	rows.add(tmpRows);
	            	}
	            	
	            }

	            ArrayList<String> errorRecord = null;
//	            		(ArrayList<String>) paymentTermService.uploadFile(rows);
		        	if(errorRecord==null){
		        		modelMAP.addAttribute("msg", "Success");
		        	 return modelAndView;

		        	}
		        	else 
		        		//redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
		        	modelMAP.addAttribute("msg", errorRecord);
		        		 return modelAndView;
		        		 		//+ "&status="
		        	//+URLEncoder.encode(status,"UTF-8");
		       
		       
		    } catch (IOException e) {
		        System.out.println( e.getMessage());
		    }
		    return modelAndView;
			
		   
		
	}
	
}
