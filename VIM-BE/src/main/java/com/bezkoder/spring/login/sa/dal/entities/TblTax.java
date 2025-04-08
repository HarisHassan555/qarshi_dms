package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name="tbl_tax")
@NamedQuery(name="TblTax.findAll", query="SELECT c FROM TblTax c")
public class TblTax implements Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tbl_tax_ser_tax_id_seq")
	@SequenceGenerator(name = "tbl_tax_ser_tax_id_seq", sequenceName = "tbl_tax_ser_tax_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_tax_id")
	private Integer serTaxId;

	 
	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="txt_tax_organization")
	private String txtTaxOrganization;

	@Column(name="txt_organization_status")
	private String txtOrganizationStatus;
	
	@Column(name="num_tax_percentage")
	private BigDecimal numTaxPercentage;
	
	

	public Integer getSerTaxId() {
		return serTaxId;
	}

	public void setSerTaxId(Integer serTaxId) {
		this.serTaxId = serTaxId;
	}

	public String getTxtTaxOrganization() {
		return txtTaxOrganization;
	}

	public void setTxtTaxOrganization(String txtTaxOrganization) {
		this.txtTaxOrganization = txtTaxOrganization;
	}

	public String getTxtOrganizationStatus() {
		return txtOrganizationStatus;
	}

	public void setTxtOrganizationStatus(String txtOrganizationStatus) {
		this.txtOrganizationStatus = txtOrganizationStatus;
	}

	public BigDecimal getNumTaxPercentage() {
		return numTaxPercentage;
	}

	public void setNumTaxPercentage(BigDecimal numTaxPercentage) {
		this.numTaxPercentage = numTaxPercentage;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}


}