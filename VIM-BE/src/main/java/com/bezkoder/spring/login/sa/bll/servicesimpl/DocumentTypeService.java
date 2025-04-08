package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDocumentTypeService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDocumentTypeDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDocumentType;


@Service
public class DocumentTypeService implements IDocumentTypeService {
	
	@Autowired
	private ICfgTblDocumentTypeDAO citTableDocumentTypeDAO;

	private Logger logger = LogManager.getLogger(DocumentTypeService.class);

	public DocumentTypeService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<CfgTblDocumentType> getAllDocumentType() {
		logger.debug("getAllDocumentTypes()");
		List<CfgTblDocumentType> documentTypes = citTableDocumentTypeDAO.getAllDocumentType();
		return documentTypes;
	}
	
	@Override
	public List<CfgTblDocumentType> getActiveDocumentType() {
		logger.debug("getActiveDocumentTypes()");
		List<CfgTblDocumentType> documentTypes = citTableDocumentTypeDAO.getActiveDocumentType();
		return documentTypes;
	}
	
	@Override
	public String generateDocumentTypeNo(String type) {
		
		return citTableDocumentTypeDAO.generateDocumentTypeNo(type);
		
	}
	
	@Override
	public boolean documentTypeExistByProperty(String property, String value,String mode, String oldValue) {
		return !citTableDocumentTypeDAO.getDocumentTypeByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDocumentType(CfgTblDocumentType cfgTblDocumentType) {
				
		return citTableDocumentTypeDAO.addNewDocumentType(cfgTblDocumentType);
	}

	@Override
	public String updateDocumentType(CfgTblDocumentType cfgTblDocumentType) {
		
		return citTableDocumentTypeDAO.updateDocumentType(cfgTblDocumentType);
	}

	@Override
	public String deleteDocumentType(List<String> documentTypesId) {
		// TODO Auto-generated method stub
		return citTableDocumentTypeDAO.deleteDocumentType(documentTypesId);
	}
	
	@Override
	public List<CfgTblDocumentType> searchDocumentType(CfgTblDocumentType documentType) {
		// TODO Auto-generated method stub
		return citTableDocumentTypeDAO.searchDocumentType(documentType);
	}

}
