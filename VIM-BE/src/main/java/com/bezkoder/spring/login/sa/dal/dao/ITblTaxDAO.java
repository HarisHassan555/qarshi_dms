package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.TblTax;

public interface ITblTaxDAO {

	List<TblTax> getAllTax();

	List<TblTax> getActiveTax();

	String addNewTax(TblTax dto);

	String deleteTax(List<String> id);

	String updateTax(TblTax dto);

	
}
