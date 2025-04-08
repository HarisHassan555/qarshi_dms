package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployee;


public interface IEmployeeService {

	List<HrTblEmployee> getAllEmployees();
	
	List<HrTblEmployee> getActiveEmployees();
	
	String addNewEmployee(HrTblEmployee HrTblEmployee);

	boolean EmployeeExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteEmployees(List<String> EmployeesId);

	String updateEmployee(HrTblEmployee HrTblEmployee);
	
	String generateEmployeeNo(String type);
	
	List<HrTblEmployee> searchEmployee(HrTblEmployee employee);
	
	HrTblEmployee getEmployeeByPK(int EmployeeId);
	

}
