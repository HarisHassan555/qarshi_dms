package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblIncoTermsDAO;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblIncoTerm;

@Repository
public class CfgTblIncoTermsDAO implements ICfgTblIncoTermsDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblIncoTermsDAO.class);

	public CfgTblIncoTermsDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblIncoTerm> getAllIncoTerm() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblIncoTerm> IncoTerms = entityManager.createQuery("FROM CfgTblIncoTerm where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return IncoTerms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblIncoTerm> getActiveIncoTerm() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblIncoTerm> IncoTerms = entityManager
				.createQuery("FROM CfgTblIncoTerm where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return IncoTerms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblIncoTerm> getIncoTermByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblIncoTerm where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblIncoTerm> IncoTerms = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return IncoTerms;
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
	public String addNewIncoTerm(CfgTblIncoTerm CfgTblIncoTerm) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblIncoTerm.setBlnStatus(true);
			CfgTblIncoTerm.setBlIsDeleted(false);
			entityManager.persist(CfgTblIncoTerm);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteIncoTerm(List<String> IncoTermsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serIncoTermId : IncoTermsId) {
				CfgTblIncoTerm IncoTerm = entityManager.find(CfgTblIncoTerm.class, Integer.parseInt(serIncoTermId));
				if (IncoTerm != null) {
					IncoTerm.setBlIsDeleted(true);
					IncoTerm.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					IncoTerm.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateIncoTerm(CfgTblIncoTerm CfgTblIncoTerm) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblIncoTerm.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblIncoTerm.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblIncoTerm);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateIncoTermNo(String type) {
		// int IncoTermNo;
		String IncoTermType = type;
		// String IncoTermCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (IncoTermType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtIncoTermCode) from CfgTblIncoTerm ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtIncoTermCode) from CfgTblIncoTerm ")
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

	public String getIncoTermById(String IncoTermId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblIncoTerm where txtIncoTermCode='" + IncoTermId + "'";

			List<CfgTblIncoTerm> IncoTerm = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (IncoTerm.size() > 0) {
				return String.valueOf(IncoTerm.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblIncoTerm> searchIncoTerm(CfgTblIncoTerm IncoTerm) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblIncoTerm IncoTerm where 1=1 ";
	  
	    if(IncoTerm.getTxtName() !=null){
	    	query+=" and upper(IncoTerm.txtName) like"+" upper('"+IncoTerm.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(IncoTerm.getTxtEmail() !=null){
	    	query+=" and upper(IncoTerm.txtEmail) like"+" upper('"+IncoTerm.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(IncoTerm.getSerIncoTermsId() !=null){
	    	query+=" and IncoTerm.serIncoTermId ="+" "+IncoTerm.getSerIncoTermsId()+""+"  ";
	    }
	  
	    query+=" order by IncoTerm.serIncoTermId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblIncoTerm> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public CfgTblIncoTerm getIncoTermByPK(int IncoTermId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblIncoTerm where serIncoTermsId=" + IncoTermId ;

			CfgTblIncoTerm IncoTerm = (CfgTblIncoTerm)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			
			return IncoTerm;

		}  
		catch (NoResultException e) {
			log.error("IncoTerm not found");
			return null;
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
