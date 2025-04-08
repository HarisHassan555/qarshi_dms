package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblPhenomenonDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblPhenomenon;

@Repository
public class CfgTblPhenomenonDAO implements ICfgTblPhenomenonDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblPhenomenonDAO.class);

	public CfgTblPhenomenonDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPhenomenon> getAllPhenomenon() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblPhenomenon> Phenomenons = entityManager.createQuery("FROM CfgTblPhenomenon where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Phenomenons;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPhenomenon> getActivePhenomenon() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblPhenomenon> Phenomenons = entityManager
				.createQuery("FROM CfgTblPhenomenon where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Phenomenons;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblPhenomenon> getPhenomenonByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPhenomenon where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblPhenomenon> Phenomenons = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Phenomenons;
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
	public String addNewPhenomenon(CfgTblPhenomenon CfgTblPhenomenon) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblPhenomenon.setBlnStatus(true);
			CfgTblPhenomenon.setBlIsDeleted(false);
			entityManager.persist(CfgTblPhenomenon);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePhenomenon(List<String> PhenomenonsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPhenomenonId : PhenomenonsId) {
				CfgTblPhenomenon Phenomenon = entityManager.find(CfgTblPhenomenon.class, Integer.parseInt(serPhenomenonId));
				if (Phenomenon != null) {
					Phenomenon.setBlIsDeleted(true);
					Phenomenon.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Phenomenon.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updatePhenomenon(CfgTblPhenomenon CfgTblPhenomenon) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblPhenomenon.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblPhenomenon.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblPhenomenon);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePhenomenonNo(String type) {
		// int PhenomenonNo;
		String PhenomenonType = type;
		// String PhenomenonCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (PhenomenonType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPhenomenonCode) from CfgTblPhenomenon ")
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
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPhenomenonCode) from CfgTblPhenomenon ")
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

	public String getPhenomenonById(String PhenomenonId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblPhenomenon where txtPhenomenonCode='" + PhenomenonId + "'";

			List<CfgTblPhenomenon> Phenomenon = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Phenomenon.size() > 0) {
				return String.valueOf(Phenomenon.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblPhenomenon> searchPhenomenon(CfgTblPhenomenon Phenomenon) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblPhenomenon Phenomenon where 1=1 ";
	  
	    if(Phenomenon.getTxtPhenomenonName() !=null){
	    	query+=" and upper(Phenomenon.txtPhenomenonName) like"+" upper('"+Phenomenon.getTxtPhenomenonName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Phenomenon.getTxtEmail() !=null){
	    	query+=" and upper(Phenomenon.txtEmail) like"+" upper('"+Phenomenon.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Phenomenon.getSerPhenomenonId() !=null){
	    	query+=" and Phenomenon.serPhenomenonId ="+" "+Phenomenon.getSerPhenomenonId()+""+"  ";
	    }
	  
	    query+=" order by Phenomenon.serPhenomenonId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblPhenomenon> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
