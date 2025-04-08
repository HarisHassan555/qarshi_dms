package com.bezkoder.spring.login.admin.dal.dao;

import java.util.List;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

public interface LoginDAO {

	CfgTblUser userLogin(CfgTblUser appUser); 
	
	List<String> getUserRoles(String username);
	
	 String userPasswordUpdate(int id,String NewPassword,String oldPassword);
	 
	 public CfgTblUser getUserInformation(int appUserId);
}
