package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.IProviderAuthenticationService;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin( origins = "*" )
public class ProviderAuthenticationController {

	private Logger logger = LogManager.getLogger(ProviderAuthenticationController.class);

	@Autowired
	private IProviderAuthenticationService providerAuthenticationService;

	@RequestMapping(value = "/getAllProviderAuthentication", method = RequestMethod.GET)
	public List<ProviderAuthentication> getAllProviderAuthenticationAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProviderAuthenticationes()");
		List<ProviderAuthentication> providerAuthentications = providerAuthenticationService.getAllProviderAuthentication();
		return providerAuthentications;
	}

	@RequestMapping(value = "/getActiveProviderAuthentication", method = RequestMethod.GET)
	public List<ProviderAuthentication> getActiveProviderAuthentication(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveProviderAuthentication()");
		List<ProviderAuthentication> providerAuthentications = providerAuthenticationService.getActiveProviderAuthentication();
		return providerAuthentications;
	}

	@RequestMapping(value = "/generateProviderAuthenticationNo", method = RequestMethod.GET)
	public String generateProviderAuthenticationNo(HttpServletRequest request, HttpServletResponse response) {
		try {
			return providerAuthenticationService.generateProviderAuthenticationNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/getNewProviderAuthentication", method = RequestMethod.GET)
	public ProviderAuthentication getNewProviderAuthenticationAction(HttpServletRequest request, HttpServletResponse response) {
		ProviderAuthentication providerAuthentication = new ProviderAuthentication();
		return providerAuthentication;
	}

	
	
	@RequestMapping(value = "/addNewProviderAuthenitication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProviderAuthenitication(@RequestBody ProviderAuthentication citTblProviderAuthentication, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return providerAuthenticationService.addNewProviderAuthenitication(citTblProviderAuthentication);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteProviderAuthentication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteProviderAuthenticationAction(@RequestBody String providerAuthenticationesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : providerAuthenticationesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return providerAuthenticationService.deleteProviderAuthentication(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateProviderAuthentication", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateProviderAuthenticationAction(@RequestBody ProviderAuthentication citTblProviderAuthentication, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return providerAuthenticationService.updateProviderAuthentication(citTblProviderAuthentication);
		} catch (Exception ex) {
			return "Failure";
		}
	}


}
