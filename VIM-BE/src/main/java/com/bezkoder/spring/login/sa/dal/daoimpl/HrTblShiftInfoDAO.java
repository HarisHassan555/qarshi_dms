package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblShiftInfoDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblShiftInfo;

@Repository
public class HrTblShiftInfoDAO implements IHrTblShiftInfoDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(HrTblShiftInfoDAO.class);

	public HrTblShiftInfoDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblShiftInfo> getAllShiftInfos() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblShiftInfo> ShiftInfos = entityManager.createQuery("FROM HrTblShiftInfo where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ShiftInfos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblShiftInfo> getActiveShiftInfos() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblShiftInfo> ShiftInfos = entityManager
				.createQuery("FROM HrTblShiftInfo where blStatus=true  and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ShiftInfos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblShiftInfo> getShiftInfoByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblShiftInfo where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<HrTblShiftInfo> ShiftInfos = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ShiftInfos;
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
	public String addNewShiftInfo(HrTblShiftInfo HrTblShiftInfo) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblShiftInfo.setBlnStatus(true);
			HrTblShiftInfo.setBlIsDeleted(false);
			entityManager.persist(HrTblShiftInfo);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteShiftInfos(List<String> ShiftInfosId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serShiftInfoId : ShiftInfosId) {
				HrTblShiftInfo ShiftInfo = entityManager.find(HrTblShiftInfo.class, Integer.parseInt(serShiftInfoId));
				if (ShiftInfo != null) {
					ShiftInfo.setBlIsDeleted(true);
				
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
	public String updateShiftInfo(HrTblShiftInfo HrTblShiftInfo) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
		
			entityManager.merge(HrTblShiftInfo);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateShiftInfoNo(String type) {
		// int ShiftInfoNo;
		String ShiftInfoType = type;
		// String ShiftInfoCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ShiftInfoType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtShiftInfoCode) from HrTblShiftInfo ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtShiftInfoCode) from HrTblShiftInfo ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "COM-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "COM-1";
				if (ord_no1 < 10)
					code = "COM-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "COM-0" + ord_no1;
				else
					code = "COM-" + ord_no1;
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

	public String getShiftInfoById(String ShiftInfoId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblShiftInfo where txtShiftInfoCode='" + ShiftInfoId + "'";

			List<HrTblShiftInfo> ShiftInfo = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ShiftInfo.size() > 0) {
				return String.valueOf(ShiftInfo.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<HrTblShiftInfo> searchShiftInfo(HrTblShiftInfo ShiftInfo) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from HrTblShiftInfo ShiftInfo where 1=1 ";
	  
	    
	  
	    
	    /*if(ShiftInfo.getTxtEmail() !=null){
	    	query+=" and upper(ShiftInfo.txtEmail) like"+" upper('"+ShiftInfo.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ShiftInfo.getSerShiftInfoId() !=null){
	    	query+=" and ShiftInfo.serShiftInfoId ="+" "+ShiftInfo.getSerShiftInfoId()+""+"  ";
	    }
	  
	    query+=" order by ShiftInfo.serShiftInfoId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<HrTblShiftInfo> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
