package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;

public interface IHrTblDepartmentDAO {

	List<HrTblDepartment> getAllDepartments();

	List<HrTblDepartment> getActiveDepartments();

	List<HrTblDepartment> getDepartmentByProperty(String property, String value, String mode, String oldValue);

	String addNewDepartment(HrTblDepartment hrTblDepartment);

	String deleteDepartments(List<String> DepartmentId);

	String updateDepartment(HrTblDepartment hrTblDepartment);

	String generateDepartmentNo(String type);

	String getDepartmentById(String DepartmentId);
	
	List<HrTblDepartment> searchDepartment(HrTblDepartment department);
}
