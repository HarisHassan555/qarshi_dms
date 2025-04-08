package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IDivisionService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDivision;
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
//@RequestMapping("/api/division")
public class DivisionController {

	private Logger logger = LogManager.getLogger(DivisionController.class);

	@Autowired
	private IDivisionService divisionService;
	
	

	/*@RequestMapping(value = "/getAllCountries", method = RequestMethod.GET)
	public List<CfgTblDivision> getAllCountriesAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDivisiones()");
		List<CfgTblDivision> divisions = divisionService.getAllDivision();
		return divisions;
	}
*/
	@RequestMapping(value = "/generateDivisionNo", method = RequestMethod.GET)
	public String generateDivisionNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return divisionService.generateDivisionNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllDivision", method = RequestMethod.GET)
	public List<CfgTblDivision> getAllDivision(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDivisiones()");
		List<CfgTblDivision> divisions = divisionService.getAllDivision();
		return divisions;
	}
	
	@RequestMapping(value = "/getActiveDivision", method = RequestMethod.GET)
	public List<CfgTblDivision> getActiveDivision(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveDivision()");
		List<CfgTblDivision> divisions = divisionService.getActiveDivision();
		return divisions;
	}
	
	
	@RequestMapping(value = "/getNewDivision", method = RequestMethod.GET)
	public CfgTblDivision getNewDivisionAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblDivision division = new CfgTblDivision();
		return division;
	}

	@RequestMapping(value = "/addNewDivision", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewDivisionAction(@RequestBody CfgTblDivision citTblDivision, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return divisionService.addNewDivision(citTblDivision);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteDivision", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteDivisionAction(@RequestBody String divisionesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : divisionesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return divisionService.deleteDivision(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateDivision", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDivisionAction(@RequestBody CfgTblDivision citTblDivision, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return divisionService.updateDivision(citTblDivision);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/divisionExistByProperty", method = RequestMethod.POST)
	public String divisionExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return divisionService.divisionExistByProperty(property,mode,customer,oldValue)?"true":"false";
//					divisionExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="divisionBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("division/bulk_division");
		
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
//	            		(ArrayList<String>) divisionService.uploadFile(rows);
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
