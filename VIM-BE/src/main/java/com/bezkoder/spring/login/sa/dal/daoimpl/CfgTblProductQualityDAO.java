package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductQualityDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;

@Repository
public class CfgTblProductQualityDAO implements ICfgTblProductQualityDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblProductQualityDAO.class);

	public CfgTblProductQualityDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductQuality> getAllProductQuality() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductQuality> ProductQualitys = entityManager.createQuery("FROM CfgTblProductQuality where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductQualitys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductQuality> getActiveProductQuality() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductQuality> ProductQualitys = entityManager
				.createQuery("FROM CfgTblProductQuality where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductQualitys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductQuality> getProductQualityByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductQuality where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblProductQuality> ProductQualitys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProductQualitys;
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
	public String addNewProductQuality(CfgTblProductQuality CfgTblProductQuality) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblProductQuality.setBlnStatus(true);
			CfgTblProductQuality.setBlIsDeleted(false);
			CfgTblProductQuality.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblProductQuality.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblProductQuality);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProductQuality(List<String> ProductQualitysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProductQualityId : ProductQualitysId) {
				CfgTblProductQuality ProductQuality = entityManager.find(CfgTblProductQuality.class, Integer.parseInt(serProductQualityId));
				if (ProductQuality != null) {
					ProductQuality.setBlIsDeleted(true);

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
	public String updateProductQuality(CfgTblProductQuality CfgTblProductQuality) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblProductQuality);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProductQualityNo(String type) {
		// int ProductQualityNo;
		String ProductQualityType = type;
		// String ProductQualityCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ProductQualityType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductQualityCode) from CfgTblProductQuality ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "QLT-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(4));

				ord_no = ord_no + 1;
				String code = "QLT-1";
				if (ord_no < 10)
					code = "QLT-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "QLT-0" + ord_no;
				else
					code = "QLT-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductQualityCode) from CfgTblProductQuality ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "QLT-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "QLT-1";
				if (ord_no1 < 10)
					code = "QLT-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "QLT-0" + ord_no1;
				else
					code = "QLT-" + ord_no1;
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

	public String getProductQualityById(String ProductQualityId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductQuality where txtProductQualityCode='" + ProductQualityId + "'";

			List<CfgTblProductQuality> ProductQuality = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProductQuality.size() > 0) {
				return String.valueOf(ProductQuality.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblProductQuality> searchProductQuality(CfgTblProductQuality ProductQuality) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblProductQuality ProductQuality where 1=1 ";
	    if(ProductQuality.getTxtProductQualityCode() != null)
	    {
	    	query+=" and upper(ProductQuality.txtProductQualityCode) like"+" upper('"+ProductQuality.getTxtProductQualityCode()+"%')"+" ";
	    }
	    if(ProductQuality.getTxtProductQualityName() !=null){
	    	query+=" and upper(ProductQuality.txtProductQualityName) like"+" upper('"+ProductQuality.getTxtProductQualityName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(ProductQuality.getTxtEmail() !=null){
	    	query+=" and upper(ProductQuality.txtEmail) like"+" upper('"+ProductQuality.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ProductQuality.getSerProductQualityId() !=null){
	    	query+=" and ProductQuality.serProductQualityId ="+" "+ProductQuality.getSerProductQualityId()+""+"  ";
	    }
	  
	    query+=" order by ProductQuality.serProductQualityId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblProductQuality> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
