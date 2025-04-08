package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblDistributionChannelDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblDistributionChannel;

@Repository
public class CfgTblDistributionChannelDAO implements ICfgTblDistributionChannelDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblDistributionChannelDAO.class);

	public CfgTblDistributionChannelDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDistributionChannel> getAllDistributionChannel() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDistributionChannel> DistributionChannels = entityManager.createQuery("FROM CfgTblDistributionChannel where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return DistributionChannels;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDistributionChannel> getActiveDistributionChannel() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblDistributionChannel> DistributionChannels = entityManager
				.createQuery("FROM CfgTblDistributionChannel where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return DistributionChannels;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblDistributionChannel> getDistributionChannelByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDistributionChannel where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblDistributionChannel> DistributionChannels = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return DistributionChannels;
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
	public String addNewDistributionChannel(CfgTblDistributionChannel CfgTblDistributionChannel) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDistributionChannel.setBlnStatus(true);
			CfgTblDistributionChannel.setBlIsDeleted(false);
			entityManager.persist(CfgTblDistributionChannel);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDistributionChannel(List<String> DistributionChannelsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDistributionChannelId : DistributionChannelsId) {
				CfgTblDistributionChannel DistributionChannel = entityManager.find(CfgTblDistributionChannel.class, Integer.parseInt(serDistributionChannelId));
				if (DistributionChannel != null) {
					DistributionChannel.setBlIsDeleted(true);
					DistributionChannel.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					DistributionChannel.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
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
	public String updateDistributionChannel(CfgTblDistributionChannel CfgTblDistributionChannel) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblDistributionChannel.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			CfgTblDistributionChannel.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			entityManager.merge(CfgTblDistributionChannel);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDistributionChannelNo(String type) {
		// int DistributionChannelNo;
		String DistributionChannelType = type;
		// String DistributionChannelCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DistributionChannelType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDistributionChannelCode) from CfgTblDistributionChannel ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDistributionChannelCode) from CfgTblDistributionChannel ")
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

	public String getDistributionChannelById(String DistributionChannelId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblDistributionChannel where txtDistributionChannelCode='" + DistributionChannelId + "'";

			List<CfgTblDistributionChannel> DistributionChannel = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (DistributionChannel.size() > 0) {
				return String.valueOf(DistributionChannel.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblDistributionChannel> searchDistributionChannel(CfgTblDistributionChannel DistributionChannel) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblDistributionChannel DistributionChannel where 1=1 ";
	  
	    if(DistributionChannel.getTxtName() !=null){
	    	query+=" and upper(DistributionChannel.txtName) like"+" upper('"+DistributionChannel.getTxtName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(DistributionChannel.getTxtEmail() !=null){
	    	query+=" and upper(DistributionChannel.txtEmail) like"+" upper('"+DistributionChannel.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(DistributionChannel.getSerDistributionChannelId() !=null){
	    	query+=" and DistributionChannel.serDistributionChannelId ="+" "+DistributionChannel.getSerDistributionChannelId()+""+"  ";
	    }
	  
	    query+=" order by DistributionChannel.serDistributionChannelId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblDistributionChannel> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
