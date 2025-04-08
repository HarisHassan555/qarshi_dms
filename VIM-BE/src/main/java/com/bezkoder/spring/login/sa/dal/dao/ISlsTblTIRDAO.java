package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIR;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIRDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIRDocument;

public interface ISlsTblTIRDAO {

	List<SlsTblTIR> getAllTIR();

	List<SlsTblTIR> getActiveTIR();

	List<SlsTblTIR> getTIRByProperty(String property, String value, String mode, String oldValue);

	String addNewTIR(SlsTblTIR slsTblTIR);

	String deleteTIR(List<String> customerId);

	String updateTIR(SlsTblTIR slsTblTIR);

	String generateTIRNo(String type);

	String getTIRById(String customerId);
	
	List<SlsTblTIR> searchTIR(SlsTblTIR TIR);
	
	List<SlsTblTIRDetail> searchTIRDetail(int TIRId);
	
	public String AssignGatePassNumber(SlsTblTIR slsTblTIR);
	
	String addNewTIR(DCDTO slsTblTIR);
	
	String updateTIR(DCDTO slsTblTIR);
	
	SlsTblTIR getPrevious(String vehicle);
	
	String uploadTIRDocument(SlsTblTIRDocument tirDocument);
	
	 byte[]  downloadDocument(int documentId) ;
	
	 String removeCandidateDocument(int documentId);
	
	 List<SlsTblTIRDocument> getSOPaymentDocumentList(int id);
	 
	 
	 byte[] getTIRPicture(String tir,String id);
	 
	 public SlsTblTIR getTIRByPK(Integer TIRId);
	 
	 String deleteTIRDetail(List<String> TIRsId);
}
