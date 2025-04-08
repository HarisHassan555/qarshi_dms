package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.Baa;


public interface IBaaService {

	List<Baa> getAllBaa();
	
	List<Baa> getActiveBaa();
	
	String addNewBaa(Baa baa);

	boolean getBaaByProperty(String property, String value, String mode, String oldValue);
	
	String deleteBaa(List<String> baasId);

	String updateBaa(Baa baa);
	
	String generateBaaNo(String type);
	
	List<Baa> searchBaa(Baa baa);
	

}
