package com.bezkoder.spring.login.sa.bll.dto;



import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
@XmlRootElement(name = "urn:ZhamSdFromDmsCustmrCreate")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType

public class EndResult {
	private String errorMessage;
	 private String status;
	 private String Success;
	 private String TId;
	@Override
	public String toString() {
		return "Result [errorMessage=" + errorMessage + ", status=" + status + ", Success=" + Success + ", TId=" + TId
				+ "]";
	}
	 
	 
	
}