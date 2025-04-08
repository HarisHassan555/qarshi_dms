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
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblMaintinanceFUDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblMaintinanceFU;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrder;

@Repository
public class SlsTblMaintinanceFUDAO implements ISlsTblMaintinanceFUDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblMaintinanceFUDAO.class);

	public SlsTblMaintinanceFUDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblMaintinanceFU> getAllMaintinanceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblMaintinanceFU> MaintinanceFUs = entityManager.createQuery("FROM SlsTblMaintinanceFU where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return MaintinanceFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblMaintinanceFU> getActiveMaintinanceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblMaintinanceFU> MaintinanceFUs = entityManager
				.createQuery("FROM SlsTblMaintinanceFU where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return MaintinanceFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblMaintinanceFU> getMaintinanceFUByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblMaintinanceFU where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblMaintinanceFU> MaintinanceFUs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return MaintinanceFUs;
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
	public String addNewMaintinanceFU(SlsTblMaintinanceFU SlsTblMaintinanceFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblMaintinanceFU.setBlIsDeleted(false);
			SlsTblMaintinanceFU.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblMaintinanceFU.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblMaintinanceFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteMaintinanceFU(List<String> MaintinanceFUsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serMaintinanceFUId : MaintinanceFUsId) {
				SlsTblMaintinanceFU MaintinanceFU = entityManager.find(SlsTblMaintinanceFU.class, Integer.parseInt(serMaintinanceFUId));
				if (MaintinanceFU != null) {
					MaintinanceFU.setBlIsDeleted(true);

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
	public String updateMaintinanceFU(SlsTblMaintinanceFU SlsTblMaintinanceFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblMaintinanceFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateMaintinanceFUNo(String type) {
		// int MaintinanceFUNo;
		String MaintinanceFUType = type;
		// String MaintinanceFUCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (MaintinanceFUType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtMaintinanceFUCode) from SlsTblMaintinanceFU ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtMaintinanceFUCode) from SlsTblMaintinanceFU ")
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

	public String getMaintinanceFUById(String MaintinanceFUId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblMaintinanceFU where txtMaintinanceFUCode='" + MaintinanceFUId + "'";

			List<SlsTblMaintinanceFU> MaintinanceFU = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (MaintinanceFU.size() > 0) {
				return String.valueOf(MaintinanceFU.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblMaintinanceFU> searchMaintinanceFU(SlsTblMaintinanceFU MaintinanceFU) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblMaintinanceFU MaintinanceFU where 1=1 ";
//	    if(MaintinanceFU.getTxtMaintinanceFUCode() != null)
//	    {
//	    	query+=" and upper(MaintinanceFU.txtMaintinanceFUCode) like"+" upper('"+MaintinanceFU.getTxtMaintinanceFUCode()+"%')"+" ";
//	    }
//	    if(MaintinanceFU.getTxtMaintinanceFUName() !=null){
//	    	query+=" and upper(MaintinanceFU.txtMaintinanceFUName) like"+" upper('"+MaintinanceFU.getTxtMaintinanceFUName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(MaintinanceFU.getTxtEmail() !=null){
	    	query+=" and upper(MaintinanceFU.txtEmail) like"+" upper('"+MaintinanceFU.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(MaintinanceFU.getSerMaintinanceFUId() !=null){
//	    	query+=" and MaintinanceFU.serMaintinanceFUId ="+" "+MaintinanceFU.getSerMaintinanceFUId()+""+"  ";
//	    }
	  
	    query+=" order by MaintinanceFU.serMaintinanceFUId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblMaintinanceFU> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public String serivcetoaddMaintinanceFUFromthread() {
			
		List<SlsTblWorkOrder> WorkOrders = slsTblWorkOrderDAO.getWorkOrderForMaintinanceFU();

		if(WorkOrders !=null && WorkOrders.size() >0)
		{
			SlsTblMaintinanceFU entity;
			for(SlsTblWorkOrder wo : WorkOrders)
			{
				entity= new SlsTblMaintinanceFU();
				entity.setSlsTblWorkOrder(wo);
				entity.setSlsTblSoVehicleDetail(wo.getSlsTblSoVehicleDetail());
				entity.setDteDate(new Date());
				entity.setCfgTblProduct(wo.getCfgTblProduct());
				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
				entity.setTxtStatus("Pending");
				addNewMaintinanceFU(entity);
				wo.setBlIsMaintinanceFollowUp(true);
				slsTblWorkOrderDAO.updateWorkOrder(wo);
			}
		}
		
		
	  
	 
	    return "Success";
	}
}
