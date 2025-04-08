package com.bezkoder.spring.login.controllers;


import com.bezkoder.spring.login.sa.bll.services.ITaxService;
import com.bezkoder.spring.login.sa.dal.entities.TblTax;
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
@CrossOrigin(origins = "*")
public class TaxController {

	private Logger logger = LogManager.getLogger(TaxController.class);

	@Autowired
	private ITaxService taxService;
	
	
	@RequestMapping(value = "/getAllTax", method = RequestMethod.GET)
	public List<TblTax> getAllTax(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllTax()");
		List<TblTax> tax = taxService.getAllTax();
		
		
		return tax;
	}
	
	@RequestMapping(value = "/getActiveTax", method = RequestMethod.GET)
	public List<TblTax> getActiveTax(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveTax()");
		List<TblTax> tax = taxService.getActiveTax();
		
		
		return tax;
	}
	


	@RequestMapping(value = "/addNewTax", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewTaxAction(@RequestBody TblTax tax, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return taxService.addNewTax(tax);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteTax", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteTaxAction(@RequestBody String Id, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : Id.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return taxService.deleteTax(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateTax", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateCityAction(@RequestBody TblTax tax, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return taxService.updateTax(tax);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	
}
