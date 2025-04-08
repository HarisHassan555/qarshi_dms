package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblModelDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblModel;

@Repository
public class CfgTblModelDAO implements ICfgTblModelDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private LoginDAO loginDao;

	private static final Logger log = LoggerFactory.getLogger(CfgTblModelDAO.class);

	public CfgTblModelDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblModel> getAllModel() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblModel> Models = entityManager.createQuery("FROM CfgTblModel where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Models;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblModel> getActiveModel() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
		List<String> lstUsers=new ArrayList();
		lstUsers.add("CSF.service");
		lstUsers.add("CIS.service");
		lstUsers.add("CMN.service");
		lstUsers.add("CGT.service");
		lstUsers.add("CCY.service");
		lstUsers.add("CGB.service");
		lstUsers.add("CLP.service");
		lstUsers.add("CCL.service");
		lstUsers.add("CID.service");
		lstUsers.add("GAL.service");
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		List<CfgTblModel> Models=new ArrayList();
		if(user !=null 	&& lstUsers.indexOf(user.getTxtUserName()) > 0)
		{
			Models = entityManager
					.createQuery("FROM CfgTblModel where blnStatus=TRUE and blIsDeleted=FALSE and serGroupId=1 ").getResultList();
		}
		else
		{
			 Models = entityManager
					.createQuery("FROM CfgTblModel where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();
		}
		

		entityManager.getTransaction().commit();
		entityManager.close();

		return Models;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblModel> getModelByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblModel where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblModel> Models = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Models;
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
	public String addNewModel(CfgTblModel CfgTblModel) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblModel.setBlnStatus(true);
			CfgTblModel.setBlIsDeleted(false);
			entityManager.persist(CfgTblModel);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteModel(List<String> ModelsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serModelId : ModelsId) {
				CfgTblModel Model = entityManager.find(CfgTblModel.class, Integer.parseInt(serModelId));
				if (Model != null) {
					Model.setBlIsDeleted(true);
					Model.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					Model.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateModel(CfgTblModel CfgTblModel) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblModel.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblModel.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblModel);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateModelNo(String type) {
		// int ModelNo;
		String ModelType = type;
		// String ModelCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ModelType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtModelCode) from CfgTblModel ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtModelCode) from CfgTblModel ")
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

	public String getModelById(String ModelId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblModel where txtModelCode='" + ModelId + "'";

			List<CfgTblModel> Model = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Model.size() > 0) {
				return String.valueOf(Model.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblModel> searchModel(CfgTblModel Model) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblModel Model where 1=1 ";
	  
	    if(Model.getTxtModelName() !=null){
	    	query+=" and upper(Model.txtModelName) like"+" upper('"+Model.getTxtModelName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Model.getTxtEmail() !=null){
	    	query+=" and upper(Model.txtEmail) like"+" upper('"+Model.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Model.getSerModelId() !=null){
	    	query+=" and Model.serModelId ="+" "+Model.getSerModelId()+""+"  ";
	    }
	  
	    query+=" order by Model.serModelId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblModel> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
