package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDocumentType;

public interface ICfgTblDocumentTypeDAO {

	List<CfgTblDocumentType> getAllDocumentType();

	List<CfgTblDocumentType> getActiveDocumentType();

	List<CfgTblDocumentType> getDocumentTypeByProperty(String property, String value, String mode, String oldValue);

	String addNewDocumentType(CfgTblDocumentType cfgTblDocumentType);

	String deleteDocumentType(List<String> customerId);

	String updateDocumentType(CfgTblDocumentType cfgTblDocumentType);

	String generateDocumentTypeNo(String type);

	String getDocumentTypeById(String customerId);
	
	List<CfgTblDocumentType> searchDocumentType(CfgTblDocumentType documentType);
}
