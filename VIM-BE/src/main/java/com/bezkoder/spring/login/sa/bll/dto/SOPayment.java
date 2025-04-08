package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "ZhamSdFromDmsCreateDpr")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType()

//propOrder={"Bookingdate", "Customercode", "Dealercode", "Deliverydate",
//		"Enduser", "Invoicetext", "Ordertype", "Psonumber"
//		               }
public class SOPayment {
//	<Bookingdate>2022-07-16</Bookingdate>
//    <Customercode>0000100000</Customercode>
//    <Dealercode>0000100000</Dealercode>
//    <Deliverydate>2022-11-16</Deliverydate>
//    <Enduser>0000300063</Enduser>
//    <Invoicetext>Muhammad Naveed</Invoicetext>
//	 <Ordertype>ZOR1</Ordertype>
//    <Psonumber>PSO123456</Psonumber>

	 
	 
	private String Psono;
	 private String Dueondate;
	 private String Instrumentdate;
	 private String Instrumentno;
	 private String Payment;
	 private String Depositbank;
	 
	
	 @XmlElement(name="Psono")
	public String getPsono() {
		return Psono;
	}

	public void setPsono(String psono) {
		Psono = psono;
	}

	public String getDueondate() {
		return Dueondate;
	}
	
	@XmlElement(name="Dueondate")
	public void setDueondate(String dueondate) {
		Dueondate = dueondate;
	}
	public String getInstrumentdate() {
		return Instrumentdate;
	}
	
	@XmlElement(name="Instrumentdate")
	public void setInstrumentdate(String instrumentdate) {
		Instrumentdate = instrumentdate;
	}
	public String getInstrumentno() {
		return Instrumentno;
	}
	
	@XmlElement(name="Instrumentno")
	public void setInstrumentno(String instrumentno) {
		Instrumentno = instrumentno;
	}
	public String getPayment() {
		return Payment;
	}
	
	@XmlElement(name="Payment")
	public void setPayment(String payment) {
		Payment = payment;
	}
	public String getDepositbank() {
		return Depositbank;
	}
	
	@XmlElement(name="Depositbank")
	public void setDepositbank(String depositbank) {
		Depositbank = depositbank;
	}

	 
	
	
	 
}
