package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IBaaDAO;
import com.bezkoder.spring.login.admin.dal.entities.Baa;

@Repository
public class BaaDAO implements IBaaDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(BaaDAO.class);

	public BaaDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Baa> getAllBaa() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<Baa> Baas = entityManager.createQuery("FROM Baa where blIsDeleted=false or blIsDeleted is null ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Baas;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Baa> getActiveBaa() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<Baa> Baas = entityManager
				.createQuery("FROM Baa where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Baas;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Baa> getBaaByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Baa where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBaaId <> " + oldValue;
			}
			List<Baa> Baas = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Baas;
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
	public String addNewBaa(Baa Baa) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Baa.setCreatedBy(commonService.getCurrentLoggedInUser());
			Baa.setCreatedAt(commonService.getCurrentTimeStamp_new());
			Baa.setIsActive(true);
			entityManager.persist(Baa);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteBaa(List<String> BaasId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serBaaId : BaasId) {
				Baa Baa = entityManager.find(Baa.class, Integer.parseInt(serBaaId));
//				if (Baa != null) {
//					Baa.setBlIsDeleted(true);
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
	public String updateBaa(Baa Baa) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			Baa.setUpdatedBy(commonService.getCurrentLoggedInUser());
			Baa.setUpdatedAt(commonService.getCurrentTimeStamp_new());
			entityManager.merge(Baa);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateBaaNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getBaaById(String BaaId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Baa where txtBaaCode='" + BaaId + "'";

			List<Baa> Baa = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Baa.size() > 0) {
				return String.valueOf(Baa.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<Baa> searchBaa(Baa Baa) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from Baa Baa where 1=1 ";
	  
	    if(Baa.getBaaName() !=null){
	    	query+=" and upper(Baa.baaName) like"+" upper('"+Baa.getBaaName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Baa.getTxtEmail() !=null){
	    	query+=" and upper(Baa.txtEmail) like"+" upper('"+Baa.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Baa.getBaaId() !=null){
	    	query+=" and Baa.baaId ="+" "+Baa.getBaaId()+""+"  ";
	    }
	  
	    query+=" order by Baa.serBaaId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<Baa> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
