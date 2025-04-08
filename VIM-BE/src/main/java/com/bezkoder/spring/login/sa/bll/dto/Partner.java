package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Item")
@XmlType()

//"Lineitem","Materialcode","Color","Zpr","Zait","Zfni",

//propOrder = {"Lineitem","Materialcode","partn_role", "partn_numb"}
public class Partner {

	@Override
	public String toString() {
		return "Partner [partn_role=" + partn_role + ", partn_numb=" + partn_numb + ", Lineitem=" + Lineitem
				+ ", Materialcode=" + Materialcode + ", Color=" + Color + ", Zpr=" + Zpr + ", Zait=" + Zait + ", Zfni="
				+ Zfni + ", Quantity="+ Quantity + "]";
	}

	private String partn_role;
	private String partn_numb;
	
	
	public String getPartn_role() {
		return partn_role;
	}
	
	@XmlElement(name = "PARTN_ROLE")
	public void setPartn_role(String partn_role) {
		this.partn_role = partn_role;
	}
	public String getPartn_numb() {
		return partn_numb;
	}
	
	@XmlElement(name = "PARTN_NUMB")
	public void setPartn_numb(String partn_numb) {
		this.partn_numb = partn_numb;
	}
	
	private String Lineitem;
	private String Materialcode;
	private String Color;
	private String Zpr;
	private String Zait;
	private String Zfni;
	private String Mvgr3;
	private String Quantity;


	
	public String getLineitem() {
		return Lineitem;
	}
	
	@XmlElement(name = "LINEITEM")
	public void setLineitem(String lineitem) {
		Lineitem = lineitem;
	}

	
	public String getMaterialcode() {
		return Materialcode;
	}
	
	@XmlElement(name = "MATERIALCODE")
	public void setMaterialcode(String materialcode) {
		Materialcode = materialcode;
	}

	public String getColor() {
		return Color;
	}
	@XmlElement(name = "COLOR_CODE")
	public void setColor(String color) {
		Color = color;
	}



	public String getZpr() {
		return Zpr;
	}

	@XmlElement(name = "ZGPR")
	public void setZpr(String zpr) {
		Zpr = zpr;
	}

	public String getZait() {
		return Zait;
	}

	@XmlElement(name = "ZAT1")
	public void setZait(String zait) {
		Zait = zait;
	}

	public String getZfni() {
		return Zfni;
	}

	@XmlElement(name = "ZFI2")
	public void setZfni(String zfni) {
		Zfni = zfni;
	}

	public String getMvgr3() {
		return Mvgr3;
	}

	@XmlElement(name = "MVGR3")
	public void setMvgr3(String mvgr3) {
		Mvgr3 = mvgr3;
	}

	public String getQuantity() {
		return Quantity;
	}

	@XmlElement(name = "QUANTITY")
	public void setQuantity(String quantity) {
		Quantity = quantity;
	}
	
	
	
}
