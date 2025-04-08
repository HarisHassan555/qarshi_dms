package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployee;

public interface IHrTblEmployeeDAO {

	List<HrTblEmployee> getAllEmployees();

	List<HrTblEmployee> getActiveEmployees();

	List<HrTblEmployee> getEmployeeByProperty(String property, String value, String mode, String oldValue);

	String addNewEmployee(HrTblEmployee hrTblEmployee);

	String deleteEmployees(List<String> EmployeeId);

	String updateEmployee(HrTblEmployee hrTblEmployee);

	String generateEmployeeNo(String type);

	String getEmployeeById(String EmployeeId);
	
	List<HrTblEmployee> searchEmployee(HrTblEmployee employee);
	
	HrTblEmployee getEmployeeByPK(int EmployeeId);
}
