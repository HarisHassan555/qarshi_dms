package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCountryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCountry;

@Repository
public class CfgTblCountryDAO implements ICfgTblCountryDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblCountryDAO.class);

	public CfgTblCountryDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCountry> getAllCountry() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCountry> Countrys = entityManager.createQuery("FROM CfgTblCountry where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Countrys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCountry> getActiveCountry() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCountry> Countrys = entityManager
				.createQuery("FROM CfgTblCountry where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Countrys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCountry> getCountryByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCountry where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblCountry> Countrys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Countrys;
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
	public String addNewCountry(CfgTblCountry CfgTblCountry) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCountry.setBlnStatus(true);
			CfgTblCountry.setBlIsDeleted(false);
			entityManager.persist(CfgTblCountry);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteCountry(List<String> CountrysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serCountryId : CountrysId) {
				CfgTblCountry Country = entityManager.find(CfgTblCountry.class, Integer.parseInt(serCountryId));
				if (Country != null) {
					Country.setBlIsDeleted(true);
					Country.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Country.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateCountry(CfgTblCountry CfgTblCountry) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCountry.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblCountry.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblCountry);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateCountryNo(String type) {
		// int CountryNo;
		String CountryType = type;
		// String CountryCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (CountryType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCountryCode) from CfgTblCountry ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "BK-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "BK-1";
				if (ord_no < 10)
					code = "BK-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "BK-0" + ord_no;
				else
					code = "BK-" + ord_no;
				entityManager.close();
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCountryCode) from CfgTblCountry ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CTR-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "CTR-1";
				if (ord_no1 < 10)
					code = "CTR-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "CTR-0" + ord_no1;
				else
					code = "CTR-" + ord_no1;
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

	public String getCountryById(String CountryId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCountry where txtCountryCode='" + CountryId + "'";

			List<CfgTblCountry> Country = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Country.size() > 0) {
				return String.valueOf(Country.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblCountry> searchCountry(CfgTblCountry Country) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblCountry Country where 1=1 ";
	  
	    if(Country.getTxtName() !=null){
	    	query+=" and upper(Country.txtName) like"+" upper('"+Country.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Country.getTxtEmail() !=null){
	    	query+=" and upper(Country.txtEmail) like"+" upper('"+Country.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Country.getSerCountryId() !=null){
	    	query+=" and Country.serCountryId ="+" "+Country.getSerCountryId()+""+"  ";
	    }
	  
	    query+=" order by Country.serCountryId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblCountry> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
