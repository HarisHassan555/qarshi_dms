package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblPostServiceFU;

public interface ISlsTblPostServiceFUDAO {

	List<SlsTblPostServiceFU> getAllPostServiceFU();

	List<SlsTblPostServiceFU> getActivePostServiceFU();

	List<SlsTblPostServiceFU> getPostServiceFUByProperty(String property, String value, String mode, String oldValue);

	String addNewPostServiceFU(SlsTblPostServiceFU slsTblPostServiceFU);

	String deletePostServiceFU(List<String> customerId);

	String updatePostServiceFU(SlsTblPostServiceFU slsTblPostServiceFU);

	String generatePostServiceFUNo(String type);

	String getPostServiceFUById(String id);
	
	SlsTblPostServiceFU getPostServiceFUByPK(int PostServiceFUId);
	
	List<SlsTblPostServiceFU> searchPostServiceFU(SlsTblPostServiceFU SlsTblPostServiceFU);
	
	public String serivcetoaddPostServiceFUFromthread();
}
