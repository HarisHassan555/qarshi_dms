package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblPasswordPolicyDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;

@Repository
public class CfgTblPasswordPolicyDAO implements ICfgTblPasswordPolicyDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblPasswordPolicyDAO.class);

	public CfgTblPasswordPolicyDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPasswordPolicy> getAllPasswordPolicy() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<CfgTblPasswordPolicy> PasswordPolicys = entityManager.createQuery("FROM CfgTblPasswordPolicy where blIsDeleted=false ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PasswordPolicys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPasswordPolicy> getActivePasswordPolicy() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblPasswordPolicy> PasswordPolicys = entityManager
				.createQuery("FROM CfgTblPasswordPolicy where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PasswordPolicys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPasswordPolicy> getPasswordPolicyByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPasswordPolicy where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serPasswordPolicyId <> " + oldValue;
			}
			List<CfgTblPasswordPolicy> PasswordPolicys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return PasswordPolicys;
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
	public String addNewPasswordPolicy(CfgTblPasswordPolicy CfgTblPasswordPolicy) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblPasswordPolicy.setBlnStatus(true);
			CfgTblPasswordPolicy.setBlIsDeleted(false);
			CfgTblPasswordPolicy.setBlIsActive(true);
			entityManager.persist(CfgTblPasswordPolicy);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePasswordPolicy(List<String> PasswordPolicysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPasswordPolicyId : PasswordPolicysId) {
				CfgTblPasswordPolicy PasswordPolicy = entityManager.find(CfgTblPasswordPolicy.class, Integer.parseInt(serPasswordPolicyId));
				if (PasswordPolicy != null) {
					PasswordPolicy.setBlIsDeleted(true);

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
	public String updatePasswordPolicy(CfgTblPasswordPolicy CfgTblPasswordPolicy) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblPasswordPolicy);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePasswordPolicyNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getPasswordPolicyById(String PasswordPolicyId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPasswordPolicy where txtPasswordPolicyCode='" + PasswordPolicyId + "'";

			List<CfgTblPasswordPolicy> PasswordPolicy = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (PasswordPolicy.size() > 0) {
				return String.valueOf(PasswordPolicy.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblPasswordPolicy> searchPasswordPolicy(CfgTblPasswordPolicy PasswordPolicy) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblPasswordPolicy PasswordPolicy where 1=1 ";
	 
	    
	  
	    
	    /*if(PasswordPolicy.getTxtEmail() !=null){
	    	query+=" and upper(PasswordPolicy.txtEmail) like"+" upper('"+PasswordPolicy.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(PasswordPolicy.getSerPasswordPolicyId() !=null){
	    	query+=" and PasswordPolicy.serPasswordPolicyId ="+" "+PasswordPolicy.getSerPasswordPolicyId()+""+"  ";
	    }
	  
	    query+=" order by PasswordPolicy.serPasswordPolicyId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblPasswordPolicy> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
