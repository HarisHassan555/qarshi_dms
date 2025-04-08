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
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblPostServiceFUDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblPostServiceFU;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrder;

@Repository
public class SlsTblPostServiceFUDAO implements ISlsTblPostServiceFUDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ISlsTblWorkOrderDAO slsTblWorkOrderDAO;
	
	

	private static final Logger log = LoggerFactory.getLogger(SlsTblPostServiceFUDAO.class);

	public SlsTblPostServiceFUDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostServiceFU> getAllPostServiceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblPostServiceFU> PostServiceFUs = entityManager.createQuery("FROM SlsTblPostServiceFU where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PostServiceFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostServiceFU> getActivePostServiceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblPostServiceFU> PostServiceFUs = entityManager
				.createQuery("FROM SlsTblPostServiceFU where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return PostServiceFUs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblPostServiceFU> getPostServiceFUByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblPostServiceFU where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblPostServiceFU> PostServiceFUs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return PostServiceFUs;
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
	public String addNewPostServiceFU(SlsTblPostServiceFU SlsTblPostServiceFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			SlsTblPostServiceFU.setBlIsDeleted(false);
			SlsTblPostServiceFU.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblPostServiceFU.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(SlsTblPostServiceFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePostServiceFU(List<String> PostServiceFUsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPostServiceFUId : PostServiceFUsId) {
				SlsTblPostServiceFU PostServiceFU = entityManager.find(SlsTblPostServiceFU.class, Integer.parseInt(serPostServiceFUId));
				if (PostServiceFU != null) {
					PostServiceFU.setBlIsDeleted(true);

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
	public String updatePostServiceFU(SlsTblPostServiceFU SlsTblPostServiceFU) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblPostServiceFU);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePostServiceFUNo(String type) {
		// int PostServiceFUNo;
		String PostServiceFUType = type;
		// String PostServiceFUCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (PostServiceFUType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPostServiceFUCode) from SlsTblPostServiceFU ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtPostServiceFUCode) from SlsTblPostServiceFU ")
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

	public String getPostServiceFUById(String PostServiceFUId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblPostServiceFU where txtPostServiceFUCode='" + PostServiceFUId + "'";

			List<SlsTblPostServiceFU> PostServiceFU = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (PostServiceFU.size() > 0) {
				return String.valueOf(PostServiceFU.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	public SlsTblPostServiceFU getPostServiceFUByPK(int PostServiceFUId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblPostServiceFU where serPOSTServiceFUId='" + PostServiceFUId + "'";

			SlsTblPostServiceFU PostServiceFU = (SlsTblPostServiceFU)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			
			if(PostServiceFU!=null)
			return PostServiceFU;
			else
				return new SlsTblPostServiceFU();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblPostServiceFU> searchPostServiceFU(SlsTblPostServiceFU PostServiceFU) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblPostServiceFU PostServiceFU where 1=1 ";
//	    if(PostServiceFU.getTxtPostServiceFUCode() != null)
//	    {
//	    	query+=" and upper(PostServiceFU.txtPostServiceFUCode) like"+" upper('"+PostServiceFU.getTxtPostServiceFUCode()+"%')"+" ";
//	    }
//	    if(PostServiceFU.getTxtPostServiceFUName() !=null){
//	    	query+=" and upper(PostServiceFU.txtPostServiceFUName) like"+" upper('"+PostServiceFU.getTxtPostServiceFUName()+"%')"+"  ";
//	    }
	    
	  
	    
	    /*if(PostServiceFU.getTxtEmail() !=null){
	    	query+=" and upper(PostServiceFU.txtEmail) like"+" upper('"+PostServiceFU.getTxtEmail()+"')"+"  ";
	    }*/
	    
//	    if(PostServiceFU.getSerPostServiceFUId() !=null){
//	    	query+=" and PostServiceFU.serPostServiceFUId ="+" "+PostServiceFU.getSerPostServiceFUId()+""+"  ";
//	    }
	  
	    query+=" order by PostServiceFU.serPostServiceFUId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblPostServiceFU> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public String serivcetoaddPostServiceFUFromthread() {
			
		List<SlsTblWorkOrder> WorkOrders = slsTblWorkOrderDAO.getWorkOrderForPostServiceFU();
		if(WorkOrders !=null && WorkOrders.size() >0)
		{
			SlsTblPostServiceFU entity;
			for(SlsTblWorkOrder wo : WorkOrders)
			{
				entity= new SlsTblPostServiceFU();
				entity.setSlsTblWorkOrder(wo);
				entity.setSlsTblSoVehicleDetail(wo.getSlsTblSoVehicleDetail());
				entity.setDteDate(new Date());
				entity.setCfgTblProduct(wo.getCfgTblProduct());
				entity.setCfgTblCustomer(wo.getCfgTblCustomer());
				entity.setTxtStatus("Pending");
				addNewPostServiceFU(entity);
				wo.setBlIsPostServiceFollowUp(true);
				slsTblWorkOrderDAO.updateWorkOrder(wo);
			}
		}
		
		
	  
	 
	    return "Success";
	}
}
