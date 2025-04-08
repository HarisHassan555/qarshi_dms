package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.services.ICustomerService;
import com.bezkoder.spring.login.sa.dal.daoimpl.CfgTblCustomerDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
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
//@RequestMapping("/api/customer")
public class CustomerController {

	private Logger logger = LogManager.getLogger(CustomerController.class);

	@Autowired
	private ICustomerService customerService;
	
	 @Autowired
	  private LoginDAO loginDao;
	 
	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private CfgTblCustomerDAO customerDao;
	
	@RequestMapping(value = "/getAllCustomer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getAllCustomerAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCustomeres()");
		List<CfgTblCustomer> customers = customerService.getAllCustomer();
		return customers;
	}
	
	@RequestMapping(value = "/getActiveCustomer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getActiveCustomer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveCustomer()");
		List<CfgTblCustomer> customers = customerService.getActiveCustomer();
		return customers;
	}
	

//	@RequestMapping(value = "/generateCustomerNo", method = RequestMethod.GET)
//	public String generateCustomerNo(HttpServletRequest request,
//			HttpServletResponse response) {
//		try {
//			return customerService.generateCustomerNo("");
//		} catch (Exception ex) {
//			return "{\"status\":\"Failure\"}";
//		}
//	}
	
	@RequestMapping(value = "/generateCustomerNo", method = RequestMethod.GET)
	public String generateCustomerNo(HttpServletRequest request,
			HttpServletResponse response, String category) {
		try {
			return customerService.generateCustomerNo(category);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/generateCustomerNoNew", method = RequestMethod.GET)
	public String generateCustomerNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerService.generateCustomerNo("6");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/generateCustomerNowithType", method = RequestMethod.GET)
	public String generateCustomerNowithType(String type,HttpServletRequest request,
			HttpServletResponse response) {
		try {
			System.out.println("type----------"+type);
			return customerService.generateCustomerNo("Packing");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllCustomer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getAllCustomer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCustomer()");
		List<CfgTblCustomer> customers = customerService.getAllCustomer();
		return customers;
	}*/
	
	
	@RequestMapping(value = "/getNewCustomer", method = RequestMethod.GET)
	public CfgTblCustomer getNewCustomerAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblCustomer customer = new CfgTblCustomer();
		return customer;
	}

	@RequestMapping(value = "/addNewCustomer", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewCustomerAction(@RequestBody CfgTblCustomer citTblCustomer, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			
			
			/*	List lstUser=customerService.searchCustomer(citTblCustomer);
				if(lstUser!=null && lstUser.size() > 0)
				{
					return "AX";
			}
				else*/
			return customerService.addNewCustomer(citTblCustomer);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteCustomer", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteCustomerAction(@RequestBody String customeresId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : customeresId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return customerService.deleteCustomer(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateCustomer", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateCustomerAction(@RequestBody CfgTblCustomer citTblCustomer, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerService.updateCustomer(citTblCustomer);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/customerExistByProperty", method = RequestMethod.POST)
	public String customerExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerService.getCustomerByProperty(property, value, mode, oldValue)?"true":"false";
//					customerExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="customerBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("customer/bulk_customer");
		
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
//	            		(ArrayList<String>) customerService.uploadFile(rows);
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
	
	@RequestMapping(value = "/getAllDealer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getAllDealer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDealer()");
		List<CfgTblCustomer> customers = customerService.getAllDealer();
		return customers;
	}
	
	@RequestMapping(value = "/getActiveDealer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getActiveDealer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveDealer()");
		List<CfgTblCustomer> customers = customerService.getActiveDealer();
		return customers;
	}
	
	@RequestMapping(value = "/getCustomerWODealer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getCustomerWODealer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getCustomerWODealer()");
		List<CfgTblCustomer> customers = customerService.getCustomerWODealer();
		return customers;
	}
	
	@RequestMapping(value = "/getgroupActiveCustomer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getgroupActiveCustomer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getgroupActiveCustomer()");
		List<CfgTblCustomer> customers = customerService.getgroupActiveCustomer();
		return customers;
	}	
	
	
	@RequestMapping(value = "/getloginCustomerProfile", method = RequestMethod.GET)
	public CfgTblCustomer getloginCustomerProfile(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getloginCustomer()");

		
		CfgTblUser cfgTblUser= this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if(cfgTblUser!=null && cfgTblUser.getCfgTblCustomer()!=null)
		{
			return customerDao.getCustomerByPK(cfgTblUser.getCfgTblCustomer().getSerCustomerId().toString());
		}
		
		return null;

	}

	@RequestMapping(value = "/getAllMainDealer", method = RequestMethod.GET)
	public List<CfgTblCustomer> getAllMainDealer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllMainDealer()");
		List<CfgTblCustomer> customers = customerService.getAllMainDealer();
		return customers;
	}
	
	@RequestMapping(value = "/updateAndrePostCustomerInSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateAndrePostCustomerInSAP(@RequestBody CfgTblCustomer citTblCustomer, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return customerService.updateAndrePostCustomerInSAP(citTblCustomer);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	 
}
