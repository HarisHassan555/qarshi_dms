package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.HrTblShiftInfo;

public interface IHrTblShiftInfoDAO {

	List<HrTblShiftInfo> getAllShiftInfos();

	List<HrTblShiftInfo> getActiveShiftInfos();

	List<HrTblShiftInfo> getShiftInfoByProperty(String property, String value, String mode, String oldValue);

	String addNewShiftInfo(HrTblShiftInfo hrTblShiftInfo);

	String deleteShiftInfos(List<String> ShiftInfoId);

	String updateShiftInfo(HrTblShiftInfo hrTblShiftInfo);

	String generateShiftInfoNo(String type);

	String getShiftInfoById(String ShiftInfoId);
	
	List<HrTblShiftInfo> searchShiftInfo(HrTblShiftInfo shift);
}
