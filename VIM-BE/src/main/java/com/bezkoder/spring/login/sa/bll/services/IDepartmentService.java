package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;


public interface IDepartmentService {

	List<HrTblDepartment> getAllDepartments();
	
	List<HrTblDepartment> getActiveDepartments();
	
	String addNewDepartment(HrTblDepartment cfgTblDepartment);

	boolean DepartmentExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDepartments(List<String> DepartmentsId);

	String updateDepartment(HrTblDepartment cfgTblDepartment);
	
	String generateDepartmentNo(String type);
	
	List<HrTblDepartment> searchDepartment(HrTblDepartment department);
	

}
