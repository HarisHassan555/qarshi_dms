package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPhenomenon;

public interface ICfgTblPhenomenonDAO {

	List<CfgTblPhenomenon> getAllPhenomenon();

	List<CfgTblPhenomenon> getActivePhenomenon();

	List<CfgTblPhenomenon> getPhenomenonByProperty(String property, String value, String mode, String oldValue);

	String addNewPhenomenon(CfgTblPhenomenon cfgTblPhenomenon);

	String deletePhenomenon(List<String> id);

	String updatePhenomenon(CfgTblPhenomenon cfgTblPhenomenon);

	String generatePhenomenonNo(String type);

	String getPhenomenonById(String phenomenon);
	
	List<CfgTblPhenomenon> searchPhenomenon(CfgTblPhenomenon phenomenon);
}
