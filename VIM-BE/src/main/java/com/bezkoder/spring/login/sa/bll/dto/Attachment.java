package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement(name = "ATTACHMENT")
@XmlType(propOrder = {"fname","ftype", "so", "immage"})
public class Attachment {
	
	private String fname;
	
	public String getFname() {
		return fname;
	}


	@XmlElement(name = "FNAME")
	public void setFname(String fname) {
		this.fname = fname;
	}



	public String getFtype() {
		return ftype;
	}


	@XmlElement(name = "FTYPE")
	public void setFtype(String ftype) {
		this.ftype = ftype;
	}



	public String getSo() {
		return so;
	}


	@XmlElement(name = "SO")
	public void setSo(String so) {
		this.so = so;
	}



	public String getImmage() {
		return immage;
	}


	@XmlElement(name = "IMMAGE")
	public void setImmage(String immage) {
		this.immage = immage;
	}



	private String ftype;
	
	private String so;
	
	private String immage;
	
	
	
	
	
   

}
