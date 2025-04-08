package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblTrouble;

public interface ICfgTblTroubleDAO {

	List<CfgTblTrouble> getAllTrouble();

	List<CfgTblTrouble> getActiveTrouble();

	List<CfgTblTrouble> getTroubleByProperty(String property, String value, String mode, String oldValue);

	String addNewTrouble(CfgTblTrouble cfgTblTrouble);

	String deleteTrouble(List<String> customerId);

	String updateTrouble(CfgTblTrouble cfgTblTrouble);

	String generateTroubleNo(String type);

	String getTroubleById(String trouble);
	
	List<CfgTblTrouble> searchTrouble(CfgTblTrouble trouble);
}
