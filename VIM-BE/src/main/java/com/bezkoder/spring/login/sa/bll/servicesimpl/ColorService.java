package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IColorService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblColorDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblColor;


@Service
public class ColorService implements IColorService {
	
	@Autowired
	private ICfgTblColorDAO citTableColorDAO;

	private Logger logger = LogManager.getLogger(ColorService.class);

	public ColorService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblColor> getAllColor() {
		logger.debug("getAllColors()");
		List<CfgTblColor> colors = citTableColorDAO.getAllColor();
		return colors;
	}
	
	@Override
	public List<CfgTblColor> getActiveColor() {
		logger.debug("getActiveColors()");
		List<CfgTblColor> colors = citTableColorDAO.getActiveColor();
		return colors;
	}
	
	@Override
	public String generateColorNo(String type) {
		
		return citTableColorDAO.generateColorNo(type);
		
	}
	
	@Override
	public boolean colorExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableColorDAO.getColorByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewColor(CfgTblColor cfgTblColor) {
				
		return citTableColorDAO.addNewColor(cfgTblColor);
	}

	@Override
	public String updateColor(CfgTblColor cfgTblColor) {
		
		return citTableColorDAO.updateColor(cfgTblColor);
	}

	@Override
	public String deleteColor(List<String> colorsId) {
		// TODO Auto-generated method stub
		return citTableColorDAO.deleteColor(colorsId);
	}
	
	@Override
	public List<CfgTblColor> searchColor(CfgTblColor color) {
		// TODO Auto-generated method stub
		return citTableColorDAO.searchColor(color);
	}

}
