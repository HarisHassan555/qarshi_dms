package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IDepartmentService;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;
import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
//@RequestMapping("/api/department")
public class DepartmentController {

	
	
	private Logger logger = LogManager.getLogger(DepartmentController.class);

	@Autowired
	private IDepartmentService departmentService;
	
	@Autowired
	private IUserService userService;
	
	

	/*@RequestMapping(value = "/getAllDepartments", method = RequestMethod.GET)
	public List<HrTblDepartment> getAllDepartmentsAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDepartmentes()");
		List<HrTblDepartment> departments = departmentService.getAllDepartments();
		return departments;
	}*/

	@RequestMapping(value = "/generateDepartmentNo", method = RequestMethod.GET)
	public String generateDepartmentNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return departmentService.generateDepartmentNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllDepartments", method = RequestMethod.GET)
	public List<HrTblDepartment> getAllDepartments(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDepartments()");
		List<HrTblDepartment> departments = departmentService.getAllDepartments();
		return departments;
	}
	
	
	@RequestMapping(value = "/getNewDepartment", method = RequestMethod.GET)
	public HrTblDepartment getNewDepartmentAction(HttpServletRequest request, HttpServletResponse response) {
		HrTblDepartment department = new HrTblDepartment();
		return department;
	}

	@RequestMapping(value = "/addNewDepartment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewDepartmentAction(@RequestBody HrTblDepartment citTblDepartment, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return departmentService.addNewDepartment(citTblDepartment);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteDepartment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteDepartmentAction(@RequestBody String departmentesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : departmentesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return departmentService.deleteDepartments(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateDepartment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDepartmentAction(@RequestBody HrTblDepartment citTblDepartment, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return departmentService.updateDepartment(citTblDepartment);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/departmentExistByProperty", method = RequestMethod.POST)
	public String departmentExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return departmentService.DepartmentExistByProperty(property, value, mode, oldValue)?"true":"false";
//					departmentExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/getUsersByDepartment", method = RequestMethod.GET)
	public List<CfgTblUser> getUsersByDepartment(@RequestParam Integer departmentId, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("getUsersByDepartment() - departmentId: " + departmentId);
		try {
			List<CfgTblUser> allUsers = userService.getAllUser();
			List<CfgTblUser> departmentUsers = new ArrayList<>();
			
			for (CfgTblUser user : allUsers) {
				if (user.getHrTblDepartment() != null && 
					user.getHrTblDepartment().getSerDepartmentId() != null &&
					user.getHrTblDepartment().getSerDepartmentId().equals(departmentId)) {
					departmentUsers.add(user);
				}
			}
			
			return departmentUsers;
		} catch (Exception ex) {
			logger.error("Error getting users by department: " + ex.getMessage(), ex);
			return new ArrayList<>();
		}
	}
	
	@RequestMapping(value = "/assignUsersToDepartment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String assignUsersToDepartment(@RequestBody Map<String, Object> requestBody, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("assignUsersToDepartment()");
		try {
			Integer departmentId = (Integer) requestBody.get("departmentId");
			@SuppressWarnings("unchecked")
			List<Integer> userIds = (List<Integer>) requestBody.get("userIds");
			
			if (departmentId == null) {
				return "{\"status\":\"Failure\",\"message\":\"Department ID is required\"}";
			}
			
			if (userIds == null || userIds.isEmpty()) {
				return "{\"status\":\"Failure\",\"message\":\"User IDs are required\"}";
			}
			
			// Get the department
			HrTblDepartment department = null;
			List<HrTblDepartment> departments = departmentService.getAllDepartments();
			for (HrTblDepartment dept : departments) {
				if (dept.getSerDepartmentId().equals(departmentId)) {
					department = dept;
					break;
				}
			}
			
			if (department == null) {
				return "{\"status\":\"Failure\",\"message\":\"Department not found\"}";
			}
			
			// Get all users and update those in the list
			List<CfgTblUser> allUsers = userService.getAllUser();
			int updatedCount = 0;
			
			for (CfgTblUser user : allUsers) {
				if (userIds.contains(user.getSerUserId())) {
					// Assign user to department
					user.setHrTblDepartment(department);
					userService.updateUser(user);
					updatedCount++;
				} else if (user.getHrTblDepartment() != null && 
						   user.getHrTblDepartment().getSerDepartmentId() != null &&
						   user.getHrTblDepartment().getSerDepartmentId().equals(departmentId)) {
					// Remove user from department if not in the new list
					user.setHrTblDepartment(null);
					userService.updateUser(user);
				}
			}
			
			return "{\"status\":\"Success\",\"message\":\"" + updatedCount + " user(s) assigned to department\"}";
		} catch (Exception ex) {
			logger.error("Error assigning users to department: " + ex.getMessage(), ex);
			return "{\"status\":\"Failure\",\"message\":\"" + ex.getMessage() + "\"}";
		}
	}
	
	
	@RequestMapping(value="departmentBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("department/bulk_department");
		
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
//	            		(ArrayList<String>) departmentService.uploadFile(rows);
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
