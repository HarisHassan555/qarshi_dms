package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDepartmentService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblDepartmentDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;


@Service
public class DepartmentService implements IDepartmentService {
	
	@Autowired
	private IHrTblDepartmentDAO hrTableDepartmentDAO;

	private Logger logger = LogManager.getLogger(DepartmentService.class);

	public DepartmentService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<HrTblDepartment> getAllDepartments() {
		logger.debug("getAllDepartments()");
		List<HrTblDepartment> Departments = hrTableDepartmentDAO.getAllDepartments();
		return Departments;
	}
	
	@Override
	public List<HrTblDepartment> getActiveDepartments() {
		logger.debug("getActiveDepartments()");
		List<HrTblDepartment> Departments = hrTableDepartmentDAO.getActiveDepartments();
		return Departments;
	}
	
	@Override
	public String generateDepartmentNo(String type) {
		
		return hrTableDepartmentDAO.generateDepartmentNo(type);
		
	}
	
	@Override
	public boolean DepartmentExistByProperty(String property, String value,String mode, String oldValue) {
		return !hrTableDepartmentDAO.getDepartmentByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDepartment(HrTblDepartment HrTblDepartment) {
				
		return hrTableDepartmentDAO.addNewDepartment(HrTblDepartment);
	}

	@Override
	public String updateDepartment(HrTblDepartment HrTblDepartment) {
		
		return hrTableDepartmentDAO.updateDepartment(HrTblDepartment);
	}

	@Override
	public String deleteDepartments(List<String> departmentId) {
		// TODO Auto-generated method stub
		return hrTableDepartmentDAO.deleteDepartments(departmentId);
	}
	
	@Override
	public List<HrTblDepartment> searchDepartment(HrTblDepartment department) {
		// TODO Auto-generated method stub
		return hrTableDepartmentDAO.searchDepartment(department);
	}

}
