package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblDesignationDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDesignation;

@Repository
public class HrTblDesignationDAO implements IHrTblDesignationDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(HrTblDesignationDAO.class);

	public HrTblDesignationDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDesignation> getAllDesignations() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblDesignation> Designations = entityManager.createQuery("FROM HrTblDesignation where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Designations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDesignation> getActiveDesignations() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblDesignation> Designations = entityManager
				.createQuery("FROM HrTblDesignation where blStatus=true  and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Designations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDesignation> getDesignationByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblDesignation where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<HrTblDesignation> Designations = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Designations;
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
	public String addNewDesignation(HrTblDesignation HrTblDesignation) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblDesignation.setBlnStatus(true);
			HrTblDesignation.setBlIsDeleted(false);
			entityManager.persist(HrTblDesignation);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDesignations(List<String> DesignationsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDesignationId : DesignationsId) {
				HrTblDesignation Designation = entityManager.find(HrTblDesignation.class, Integer.parseInt(serDesignationId));
				if (Designation != null) {
					Designation.setBlIsDeleted(true);
					Designation.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
					Designation.setSerModifiedUser(commonService.getCurrentLoggedInUser());
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
	public String updateDesignation(HrTblDesignation HrTblDesignation) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblDesignation.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
			HrTblDesignation.setSerModifiedUser(commonService.getCurrentLoggedInUser());
			entityManager.merge(HrTblDesignation);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDesignationNo(String type) {
		// int DesignationNo;
		String DesignationType = type;
		// String DesignationCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DesignationType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDesignationCode) from HrTblDesignation ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDesignationCode) from HrTblDesignation ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "DSG-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "DSG-1";
				if (ord_no1 < 10)
					code = "DSG-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "DSG-0" + ord_no1;
				else
					code = "DSG-" + ord_no1;
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

	public String getDesignationById(String DesignationId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblDesignation where txtDesignationCode='" + DesignationId + "'";

			List<HrTblDesignation> Designation = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Designation.size() > 0) {
				return String.valueOf(Designation.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<HrTblDesignation> searchDesignation(HrTblDesignation Designation) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from HrTblDesignation Designation where 1=1 ";
	    if(Designation.getTxtDesignationCode() != null)
	    {
	    	query+=" and upper(Designation.txtDesignationCode) like"+" upper('"+Designation.getTxtDesignationCode()+"%')"+" ";
	    }
	    if(Designation.getTxtDesignationName() !=null){
	    	query+=" and upper(Designation.txtDesignationName) like"+" upper('"+Designation.getTxtDesignationName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Designation.getTxtEmail() !=null){
	    	query+=" and upper(Designation.txtEmail) like"+" upper('"+Designation.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Designation.getSerDesignationId() !=null){
	    	query+=" and Designation.serDesignationId ="+" "+Designation.getSerDesignationId()+""+"  ";
	    }
	  
	    query+=" order by Designation.serDesignationId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<HrTblDesignation> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
