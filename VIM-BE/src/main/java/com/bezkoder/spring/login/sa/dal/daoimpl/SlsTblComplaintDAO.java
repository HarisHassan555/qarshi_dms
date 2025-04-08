package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblComplaintDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblPostServiceFUDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblComplaint;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblPostServiceFU;

@Repository
public class SlsTblComplaintDAO implements ISlsTblComplaintDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;
	
	@Autowired
	private ISlsTblPostServiceFUDAO slsTblPostServiceFUDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblComplaintDAO.class);

	public SlsTblComplaintDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblComplaint> getAllComplaint() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblComplaint> Complaints = entityManager.createQuery("FROM SlsTblComplaint tt where tt.blIsDeleted=FALSE and upper(tt.txtType) like upper('Complaint')")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Complaints;
	}
	
	

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblComplaint> getActiveComplaint() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
//		List<SlsTblComplaint> Complaints = entityManager
//				.createQuery("FROM SlsTblComplaint where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();
		
		List<SlsTblComplaint> Complaints = entityManager.createQuery("FROM SlsTblComplaint tt where tt.blIsDeleted=FALSE and upper(tt.txtType) like upper('Inquiry')")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Complaints;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblComplaint> getComplaintByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblComplaint where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblComplaint> Complaints = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Complaints;
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
	public String addNewComplaint(SlsTblComplaint SlsTblComplaint) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblComplaint.setBlIsDeleted(false);
			SlsTblComplaint.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblComplaint.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblComplaint);
			entityManager.getTransaction().commit();
			entityManager.close();
//			slsTblPostServiceFUDAO		
			if(SlsTblComplaint.getSlsTblPostServiceFU() != null && SlsTblComplaint.getSlsTblPostServiceFU().getSerPOSTServiceFUId() > 0)
			{
				EntityManager entityManager2 = getEntityManager();
				entityManager2.getTransaction().begin();
				SlsTblPostServiceFU obj= 	slsTblPostServiceFUDAO.getPostServiceFUByPK(SlsTblComplaint.getSlsTblPostServiceFU().getSerPOSTServiceFUId());
				if(obj.getTxtcomplaint()!=null && obj.getTxtcomplaint().trim().length()>0)
					obj.setTxtcomplaint(obj.getTxtcomplaint()+", "+SlsTblComplaint.getSerComplaintId()+"");
				else
					obj.setTxtcomplaint(SlsTblComplaint.getSerComplaintId()+"");
				
				entityManager2.merge(obj);
				entityManager2.getTransaction().commit();
				entityManager2.close();
			}
			
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
			return "Failure";
		}
	}

	@Override
	public String deleteComplaint(List<String> ComplaintsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serComplaintId : ComplaintsId) {
				SlsTblComplaint Complaint = entityManager.find(SlsTblComplaint.class, Integer.parseInt(serComplaintId));
				if (Complaint != null) {
					Complaint.setBlIsDeleted(true);

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
	public String updateComplaint(SlsTblComplaint SlsTblComplaint) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblComplaint);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateComplaintNo(String type) {
		// int ComplaintNo;
		String ComplaintType = type;
		// String ComplaintCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
	
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtComplaintCode) from SlsTblComplaint ")
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


	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getComplaintById(String ComplaintId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblComplaint where txtComplaintCode='" + ComplaintId + "'";

			List<SlsTblComplaint> Complaint = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Complaint.size() > 0) {
				return String.valueOf(Complaint.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblComplaint> searchComplaint(SlsTblComplaint Complaint) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblComplaint Complaint where 1=1 ";
//	    if(Complaint.getTxtComplaintCode() != null)
//	    {
//	    	query+=" and upper(Complaint.txtComplaintCode) like"+" upper('"+Complaint.getTxtComplaintCode()+"%')"+" ";
//	    }
	    if(Complaint.getTxtComplainttype() !=null && Complaint.getTxtComplainttype().equalsIgnoreCase("Inquiry")){
	    	query+=" and upper(Complaint.txtType) like"+" upper('"+Complaint.getTxtComplainttype()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Complaint.getTxtEmail() !=null){
	    	query+=" and upper(Complaint.txtEmail) like"+" upper('"+Complaint.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(Complaint.getSerComplaintId() !=null){
//	    	query+=" and Complaint.serComplaintId ="+" "+Complaint.getSerComplaintId()+""+"  ";
//	    }
	  
	    query+=" order by Complaint.serComplaintId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblComplaint> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public String serivcetoaddComplaintFromthread() {
			
//		List<SlsTblWorkOrder> WorkOrders = slsTblWorkOrderDAO.getWorkOrderForComplaint();
//
//		if(WorkOrders !=null && WorkOrders.size() >0)
//		{
//			SlsTblComplaint entity;
//			for(SlsTblWorkOrder wo : WorkOrders)
//			{
//				entity= new SlsTblComplaint();
//				entity.setSlsTblWorkOrder(wo);
//				entity.setSlsTblSoVehicleDetail(wo.getSlsTblSoVehicleDetail());
//				entity.setDteDate(new Date());
//				entity.setCfgTblProduct(wo.getCfgTblProduct());
//				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
//				entity.setTxtStatus("Pending");
//				addNewComplaint(entity);
//				wo.setBlIsMaintinanceFollowUp(true);
//				slsTblWorkOrderDAO.updateWorkOrder(wo);
//			}
//		}
//		
		
	  
	 
	    return "Success";
	}
}
