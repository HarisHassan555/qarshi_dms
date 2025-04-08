package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IAccountAuthenticationDAO;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.AccountAuthentication;
import org.springframework.security.crypto.password.PasswordEncoder;
@Repository
public class AccountAuthenticationDAO implements IAccountAuthenticationDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private DataSource dataSource;
	
	private JdbcTemplate jdbcTemplateObject;

	private static final Logger log = LoggerFactory.getLogger(AccountAuthenticationDAO.class);

	public AccountAuthenticationDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AccountAuthentication> getAllAccountAuthentication() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<AccountAuthentication> AccountAuthentications = entityManager.createQuery("FROM AccountAuthentication  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return AccountAuthentications;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AccountAuthentication> getActiveAccountAuthentication() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<AccountAuthentication> AccountAuthentications = entityManager
				.createQuery("FROM AccountAuthentication ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return AccountAuthentications;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AccountAuthentication> getAccountAuthenticationByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM AccountAuthentication where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serAccountAuthenticationId <> " + oldValue;
			}
			List<AccountAuthentication> AccountAuthentications = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return AccountAuthentications;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);


	
	
	
//	public String addNewAccountAuthenitication(AccountAuthentication accountAuthentication) {
	@Override
	public String addNewAccountAuthenitication(AccountAuthentication accountAuthentication) {
		EntityManager entityManager = getEntityManager();
		try {
			
			AccountAuthentication account_authentication=new AccountAuthentication();
			account_authentication.setUsername(accountAuthentication.getUsername());
		
			List lstAccount=searchAccountAuthenticationforDuplication(account_authentication,false);
			if(lstAccount!=null && lstAccount.size() >0)
			{
				return "EXIST";
			}
			entityManager.getTransaction().begin();
//			accountAuthentication.getAccount().setCreatedBy(commonService.getCurrentLoggedInUser());
			accountAuthentication.getAccount().setCreatedAt(commonService.getCurrentTimeStamp_new());
			CharSequence seq_pass=accountAuthentication.getPassword();
//			accountAuthentication.setPassword(passwordEncoder.encode(seq_pass));
			accountAuthentication.setPassword(passwordEncoder.encode(accountAuthentication.getPassword()));
			
			this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
			int result = this.jdbcTemplateObject.queryForObject("SELECT roleid from  role_entity where name ilike 'PATIENT'", Integer.class);
			if(result > 0)
			accountAuthentication.setRoleid((long)result);
			accountAuthentication.setIsForcedChangePassword(true);
			
		//	AccountAuthentication.setIsActive(true);
//			AccountAuthentication accountAuthentication=new AccountAuthentication();
//			accountAuthentication=AccountAuthentication.getAccountAuthentication();
			entityManager.persist(accountAuthentication);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	

	@Override
	public String deleteAccountAuthentication(List<String> AccountAuthenticationsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serAccountAuthenticationId : AccountAuthenticationsId) {
				AccountAuthentication AccountAuthentication = entityManager.find(AccountAuthentication.class, Integer.parseInt(serAccountAuthenticationId));
//				if (AccountAuthentication != null) {
//					AccountAuthentication.setBlIsDeleted(true);
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
	public String updateAccountAuthentication(AccountAuthentication AccountAuthentication) {
		EntityManager entityManager = getEntityManager();
		try {
			
			AccountAuthentication account_authentication=new AccountAuthentication();
			account_authentication.setUsername(AccountAuthentication.getUsername());
			account_authentication.setAccountAuthid(AccountAuthentication.getAccountAuthid());
			List lstAccount=searchAccountAuthenticationforDuplication(account_authentication,true);
			
			if(lstAccount!=null && lstAccount.size() >0)
			{
				return "EXIST";
			}
			
			entityManager.getTransaction().begin();
			entityManager.merge(AccountAuthentication);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateAccountAuthenticationNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getAccountAuthenticationById(String AccountAuthenticationId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM AccountAuthentication where txtAccountAuthenticationCode='" + AccountAuthenticationId + "'";

			List<AccountAuthentication> AccountAuthentication = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (AccountAuthentication.size() > 0) {
				return String.valueOf(AccountAuthentication.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<AccountAuthentication> searchAccountAuthentication(AccountAuthentication AccountAuthentication) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from AccountAuthentication AccountAuthentication where 1=1 ";
	  
	 
	    if(AccountAuthentication.getUsername() !=null){
	    	if(AccountAuthentication.getUsername().equalsIgnoreCase("Account"))
	    	query+=" and AccountAuthentication.account is not null  ";
	    	else
	    		query+=" and AccountAuthentication.accountAccount is not null  ";
	    }
	  
	    
	    /*if(AccountAuthentication.getTxtEmail() !=null){
	    	query+=" and upper(AccountAuthentication.txtEmail) like"+" upper('"+AccountAuthentication.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(AccountAuthentication.getAccountAuthenticationId() !=null){
//	    	query+=" and AccountAuthentication.accountAuthenticationId ="+" "+AccountAuthentication.getAccountAuthenticationId()+""+"  ";
//	    }
	  
//	    query+=" order by AccountAuthentication.serAccountAuthenticationId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<AccountAuthentication> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	

	public List<AccountAuthentication> searchAccountAuthenticationforDuplication(AccountAuthentication AccountAuthentication, boolean isupdate) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from AccountAuthentication accountAuthentication where 1=1 ";
	  
	 
	    if(AccountAuthentication.getUsername() !=null){
	    	
	    	query+=" and accountAuthentication.username = '"+AccountAuthentication.getUsername()+"'";
	    	
	    }
	    
	    if(isupdate)
	    {
	    	 if(AccountAuthentication.getAccountAuthid() !=null){
	    	    	query+=" and accountAuthentication.accountAuthid !="+" "+AccountAuthentication.getAccountAuthid()+""+"  ";
	    	    }
	    	    
	    }
	  
	 
	  
	   
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<AccountAuthentication> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
