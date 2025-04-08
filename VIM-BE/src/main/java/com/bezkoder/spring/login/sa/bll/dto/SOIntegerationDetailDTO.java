package com.bezkoder.spring.login.sa.bll.dto;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



public class SOIntegerationDetailDTO {
	
	
	@Override
	public String toString() {
		return "Header [itm_ItemCode=" + itm_ItemCode + ", sod_SellingQty=" + sod_SellingQty + ", sod_SellingUnitCode=" + sod_SellingUnitCode
				+ ", sod_Conversion=" + sod_Conversion + ", sod_Conversion=" + sod_Conversion+ ", sod_StoringUnitCode=" + sod_StoringUnitCode
				+ ", sod_Qty=" + sod_Qty+ ", sod_QtyRemain=" + sod_QtyRemain+ ", sod_Rate=" + sod_Rate
				+ "]";
	}

//	"itm_ItemCode": "45110100001", 
//    "sod_SellingQty": 2, 
//    "sod_SellingUnitCode": "0002", 
//    "sod_StoringUnitCode": "0002", 
//    "sod_Conversion": 1, 
//    "sod_Qty": 2, 
//    "sod_QtyRemain": 2, 
//    "sod_Rate": 10

//	   "itm_ItemCode": "45110100001",
//	      "sod_SellingQty": 2,
//	      "sod_SellingUnitCode": "0002",
//	      "sod_StoringUnitCode": "0002",
//	      "sod_Conversion": 1,
//	      "sod_Qty": 2,
//	      "sod_QtyRemain": 2,
//	      "sod_Rate": 50
	private String itm_ItemCode;
	private BigDecimal sod_SellingQty;
    private String sod_SellingUnitCode;
    private String sod_StoringUnitCode;
    private BigDecimal sod_Conversion;
    private BigDecimal sod_Qty;
    private BigDecimal sod_QtyRemain;
    private BigDecimal sod_Rate;
    


	public String getItm_ItemCode() {
		return itm_ItemCode;
	}


	public void setItm_ItemCode(String itm_ItemCode) {
		this.itm_ItemCode = itm_ItemCode;
	}


	public BigDecimal getSod_SellingQty() {
		return sod_SellingQty;
	}


	public void setSod_SellingQty(BigDecimal sod_SellingQty) {
		this.sod_SellingQty = sod_SellingQty;
	}


	public String getSod_SellingUnitCode() {
		return sod_SellingUnitCode;
	}


	public void setSod_SellingUnitCode(String sod_SellingUnitCode) {
		this.sod_SellingUnitCode = sod_SellingUnitCode;
	}


	public BigDecimal getSod_Conversion() {
		return sod_Conversion;
	}


	public void setSod_Conversion(BigDecimal sod_Conversion) {
		this.sod_Conversion = sod_Conversion;
	}


	public BigDecimal getSod_Qty() {
		return sod_Qty;
	}


	public void setSod_Qty(BigDecimal sod_Qty) {
		this.sod_Qty = sod_Qty;
	}


	public BigDecimal getSod_QtyRemain() {
		return sod_QtyRemain;
	}


	public void setSod_QtyRemain(BigDecimal sod_QtyRemain) {
		this.sod_QtyRemain = sod_QtyRemain;
	}


	public BigDecimal getSod_Rate() {
		return sod_Rate;
	}


	public void setSod_Rate(BigDecimal sod_Rate) {
		this.sod_Rate = sod_Rate;
	}


	public String getSod_StoringUnitCode() {
		return sod_StoringUnitCode;
	}


	public void setSod_StoringUnitCode(String sod_StoringUnitCode) {
		this.sod_StoringUnitCode = sod_StoringUnitCode;
	}




    
}
