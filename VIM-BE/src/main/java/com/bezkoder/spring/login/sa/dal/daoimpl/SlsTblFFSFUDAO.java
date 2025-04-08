package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblFFSFUDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSOVehicleDetailDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblFFSFU;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoVehicleDetail;

@Repository
public class SlsTblFFSFUDAO implements ISlsTblFFSFUDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;
	
	@Autowired
	ISlsTblSOVehicleDetailDAO slsTblSOVehicleDetailDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblFFSFUDAO.class);

	public SlsTblFFSFUDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblFFSFU> getAllFFSFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblFFSFU> FFSFUs = entityManager.createQuery("FROM SlsTblFFSFU where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return FFSFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblFFSFU> getActiveFFSFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblFFSFU> FFSFUs = entityManager
				.createQuery("FROM SlsTblFFSFU where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return FFSFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblFFSFU> getFFSFUByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblFFSFU where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblFFSFU> FFSFUs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return FFSFUs;
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
	public String addNewFFSFU(SlsTblFFSFU SlsTblFFSFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblFFSFU.setBlIsDeleted(false);
			SlsTblFFSFU.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblFFSFU.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblFFSFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteFFSFU(List<String> FFSFUsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serFFSFUId : FFSFUsId) {
				SlsTblFFSFU FFSFU = entityManager.find(SlsTblFFSFU.class, Integer.parseInt(serFFSFUId));
				if (FFSFU != null) {
					FFSFU.setBlIsDeleted(true);

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
	public String updateFFSFU(SlsTblFFSFU SlsTblFFSFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblFFSFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateFFSFUNo(String type) {
		// int FFSFUNo;
		String FFSFUType = type;
		// String FFSFUCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (FFSFUType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtFFSFUCode) from SlsTblFFSFU ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtFFSFUCode) from SlsTblFFSFU ")
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

	public String getFFSFUById(String FFSFUId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblFFSFU where txtFFSFUCode='" + FFSFUId + "'";

			List<SlsTblFFSFU> FFSFU = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (FFSFU.size() > 0) {
				return String.valueOf(FFSFU.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblFFSFU> searchFFSFU(SlsTblFFSFU FFSFU) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblFFSFU FFSFU where 1=1 ";
//	    if(FFSFU.getTxtFFSFUCode() != null)
//	    {
//	    	query+=" and upper(FFSFU.txtFFSFUCode) like"+" upper('"+FFSFU.getTxtFFSFUCode()+"%')"+" ";
//	    }
//	    if(FFSFU.getTxtFFSFUName() !=null){
//	    	query+=" and upper(FFSFU.txtFFSFUName) like"+" upper('"+FFSFU.getTxtFFSFUName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(FFSFU.getTxtEmail() !=null){
	    	query+=" and upper(FFSFU.txtEmail) like"+" upper('"+FFSFU.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(FFSFU.getSerFFSFUId() !=null){
//	    	query+=" and FFSFU.serFFSFUId ="+" "+FFSFU.getSerFFSFUId()+""+"  ";
//	    }
	  
	    query+=" order by FFSFU.serFFSFUId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblFFSFU> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public String serivcetoaddFFSFUFromthread() {

		List<SlsTblSoVehicleDetail> vehicleDetails=slsTblSOVehicleDetailDAO.getWorkOrderForFFSFU();
		if(vehicleDetails !=null && vehicleDetails.size() >0)
		{
			SlsTblFFSFU entity;
			for(SlsTblSoVehicleDetail wo : vehicleDetails)
			{
				entity= new SlsTblFFSFU();
				entity.setSlsTblSoVehicleDetail(wo);
		
				entity.setDteDate(new Date());
				entity.setCfgTblProduct(wo.getCfgTblProduct());
				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
				entity.setTxtStatus("Pending");
				addNewFFSFU(entity);
				wo.setBlIsFFSFollowUp(true);
				slsTblSOVehicleDetailDAO.updateVehicleDetail(wo);
			}
		}
		
		
	  
	 
	    return "Success";
	}
}
