package com.bezkoder.spring.login.admin.bll.dto;

import java.math.BigDecimal;
import java.util.Date;


public class SPDTO {
	
	private int serProductId;
	private String txtProductCode;
	private String txtProductName;
	private String txtQuality;
	private String txtType;
	private Boolean blnStatus;
	private BigDecimal numSalePrice;

	public SPDTO()
	{
		
	}
	public SPDTO(int serProductId,String txtProductCode,String txtProductName,String txtQuality,Boolean blnStatus,BigDecimal numSalePrice)
	{
		this.serProductId=serProductId;
		this.txtProductCode=txtProductCode;
		this.txtProductName=txtProductName;
		this.txtQuality=txtQuality;
		this.blnStatus=blnStatus;
		this.numSalePrice=numSalePrice;
	}
	
	
	public SPDTO(int serProductId,String txtProductCode,String txtProductName,String txtQuality,Boolean blnStatus,BigDecimal numSalePrice,String txtType)
	{
		this.serProductId=serProductId;
		this.txtProductCode=txtProductCode;
		this.txtProductName=txtProductName;
		this.txtQuality=txtQuality;
		this.txtType=txtType;
		this.blnStatus=blnStatus;
		this.numSalePrice=numSalePrice;
	}
	
	public int getSerProductId() {
		return serProductId;
	}
	public void setSerProductId(int serProductId) {
		this.serProductId = serProductId;
	}
	public String getTxtProductCode() {
		return txtProductCode;
	}
	public void setTxtProductCode(String txtProductCode) {
		this.txtProductCode = txtProductCode;
	}
	public String getTxtProductName() {
		return txtProductName;
	}
	public void setTxtProductName(String txtProductName) {
		this.txtProductName = txtProductName;
	}
	public String getTxtQuality() {
		return txtQuality;
	}
	public void setTxtQuality(String txtQuality) {
		this.txtQuality = txtQuality;
	}
	public Boolean getBlnStatus() {
		return blnStatus;
	}
	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}
	public BigDecimal getNumSalePrice() {
		return numSalePrice;
	}
	public void setNumSalePrice(BigDecimal numSalePrice) {
		this.numSalePrice = numSalePrice;
	}
	public String getTxtType() {
		return txtType;
	}
	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}
	
	
	
}
