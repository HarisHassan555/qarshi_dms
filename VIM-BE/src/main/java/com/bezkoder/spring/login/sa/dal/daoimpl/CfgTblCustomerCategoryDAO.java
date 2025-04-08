package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomerCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomerCategory;

@Repository
public class CfgTblCustomerCategoryDAO implements ICfgTblCustomerCategoryDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblCustomerCategoryDAO.class);

	public CfgTblCustomerCategoryDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomerCategory> getAllCustomerCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCustomerCategory> CustomerCategorys = entityManager.createQuery("FROM CfgTblCustomerCategory where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return CustomerCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomerCategory> getActiveCustomerCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCustomerCategory> CustomerCategorys = entityManager
				.createQuery("FROM CfgTblCustomerCategory where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return CustomerCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomerCategory> getCustomerCategoryByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomerCategory where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblCustomerCategory> CustomerCategorys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return CustomerCategorys;
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
	public String addNewCustomerCategory(CfgTblCustomerCategory CfgTblCustomerCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCustomerCategory.setBlnStatus(true);
			CfgTblCustomerCategory.setBlIsDeleted(false);
			CfgTblCustomerCategory.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblCustomerCategory.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblCustomerCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteCustomerCategory(List<String> CustomerCategorysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serCustomerCategoryId : CustomerCategorysId) {
				CfgTblCustomerCategory CustomerCategory = entityManager.find(CfgTblCustomerCategory.class, Integer.parseInt(serCustomerCategoryId));
				if (CustomerCategory != null) {
					CustomerCategory.setBlIsDeleted(true);

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
	public String updateCustomerCategory(CfgTblCustomerCategory CfgTblCustomerCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblCustomerCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateCustomerCategoryNo(String type) {
		// int CustomerCategoryNo;
		String CustomerCategoryType = type;
		// String CustomerCategoryCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (CustomerCategoryType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCustomerCategoryCode) from CfgTblCustomerCategory ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "C-Cat-OPL-RW-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(13));

				ord_no = ord_no + 1;
				String code = "C-Cat-OPL-RW-1";
				if (ord_no < 10)
					code = "C-Cat-OPL-RW-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "C-Cat-OPL-RW-0" + ord_no;
				else
					code = "C-Cat-OPL-RW-" + ord_no;
				entityManager.close();
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCustomerCategoryCode) from CfgTblCustomerCategory ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "C-Cat-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(6));

				ord_no1 = ord_no1 + 1;
				String code = "C-Cat-1";
				if (ord_no1 < 10)
					code = "C-Cat-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "C-Cat-0" + ord_no1;
				else
					code = "C-Cat-" + ord_no1;
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

	public String getCustomerCategoryById(String CustomerCategoryId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomerCategory where txtCustomerCategoryCode='" + CustomerCategoryId + "'";

			List<CfgTblCustomerCategory> CustomerCategory = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (CustomerCategory.size() > 0) {
				return String.valueOf(CustomerCategory.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblCustomerCategory> searchCustomerCategory(CfgTblCustomerCategory CustomerCategory) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblCustomerCategory CustomerCategory where 1=1 ";
	    if(CustomerCategory.getTxtCustomerCategoryCode() != null)
	    {
	    	query+=" and upper(CustomerCategory.txtCustomerCategoryCode) like"+" upper('"+CustomerCategory.getTxtCustomerCategoryCode()+"%')"+" ";
	    }
	    if(CustomerCategory.getTxtCustomerCategoryName() !=null){
	    	query+=" and upper(CustomerCategory.txtCustomerCategoryName) like"+" upper('"+CustomerCategory.getTxtCustomerCategoryName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(CustomerCategory.getTxtEmail() !=null){
	    	query+=" and upper(CustomerCategory.txtEmail) like"+" upper('"+CustomerCategory.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(CustomerCategory.getSerCustomerCategoryId() !=null){
	    	query+=" and CustomerCategory.serCustomerCategoryId ="+" "+CustomerCategory.getSerCustomerCategoryId()+""+"  ";
	    }
	  
	    query+=" order by CustomerCategory.serCustomerCategoryId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblCustomerCategory> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
