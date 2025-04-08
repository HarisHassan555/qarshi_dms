package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="sls_so_payment_document")
@NamedQuery(name="SOPaymentDocument.findAll", query="SELECT r FROM SOPaymentDocument r")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SOPaymentDocument implements Serializable {
	
	
	@Id
	@GeneratedValue
	@Column(name="ser_document_id")
	private int documentId;
	
	@Column(name="txt_document_name")
	private String documentName;
	
	
	@Column(name="txt_document_type")
	private String documentType;
	
	@Column(name="txt_original_name")
	private String originalName;	
	
	@Column(name="txt_size")
	private String size;
	

	
	@Lob
    @Column(name="payment_document", nullable=false, columnDefinition="longblob")
	private byte[] documentFile;

	
	/*
	 * @JsonIgnoreProperties(value={"candidateDocuments",
	 * "rcsCandidateVerificationData"})
	 * 
	 * @ManyToOne(fetch=FetchType.EAGER)
	 * 
	 * @JoinColumn(name="ser_so_payment_id") private SlsTblSoPayments
	 * slsTblSoPayments;
	 */

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;


	@Column(name="ser_created_user_name")
	private String createdUserName;

	public String getCreatedUserName() {
		return createdUserName;
	}

	public void setCreatedUserName(String createdUserName) {
		this.createdUserName = createdUserName;
	}

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;
	
	@JsonIgnoreProperties(value={"candidateDocuments","rcsCandidateVerificationData"})
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="ser_sale_order_id")
	private SlsTblSaleOrder slsTblSaleOrder;


	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

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

	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public byte[] getDocumentFile() {
		return documentFile;
	}

	public void setDocumentFile(byte[] documentFile) {
		this.documentFile = documentFile;
	}

	/*
	 * public SlsTblSoPayments getSlsTblSoPayments() { return slsTblSoPayments; }
	 * 
	 * public void setSlsTblSoPayments(SlsTblSoPayments slsTblSoPayments) {
	 * this.slsTblSoPayments = slsTblSoPayments; }
	 */
	
	

	public String getDocumentType() {
		return documentType;
	}

	public SlsTblSaleOrder getSlsTblSaleOrder() {
		return slsTblSaleOrder;
	}

	public void setSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		this.slsTblSaleOrder = slsTblSaleOrder;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
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
	
}