package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="sls_tbl_tir_document")
@NamedQuery(name="SlsTblTIRDocument.findAll", query="SELECT r FROM SlsTblTIRDocument r")
public class SlsTblTIRDocument implements Serializable {
	
	
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
    @Column(name="tir_document", nullable=false, columnDefinition="longblob")
	private byte[] documentFile;

	
	@JsonIgnoreProperties(value={"candidateDocuments","rcsCandidateVerificationData"})
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="ser_tir_id")
	private SlsTblTIR slsTblTIR;


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


	public SlsTblTIR getSlsTblTIR() {
		return slsTblTIR;
	}


	public void setSlsTblTIR(SlsTblTIR slsTblTIR) {
		this.slsTblTIR = slsTblTIR;
	}


	public String getDocumentType() {
		return documentType;
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
	
	


	
	
}