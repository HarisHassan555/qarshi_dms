package com.bezkoder.spring.login.sa.dal.dao;

import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployeeType;

import java.util.List;


public interface IHrTblEmployeeTypeDao {

	List<HrTblEmployeeType> getAllEmployeeTypes();

	HrTblEmployeeType addNewEmployeeType(HrTblEmployeeType hrTblEmployeeType);

	String deleteEmployeeType(List<HrTblEmployeeType> listEmployeeTypes);

	HrTblEmployeeType updateEmployeeType(HrTblEmployeeType hrTblEmployeeType);

	HrTblEmployeeType getEmployeeTypeById(HrTblEmployeeType hrTblEmployeeType);
		
}
