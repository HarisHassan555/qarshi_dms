package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderAuthenticationDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import com.bezkoder.spring.login.admin.dal.entities.ProviderAuthentication;
import org.springframework.security.crypto.password.PasswordEncoder;
@Repository
public class ProviderAuthenticationDAO implements IProviderAuthenticationDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private DataSource dataSource;
	
	private JdbcTemplate jdbcTemplateObject;

	private static final Logger log = LoggerFactory.getLogger(ProviderAuthenticationDAO.class);

	public ProviderAuthenticationDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAuthentication> getAllProviderAuthentication() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<ProviderAuthentication> ProviderAuthentications = entityManager.createQuery("FROM ProviderAuthentication  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProviderAuthentications;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAuthentication> getActiveProviderAuthentication() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<ProviderAuthentication> ProviderAuthentications = entityManager
				.createQuery("FROM ProviderAuthentication ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProviderAuthentications;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ProviderAuthentication> getProviderAuthenticationByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ProviderAuthentication where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and providerAuthid <> " + oldValue;
			}
			List<ProviderAuthentication> ProviderAuthentications = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProviderAuthentications;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);


	
	
	
//	public String addNewProviderAuthenitication(ProviderAuthentication providerAuthentication) {
	@Override
	public String addNewProviderAuthenitication(ProviderAuthentication providerAuthentication) {
		EntityManager entityManager = getEntityManager();
		try {
			
			ProviderAuthentication provider_account=new ProviderAuthentication();
			provider_account.setUsername(providerAuthentication.getUsername());
			List lstAccount=searchProviderAuthenticationforduplication(provider_account,false);
			if(lstAccount!=null && lstAccount.size() >0)
			{
				return "EXIST";
			}
			
			
			
			
			entityManager.getTransaction().begin();
			providerAuthentication.getProviderAccount().setCreatedBy(commonService.getCurrentLoggedInUser());
			providerAuthentication.getProviderAccount().setCreatedAt(commonService.getCurrentTimeStamp_new());
//			CharSequence seq_pass=providerAuthentication.getPassword();
			providerAuthentication.setPassword(passwordEncoder.encode(providerAuthentication.getPassword()));
			
//			passwordEncoder.encode("adf");
			
			this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
			int result = this.jdbcTemplateObject.queryForObject("SELECT roleid from  role_entity where name ilike 'PROVIDER'", Integer.class);
			if(result > 0)
			providerAuthentication.setRoleid((long)result);
			providerAuthentication.setIsForcedChangePassword(true);
			
		//	ProviderAuthentication.setIsActive(true);
//			ProviderAuthentication providerAuthentication=new ProviderAuthentication();
//			providerAuthentication=ProviderAuthentication.getProviderAuthentication();
			entityManager.persist(providerAuthentication);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	

	@Override
	public String deleteProviderAuthentication(List<String> ProviderAuthenticationsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String providerAuthid : ProviderAuthenticationsId) {
				ProviderAuthentication ProviderAuthentication = entityManager.find(ProviderAuthentication.class, Integer.parseInt(providerAuthid));
//				if (ProviderAuthentication != null) {
//					ProviderAuthentication.setBlIsDeleted(true);
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
	public String updateProviderAuthentication(ProviderAuthentication ProviderAuthentication) {
		EntityManager entityManager = getEntityManager();
		try {
			
			ProviderAuthentication provider_account=new ProviderAuthentication();
			provider_account.setUsername(ProviderAuthentication.getUsername());
			provider_account.setProviderAuthid(ProviderAuthentication.getProviderAuthid());
			List lstAccount=searchProviderAuthenticationforduplication(provider_account,true);
			if(lstAccount!=null && lstAccount.size() >0)
			{
				return "EXIST";
			}
			
			entityManager.getTransaction().begin();
			ProviderAuthentication.getProviderAccount().setUpdatedBy((long)commonService.getCurrentLoggedInUser());
			ProviderAuthentication.getProviderAccount().setUpdatedAt(commonService.getCurrentTimeStamp_new());
			entityManager.merge(ProviderAuthentication);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProviderAuthenticationNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getProviderAuthenticationById(String ProviderAuthenticationId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ProviderAuthentication where txtProviderAuthenticationCode='" + ProviderAuthenticationId + "'";

			List<ProviderAuthentication> ProviderAuthentication = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProviderAuthentication.size() > 0) {
				return String.valueOf(ProviderAuthentication.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<ProviderAuthentication> searchProviderAuthentication(ProviderAuthentication ProviderAuthentication) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from ProviderAuthentication ProviderAuthentication where 1=1 ";
	  
	 
	    if(ProviderAuthentication.getUsername() !=null){
	    	if(ProviderAuthentication.getUsername().equalsIgnoreCase("Provider"))
	    	query+=" and ProviderAuthentication.provider is not null  ";
	    	else
	    		query+=" and ProviderAuthentication.providerAccount is not null  ";
	    }
	  
	    
	    /*if(ProviderAuthentication.getTxtEmail() !=null){
	    	query+=" and upper(ProviderAuthentication.txtEmail) like"+" upper('"+ProviderAuthentication.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(ProviderAuthentication.getProviderAuthenticationId() !=null){
//	    	query+=" and ProviderAuthentication.providerAuthenticationId ="+" "+ProviderAuthentication.getProviderAuthenticationId()+""+"  ";
//	    }
	  
	    query+=" order by ProviderAuthentication.providerAuthid  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<ProviderAuthentication> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	public List<ProviderAuthentication> searchProviderAuthenticationforduplication(ProviderAuthentication ProviderAuthentication,boolean isupdate) {
		EntityManager entityManager = getEntityManager();
    entityManager.getTransaction().begin();
    String query = "from ProviderAuthentication ProviderAuthentication where 1=1 ";
    

    		
 
    
    if(ProviderAuthentication.getUsername() !=null){
    	
    	query+=" and ProviderAuthentication.username = '"+ProviderAuthentication.getUsername()+"'";
    	
    }
    
    if(isupdate)
    {
    	 if(ProviderAuthentication.getProviderAuthid() !=null){
    	    	query+=" and ProviderAuthentication.providerAuthid !="+" "+ProviderAuthentication.getProviderAuthid()+""+"  ";
    	    }
    	    
    }
  
    
    /*if(ProviderAuthentication.getTxtEmail() !=null){
    	query+=" and upper(ProviderAuthentication.txtEmail) like"+" upper('"+ProviderAuthentication.getTxtEmail()+"')"+"  ";
    }*/
    
//    if(ProviderAuthentication.getProviderAuthenticationId() !=null){
//    	query+=" and ProviderAuthentication.providerAuthenticationId ="+" "+ProviderAuthentication.getProviderAuthenticationId()+""+"  ";
//    }
  
    query+=" order by ProviderAuthentication.providerAuthid  DESC";
    log.info("Query is ---"+query.substring(0, query.length()));
    
    System.out.println("query ----:"+query.substring(0, query.length()));
    String subQuery = query.substring(0, query.length());
    List<ProviderAuthentication> cust = entityManager.createQuery(
    		subQuery).getResultList();
    
    entityManager.getTransaction().commit();
    entityManager.close();
 
    return cust;
}
}
