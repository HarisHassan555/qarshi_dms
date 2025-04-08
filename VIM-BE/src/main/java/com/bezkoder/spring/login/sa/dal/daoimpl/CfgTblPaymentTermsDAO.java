package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblPaymentTermsDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPaymentTerm;

@Repository
public class CfgTblPaymentTermsDAO implements ICfgTblPaymentTermsDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblPaymentTermsDAO.class);

	public CfgTblPaymentTermsDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPaymentTerm> getAllPaymentTerms() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblPaymentTerm> PaymentTerms = entityManager.createQuery("FROM CfgTblPaymentTerm where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PaymentTerms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPaymentTerm> getActivePaymentTerms() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblPaymentTerm> PaymentTerms = entityManager
				.createQuery("FROM CfgTblPaymentTerm where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PaymentTerms;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPaymentTerm> getPaymentTermsByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPaymentTerm where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblPaymentTerm> PaymentTerms = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return PaymentTerms;
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
	public String addNewPaymentTerms(CfgTblPaymentTerm CfgTblPaymentTerm) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblPaymentTerm.setBlnStatus(true);
			CfgTblPaymentTerm.setBlIsDeleted(false);
			entityManager.persist(CfgTblPaymentTerm);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePaymentTerms(List<String> PaymentTermsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPaymentTermId : PaymentTermsId) {
				CfgTblPaymentTerm PaymentTerm = entityManager.find(CfgTblPaymentTerm.class, Integer.parseInt(serPaymentTermId));
				if (PaymentTerm != null) {
					PaymentTerm.setBlIsDeleted(true);
					PaymentTerm.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					PaymentTerm.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updatePaymentTerms(CfgTblPaymentTerm CfgTblPaymentTerm) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblPaymentTerm.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblPaymentTerm.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblPaymentTerm);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePaymentTermsNo(String type) {
		// int PaymentTermNo;
		String PaymentTermType = type;
		// String PaymentTermCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (PaymentTermType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPaymentTermCode) from CfgTblPaymentTerm ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPaymentTermCode) from CfgTblPaymentTerm ")
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

	public String getPaymentTermsById(String PaymentTermId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPaymentTerm where txtPaymentTermCode='" + PaymentTermId + "'";

			List<CfgTblPaymentTerm> PaymentTerm = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (PaymentTerm.size() > 0) {
				return String.valueOf(PaymentTerm.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblPaymentTerm> searchPaymentTerms(CfgTblPaymentTerm PaymentTerm) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblPaymentTerm PaymentTerm where 1=1 ";
	  
	    if(PaymentTerm.getTxtName() !=null){
	    	query+=" and upper(PaymentTerm.txtName) like"+" upper('"+PaymentTerm.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(PaymentTerm.getTxtEmail() !=null){
	    	query+=" and upper(PaymentTerm.txtEmail) like"+" upper('"+PaymentTerm.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(PaymentTerm.getSerPaymentTermsId() !=null){
	    	query+=" and PaymentTerm.serPaymentTermId ="+" "+PaymentTerm.getSerPaymentTermsId()+""+"  ";
	    }
	  
	    query+=" order by PaymentTerm.serPaymentTermId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblPaymentTerm> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}

	
}
