package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblColor;


public interface IColorService {

	List<CfgTblColor> getAllColor();
	
	List<CfgTblColor> getActiveColor();
	
	String addNewColor(CfgTblColor cfgTblColor);

	boolean colorExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteColor(List<String> colorsId);

	String updateColor(CfgTblColor cfgTblColor);
	
	String generateColorNo(String type);
	
	List<CfgTblColor> searchColor(CfgTblColor color);
	

}
