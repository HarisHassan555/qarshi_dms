package com.bezkoder.spring.login.admin.bll.services;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;


public interface IUserService {

	List<CfgTblUser> getAllUser();
	
	List<CfgTblUser> getActiveUser();
	
	String addNewUser(CfgTblUser cfgTblUser);

	boolean getUserByProperty(String property, String value, String mode, String oldValue);
	
	String deleteUser(List<String> usersId);

	String updateUser(CfgTblUser cfgTblUser);
	
	String generateUserNo(String type);

	CfgTblUser getUserById(Integer userId);
	
	List<CfgTblUser> searchUser(CfgTblUser user);
	
	String userPasswordUpdate(int id,String NewPassword,String oldPassword);
	
	String ForgetPassword(CfgTblUser cfgTblUser);
	
	List<CfgTblUser> CheckUserDuplicationForUpdate(CfgTblUser user);
	
	List<CfgTblUser> getCustomerActiveUser();

	CfgTblUser getAdminOfCurrentUserRole(Integer userId);
	

}
