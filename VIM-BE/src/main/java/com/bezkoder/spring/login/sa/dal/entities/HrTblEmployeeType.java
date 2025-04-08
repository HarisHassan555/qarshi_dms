package com.bezkoder.spring.login.sa.dal.entities;

import javax.persistence.*;


@Entity
@Table(name="hr_tbl_employee_types")
@NamedQuery(name="hr_tbl_employee_types.findAll", query="SELECT e FROM HrTblEmployeeType e")
public class HrTblEmployeeType {

	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hr_tbl_employee_types_ser_employee_type_id_seq")
	@SequenceGenerator(name = "hr_tbl_employee_types_ser_employee_type_id_seq", sequenceName = "hr_tbl_employee_types_ser_employee_type_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_employee_type_id")
	  int serEmployeeTypeId;
	  
	  @Column(name="txt_employee_type")
	  String txtEmployeeType;
	  
	  @Column(name="txt_description")
	  String txtDescription;
	  
	  @Column(name="bln_status")
	  boolean blnStatus;
	  
	  @Column(name="txt_machine_ip")
	  String txtMachineIp;
	  
	  @Column(name="ser_modified_user_id")
	  int serModifiedUserId;
	  
	  @Column(name="ser_created_user_id")
	  int seCreatedUserId;
	  
	  @Column(name="dte_created_date")
	  java.sql.Timestamp dteCreateddate;
	  
	  @Column(name="dte_modified_date0")
	  java.sql.Timestamp dteModifieddate;
	  
	  @Column(name="num_probation_days")
	  int numProbationDays;
	
//	  public int getSerEmployeeTypeId() {
//		return serEmployeeTypeId;
//	}
//
//	public void setSerEmployeeTypeId(int serEmployeeTypeId) {
//		this.serEmployeeTypeId = serEmployeeTypeId;
//	}

	public String getTxtEmployeeType() {
		return txtEmployeeType;
	}

	public void setTxtEmployeeType(String txtEmployeeType) {
		this.txtEmployeeType = txtEmployeeType;
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

	public String getTxtMachineIp() {
		return txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public int getSerModifiedUserId() {
		return serModifiedUserId;
	}

	public void setSerModifiedUserId(int serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public int getSeCreatedUserId() {
		return seCreatedUserId;
	}

	public void setSeCreatedUserId(int seCreatedUserId) {
		this.seCreatedUserId = seCreatedUserId;
	}

	public java.sql.Timestamp getDteCreateddate() {
		return dteCreateddate;
	}

	public void setDteCreateddate(java.sql.Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public java.sql.Timestamp getDteModifieddate() {
		return dteModifieddate;
	}

	public void setDteModifieddate(java.sql.Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public int getNumProbationDays() {
		return numProbationDays;
	}

	public void setNumProbationDays(int numProbationDays) {
		this.numProbationDays = numProbationDays;
	}
	
	 public int getSerEmployeeTypeId() {
		 return serEmployeeTypeId;
	 }

	 public void setSerEmployeeTypeId(int serEmployeeTypeId) {
	    this.serEmployeeTypeId = serEmployeeTypeId;
	 }
}
