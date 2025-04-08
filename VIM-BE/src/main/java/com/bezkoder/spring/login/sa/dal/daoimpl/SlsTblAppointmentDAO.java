package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblAppointmentDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblAppointment;

@Repository
public class SlsTblAppointmentDAO implements ISlsTblAppointmentDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblAppointmentDAO.class);

	public SlsTblAppointmentDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblAppointment> getAllAppointment() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblAppointment> Appointments = entityManager.createQuery("FROM SlsTblAppointment where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Appointments;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblAppointment> getActiveAppointment() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblAppointment> Appointments = entityManager
				.createQuery("FROM SlsTblAppointment where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Appointments;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblAppointment> getAppointmentByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblAppointment where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblAppointment> Appointments = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Appointments;
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
	public String addNewAppointment(SlsTblAppointment SlsTblAppointment) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblAppointment.setBlIsDeleted(false);
			SlsTblAppointment.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblAppointment.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblAppointment);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteAppointment(List<String> AppointmentsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serAppointmentId : AppointmentsId) {
				SlsTblAppointment Appointment = entityManager.find(SlsTblAppointment.class, Integer.parseInt(serAppointmentId));
				if (Appointment != null) {
					Appointment.setBlIsDeleted(true);

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
	public String updateAppointment(SlsTblAppointment SlsTblAppointment) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblAppointment);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateAppointmentNo(String type) {
		// int AppointmentNo;
		String AppointmentType = type;
		// String AppointmentCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (AppointmentType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtAppointmentCode) from SlsTblAppointment ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtAppointmentCode) from SlsTblAppointment ")
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

	public String getAppointmentById(String AppointmentId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblAppointment where txtAppointmentCode='" + AppointmentId + "'";

			List<SlsTblAppointment> Appointment = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Appointment.size() > 0) {
				return String.valueOf(Appointment.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblAppointment> searchAppointment(SlsTblAppointment Appointment) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblAppointment Appointment where 1=1 ";
//	    if(Appointment.getTxtAppointmentCode() != null)
//	    {
//	    	query+=" and upper(Appointment.txtAppointmentCode) like"+" upper('"+Appointment.getTxtAppointmentCode()+"%')"+" ";
//	    }
//	    if(Appointment.getTxtAppointmentName() !=null){
//	    	query+=" and upper(Appointment.txtAppointmentName) like"+" upper('"+Appointment.getTxtAppointmentName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(Appointment.getTxtEmail() !=null){
	    	query+=" and upper(Appointment.txtEmail) like"+" upper('"+Appointment.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(Appointment.getSerAppointmentId() !=null){
//	    	query+=" and Appointment.serAppointmentId ="+" "+Appointment.getSerAppointmentId()+""+"  ";
//	    }
	  
	    query+=" order by Appointment.serAppointmentId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblAppointment> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public String serivcetoaddAppointmentFromthread() {
			
//		List<SlsTblWorkOrder> WorkOrders = slsTblWorkOrderDAO.getWorkOrderForAppointment();
//
//		if(WorkOrders !=null && WorkOrders.size() >0)
//		{
//			SlsTblAppointment entity;
//			for(SlsTblWorkOrder wo : WorkOrders)
//			{
//				entity= new SlsTblAppointment();
//				entity.setSlsTblWorkOrder(wo);
//				entity.setSlsTblSoVehicleDetail(wo.getSlsTblSoVehicleDetail());
//				entity.setDteDate(new Date());
//				entity.setCfgTblProduct(wo.getCfgTblProduct());
//				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
//				entity.setTxtStatus("Pending");
//				addNewAppointment(entity);
//				wo.setBlIsMaintinanceFollowUp(true);
//				slsTblWorkOrderDAO.updateWorkOrder(wo);
//			}
//		}
//		
		
	  
	 
	    return "Success";
	}
}
