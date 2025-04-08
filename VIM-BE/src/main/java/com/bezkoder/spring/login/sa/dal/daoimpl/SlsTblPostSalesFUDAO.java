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
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblPostSalesFUDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSOVehicleDetailDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblPostSalesFU;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoVehicleDetail;

@Repository
public class SlsTblPostSalesFUDAO implements ISlsTblPostSalesFUDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;
	
	
	@Autowired
	ISlsTblSOVehicleDetailDAO slsTblSOVehicleDetailDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblPostSalesFUDAO.class);

	public SlsTblPostSalesFUDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostSalesFU> getAllPostSalesFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblPostSalesFU> PostSalesFUs = entityManager.createQuery("FROM SlsTblPostSalesFU where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PostSalesFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostSalesFU> getActivePostSalesFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblPostSalesFU> PostSalesFUs = entityManager
				.createQuery("FROM SlsTblPostSalesFU where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PostSalesFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostSalesFU> getPostSalesFUByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblPostSalesFU where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblPostSalesFU> PostSalesFUs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return PostSalesFUs;
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
	public String addNewPostSalesFU(SlsTblPostSalesFU SlsTblPostSalesFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblPostSalesFU.setBlIsDeleted(false);
			SlsTblPostSalesFU.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblPostSalesFU.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblPostSalesFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePostSalesFU(List<String> PostSalesFUsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPostSalesFUId : PostSalesFUsId) {
				SlsTblPostSalesFU PostSalesFU = entityManager.find(SlsTblPostSalesFU.class, Integer.parseInt(serPostSalesFUId));
				if (PostSalesFU != null) {
					PostSalesFU.setBlIsDeleted(true);

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
	public String updatePostSalesFU(SlsTblPostSalesFU SlsTblPostSalesFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblPostSalesFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePostSalesFUNo(String type) {
		// int PostSalesFUNo;
		String PostSalesFUType = type;
		// String PostSalesFUCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (PostSalesFUType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPostSalesFUCode) from SlsTblPostSalesFU ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPostSalesFUCode) from SlsTblPostSalesFU ")
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

	public String getPostSalesFUById(String PostSalesFUId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblPostSalesFU where txtPostSalesFUCode='" + PostSalesFUId + "'";

			List<SlsTblPostSalesFU> PostSalesFU = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (PostSalesFU.size() > 0) {
				return String.valueOf(PostSalesFU.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblPostSalesFU> searchPostSalesFU(SlsTblPostSalesFU PostSalesFU) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblPostSalesFU PostSalesFU where 1=1 ";
//	    if(PostSalesFU.getTxtPostSalesFUCode() != null)
//	    {
//	    	query+=" and upper(PostSalesFU.txtPostSalesFUCode) like"+" upper('"+PostSalesFU.getTxtPostSalesFUCode()+"%')"+" ";
//	    }
//	    if(PostSalesFU.getTxtPostSalesFUName() !=null){
//	    	query+=" and upper(PostSalesFU.txtPostSalesFUName) like"+" upper('"+PostSalesFU.getTxtPostSalesFUName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(PostSalesFU.getTxtEmail() !=null){
	    	query+=" and upper(PostSalesFU.txtEmail) like"+" upper('"+PostSalesFU.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(PostSalesFU.getSerPostSalesFUId() !=null){
//	    	query+=" and PostSalesFU.serPostSalesFUId ="+" "+PostSalesFU.getSerPostSalesFUId()+""+"  ";
//	    }
	  
	    query+=" order by PostSalesFU.serPostSalesFUId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblPostSalesFU> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	

	@Override
	public String serivcetoaddPostSalesFUFromthread() {
	
		List<SlsTblSoVehicleDetail> vehicleDetails = slsTblSOVehicleDetailDAO.getWorkOrderForPostSalesFU();
		if(vehicleDetails !=null && vehicleDetails.size() >0)
		{
			SlsTblPostSalesFU entity;
			for(SlsTblSoVehicleDetail wo : vehicleDetails)
			{
				entity= new SlsTblPostSalesFU();
			
				entity.setSlsTblSoVehicleDetail(wo);
				entity.setDteDate(new Date());
				entity.setCfgTblProduct(wo.getCfgTblProduct());
				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
				entity.setTxtStatus("Pending");
				addNewPostSalesFU(entity);
				wo.setBlIsPostSalesFollowUp(true);
				slsTblSOVehicleDetailDAO.updateVehicleDetail(wo);
			}
		}
		
		
	  
	 
	    return "Success";
	}
}
