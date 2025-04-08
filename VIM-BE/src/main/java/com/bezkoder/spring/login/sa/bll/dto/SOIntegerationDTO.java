package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



public class SOIntegerationDTO {
	
	



//	 "cst_Code": "003569",
//	  "som_Date": "2024-02-18T06:39:53.930Z",
//	  "sO_DocNumber": "PSO-022",
//	  "sO_DocType": "000000000000003",
//	  "soDetail"
	
	private String cst_Code;
//	@Override
//	public String toString() {
//		return "SOIntegerationDTO [cst_Code=" + cst_Code + ", som_Date=" + som_Date + ", sO_DocNumber=" + sO_DocNumber
//				+ ", sO_DocType=" + sO_DocType + ", dept_code=" + dept_code + ", soDetail=" + soDetail + "]";
//	}

	

	private String som_Date;
    @Override
	public String toString() {
		return "SOIntegerationDTO [cst_Code=" + cst_Code + ", som_Date=" + som_Date + ", sO_DocNumber=" + sO_DocNumber
				+ ", sO_DocType=" + sO_DocType + ", dept_code=" + dept_code + ", color=" + color + ", soDetail="
				+ soDetail + "]";
	}


	private String sO_DocNumber;
    private String sO_DocType;
    private String dept_code;
    
    private String color;
    

	private List<SOIntegerationDetailDTO> soDetail;


	public String getCst_Code() {
		return cst_Code;
	}


	public void setCst_Code(String cst_Code) {
		this.cst_Code = cst_Code;
	}


	public String getSom_Date() {
		return som_Date;
	}


	public void setSom_Date(String som_Date) {
		this.som_Date = som_Date;
	}

	public String getsO_DocNumber() {
		return sO_DocNumber;
	}


	public void setsO_DocNumber(String sO_DocNumber) {
		this.sO_DocNumber = sO_DocNumber;
	}


	public String getsO_DocType() {
		return sO_DocType;
	}


	public void setsO_DocType(String sO_DocType) {
		this.sO_DocType = sO_DocType;
	}


	public List<SOIntegerationDetailDTO> getSoDetail() {
		return soDetail;
	}


	public void setSoDetail(List<SOIntegerationDetailDTO> soDetail) {
		this.soDetail = soDetail;
	}


	public String getDept_code() {
		return dept_code;
	}


	public void setDept_code(String dept_code) {
		this.dept_code = dept_code;
	}


	public String getColor() {
		return color;
	}


	public void setColor(String color) {
		this.color = color;
	}


}
