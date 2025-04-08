package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IAccountDAO;
import com.bezkoder.spring.login.admin.dal.entities.Account;

@Repository
public class AccountDAO implements IAccountDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(AccountDAO.class);

	public AccountDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Account> getAllAccount() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<Account> Accounts = entityManager.createQuery("FROM Account  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Accounts;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Account> getActiveAccount() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<Account> Accounts = entityManager
				.createQuery("FROM Account ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Accounts;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Account> getAccountByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Account where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serAccountId <> " + oldValue;
			}
			List<Account> Accounts = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Accounts;
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
	public String addNewAccount(Account Account) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
//			Account.setCreatedBy(commonService.getCurrentLoggedInUser());
			Account.setCreatedAt(commonService.getCurrentTimeStamp_new());
//			Account.setIsActive(true);
			entityManager.persist(Account);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteAccount(List<String> AccountsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serAccountId : AccountsId) {
				Account Account = entityManager.find(Account.class, Integer.parseInt(serAccountId));
//				if (Account != null) {
//					Account.setBlIsDeleted(true);
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
	public String updateAccount(Account Account) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(Account);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateAccountNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getAccountById(String AccountId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Account where txtAccountCode='" + AccountId + "'";

			List<Account> Account = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Account.size() > 0) {
				return String.valueOf(Account.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<Account> searchAccount(Account Account) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from Account Account where 1=1 ";
	  
	  
	  
	    
	    /*if(Account.getTxtEmail() !=null){
	    	query+=" and upper(Account.txtEmail) like"+" upper('"+Account.getTxtEmail()+"')"+"  ";
	    }*/
	    
	   
	  
	    query+=" order by Account.serAccountId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<Account> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
