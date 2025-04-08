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
@Table(name = "sls_tbl_Appointment")
@NamedQuery(name = "SlsTblAppointment.findAll", query = "SELECT s FROM SlsTblAppointment s")
public class SlsTblAppointment implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_so_detail_id") private Integer serSoDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SlsTblAppointment_ser_Appointment_id_seq")
	@SequenceGenerator(name = "SlsTblAppointment_ser_Appointment_id_seq", sequenceName = "SlsTblAppointment_ser_Appointment_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_Appointment_id")
	private Integer serAppointmentId;

	@Column(name = "txt_Appointment_no")
	private String txtAppointmentNo;

	@Column(name = "ser_group_id")
	private Integer serGroupId;
	
	 @Column(name="dte_appointment_date")
	  Timestamp dteappointmentDate;
	 
	 @Column(name="dte_date_resolved")
	  Timestamp dteDateResolved;
	 
	  @Column(name = "txt_time_Resolve")
		private String txtTimeResolve;
		  
	  @Column(name = "txt_time_in")
		private String txtTimeIn;
	  
		@Column(name = "duration_in_days")
		private Integer durationInDays;
	  

	@Column(name = "txt_Appointment_type")
	private String txtAppointmenttype;

	@Column(name = "txt_Appointment_chanel")
	private String txtAppointmentChannel;


	@Temporal(TemporalType.DATE)
	@Column(name = "dte_invoice_date")
	private Date dteInvoiceDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_date")
	private Date dteDate;
	
	@Column(name = "txt_voc")
	private String txtVOC;

	@Column(name = "txt_remarks")
	private String txtRemarks;
	
	@Column(name = "txt_action_taken")
	private String txtActionTaken;

	@Column(name = "txt_status")
	private String txtStatus;
	
	
	@Column(name = "txt_additional_Details")
	private String txtAdditionalDetails;
	
	@Column(name = "txt_registration_no")
	private String txtRegistrationNo;
	

	@Column(name = "num_current_millage")
	private BigDecimal numCurrentMillage;
	

	@Column(name = "bl_is_satisfied")
	private Boolean blIsSatisfied;
	
	

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

	@Column(name="tim_delivery_time")
	private Time timDeliveryTime;
	// bi-directional many-to-one association to SlsTblSaleOrder
//	@OneToMany
//	@JoinColumn(name = "ser_Appointment_detail_id")
//	@Column(name = "ser_Appointment_detail_id")
//	private List<SlsTblAppointmentDetail> SlsTblAppointmentDetail;

	


	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	



	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;
	
    private String dte_date_from;
	
	
	private String dte_date_to;


	public Integer getSerAppointmentId() {
		return serAppointmentId;
	}


	public void setSerAppointmentId(Integer serAppointmentId) {
		this.serAppointmentId = serAppointmentId;
	}


	public String getTxtAppointmentNo() {
		return txtAppointmentNo;
	}


	public void setTxtAppointmentNo(String txtAppointmentNo) {
		this.txtAppointmentNo = txtAppointmentNo;
	}


	public Integer getSerGroupId() {
		return serGroupId;
	}


	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}


	public Timestamp getDteappointmentDate() {
		return dteappointmentDate;
	}


	public void setDteappointmentDate(Timestamp dteappointmentDate) {
		this.dteappointmentDate = dteappointmentDate;
	}


	public Timestamp getDteDateResolved() {
		return dteDateResolved;
	}


	public void setDteDateResolved(Timestamp dteDateResolved) {
		this.dteDateResolved = dteDateResolved;
	}


	public String getTxtTimeResolve() {
		return txtTimeResolve;
	}


	public void setTxtTimeResolve(String txtTimeResolve) {
		this.txtTimeResolve = txtTimeResolve;
	}


	public String getTxtTimeIn() {
		return txtTimeIn;
	}


	public void setTxtTimeIn(String txtTimeIn) {
		this.txtTimeIn = txtTimeIn;
	}


	public Integer getDurationInDays() {
		return durationInDays;
	}


	public void setDurationInDays(Integer durationInDays) {
		this.durationInDays = durationInDays;
	}


	public String getTxtAppointmenttype() {
		return txtAppointmenttype;
	}


	public void setTxtAppointmenttype(String txtAppointmenttype) {
		this.txtAppointmenttype = txtAppointmenttype;
	}


	public String getTxtAppointmentChannel() {
		return txtAppointmentChannel;
	}


	public void setTxtAppointmentChannel(String txtAppointmentChannel) {
		this.txtAppointmentChannel = txtAppointmentChannel;
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


	public String getTxtVOC() {
		return txtVOC;
	}


	public void setTxtVOC(String txtVOC) {
		this.txtVOC = txtVOC;
	}


	public String getTxtRemarks() {
		return txtRemarks;
	}


	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}


	public String getTxtActionTaken() {
		return txtActionTaken;
	}


	public void setTxtActionTaken(String txtActionTaken) {
		this.txtActionTaken = txtActionTaken;
	}


	public String getTxtStatus() {
		return txtStatus;
	}


	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}


	public String getTxtAdditionalDetails() {
		return txtAdditionalDetails;
	}


	public void setTxtAdditionalDetails(String txtAdditionalDetails) {
		this.txtAdditionalDetails = txtAdditionalDetails;
	}


	public String getTxtRegistrationNo() {
		return txtRegistrationNo;
	}


	public void setTxtRegistrationNo(String txtRegistrationNo) {
		this.txtRegistrationNo = txtRegistrationNo;
	}


	public BigDecimal getNumCurrentMillage() {
		return numCurrentMillage;
	}


	public void setNumCurrentMillage(BigDecimal numCurrentMillage) {
		this.numCurrentMillage = numCurrentMillage;
	}


	public Boolean getBlIsSatisfied() {
		return blIsSatisfied;
	}


	public void setBlIsSatisfied(Boolean blIsSatisfied) {
		this.blIsSatisfied = blIsSatisfied;
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


	public Time getTimDeliveryTime() {
		return timDeliveryTime;
	}


	public void setTimDeliveryTime(Time timDeliveryTime) {
		this.timDeliveryTime = timDeliveryTime;
	}


	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}


	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
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


	

	

}