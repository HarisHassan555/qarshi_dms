package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;

@Service
public class SetupService {


	@Autowired
	private LoginDAO loginDao;
	

	
	public CfgTblUser userLogin(CfgTblUser appUser) {
	return	loginDao.userLogin(appUser);
	}


	
	public List<String> getUserRoles(String username) {
		// TODO Auto-generated method stub
		return loginDao.getUserRoles(username);
	}

}
	
