package com.bezkoder.spring.login.sa.bll.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//<DOC_TYPE>ZLOC</DOC_TYPE>
//<PURCH_NO>123456</PURCH_NO>
//<PURCH_DATE>20200522</PURCH_DATE>
//<DISTR_CHAN>10</DISTR_CHAN>
//<SALES_ORG>1000</SALES_ORG>
//<DIVISION>10</DIVISION>
//<PMNDISTR_CHANTTRMS>Z000</PMNDISTR_CHANTTRMS>


public class SOClaim {
	
	private String txtOrderNO;
	private String txtDocType;
	private String txtPurchaseNo;
	private String txtPurchaseDate;
	private String txtDistChannel;
	private String txtSalesOrg;
	private Date dteDate;
	private String fisYear;
	private String fisPeriod;
	private String headerText;
	
	private Date dteDeliveryDate;
	
	private String txtChassisNo;
	private String txtEngineNo;
	private String txtRegistrationNo;
	private String txtDeliveryPartNo;
	private String txtItemNo;
	private String materialProduct;
	
	private String txtDealer;
	private String txtCustomer;
	
	private String txtDealerName;
	private String txtCustomerName;
	
	private String txtPhone;
	private String txtCNIC;
	private String txtCity;

	private int level;
	
	private String txtDivision;
	private String txtPmndistChanterms;
	private String txtprofitCenter;
	private String txtpostingDate;
	private String refDocNo;
	private String shipToParty;

	private BigDecimal numQty;
	
	private List<Integer> lstIds;
	
	
	private List<SODetailDTO> lstSchedule;
	
	private List<SODetailDTO> lstDetail;

	
	private String txtPsoNo;
	private String txtDoDoc;
	private String txtChassis;
	private String txtEngine;
	private boolean blnFlag;
	


	


	


	public String getTxtPsoNo() {
		return txtPsoNo;
	}


	public void setTxtPsoNo(String txtPsoNo) {
		this.txtPsoNo = txtPsoNo;
	}


	public String getTxtDoDoc() {
		return txtDoDoc;
	}


	public void setTxtDoDoc(String txtDoDoc) {
		this.txtDoDoc = txtDoDoc;
	}


	public String getTxtChassis() {
		return txtChassis;
	}


	public void setTxtChassis(String txtChassis) {
		this.txtChassis = txtChassis;
	}


	public String getTxtEngine() {
		return txtEngine;
	}


	public void setTxtEngine(String txtEngine) {
		this.txtEngine = txtEngine;
	}


	

	public boolean isBlnFlag() {
		return blnFlag;
	}


	public void setBlnFlag(boolean blnFlag) {
		this.blnFlag = blnFlag;
	}


	public String getTxtOrderNO() {
		return txtOrderNO;
	}


	public void setTxtOrderNO(String txtOrderNO) {
		this.txtOrderNO = txtOrderNO;
	}


	public String getTxtDocType() {
		return txtDocType;
	}


	public void setTxtDocType(String txtDocType) {
		this.txtDocType = txtDocType;
	}


	public String getTxtPurchaseNo() {
		return txtPurchaseNo;
	}


	public void setTxtPurchaseNo(String txtPurchaseNo) {
		this.txtPurchaseNo = txtPurchaseNo;
	}


	public String getTxtPurchaseDate() {
		return txtPurchaseDate;
	}


	public void setTxtPurchaseDate(String txtPurchaseDate) {
		this.txtPurchaseDate = txtPurchaseDate;
	}


	public String getTxtDistChannel() {
		return txtDistChannel;
	}


	public void setTxtDistChannel(String txtDistChannel) {
		this.txtDistChannel = txtDistChannel;
	}


	public String getTxtSalesOrg() {
		return txtSalesOrg;
	}


	public void setTxtSalesOrg(String txtSalesOrg) {
		this.txtSalesOrg = txtSalesOrg;
	}


	public String getTxtDivision() {
		return txtDivision;
	}


	public void setTxtDivision(String txtDivision) {
		this.txtDivision = txtDivision;
	}


	public String getTxtPmndistChanterms() {
		return txtPmndistChanterms;
	}


	public void setTxtPmndistChanterms(String txtPmndistChanterms) {
		this.txtPmndistChanterms = txtPmndistChanterms;
	}


	public String getTxtItemNo() {
		return txtItemNo;
	}


	public void setTxtItemNo(String txtItemNo) {
		this.txtItemNo = txtItemNo;
	}


	

	public BigDecimal getNumQty() {
		return numQty;
	}


	public void setNumQty(BigDecimal numQty) {
		this.numQty = numQty;
	}


	public List<SODetailDTO> getLstSchedule() {
		return lstSchedule;
	}


	public void setLstSchedule(List<SODetailDTO> lstSchedule) {
		this.lstSchedule = lstSchedule;
	}


	public Date getDteDate() {
		return dteDate;
	}


	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}


	public String getTxtDealer() {
		return txtDealer;
	}


	public void setTxtDealer(String txtDealer) {
		this.txtDealer = txtDealer;
	}


	public String getTxtCustomer() {
		return txtCustomer;
	}


	public void setTxtCustomer(String txtCustomer) {
		this.txtCustomer = txtCustomer;
	}


	public String getTxtDealerName() {
		return txtDealerName;
	}


	public void setTxtDealerName(String txtDealerName) {
		this.txtDealerName = txtDealerName;
	}


	public String getTxtCustomerName() {
		return txtCustomerName;
	}


	public void setTxtCustomerName(String txtCustomerName) {
		this.txtCustomerName = txtCustomerName;
	}


	public String getTxtPhone() {
		return txtPhone;
	}


	public void setTxtPhone(String txtPhone) {
		this.txtPhone = txtPhone;
	}


	public String getTxtCNIC() {
		return txtCNIC;
	}


	public void setTxtCNIC(String txtCNIC) {
		this.txtCNIC = txtCNIC;
	}


	public String getTxtCity() {
		return txtCity;
	}


	public void setTxtCity(String txtCity) {
		this.txtCity = txtCity;
	}


	public List<Integer> getLstIds() {
		return lstIds;
	}


	public void setLstIds(List<Integer> lstIds) {
		this.lstIds = lstIds;
	}

	public int getLevel() {
		return level;
	}


	public void setLevel(int level) {
		this.level = level;
	}


	public String getTxtChassisNo() {
		return txtChassisNo;
	}


	public void setTxtChassisNo(String txtChassisNo) {
		this.txtChassisNo = txtChassisNo;
	}


	public String getTxtEngineNo() {
		return txtEngineNo;
	}


	public void setTxtEngineNo(String txtEngineNo) {
		this.txtEngineNo = txtEngineNo;
	}


	public String getTxtRegistrationNo() {
		return txtRegistrationNo;
	}


	public void setTxtRegistrationNo(String txtRegistrationNo) {
		this.txtRegistrationNo = txtRegistrationNo;
	}


	public String getTxtDeliveryPartNo() {
		return txtDeliveryPartNo;
	}


	public void setTxtDeliveryPartNo(String txtDeliveryPartNo) {
		this.txtDeliveryPartNo = txtDeliveryPartNo;
	}


	

	public Date getDteDeliveryDate() {
		return dteDeliveryDate;
	}


	public void setDteDeliveryDate(Date dteDeliveryDate) {
		this.dteDeliveryDate = dteDeliveryDate;
	}


	public List<SODetailDTO> getLstDetail() {
		return lstDetail;
	}


	public void setLstDetail(List<SODetailDTO> lstDetail) {
		this.lstDetail = lstDetail;
	}


	@Override
	public String toString() {
		return "SODTO [txtOrderNO=" + txtOrderNO + ", txtDocType=" + txtDocType + ", txtPurchaseNo=" + txtPurchaseNo
				+ ", txtPurchaseDate=" + txtPurchaseDate + ", txtDistChannel=" + txtDistChannel + ", txtSalesOrg="
				+ txtSalesOrg + ", dteDate=" + dteDate + ", dteDeliveryDate=" + dteDeliveryDate + ", txtChassisNo="
				+ txtChassisNo + ", txtEngineNo=" + txtEngineNo + ", txtRegistrationNo=" + txtRegistrationNo
				+ ", txtDeliveryPartNo=" + txtDeliveryPartNo + ", txtDealer=" + txtDealer + ", txtCustomer="
				+ txtCustomer + ", txtDealerName=" + txtDealerName + ", txtCustomerName=" + txtCustomerName
				+ ", txtPhone=" + txtPhone + ", txtCNIC=" + txtCNIC + ", txtCity=" + txtCity + ", level=" + level
				+ ", txtDivision=" + txtDivision + ", txtPmndistChanterms=" + txtPmndistChanterms + ", txtItemNo="
				+ txtItemNo + ", numQty=" + numQty + ", lstIds=" + lstIds + ", lstSchedule=" + lstSchedule
				+ ", lstDetail=" + lstDetail + ", txtPsoNo=" + txtPsoNo + ", txtDoDoc=" + txtDoDoc + ", txtChassis="
				+ txtChassis + ", txtEngine=" + txtEngine + ", blnFlag=" + blnFlag + "]";
	}
    
	
   

}
