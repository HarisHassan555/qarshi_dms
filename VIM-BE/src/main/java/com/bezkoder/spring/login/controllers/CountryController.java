package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICountryService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCountry;
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

@CrossOrigin(origins = "*")
@RestController
public class CountryController {

	private Logger logger = LogManager.getLogger(CountryController.class);

	@Autowired
	private ICountryService countryService;
	
	

	/*@RequestMapping(value = "/getAllCountries", method = RequestMethod.GET)
	public List<CfgTblCountry> getAllCountriesAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCountryes()");
		List<CfgTblCountry> countrys = countryService.getAllCountry();
		return countrys;
	}
*/
	@RequestMapping(value = "/generateCountryNo", method = RequestMethod.GET)
	public String generateCountryNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return countryService.generateCountryNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllCountry", method = RequestMethod.GET)
	public List<CfgTblCountry> getAllCountry(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCountryes()");
		List<CfgTblCountry> countrys = countryService.getAllCountry();
		return countrys;
	}
	
	@RequestMapping(value = "/getActiveCountry", method = RequestMethod.GET)
	public List<CfgTblCountry> getActiveCountry(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveCountry()");
		List<CfgTblCountry> countrys = countryService.getActiveCountry();
		return countrys;
	}
	
	
	@RequestMapping(value = "/getNewCountry", method = RequestMethod.GET)
	public CfgTblCountry getNewCountryAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblCountry country = new CfgTblCountry();
		return country;
	}

	@RequestMapping(value = "/addNewCountry", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewCountryAction(@RequestBody CfgTblCountry citTblCountry, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return countryService.addNewCountry(citTblCountry);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteCountry", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteCountryAction(@RequestBody String countryesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : countryesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return countryService.deleteCountry(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateCountry", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateCountryAction(@RequestBody CfgTblCountry citTblCountry, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return countryService.updateCountry(citTblCountry);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/countryExistByProperty", method = RequestMethod.POST)
	public String countryExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return countryService.countryExistByProperty(property,mode,customer,oldValue)?"true":"false";
//					countryExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="countryBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("country/bulk_country");
		
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
//	            		(ArrayList<String>) countryService.uploadFile(rows);
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
