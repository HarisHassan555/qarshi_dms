package com.bezkoder.spring.login.sa.bll.servicesimpl;

//public class TerritiryService {
//
//}

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ITerritotyService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTerritoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblArea;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblRegion;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone;
@Service
public class TerritoryService implements ITerritotyService {

	@Autowired
private ICfgTerritoryDAO citTerritoryDAO;



	private Logger logger = LogManager.getLogger(TerritoryService.class);
	@Override
	public String addNewRegion(CfgTblRegion cfgTblRegion) {
		if (cfgTblRegion.getSerRegionId() == 0) {
			
		}
		return citTerritoryDAO.addNewRegion(cfgTblRegion);
	}

	@Override
	public List<CfgTblRegion> getAllRegion() {
		logger.debug("getAllRegion()");
		List<CfgTblRegion> region = citTerritoryDAO.getAllRegion();
		return region;
	}

	@Override
	public String deleteRegion(List<String> regionId) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.deleteRegion(regionId);
	}

	@Override
	public String updateRegion(CfgTblRegion cfgTblRegion) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.updateRegion(cfgTblRegion);
	}

	@Override
	public String generateRegionCode() {
		// TODO Auto-generated method stub
		return citTerritoryDAO.generateRegionCode();
	}

	@Override
	public String addNewZone(CfgTblZone cfgTblZone) {
       
	/*	if (cfgTblZone.getSerZoneId() == 0) {
			
		}*/
		return citTerritoryDAO.addNewZone(cfgTblZone);
	}

	@Override
	public String deleteZone(List<String> zonesId) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.deleteZone(zonesId);
	}

	@Override
	public String updateZone(CfgTblZone cfgTblZone) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.updateZone(cfgTblZone);
	}
	
	@Override
	public String generateZoneCode() {
		// TODO Auto-generated method stub
		return citTerritoryDAO.generateZoneCode();
	}

	@Override
	public List<CfgTblZone> getAllZone() {
		logger.debug("getAllZone()");
		List<CfgTblZone> zone = citTerritoryDAO.getAllZone();
		return zone;
	}
	
	@Override
	public String generateAreaCode() {
		// TODO Auto-generated method stub
		return citTerritoryDAO.generateAreaCode();
	}
	
	
	@Override
	public String addNewArea(CfgTblArea cfgTblArea) {
       
	/*	if (cfgTblZone.getSerZoneId() == 0) {
			
		}*/
		return citTerritoryDAO.addNewArea(cfgTblArea);
	}

	@Override
	public String deleteArea(List<String> areaId) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.deleteArea(areaId);
	}

	@Override
	public String updateArea(CfgTblArea cfgTblArea) {
		// TODO Auto-generated method stub
		return citTerritoryDAO.updateArea(cfgTblArea);
	}
	
	@Override
	public List<CfgTblArea> getAllArea() {
		logger.debug("getAllRegion()");
		List<CfgTblArea> area = citTerritoryDAO.getAllArea();
		return area;
	}
	@Override
	public List<CfgTblArea> getAllAreaBranches() {
		logger.debug("getAllRegion()");
		List<CfgTblArea> area = citTerritoryDAO.getAllAreaBranches();
		return area;
	}
	

	
	@Override
	public String generateItemCode() {
		// TODO Auto-generated method stub
		return citTerritoryDAO.generateItemCode();
	}

	
}
