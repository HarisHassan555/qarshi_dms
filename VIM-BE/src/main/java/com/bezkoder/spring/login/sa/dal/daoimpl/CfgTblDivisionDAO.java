package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDivisionDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDivision;

@Repository
public class CfgTblDivisionDAO implements ICfgTblDivisionDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblDivisionDAO.class);

	public CfgTblDivisionDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDivision> getAllDivision() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDivision> Divisions = entityManager.createQuery("FROM CfgTblDivision where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Divisions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDivision> getActiveDivision() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDivision> Divisions = entityManager
				.createQuery("FROM CfgTblDivision where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Divisions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDivision> getDivisionByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDivision where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblDivision> Divisions = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Divisions;
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
	public String addNewDivision(CfgTblDivision CfgTblDivision) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDivision.setBlnStatus(true);
			CfgTblDivision.setBlIsDeleted(false);
			entityManager.persist(CfgTblDivision);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDivision(List<String> DivisionsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDivisionId : DivisionsId) {
				CfgTblDivision Division = entityManager.find(CfgTblDivision.class, Integer.parseInt(serDivisionId));
				if (Division != null) {
					Division.setBlIsDeleted(true);
					Division.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Division.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateDivision(CfgTblDivision CfgTblDivision) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDivision.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblDivision.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblDivision);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDivisionNo(String type) {
		// int DivisionNo;
		String DivisionType = type;
		// String DivisionCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DivisionType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDivisionCode) from CfgTblDivision ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDivisionCode) from CfgTblDivision ")
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

	public String getDivisionById(String DivisionId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDivision where txtDivisionCode='" + DivisionId + "'";

			List<CfgTblDivision> Division = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Division.size() > 0) {
				return String.valueOf(Division.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblDivision> searchDivision(CfgTblDivision Division) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblDivision Division where 1=1 ";
	  
	    if(Division.getTxtName() !=null){
	    	query+=" and upper(Division.txtName) like"+" upper('"+Division.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Division.getTxtEmail() !=null){
	    	query+=" and upper(Division.txtEmail) like"+" upper('"+Division.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Division.getSerDivisionId() !=null){
	    	query+=" and Division.serDivisionId ="+" "+Division.getSerDivisionId()+""+"  ";
	    }
	  
	    query+=" order by Division.serDivisionId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblDivision> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
