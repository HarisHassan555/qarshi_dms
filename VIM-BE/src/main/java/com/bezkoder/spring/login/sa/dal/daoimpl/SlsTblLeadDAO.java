package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblLeadDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblLead;

@Repository
public class SlsTblLeadDAO implements ISlsTblLeadDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(SlsTblLeadDAO.class);

	public SlsTblLeadDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblLead> getAllLead() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblLead> Leads = entityManager.createQuery("FROM SlsTblLead where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Leads;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblLead> getActiveLead() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblLead> Leads = entityManager
				.createQuery("FROM SlsTblLead where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Leads;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblLead> getLeadByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblLead where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblLead> Leads = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Leads;
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
	public String addNewLead(SlsTblLead SlsTblLead) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			SlsTblLead.setBlnStatus(true);
			SlsTblLead.setBlIsDeleted(false);
			entityManager.persist(SlsTblLead);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteLead(List<String> LeadsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serLeadId : LeadsId) {
				SlsTblLead Lead = entityManager.find(SlsTblLead.class, Integer.parseInt(serLeadId));
				if (Lead != null) {
					Lead.setBlIsDeleted(true);
					Lead.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Lead.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateLead(SlsTblLead SlsTblLead) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			SlsTblLead.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			SlsTblLead.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(SlsTblLead);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateLeadNo(String type) {
		// int LeadNo;
		String LeadType = type;
		// String LeadCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (LeadType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtLeadCode) from SlsTblLead ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtLeadCode) from SlsTblLead ")
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

	public String getLeadById(String LeadId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblLead where txtLeadCode='" + LeadId + "'";

			List<SlsTblLead> Lead = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Lead.size() > 0) {
				return String.valueOf(Lead.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<SlsTblLead> searchLead(SlsTblLead Lead) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblLead Lead where 1=1 ";
	  
	    if(Lead.getTxtName() !=null){
	    	query+=" and upper(Lead.txtName) like"+" upper('"+Lead.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Lead.getTxtEmail() !=null){
	    	query+=" and upper(Lead.txtEmail) like"+" upper('"+Lead.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Lead.getSerLeadId() !=null){
	    	query+=" and Lead.serLeadId ="+" "+Lead.getSerLeadId()+""+"  ";
	    }
	  
	    query+=" order by Lead.serLeadId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblLead> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
