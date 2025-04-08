package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblJobCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobCategory;

@Repository
public class CfgTblJobCategoryDAO implements ICfgTblJobCategoryDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblJobCategoryDAO.class);

	public CfgTblJobCategoryDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobCategory> getAllJobCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblJobCategory> JobCategorys = entityManager.createQuery("FROM CfgTblJobCategory where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return JobCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobCategory> getActiveJobCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblJobCategory> JobCategorys = entityManager
				.createQuery("FROM CfgTblJobCategory where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return JobCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobCategory> getJobCategoryByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblJobCategory where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblJobCategory> JobCategorys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return JobCategorys;
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
	public String addNewJobCategory(CfgTblJobCategory CfgTblJobCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblJobCategory.setBlnStatus(true);
			CfgTblJobCategory.setBlIsDeleted(false);
			entityManager.persist(CfgTblJobCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteJobCategory(List<String> JobCategorysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serJobCategoryId : JobCategorysId) {
				CfgTblJobCategory JobCategory = entityManager.find(CfgTblJobCategory.class, Integer.parseInt(serJobCategoryId));
				if (JobCategory != null) {
					JobCategory.setBlIsDeleted(true);
					JobCategory.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					JobCategory.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateJobCategory(CfgTblJobCategory CfgTblJobCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblJobCategory.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblJobCategory.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblJobCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateJobCategoryNo(String type) {
		// int JobCategoryNo;
		String JobCategoryType = type;
		// String JobCategoryCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (JobCategoryType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtJobCategoryCode) from CfgTblJobCategory ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtJobCategoryCode) from CfgTblJobCategory ")
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

	public String getJobCategoryById(String JobCategoryId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblJobCategory where txtJobCategoryCode='" + JobCategoryId + "'";

			List<CfgTblJobCategory> JobCategory = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (JobCategory.size() > 0) {
				return String.valueOf(JobCategory.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblJobCategory> searchJobCategory(CfgTblJobCategory JobCategory) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblJobCategory JobCategory where 1=1 ";
	  
	    if(JobCategory.getTxtName() !=null){
	    	query+=" and upper(JobCategory.txtName) like"+" upper('"+JobCategory.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(JobCategory.getTxtEmail() !=null){
	    	query+=" and upper(JobCategory.txtEmail) like"+" upper('"+JobCategory.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(JobCategory.getSerJobCategoryId() !=null){
	    	query+=" and JobCategory.serJobCategoryId ="+" "+JobCategory.getSerJobCategoryId()+""+"  ";
	    }
	  
	    query+=" order by JobCategory.serJobCategoryId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblJobCategory> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
