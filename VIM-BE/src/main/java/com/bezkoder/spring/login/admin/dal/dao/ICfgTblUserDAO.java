package com.bezkoder.spring.login.admin.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

public interface ICfgTblUserDAO {

	List<CfgTblUser> getAllUser();

	List<CfgTblUser> getActiveUser();

	List<CfgTblUser> getUserByProperty(String property, String value, String mode, String oldValue);

	String addNewUser(CfgTblUser CfgTblUser);

	String deleteUser(List<String> userId);

	String updateUser(CfgTblUser CfgTblUser);

	String generateUserNo(String type);

	String getUserById(String userId);
	
	List<CfgTblUser> searchUser(CfgTblUser user);
	
	String userPasswordUpdate(int id,String NewPassword,String oldPassword);

	String ForgetPassword(CfgTblUser CfgTblUser);
	
	List<CfgTblUser> CheckUserDuplicationForUpdate(CfgTblUser user);
	
	List<CfgTblUser> getCustomerActiveUser();

	CfgTblUser getAdminOfCurrentUserRole(Integer userId);
	
	
}
