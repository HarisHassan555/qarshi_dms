package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblSalesOrganizationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSalesOrganization;

@Repository
public class CfgTblSalesOrganizationDAO implements ICfgTblSalesOrganizationDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblSalesOrganizationDAO.class);

	public CfgTblSalesOrganizationDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSalesOrganization> getAllSalesOrganization() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSalesOrganization> SalesOrganizations = entityManager.createQuery("FROM CfgTblSalesOrganization where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SalesOrganizations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSalesOrganization> getActiveSalesOrganization() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSalesOrganization> SalesOrganizations = entityManager
				.createQuery("FROM CfgTblSalesOrganization where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SalesOrganizations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSalesOrganization> getSalesOrganizationByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSalesOrganization where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblSalesOrganization> SalesOrganizations = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return SalesOrganizations;
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
	public String addNewSalesOrganization(CfgTblSalesOrganization CfgTblSalesOrganization) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblSalesOrganization.setBlnStatus(true);
			CfgTblSalesOrganization.setBlIsDeleted(false);
			entityManager.persist(CfgTblSalesOrganization);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteSalesOrganization(List<String> SalesOrganizationsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serSalesOrganizationId : SalesOrganizationsId) {
				CfgTblSalesOrganization SalesOrganization = entityManager.find(CfgTblSalesOrganization.class, Integer.parseInt(serSalesOrganizationId));
				if (SalesOrganization != null) {
					SalesOrganization.setBlIsDeleted(true);
					SalesOrganization.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					SalesOrganization.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateSalesOrganization(CfgTblSalesOrganization CfgTblSalesOrganization) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblSalesOrganization.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblSalesOrganization.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblSalesOrganization);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateSalesOrganizationNo(String type) {
		// int SalesOrganizationNo;
		String SalesOrganizationType = type;
		// String SalesOrganizationCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (SalesOrganizationType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtSalesOrganizationCode) from CfgTblSalesOrganization ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtSalesOrganizationCode) from CfgTblSalesOrganization ")
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

	public String getSalesOrganizationById(String SalesOrganizationId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSalesOrganization where txtSalesOrganizationCode='" + SalesOrganizationId + "'";

			List<CfgTblSalesOrganization> SalesOrganization = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (SalesOrganization.size() > 0) {
				return String.valueOf(SalesOrganization.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblSalesOrganization> searchSalesOrganization(CfgTblSalesOrganization SalesOrganization) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblSalesOrganization SalesOrganization where 1=1 ";
	  
	    if(SalesOrganization.getTxtName() !=null){
	    	query+=" and upper(SalesOrganization.txtName) like"+" upper('"+SalesOrganization.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(SalesOrganization.getTxtEmail() !=null){
	    	query+=" and upper(SalesOrganization.txtEmail) like"+" upper('"+SalesOrganization.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(SalesOrganization.getSerSalesOrganizationId() !=null){
	    	query+=" and SalesOrganization.serSalesOrganizationId ="+" "+SalesOrganization.getSerSalesOrganizationId()+""+"  ";
	    }
	  
	    query+=" order by SalesOrganization.serSalesOrganizationId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblSalesOrganization> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
