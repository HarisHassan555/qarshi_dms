package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "urn:ZBT_SD_FROM_DMS_CREATE_DPR")
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(propOrder={"CompanyCode","Psono", "Dueondate", "Instrumentdate", "Instrumentno",
//		"Payment", "Depositbank","Rejectionreason","Customer"
//		               })

public class Payment {

	 
	@Override
	public String toString() {
		return "Payment [Psono=" + Psono + ", Dueondate=" + Dueondate + ", Instrumentdate=" + Instrumentdate
				+ ", Instrumentno=" + Instrumentno + ", Payment=" + Payment + ", Depositbank=" + Depositbank + ", Customer=" + Customer + "]";
	}
	private String Psono;
	 private String Dueondate;
	 private String Instrumentdate;
	 private String Instrumentno;
	 private String Payment;
	 private String Depositbank;
	 private String Rejectionreason;
	 private String Customer;
	 private String CompanyCode;

	 
	public String getPsono() {
		return Psono;
	}
	@XmlElement(name="PSONO")
	public void setPsono(String psono) {
		Psono = psono;
	}
	public String getDueondate() {
		return Dueondate;
	}
	@XmlElement(name="DUEONDATE")
	public void setDueondate(String dueondate) {
		Dueondate = dueondate;
	}
	public String getInstrumentdate() {
		return Instrumentdate;
	}
	@XmlElement(name="INSTRUMENTDATE")
	public void setInstrumentdate(String instrumentdate) {
		Instrumentdate = instrumentdate;
	}
	public String getInstrumentno() {
		return Instrumentno;
	}
	@XmlElement(name="INSTRUMENTNO")
	public void setInstrumentno(String instrumentno) {
		Instrumentno = instrumentno;
	}
	public String getPayment() {
		return Payment;
	}
	@XmlElement(name="PAYMENT")
	public void setPayment(String payment) {
		Payment = payment;
	}
	public String getDepositbank() {
		return Depositbank;
	}
	@XmlElement(name="DEPOSITBANK")
	public void setDepositbank(String depositbank) {
		Depositbank = depositbank;
	}
	public String getRejectionreason() {
		return Rejectionreason;
	}
	@XmlElement(name="REJECTIONREASON")
	public void setRejectionreason(String rejectionreason) {
		Rejectionreason = rejectionreason;
	}
	public String getCustomer() {
		return Customer;
	}
	@XmlElement(name="CUSTOMER")
	public void setCustomer(String customer) {
		Customer = customer;
	}
	public String getCompanyCode() {
		return CompanyCode;
	}
	
	@XmlElement(name="COMPCODE")
	public void setCompanyCode(String companyCode) {
		CompanyCode = companyCode;
	}
	
	 
	 
	
	 
}
