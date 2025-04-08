package com.bezkoder.spring.login.admin;

public class ServerConfiguration {
	
//
	public static String ip_servre="http://192.168.0.27:8000";
	public static boolean send_mail=false;
	
	public static boolean integeration_required = false;
	
//	DEV server
//	public static String ip_servre="http://192.168.0.23:8000";
//	public static boolean send_mail=false;
	
	 //  192.168.0.20:8000 This is production server of ICL
	
//	public static String ip_servre="http://192.168.0.20:8000";   
//	public static boolean send_mail=true;
	
	public static String service_auth_tokenSH="http://115.167.64.214:5000/Auth/Login";
			
	public static String service_customerSH= "http://115.167.64.214:5000/SCM/CreateCustomer";
	
	public static String service_SOSH= "http://115.167.64.214:5000/SCM/CreateSalesOrder";
	
	public static String service_paymentSH= "http://115.167.64.214:5000/SCM/CreateReceipt";
	
	public static String service_CancelSH= "";

	
	
	//  QSA Server
	
	public static String service_customer="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
//										   "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create
	public static String service_so=      "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
	public static String service_payment= "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
	public static String service_cancel_so="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
	public static String usercontext="sap-usercontext=sap-client=100";
	public static String uname="dmsuser";
	public static String Password="SAPdms@123";
	
	
	
}
