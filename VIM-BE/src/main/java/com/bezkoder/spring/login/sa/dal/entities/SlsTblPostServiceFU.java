package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name = "sls_tbl_post_service_follow_up")
@NamedQuery(name = "SlsTblPostServiceFU.findAll", query = "SELECT s FROM SlsTblPostServiceFU s")
public class SlsTblPostServiceFU implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SlsTblPostServiceFU_ser_work_order_id_seq")
	@SequenceGenerator(name = "SlsTblPostServiceFU_ser_work_order_id_seq", sequenceName = "SlsTblPostServiceFU_ser_work_order_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_post_service_fu_id")
	private Integer serPOSTServiceFUId;

	@Column(name = "txt_code")
	private String txtCode;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_date")
	private Date dteDate;

	@Column(name = "txt_remarks")
	private String txtRemarks;
	
	@Column(name = "txt_complaint")
	private String txtcomplaint;


	@Column(name = "txt_status")
	private String txtStatus;
	
	@Column(name = "txt_Q1")
	private String txtQ1;
	
	@Column(name = "txt_Q2")
	private String txtQ2;
	
	@Column(name = "txt_Q3")
	private String txtQ3;
	
	@Column(name = "txt_Q4")
	private String txtQ4;
	
	@Column(name = "txt_Q5")
	private String txtQ5;
	
	@Column(name = "txt_Q6")
	private String txtQ6;
	

	@Column(name="dte_call1")
	private Timestamp dteCall1;
	
	@Column(name="dte_call2")
	private Timestamp dteCall2;
	
	@Column(name="dte_call3")
	private Timestamp dteCall3;
	
	@Column(name="dte_call4")
	private Timestamp dteCall4;
	
	@Column(name="dte_call5")
	private Timestamp dteCall5;
	
	@Column(name="dte_call6")
	private Timestamp dteCall6;
	
	@Column(name="dte_appointment_date")
	private Timestamp dteAppointmentDate;
	
	@Column(name = "txt_remarks1")
	private String txtRemarks1;
	
	@Column(name = "txt_remarks2")
	private String txtRemarks2;
	
	@Column(name = "txt_remarks3")
	private String txtRemarks3;
	
	@Column(name = "txt_remarks4")
	private String txtRemarks4;
	
	@Column(name = "txt_remarks5")
	private String txtRemarks5;
	
	@Column(name = "txt_remarks6")
	private String txtRemarks6;
	
	
	@Column(name = "txt_customer_remarks")
	private String txtCustomerRemarks;
	
	
	@Column(name = "txt_further_action")
	private String txtFurtherAction;
	
	
	@Column(name = "bl_is_closed")
	private Boolean blIsClosed;
	
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_product_id")
	private CfgTblProduct cfgTblProduct;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_work_order_id")
	private SlsTblWorkOrder slsTblWorkOrder;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;

	private String dte_date_from;

	private String dte_date_to;

	@Column(name = "bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name = "dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name = "dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name = "ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name = "ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name = "txt_machine_ip")
	private String txtMachineIp;

	

	public Integer getSerPOSTServiceFUId() {
		return serPOSTServiceFUId;
	}

	public void setSerPOSTServiceFUId(Integer serPOSTServiceFUId) {
		this.serPOSTServiceFUId = serPOSTServiceFUId;
	}

	public String getTxtCode() {
		return txtCode;
	}

	public void setTxtCode(String txtCode) {
		this.txtCode = txtCode;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
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

	public String getTxtStatus() {
		return txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	public String getTxtQ1() {
		return txtQ1;
	}

	public void setTxtQ1(String txtQ1) {
		this.txtQ1 = txtQ1;
	}

	public String getTxtQ2() {
		return txtQ2;
	}

	public void setTxtQ2(String txtQ2) {
		this.txtQ2 = txtQ2;
	}

	public String getTxtQ3() {
		return txtQ3;
	}

	public void setTxtQ3(String txtQ3) {
		this.txtQ3 = txtQ3;
	}

	public String getTxtQ4() {
		return txtQ4;
	}

	public void setTxtQ4(String txtQ4) {
		this.txtQ4 = txtQ4;
	}

	public String getTxtQ5() {
		return txtQ5;
	}

	public void setTxtQ5(String txtQ5) {
		this.txtQ5 = txtQ5;
	}

	public String getTxtQ6() {
		return txtQ6;
	}

	public void setTxtQ6(String txtQ6) {
		this.txtQ6 = txtQ6;
	}

	public Timestamp getDteCall1() {
		return dteCall1;
	}

	public void setDteCall1(Timestamp dteCall1) {
		this.dteCall1 = dteCall1;
	}

	public Timestamp getDteCall2() {
		return dteCall2;
	}

	public void setDteCall2(Timestamp dteCall2) {
		this.dteCall2 = dteCall2;
	}

	public Timestamp getDteCall3() {
		return dteCall3;
	}

	public void setDteCall3(Timestamp dteCall3) {
		this.dteCall3 = dteCall3;
	}

	public Timestamp getDteCall4() {
		return dteCall4;
	}

	public void setDteCall4(Timestamp dteCall4) {
		this.dteCall4 = dteCall4;
	}

	public Timestamp getDteCall5() {
		return dteCall5;
	}

	public void setDteCall5(Timestamp dteCall5) {
		this.dteCall5 = dteCall5;
	}

	public Timestamp getDteCall6() {
		return dteCall6;
	}

	public void setDteCall6(Timestamp dteCall6) {
		this.dteCall6 = dteCall6;
	}

	public Timestamp getDteAppointmentDate() {
		return dteAppointmentDate;
	}

	public void setDteAppointmentDate(Timestamp dteAppointmentDate) {
		this.dteAppointmentDate = dteAppointmentDate;
	}

	public String getTxtRemarks1() {
		return txtRemarks1;
	}

	public void setTxtRemarks1(String txtRemarks1) {
		this.txtRemarks1 = txtRemarks1;
	}

	public String getTxtRemarks2() {
		return txtRemarks2;
	}

	public void setTxtRemarks2(String txtRemarks2) {
		this.txtRemarks2 = txtRemarks2;
	}

	public String getTxtRemarks3() {
		return txtRemarks3;
	}

	public void setTxtRemarks3(String txtRemarks3) {
		this.txtRemarks3 = txtRemarks3;
	}

	public String getTxtRemarks4() {
		return txtRemarks4;
	}

	public void setTxtRemarks4(String txtRemarks4) {
		this.txtRemarks4 = txtRemarks4;
	}

	public String getTxtRemarks5() {
		return txtRemarks5;
	}

	public void setTxtRemarks5(String txtRemarks5) {
		this.txtRemarks5 = txtRemarks5;
	}

	public String getTxtRemarks6() {
		return txtRemarks6;
	}

	public void setTxtRemarks6(String txtRemarks6) {
		this.txtRemarks6 = txtRemarks6;
	}

	public String getTxtCustomerRemarks() {
		return txtCustomerRemarks;
	}

	public void setTxtCustomerRemarks(String txtCustomerRemarks) {
		this.txtCustomerRemarks = txtCustomerRemarks;
	}

	public String getTxtFurtherAction() {
		return txtFurtherAction;
	}

	public void setTxtFurtherAction(String txtFurtherAction) {
		this.txtFurtherAction = txtFurtherAction;
	}

	public Boolean getBlIsClosed() {
		return blIsClosed;
	}

	public void setBlIsClosed(Boolean blIsClosed) {
		this.blIsClosed = blIsClosed;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public SlsTblWorkOrder getSlsTblWorkOrder() {
		return slsTblWorkOrder;
	}

	public void setSlsTblWorkOrder(SlsTblWorkOrder slsTblWorkOrder) {
		this.slsTblWorkOrder = slsTblWorkOrder;
	}

	public SlsTblSoVehicleDetail getSlsTblSoVehicleDetail() {
		return slsTblSoVehicleDetail;
	}

	public void setSlsTblSoVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail) {
		this.slsTblSoVehicleDetail = slsTblSoVehicleDetail;
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

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Timestamp getDteCreateddate() {
		return dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Timestamp getDteModifieddate() {
		return dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public Integer getSerCreatedUserId() {
		return serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerModifiedUserId() {
		return serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public String getTxtMachineIp() {
		return txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtcomplaint() {
		return txtcomplaint;
	}

	public void setTxtcomplaint(String txtcomplaint) {
		this.txtcomplaint = txtcomplaint;
	}
	
	

}