package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IEhrDAO;
import com.bezkoder.spring.login.admin.dal.entities.Ehr;

@Repository
public class EhrDAO implements IEhrDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(EhrDAO.class);

	public EhrDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Ehr> getAllEhr() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<Ehr> Ehrs = entityManager.createQuery("FROM Ehr  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Ehrs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Ehr> getActiveEhr() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<Ehr> Ehrs = entityManager
				.createQuery("FROM Ehr").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Ehrs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Ehr> getEhrByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Ehr where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serEhrId <> " + oldValue;
			}
			List<Ehr> Ehrs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Ehrs;
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
	public String addNewEhr(Ehr Ehr) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Ehr.setCreatedBy(commonService.getCurrentLoggedInUser());
			Ehr.setCreatedAt(commonService.getCurrentTimeStamp_new());
		//	Ehr.setIsActive(true);
			Ehr.setAuthUrl("NA");
		//	Ehr.setEhrTypeid(1);
			//Ehr.setBaa(null);
			entityManager.persist(Ehr);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteEhr(List<String> EhrsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serEhrId : EhrsId) {
				Ehr Ehr = entityManager.find(Ehr.class, Integer.parseInt(serEhrId));
//				if (Ehr != null) {
//					Ehr.setBlIsDeleted(true);
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
	public String updateEhr(Ehr Ehr) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Ehr.setUpdatedBy((long)commonService.getCurrentLoggedInUser());
			Ehr.setUpdatedAt(commonService.getCurrentTimeStamp_new());
			entityManager.merge(Ehr);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateEhrNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getEhrById(String EhrId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Ehr where txtEhrCode='" + EhrId + "'";

			List<Ehr> Ehr = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Ehr.size() > 0) {
				return String.valueOf(Ehr.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<Ehr> searchEhr(Ehr Ehr) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from Ehr Ehr where 1=1 ";
	  
	    if(Ehr.getEhrName() !=null){
	    	query+=" and upper(Ehr.ehrName) like"+" upper('"+Ehr.getEhrName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Ehr.getTxtEmail() !=null){
	    	query+=" and upper(Ehr.txtEmail) like"+" upper('"+Ehr.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Ehr.getEhrid() > 0){
	    	query+=" and Ehr.ehrid ="+" "+Ehr.getEhrid()+""+"  ";
	    }
	  
	    query+=" order by Ehr.serEhrId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<Ehr> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
