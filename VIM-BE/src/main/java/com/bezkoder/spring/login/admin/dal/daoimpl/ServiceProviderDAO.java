package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IServiceProviderDAO;
import com.bezkoder.spring.login.admin.dal.entities.ServiceProvider;

@Repository
public class ServiceProviderDAO implements IServiceProviderDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(ServiceProviderDAO.class);

	public ServiceProviderDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ServiceProvider> getAllServiceProvider() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<ServiceProvider> ServiceProviders = entityManager.createQuery("FROM ServiceProvider ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ServiceProviders;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ServiceProvider> getActiveServiceProvider() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<ServiceProvider> ServiceProviders = entityManager
				.createQuery("FROM ServiceProvider  ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ServiceProviders;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ServiceProvider> getServiceProviderByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ServiceProvider where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serServiceProviderId <> " + oldValue;
			}
			List<ServiceProvider> ServiceProviders = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ServiceProviders;
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
	public String addNewServiceProvider(ServiceProvider ServiceProvider) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			ServiceProvider.setCreatedBy((long)commonService.getCurrentLoggedInUser());
			ServiceProvider.setCreatedAt(commonService.getCurrentTimeStamp_new());
			ServiceProvider.setIsActive(true);
			entityManager.persist(ServiceProvider);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteServiceProvider(List<String> ServiceProvidersId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serServiceProviderId : ServiceProvidersId) {
				ServiceProvider ServiceProvider = entityManager.find(ServiceProvider.class, Integer.parseInt(serServiceProviderId));
//				if (ServiceProvider != null) {
//					ServiceProvider.setBlIsDeleted(true);
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
	public String updateServiceProvider(ServiceProvider ServiceProvider) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			ServiceProvider.setUpdatedBy((long)commonService.getCurrentLoggedInUser());
			ServiceProvider.setUpdatedAt(commonService.getCurrentTimeStamp_new());
			entityManager.merge(ServiceProvider);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateServiceProviderNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getServiceProviderById(String ServiceProviderId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM ServiceProvider where txtServiceProviderCode='" + ServiceProviderId + "'";

			List<ServiceProvider> ServiceProvider = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ServiceProvider.size() > 0) {
				return String.valueOf(ServiceProvider.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<ServiceProvider> searchServiceProvider(ServiceProvider ServiceProvider) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from ServiceProvider ServiceProvider where 1=1 ";
	  
	    if(ServiceProvider.getServiceProviderName() !=null){
	    	query+=" and upper(ServiceProvider.serviceProviderName) like"+" upper('"+ServiceProvider.getServiceProviderName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(ServiceProvider.getTxtEmail() !=null){
	    	query+=" and upper(ServiceProvider.txtEmail) like"+" upper('"+ServiceProvider.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ServiceProvider.getServiceProviderid() !=null){
	    	query+=" and ServiceProvider.serviceProviderId ="+" "+ServiceProvider.getServiceProviderid()+""+"  ";
	    }
	  
	    query+=" order by ServiceProvider.serServiceProviderId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<ServiceProvider> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
