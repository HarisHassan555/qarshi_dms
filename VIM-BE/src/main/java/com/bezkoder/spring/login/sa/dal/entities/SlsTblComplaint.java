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
@Table(name = "sls_tbl_complaint")
@NamedQuery(name = "SlsTblComplaint.findAll", query = "SELECT s FROM SlsTblComplaint s")
public class SlsTblComplaint implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_so_detail_id") private Integer serSoDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SlsTblComplaint_ser_complaint_id_seq")
	@SequenceGenerator(name = "SlsTblComplaint_ser_complaint_id_seq", sequenceName = "SlsTblComplaint_ser_complaint_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_complaint_id")
	private Integer serComplaintId;

	@Column(name = "txt_complaint_no")
	private String txtComplaintNo;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Column(name = "dte_date_receive")
	Timestamp dteDateReceive;

	@Column(name = "dte_date_resolved")
	Timestamp dteDateResolved;

	@Column(name = "txt_time_Resolve")
	private String txtTimeResolve;

	@Column(name = "txt_time_in")
	private String txtTimeIn;

	@Column(name = "duration_in_days")
	private Integer durationInDays;

	@Column(name = "txt_complaint_type")
	private String txtComplainttype;

	@Column(name = "txt_complaint_chanel")
	private String txtComplaintChannel;

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

	@Column(name = "txt_city")
	private String txtCity;

	@Column(name = "txt_customer_name")
	private String txtCustomerName;

	@Column(name = "txt_contact_no")
	private String txtContactNo;

	@Column(name = "txt_make")
	private String txtMake;

	@Column(name = "txt_time")
	private String txttime;
	
	@Column(name = "txt_type")
	private String txtType;
	
	@Column(name = "txt_department")
	private String txtDepartment;

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

	@Column(name = "tim_delivery_time")
	private Time timDeliveryTime;
	// bi-directional many-to-one association to SlsTblSaleOrder
//	@OneToMany
//	@JoinColumn(name = "ser_complaint_detail_id")
//	@Column(name = "ser_complaint_detail_id")
//	private List<SlsTblComplaintDetail> SlsTblComplaintDetail;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;
	
	
	@ManyToOne
	@JoinColumn(name = "ser_post_service_fu_id")
	private SlsTblPostServiceFU slsTblPostServiceFU;
	
	
	@ManyToOne
	@JoinColumn(name = "ser_post_sale_fu_id")
	private SlsTblPostSalesFU slsTblPostSalesFU;
	
	@Column(name="bl_is_from_social_media")
	private Boolean blIsFromSocialMedia;	
	
	@Column(name="bl_is_from_whats_app")
	private Boolean blIsFromWhatsApp;	
	
	@Column(name="bl_is_from_hot_line")
	private Boolean blIsFromHotline;	
	
	@Column(name="bl_is_inquiry")
	private Boolean blIsInquiry;
	
	private String dte_date_from;

	private String dte_date_to;

	public Integer getSerComplaintId() {
		return serComplaintId;
	}

	public void setSerComplaintId(Integer serComplaintId) {
		this.serComplaintId = serComplaintId;
	}

	public String getTxtComplaintNo() {
		return txtComplaintNo;
	}

	public void setTxtComplaintNo(String txtComplaintNo) {
		this.txtComplaintNo = txtComplaintNo;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public Timestamp getDteDateReceive() {
		return dteDateReceive;
	}

	public void setDteDateReceive(Timestamp dteDateReceive) {
		this.dteDateReceive = dteDateReceive;
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

	public String getTxtComplainttype() {
		return txtComplainttype;
	}

	public void setTxtComplainttype(String txtComplainttype) {
		this.txtComplainttype = txtComplainttype;
	}

	public String getTxtComplaintChannel() {
		return txtComplaintChannel;
	}

	public void setTxtComplaintChannel(String txtComplaintChannel) {
		this.txtComplaintChannel = txtComplaintChannel;
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

	public String getTxtCity() {
		return txtCity;
	}

	public void setTxtCity(String txtCity) {
		this.txtCity = txtCity;
	}

	public String getTxtCustomerName() {
		return txtCustomerName;
	}

	public void setTxtCustomerName(String txtCustomerName) {
		this.txtCustomerName = txtCustomerName;
	}

	public String getTxtContactNo() {
		return txtContactNo;
	}

	public void setTxtContactNo(String txtContactNo) {
		this.txtContactNo = txtContactNo;
	}

	public String getTxtMake() {
		return txtMake;
	}

	public void setTxtMake(String txtMake) {
		this.txtMake = txtMake;
	}

	public String getTxttime() {
		return txttime;
	}

	public void setTxttime(String txttime) {
		this.txttime = txttime;
	}

	public String getTxtType() {
		return txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public SlsTblPostServiceFU getSlsTblPostServiceFU() {
		return slsTblPostServiceFU;
	}

	public void setSlsTblPostServiceFU(SlsTblPostServiceFU slsTblPostServiceFU) {
		this.slsTblPostServiceFU = slsTblPostServiceFU;
	}

	public SlsTblPostSalesFU getSlsTblPostSalesFU() {
		return slsTblPostSalesFU;
	}

	public void setSlsTblPostSalesFU(SlsTblPostSalesFU slsTblPostSalesFU) {
		this.slsTblPostSalesFU = slsTblPostSalesFU;
	}

	public Boolean getBlIsFromSocialMedia() {
		return blIsFromSocialMedia;
	}

	public void setBlIsFromSocialMedia(Boolean blIsFromSocialMedia) {
		this.blIsFromSocialMedia = blIsFromSocialMedia;
	}

	public Boolean getBlIsFromWhatsApp() {
		return blIsFromWhatsApp;
	}

	public void setBlIsFromWhatsApp(Boolean blIsFromWhatsApp) {
		this.blIsFromWhatsApp = blIsFromWhatsApp;
	}

	public Boolean getBlIsFromHotline() {
		return blIsFromHotline;
	}

	public void setBlIsFromHotline(Boolean blIsFromHotline) {
		this.blIsFromHotline = blIsFromHotline;
	}

	public Boolean getBlIsInquiry() {
		return blIsInquiry;
	}

	public void setBlIsInquiry(Boolean blIsInquiry) {
		this.blIsInquiry = blIsInquiry;
	}

	public String getTxtDepartment() {
		return txtDepartment;
	}

	public void setTxtDepartment(String txtDepartment) {
		this.txtDepartment = txtDepartment;
	}
	
	

}