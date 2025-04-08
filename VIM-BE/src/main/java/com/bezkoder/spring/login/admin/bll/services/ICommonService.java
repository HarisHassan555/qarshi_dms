package com.bezkoder.spring.login.admin.bll.services;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bezkoder.spring.login.admin.bll.dto.NavigationMenuRoles;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;


public interface ICommonService {
	
	RestTemplate getRestTemplate();
	
	ObjectMapper getMapper();
	
	//PfServerProperties getServerProperties();
	
	int getCurrentLoggedInUser();
	
	DateFormat getDateFormater();
	
	String getCurrentUserRole();

	//Map<String, List<Visit>> getConfirmRejectOrNewVisits();

	public boolean isSyncEmployeeCompleted();

	public void setSyncEmployeeCompleted(boolean syncEmployeeCompleted);

	List<NavigationMenuRoles> getNavigationMenuRoles();

	void removeNavigationMenuRoles();
	
	int getCurrentUserRegionId();

	Boolean isCurrentUserRegionHead();

	String getRegionBasedQuery(String tableColumnName,Boolean startAnd,Boolean endAnd);

	int getCurrentUserVoId();

	String getCurrentTimeStamp();
	
	Timestamp getCurrentTimeStamp_new();
	
	String getCurrentUserName();
	
	boolean getIsPasswordChange(int id);
	
	CfgTblUser getCurrentUser(int id);

	List<String> getAddressesBasedOnRoleHierarchy(CfgTblUser currentUser);
}
