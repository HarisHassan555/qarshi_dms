package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.math.BigDecimal;
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
import com.bezkoder.spring.login.sa.bll.dto.DCDTO;

import com.bezkoder.spring.login.sa.dal.dao.IInvTblIssueDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssue;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssueDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;




@Repository
public class InvTblIssueDAO implements IInvTblIssueDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	
	 @Autowired
	  private LoginDAO loginDao;
	 
	 @Autowired
	  private ISlsTblSaleOrderDAO slsTblSaleOrderDAO;

	private static final Logger log = LoggerFactory.getLogger(InvTblIssueDAO.class);

	public InvTblIssueDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InvTblIssue> getAllIssue() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<InvTblIssue> Issues = entityManager.createQuery("FROM InvTblIssue where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Issues;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InvTblIssue> getActiveIssue() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<InvTblIssue> Issues = entityManager
				.createQuery("FROM InvTblIssue where blnStatus=1 and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Issues;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InvTblIssue> getIssueByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM InvTblIssue where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<InvTblIssue> Issues = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Issues;
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
	public String addNewIssue(InvTblIssue InvTblIssue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
//			InvTblIssue.setBlnStatus(true);
			InvTblIssue.setBlIsDeleted(false);
//			InvTblIssue.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			InvTblIssue.setDteCreateddate(commonService.getCurrentTimeStamp_new());
//			InvTblIssue.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			
			entityManager.persist(InvTblIssue);
			
			if( InvTblIssue.getInvTblIssueDetails() != null &&  InvTblIssue.getInvTblIssueDetails().size() >0)
			{
				InvTblIssueDetail issueDetail;
	    		 Iterator<InvTblIssueDetail> itr = InvTblIssue.getInvTblIssueDetails().iterator();
				 
				while (itr.hasNext()) {
					
					issueDetail = (InvTblIssueDetail) itr.next();
					if(issueDetail.getNumQuantity() !=null && issueDetail.getNumQuantity().doubleValue() >0)
					{
					if(!(issueDetail.getCfgTblProductDesign()!=null && issueDetail.getCfgTblProductDesign().getSerProductDesignId() >0))
					{
						issueDetail.setCfgTblProductDesign(null);
					}
					issueDetail.setBlIsDeleted(false);
//					issueDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
					issueDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
					issueDetail.setInvTblIssue(InvTblIssue);
//					ProTblProductionSummary proTblProductionSummary=
							
//					issueDetail.setNumUnitWt(proTblProductionSummary.getNumUnitWt());
					entityManager.persist(issueDetail);
			}
		
				
				
				}
			}
			SlsTblSaleOrder slsTblIssue=InvTblIssue.getSlsTblSaleOrder();
			slsTblIssue.setBlnIsCompleted(true);
			slsTblIssue.setTxtIssueCode(InvTblIssue.getTxtIssueCode());
			slsTblIssue.setDteIssuedate(InvTblIssue.getDteDate());
			entityManager.merge(slsTblIssue);
			entityManager.getTransaction().commit();
			entityManager.close();
//			setLedgerandStockEnteriesOnIssuance(InvTblIssue);
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteIssue(List<String> IssuesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serIssueId : IssuesId) {
				InvTblIssue Issue = entityManager.find(InvTblIssue.class, Integer.parseInt(serIssueId));
				if (Issue != null) {
					Issue.setBlIsDeleted(true);

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
	public String updateIssue(InvTblIssue invTblIssue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			invTblIssue.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			invTblIssue.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			
			
			entityManager.merge(invTblIssue);
			InvTblIssueDetail issueDetail;
	   		  
	   		  boolean status=true;
			if(invTblIssue.getTxtStatus()!=null && invTblIssue.getTxtStatus().equalsIgnoreCase("Approve"))
			{
				List<InvTblIssueDetail> lstIssueDetail=searchIssueDetail(invTblIssue.getSerIssueId());
				if(invTblIssue.getBlnIsApproved()!=null && invTblIssue.getBlnIsApproved())
				{
					invTblIssue.setInvTblIssueDetails(lstIssueDetail);
					/*Iterator<InvTblIssueDetail> itr = lstIssueDetail.iterator();
					while (itr.hasNext())
					{
						issueDetail = (InvTblIssueDetail) itr.next();
						if(issueDetail.getNumStockAvailabe().doubleValue() < issueDetail.getNumQuantity().doubleValue())
						{
							status=false;
						}
					}*/
				}
				
			}
			
			else {
				if (invTblIssue.getInvTblIssueDetails() != null) {
					Iterator<InvTblIssueDetail> itr = invTblIssue.getInvTblIssueDetails().iterator();

					while (itr.hasNext()) {
						issueDetail = (InvTblIssueDetail) itr.next();
						if (issueDetail.getSerIssueDetailId() != null && issueDetail.getSerIssueDetailId() > 0) {
							if (issueDetail.getNumQuantity() != null
									&& issueDetail.getNumQuantity().doubleValue() > 0) {
								issueDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								issueDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							} else {
								issueDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								issueDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
								issueDetail.setBlIsDeleted(true);
							}

							entityManager.merge(issueDetail);
						} else {
							if (issueDetail.getNumQuantity() != null
									&& issueDetail.getNumQuantity().doubleValue() > 0) {
								issueDetail.setBlIsDeleted(false);
								issueDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
								issueDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
								issueDetail.setInvTblIssue(invTblIssue);

								entityManager.persist(issueDetail);
							}
						}
					}
				}
			}
			if(status)
			{
			entityManager.getTransaction().commit();
			entityManager.close();
			
			if(invTblIssue.getTxtStatus()!=null && invTblIssue.getTxtStatus().equalsIgnoreCase("Approve"))
			setLedgerandStockEnteriesOnIssuance(invTblIssue);
			return "Success";
			}
			else
			{
				entityManager.close();
				return "QNA";
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateIssueNo(String type) {
		// int IssueNo;
		String IssueType = type;
		// String IssueCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (IssueType.equals("1"))
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if(user.getSerGroupId()!=null && user.getSerGroupId()==2)
		{
			try {
				
				if (IssueType.equals("GP")) {
					entityManager.getTransaction().begin();

					/*String zoneCode = (String) entityManager.createQuery("select MAX(txtGatePassNo) from InvTblIssue ")
							.getSingleResult();*/
					
					  String zoneCode = (String)entityManager.createQuery("SELECT b.txtGatePassNo FROM InvTblIssue b where serGroupId= "+user.getSerGroupId() +"  ORDER BY b.serIssueId DESC").setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "GP-CNT-00";
					}
					ord_no = Integer.valueOf(zoneCode.substring(7));

					ord_no = ord_no + 1;
					String code = "GP-CNT-1";
					if (ord_no < 10)
						code = "GP-CNT-00" + ord_no;
					else if (ord_no > 9 && ord_no < 100)
						code = "GP-CNT-0" + ord_no;
					else
						code = "GP-CNT-" + ord_no;
					return code;
				}
				else
				{
				entityManager.getTransaction().begin();

				/*String zoneCode = (String) entityManager.createQuery("select MAX(txtIssueCode) from InvTblIssue where serGroupId= "+user.getSerGroupId())
						.getSingleResult();*/
				
				  String zoneCode = (String)entityManager.createQuery("SELECT b.txtIssueCode FROM InvTblIssue b  where serGroupId= "+user.getSerGroupId() +" ORDER BY b.serIssueId DESC").setMaxResults(1).getSingleResult();

				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "DC-CNT-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(7));

				ord_no = ord_no + 1;
				String code = "DC-CNT-1";
				if (ord_no < 10)
					code = "DC-CNT-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "DC-CNT-0" + ord_no;
				else
					code = "DC-CNT-" + ord_no;
				return code;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} 
		else 
		{
			if (IssueType.equals("GP")) {
			try {
				entityManager.getTransaction().begin();

				/*String zoneCode = (String) entityManager.createQuery("select MAX(txtGatePassNo) from InvTblIssue ")
						.getSingleResult();*/
				
				  String zoneCode = (String)entityManager.createQuery("SELECT b.txtGatePassNo FROM InvTblIssue b where serGroupId= "+user.getSerGroupId() +"  ORDER BY b.serIssueId DESC").setMaxResults(1).getSingleResult();

				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "GP-RW-OPL-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(10));

				ord_no = ord_no + 1;
				String code = "GP-RW-OPL-1";
				if (ord_no < 10)
					code = "GP-RW-OPL-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "GP-RW-OPL-0" + ord_no;
				else
					code = "GP-RW-OPL-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} 
		else {
			try {
				entityManager.getTransaction().begin();

				/*String zoneCode = (String) entityManager.createQuery("select MAX(txtIssueCode) from InvTblIssue ")
						.getSingleResult();*/
				  String zoneCode = (String)entityManager.createQuery("SELECT b.txtIssueCode FROM InvTblIssue b where serGroupId= "+user.getSerGroupId() +"  ORDER BY b.serIssueId DESC").setMaxResults(1).getSingleResult();

				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "DC-RW-OPL-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(10));

				ord_no1 = ord_no1 + 1;
				String code = "DC-RW-OPL-1";
				if (ord_no1 < 10)
					code = "DC-RW-OPL-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "DC-RW-OPL-0" + ord_no1;
				else
					code = "DC-RW-OPL-" + ord_no1;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
		}
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getIssueById(String IssueId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM InvTblIssue where txtIssueCode='" + IssueId + "'";

			List<InvTblIssue> Issue = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Issue.size() > 0) {
				return String.valueOf(Issue.size());
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
	public List<InvTblIssue> searchIssue(InvTblIssue Issue) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from InvTblIssue Issue where 1=1 and Issue.txtStatus in ( 'Created','Completed') ";
		if (Issue.getTxtIssueCode() != null) {
			query += " and upper(Issue.txtIssueCode) like" + " upper('" + Issue.getTxtIssueCode() + "%')" + " ";
		}
		
		
CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
		{
			query+=" and Issue.serGroupId = "+user.getSerGroupId()+" ";
			
		}

		if (Issue.getSerIssueId() != null) {
			query += " and Issue.serIssueId =" + " " + Issue.getSerIssueId() + "" + "  ";
		}

		if (Issue.getTxtStatus() != null && Issue.getTxtStatus().trim().length() > 0) {
			query += " and Issue.txtStatus = 'Approved'  ";
		}

		if (Issue.getDte_date_from() != null && Issue.getDte_date_from().trim().length() > 0) {
			try {

				query += " and Issue.dteDate >= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(Issue.getDte_date_from()))
						+ "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (Issue.getDte_date_to() != null && Issue.getDte_date_to().trim().length() > 0) {
			try {
				query += " and Issue.dteDate <= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(Issue.getDte_date_to()))
						+ "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (Issue.getSlsTblSaleOrder()!= null && Issue.getSlsTblSaleOrder().getSerSaleOrderId() !=null) {
			query += " and Issue.slsTblSaleOrder.serSaleOrderId =" + " " + Issue.getSlsTblSaleOrder().getSerSaleOrderId() + "" + "  ";
		}
	  
	    query+=" order by Issue.serIssueId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<InvTblIssue> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<InvTblIssueDetail> searchIssueDetail(int IssueId) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query="";
	    if(IssueId>0)
	     query = "from InvTblIssueDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.invTblIssue.serIssueId= "+IssueId;
	  
	    else
	    	return null;
	 
	    
		

	
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<InvTblIssueDetail> lstDC = entityManager.createQuery(
	    		subQuery).getResultList();
    
	    entityManager.getTransaction().commit();
	    entityManager.close();
		InvTblIssueDetail issueDetail;
		Iterator<InvTblIssueDetail> itr = lstDC.iterator();

		while (itr.hasNext()) {

			issueDetail = (InvTblIssueDetail) itr.next();
			issueDetail.setNumStockAvailabe(new BigDecimal(0));
			double available_qty = 0;

			try {
				if (!(issueDetail.getCfgTblProductDesign() != null && issueDetail.getCfgTblProductDesign().getSerProductDesignId()!=null) ) {
					CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
					cfgTblProductDesign.setSerProductDesignId(0);
					issueDetail.setCfgTblProductDesign(cfgTblProductDesign);
				}

				if (!(issueDetail.getCfgTblProductQuality() != null && issueDetail.getCfgTblProductDesign().getSerProductDesignId()!=null)) {
					CfgTblProductQuality cfgTblProductQuality = new CfgTblProductQuality();
					cfgTblProductQuality.setSerProductQualityId(0);
					issueDetail.setCfgTblProductQuality(cfgTblProductQuality);
				}

				available_qty = 0;/*proTblProductionDetailDAO.getProductAvailableQty(
						issueDetail.getCfgTblProduct().getSerProductId(),
						issueDetail.getCfgTblProductDesign().getSerProductDesignId(),
						issueDetail.getCfgTblProductQuality().getSerProductQualityId());*/

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(available_qty>0)
			issueDetail.setNumStockAvailabe(new BigDecimal(available_qty));
		}	 
	    return lstDC;
		
	}
	
	public void setLedgerandStockEnteriesOnIssuance(InvTblIssue invTblIssue) {

	}
	
	
	
	@Override
	public String AssignGatePassNumber(InvTblIssue invTblIssue) {
		EntityManager entityManager = getEntityManager();
		try {
			String GP_No=generateIssueNo("GP");
			entityManager.getTransaction().begin();
			invTblIssue.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			invTblIssue.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			
			invTblIssue.setTxtGatePassNo(GP_No);
			entityManager.merge(invTblIssue);
	
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	
	
//	String addNewIssue(DCDTO invTblIssue);
	@Override
	public String addNewIssue(DCDTO dto) {
		
		if(dto.isInvoice())
		{
			if(!(dto.getTxtInvoiceNo() !=null && dto.getTxtInvoiceNo().trim().length() >0))
			{
				return "Invoice Number is empty";
			}
			else if(!(dto.getTxtDONo() !=null && dto.getTxtDONo().trim().length() >0))
			{
				return "DO Number is empty";
			}
			else if(!(dto.getDteInvDate() !=null ))
			{
				return "Invoice Date is empty";
			}
			
			List<InvTblIssue>  lstIssue = getIssueListById(dto.getTxtDONo()) ;
			if(!(lstIssue !=null && lstIssue.size() >0 ))
			{
				return "DC Not found";
			}

			InvTblIssue InvTblIssue=lstIssue.get(0);
			EntityManager entityManager = getEntityManager();
			try {
				
				InvTblIssue.setTxtINVNo(dto.getTxtInvoiceNo());
				InvTblIssue.setNumInvoiceQuantity(new BigDecimal(dto.getInvQuantity()));
				
				InvTblIssue.setTxtInvoiceStatus("Created");
				if(dto.getDteInvDate() != null)
						InvTblIssue.setDteInvoiceDate(dto.getDteInvDate());
				entityManager.getTransaction().begin();
				InvTblIssue.setBlIsDeleted(false);
				InvTblIssue.setDteInvcreateddate(commonService.getCurrentTimeStamp_new());
			
				entityManager.merge(InvTblIssue);
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(InvTblIssue);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
		else
		{
			if(!(dto.getTxtSaleOrderNo() !=null && dto.getTxtSaleOrderNo().trim().length() >0))
			{
				return "Sale Order Number is empty";
			}
			else if(!(dto.getTxtDONo() !=null && dto.getTxtDONo().trim().length() >0))
			{
				return "DO Number is empty";
			}
			else if(!(dto.getDteDate() !=null ))
			{
				return "DO Date is empty";
			}
			
			List<SlsTblSaleOrder>  lstOrder = slsTblSaleOrderDAO.getSaleOrderBySapId(dto.getTxtSaleOrderNo()) ;
			if(!(lstOrder !=null && lstOrder.size() >0 ))
			{
				return "Sale Order Not found";
			}
	
			SlsTblSaleOrder slsTblSaleOrder=lstOrder.get(0);
			EntityManager entityManager = getEntityManager();
			try {
				
				InvTblIssue InvTblIssue=new InvTblIssue();
				InvTblIssue.setSlsTblSaleOrder(slsTblSaleOrder);
				if(dto.getTxtDONo()!=null && dto.getTxtDONo().trim().length() >0)
						InvTblIssue.setTxtIssueCode(dto.getTxtDONo());
				if( dto.getQuantity() >0)
				InvTblIssue.setNumQuantity(new BigDecimal(dto.getQuantity()));
				
				if( dto.getTareWeight() >0)
				InvTblIssue.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
				
				if( dto.getGrossWeight() >0)
				InvTblIssue.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
				
				if( dto.getNetWeight() >0)
				InvTblIssue.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
				
//				if(dto.getTxtStatus() !=null && dto.getTxtStatus().trim().length()>0)
//				{
//					InvTblIssue.setTxtStatus(dto.getTxtStatus().trim());
//				}
//				else
				InvTblIssue.setTxtStatus("Created");
				if(dto.getDteDate() != null)
						InvTblIssue.setDteDate(dto.getDteDate());
				entityManager.getTransaction().begin();
				InvTblIssue.setBlIsDeleted(false);
				InvTblIssue.setDteCreateddate(commonService.getCurrentTimeStamp_new());
	
				
				entityManager.persist(InvTblIssue);
				
				
				SlsTblSaleOrder slsTblIssue=InvTblIssue.getSlsTblSaleOrder();
				slsTblIssue.setBlnIsCompleted(true);
				if(slsTblIssue.getTxtIssueCode()!=null && slsTblIssue.getTxtIssueCode().trim().length() >0 )
						slsTblIssue.setTxtIssueCode(slsTblIssue.getTxtIssueCode()+","+InvTblIssue.getTxtIssueCode());
				else
					slsTblIssue.setTxtIssueCode(InvTblIssue.getTxtIssueCode());
				slsTblIssue.setDteIssuedate(InvTblIssue.getDteDate());
				entityManager.merge(slsTblIssue);
				
				entityManager.getTransaction().commit();
				entityManager.close();
	//			setLedgerandStockEnteriesOnIssuance(InvTblIssue);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
	}

	
	@Override
	public String updateIssue(DCDTO dto) {
		
		if(dto.isInvoice())
		{
			if (dto.getTxtStatus().equalsIgnoreCase("Completed") ||
					dto.getTxtStatus().equalsIgnoreCase("Cancelled") ||
					dto.getTxtStatus().equalsIgnoreCase("Cancel")   ) {
				
//				||
//				dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//				|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//				|| dto.getTxtStatus().equalsIgnoreCase("Deleted")

				List<InvTblIssue> lstDetail = new ArrayList();
				if (dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)
					lstDetail = getIssueListByInvoiceId(dto.getTxtInvoiceNo());
				else
					return "Invoice Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "Invoice Not Found";
				}

				InvTblIssue invTblIssue = lstDetail.get(0);
				/*
				 * if (invTblIssue.getTxtStatus() != null &&
				 * invTblIssue.getTxtStatus().trim().length() > 0) { if
				 * (invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Approved")) return
				 * "Invoice Already Approved"; else if
				 * (invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Approve")) return
				 * "Invoice Already Approved"; else if
				 * (invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Rejected")) return
				 * "Invoice Already Rejected"; else if
				 * (invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Cancel")) return
				 * "Invoice Already Cancel"; else if
				 * (invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Deleted")) return
				 * "Invoice Already Deleted"; }
				 */

				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();
					
					invTblIssue.setDteInvModifieddate(commonService.getCurrentTimeStamp_new());
					if( dto.getTxtStatus().equalsIgnoreCase("Cancelled") ||
					dto.getTxtStatus().equalsIgnoreCase("Cancel") )
					{
						invTblIssue.setTxtInvoiceStatus(null);
						invTblIssue.setTxtINVNo(null);
						invTblIssue.setDteInvoiceDate(null);
					}
					else
					invTblIssue.setTxtInvoiceStatus(dto.getTxtStatus());

					entityManager.merge(invTblIssue);

					entityManager.getTransaction().commit();
					entityManager.close();
					return "Invoice Updated successfully.";

				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return "Failure";
				}
			} else {
				return "Status is not Valid";
			}
		}
		else {
			if (!dto.isUpdate())
				if (dto.getTxtStatus() != null && dto.getTxtStatus().trim().length() > 0) {
				} else {
					return "Status is empty";
				}

//			if (dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//					|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//					|| dto.getTxtStatus().equalsIgnoreCase("Deleted")) 
			if (!dto.isUpdate())
			if (dto.getTxtStatus().equalsIgnoreCase("Completed") ||
					dto.getTxtStatus().equalsIgnoreCase("Deleted") ||dto.getTxtStatus().equalsIgnoreCase("Reversed") )
			{
				
			}
		 else {
			return "Status is not Valid";
		}

				List<InvTblIssue> lstDetail = new ArrayList();
				if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
					lstDetail = getIssueListById(dto.getTxtDONo());
				else
					return "DO Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "DO Not Found";
				}
				InvTblIssue invTblIssue = lstDetail.get(0);

				/*
				 *  if (invTblIssue.getTxtStatus() !=
				 * null && invTblIssue.getTxtStatus().trim().length() > 0) { if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approved")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approve")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Rejected")) return
				 * "DO Already Rejected"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Cancel")) return
				 * "DO Already Cancel"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Deleted")) return
				 * "DO Already Deleted"; }
				 */
				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();
					invTblIssue.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					
					if(dto.isUpdate())
					{
						
					if( dto.getQuantity() >0)
						invTblIssue.setNumQuantity(new BigDecimal(dto.getQuantity()));
					
					if( dto.getTareWeight() >0)
						invTblIssue.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
					
					if( dto.getGrossWeight() >0)
						invTblIssue.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
					
					if( dto.getNetWeight() >0)
						invTblIssue.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
					
//						invTblIssue.setNumQuantity(new BigDecimal(dto.getQuantity()));
//						invTblIssue.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//						invTblIssue.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//						invTblIssue.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
					}
					
					if(dto.getTxtStatus()!=null && dto.getTxtStatus().trim().length() >1)
					invTblIssue.setTxtStatus(dto.getTxtStatus());
					
					if(dto.getTxtInvStatus()!=null && dto.getTxtInvStatus().trim().length() >0)
					   invTblIssue.setTxtInvoiceStatus(dto.getTxtInvStatus());
					
					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() >0)
						   invTblIssue.setTxtINVNo(dto.getTxtInvoiceNo());
					
					if(dto.getDteInvDate()!=null )
						   invTblIssue.setDteInvoiceDate(dto.getDteInvDate());

					entityManager.merge(invTblIssue);

					entityManager.getTransaction().commit();
					entityManager.close();
					return "DO Updated successfully.";

				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return "Failure";
				}
		
		
		}
	}

	public List<InvTblIssue> getIssueListById(String IssueId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM InvTblIssue where txtIssueCode='" + IssueId + "'";

			List<InvTblIssue> Issue = entityManager.createQuery(query).getResultList();

			return Issue;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	public List<InvTblIssue> getIssueListByInvoiceId(String InvoiceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM InvTblIssue where txtINVNo='" + InvoiceId + "'";

			List<InvTblIssue> Issue = entityManager.createQuery(query).getResultList();

			return Issue;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	
	@Override
	public String updateIssueWithDCDelete(DCDTO dto) {
		

		
				List<InvTblIssue> lstDetail = new ArrayList();
				if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
					lstDetail = getIssueListById(dto.getTxtDONo());
				else
					return "DO Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "DO Not Found";
				}
				InvTblIssue invTblIssue = lstDetail.get(0);

				/*
				 *  if (invTblIssue.getTxtStatus() !=
				 * null && invTblIssue.getTxtStatus().trim().length() > 0) { if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approved")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approve")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Rejected")) return
				 * "DO Already Rejected"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Cancel")) return
				 * "DO Already Cancel"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Deleted")) return
				 * "DO Already Deleted"; }
				 */
				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();
					invTblIssue.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					
					if(invTblIssue.getTxtINVNo()!=null && invTblIssue.getTxtInvoiceStatus().equalsIgnoreCase("Cancel"))
					{
						
					}
					else
					{
						return "Invoice is not Canceled";
					}
					
							
					invTblIssue.setTxtStatus("Delete");
					
					

					entityManager.merge(invTblIssue);

					entityManager.getTransaction().commit();
					entityManager.close();
					return "DO Deleted successfully.";

				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return "Failure";
				}
		
		
	}
	
	
	@Override
	public String updateIssueWithInvoiceCancel(DCDTO dto) {
		

			if (dto.getTxtInvStatus().equalsIgnoreCase("Cancel") )
			{
				return "Invoice is already Canceled";
			}
		 else {
			
		}

				List<InvTblIssue> lstDetail = new ArrayList();
				if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
					lstDetail = getIssueListById(dto.getTxtDONo());
				else
					return "DO Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "DO Not Found";
				}
				InvTblIssue invTblIssue = lstDetail.get(0);

				/*
				 *  if (invTblIssue.getTxtStatus() !=
				 * null && invTblIssue.getTxtStatus().trim().length() > 0) { if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approved")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Approve")) return
				 * "DO Already Approved"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Rejected")) return
				 * "DO Already Rejected"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Cancel")) return
				 * "DO Already Cancel"; else if
				 * (invTblIssue.getTxtStatus().equalsIgnoreCase("Deleted")) return
				 * "DO Already Deleted"; }
				 */
				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();
					invTblIssue.setDteModifieddate(commonService.getCurrentTimeStamp_new());
					
							
					invTblIssue.setTxtInvoiceStatus("Cancel");
					
					

					entityManager.merge(invTblIssue);

					entityManager.getTransaction().commit();
					entityManager.close();
					return "Invoice Cancel successfully.";

				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return "Failure";
				}
		
		
	}
}
