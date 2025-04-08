package com.bezkoder.spring.login.admin.bll.dto;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

public class TIRDTO {
	

	
	private Integer serTIRId;
	private String txtTIRNo;
	private Integer serGroupId;
	private String txtDriver;
	private String txtDriverPhoneNo;
	private Timestamp dteTimeIn;
	private Date dteTimeOut;
	private Timestamp dtePromiseTime;
	private BigDecimal numPreviousMillage;
	private String txtEstimatedRef;
	private String txtWarrantyType;
	private Date dteInvoiceDate;
	private Date dteDate;
	private String txtRemarks;
	private Time timDeliveryTime;
	private String txtComplain;
	private String txtDetailOfRepair;
	private String txtCauseOfFailure;
	private Integer numLevel;
	private String txtStatus;
	
	private String txtCustomerName;
	private String txtDealerName;
	private String txtModelName;
	
	private String txtChassisNo;
	private String txtEngineNo;
	private String txtProductCategoryName;
	private String  txtRegistrationNo;
	private String txtColorName;

	
	private String txtStatus1;
	private String txtStatus2;
	private String txtStatus3;
	private String txtLevel;
	
	private Date dteApproveddate1;
	private Date dteApproveddate2;
	private Date dteApproveddate3;


	public TIRDTO()
	{
		
	}
	
//	
//	(TIR.dteDate,TIR.txtTIRNo,TIR.cfgTblDealer.txtCustomerName,TIR.slsTblSoVehicleDetail.cfgTblModel.txtModelName,
//			TIR.slsTblSoVehicleDetail.txtChassisNo,TIR.slsTblSoVehicleDetail.txtEngineNo,TIR.cfgTblProduct.cfgTblProductCategory.txtProductCategoryName,
//			TIR.slsTblSoVehicleDetail.cfgTblColor.txtColorName,
//			TIR.dteTimeOut,TIR.txtStatus1,TIR.txtStatus2,TIR.txtStatus3,TIR.txtStatus1,txtLevel
	
//	java.util.Date, 
//	java.lang.String, 
//	java.lang.String,
//	java.lang.String, 
//	java.lang.String, 
//	java.lang.String, 
//	java.lang.String, 
//	java.lang.String, 
//	java.util.Date, 
//	java.lang.String,
//	java.lang.String,
//	java.lang.String, 
//	java.lang.String, 
//	java.lang.String

	
	public TIRDTO(String txtRegistrationNo,int serTIRId,Date dteDate,String txtTIRNo,String txtCustomerName,String txtModelName,String txtChassisNo,String txtEngineNo,String txtProductCategoryName,String txtColorName,
			Date dteTimeOut,String txtStatus1,String txtStatus2,String txtStatus3,String txtLevel,Date dteApproveddate1,Date dteApproveddate2,Date dteApproveddate3)
	{
		this.txtRegistrationNo=txtRegistrationNo;
		this.serTIRId=serTIRId;
		this.dteDate=dteDate;
		this.txtTIRNo=txtTIRNo;
		this.txtCustomerName=txtCustomerName;
		this.txtModelName=txtModelName;
		this.txtChassisNo=txtChassisNo;
		this.txtEngineNo=txtEngineNo;
		this.txtColorName=txtColorName;
		this.dteTimeOut=dteTimeOut;
		
		this.txtStatus1=txtStatus1;
		this.txtStatus2=txtStatus2;
		this.txtStatus3=txtStatus3;
		this.txtLevel=txtLevel;
		this.dteApproveddate1=dteApproveddate1;
		this.dteApproveddate2=dteApproveddate2;
		this.dteApproveddate3=dteApproveddate3;
	}
//	
//	
//	public TIRDTO(int serProductId,String txtProductCode,String txtProductName,String txtQuality,Boolean blnStatus,BigDecimal numSalePrice,String txtType)
//	{
//		this.serProductId=serProductId;
//		this.txtProductCode=txtProductCode;
//		this.txtProductName=txtProductName;
//		this.txtQuality=txtQuality;
//		this.txtType=txtType;
//		this.blnStatus=blnStatus;
//		this.numSalePrice=numSalePrice;
//	}


	public Integer getSerTIRId() {
		return serTIRId;
	}


	public void setSerTIRId(Integer serTIRId) {
		this.serTIRId = serTIRId;
	}


	public String getTxtTIRNo() {
		return txtTIRNo;
	}


	public void setTxtTIRNo(String txtTIRNo) {
		this.txtTIRNo = txtTIRNo;
	}


	public Integer getSerGroupId() {
		return serGroupId;
	}


	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}


	public String getTxtDriver() {
		return txtDriver;
	}


	public void setTxtDriver(String txtDriver) {
		this.txtDriver = txtDriver;
	}


	public String getTxtDriverPhoneNo() {
		return txtDriverPhoneNo;
	}


	public void setTxtDriverPhoneNo(String txtDriverPhoneNo) {
		this.txtDriverPhoneNo = txtDriverPhoneNo;
	}


	public Timestamp getDteTimeIn() {
		return dteTimeIn;
	}


	public void setDteTimeIn(Timestamp dteTimeIn) {
		this.dteTimeIn = dteTimeIn;
	}


	public Date getDteTimeOut() {
		return dteTimeOut;
	}


	public void setDteTimeOut(Date dteTimeOut) {
		this.dteTimeOut = dteTimeOut;
	}


	public Timestamp getDtePromiseTime() {
		return dtePromiseTime;
	}


	public void setDtePromiseTime(Timestamp dtePromiseTime) {
		this.dtePromiseTime = dtePromiseTime;
	}


	public BigDecimal getNumPreviousMillage() {
		return numPreviousMillage;
	}


	public void setNumPreviousMillage(BigDecimal numPreviousMillage) {
		this.numPreviousMillage = numPreviousMillage;
	}


	public String getTxtEstimatedRef() {
		return txtEstimatedRef;
	}


	public void setTxtEstimatedRef(String txtEstimatedRef) {
		this.txtEstimatedRef = txtEstimatedRef;
	}


	public String getTxtWarrantyType() {
		return txtWarrantyType;
	}


	public void setTxtWarrantyType(String txtWarrantyType) {
		this.txtWarrantyType = txtWarrantyType;
	}


	public Date getDteInvoiceDate() {
		return dteInvoiceDate;
	}


	public void setDteInvoiceDate(Date dteInvoiceDate) {
		this.dteInvoiceDate = dteInvoiceDate;
	}


	public Date getDteDate() {
		return dteDate;
	}


	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}


	public String getTxtRemarks() {
		return txtRemarks;
	}


	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}


	public Time getTimDeliveryTime() {
		return timDeliveryTime;
	}


	public void setTimDeliveryTime(Time timDeliveryTime) {
		this.timDeliveryTime = timDeliveryTime;
	}


	public String getTxtComplain() {
		return txtComplain;
	}


	public void setTxtComplain(String txtComplain) {
		this.txtComplain = txtComplain;
	}


	public String getTxtDetailOfRepair() {
		return txtDetailOfRepair;
	}


	public void setTxtDetailOfRepair(String txtDetailOfRepair) {
		this.txtDetailOfRepair = txtDetailOfRepair;
	}


	public String getTxtCauseOfFailure() {
		return txtCauseOfFailure;
	}


	public void setTxtCauseOfFailure(String txtCauseOfFailure) {
		this.txtCauseOfFailure = txtCauseOfFailure;
	}


	public Integer getNumLevel() {
		return numLevel;
	}


	public void setNumLevel(Integer numLevel) {
		this.numLevel = numLevel;
	}


	public String getTxtStatus() {
		return txtStatus;
	}


	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}


	public String getTxtCustomerName() {
		return txtCustomerName;
	}


	public void setTxtCustomerName(String txtCustomerName) {
		this.txtCustomerName = txtCustomerName;
	}


	public String getTxtDealerName() {
		return txtDealerName;
	}


	public void setTxtDealerName(String txtDealerName) {
		this.txtDealerName = txtDealerName;
	}


	public String getTxtModelName() {
		return txtModelName;
	}


	public void setTxtModelName(String txtModelName) {
		this.txtModelName = txtModelName;
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


	public String getTxtProductCategoryName() {
		return txtProductCategoryName;
	}


	public void setTxtProductCategoryName(String txtProductCategoryName) {
		this.txtProductCategoryName = txtProductCategoryName;
	}


	public String getTxtColorName() {
		return txtColorName;
	}


	public void setTxtColorName(String txtColorName) {
		this.txtColorName = txtColorName;
	}


	public String getTxtStatus1() {
		return txtStatus1;
	}


	public void setTxtStatus1(String txtStatus1) {
		this.txtStatus1 = txtStatus1;
	}


	public String getTxtStatus2() {
		return txtStatus2;
	}


	public void setTxtStatus2(String txtStatus2) {
		this.txtStatus2 = txtStatus2;
	}


	public String getTxtStatus3() {
		return txtStatus3;
	}


	public void setTxtStatus3(String txtStatus3) {
		this.txtStatus3 = txtStatus3;
	}


	public String getTxtLevel() {
		return txtLevel;
	}


	public void setTxtLevel(String txtLevel) {
		this.txtLevel = txtLevel;
	}

	public String getTxtRegistrationNo() {
		return txtRegistrationNo;
	}

	public void setTxtRegistrationNo(String txtRegistrationNo) {
		this.txtRegistrationNo = txtRegistrationNo;
	}

	public Date getDteApproveddate1() {
		return dteApproveddate1;
	}

	public void setDteApproveddate1(Date dteApproveddate1) {
		this.dteApproveddate1 = dteApproveddate1;
	}

	public Date getDteApproveddate2() {
		return dteApproveddate2;
	}

	public void setDteApproveddate2(Date dteApproveddate2) {
		this.dteApproveddate2 = dteApproveddate2;
	}

	public Date getDteApproveddate3() {
		return dteApproveddate3;
	}

	public void setDteApproveddate3(Date dteApproveddate3) {
		this.dteApproveddate3 = dteApproveddate3;
	}

	
	
	
	
	
	
}
