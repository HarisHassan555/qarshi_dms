package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductCategoryDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductCategory;

@Repository
public class CfgTblProductCategoryDAO implements ICfgTblProductCategoryDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblProductCategoryDAO.class);

	public CfgTblProductCategoryDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductCategory> getAllProductCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductCategory> ProductCategorys = entityManager.createQuery("FROM CfgTblProductCategory where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductCategory> getActiveProductCategory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductCategory> ProductCategorys = entityManager
				.createQuery("FROM CfgTblProductCategory where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductCategorys;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductCategory> getProductCategoryByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductCategory where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblProductCategory> ProductCategorys = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProductCategorys;
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
	public String addNewProductCategory(CfgTblProductCategory CfgTblProductCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblProductCategory.setBlnStatus(true);
			CfgTblProductCategory.setBlIsDeleted(false);
			CfgTblProductCategory.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblProductCategory.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblProductCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProductCategory(List<String> ProductCategorysId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProductCategoryId : ProductCategorysId) {
				CfgTblProductCategory ProductCategory = entityManager.find(CfgTblProductCategory.class, Integer.parseInt(serProductCategoryId));
				if (ProductCategory != null) {
					ProductCategory.setBlIsDeleted(true);

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
	public String updateProductCategory(CfgTblProductCategory CfgTblProductCategory) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblProductCategory);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProductCategoryNo(String type) {
		// int ProductCategoryNo;
		String ProductCategoryType = type;
		// String ProductCategoryCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ProductCategoryType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductCategoryCode) from CfgTblProductCategory ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Cat-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(4));

				ord_no = ord_no + 1;
				String code = "Cat-1";
				if (ord_no < 10)
					code = "Cat-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "Cat-0" + ord_no;
				else
					code = "Cat-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductCategoryCode) from CfgTblProductCategory ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Cat-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "Cat-1";
				if (ord_no1 < 10)
					code = "Cat-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "Cat-0" + ord_no1;
				else
					code = "Cat-RW-" + ord_no1;
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

	public String getProductCategoryById(String ProductCategoryId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductCategory where txtProductCategoryCode='" + ProductCategoryId + "'";

			List<CfgTblProductCategory> ProductCategory = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProductCategory.size() > 0) {
				return String.valueOf(ProductCategory.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblProductCategory> searchProductCategory(CfgTblProductCategory ProductCategory) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblProductCategory ProductCategory where 1=1 ";
	    if(ProductCategory.getTxtProductCategoryCode() != null)
	    {
	    	query+=" and upper(ProductCategory.txtProductCategoryCode) like"+" upper('"+ProductCategory.getTxtProductCategoryCode()+"%')"+" ";
	    }
	    if(ProductCategory.getTxtProductCategoryName() !=null){
	    	query+=" and upper(ProductCategory.txtProductCategoryName) like"+" upper('"+ProductCategory.getTxtProductCategoryName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(ProductCategory.getTxtEmail() !=null){
	    	query+=" and upper(ProductCategory.txtEmail) like"+" upper('"+ProductCategory.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ProductCategory.getSerProductCategoryId() !=null){
	    	query+=" and ProductCategory.serProductCategoryId ="+" "+ProductCategory.getSerProductCategoryId()+""+"  ";
	    }
	  
	    query+=" order by ProductCategory.serProductCategoryId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblProductCategory> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
