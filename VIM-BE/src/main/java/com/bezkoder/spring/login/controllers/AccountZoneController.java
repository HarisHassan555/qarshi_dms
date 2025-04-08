/*

package com.bezkoder.spring.login.controllers;

//public class AccountZoneController { // //}

import com.bezkoder.spring.login.sa.dal.dao.ICfgTblAccountZoneDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone1;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone2;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone3;
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
public class AccountZoneController {
	private Logger logger = LogManager.getLogger(TerritorySetupController.class);

	@Autowired
	private ICfgTblAccountZoneDAO territoryService;

	@RequestMapping(value = "/getAllZone1", method = RequestMethod.GET)
	public List<CfgTblZone1> getAllZone1(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllZone2()");

		List<CfgTblZone1> zone1 = territoryService.getAllZone1();
		return zone1;
	}

	@RequestMapping(value = "/getAllZone2", method = RequestMethod.GET)
	public List<CfgTblZone2> getAllZone2(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllZone2()");

		List<CfgTblZone2> zone2 = territoryService.getAllZone2();
		return zone2;
	}
	
	@RequestMapping(value = "/getAllZone3", method = RequestMethod.GET)
	public List<CfgTblZone3> getAllZone3(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllZone3()");

		List<CfgTblZone3> zone2 = territoryService.getAllZone3();
		return zone2;
	}

	@RequestMapping(value = "/getZone1Code", method = RequestMethod.GET)
	public CfgTblZone2 getZone1Code(HttpServletRequest request, HttpServletResponse response) {
		CfgTblZone2 zone21 = new CfgTblZone2();
		zone21.setTxtZone2Code(territoryService.generateZone2Code());
		return zone21;

	}

	@RequestMapping(value = "/getZone2Code", method = RequestMethod.GET)
	public CfgTblZone2 getZone2Code(HttpServletRequest request, HttpServletResponse response) {
		CfgTblZone2 zone2 = new CfgTblZone2();
		zone2.setTxtZone2Code(territoryService.generateZone2Code());
		return zone2;

	}

	@RequestMapping(value = "/getZone3Code", method = RequestMethod.GET)
	public CfgTblZone3 getZone3Code(HttpServletRequest request, HttpServletResponse response) {
		CfgTblZone3 zone3 = new CfgTblZone3();
		zone3.setTxtZone3Code(territoryService.generateZone3Code());
		return zone3;

	}


	@RequestMapping(value = "/getAllZone3Branches", method = RequestMethod.GET)
	public List<CfgTblZone3> getAllZone3Branches(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllZone3()");

		List<CfgTblZone3> zone3 = territoryService.getAllZone3Branches();
		return zone3;
	}

	@RequestMapping(value = "/addNewZone1", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String addZone2(@RequestBody CfgTblZone1 citTableZone1, HttpServletRequest request,
			HttpServletResponse response) {
		try {

			return territoryService.addNewZone1(citTableZone1);

		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	*/
/*
	 * @RequestMapping(value = "/deleteZone1", method = RequestMethod.POST, headers
	 * = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	 * public String deleteZone1(@RequestBody String zone21Id, HttpServletRequest
	 * request, HttpServletResponse response) { try { List<String> idList = new
	 * ArrayList<String>(); for (String id : zone21Id.split(",")) { if
	 * (id.isEmpty()) { continue;
	 * 
	 * } idList.add(id); } return territoryService.deleteZone1(idList); } catch
	 * (Exception ex) { logger.error(ex.getMessage(), ex); return "Failure"; } }
	 *//*


	@RequestMapping(value = "/updateZone1", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateZone1(@RequestBody CfgTblZone1 citZone2Setup, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return territoryService.updateZone1(citZone2Setup);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	@RequestMapping(value = "/addNewZone2", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String addZone1(@RequestBody CfgTblZone2 cfgTblZone2, HttpServletRequest request,
			HttpServletResponse response) {
		try {

			return territoryService.addNewZone2(cfgTblZone2);

		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteZone2", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteZone2(@RequestBody String zone2sId, HttpServletRequest request, HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : zone2sId.split(",")) {
				if (id.isEmpty()) {
					continue;

				}
				idList.add(id);
			}
			return territoryService.deleteZone2(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateZone2", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateZone2(@RequestBody CfgTblZone2 cfgTblZone2, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return territoryService.updateZone2(cfgTblZone2);
		} catch (Exception ex) {
			return "Failure";
		}
	}

	// Zone3 Setup

	@RequestMapping(value = "/addNewZone3", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody String addZone3(@RequestBody CfgTblZone3 cfgTblZone3, HttpServletRequest request,
			HttpServletResponse response) {
		try {

			return territoryService.addNewZone3(cfgTblZone3);

		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteZone3", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteZone3(@RequestBody String zone3Id, HttpServletRequest request, HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : zone3Id.split(",")) {
				if (id.isEmpty()) {
					continue;

				}
				idList.add(id);
			}
			return territoryService.deleteZone3(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateZone3", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateZone3(@RequestBody CfgTblZone3 cfgTblZone3, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return territoryService.updateZone3(cfgTblZone3);
		} catch (Exception ex) {
			return "Failure";
		}
	}

}
*/
