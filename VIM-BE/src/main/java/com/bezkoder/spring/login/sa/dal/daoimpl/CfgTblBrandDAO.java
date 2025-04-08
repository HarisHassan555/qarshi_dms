package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblBrandDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBrand;

@Repository
public class CfgTblBrandDAO implements ICfgTblBrandDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblBrandDAO.class);

	public CfgTblBrandDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBrand> getAllBrand() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblBrand> Brands = entityManager.createQuery("FROM CfgTblBrand where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Brands;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBrand> getActiveBrand() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblBrand> Brands = entityManager
				.createQuery("FROM CfgTblBrand where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Brands;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBrand> getBrandByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblBrand where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblBrand> Brands = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Brands;
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
	public String addNewBrand(CfgTblBrand CfgTblBrand) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblBrand.setBlnStatus(true);
			CfgTblBrand.setBlIsDeleted(false);
			CfgTblBrand.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblBrand.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblBrand);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteBrand(List<String> BrandsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serBrandId : BrandsId) {
				CfgTblBrand Brand = entityManager.find(CfgTblBrand.class, Integer.parseInt(serBrandId));
				if (Brand != null) {
					Brand.setBlIsDeleted(true);

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
	public String updateBrand(CfgTblBrand CfgTblBrand) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblBrand);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateBrandNo(String type) {
		// int BrandNo;
		String BrandType = type;
		// String BrandCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (BrandType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtBrandCode) from CfgTblBrand ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "BRND-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "BRND-1";
				if (ord_no < 10)
					code = "BRND-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "BRND-0" + ord_no;
				else
					code = "BRND-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtBrandCode) from CfgTblBrand ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "BRND-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(5));

				ord_no1 = ord_no1 + 1;
				String code = "BRND-1";
				if (ord_no1 < 10)
					code = "BRND-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "BRND-0" + ord_no1;
				else
					code = "BRND-" + ord_no1;
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

	public String getBrandById(String BrandId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblBrand where txtBrandCode='" + BrandId + "'";

			List<CfgTblBrand> Brand = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Brand.size() > 0) {
				return String.valueOf(Brand.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblBrand> searchBrand(CfgTblBrand Brand) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblBrand Brand where 1=1 ";
	    if(Brand.getTxtBrandCode() != null)
	    {
	    	query+=" and upper(Brand.txtBrandCode) like"+" upper('"+Brand.getTxtBrandCode()+"%')"+" ";
	    }
	    if(Brand.getTxtBrandName() !=null){
	    	query+=" and upper(Brand.txtBrandName) like"+" upper('"+Brand.getTxtBrandName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Brand.getTxtEmail() !=null){
	    	query+=" and upper(Brand.txtEmail) like"+" upper('"+Brand.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Brand.getSerBrandId() !=null){
	    	query+=" and Brand.serBrandId ="+" "+Brand.getSerBrandId()+""+"  ";
	    }
	  
	    query+=" order by Brand.serBrandId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblBrand> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
