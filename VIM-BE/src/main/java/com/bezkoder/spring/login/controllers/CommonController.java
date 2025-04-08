package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.servicesimpl.CommonService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
//@RequestMapping("/api/common")
public class CommonController {
	private Logger logger = LogManager.getLogger(CommonController.class);

	@Autowired
	private CommonService commonService;

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public ModelAndView dashboardView(HttpServletRequest request, Model model, HttpServletResponse response) {
		
		
		if(commonService.getIsPasswordChange(commonService.getCurrentLoggedInUser())==true)
		{
			ModelAndView modelAndView = new ModelAndView("setup/ChangePassword");
			addPreDefinedFields(model);
			return modelAndView;
		}
		else
		{
		ModelAndView modelAndView = new ModelAndView("common/main");
		addPreDefinedFields(model);
		return modelAndView;
		}
	}

	/*@RequestMapping(value = "/login")
	public ModelAndView loginView(HttpServletRequest request, Model model, HttpServletResponse response)
			throws Exception {

		ModelAndView modelAndView = new ModelAndView("login");
		//response.sendRedirect("/Admin/ChangePassword");
		addPreDefinedFields(model);
		return modelAndView;
	}*/
	
	
	

	private void addPreDefinedFields(Model model) {
		
		model.addAttribute("user",commonService.getCurrentUserName());
		model.addAttribute("navigationMenuRoles", commonService.getNavigationMenuRoles());
		model.addAttribute("userRole", commonService.getCurrentUserRole());
	}

}
