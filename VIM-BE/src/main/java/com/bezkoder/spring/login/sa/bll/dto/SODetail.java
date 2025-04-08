package com.bezkoder.spring.login.sa.bll.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Items")
@XmlType()

//propOrder = {"Lineitem", "Color","Zpr0","Zait"}
public class SODetail {
	
//	<Lineitem>000020</Lineitem>
//    <Materialcode>000000000019000034</Materialcode>
//    <Color>C01</Color>
//    <Zpr0>10000</Zpr0>
//    <Zait>100</Zait>


	private String Lineitem;
	private String Materialcode;
	private String Color;
	private String Zpr0;
	private String Zait;
	private String Zfni;
	

	
	

	public String getLineitem() {
		return Lineitem;
	}
	public void setLineitem(String lineitem) {
		Lineitem = lineitem;
	}
	
	
	public String getMaterialcode() {
		return Materialcode;
	}
	public void setMaterialcode(String materialcode) {
		Materialcode = materialcode;
	}
	public String getColor() {
		return Color;
	}
	public void setColor(String color) {
		Color = color;
	}
	public String getZpr0() {
		return Zpr0;
	}
	public void setZpr0(String zpr0) {
		Zpr0 = zpr0;
	}
	public String getZait() {
		return Zait;
	}
	public void setZait(String zait) {
		Zait = zait;
	}
	public String getZfni() {
		return Zfni;
	}
	public void setZfni(String zfni) {
		Zfni = zfni;
	}
	
	

	
	
}
