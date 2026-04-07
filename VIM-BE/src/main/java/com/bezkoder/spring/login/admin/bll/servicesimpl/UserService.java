package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblUserDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;


@Service
public class UserService implements IUserService {
	
	@Autowired
	private ICfgTblUserDAO citTableUserDAO;

	private Logger logger = LogManager.getLogger(UserService.class);

	public UserService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblUser> getAllUser() {
		logger.debug("getAllUsers()");
		List<CfgTblUser> users = citTableUserDAO.getAllUser();
		return users;
	}
	
	@Override
	public List<CfgTblUser> getActiveUser() {
		logger.debug("getActiveUsers()");
		List<CfgTblUser> users = citTableUserDAO.getActiveUser();
		return users;
	}
	
	@Override
	public String generateUserNo(String type) {
		
		return citTableUserDAO.generateUserNo(type);
		
	}

	@Override
	public CfgTblUser getUserById(Integer userId) {
		return citTableUserDAO.getUserEntityById(userId);
	}
	
	@Override
	public boolean getUserByProperty(String property, String value,String mode, String oldValue) {
		return !citTableUserDAO.getUserByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewUser(CfgTblUser cfgTblUser) {
				
		return citTableUserDAO.addNewUser(cfgTblUser);
	}

	@Override
	public String updateUser(CfgTblUser cfgTblUser) {
		
		return citTableUserDAO.updateUser(cfgTblUser);
	}

	@Override
	public String deleteUser(List<String> usersId) {
		// TODO Auto-generated method stub
		return citTableUserDAO.deleteUser(usersId);
	}
	
	@Override
	public List<CfgTblUser> searchUser(CfgTblUser user) {
		// TODO Auto-generated method stub
		return citTableUserDAO.searchUser(user);
	}
	
	
	@Override
	public String userPasswordUpdate(int id,String NewPassword,String oldPassword) {
		// TODO Auto-generated method stub
		return citTableUserDAO.userPasswordUpdate(id,NewPassword,oldPassword);
	}
	
	@Override
	public String ForgetPassword(CfgTblUser cfgTblUser) {
				
		return citTableUserDAO.ForgetPassword(cfgTblUser);
	}
	
	
	
	@Override
	public List<CfgTblUser> CheckUserDuplicationForUpdate(CfgTblUser user) {
		// TODO Auto-generated method stub
		return citTableUserDAO.CheckUserDuplicationForUpdate(user);
	}
	

	@Override
	public List<CfgTblUser> getCustomerActiveUser() {
		logger.debug("getAllUsers()");
		List<CfgTblUser> users = citTableUserDAO.getCustomerActiveUser();
		return users;
	}

	@Override
	public CfgTblUser getAdminOfCurrentUserRole(Integer userId) {
		CfgTblUser users = citTableUserDAO.getAdminOfCurrentUserRole(userId);
		return users;
	}


}
