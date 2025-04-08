package com.bezkoder.spring.login.admin.utility.common;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * @author Muhammad Khalil
 * 
 */
public final class GlobalUtility {

	private static final Logger			logger			= LogManager.getLogger(GlobalUtility.class);

	private static ApplicationContext	ctx				= null;

	//private static String				userRealName	= "";

	//private static String				userRealID		= "";
	
//	private static String				userType	= "";
//
//	private static String				userBaseLocation		= "";
//	
//	private static String				userBranch		= "";


	private GlobalUtility() {

	}

	public static void clearUserInfo() {
		//userRealName = "";
		//userRealID = "";
	}

	public static ApplicationContext getApplicationContext() {

		if (ctx == null) {
			// need to look for other options
			ctx = new ClassPathXmlApplicationContext("applicationcontext.xml");
			
			return ctx;
		}
		else {
			return ctx;
		}
	}

/*	public static String getTableName(String tableAlias) {

		String tableName = "";
		tableName = ResourceBundleUtility.getInstance().getString(ResourceBundleUtility.TABLE_NAMES_RESOURCE_BUNDLE,
				tableAlias);
		return tableName;
	}*/

	public static String getCurrentLoggedInUser() {

		return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
	}

	public static String getCurrentLoggedInUserRole() {
		// GrantedAuthority grantedAuthority = (GrantedAuthority)
		// (SecurityContextHolder.getContext().getAuthentication().getAuthorities().toArray())[0];
		Object[] arrayList = (SecurityContextHolder.getContext().getAuthentication().getAuthorities().toArray());
		GrantedAuthority grantedAuthority = (GrantedAuthority) arrayList[0];
		return grantedAuthority.getAuthority();
	}

	/*public static UserInfo getUserInfoFromSession() {

		if (userInfo == null) {
			ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			Object object = attr.getRequest().getSession().getAttribute("LOGGEDIN_USER_INFO");
			userInfo = object != null ? (UserInfo) object : null;
		}
		return userInfo;
	}*/
	
	public static void clearSession() {

			ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			attr.getRequest().getSession().invalidate();
	}
	
	

	/*public static String getUserRealName() {


		 UserInfo userInfo = null; ServletRequestAttributes attr =
		 * (ServletRequestAttributes)
		 * RequestContextHolder.currentRequestAttributes(); Object object =
		 * attr.getRequest().getSession().getAttribute("LOGGEDIN_USER_INFO");
		 * userInfo = object != null ? (UserInfo) object : null; 
		return userRealName;

	}*/

	/**
	 * @param userRealName
	 *            the userRealName to set
	 */
/*	public static void setUserRealName(String userRealName) {
		GlobalUtility.userRealName = userRealName;
	}*/

/*	public static String getUserRealID() {


		 UserInfo userInfo = null; ServletRequestAttributes attr =
		 * (ServletRequestAttributes)
		 * RequestContextHolder.currentRequestAttributes(); Object object =
		 * attr.getRequest().getSession().getAttribute("LOGGEDIN_USER_INFO");
		 * userInfo = object != null ? (UserInfo) object : null; 
		return userRealID;

	}
*/
	/**
	 * @param userRealID
	 *            the userRealID to set
	 */
/*	public static void setUserRealID(String userRealID) {
		GlobalUtility.userRealID = userRealID;
	}*/

/*	public static String getCurrentLoggedInUserRoleType() {
		UserInfo userInfo = getUserInfoFromSession();
		return userInfo != null ? userInfo.getAppUser().getAppRole().getRoleType() : "";
	}*/
}
