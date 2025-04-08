package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Time;
import java.util.Date;
import java.sql.Timestamp;


/**
 * The persistent class for the hr_tbl_shift_info database table.
 * 
 */
@Entity
@Table(name="hr_tbl_shift_info")
@NamedQuery(name="HrTblShiftInfo.findAll", query="SELECT h FROM HrTblShiftInfo h")
public class HrTblShiftInfo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ser_shift_info_id")
	private Integer serShiftInfoId;

	@Column(name="bln_is_night_shift")
	private Boolean blnIsNightShift;

	@Column(name="bln_isactive")
	private Boolean blnIsactive;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="dte_shift_endtime")
	private Time dteShiftEndtime;

	@Column(name="dte_shift_starttime")
	private Time dteShiftStarttime;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_shiftdate")
	private Date dteShiftdate;

	@Column(name="num_break_hour")
	private Time numBreakHour;

	@Column(name="num_total_hour")
	private Time numTotalHour;

	@Column(name="num_working_hour")
	private Time numWorkingHour;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="tim_shift_break_end")
	private Time timShiftBreakEnd;

	@Column(name="tim_shift_break_start")
	private Time timShiftBreakStart;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_shift_description")
	private String txtShiftDescription;

	public HrTblShiftInfo() {
	}

	public Integer getSerShiftInfoId() {
		return this.serShiftInfoId;
	}

	public void setSerShiftInfoId(Integer serShiftInfoId) {
		this.serShiftInfoId = serShiftInfoId;
	}

	public Boolean getBlnIsNightShift() {
		return this.blnIsNightShift;
	}

	public void setBlnIsNightShift(Boolean blnIsNightShift) {
		this.blnIsNightShift = blnIsNightShift;
	}

	public Boolean getBlnIsactive() {
		return this.blnIsactive;
	}

	public void setBlnIsactive(Boolean blnIsactive) {
		this.blnIsactive = blnIsactive;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public Timestamp getDteCreateddate() {
		return this.dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public Time getDteShiftEndtime() {
		return this.dteShiftEndtime;
	}

	public void setDteShiftEndtime(Time dteShiftEndtime) {
		this.dteShiftEndtime = dteShiftEndtime;
	}

	public Time getDteShiftStarttime() {
		return this.dteShiftStarttime;
	}

	public void setDteShiftStarttime(Time dteShiftStarttime) {
		this.dteShiftStarttime = dteShiftStarttime;
	}

	public Date getDteShiftdate() {
		return this.dteShiftdate;
	}

	public void setDteShiftdate(Date dteShiftdate) {
		this.dteShiftdate = dteShiftdate;
	}

	public Time getNumBreakHour() {
		return this.numBreakHour;
	}

	public void setNumBreakHour(Time numBreakHour) {
		this.numBreakHour = numBreakHour;
	}

	public Time getNumTotalHour() {
		return this.numTotalHour;
	}

	public void setNumTotalHour(Time numTotalHour) {
		this.numTotalHour = numTotalHour;
	}

	public Time getNumWorkingHour() {
		return this.numWorkingHour;
	}

	public void setNumWorkingHour(Time numWorkingHour) {
		this.numWorkingHour = numWorkingHour;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public Time getTimShiftBreakEnd() {
		return this.timShiftBreakEnd;
	}

	public void setTimShiftBreakEnd(Time timShiftBreakEnd) {
		this.timShiftBreakEnd = timShiftBreakEnd;
	}

	public Time getTimShiftBreakStart() {
		return this.timShiftBreakStart;
	}

	public void setTimShiftBreakStart(Time timShiftBreakStart) {
		this.timShiftBreakStart = timShiftBreakStart;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtShiftDescription() {
		return this.txtShiftDescription;
	}

	public void setTxtShiftDescription(String txtShiftDescription) {
		this.txtShiftDescription = txtShiftDescription;
	}
	
	
	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	
	@Column(name="txt_shift_start")
	private String txtShiftStart;
	
	public String getTxtShiftStart() {
		return txtShiftStart;
	}

	public void setTxtShiftStart(String txtShiftStart) {
		this.txtShiftStart = txtShiftStart;
	}

	public String getTxtShiftEnd() {
		return txtShiftEnd;
	}

	public void setTxtShiftEnd(String txtShiftEnd) {
		this.txtShiftEnd = txtShiftEnd;
	}


	@Column(name="txt_shift_end")
	private String txtShiftEnd;

}