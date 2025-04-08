package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDefectDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDefect;

@Repository
public class CfgTblDefectDAO implements ICfgTblDefectDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblDefectDAO.class);

	public CfgTblDefectDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDefect> getAllDefect() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDefect> Defects = entityManager.createQuery("FROM CfgTblDefect where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Defects;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDefect> getActiveDefect() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDefect> Defects = entityManager
				.createQuery("FROM CfgTblDefect where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Defects;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDefect> getDefectByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDefect where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblDefect> Defects = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Defects;
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
	public String addNewDefect(CfgTblDefect CfgTblDefect) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDefect.setBlnStatus(true);
			CfgTblDefect.setBlIsDeleted(false);
			entityManager.persist(CfgTblDefect);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDefect(List<String> DefectsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDefectId : DefectsId) {
				CfgTblDefect Defect = entityManager.find(CfgTblDefect.class, Integer.parseInt(serDefectId));
				if (Defect != null) {
					Defect.setBlIsDeleted(true);
					Defect.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Defect.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateDefect(CfgTblDefect CfgTblDefect) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDefect.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblDefect.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblDefect);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDefectNo(String type) {
		// int DefectNo;
		String DefectType = type;
		// String DefectCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DefectType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDefectCode) from CfgTblDefect ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDefectCode) from CfgTblDefect ")
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

	public String getDefectById(String DefectId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDefect where txtDefectCode='" + DefectId + "'";

			List<CfgTblDefect> Defect = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Defect.size() > 0) {
				return String.valueOf(Defect.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblDefect> searchDefect(CfgTblDefect Defect) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblDefect Defect where 1=1 ";
	  
	    if(Defect.getTxtDefectName() !=null){
	    	query+=" and upper(Defect.txtDefectName) like"+" upper('"+Defect.getTxtDefectName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Defect.getTxtEmail() !=null){
	    	query+=" and upper(Defect.txtEmail) like"+" upper('"+Defect.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Defect.getSerDefectId() !=null){
	    	query+=" and Defect.serDefectId ="+" "+Defect.getSerDefectId()+""+"  ";
	    }
	  
	    query+=" order by Defect.serDefectId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblDefect> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
