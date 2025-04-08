package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;


import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployeeType;

public interface IEmployeeTypeService {
	List<HrTblEmployeeType> getAllEmployeeTypes();

	HrTblEmployeeType addNewEmployeeType(HrTblEmployeeType hrTblEmployeeType);

	String deleteEmployeeType(List<HrTblEmployeeType> listEmployeeTypes);

	HrTblEmployeeType updateEmployeeType(HrTblEmployeeType hrTblEmployeeType);

	HrTblEmployeeType getEmployeeTypeById(HrTblEmployeeType hrTblEmployeeType );
}
