package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomerCategoryService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomerCategory;
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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
//@RequestMapping("/api/customer-category")
public class CustomerCategoryController {

	private Logger logger = LogManager.getLogger(CustomerCategoryController.class);

	@Autowired
	private ICustomerCategoryService customerCategoryService;
	
	


	@RequestMapping(value = "/getAllCustomerCategory", method = RequestMethod.GET)
	public List<CfgTblCustomerCategory> getAllCustomerCategoryAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCustomerCategoryes()");
		List<CfgTblCustomerCategory> customerCategorys = customerCategoryService.getAllCustomerCategory();
		return customerCategorys;
	}
	
	@RequestMapping(value = "/getActiveCustomerCategory", method = RequestMethod.GET)
	public List<CfgTblCustomerCategory> getActiveCustomerCategory(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveCustomerCategory()");
		List<CfgTblCustomerCategory> customerCategorys = customerCategoryService.getActiveCustomerCategory();
		return customerCategorys;
	}

	@RequestMapping(value = "/generateCustomerCategoryNo", method = RequestMethod.GET)
	public String generateCustomerCategoryNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerCategoryService.generateCustomerCategoryNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllCustomerCategory", method = RequestMethod.GET)
	public List<CfgTblCustomerCategory> getAllCustomerCategory(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCustomerCategory()");
		List<CfgTblCustomerCategory> customerCategorys = customerCategoryService.getAllCustomerCategory();
		return customerCategorys;
	}*/
	
	
	@RequestMapping(value = "/getNewCustomerCategory", method = RequestMethod.GET)
	public CfgTblCustomerCategory getNewCustomerCategoryAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblCustomerCategory customerCategory = new CfgTblCustomerCategory();
		return customerCategory;
	}

	@RequestMapping(value = "/addNewCustomerCategory", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewCustomerCategoryAction(@RequestBody CfgTblCustomerCategory citTblCustomerCategory, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerCategoryService.addNewCustomerCategory(citTblCustomerCategory);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteCustomerCategory", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteCustomerCategoryAction(@RequestBody String customerCategoryesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : customerCategoryesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return customerCategoryService.deleteCustomerCategory(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateCustomerCategory", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateCustomerCategoryAction(@RequestBody CfgTblCustomerCategory citTblCustomerCategory, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerCategoryService.updateCustomerCategory(citTblCustomerCategory);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/customerCategoryExistByProperty", method = RequestMethod.POST)
	public String customerCategoryExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerCategoryService.getCustomerCategoryByProperty(property, value, mode, oldValue)?"true":"false";
//					customerCategoryExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="customerCategoryBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("customerCategory/bulk_customerCategory");
		
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
//	            		(ArrayList<String>) customerCategoryService.uploadFile(rows);
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
