package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


/**
 * The persistent class for the sls_tbl_sale_item_schedule database table.
 * 
 */
@Entity
@Table(name="sls_tbl_sale_item_schedule")
@NamedQuery(name="SlsTblSaleItemSchedule.findAll", query="SELECT s FROM SlsTblSaleItemSchedule s")
public class SlsTblSaleItemSchedule implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="SLS_TBL_SALE_ITEM_SCHEDULE_SERSALEITEMSCHEDULEID_GENERATOR", sequenceName="SLS_TBL_SALE_ITEM_SCHEDULE_SER_SALE_ITEM_SCHEDULE_ID_SEQ", initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SLS_TBL_SALE_ITEM_SCHEDULE_SERSALEITEMSCHEDULEID_GENERATOR")
	@Column(name="ser_sale_item_schedule_id")
	private Integer serSaleItemScheduleId;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;

	@Column(name="num_quantity")
	private BigDecimal numQuantity;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	public SlsTblSaleItemSchedule() {
	}

	public Integer getSerSaleItemScheduleId() {
		return this.serSaleItemScheduleId;
	}

	public void setSerSaleItemScheduleId(Integer serSaleItemScheduleId) {
		this.serSaleItemScheduleId = serSaleItemScheduleId;
	}

	public Date getDteDate() {
		return this.dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public BigDecimal getNumQuantity() {
		return this.numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	//bi-directional many-to-one association to SlsTblSoDetail
	@ManyToOne
	@JoinColumn(name="ser_so_detail_id", nullable = false)
	@JsonBackReference
	private SlsTblSoDetail slsTblSoDetail;

	public SlsTblDealDetails getSlsTblDealDetails() {
		return slsTblDealDetails;
	}

	public void setSlsTblDealDetails(SlsTblDealDetails slsTblDealDetails) {
		this.slsTblDealDetails = slsTblDealDetails;
	}

	@ManyToOne
	@JoinColumn(name = "deal_detail_id", nullable = false)
	@JsonBackReference("dealDetails")
	private SlsTblDealDetails slsTblDealDetails;


	public SlsTblSoDetail getSlsTblSoDetail() {
		return slsTblSoDetail;
	}

	public void setSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		this.slsTblSoDetail = slsTblSoDetail;
	}

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	
}