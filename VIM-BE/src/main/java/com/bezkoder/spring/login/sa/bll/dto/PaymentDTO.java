package com.bezkoder.spring.login.sa.bll.dto;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



public class PaymentDTO {
	
	
	
	
	
//	 "recm_Date": "2024-02-26T10:09:12.111Z",
//	  "recm_AdvancePay": 0,
//	  "cst_Code": "string",
//	  "paymod_Type": "string",
//	  "recm_TotalAmount": 0,
//	  "recm_WHTaxYesNo": 0,
//	  "recm_stax_detctn": 0,
//	  "recm_TotalWHTaxAmt": 0


//	{
//		 "recm_Date": "2024-02-18",
//		 "recm_AdvancePay": 0,
//		 "cst_Code": "003569",
//		 "paymod_Type": "000000000000002",
//		 "recm_TotalAmount": 10000,
//		 "recm_WHTaxYesNo": 2000
//		}

	@Override
	public String toString() {
		return "PaymentDTO [recm_Date=" + recm_Date + ", recm_AdvancePay=" + recm_AdvancePay + ", cst_Code=" + cst_Code
				+ ", paymod_Type=" + paymod_Type + ", recm_TotalAmount=" + recm_TotalAmount + ", recm_stax_detctn="
				+ recm_stax_detctn + ", recm_TotalWHTaxAmt=" + recm_TotalWHTaxAmt + ", recm_WHTaxYesNo="
				+ recm_WHTaxYesNo + ", bnk_code=" + bnk_code + ", recm_chqno=" + recm_chqno + ", recm_narration="
				+ recm_narration + "]";
	}
	private String recm_Date;
	private Integer recm_AdvancePay;
	
	private String cst_Code;
	private String paymod_Type;
    private BigDecimal recm_TotalAmount;
    
    private BigDecimal  recm_stax_detctn;
    
    private BigDecimal  recm_TotalWHTaxAmt;
    
    private Integer recm_WHTaxYesNo;
    
    private String bnk_code;
    
    private String dept_code;
    
    private String recm_chqno;
    
    private String recm_narration;
    
    
	public String getRecm_Date() {
		return recm_Date;
	}
	public void setRecm_Date(String recm_Date) {
		this.recm_Date = recm_Date;
	}
	public Integer getRecm_AdvancePay() {
		return recm_AdvancePay;
	}
	public void setRecm_AdvancePay(Integer recm_AdvancePay) {
		this.recm_AdvancePay = recm_AdvancePay;
	}
	public String getCst_Code() {
		return cst_Code;
	}
	public void setCst_Code(String cst_Code) {
		this.cst_Code = cst_Code;
	}
	public String getPaymod_Type() {
		return paymod_Type;
	}
	public void setPaymod_Type(String paymod_Type) {
		this.paymod_Type = paymod_Type;
	}
	public BigDecimal getRecm_TotalAmount() {
		return recm_TotalAmount;
	}
	public void setRecm_TotalAmount(BigDecimal recm_TotalAmount) {
		this.recm_TotalAmount = recm_TotalAmount;
	}
	public Integer getRecm_WHTaxYesNo() {
		return recm_WHTaxYesNo;
	}
	public void setRecm_WHTaxYesNo(Integer recm_WHTaxYesNo) {
		this.recm_WHTaxYesNo = recm_WHTaxYesNo;
	}
	public BigDecimal getRecm_stax_detctn() {
		return recm_stax_detctn;
	}
	public void setRecm_stax_detctn(BigDecimal recm_stax_detctn) {
		this.recm_stax_detctn = recm_stax_detctn;
	}
	public BigDecimal getRecm_TotalWHTaxAmt() {
		return recm_TotalWHTaxAmt;
	}
	public void setRecm_TotalWHTaxAmt(BigDecimal recm_TotalWHTaxAmt) {
		this.recm_TotalWHTaxAmt = recm_TotalWHTaxAmt;
	}
	public String getBnk_code() {
		return bnk_code;
	}
	public void setBnk_code(String bnk_code) {
		this.bnk_code = bnk_code;
	}
	public String getRecm_chqno() {
		return recm_chqno;
	}
	public void setRecm_chqno(String recm_chqno) {
		this.recm_chqno = recm_chqno;
	}
	public String getRecm_narration() {
		return recm_narration;
	}
	public void setRecm_narration(String recm_narration) {
		this.recm_narration = recm_narration;
	}
	public String getDept_code() {
		return dept_code;
	}
	public void setDept_code(String dept_code) {
		this.dept_code = dept_code;
	}
    
    

}
