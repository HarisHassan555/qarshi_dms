package com.bezkoder.spring.login.sa.bll.dto;

import java.util.Date;
public class DCDTO {
	
	private String dte_date;
	private String dte_date_from;
	private String dte_date_to;
	private String txtDONo;
	private String txtSaleOrderNo;
	private String txtProduct;
	private double Quantity;
	private Date dteDate;
	private String txtStatus;
	private double tareWeight;
	private double netWeight;
	private double grossWeight;
	
	private String txtInvoiceNo;
	private Date dteInvDate;
	private double InvQuantity;
	private String txtInvStatus;
	private boolean isInvoice;
	private boolean isUpdate;
	
	public String getDte_date() {
		return dte_date;
	}
	public void setDte_date(String dte_date) {
		this.dte_date = dte_date;
	}
	public String getDte_date_from() {
		return dte_date_from;
	}
	public void setDte_date_from(String dte_date_from) {
		this.dte_date_from = dte_date_from;
	}
	public String getDte_date_to() {
		return dte_date_to;
	}
	public void setDte_date_to(String dte_date_to) {
		this.dte_date_to = dte_date_to;
	}
	public String getTxtDONo() {
		return txtDONo;
	}
	public void setTxtDONo(String txtDONo) {
		this.txtDONo = txtDONo;
	}
	public String getTxtSaleOrderNo() {
		return txtSaleOrderNo;
	}
	public void setTxtSaleOrderNo(String txtSaleOrderNo) {
		this.txtSaleOrderNo = txtSaleOrderNo;
	}
	public String getTxtProduct() {
		return txtProduct;
	}
	public void setTxtProduct(String txtProduct) {
		this.txtProduct = txtProduct;
	}
	public double getQuantity() {
		return Quantity;
	}
	public void setQuantity(double quantity) {
		Quantity = quantity;
	}
	public Date getDteDate() {
		return dteDate;
	}
	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}
	public String getTxtStatus() {
		return txtStatus;
	}
	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}
	public double getTareWeight() {
		return tareWeight;
	}
	public void setTareWeight(double tareWeight) {
		this.tareWeight = tareWeight;
	}
	public double getNetWeight() {
		return netWeight;
	}
	public void setNetWeight(double netWeight) {
		this.netWeight = netWeight;
	}
	public double getGrossWeight() {
		return grossWeight;
	}
	public void setGrossWeight(double grossWeight) {
		this.grossWeight = grossWeight;
	}
	public String getTxtInvoiceNo() {
		return txtInvoiceNo;
	}
	public void setTxtInvoiceNo(String txtInvoiceNo) {
		this.txtInvoiceNo = txtInvoiceNo;
	}
	public Date getDteInvDate() {
		return dteInvDate;
	}
	public void setDteInvDate(Date dteInvDate) {
		this.dteInvDate = dteInvDate;
	}
	public double getInvQuantity() {
		return InvQuantity;
	}
	public void setInvQuantity(double invQuantity) {
		InvQuantity = invQuantity;
	}
	public String getTxtInvStatus() {
		return txtInvStatus;
	}
	public void setTxtInvStatus(String txtInvStatus) {
		this.txtInvStatus = txtInvStatus;
	}
	public boolean isInvoice() {
		return isInvoice;
	}
	public void setInvoice(boolean isInvoice) {
		this.isInvoice = isInvoice;
	}
	public boolean isUpdate() {
		return isUpdate;
	}
	public void setUpdate(boolean isUpdate) {
		this.isUpdate = isUpdate;
	}

    
	
	 
	 
	 
	 
}
