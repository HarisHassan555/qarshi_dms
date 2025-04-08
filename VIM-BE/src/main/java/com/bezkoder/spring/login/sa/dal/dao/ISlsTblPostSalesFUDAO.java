package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblPostSalesFU;

public interface ISlsTblPostSalesFUDAO {

	List<SlsTblPostSalesFU> getAllPostSalesFU();

	List<SlsTblPostSalesFU> getActivePostSalesFU();

	List<SlsTblPostSalesFU> getPostSalesFUByProperty(String property, String value, String mode, String oldValue);

	String addNewPostSalesFU(SlsTblPostSalesFU slsTblPostSalesFU);

	String deletePostSalesFU(List<String> customerId);

	String updatePostSalesFU(SlsTblPostSalesFU slsTblPostSalesFU);

	String generatePostSalesFUNo(String type);

	String getPostSalesFUById(String id);
	
	List<SlsTblPostSalesFU> searchPostSalesFU(SlsTblPostSalesFU SlsTblPostSalesFU);
	
	public String serivcetoaddPostSalesFUFromthread();
}
