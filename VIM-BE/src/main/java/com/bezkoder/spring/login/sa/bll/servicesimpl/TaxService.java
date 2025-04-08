package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ITaxService;
import com.bezkoder.spring.login.sa.dal.dao.ITblTaxDAO;
import com.bezkoder.spring.login.sa.dal.entities.TblTax;


@Service
public class TaxService implements ITaxService {
	
	@Autowired
	private ITblTaxDAO dao;

	private Logger logger = LogManager.getLogger(TaxService.class);

	public TaxService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<TblTax> getAllTax() {
		logger.debug("getAllTax()");
		List<TblTax> tax = dao.getAllTax();
		return tax;
	}
	
	@Override
	public List<TblTax> getActiveTax() {
		logger.debug("getActiveTax()");
		List<TblTax> tax = dao.getActiveTax();
		return tax;
	}
	
	@Override
	public String addNewTax(TblTax tax) {
				
		return dao.addNewTax(tax);
	}

	@Override
	public String updateTax(TblTax tax) {
		
		return dao.updateTax(tax);
	}

	@Override
	public String deleteTax(List<String> id) {
		// TODO Auto-generated method stub
		return dao.deleteTax(id);
	}
	

}
