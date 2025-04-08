package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblBankDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblBank;

@Repository
public class CfgTblBankDAO implements ICfgTblBankDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblBankDAO.class);

	public CfgTblBankDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBank> getAllBank() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblBank> Banks = entityManager.createQuery("FROM CfgTblBank where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Banks;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBank> getActiveBank() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblBank> Banks = entityManager
				.createQuery("FROM CfgTblBank where  blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Banks;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblBank> getBankByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblBank where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblBank> Banks = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Banks;
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
	public String addNewBank(CfgTblBank CfgTblBank) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblBank.setBlnStatus(true);
			CfgTblBank.setBlIsDeleted(false);
			CfgTblBank.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblBank.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(CfgTblBank);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteBank(List<String> BanksId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serBankId : BanksId) {
				CfgTblBank Bank = entityManager.find(CfgTblBank.class, Integer.parseInt(serBankId));
				if (Bank != null) {
					Bank.setBlIsDeleted(true);

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
	public String updateBank(CfgTblBank CfgTblBank) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblBank);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateBankNo(String type) {
		// int BankNo;
		String BankType = type;
		// String BankCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (BankType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtBankCode) from CfgTblBank ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtBankCode) from CfgTblBank ")
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

	public String getBankById(String BankId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblBank where txtBankCode='" + BankId + "'";

			List<CfgTblBank> Bank = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Bank.size() > 0) {
				return String.valueOf(Bank.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblBank> searchBank(CfgTblBank Bank) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblBank Bank where 1=1 ";
	    if(Bank.getTxtBankCode() != null)
	    {
	    	query+=" and upper(Bank.txtBankCode) like"+" upper('"+Bank.getTxtBankCode()+"%')"+" ";
	    }
	    if(Bank.getTxtBankName() !=null){
	    	query+=" and upper(Bank.txtBankName) like"+" upper('"+Bank.getTxtBankName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Bank.getTxtEmail() !=null){
	    	query+=" and upper(Bank.txtEmail) like"+" upper('"+Bank.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Bank.getSerBankId() !=null){
	    	query+=" and Bank.serBankId ="+" "+Bank.getSerBankId()+""+"  ";
	    }
	  
	    query+=" order by Bank.serBankId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblBank> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
