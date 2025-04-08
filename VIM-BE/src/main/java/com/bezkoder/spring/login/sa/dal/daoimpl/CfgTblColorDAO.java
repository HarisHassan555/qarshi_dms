package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblColorDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblColor;

@Repository
public class CfgTblColorDAO implements ICfgTblColorDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblColorDAO.class);

	public CfgTblColorDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblColor> getAllColor() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblColor> Colors = entityManager.createQuery("FROM CfgTblColor where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Colors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblColor> getActiveColor() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblColor> Colors = entityManager
				.createQuery("FROM CfgTblColor where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Colors;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblColor> getColorByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblColor where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblColor> Colors = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Colors;
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
	public String addNewColor(CfgTblColor CfgTblColor) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblColor.setBlnStatus(true);
			CfgTblColor.setBlIsDeleted(false);
			entityManager.persist(CfgTblColor);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteColor(List<String> ColorsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serColorId : ColorsId) {
				CfgTblColor Color = entityManager.find(CfgTblColor.class, Integer.parseInt(serColorId));
				if (Color != null) {
					Color.setBlIsDeleted(true);
					Color.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Color.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateColor(CfgTblColor CfgTblColor) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblColor.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblColor.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblColor);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateColorNo(String type) {
		// int ColorNo;
		String ColorType = type;
		// String ColorCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ColorType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtColorCode) from CfgTblColor ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtColorCode) from CfgTblColor ")
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

	public String getColorById(String ColorId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblColor where txtColorCode='" + ColorId + "'";

			List<CfgTblColor> Color = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Color.size() > 0) {
				return String.valueOf(Color.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblColor> searchColor(CfgTblColor Color) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblColor Color where 1=1 ";
	  
	    if(Color.getTxtColorName() !=null){
	    	query+=" and upper(Color.txtColorName) like"+" upper('"+Color.getTxtColorName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Color.getTxtEmail() !=null){
	    	query+=" and upper(Color.txtEmail) like"+" upper('"+Color.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Color.getSerColorId() !=null){
	    	query+=" and Color.serColorId ="+" "+Color.getSerColorId()+""+"  ";
	    }
	  
	    query+=" order by Color.serColorId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblColor> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
