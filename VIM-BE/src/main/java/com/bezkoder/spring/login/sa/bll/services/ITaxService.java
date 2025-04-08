package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.TblTax;


public interface ITaxService {

	List<TblTax> getAllTax();

	List<TblTax> getActiveTax();

	String addNewTax(TblTax dto);

	String deleteTax(List<String> id);

	String updateTax(TblTax dto);

}
