package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IProviderDAO;
import com.bezkoder.spring.login.admin.dal.entities.Provider;

@Repository
public class ProviderDAO implements IProviderDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(ProviderDAO.class);

	public ProviderDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Provider> getAllProvider() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<Provider> Providers = entityManager.createQuery("FROM Provider where blIsDeleted=false or blIsDeleted is null ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Providers;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Provider> getActiveProvider() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<Provider> Providers = entityManager
				.createQuery("FROM Provider where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Providers;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Provider> getProviderByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Provider where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serProviderId <> " + oldValue;
			}
			List<Provider> Providers = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Providers;
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
	public String addNewProvider(Provider Provider) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Provider.setCreatedBy(commonService.getCurrentLoggedInUser());
			Provider.setCreatedAt(commonService.getCurrentTimeStamp_new());
			Provider.setIsActive(true);
			entityManager.persist(Provider);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProvider(List<String> ProvidersId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProviderId : ProvidersId) {
				Provider Provider = entityManager.find(Provider.class, Integer.parseInt(serProviderId));
//				if (Provider != null) {
//					Provider.setBlIsDeleted(true);
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
	public String updateProvider(Provider Provider) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Provider.setUpdatedBy((long)commonService.getCurrentLoggedInUser());
			Provider.setUpdatedAt(commonService.getCurrentTimeStamp_new());
			entityManager.merge(Provider);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProviderNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getProviderById(String ProviderId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Provider where txtProviderCode='" + ProviderId + "'";

			List<Provider> Provider = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Provider.size() > 0) {
				return String.valueOf(Provider.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<Provider> searchProvider(Provider Provider) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from Provider Provider where 1=1 ";
	  
//	    if(Provider.getProviderName() !=null){
//	    	query+=" and upper(Provider.providerName) like"+" upper('"+Provider.getProviderName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(Provider.getTxtEmail() !=null){
	    	query+=" and upper(Provider.txtEmail) like"+" upper('"+Provider.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(Provider.getProviderId() !=null){
//	    	query+=" and Provider.providerId ="+" "+Provider.getProviderId()+""+"  ";
//	    }
//	  
	    query+=" order by Provider.serProviderId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<Provider> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
