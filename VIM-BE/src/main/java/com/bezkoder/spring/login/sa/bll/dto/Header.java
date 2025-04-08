package com.bezkoder.spring.login.sa.bll.dto;

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
@XmlRootElement(name = "Header")

public class Header {
	
	
	@Override
	public String toString() {
		return "Header [DOC_TYPE=" + DOC_TYPE + ", PURCH_NO=" + PURCH_NO + ", PURCH_DATE=" + PURCH_DATE
				+ ", DISTR_CHAN=" + DISTR_CHAN + ", SALES_ORG=" + SALES_ORG + ", DIVISION=" + DIVISION
				+ ", PMNDISTR_CHANTTRMS=" + PMNDISTR_CHANTTRMS + ", SALES_ORD=" + SALES_ORD + ", incoterm1=" + incoterm1
				+ ", incoterm2=" + incoterm2 + ", Bookingdate=" + Bookingdate + ", Customercode=" + Customercode
				+ ", Dealercode=" + Dealercode + ", Deliverydate=" + Deliverydate + ", Enduser=" + Enduser
				+ ", Invoicetext=" + Invoicetext + ", Ordertype=" + Ordertype + ", Psonumber=" + Psonumber
				+ ", lstPartner=" + lstPartner + "]";
	}


	private String DOC_TYPE;
	private String PURCH_NO;
    private String PURCH_DATE;
    private String DISTR_CHAN;
    
    private String SALES_ORG;
    private String DIVISION;
    private String PMNDISTR_CHANTTRMS;
    
    private String SALES_ORD;
    
    
    public String getDOC_TYPE() {
		return DOC_TYPE;
	}
    
    @XmlElement(name = "DOC_TYPE")
	public void setDOC_TYPE(String dOC_TYPE) {
		DOC_TYPE = dOC_TYPE;
	}
	public String getPURCH_NO() {
		return PURCH_NO;
	}
	
	@XmlElement(name = "PURCH_NO")
	public void setPURCH_NO(String pURCH_NO) {
		PURCH_NO = pURCH_NO;
	}
	public String getPURCH_DATE() {
		return PURCH_DATE;
	}
	
	@XmlElement(name = "PURCH_DATE")
	public void setPURCH_DATE(String pURCH_DATE) {
		PURCH_DATE = pURCH_DATE;
	}
	public String getDISTR_CHAN() {
		return DISTR_CHAN;
	}
	
	@XmlElement(name = "DISTR_CHAN")
	public void setDISTR_CHAN(String dISTR_CHAN) {
		DISTR_CHAN = dISTR_CHAN;
	}
	public String getSALES_ORG() {
		return SALES_ORG;
	}
	
	@XmlElement(name = "SALES_ORG")
	public void setSALES_ORG(String sALES_ORG) {
		SALES_ORG = sALES_ORG;
	}
	public String getDIVISION() {
		return DIVISION;
	}
	
	@XmlElement(name = "DIVISION")
	public void setDIVISION(String dIVISION) {
		DIVISION = dIVISION;
	}
	public String getPMNDISTR_CHANTTRMS() {
		return PMNDISTR_CHANTTRMS;
	}
	
	@XmlElement(name = "PMNDISTR_CHANTTRMS")
	public void setPMNDISTR_CHANTTRMS(String pMNDISTR_CHANTTRMS) {
		PMNDISTR_CHANTTRMS = pMNDISTR_CHANTTRMS;
	}
	
	
	private String incoterm1;
	private String incoterm2;

	public String getIncoterm1() {
		return incoterm1;
	}

	@XmlElement(name = "INCOTERMS1")
	public void setIncoterm1(String incoterm1) {
		this.incoterm1 = incoterm1;
	}

	public String getIncoterm2() {
		return incoterm2;
	}

	@XmlElement(name = "INCOTERMS2")
	public void setIncoterm2(String incoterm2) {
		this.incoterm2 = incoterm2;
	}

	public String getSALES_ORD() {
		return SALES_ORD;
	}

	public void setSALES_ORD(String sALES_ORD) {
		SALES_ORD = sALES_ORD;
	}

   
	
	 private String Bookingdate;
	 private String Customercode;
	 private String Dealercode;
	 private String Deliverydate;
	 private String Enduser;
	 private String Invoicetext;
	 private String Ordertype;
	 private String Psonumber;


	public String getBookingdate() {
		return Bookingdate;
	}

	public void setBookingdate(String bookingdate) {
		Bookingdate = bookingdate;
	}

	public String getCustomercode() {
		return Customercode;
	}

	public void setCustomercode(String customercode) {
		Customercode = customercode;
	}

	public String getDealercode() {
		return Dealercode;
	}

	public void setDealercode(String dealercode) {
		Dealercode = dealercode;
	}

	public String getDeliverydate() {
		return Deliverydate;
	}

	public void setDeliverydate(String deliverydate) {
		Deliverydate = deliverydate;
	}

	public String getEnduser() {
		return Enduser;
	}

	public void setEnduser(String enduser) {
		Enduser = enduser;
	}

	public String getInvoicetext() {
		return Invoicetext;
	}

	public void setInvoicetext(String invoicetext) {
		Invoicetext = invoicetext;
	}

	public String getOrdertype() {
		return Ordertype;
	}

	public void setOrdertype(String ordertype) {
		Ordertype = ordertype;
	}

	public String getPsonumber() {
		return Psonumber;
	}

	public void setPsonumber(String psonumber) {
		Psonumber = psonumber;
	}
	 

	private List<Partner> lstPartner;
	
	public List<Partner> getLstPartner() {
		return lstPartner;
	}

	@XmlElement(name="item")
	public void setLstPartner(List<Partner> lstPartner) {
		this.lstPartner = lstPartner;
	}

}
