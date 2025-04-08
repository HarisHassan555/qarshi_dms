package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "REJ_TB")
@XmlType()

//"Lineitem","Materialcode","Color","Zpr","Zait","Zfni",

//propOrder = {"Lineitem","Materialcode","partn_role", "partn_numb"}
public class CancelDetail {



	private String POSNR;
	private String ABGRU;
	public String getPOSNR() {
		return POSNR;
	}
	@XmlElement(name = "POSNR")
	public void setPOSNR(String pOSNR) {
		POSNR = pOSNR;
	}
	public String getABGRU() {
		return ABGRU;
	}
	@XmlElement(name = "ABGRU")
	public void setABGRU(String aBGRU) {
		ABGRU = aBGRU;
	}
	@Override
	public String toString() {
		return "CancelDetail [POSNR=" + POSNR + ", ABGRU=" + ABGRU + "]";
	}
	
	
	 private List<CancelDetailInner> lstDetail;
	public List<CancelDetailInner> getLstDetail() {
		return lstDetail;
	}
	
	@XmlElement(name="item")
	public void setLstDetail(List<CancelDetailInner> lstDetail) {
		this.lstDetail = lstDetail;
	}
	
}
