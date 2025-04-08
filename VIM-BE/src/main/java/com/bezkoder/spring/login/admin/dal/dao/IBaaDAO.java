package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.Baa;

public interface IBaaDAO {

	List<Baa> getAllBaa();

	List<Baa> getActiveBaa();

	List<Baa> getBaaByProperty(String property, String value, String mode, String oldValue);

	String addNewBaa(Baa Baa);

	String deleteBaa(List<String> customerId);

	String updateBaa(Baa Baa);

	String generateBaaNo(String type);

	String getBaaById(String customerId);
	
	List<Baa> searchBaa(Baa baa);
}
