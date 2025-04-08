package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



public class CustomerDTO {
	
	


//	"cst_Code": "003569",
//	"csttyp_Code": "000008",
//	"cty_Code": "000024",
//	"sg1_Code": "000007",
//	"bsg2_Code": "000001",
//	"cstg_Code": "000001",
//	"cst_Name": "short name",
//	"cst_short_name": "Short Name",
//	"cst_address1": "Lahore",
//	"cst_phone": "033305828266",
//	"cst_Email": "abc@gmail.com",
//	"cst_NTN_No": "",
//	"cst_GSTNo": ""

	@Override
	public String toString() {
		return "CustomerDTO [email=" + email + ", password=" + password + ", cst_Code=" + cst_Code + ", csttyp_Code="
				+ csttyp_Code + ", cty_Code=" + cty_Code + ", sg1_Code=" + sg1_Code + ", bsg2_Code=" + bsg2_Code
				+ ", cstg_Code=" + cstg_Code + ", cst_Name=" + cst_Name + ", cst_short_name=" + cst_short_name
				+ ", cst_address1=" + cst_address1 + ", cst_phone=" + cst_phone + ", cst_Email=" + cst_Email
				+ ", cst_NTN_No=" + cst_NTN_No + ", cst_GSTNo=" + cst_GSTNo + ", cst_address2=" + cst_address2
				+ ", cst_address3=" + cst_address3 + ", adv_filertag=" + adv_filertag + "]";
	}
	private String email;
	private String password;
	
	private String cst_Code;
	private String csttyp_Code;
    private String cty_Code;
    private String sg1_Code;
    
    private String bsg2_Code;
    
    private String cstg_Code;
    private String cst_Name;
    private String cst_short_name;
    private String cst_address1;    
    private String cst_phone;
    
    private String cst_Email;
    private String cst_NTN_No;
    private String cst_GSTNo;
    
    private String cst_address2;
    private String cst_address3;
    private int adv_filertag;
    
    
    
    
	public String getCst_Code() {
		return cst_Code;
	}
	public void setCst_Code(String cst_Code) {
		this.cst_Code = cst_Code;
	}
	public String getCsttyp_Code() {
		return csttyp_Code;
	}
	public void setCsttyp_Code(String csttyp_Code) {
		this.csttyp_Code = csttyp_Code;
	}
	public String getCty_Code() {
		return cty_Code;
	}
	public void setCty_Code(String cty_Code) {
		this.cty_Code = cty_Code;
	}
	public String getSg1_Code() {
		return sg1_Code;
	}
	public void setSg1_Code(String sg1_Code) {
		this.sg1_Code = sg1_Code;
	}
	public String getBsg2_Code() {
		return bsg2_Code;
	}
	public void setBsg2_Code(String bsg2_Code) {
		this.bsg2_Code = bsg2_Code;
	}
	public String getCstg_Code() {
		return cstg_Code;
	}
	public void setCstg_Code(String cstg_Code) {
		this.cstg_Code = cstg_Code;
	}
	public String getCst_Name() {
		return cst_Name;
	}
	public void setCst_Name(String cst_Name) {
		this.cst_Name = cst_Name;
	}
	public String getCst_short_name() {
		return cst_short_name;
	}
	public void setCst_short_name(String cst_short_name) {
		this.cst_short_name = cst_short_name;
	}
	public String getCst_address1() {
		return cst_address1;
	}
	public void setCst_address1(String cst_address1) {
		this.cst_address1 = cst_address1;
	}
	public String getCst_phone() {
		return cst_phone;
	}
	public void setCst_phone(String cst_phone) {
		this.cst_phone = cst_phone;
	}
	public String getCst_Email() {
		return cst_Email;
	}
	public void setCst_Email(String cst_Email) {
		this.cst_Email = cst_Email;
	}
	public String getCst_NTN_No() {
		return cst_NTN_No;
	}
	public void setCst_NTN_No(String cst_NTN_No) {
		this.cst_NTN_No = cst_NTN_No;
	}
	public String getCst_GSTNo() {
		return cst_GSTNo;
	}
	public void setCst_GSTNo(String cst_GSTNo) {
		this.cst_GSTNo = cst_GSTNo;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getCst_address2() {
		return cst_address2;
	}
	public void setCst_address2(String cst_address2) {
		this.cst_address2 = cst_address2;
	}
	public String getCst_address3() {
		return cst_address3;
	}
	public void setCst_address3(String cst_address3) {
		this.cst_address3 = cst_address3;
	}
	public int getAdv_filertag() {
		return adv_filertag;
	}
	public void setAdv_filertag(int adv_filertag) {
		this.adv_filertag = adv_filertag;
	}   
    
    


}
