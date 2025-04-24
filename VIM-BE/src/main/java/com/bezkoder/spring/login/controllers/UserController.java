package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.dto.ChangePasswordDTO;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.daoimpl.LoginDAOImpl;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class UserController {

	private Logger logger = LogManager.getLogger(UserController.class);

	@Autowired
	private IUserService userService;
	
	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private LoginDAOImpl loginDaoImpl;

	@RequestMapping(value = "/getAllUser", method = RequestMethod.GET)
	public List<CfgTblUser> getAllUserAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllUseres()");
		List<CfgTblUser> users = userService.getAllUser();
		return users;
	}

	@RequestMapping(value = "/getActiveUser", method = RequestMethod.GET)
	public List<CfgTblUser> getActiveUser(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveUser()");
		List<CfgTblUser> users = userService.getActiveUser();
		return users;
	}

	

	@RequestMapping(value = "/getNewUser", method = RequestMethod.GET)
	public CfgTblUser getNewUserAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblUser user = new CfgTblUser();
		return user;
	}

	@RequestMapping(value = "/addNewUser", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> addNewUserAction(@RequestBody CfgTblUser cfgTblUser) {
		Map<String, String> response = new HashMap<>();
		try {
			List<?> lstUser = userService.searchUser(cfgTblUser);
			if (lstUser != null && !lstUser.isEmpty()) {
				response.put("status", "AX");
				return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
			} else {
				userService.addNewUser(cfgTblUser);
				response.put("status", "Success");
				return ResponseEntity.ok(response); // 200 OK
			}
		} catch (Exception ex) {
			response.put("status", "Failure");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500 Internal Server Error
		}
	}

	@RequestMapping(value = "/deleteUser", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteUserAction(@RequestBody String useresId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : useresId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return userService.deleteUser(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateUser", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateUserAction(@RequestBody CfgTblUser cfgTblUser, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List lstUser=userService.CheckUserDuplicationForUpdate(cfgTblUser);
			if(lstUser!=null && lstUser.size() > 0)
			{
				return "AX";
		}
			else
			return userService.updateUser(cfgTblUser);
		} catch (Exception ex) {
			return "Failure";
		}
	}

		@RequestMapping(value = "/getCurrentUser", method = RequestMethod.GET)
		public CfgTblUser getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
		try {
			return commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
		} catch (Exception ex) {
			return null;
		}
	}

//	

	@RequestMapping(value = "/UpdatePasswordReconfirm",   method = RequestMethod.POST)
	public String UpdatePasswordReconfirm(@RequestBody ChangePasswordDTO  jsonStr, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
         String msg;
		 msg=userService.userPasswordUpdate(jsonStr.getUserId(), jsonStr.getNewPass(),jsonStr.getOldPass());
		 System.out.println(msg);
		 return msg;

	}
	
	


		@RequestMapping(value = "/ForgetPassword", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
		public String ForgetPassword(@RequestBody CfgTblUser cfgTblUser, HttpServletRequest request,
				HttpServletResponse response) {
			try {
		
				return userService.ForgetPassword(cfgTblUser);
			} catch (Exception ex) {
				return "{\"status\":\"Failure\"}";
			}
		}
		
		
		
		@RequestMapping(value = "/getCustomerActiveUser", method = RequestMethod.GET)
		public List<CfgTblUser> getCustomerActiveUser(HttpServletRequest request, HttpServletResponse response) {
			logger.debug("getCustomerActiveUser()");
			List<CfgTblUser> users = userService.getCustomerActiveUser();
			return users;
		}
		
		
		@RequestMapping(value = "/UpdatePasswordAdmin",   method = RequestMethod.POST)
		public String UpdatePasswordAdmin(@RequestBody ChangePasswordDTO  jsonStr, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			
	         String msg;
			 msg=loginDaoImpl.userPasswordUpdatebyAdmin(jsonStr.getUserId(), jsonStr.getNewPass());
			 System.out.println(msg);
			 
			 return "Success";
//			 if(msg.equals("Success")){
//				 return "Success";
//			 }else{
//				return  "Failure";
//			 }

		}


}
