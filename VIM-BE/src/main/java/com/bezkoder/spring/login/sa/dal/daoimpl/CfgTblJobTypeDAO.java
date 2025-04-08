package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblJobTypeDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblJobType;

@Repository
public class CfgTblJobTypeDAO implements ICfgTblJobTypeDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblJobTypeDAO.class);

	public CfgTblJobTypeDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobType> getAllJobType() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblJobType> JobTypes = entityManager.createQuery("FROM CfgTblJobType where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return JobTypes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobType> getActiveJobType() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblJobType> JobTypes = entityManager
				.createQuery("FROM CfgTblJobType where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return JobTypes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblJobType> getJobTypeByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblJobType where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblJobType> JobTypes = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return JobTypes;
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
	public String addNewJobType(CfgTblJobType CfgTblJobType) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblJobType.setBlnStatus(true);
			CfgTblJobType.setBlIsDeleted(false);
			entityManager.persist(CfgTblJobType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteJobType(List<String> JobTypesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serJobTypeId : JobTypesId) {
				CfgTblJobType JobType = entityManager.find(CfgTblJobType.class, Integer.parseInt(serJobTypeId));
				if (JobType != null) {
					JobType.setBlIsDeleted(true);
					JobType.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					JobType.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateJobType(CfgTblJobType CfgTblJobType) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblJobType.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblJobType.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblJobType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateJobTypeNo(String type) {
		// int JobTypeNo;
		String JobTypeType = type;
		// String JobTypeCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (JobTypeType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtJobTypeCode) from CfgTblJobType ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtJobTypeCode) from CfgTblJobType ")
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

	public String getJobTypeById(String JobTypeId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblJobType where txtJobTypeCode='" + JobTypeId + "'";

			List<CfgTblJobType> JobType = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (JobType.size() > 0) {
				return String.valueOf(JobType.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblJobType> searchJobType(CfgTblJobType JobType) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblJobType JobType where 1=1 ";
	  
	    if(JobType.getTxtName() !=null){
	    	query+=" and upper(JobType.txtName) like"+" upper('"+JobType.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(JobType.getTxtEmail() !=null){
	    	query+=" and upper(JobType.txtEmail) like"+" upper('"+JobType.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(JobType.getSerJobTypeId() !=null){
	    	query+=" and JobType.serJobTypeId ="+" "+JobType.getSerJobTypeId()+""+"  ";
	    }
	  
	    query+=" order by JobType.serJobTypeId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblJobType> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
