package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IEmployeeService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblEmployeeDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployee;


@Service
public class EmployeeService implements IEmployeeService {
	
	@Autowired
	private IHrTblEmployeeDAO hrTableEmployeeDAO;

	private Logger logger = LogManager.getLogger(EmployeeService.class);

	public EmployeeService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<HrTblEmployee> getAllEmployees() {
		logger.debug("getAllEmployees()");
		List<HrTblEmployee> Employees = hrTableEmployeeDAO.getAllEmployees();
		return Employees;
	}
	
	@Override
	public List<HrTblEmployee> getActiveEmployees() {
		logger.debug("getActiveEmployees()");
		List<HrTblEmployee> Employees = hrTableEmployeeDAO.getActiveEmployees();
		return Employees;
	}
	
	@Override
	public String generateEmployeeNo(String type) {
		
		return hrTableEmployeeDAO.generateEmployeeNo(type);
		
	}
	
	@Override
	public boolean EmployeeExistByProperty(String property, String value,String mode, String oldValue) {
		return !hrTableEmployeeDAO.getEmployeeByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewEmployee(HrTblEmployee HrTblEmployee) {
				
		return hrTableEmployeeDAO.addNewEmployee(HrTblEmployee);
	}

	@Override
	public String updateEmployee(HrTblEmployee HrTblEmployee) {
		
		return hrTableEmployeeDAO.updateEmployee(HrTblEmployee);
	}

	@Override
	public String deleteEmployees(List<String> employeeId) {
		// TODO Auto-generated method stub
		return hrTableEmployeeDAO.deleteEmployees(employeeId);
	}
	
	@Override
	public List<HrTblEmployee> searchEmployee(HrTblEmployee employee) {
		// TODO Auto-generated method stub
		return hrTableEmployeeDAO.searchEmployee(employee);
	}

	@Override
	public HrTblEmployee getEmployeeByPK(int EmployeeId){
		// TODO Auto-generated method stub
		return hrTableEmployeeDAO.getEmployeeByPK(EmployeeId);
	}
}
