package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "urn:ZhamSdFromDmsCreateSo")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType()

//propOrder={"Bookingdate", "Customercode", "Dealercode", "Deliverydate",
//		"Enduser", "Invoicetext", "Ordertype", "Psonumber"
//		               }
public class SOMaster {
//	<Bookingdate>2022-07-16</Bookingdate>
//    <Customercode>0000100000</Customercode>
//    <Dealercode>0000100000</Dealercode>
//    <Deliverydate>2022-11-16</Deliverydate>
//    <Enduser>0000300063</Enduser>
//    <Invoicetext>Muhammad Naveed</Invoicetext>
//	 <Ordertype>ZOR1</Ordertype>
//    <Psonumber>PSO123456</Psonumber>

	 
	 
	 private String Bookingdate;
	 private String Customercode;
	 private String Dealercode;
	 private String Deliverydate;
	 private String Enduser;
	 private String Invoicetext;
	 private String Ordertype;
	 private String Psonumber;
	 private SODetail detail;

	 
	 
	public SODetail getDetail() {
		return detail;
	}
	public void setDetail(SODetail detail) {
		this.detail = detail;
	}
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


	private List<SODetail> lstSoDetail;



	public List<SODetail> getLstSoDetail() {
		return lstSoDetail;
	}
	
	@XmlElement(name="BAPISCHEDULE")
	public void setLstSoDetail(List<SODetail> lstSoDetail) {
		this.lstSoDetail = lstSoDetail;
	}
	
	
	 
}
