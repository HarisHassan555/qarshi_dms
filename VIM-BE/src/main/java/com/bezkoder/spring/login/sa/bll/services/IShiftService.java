package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.HrTblShiftInfo;


public interface IShiftService {

	List<HrTblShiftInfo> getAllShiftInfos();
	
	List<HrTblShiftInfo> getActiveShiftInfos();
	
	String addNewShiftInfo(HrTblShiftInfo cfgTblShiftInfo);

	boolean ShiftInfoExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteShiftInfos(List<String> ShiftInfosId);

	String updateShiftInfo(HrTblShiftInfo cfgTblShiftInfo);
	
	String generateShiftInfoNo(String type);
	
	List<HrTblShiftInfo> searchShiftInfo(HrTblShiftInfo shiftInfo);
	

}
