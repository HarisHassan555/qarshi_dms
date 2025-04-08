package com.bezkoder.spring.login.admin.bll.dto;

import java.math.BigDecimal;
import java.util.Date;


public class ProductDTO {
	
	private int ser_poduction_summary_id;	
	private int ser_product_id;
	private int ser_product_category_id;
	private int ser_product_family_id;
	private int ser_type_id;	
	private int serProductId;
	
	private String dte_date;
	private String dte_date_from;
	private String dte_date_to;
	
	private String txt_product_code;
	private String txt_product_name;
	
	private String txtVariantNo;	
	private String txtVariantName;
	private String txtVariantMold;
	private BigDecimal numLoosePcs;
	private BigDecimal numInnerQty;
	private BigDecimal numPCsInInner;
	private BigDecimal numInnerPcs;
	private BigDecimal numMCQty;
	private BigDecimal numPCsInMC;
	private BigDecimal numMCPcs;
	private BigDecimal numPLTQty;
	private BigDecimal numPCsInPLT;
	private BigDecimal numPLTPcs;
	private BigDecimal numQty;
	private BigDecimal numTotalPcs;
	private BigDecimal numTotalWt;	
	private Boolean blnLNP;	
	private Boolean blnLSP;	
	private Boolean blnSWH;
	public int getSer_poduction_summary_id() {
		return ser_poduction_summary_id;
	}
	public void setSer_poduction_summary_id(int ser_poduction_summary_id) {
		this.ser_poduction_summary_id = ser_poduction_summary_id;
	}
	public int getSer_product_id() {
		return ser_product_id;
	}
	public void setSer_product_id(int ser_product_id) {
		this.ser_product_id = ser_product_id;
	}
	public int getSer_product_category_id() {
		return ser_product_category_id;
	}
	public void setSer_product_category_id(int ser_product_category_id) {
		this.ser_product_category_id = ser_product_category_id;
	}
	public int getSer_product_family_id() {
		return ser_product_family_id;
	}
	public void setSer_product_family_id(int ser_product_family_id) {
		this.ser_product_family_id = ser_product_family_id;
	}
	public int getSer_type_id() {
		return ser_type_id;
	}
	public void setSer_type_id(int ser_type_id) {
		this.ser_type_id = ser_type_id;
	}
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
	public String getTxt_product_code() {
		return txt_product_code;
	}
	public void setTxt_product_code(String txt_product_code) {
		this.txt_product_code = txt_product_code;
	}
	public String getTxt_product_name() {
		return txt_product_name;
	}
	public void setTxt_product_name(String txt_product_name) {
		this.txt_product_name = txt_product_name;
	}
	public String getTxtVariantNo() {
		return txtVariantNo;
	}
	public void setTxtVariantNo(String txtVariantNo) {
		this.txtVariantNo = txtVariantNo;
	}
	public String getTxtVariantName() {
		return txtVariantName;
	}
	public void setTxtVariantName(String txtVariantName) {
		this.txtVariantName = txtVariantName;
	}
	public String getTxtVariantMold() {
		return txtVariantMold;
	}
	public void setTxtVariantMold(String txtVariantMold) {
		this.txtVariantMold = txtVariantMold;
	}
	public BigDecimal getNumLoosePcs() {
		return numLoosePcs;
	}
	public void setNumLoosePcs(BigDecimal numLoosePcs) {
		this.numLoosePcs = numLoosePcs;
	}
	public BigDecimal getNumInnerQty() {
		return numInnerQty;
	}
	public void setNumInnerQty(BigDecimal numInnerQty) {
		this.numInnerQty = numInnerQty;
	}
	public BigDecimal getNumPCsInInner() {
		return numPCsInInner;
	}
	public void setNumPCsInInner(BigDecimal numPCsInInner) {
		this.numPCsInInner = numPCsInInner;
	}
	public BigDecimal getNumInnerPcs() {
		return numInnerPcs;
	}
	public void setNumInnerPcs(BigDecimal numInnerPcs) {
		this.numInnerPcs = numInnerPcs;
	}
	public BigDecimal getNumMCQty() {
		return numMCQty;
	}
	public void setNumMCQty(BigDecimal numMCQty) {
		this.numMCQty = numMCQty;
	}
	public BigDecimal getNumPCsInMC() {
		return numPCsInMC;
	}
	public void setNumPCsInMC(BigDecimal numPCsInMC) {
		this.numPCsInMC = numPCsInMC;
	}
	public BigDecimal getNumMCPcs() {
		return numMCPcs;
	}
	public void setNumMCPcs(BigDecimal numMCPcs) {
		this.numMCPcs = numMCPcs;
	}
	public BigDecimal getNumPLTQty() {
		return numPLTQty;
	}
	public void setNumPLTQty(BigDecimal numPLTQty) {
		this.numPLTQty = numPLTQty;
	}
	public BigDecimal getNumPCsInPLT() {
		return numPCsInPLT;
	}
	public void setNumPCsInPLT(BigDecimal numPCsInPLT) {
		this.numPCsInPLT = numPCsInPLT;
	}
	public BigDecimal getNumPLTPcs() {
		return numPLTPcs;
	}
	public void setNumPLTPcs(BigDecimal numPLTPcs) {
		this.numPLTPcs = numPLTPcs;
	}
	public BigDecimal getNumQty() {
		return numQty;
	}
	public void setNumQty(BigDecimal numQty) {
		this.numQty = numQty;
	}
	public BigDecimal getNumTotalPcs() {
		return numTotalPcs;
	}
	public void setNumTotalPcs(BigDecimal numTotalPcs) {
		this.numTotalPcs = numTotalPcs;
	}
	public BigDecimal getNumTotalWt() {
		return numTotalWt;
	}
	public void setNumTotalWt(BigDecimal numTotalWt) {
		this.numTotalWt = numTotalWt;
	}
	public Boolean getBlnLNP() {
		return blnLNP;
	}
	public void setBlnLNP(Boolean blnLNP) {
		this.blnLNP = blnLNP;
	}
	public Boolean getBlnLSP() {
		return blnLSP;
	}
	public void setBlnLSP(Boolean blnLSP) {
		this.blnLSP = blnLSP;
	}
	public Boolean getBlnSWH() {
		return blnSWH;
	}
	public void setBlnSWH(Boolean blnSWH) {
		this.blnSWH = blnSWH;
	}
	public int getSerProductId() {
		return serProductId;
	}
	public void setSerProductId(int serProductId) {
		this.serProductId = serProductId;
	}
	
	
}
