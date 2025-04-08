package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductDesignDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProcess;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

@Repository
public class CfgTblProductDesignDAO implements ICfgTblProductDesignDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	 @Autowired
	  private LoginDAO loginDao;

	private static final Logger log = LoggerFactory.getLogger(CfgTblProductDesignDAO.class);

	public CfgTblProductDesignDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductDesign> getAllProductDesign() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProductDesign> ProductDesigns = entityManager.createQuery("FROM CfgTblProductDesign where blIsDeleted=FALSE")
				.getResultList();*/
		
		List<CfgTblProductDesign> ProductDesigns =new ArrayList();
		List<CfgTblProcess> Processs=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
		{
			ProductDesigns = entityManager.createQuery("FROM CfgTblProductDesign where   serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE ").getResultList();

		}
		else
			ProductDesigns = entityManager
				.createQuery("FROM CfgTblProductDesign where  blIsDeleted=FALSE").getResultList();
		
		
		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductDesigns;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductDesign> getActiveProductDesign() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
	/*	List<CfgTblProductDesign> ProductDesigns = entityManager
				.createQuery("FROM CfgTblProductDesign where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();
*/
		List<CfgTblProductDesign> ProductDesigns =new ArrayList();
		List<CfgTblProcess> Processs=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
		{
//			ProductDesigns = entityManager.createQuery("FROM CfgTblProductDesign where blnStatus=TRUE and  serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE ").getResultList();
			ProductDesigns = entityManager.createQuery("FROM CfgTblProductDesign  ").getResultList();

		}
		else
			ProductDesigns = entityManager
				.createQuery("FROM CfgTblProductDesign where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		
		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductDesigns;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductDesign> getProductDesignByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductDesign where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblProductDesign> ProductDesigns = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProductDesigns;
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
	public String addNewProductDesign(CfgTblProductDesign CfgTblProductDesign) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblProductDesign.setBlnStatus(true);
			CfgTblProductDesign.setBlIsDeleted(false);
			CfgTblProductDesign.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblProductDesign.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			
			CfgTblProductDesign.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
			 
			entityManager.persist(CfgTblProductDesign);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProductDesign(List<String> ProductDesignsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProductDesignId : ProductDesignsId) {
				CfgTblProductDesign ProductDesign = entityManager.find(CfgTblProductDesign.class, Integer.parseInt(serProductDesignId));
				if (ProductDesign != null) {
					ProductDesign.setBlIsDeleted(true);

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
	public String updateProductDesign(CfgTblProductDesign CfgTblProductDesign) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblProductDesign);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProductDesignNo(String type) {
		// int ProductDesignNo;
		String ProductDesignType = type;
		// String ProductDesignCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ProductDesignType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductDesignCode) from CfgTblProductDesign ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Dsgn-OPL-RW-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(12));

				ord_no = ord_no + 1;
				String code = "Dsgn-OPL-RW-1";
				if (ord_no < 10)
					code = "Dsgn-OPL-RW-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "Dsgn-OPL-RW-0" + ord_no;
				else
					code = "Dsgn-OPL-RW-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductDesignCode) from CfgTblProductDesign ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Dsgn-OPL-RW-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(12));

				ord_no1 = ord_no1 + 1;
				String code = "Dsgn-OPL-RW-1";
				if (ord_no1 < 10)
					code = "Dsgn-OPL-RW-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "Dsgn-OPL-RW-0" + ord_no1;
				else
					code = "Dsgn-OPL-RW-" + ord_no1;
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

	public String getProductDesignById(String ProductDesignId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductDesign where txtProductDesignCode='" + ProductDesignId + "'";

			List<CfgTblProductDesign> ProductDesign = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProductDesign.size() > 0) {
				return String.valueOf(ProductDesign.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblProductDesign> searchProductDesign(CfgTblProductDesign ProductDesign) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblProductDesign ProductDesign where 1=1 ";
	    if(ProductDesign.getTxtProductDesignCode() != null)
	    {
	    	query+=" and upper(ProductDesign.txtProductDesignCode) like"+" upper('"+ProductDesign.getTxtProductDesignCode()+"%')"+" ";
	    }
	    if(ProductDesign.getTxtProductDesignName() !=null){
	    	query+=" and upper(ProductDesign.txtProductDesignName) like"+" upper('"+ProductDesign.getTxtProductDesignName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(ProductDesign.getTxtEmail() !=null){
	    	query+=" and upper(ProductDesign.txtEmail) like"+" upper('"+ProductDesign.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ProductDesign.getSerProductDesignId() !=null){
	    	query+=" and ProductDesign.serProductDesignId ="+" "+ProductDesign.getSerProductDesignId()+""+"  ";
	    }
	  
	    query+=" order by ProductDesign.serProductDesignId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblProductDesign> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
