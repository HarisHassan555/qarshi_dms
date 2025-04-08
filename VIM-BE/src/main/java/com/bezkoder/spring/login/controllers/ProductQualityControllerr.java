package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IProductQualityService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;
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
public class ProductQualityControllerr {

	private Logger logger = LogManager.getLogger(ProductQualityControllerr.class);

	@Autowired
	private IProductQualityService productQualityService;
	
	

	@RequestMapping(value = "/getAllProductQuality", method = RequestMethod.GET)
	public List<CfgTblProductQuality> getAllProductQualityAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductQualityes()");
		List<CfgTblProductQuality> productQualitys = productQualityService.getAllProductQuality();
		return productQualitys;
	}
	
	@RequestMapping(value = "/getActiveProductQuality", method = RequestMethod.GET)
	public List<CfgTblProductQuality> getActiveProductQuality(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getActiveProductQuality()");
		List<CfgTblProductQuality> productQualitys = productQualityService.getActiveProductQuality();
		return productQualitys;
	}

	@RequestMapping(value = "/generateProductQualityNo", method = RequestMethod.GET)
	public String generateProductQualityNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productQualityService.generateProductQualityNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllProductQuality", method = RequestMethod.GET)
	public List<CfgTblProductQuality> getAllProductQuality(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductQuality()");
		List<CfgTblProductQuality> productQualitys = productQualityService.getAllProductQuality();
		return productQualitys;
	}*/
	
	
	@RequestMapping(value = "/getNewProductQuality", method = RequestMethod.GET)
	public CfgTblProductQuality getNewProductQualityAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblProductQuality productQuality = new CfgTblProductQuality();
		return productQuality;
	}

	@RequestMapping(value = "/addNewProductQuality", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductQualityAction(@RequestBody CfgTblProductQuality citTblProductQuality, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productQualityService.addNewProductQuality(citTblProductQuality);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteProductQuality", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteProductQualityAction(@RequestBody String productQualityesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : productQualityesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return productQualityService.deleteProductQuality(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateProductQuality", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateProductQualityAction(@RequestBody CfgTblProductQuality citTblProductQuality, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productQualityService.updateProductQuality(citTblProductQuality);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/productQualityExistByProperty", method = RequestMethod.POST)
	public String productQualityExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productQualityService.getProductQualityByProperty(property, value, mode, oldValue)?"true":"false";
//					productQualityExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
}
