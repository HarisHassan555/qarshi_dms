package com.bezkoder.spring.login.admin.bll.dto;

import java.math.BigDecimal;

public class DashBoardDto {
 
	private BigDecimal count;
	private Integer serId;
	
	private String txtName;
	private String txtCode;
	
	public DashBoardDto(Integer serId, String txtName, BigDecimal count) {
 		this.serId = serId;
		this.txtName = txtName;
		this.count = count;
	}
	
	public DashBoardDto(Integer serId, String txtName, long count) {
 		this.serId = serId;
		this.txtName = txtName;
		this.count = new BigDecimal(count);
	}
	
	public DashBoardDto(Integer serId, String txtName, String txtCode, BigDecimal count) {
 		this.serId = serId;
		this.txtName = txtName;
		this.txtCode = txtCode;
		this.count = count;
	}
	
	public DashBoardDto() {
 		
	}




	public BigDecimal getCount() {
		return count;
	}

	public void setCount(BigDecimal count) {
		this.count = count;
	}

	public Integer getSerId() {
		return serId;
	}

	public void setSerId(Integer serId) {
		this.serId = serId;
	}

	public String getTxtName() {
		return txtName;
	}

	public void setTxtName(String txtName) {
		this.txtName = txtName;
	}

	public String getTxtCode() {
		return txtCode;
	}

	public void setTxtCode(String txtCode) {
		this.txtCode = txtCode;
	}
	
	
  }