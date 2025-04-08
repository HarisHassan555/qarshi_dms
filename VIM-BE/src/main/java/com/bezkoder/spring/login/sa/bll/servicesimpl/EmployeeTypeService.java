package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import com.bezkoder.spring.login.sa.bll.services.IEmployeeTypeService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblEmployeeTypeDao;
import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployeeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeTypeService implements IEmployeeTypeService {

	@Autowired
	IHrTblEmployeeTypeDao employeeTypeDao;
	
	@Override
	public List<HrTblEmployeeType> getAllEmployeeTypes() {
		return employeeTypeDao.getAllEmployeeTypes();
	}

	@Override
	public HrTblEmployeeType addNewEmployeeType(HrTblEmployeeType hrTblEmployeeType) {
		return employeeTypeDao.addNewEmployeeType(hrTblEmployeeType);
	}

	@Override
	public String deleteEmployeeType(List<HrTblEmployeeType> listEmployeeTypes) {
		return employeeTypeDao.deleteEmployeeType(listEmployeeTypes);
	}

	@Override
	public HrTblEmployeeType updateEmployeeType(HrTblEmployeeType hrTblEmployeeType) {
		return employeeTypeDao.updateEmployeeType(hrTblEmployeeType);
	}

	@Override
	public HrTblEmployeeType getEmployeeTypeById(HrTblEmployeeType hrTblEmployeeType) {
		return employeeTypeDao.getEmployeeTypeById(hrTblEmployeeType);
	}

}
