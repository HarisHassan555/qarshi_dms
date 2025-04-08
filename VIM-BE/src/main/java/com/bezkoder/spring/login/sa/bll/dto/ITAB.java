package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;




@XmlRootElement(name = "urn:ZBT_SD_FROM_DMS_CREATE_SO")
public class ITAB {

	 @Override
	public String toString() {
		return "ITAB [header=" + header + ", item=" + item + ", lstPartner=" + lstPartner + ", lstSchedule="
				+ lstSchedule + ", lstHeader=" + lstHeader + ", attachment=" + attachment + ", Bookingdate="
				+ Bookingdate + ", Customercode=" + Customercode + ", Dealercode=" + Dealercode + ", Deliverydate="
				+ Deliverydate + ", Enduser=" + Enduser + ", Invoicetext=" + Invoicetext + ", Ordertype=" + Ordertype
				+ ", Psonumber=" + Psonumber + ", Pricegroup=" + Pricegroup + "]";
	}


	private Header header;
	 
	 

	public Header getHeader() {
		return header;
	}

	@XmlElement(name="ITEMS")
	public void setHeader(Header header) {
		this.header = header;
	}

	
	private Item item;

	public Item getItem() {
		return item;
	}

	@XmlElement(name="BAPIITEMIN")
	public void setItem(Item item) {
		this.item = item;
	}
	
	private List<Partner> lstPartner;
//	private Partner Partner;



	public List<Partner> getLstPartner() {
		return lstPartner;
	}

	@XmlElement(name="BAPIPARNR")
	public void setLstPartner(List<Partner> lstPartner) {
		this.lstPartner = lstPartner;
	}
	
	
	private List<Schedule> lstSchedule;
	
	private List<Header> lstHeader;



	public List<Schedule> getLstSchedule() {
		return lstSchedule;
	}
	
	@XmlElement(name="BAPISCHEDULE")
	public void setLstSchedule(List<Schedule> lstSchedule) {
		this.lstSchedule = lstSchedule;
	}
	
	
	private Attachment attachment;



	public Attachment getAttachment() {
		return attachment;
	}

	@XmlElement(name="ATTACHMENT")
	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}

	public List<Header> getLstHeader() {
		return lstHeader;
	}
	
	@XmlElement(name="SO")
	public void setLstHeader(List<Header> lstHeader) {
		this.lstHeader = lstHeader;
	}
	
	
	 private String Bookingdate;
	 private String Customercode;
	 private String Dealercode;
	 private String Deliverydate;
	 private String Enduser;
	 private String Invoicetext;
	 private String Ordertype;
	 private String Psonumber;
	 private String Pricegroup;
	 
	 private String Color;
	 private String Remarks;
	 
	 private String ASSIGNMENT_NUMBER;


	public String getBookingdate() {
		return Bookingdate;
	}
	
	@XmlElement(name="BOOKINGDATE")
	public void setBookingdate(String bookingdate) {
		Bookingdate = bookingdate;
	}

	public String getCustomercode() {
		return Customercode;
	}
	
	@XmlElement(name="CUSTOMERCODE")
	public void setCustomercode(String customercode) {
		Customercode = customercode;
	}

	public String getDealercode() {
		return Dealercode;
	}

	@XmlElement(name="DEALERCODE")
	public void setDealercode(String dealercode) {
		Dealercode = dealercode;
	}

	@XmlElement(name="DELIVERYDATE")
	public String getDeliverydate() {
		return Deliverydate;
	}

	public void setDeliverydate(String deliverydate) {
		Deliverydate = deliverydate;
	}

	public String getEnduser() {
		return Enduser;
	}

	@XmlElement(name="ENDUSER")
	public void setEnduser(String enduser) {
		Enduser = enduser;
	}

	public String getInvoicetext() {
		return Invoicetext;
	}

	@XmlElement(name="INVOICETEXT")
	public void setInvoicetext(String invoicetext) {
		Invoicetext = invoicetext;
	}

	public String getOrdertype() {
		return Ordertype;
	}

	@XmlElement(name="ORDERTYPE")
	public void setOrdertype(String ordertype) {
		Ordertype = ordertype;
	}

	public String getPsonumber() {
		return Psonumber;
	}

	@XmlElement(name="PSONUMBER")
	public void setPsonumber(String psonumber) {
		Psonumber = psonumber;
	}

	public String getPricegroup() {
		return Pricegroup;
	}
	
	@XmlElement(name="PRICEGROUP")
	public void setPricegroup(String pricegroup) {
		Pricegroup = pricegroup;
	}

	public String getColor() {
		return Color;
	}

	@XmlElement(name="COLOR")
	public void setColor(String color) {
		Color = color;
	}

	public String getRemarks() {
		return Remarks;
	}

	@XmlElement(name="REMARKS")
	public void setRemarks(String remarks) {
		Remarks = remarks;
	}

	public String getASSIGNMENT_NUMBER() {
		return ASSIGNMENT_NUMBER;
	}

	@XmlElement(name="ASSIGNMENT_NUMBER")
	public void setASSIGNMENT_NUMBER(String aSSIGNMENT_NUMBER) {
		ASSIGNMENT_NUMBER = aSSIGNMENT_NUMBER;
	}
	
	

}
