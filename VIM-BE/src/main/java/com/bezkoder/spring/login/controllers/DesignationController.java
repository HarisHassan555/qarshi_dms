package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IDesignationService;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDesignation;
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
//@RequestMapping("/api/designation")
public class DesignationController {

	private Logger logger = LogManager.getLogger(DesignationController.class);

	@Autowired
	private IDesignationService designationService;
	


	@RequestMapping(value = "/getAllDesignations", method = RequestMethod.GET)
	public List<HrTblDesignation> getAllDesignationsAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDesignationes()");
		List<HrTblDesignation> designations = designationService.getAllDesignations();
		return designations;
	}

	@RequestMapping(value = "/generateDesignationNo", method = RequestMethod.GET)
	public String generateDesignationNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return designationService.generateDesignationNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllDesignations", method = RequestMethod.GET)
	public List<HrTblDesignation> getAllDesignations(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDesignations()");
		List<HrTblDesignation> designations = designationService.getAllDesignations();
		return designations;
	}*/
	
	
	@RequestMapping(value = "/getNewDesignation", method = RequestMethod.GET)
	public HrTblDesignation getNewDesignationAction(HttpServletRequest request, HttpServletResponse response) {
		HrTblDesignation designation = new HrTblDesignation();
		return designation;
	}

	@RequestMapping(value = "/addNewDesignation", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewDesignationAction(@RequestBody HrTblDesignation citTblDesignation, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return designationService.addNewDesignation(citTblDesignation);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteDesignation", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteDesignationAction(@RequestBody String designationesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : designationesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return designationService.deleteDesignations(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateDesignation", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDesignationAction(@RequestBody HrTblDesignation citTblDesignation, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return designationService.updateDesignation(citTblDesignation);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/designationExistByProperty", method = RequestMethod.POST)
	public String designationExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return designationService.DesignationExistByProperty(property, value, mode, oldValue)?"true":"false";
//					designationExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="designationBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("designation/bulk_designation");
		
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
//	            		(ArrayList<String>) designationService.uploadFile(rows);
		        	if(errorRecord.isEmpty()){
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
