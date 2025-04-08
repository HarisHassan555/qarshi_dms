package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.Timestamp;

import javax.persistence.*;



@Entity
@Table(name="hr_tbl_job")
@NamedQuery(name="HrTblJob.findAll", query="SELECT h FROM HrTblJob h")
public class HrTblJob implements Serializable {
 
	private static final long serialVersionUID = 1L;

	@Id
	  @GeneratedValue(strategy=GenerationType.AUTO)
 	  @Column(name="ser_job_id")
	  private int serJobId;

   	  @Column(name="txt_job_name")
	  private String txtJobName;
	  
	  @Column(name="txt_description")
	  private String txtDescription;
	  
	  @Column(name="bln_status")
	  private boolean blnStatus;
	  
	  @Column(name="rate")
	  private BigDecimal rate;
	  
	  @Column(name="rate_per_hour")
	  private BigDecimal ratePerHour;
	  
	  @Column(name="txtMachineIp")
	  private String txt_machine_ip;
	  
	  @Column(name="serModifiedUserId")
	  private Integer ser_modified_user_id;
	  
	  @Column(name="serCreatedUserId")
	  private Integer ser_created_user_id;
	  
	  @Column(name="dteCreateddate")
	  private Timestamp dte_createddate;
	  
	  @Column(name="dteModifieddate")
	  private Timestamp dte_modifieddate;
	  
 
	  public int getSerJobId() {
		return serJobId;
	}

	public void setSerJobId(int serJobId) {
		this.serJobId = serJobId;
	}

	public String getTxtJobName() {
		return txtJobName;
	}

	public void setTxtJobName(String txtJobName) {
		this.txtJobName = txtJobName;
	}

	public String getTxtDescription() {
		return txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public boolean isBlnStatus() {
		return blnStatus;
	}

	public void setBlnStatus(boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public String getTxt_machine_ip() {
		return txt_machine_ip;
	}

	public void setTxt_machine_ip(String txt_machine_ip) {
		this.txt_machine_ip = txt_machine_ip;
	}

	public Integer getSer_modified_user_id() {
		return ser_modified_user_id;
	}

	public void setSer_modified_user_id(Integer ser_modified_user_id) {
		this.ser_modified_user_id = ser_modified_user_id;
	}

	public Integer getSer_created_user_id() {
		return ser_created_user_id;
	}

	public void setSer_created_user_id(Integer ser_created_user_id) {
		this.ser_created_user_id = ser_created_user_id;
	}

	public Timestamp getDte_createddate() {
		return dte_createddate;
	}

	public void setDte_createddate(Timestamp dte_createddate) {
		this.dte_createddate = dte_createddate;
	}

	public Timestamp getDte_modifieddate() {
		return dte_modifieddate;
	}

	public void setDte_modifieddate(Timestamp dte_modifieddate) {
		this.dte_modifieddate = dte_modifieddate;
	}

	public BigDecimal getRatePerHour() {
		return ratePerHour;
	}

	public void setRatePerHour(BigDecimal ratePerHour) {
		this.ratePerHour = ratePerHour;
	}

}
