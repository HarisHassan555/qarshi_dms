package com.bezkoder.spring.login.sa.dal.dao;
//
//public class ICfgTerritoryDAO {
//
//}


import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone3;
//import com.bezkoder.spring.login.sa.dal.entities.CitItemSetup;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone1;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone2;

public interface ICfgTblAccountZoneDAO {
	String addNewZone1(CfgTblZone1 cfgTblZone1);
	List<CfgTblZone1> getAllZone1();
	String deleteZone1(List<String> zone1Id);
	String updateZone1(CfgTblZone1 cfgTblZone1);
	String generateZone1Code();
	String addNewZone2(CfgTblZone2 cfgTblZone2);
	String deleteZone2(List<String> zonesId);
	String updateZone2(CfgTblZone2 cfgTblZone2);
	String generateZone2Code();
	List<CfgTblZone2> getAllZone2();
	String generateZone3Code();
	String addNewZone3(CfgTblZone3 cfgTblZone3);
	String deleteZone3(List<String> zone3Id);
	String updateZone3(CfgTblZone3 cfgTblZone3);
	List<CfgTblZone3> getAllZone3();
	List<CfgTblZone3> getAllZone3Branches();
	String generateItemCode();

	
}

