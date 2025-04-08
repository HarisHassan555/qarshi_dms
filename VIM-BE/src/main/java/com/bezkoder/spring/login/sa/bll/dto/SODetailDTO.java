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


public class SODetailDTO {
	
	private Date dteDate;
	private BigDecimal numQty;
	private String itemNo;

	
	public Date getDteDate() {
		return dteDate;
	}
	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}
	public BigDecimal getNumQty() {
		return numQty;
	}
	public void setNumQty(BigDecimal numQty) {
		this.numQty = numQty;
	}
	public String getItemNo() {
		return itemNo;
	}
	public void setItemNo(String itemNo) {
		this.itemNo = itemNo;
	}
	
	
	
	
   

}
