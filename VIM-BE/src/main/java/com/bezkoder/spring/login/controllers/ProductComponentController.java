package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.IProductComponentService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductComponent;
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
public class ProductComponentController {

	private Logger logger = LogManager.getLogger(ProductComponentController.class);

	@Autowired
	private IProductComponentService productComponentService;
	
	

	@RequestMapping(value = "/getAllProductComponent", method = RequestMethod.GET)
	public List<CfgTblProductComponent> getAllProductComponentAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductComponentes()");
		List<CfgTblProductComponent> productComponents = productComponentService.getAllProductComponent();
		return productComponents;
	}
	
	@RequestMapping(value = "/searchProductComponent", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<CfgTblProductComponent> searchProductComponent(@RequestBody CfgTblProductComponent cfgTblProductComponent, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			System.out.println("cfgTblProductComponent--------:"+cfgTblProductComponent);
			
			return productComponentService.searchProductComponent(cfgTblProductComponent);
//					productComponentService.updateProductComponent(citTblProductComponent);
		} catch (Exception ex) {
			return null;
		}
	}
	

	@RequestMapping(value = "/generateProductComponentNo", method = RequestMethod.GET)
	public String generateProductComponentNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productComponentService.generateProductComponentNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllProductComponent", method = RequestMethod.GET)
	public List<CfgTblProductComponent> getAllProductComponent(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductComponent()");
		List<CfgTblProductComponent> productComponents = productComponentService.getAllProductComponent();
		return productComponents;
	}*/
	
	
	@RequestMapping(value = "/getNewProductComponent", method = RequestMethod.GET)
	public CfgTblProductComponent getNewProductComponentAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblProductComponent productComponent = new CfgTblProductComponent();
		return productComponent;
	}
	

	@RequestMapping(value = "/addNewProductComponent", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductComponent(@RequestBody List<CfgTblProductComponent> lstcfgTblProductComponents,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			if(lstcfgTblProductComponents!=null)
			System.out.println("----componentList---------"+lstcfgTblProductComponents.size());
			
			if(lstcfgTblProductComponents!=null && lstcfgTblProductComponents.size() >0)
			{
				return productComponentService.addNewProductComponentinList(lstcfgTblProductComponents);
			}
			
			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	

	/*@RequestMapping(value = "/addNewProductComponent", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductComponentAction(@RequestBody CfgTblProductComponent citTblProductComponent, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productComponentService.addNewProductComponent(citTblProductComponent);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}*/

	@RequestMapping(value = "/deleteProductComponent", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteProductComponentAction(@RequestBody String productComponentesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : productComponentesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return productComponentService.deleteProductComponent(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateProductComponent", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateProductComponentAction(@RequestBody CfgTblProductComponent citTblProductComponent, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productComponentService.updateProductComponent(citTblProductComponent);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	@RequestMapping(value = "/productComponentExistByProperty", method = RequestMethod.POST)
	public String productComponentExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productComponentService.getProductComponentByProperty(property, value, mode, oldValue)?"true":"false";
//					productComponentExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	

}
