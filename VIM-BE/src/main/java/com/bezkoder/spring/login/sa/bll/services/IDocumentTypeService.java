package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblDocumentType;


public interface IDocumentTypeService {

	List<CfgTblDocumentType> getAllDocumentType();
	
	List<CfgTblDocumentType> getActiveDocumentType();
	
	String addNewDocumentType(CfgTblDocumentType cfgTblDocumentType);

	boolean documentTypeExistByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDocumentType(List<String> documentTypesId);

	String updateDocumentType(CfgTblDocumentType cfgTblDocumentType);
	
	String generateDocumentTypeNo(String type);
	
	List<CfgTblDocumentType> searchDocumentType(CfgTblDocumentType documentType);
	

}
