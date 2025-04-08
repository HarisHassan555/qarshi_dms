package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSOtoolDetailDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblToolDetail;

@Repository
public class slsTblSOToolDetailDAO implements ISlsTblSOtoolDetailDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(slsTblSOToolDetailDAO.class);

	public slsTblSOToolDetailDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblToolDetail> getAllToolDetail() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblToolDetail> ToolDetails = entityManager.createQuery("FROM SlsTblToolDetail where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ToolDetails;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblToolDetail> getActiveToolDetail() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblToolDetail> ToolDetails = entityManager
				.createQuery("FROM SlsTblToolDetail where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ToolDetails;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblToolDetail> getToolDetailByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblToolDetail where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblToolDetail> ToolDetails = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ToolDetails;
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
	public String addNewToolDetail(SlsTblToolDetail SlsTblToolDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblToolDetail.setBlIsDeleted(false);
			entityManager.persist(SlsTblToolDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteToolDetail(List<String> ToolDetailsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serToolDetailId : ToolDetailsId) {
				SlsTblToolDetail ToolDetail = entityManager.find(SlsTblToolDetail.class, Integer.parseInt(serToolDetailId));
				if (ToolDetail != null) {
					ToolDetail.setBlIsDeleted(true);
					ToolDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					ToolDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateToolDetail(SlsTblToolDetail SlsTblToolDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			SlsTblToolDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			SlsTblToolDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(SlsTblToolDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateToolDetailNo(String type) {
		// int ToolDetailNo;
		String ToolDetailType = type;
		// String ToolDetailCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ToolDetailType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtToolDetailCode) from SlsTblToolDetail ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtToolDetailCode) from SlsTblToolDetail ")
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

	public String getToolDetailById(String ToolDetailId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblToolDetail where txtToolDetailCode='" + ToolDetailId + "'";

			List<SlsTblToolDetail> ToolDetail = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ToolDetail.size() > 0) {
				return String.valueOf(ToolDetail.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblToolDetail> searchToolDetail(SlsTblToolDetail ToolDetail) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblToolDetail ToolDetail where 1=1 ";
	  
//	    if(ToolDetail.getTxtToolDetailName() !=null){
//	    	query+=" and upper(ToolDetail.txtToolDetailName) like"+" upper('"+ToolDetail.getTxtToolDetailName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(ToolDetail.getTxtEmail() !=null){
	    	query+=" and upper(ToolDetail.txtEmail) like"+" upper('"+ToolDetail.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ToolDetail.getSertoolDetailId() !=null){
	    	query+=" and ToolDetail.sertoolDetailId ="+" "+ToolDetail.getSertoolDetailId()+""+"  ";
	    }
	  
	    query+=" order by ToolDetail.serToolDetailId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblToolDetail> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
