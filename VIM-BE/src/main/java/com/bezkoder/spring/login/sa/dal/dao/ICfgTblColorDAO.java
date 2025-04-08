package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblColor;

public interface ICfgTblColorDAO {

	List<CfgTblColor> getAllColor();

	List<CfgTblColor> getActiveColor();

	List<CfgTblColor> getColorByProperty(String property, String value, String mode, String oldValue);

	String addNewColor(CfgTblColor cfgTblColor);

	String deleteColor(List<String> customerId);

	String updateColor(CfgTblColor cfgTblColor);

	String generateColorNo(String type);

	String getColorById(String color);
	
	List<CfgTblColor> searchColor(CfgTblColor color);
}
