package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblSourceDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSource;

@Repository
public class CfgTblSourceDAO implements ICfgTblSourceDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblSourceDAO.class);

	public CfgTblSourceDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSource> getAllSource() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSource> Sources = entityManager.createQuery("FROM CfgTblSource where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Sources;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSource> getActiveSource() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSource> Sources = entityManager
				.createQuery("FROM CfgTblSource where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Sources;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSource> getSourceByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSource where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblSource> Sources = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Sources;
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
	public String addNewSource(CfgTblSource CfgTblSource) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblSource.setBlnStatus(true);
			CfgTblSource.setBlIsDeleted(false);
			CfgTblSource.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblSource.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblSource);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteSource(List<String> SourcesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serSourceId : SourcesId) {
				CfgTblSource Source = entityManager.find(CfgTblSource.class, Integer.parseInt(serSourceId));
				if (Source != null) {
					Source.setBlIsDeleted(true);

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
	public String updateSource(CfgTblSource CfgTblSource) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblSource);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateSourceNo(String type) {
		// int SourceNo;
		String SourceType = type;
		// String SourceCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (SourceType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtSourceCode) from CfgTblSource ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtSourceCode) from CfgTblSource ")
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

	public String getSourceById(String SourceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSource where txtSourceCode='" + SourceId + "'";

			List<CfgTblSource> Source = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Source.size() > 0) {
				return String.valueOf(Source.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblSource> searchSource(CfgTblSource Source) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblSource Source where 1=1 ";
	    if(Source.getTxtSourceCode() != null)
	    {
	    	query+=" and upper(Source.txtSourceCode) like"+" upper('"+Source.getTxtSourceCode()+"%')"+" ";
	    }
	    if(Source.getTxtSourceName() !=null){
	    	query+=" and upper(Source.txtSourceName) like"+" upper('"+Source.getTxtSourceName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Source.getTxtEmail() !=null){
	    	query+=" and upper(Source.txtEmail) like"+" upper('"+Source.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Source.getSerSourceId() !=null){
	    	query+=" and Source.serSourceId ="+" "+Source.getSerSourceId()+""+"  ";
	    }
	  
	    query+=" order by Source.serSourceId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblSource> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
