package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
public class SMSSender {
public static String sendSms(String sToPhoneNo,String sMessage) {
	
	System.out.println("phone no:"+sToPhoneNo+":----");
		/*
		 * try { // Construct data String data = "id=" +
		 * URLEncoder.encode("rchittehadcl", "UTF-8"); data += "&pass=" +
		 * URLEncoder.encode("lahore58", "UTF-8"); data += "&msg=" +
		 * URLEncoder.encode(sMessage, "UTF-8"); data += "&lang=" +
		 * URLEncoder.encode("English", "UTF-8"); data += "&to=" +
		 * URLEncoder.encode(sToPhoneNo, "UTF-8"); //data += "&to=" +
		 * URLEncoder.encode("923100003838", "UTF-8");
		 * 
		 * data += "&mask=" + URLEncoder.encode("ICL", "UTF-8"); data += "&type=" +
		 * URLEncoder.encode("xml", "UTF-8"); // Send data URL url = new
		 * URL("http://www.outreach.pk/api/sendsms.php/sendsms/url"); URLConnection conn
		 * = url.openConnection(); conn.setDoOutput(true); OutputStreamWriter wr = new
		 * OutputStreamWriter(conn.getOutputStream()); wr.write(data); wr.flush(); //
		 * Get the response BufferedReader rd = new BufferedReader(new
		 * InputStreamReader(conn.getInputStream())); String line; String sResult="";
		 * while ((line = rd.readLine()) != null) { // Process line...
		 * sResult=sResult+line+" "; System.out.println("SMS "+sResult); } wr.close();
		 * rd.close(); return sResult; } catch (Exception e) {
		 * System.out.println("Error SMS "+e); return "Error "+e;
		 * 
		 * }
		 */
	return "";
}
public static void main(String[] args) {
String result = sendSms("923016753158","Hello Safi, this is a test with a 5 note and an ampersand (&)symbol");
System.out.println(result);
}
}