package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "urn:ZBT_SD_FROM_DMS_CANCEL_SO")
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(propOrder={"Psonumber", "Dueondate", "Instrumentdate", "Instrumentno",
//		"Payment", "Depositbank","Rejectionreason"
//		               })

public class CancelSO {

	 

	private String Psonumber;
	
	 private String Rejectionreason;
	 
	 private CancelDetail cancelDetail;
	
	public String getPsonumber() {
		return Psonumber;
	}
	@XmlElement(name="PSONUMBER")
	public void setPsonumber(String psonumber) {
		Psonumber = psonumber;
	}
	
	

	
	public String getRejectionreason() {
		return Rejectionreason;
	}
	@XmlElement(name="ABGRU")
	public void setRejectionreason(String rejectionreason) {
		Rejectionreason = rejectionreason;
	}

	public CancelDetail getCancelDetail() {
		return cancelDetail;
	}
	
	@XmlElement(name="REJ_TB")
	public void setCancelDetail(CancelDetail cancelDetail) {
		this.cancelDetail = cancelDetail;
	}
	
	
	 
	 
	
	 
}
