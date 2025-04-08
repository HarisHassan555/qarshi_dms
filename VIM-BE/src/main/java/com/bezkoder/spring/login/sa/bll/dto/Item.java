package com.bezkoder.spring.login.sa.bll.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

//<DOC_TYPE>ZLOC</DOC_TYPE>
//<PURCH_NO>123456</PURCH_NO>
//<PURCH_DATE>20200522</PURCH_DATE>
//<DISTR_CHAN>10</DISTR_CHAN>
//<SALES_ORG>1000</SALES_ORG>
//<DIVISION>10</DIVISION>
//<PMNDISTR_CHANTTRMS>Z000</PMNDISTR_CHANTTRMS>
@XmlRootElement(name = "Item")
@XmlType(propOrder = {"item_no","material", "req_qty"})
public class Item {
	
	@Override
	public String toString() {
		return "Item [item_no=" + item_no + ", material=" + material + ", req_qty=" + req_qty + "]";
	}

	private String item_no;
	public String getItem_no() {
		return item_no;
	}
	
	@XmlElement(name = "ITM_NUMBER")
	public void setItem_no(String item_no) {
		this.item_no = item_no;
	}

	private String material;
	private String req_qty;

	

    
	public String getMaterial() {
		return material;
	}

	@XmlElement(name = "MATERIAL")
	public void setMaterial(String material) {
		this.material = material;
	}

	public String getReq_qty() {
		return req_qty;
	}

	@XmlElement(name = "REQ_QTY")
	public void setReq_qty(String req_qty) {
		this.req_qty = req_qty;
	}
	

	

	
	
   

}
