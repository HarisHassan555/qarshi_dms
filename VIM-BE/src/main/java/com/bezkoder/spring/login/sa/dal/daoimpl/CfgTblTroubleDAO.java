package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblTroubleDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblTrouble;

@Repository
public class CfgTblTroubleDAO implements ICfgTblTroubleDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblTroubleDAO.class);

	public CfgTblTroubleDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblTrouble> getAllTrouble() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblTrouble> Troubles = entityManager.createQuery("FROM CfgTblTrouble where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Troubles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblTrouble> getActiveTrouble() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblTrouble> Troubles = entityManager
				.createQuery("FROM CfgTblTrouble where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Troubles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblTrouble> getTroubleByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblTrouble where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblTrouble> Troubles = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Troubles;
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
	public String addNewTrouble(CfgTblTrouble CfgTblTrouble) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblTrouble.setBlnStatus(true);
			CfgTblTrouble.setBlIsDeleted(false);
			entityManager.persist(CfgTblTrouble);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteTrouble(List<String> TroublesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serTroubleId : TroublesId) {
				CfgTblTrouble Trouble = entityManager.find(CfgTblTrouble.class, Integer.parseInt(serTroubleId));
				if (Trouble != null) {
					Trouble.setBlIsDeleted(true);
					Trouble.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Trouble.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateTrouble(CfgTblTrouble CfgTblTrouble) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblTrouble.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblTrouble.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblTrouble);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateTroubleNo(String type) {
		// int TroubleNo;
		String TroubleType = type;
		// String TroubleCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (TroubleType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtTroubleCode) from CfgTblTrouble ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtTroubleCode) from CfgTblTrouble ")
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

	public String getTroubleById(String TroubleId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblTrouble where txtTroubleCode='" + TroubleId + "'";

			List<CfgTblTrouble> Trouble = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Trouble.size() > 0) {
				return String.valueOf(Trouble.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblTrouble> searchTrouble(CfgTblTrouble Trouble) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblTrouble Trouble where 1=1 ";
	  
	    if(Trouble.getTxtTroubleName() !=null){
	    	query+=" and upper(Trouble.txtTroubleName) like"+" upper('"+Trouble.getTxtTroubleName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Trouble.getTxtEmail() !=null){
	    	query+=" and upper(Trouble.txtEmail) like"+" upper('"+Trouble.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Trouble.getSerTroubleId() !=null){
	    	query+=" and Trouble.serTroubleId ="+" "+Trouble.getSerTroubleId()+""+"  ";
	    }
	  
	    query+=" order by Trouble.serTroubleId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblTrouble> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
