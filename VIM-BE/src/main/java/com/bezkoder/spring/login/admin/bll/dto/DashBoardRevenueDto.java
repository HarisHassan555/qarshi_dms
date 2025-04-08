package com.bezkoder.spring.login.admin.bll.dto;

import java.math.BigDecimal;

public class DashBoardRevenueDto {
 
	private BigDecimal amount;
	private Integer dealerId;
    private String dealerName;
    private String monthYear;
    private String dealerCode;
    
    private String dealerAddress;
    private String dealerNTN;
    private String dealerCNIC;
    
    public DashBoardRevenueDto() {
    	
    }
    
    public DashBoardRevenueDto(String dealerName, String dealerCode, String dealerAddress, String dealerNTN, String dealerCNIC) {
		this.dealerName = dealerName;
		this.dealerCode = dealerCode;
		this.dealerAddress = dealerAddress;
		this.dealerNTN = dealerNTN;
		this.dealerCNIC=dealerCNIC;
	}
    
    public DashBoardRevenueDto(String monthYear, String delearName, BigDecimal amount, int dealerId) {
		this.monthYear = monthYear;
		this.dealerId = dealerId;
		this.dealerName = delearName;
		this.amount = amount;
	}
    
    public DashBoardRevenueDto(String monthYear,String delearCode, String delearName, BigDecimal amount, int dealerId) {
		this.monthYear = monthYear;
		this.dealerId = dealerId;
		this.dealerCode=delearCode;
		this.dealerName = delearName;
		this.amount = amount;
	}
    
    
    public DashBoardRevenueDto(String monthYear, String delearName, String delearCode, BigDecimal amount) {
		this.monthYear = monthYear;
		this.dealerId = dealerId;
		this.dealerName = delearName;
		this.dealerCode=delearCode;
		this.amount = amount;
	}
    
	 
    public DashBoardRevenueDto(String monthYear, BigDecimal amount) {
		this.monthYear = monthYear;
 		this.amount = amount;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Integer getDealerId() {
		return dealerId;
	}

	public void setDealerId(Integer dealerId) {
		this.dealerId = dealerId;
	}

	public String getDealerName() {
		return dealerName;
	}

	public void setDealerName(String dealerName) {
		this.dealerName = dealerName;
	}

	public String getMonthYear() {
		return monthYear;
	}

	public void setMonthYear(String monthYear) {
		this.monthYear = monthYear;
	}

	public String getDealerCode() {
		return dealerCode;
	}

	public void setDealerCode(String dealerCode) {
		this.dealerCode = dealerCode;
	}

	public String getDealerAddress() {
		return dealerAddress;
	}

	public void setDealerAddress(String dealerAddress) {
		this.dealerAddress = dealerAddress;
	}

	public String getDealerNTN() {
		return dealerNTN;
	}

	public void setDealerNTN(String dealerNTN) {
		this.dealerNTN = dealerNTN;
	}

	public String getDealerCNIC() {
		return dealerCNIC;
	}

	public void setDealerCNIC(String dealerCNIC) {
		this.dealerCNIC = dealerCNIC;
	}


	
	
}
