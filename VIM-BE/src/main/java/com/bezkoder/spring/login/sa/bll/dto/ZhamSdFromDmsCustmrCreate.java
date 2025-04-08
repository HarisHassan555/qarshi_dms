package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "urn:ZBT_SD_FROM_DMS_CUSTMR_CREATE")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"ACCOUNTGRP", "ADDRESS", "CELLNUMBER", "CITY",
		"CNIC", "COMMERCIAL", "CUSTOMERCODE", "EMAIL","FILER",
		"FIRSTNAME", "FTN", "LASTNAME", "NTN",
		"PASSENGER", "PHONENUMBER", "PROVINCE", "REGION", "STRN","RECONCILIATION_AGL"
		               })

public class ZhamSdFromDmsCustmrCreate {
//	  <Accountgrp>?</Accountgrp>
//      <Address>?</Address>
//      <Cellnumber>?</Cellnumber>
//      <City>?</City>
//      <Cnic>?</Cnic>
//      <Commercial>?</Commercial>
//      <Customercode>?</Customercode>
//      <Email>?</Email>
//      <Firstname>?</Firstname>
//      <Ftn>?</Ftn>
//      <Lastname>?</Lastname>
//      <Ntn>?</Ntn>
//      <Passenger>?</Passenger>
//      <Phonenumber>?</Phonenumber>
//      <Province>?</Province>
//      <Region>?</Region>
//      <Strn>?</Strn>
	
	
//	ZhamSdFromDmsCustmrCreate()
//	{
//		
//	}
//	ZhamSdFromDmsCustmrCreate(CfgTblCustomer dto)
//	{
//		this.setCity(dto.getCfgTblCity().getTxtCityName());
//	       this.setEmail(dto.getTxtEmailAddress());
//	       this.setAccountgrp(dto.getCfgTblCustomerCategory().getTxtCustomerCategoryCode());
//	       this.setAddress(dto.getTxtDisplayAddress());
//	       this.setCellnumber(dto.getTxtMobileNo());
//	       this.setCnic(dto.getTxtCnicNo());
//	    
//	       this.setCommercial(dto.getBlnCommercial() ? "X" : "");
//	       this.setCustomercode(dto.getTxtCustomerCode());
//	       this.setFtn("31123");
//	       this.setFirstname(dto.getTxtFName());
//	       this.setNtn(dto.getTxtNtnNo());
//	       this.setLastname("");
//	       this.setPassenger(dto.getBlnPassanger() ? "X" : "");
//	       this.setPhonenumber(dto.getTxtPhoneNo());
//	       this.setProvince("Sindh");
//	       this.setRegion("SD");
//	       this.setStrn(dto.getTxtGstNumber());
//	}
	 
	 
	 private String ACCOUNTGRP;
	 private String ADDRESS;
	 private String CELLNUMBER;
	 private String CITY;
	 private String CNIC;
	 private String COMMERCIAL;
	 private String CUSTOMERCODE;
	 private String EMAIL;
	 private String FIRSTNAME;
	 private String FTN;
	 private String LASTNAME;
	 private String NTN;
	 private String PASSENGER;
	 private String PHONENUMBER;
	 private String PROVINCE;
	 private String REGION;
	 private String STRN;
	 private String FILER;
	 private String RECONCILIATION_AGL;
	 
	public String getACCOUNTGRP() {
		return ACCOUNTGRP;
	}
	public void setACCOUNTGRP(String aCCOUNTGRP) {
		ACCOUNTGRP = aCCOUNTGRP;
	}
	public String getADDRESS() {
		return ADDRESS;
	}
	public void setADDRESS(String aDDRESS) {
		ADDRESS = aDDRESS;
	}
	public String getCELLNUMBER() {
		return CELLNUMBER;
	}
	public void setCELLNUMBER(String cELLNUMBER) {
		CELLNUMBER = cELLNUMBER;
	}
	public String getCITY() {
		return CITY;
	}
	public void setCITY(String cITY) {
		CITY = cITY;
	}
	public String getCNIC() {
		return CNIC;
	}
	public void setCNIC(String cNIC) {
		CNIC = cNIC;
	}
	public String getCOMMERCIAL() {
		return COMMERCIAL;
	}
	public void setCOMMERCIAL(String cOMMERCIAL) {
		COMMERCIAL = cOMMERCIAL;
	}
	public String getCUSTOMERCODE() {
		return CUSTOMERCODE;
	}
	public void setCUSTOMERCODE(String cUSTOMERCODE) {
		CUSTOMERCODE = cUSTOMERCODE;
	}
	public String getEMAIL() {
		return EMAIL;
	}
	public void setEMAIL(String eMAIL) {
		EMAIL = eMAIL;
	}
	public String getFIRSTNAME() {
		return FIRSTNAME;
	}
	public void setFIRSTNAME(String fIRSTNAME) {
		FIRSTNAME = fIRSTNAME;
	}
	public String getFTN() {
		return FTN;
	}
	public void setFTN(String fTN) {
		FTN = fTN;
	}
	public String getLASTNAME() {
		return LASTNAME;
	}
	public void setLASTNAME(String lASTNAME) {
		LASTNAME = lASTNAME;
	}
	public String getNTN() {
		return NTN;
	}
	public void setNTN(String nTN) {
		NTN = nTN;
	}
	public String getPASSENGER() {
		return PASSENGER;
	}
	public void setPASSENGER(String pASSENGER) {
		PASSENGER = pASSENGER;
	}
	public String getPHONENUMBER() {
		return PHONENUMBER;
	}
	public void setPHONENUMBER(String pHONENUMBER) {
		PHONENUMBER = pHONENUMBER;
	}
	public String getPROVINCE() {
		return PROVINCE;
	}
	public void setPROVINCE(String pROVINCE) {
		PROVINCE = pROVINCE;
	}
	public String getREGION() {
		return REGION;
	}
	public void setREGION(String rEGION) {
		REGION = rEGION;
	}
	public String getSTRN() {
		return STRN;
	}
	public void setSTRN(String sTRN) {
		STRN = sTRN;
	}
	public String getFILER() {
		return FILER;
	}
	public void setFILER(String fILER) {
		FILER = fILER;
	}
	public String getRECONCILIATION_AGL() {
		return RECONCILIATION_AGL;
	}
	public void setRECONCILIATION_AGL(String rECONCILIATION_AGL) {
		RECONCILIATION_AGL = rECONCILIATION_AGL;
	}
	 
	
	 
}
