package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IDistributionChannelService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDistributionChannel;
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
//@RequestMapping("/api/dcc")
public class DistributionChannelController {

	private Logger logger = LogManager.getLogger(DistributionChannelController.class);

	@Autowired
	private IDistributionChannelService distributionChannelService;
	
	

	/*@RequestMapping(value = "/getAllCountries", method = RequestMethod.GET)
	public List<CfgTblDistributionChannel> getAllCountriesAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDistributionChanneles()");
		List<CfgTblDistributionChannel> distributionChannels = distributionChannelService.getAllDistributionChannel();
		return distributionChannels;
	}
*/
	@RequestMapping(value = "/generateDistributionChannelNo", method = RequestMethod.GET)
	public String generateDistributionChannelNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return distributionChannelService.generateDistributionChannelNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllDistributionChannel", method = RequestMethod.GET)
	public List<CfgTblDistributionChannel> getAllDistributionChannel(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDistributionChanneles()");
		List<CfgTblDistributionChannel> distributionChannels = distributionChannelService.getAllDistributionChannel();
		return distributionChannels;
	}
	
	@RequestMapping(value = "/getActiveDistributionChannel", method = RequestMethod.GET)
	public List<CfgTblDistributionChannel> getActiveDistributionChannel(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveDistributionChannel()");
		List<CfgTblDistributionChannel> distributionChannels = distributionChannelService.getActiveDistributionChannel();
		return distributionChannels;
	}
	
	
	@RequestMapping(value = "/getNewDistributionChannel", method = RequestMethod.GET)
	public CfgTblDistributionChannel getNewDistributionChannelAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblDistributionChannel distributionChannel = new CfgTblDistributionChannel();
		return distributionChannel;
	}

	@RequestMapping(value = "/addNewDistributionChannel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewDistributionChannelAction(@RequestBody CfgTblDistributionChannel citTblDistributionChannel, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return distributionChannelService.addNewDistributionChannel(citTblDistributionChannel);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteDistributionChannel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteDistributionChannelAction(@RequestBody String distributionChannelesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : distributionChannelesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return distributionChannelService.deleteDistributionChannel(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateDistributionChannel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDistributionChannelAction(@RequestBody CfgTblDistributionChannel citTblDistributionChannel, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return distributionChannelService.updateDistributionChannel(citTblDistributionChannel);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/distributionChannelExistByProperty", method = RequestMethod.POST)
	public String distributionChannelExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return distributionChannelService.distributionChannelExistByProperty(property,mode,customer,oldValue)?"true":"false";
//					distributionChannelExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="distributionChannelBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("distributionChannel/bulk_distributionChannel");
		
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
//	            		(ArrayList<String>) distributionChannelService.uploadFile(rows);
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
