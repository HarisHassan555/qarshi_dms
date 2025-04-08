package com.bezkoder.spring.login.sa.dal.dao;
//
//public class ICfgTerritoryDAO {
//
//}


import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblArea;
//import com.bezkoder.spring.login.sa.dal.entities.CitItemSetup;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblRegion;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone;

public interface ICfgTerritoryDAO {
	String addNewRegion(CfgTblRegion cfgTblRegion);
	List<CfgTblRegion> getAllRegion();
	String deleteRegion(List<String> regionId);
	String updateRegion(CfgTblRegion cfgTblRegion);
	String generateRegionCode();
	String addNewZone(CfgTblZone cfgTblZone);
	String deleteZone(List<String> zonesId);
	String updateZone(CfgTblZone cfgTblZone);
	String generateZoneCode();
	List<CfgTblZone> getAllZone();
	String generateAreaCode();
	String addNewArea(CfgTblArea cfgTblArea);
	String deleteArea(List<String> areaId);
	String updateArea(CfgTblArea cfgTblArea);
	List<CfgTblArea> getAllArea();
	List<CfgTblArea> getAllAreaBranches();
	String generateItemCode();

	
}

