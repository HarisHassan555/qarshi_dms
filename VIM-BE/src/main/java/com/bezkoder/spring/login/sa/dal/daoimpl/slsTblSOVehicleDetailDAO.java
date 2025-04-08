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
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSOVehicleDetailDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoVehicleDetail;

@Repository
public class slsTblSOVehicleDetailDAO implements ISlsTblSOVehicleDetailDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(slsTblSOVehicleDetailDAO.class);

	public slsTblSOVehicleDetailDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSoVehicleDetail> getAllVehicleDetail() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSoVehicleDetail> VehicleDetails = entityManager.createQuery("FROM SlsTblSoVehicleDetail where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return VehicleDetails;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSoVehicleDetail> getActiveVehicleDetail() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSoVehicleDetail> VehicleDetails = entityManager
				.createQuery("FROM SlsTblSoVehicleDetail where blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return VehicleDetails;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSoVehicleDetail> getVehicleDetailByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblSoVehicleDetail where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblSoVehicleDetail> VehicleDetails = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return VehicleDetails;
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
	public String addNewVehicleDetail(SlsTblSoVehicleDetail VehicleDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			VehicleDetail.setDteDate(new Date());
			VehicleDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			VehicleDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			VehicleDetail.setBlIsDeleted(false);
			entityManager.persist(VehicleDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	
	
	@Override
	public SlsTblSoVehicleDetail addNewVehicleDetailRVC(SlsTblSoVehicleDetail VehicleDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			VehicleDetail.setDteDate(new Date());
			VehicleDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			VehicleDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			VehicleDetail.setBlIsDeleted(false);
			entityManager.persist(VehicleDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return VehicleDetail;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}


	@Override
	public String deleteVehicleDetail(List<String> VehicleDetailsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serVehicleDetailId : VehicleDetailsId) {
				SlsTblSoVehicleDetail VehicleDetail = entityManager.find(SlsTblSoVehicleDetail.class, Integer.parseInt(serVehicleDetailId));
				if (VehicleDetail != null) {
					VehicleDetail.setBlIsDeleted(true);
					VehicleDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					VehicleDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateVehicleDetail(SlsTblSoVehicleDetail SlsTblSoVehicleDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			SlsTblSoVehicleDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			SlsTblSoVehicleDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(SlsTblSoVehicleDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateVehicleDetailNo(String type) {
		// int VehicleDetailNo;
		String VehicleDetailType = type;
		// String VehicleDetailCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (VehicleDetailType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtVehicleDetailCode) from SlsTblSoVehicleDetail ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtVehicleDetailCode) from SlsTblSoVehicleDetail ")
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

	public String getVehicleDetailById(String VehicleDetailId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblSoVehicleDetail where txtChassisNo='" + VehicleDetailId + "'";

			List<SlsTblSoVehicleDetail> VehicleDetail = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (VehicleDetail.size() > 0) {
				return String.valueOf(VehicleDetail.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	public SlsTblSoVehicleDetail getVehicleDetailByChassisno(String ChassisNo) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblSoVehicleDetail where txtChassisNo='" + ChassisNo + "'";

			List<SlsTblSoVehicleDetail> VehicleDetail = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (VehicleDetail.size() > 0) {
				return VehicleDetail.get(0);
			}
			return null;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	
	@Override
	public List<SlsTblSoVehicleDetail> searchVehicleDetail(SlsTblSoVehicleDetail VehicleDetail) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblSoVehicleDetail VehicleDetail where 1=1 ";
	  
	    if(VehicleDetail.getTxtChassisNo() !=null){
	    	query+=" and upper(VehicleDetail.txtChassisNo) like"+" upper('"+VehicleDetail.getTxtChassisNo()+"%')"+"  ";
	    }
	    
	    if(VehicleDetail.getTxtEngineNo() !=null){
	    	query+=" and upper(VehicleDetail.txtChassisNo) like"+" upper('"+VehicleDetail.getTxtEngineNo()+"%')"+"  ";
	    }
	    
	    if(VehicleDetail.getTxtRegistrationNo() !=null){
	    	query+=" and upper(VehicleDetail.txtRegistrationNo) like"+" upper('"+VehicleDetail.getTxtRegistrationNo()+"%')"+"  ";
	    }
	    
	    if(VehicleDetail.getCfgTblCustomer() !=null && VehicleDetail.getCfgTblCustomer().getSerCustomerId() > 0){
	    	query+=" and upper(VehicleDetail.cfgTblCustomer.serCustomerId) ="+" upper("+VehicleDetail.getCfgTblCustomer().getSerCustomerId()+")"+"  ";
	    }
	  
	    
	    /*if(VehicleDetail.getTxtEmail() !=null){
	    	query+=" and upper(VehicleDetail.txtEmail) like"+" upper('"+VehicleDetail.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(VehicleDetail.getSerSoVehicleDetailId() !=null){
	    	query+=" and VehicleDetail.serSoVehicleDetailId ="+" "+VehicleDetail.getSerSoVehicleDetailId()+""+"  ";
	    }
	  
	    query+=" order by VehicleDetail.serSoVehicleDetailId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblSoVehicleDetail> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSoVehicleDetail> getWorkOrderForFFSFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSoVehicleDetail> WorkOrders = entityManager.createQuery("FROM SlsTblSoVehicleDetail  where blIsDeleted=FALSE and (blIsFFSFollowUp = false or blIsFFSFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSoVehicleDetail> getWorkOrderForPostSalesFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSoVehicleDetail> WorkOrders = entityManager.createQuery("FROM SlsTblSoVehicleDetail where blIsDeleted=FALSE and (blIsPostSalesFollowUp = false or blIsPostSalesFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}
	
}
