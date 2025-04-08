package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.Iterator;
import java.util.List;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSupplierInvoiceDAO;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssue;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSupplierInvoice;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblInvoiceDetail;

@Repository
public class SlsTblSupplierInvoiceDAO implements ISlsTblSupplierInvoiceDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	 @Autowired
	  private LoginDAO loginDao;

	private static final Logger log = LoggerFactory.getLogger(SlsTblSupplierInvoiceDAO.class);

	public SlsTblSupplierInvoiceDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSupplierInvoice> getAllSupplierInvoice() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSupplierInvoice> SupplierInvoices = entityManager.createQuery("FROM SlsTblSupplierInvoice where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SupplierInvoices;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSupplierInvoice> getActiveSupplierInvoice() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblSupplierInvoice> SupplierInvoices = entityManager
				.createQuery("FROM SlsTblSupplierInvoice where blnStatus=1 and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SupplierInvoices;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSupplierInvoice> getSupplierInvoiceByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblSupplierInvoice where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblSupplierInvoice> SupplierInvoices = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return SupplierInvoices;
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
	public String addNewSupplierInvoice(SlsTblSupplierInvoice SlsTblSupplierInvoice) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
//			SlsTblSupplierInvoice.setBlnStatus(true);
			SlsTblSupplierInvoice.setBlIsDeleted(false);
			SlsTblSupplierInvoice.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblSupplierInvoice.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			SlsTblSupplierInvoice.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
			
			entityManager.persist(SlsTblSupplierInvoice);
			
			SlsTblInvoiceDetail invoiceDetail;
    		 Iterator<SlsTblInvoiceDetail> itr = SlsTblSupplierInvoice.getSlsTblInvoiceDetails().iterator();
			 
			while (itr.hasNext()) {
				
				invoiceDetail = (SlsTblInvoiceDetail) itr.next();
				if(invoiceDetail.getNumQuantity() !=null && invoiceDetail.getNumQuantity().doubleValue() >0)
				{
				invoiceDetail.setBlIsDeleted(false);
				invoiceDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				invoiceDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				invoiceDetail.setSlsTblSupplierInvoice(SlsTblSupplierInvoice);
			
				entityManager.persist(invoiceDetail);
				}
			}
			
			InvTblIssue invTblIssue=SlsTblSupplierInvoice.getInvTblIssue();
			invTblIssue.setBlnIsBilled(true);
			invTblIssue.setTxtInvoiceStatus(SlsTblSupplierInvoice.getTxtInvoiceCode());
		
			entityManager.merge(invTblIssue);
			
			entityManager.getTransaction().commit();
			
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteSupplierInvoice(List<String> SupplierInvoicesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serSupplierInvoiceId : SupplierInvoicesId) {
				SlsTblSupplierInvoice SupplierInvoice = entityManager.find(SlsTblSupplierInvoice.class, Integer.parseInt(serSupplierInvoiceId));
				if (SupplierInvoice != null) {
					SupplierInvoice.setBlIsDeleted(true);

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
	public String updateSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblSupplierInvoice.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblSupplierInvoice.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			entityManager.merge(slsTblSupplierInvoice);
//			SlsTblInvoiceDetail invoiceDetail;
//   		   Iterator<SlsTblInvoiceDetail> itr = slsTblSupplierInvoice.getSlsTblInvoiceDetails().iterator();
			 
			/*while (itr.hasNext()) {
				invoiceDetail = (SlsTblInvoiceDetail) itr.next();
				if (invoiceDetail.getSerSoDetailId() != null && invoiceDetail.getSerSoDetailId() > 0) {
					if(invoiceDetail.getNumQuantity() !=null && invoiceDetail.getNumQuantity().doubleValue() >0)
					{
						invoiceDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
						invoiceDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					}
					else
					{
						invoiceDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
						invoiceDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
						invoiceDetail.setBlIsDeleted(true);
					}
					
					entityManager.merge(invoiceDetail);
				} else {
					if(invoiceDetail.getNumQuantity() !=null && invoiceDetail.getNumQuantity().doubleValue() >0)
					{
					invoiceDetail.setBlIsDeleted(false);
					invoiceDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
					invoiceDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
					invoiceDetail.setSlsTblSupplierInvoice(slsTblSupplierInvoice);

					entityManager.persist(invoiceDetail);
					}
				}
			}*/
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateSupplierInvoiceNo(String type) {
		// int SupplierInvoiceNo;
		String SupplierInvoiceType = type;
		// String SupplierInvoiceCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (SupplierInvoiceType.equals("1")) 
			CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if(user.getSerGroupId()!=null && user.getSerGroupId()==2)
		{
			try {
				entityManager.getTransaction().begin();

				/*String zoneCode = (String) entityManager.createQuery("select MAX(txtInvoiceCode) from SlsTblSupplierInvoice where serGroupId= "+user.getSerGroupId())
						.getSingleResult();*/
				
				  String zoneCode = (String)entityManager.createQuery("SELECT b.txtInvoiceCode FROM SlsTblSupplierInvoice b where serGroupId= "+user.getSerGroupId() +"  ORDER BY b.serSupplierInvoiceId DESC").setMaxResults(1).getSingleResult();

				  
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "INV-CNT-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(8));

				ord_no = ord_no + 1;
				String code = "INV-CNT-1";
				if (ord_no < 10)
					code = "INV-CNT-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "INV-CNT-0" + ord_no;
				else
					code = "INV-CNT-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				/*String zoneCode = (String) entityManager.createQuery("select MAX(txtInvoiceCode) from SlsTblSupplierInvoice ")
						.getSingleResult();*/
				
				  String zoneCode = (String)entityManager.createQuery("SELECT b.txtInvoiceCode FROM SlsTblSupplierInvoice b where serGroupId= "+user.getSerGroupId() +"  ORDER BY b.serSupplierInvoiceId DESC").setMaxResults(1).getSingleResult();

				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "INV-RW-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(7));

				ord_no1 = ord_no1 + 1;
				String code = "INV-RW-1";
				if (ord_no1 < 10)
					code = "INV-RW-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "INV-RW-0" + ord_no1;
				else
					code = "INV-RW-" + ord_no1;
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

	public String getSupplierInvoiceById(String SupplierInvoiceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblSupplierInvoice where txtInvoiceCode='" + SupplierInvoiceId + "'";

			List<SlsTblSupplierInvoice> SupplierInvoice = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (SupplierInvoice.size() > 0) {
				return String.valueOf(SupplierInvoice.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	 DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");
	
	@Override
	public List<SlsTblSupplierInvoice> searchSupplierInvoice(SlsTblSupplierInvoice SupplierInvoice) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblSupplierInvoice SupplierInvoice where 1=1 ";
		if (SupplierInvoice.getTxtInvoiceCode() != null) {
			query += " and upper(SupplierInvoice.txtInvoiceCode) like" + " upper('"
					+ SupplierInvoice.getTxtInvoiceCode() + "%')" + " ";
		}

		if (SupplierInvoice.getSerSupplierInvoiceId() != null) {
			query += " and SupplierInvoice.serSupplierInvoiceId =" + " " + SupplierInvoice.getSerSupplierInvoiceId()
					+ "" + "  ";
		}
		
		if (SupplierInvoice.getTxtInvoiceStatus() != null && SupplierInvoice.getTxtInvoiceStatus().trim().length() > 0) {
			query += " and SupplierInvoice.txtStatus = '"+SupplierInvoice.getTxtInvoiceStatus()+"'  ";
		}

		if (SupplierInvoice.getDte_date_from() != null && SupplierInvoice.getDte_date_from().trim().length() > 0) {
			try {

				query += " and SupplierInvoice.dteDate >= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(SupplierInvoice.getDte_date_from()))
						+ "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (SupplierInvoice.getDte_date_to() != null && SupplierInvoice.getDte_date_to().trim().length() > 0) {
			try {
				query += " and SupplierInvoice.dteDate <= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(SupplierInvoice.getDte_date_to()))
						+ "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	  
	    query+=" order by SupplierInvoice.serSupplierInvoiceId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblSupplierInvoice> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblInvoiceDetail> searchSupplierInvoiceDetail(int SupplierInvoiceId) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query="";
	    if(SupplierInvoiceId>0)
	     query = "from SlsTblInvoiceDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblSupplierInvoice.serSupplierInvoiceId= "+SupplierInvoiceId;
	  
	    else
	    	return null;
	 
	    
		

	
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblInvoiceDetail> lstSOD = entityManager.createQuery(
	    		subQuery).getResultList();
    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	


	    return lstSOD;
	}
	
}
