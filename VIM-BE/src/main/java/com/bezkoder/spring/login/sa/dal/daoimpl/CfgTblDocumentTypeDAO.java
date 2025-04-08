package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDocumentTypeDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDocumentType;

@Repository
public class CfgTblDocumentTypeDAO implements ICfgTblDocumentTypeDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblDocumentTypeDAO.class);

	public CfgTblDocumentTypeDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDocumentType> getAllDocumentType() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDocumentType> DocumentTypes = entityManager.createQuery("FROM CfgTblDocumentType where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return DocumentTypes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDocumentType> getActiveDocumentType() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDocumentType> DocumentTypes = entityManager
				.createQuery("FROM CfgTblDocumentType where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return DocumentTypes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDocumentType> getDocumentTypeByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDocumentType where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblDocumentType> DocumentTypes = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return DocumentTypes;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	/*
	 * String date = simpleDateFormat.format(new Date());
	 * System.out.println(date);
	 */
	@Override
	public String addNewDocumentType(CfgTblDocumentType CfgTblDocumentType) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDocumentType.setBlnStatus(true);
			CfgTblDocumentType.setBlIsDeleted(false);
			entityManager.persist(CfgTblDocumentType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDocumentType(List<String> DocumentTypesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDocumentTypeId : DocumentTypesId) {
				CfgTblDocumentType DocumentType = entityManager.find(CfgTblDocumentType.class, Integer.parseInt(serDocumentTypeId));
				if (DocumentType != null) {
					DocumentType.setBlIsDeleted(true);
					DocumentType.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					DocumentType.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
				}
			}
			entityManager.getTransaction().commit();
			entityManager.close();

		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}
		return "Success";
	}

	@Override
	public String updateDocumentType(CfgTblDocumentType CfgTblDocumentType) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDocumentType.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblDocumentType.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblDocumentType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDocumentTypeNo(String type) {
		// int DocumentTypeNo;
		String DocumentTypeType = type;
		// String DocumentTypeCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DocumentTypeType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDocumentTypeCode) from CfgTblDocumentType ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "BK-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "BK-1";
				if (ord_no < 10)
					code = "BK-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "BK-0" + ord_no;
				else
					code = "BK-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDocumentTypeCode) from CfgTblDocumentType ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CTR-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "CTR-1";
				if (ord_no1 < 10)
					code = "CTR-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "CTR-0" + ord_no1;
				else
					code = "CTR-" + ord_no1;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getDocumentTypeById(String DocumentTypeId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDocumentType where txtDocumentTypeCode='" + DocumentTypeId + "'";

			List<CfgTblDocumentType> DocumentType = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (DocumentType.size() > 0) {
				return String.valueOf(DocumentType.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblDocumentType> searchDocumentType(CfgTblDocumentType DocumentType) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblDocumentType DocumentType where 1=1 ";
	  
	    if(DocumentType.getTxtName() !=null){
	    	query+=" and upper(DocumentType.txtName) like"+" upper('"+DocumentType.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(DocumentType.getTxtEmail() !=null){
	    	query+=" and upper(DocumentType.txtEmail) like"+" upper('"+DocumentType.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(DocumentType.getSerDocumentTypeId() !=null){
	    	query+=" and DocumentType.serDocumentTypeId ="+" "+DocumentType.getSerDocumentTypeId()+""+"  ";
	    }
	  
	    query+=" order by DocumentType.serDocumentTypeId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblDocumentType> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
