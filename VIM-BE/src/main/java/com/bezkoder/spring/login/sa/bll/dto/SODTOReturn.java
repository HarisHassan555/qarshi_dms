package com.bezkoder.spring.login.sa.bll.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//<DOC_TYPE>ZLOC</DOC_TYPE>
//<PURCH_NO>123456</PURCH_NO>
//<PURCH_DATE>20200522</PURCH_DATE>
//<DISTR_CHAN>10</DISTR_CHAN>
//<SALES_ORG>1000</SALES_ORG>
//<DIVISION>10</DIVISION>
//<PMNDISTR_CHANTTRMS>Z000</PMNDISTR_CHANTTRMS>


public class SODTOReturn {
	
	private String Salesdocument;
	private String Success;
	private String TId;
	
	
	
	
	private List<SODetailReturn> lstSchedule;




	public String getSalesdocument() {
		return Salesdocument;
	}




	public void setSalesdocument(String salesdocument) {
		Salesdocument = salesdocument;
	}




	public String getSuccess() {
		return Success;
	}




	public void setSuccess(String success) {
		Success = success;
	}




	public String getTId() {
		return TId;
	}




	public void setTId(String tId) {
		TId = tId;
	}




	public List<SODetailReturn> getLstSchedule() {
		return lstSchedule;
	}




	public void setLstSchedule(List<SODetailReturn> lstSchedule) {
		this.lstSchedule = lstSchedule;
	}


    
	
   

}
