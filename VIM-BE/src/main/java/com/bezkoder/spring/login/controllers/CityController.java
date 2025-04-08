package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICityService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;
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
//@RequestMapping("/api/cities")
public class CityController {

	private Logger logger = LogManager.getLogger(CityController.class);

	@Autowired
	private ICityService cityService;
	
	
//	,produces=MediaType.APPLICATION_XML_VALUE

	@RequestMapping(value = "/getAllCity")
	public List<CfgTblCity> getAllCityAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCityes()");
		List<CfgTblCity> citys = cityService.getAllCity();
		
		
		return citys;
	}
	
	@RequestMapping(value = "/getActiveCity", method = RequestMethod.GET)
	public List<CfgTblCity> getActiveCity(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveCity()");
		List<CfgTblCity> citys = cityService.getActiveCity();
		return citys;
	}
	
	
	@RequestMapping(value = "/getTestCity", produces=MediaType.APPLICATION_XML_VALUE)
	public CfgTblCity getTestCity(HttpServletRequest request, HttpServletResponse response) {
		CfgTblCity cfgTblCity=new CfgTblCity();
		cfgTblCity.setTxtCityCode("123");
		cfgTblCity.setTxtCityName("Lahore");
		return cfgTblCity;
	}
	
	
	@RequestMapping(value = "/getTestCityxml", method = RequestMethod.POST,headers = "Accept=application/xml",consumes = MediaType.APPLICATION_XML_VALUE, produces=MediaType.APPLICATION_XML_VALUE)
	public CfgTblCity getTestCityxml(CfgTblCity city) {
		CfgTblCity cfgTblCity=new CfgTblCity();
		cfgTblCity.setTxtCityCode(city.getTxtCityCode());
		cfgTblCity.setTxtCityName(city.getTxtCityName());
		return city;
	}
	

	@RequestMapping(value = "/generateCityNo", method = RequestMethod.GET)
	public String generateCityNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return cityService.generateCityNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllCity", method = RequestMethod.GET)
	public List<CfgTblCity> getAllCity(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllCity()");
		List<CfgTblCity> citys = cityService.getAllCity();
		return citys;
	}*/
	
	
	@RequestMapping(value = "/getNewCity", method = RequestMethod.GET)
	public CfgTblCity getNewCityAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblCity city = new CfgTblCity();
		return city;
	}

	@RequestMapping(value = "/addNewCity", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewCityAction(@RequestBody CfgTblCity citTblCity, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return cityService.addNewCity(citTblCity);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteCity", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteCityAction(@RequestBody String cityesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : cityesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return cityService.deleteCity(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateCity", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateCityAction(@RequestBody CfgTblCity citTblCity, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return cityService.updateCity(citTblCity);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/cityExistByProperty", method = RequestMethod.POST)
	public String cityExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return cityService.getCityByProperty(property, value, mode, oldValue)?"true":"false";
//					cityExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="cityBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("city/bulk_city");
		
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
//	            		(ArrayList<String>) cityService.uploadFile(rows);
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
