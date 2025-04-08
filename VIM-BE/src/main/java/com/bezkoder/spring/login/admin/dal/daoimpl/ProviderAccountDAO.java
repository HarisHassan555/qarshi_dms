package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderAccountDAO;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAccount;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;

@Repository
public class ProviderAccountDAO implements IProviderAccountDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(ProviderAccountDAO.class);

	public ProviderAccountDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAccount> getAllProviderAccount() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<ProviderAccount> ProviderAccounts = entityManager.createQuery("FROM ProviderAccount  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProviderAccounts;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAccount> getActiveProviderAccount() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<ProviderAccount> ProviderAccounts = entityManager
				.createQuery("FROM ProviderAccount ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProviderAccounts;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAccount> getProviderAccountByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ProviderAccount where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serProviderAccountId <> " + oldValue;
			}
			List<ProviderAccount> ProviderAccounts = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProviderAccounts;
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
	public String addNewProviderAccount(ProviderAccount ProviderAccount) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			ProviderAccount.setCreatedBy(commonService.getCurrentLoggedInUser());
			ProviderAccount.setCreatedAt(commonService.getCurrentTimeStamp_new());
		//	ProviderAccount.setIsActive(true);
//			ProviderAuthentication providerAuthentication=new ProviderAuthentication();
//			providerAuthentication=ProviderAccount.getProviderAuthentication();
			entityManager.persist(ProviderAccount);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	
	
	
//	public String addNewProviderAuthenitication(ProviderAuthentication providerAccount) {
	@Override
	public String addNewProviderAuthenitication(ProviderAuthentication ProviderAccount) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			ProviderAccount.getProviderAccount().setCreatedBy(commonService.getCurrentLoggedInUser());
			ProviderAccount.getProviderAccount().setCreatedAt(commonService.getCurrentTimeStamp_new());
		//	ProviderAccount.setIsActive(true);
//			ProviderAuthentication providerAuthentication=new ProviderAuthentication();
//			providerAuthentication=ProviderAccount.getProviderAuthentication();
			entityManager.persist(ProviderAccount);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	

	@Override
	public String deleteProviderAccount(List<String> ProviderAccountsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProviderAccountId : ProviderAccountsId) {
				ProviderAccount ProviderAccount = entityManager.find(ProviderAccount.class, Integer.parseInt(serProviderAccountId));
//				if (ProviderAccount != null) {
//					ProviderAccount.setBlIsDeleted(true);
//
//				}
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
	public String updateProviderAccount(ProviderAccount ProviderAccount) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(ProviderAccount);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProviderAccountNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getProviderAccountById(String ProviderAccountId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ProviderAccount where txtProviderAccountCode='" + ProviderAccountId + "'";

			List<ProviderAccount> ProviderAccount = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProviderAccount.size() > 0) {
				return String.valueOf(ProviderAccount.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<ProviderAccount> searchProviderAccount(ProviderAccount ProviderAccount) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from ProviderAccount ProviderAccount where 1=1 ";
	  
	 
	    
	  
	    
	    /*if(ProviderAccount.getTxtEmail() !=null){
	    	query+=" and upper(ProviderAccount.txtEmail) like"+" upper('"+ProviderAccount.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(ProviderAccount.getProviderAccountId() !=null){
//	    	query+=" and ProviderAccount.providerAccountId ="+" "+ProviderAccount.getProviderAccountId()+""+"  ";
//	    }
	  
	    query+=" order by ProviderAccount.serProviderAccountId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<ProviderAccount> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
