package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCityDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCity;

@Repository
public class CfgTblCityDAO implements ICfgTblCityDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblCityDAO.class);

	public CfgTblCityDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCity> getAllCity() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<CfgTblCity> Citys = entityManager.createQuery("FROM CfgTblCity where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Citys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCity> getActiveCity() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCity> Citys = entityManager
				.createQuery("FROM CfgTblCity where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		

		return Citys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCity> getCityByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCity where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblCity> Citys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Citys;
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
	public String addNewCity(CfgTblCity CfgTblCity) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCity.setBlnStatus(true);
			CfgTblCity.setBlIsDeleted(false);
			entityManager.persist(CfgTblCity);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteCity(List<String> CitysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serCityId : CitysId) {
				CfgTblCity City = entityManager.find(CfgTblCity.class, Integer.parseInt(serCityId));
				if (City != null) {
					City.setBlIsDeleted(true);

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
	public String updateCity(CfgTblCity CfgTblCity) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblCity);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateCityNo(String type) {
		// int CityNo;
		String CityType = type;
		// String CityCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (CityType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCityCode) from CfgTblCity ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CITY-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "CITY-1";
				if (ord_no < 10)
					code = "CITY-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "CITY-0" + ord_no;
				else
					code = "CITY-" + ord_no;
				entityManager.close();
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCityCode) from CfgTblCity ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CITY-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(5));

				ord_no1 = ord_no1 + 1;
				String code = "CITY-1";
				if (ord_no1 < 10)
					code = "CITY-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "CITY-0" + ord_no1;
				else
					code = "CITY-" + ord_no1;
				entityManager.close();
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

	public String getCityById(String CityId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCity where txtCityCode='" + CityId + "'";

			List<CfgTblCity> City = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (City.size() > 0) {
				return String.valueOf(City.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblCity> searchCity(CfgTblCity City) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblCity City where 1=1 ";
	    if(City.getTxtCityCode() != null)
	    {
	    	query+=" and upper(City.txtCityCode) like"+" upper('"+City.getTxtCityCode()+"%')"+" ";
	    }
	    if(City.getTxtCityName() !=null){
	    	query+=" and upper(City.txtCityName) like"+" upper('"+City.getTxtCityName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(City.getTxtEmail() !=null){
	    	query+=" and upper(City.txtEmail) like"+" upper('"+City.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(City.getSerCityId() !=null){
	    	query+=" and City.serCityId ="+" "+City.getSerCityId()+""+"  ";
	    }
	  
	    query+=" order by City.serCityId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblCity> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
