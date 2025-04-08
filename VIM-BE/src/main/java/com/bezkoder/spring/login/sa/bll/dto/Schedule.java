package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Schedule")
@XmlType(propOrder = {"date", "quantity"})
public class Schedule {

	private String date;
	private String quantity;
	
	
	
	
	public String getDate() {
		return date;
	}

	@XmlElement(name = "DATE")
	public void setDate(String date) {
		this.date = date;
	}

	public String getQuantity() {
		return quantity;
	}

	@XmlElement(name = "QUANTITY")
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	
	
	
}
