package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDesignation;

public interface IHrTblDesignationDAO {

	List<HrTblDesignation> getAllDesignations();

	List<HrTblDesignation> getActiveDesignations();

	List<HrTblDesignation> getDesignationByProperty(String property, String value, String mode, String oldValue);

	String addNewDesignation(HrTblDesignation hrTblDesignation);

	String deleteDesignations(List<String> DesignationId);

	String updateDesignation(HrTblDesignation hrTblDesignation);

	String generateDesignationNo(String type);

	String getDesignationById(String DesignationId);
	
	List<HrTblDesignation> searchDesignation(HrTblDesignation designation);
}
