package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.bezkoder.spring.login.util.UtilDateAndTime;
import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.DCDTO;

import com.bezkoder.spring.login.sa.dal.dao.ISlsTblTIRDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIR;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIRDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblTIRDocument;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;


@Repository
public class SlsTblTIRDAO implements ISlsTblTIRDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	@Autowired
	private LoginDAO loginDao;

	@Autowired
	private ISlsTblSaleOrderDAO slsTblSaleOrderDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblTIRDAO.class);

	public SlsTblTIRDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblTIR> getAllTIR() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblTIR> TIRs = entityManager.createQuery("FROM SlsTblTIR where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return TIRs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblTIR> getActiveTIR() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblTIR> TIRs = entityManager
				.createQuery("FROM SlsTblTIR where blnStatus=1 and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return TIRs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblTIR> getTIRByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIR where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblTIR> TIRs = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return TIRs;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	/*
	 * String date = simpleDateFormat.format(new Date()); System.out.println(date);
	 */
	@Override
	public String addNewTIR(SlsTblTIR SlsTblTIR) {
		EntityManager entityManager = getEntityManager();
		try {
				entityManager.getTransaction().begin();
//			SlsTblTIR.setBlnStatus(true);
			SlsTblTIR.setBlIsDeleted(false);
			SlsTblTIR.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblTIR.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			try {
				SlsTblTIR.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (SlsTblTIR.getSlsTblSoVehicleDetail() != null) {

				SlsTblTIR.setCfgTblCustomer(SlsTblTIR.getSlsTblSoVehicleDetail().getCfgTblCustomer());
				SlsTblTIR.setCfgTblProduct(SlsTblTIR.getSlsTblSoVehicleDetail().getCfgTblProduct());

				SlsTblTIR.setCfgTblModel(SlsTblTIR.getSlsTblSoVehicleDetail().getCfgTblModel());
				SlsTblTIR.setCfgTblColor(SlsTblTIR.getSlsTblSoVehicleDetail().getCfgTblColor());
			}
			
			entityManager.persist(SlsTblTIR);
			

			if (SlsTblTIR.getSlsTblTIRDetail() != null
					&& SlsTblTIR.getSlsTblTIRDetail().size() > 0) {
				SlsTblTIRDetail tirDetail;
				Iterator<SlsTblTIRDetail> itr = SlsTblTIR.getSlsTblTIRDetail().iterator();

				while (itr.hasNext()) {

					tirDetail = (SlsTblTIRDetail) itr.next();
					tirDetail.setBlIsService(false);
					tirDetail.setBlIsSP(true);
					if (tirDetail.getNumQuantity() != null
							&& tirDetail.getNumQuantity().doubleValue() > 0) {
						if (!(tirDetail.getCfgTblProductDesign() != null
								&& tirDetail.getCfgTblProductDesign().getSerProductDesignId() > 0)) {
							tirDetail.setCfgTblProductDesign(null);
						}
						tirDetail.setBlIsDeleted(false);
//					tirDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						tirDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						tirDetail.setSlsTblTIR(SlsTblTIR);
//					ProTblProductionSummary proTblProductionSummary=

//					tirDetail.setNumUnitWt(proTblProductionSummary.getNumUnitWt());
						entityManager.persist(tirDetail);
					}

				}
			}
			
			if (SlsTblTIR.getSlsTblTIRDetailservices() != null
					&& SlsTblTIR.getSlsTblTIRDetail().size() > 0) {
				SlsTblTIRDetail tirDetail;
				Iterator<SlsTblTIRDetail> itr = SlsTblTIR.getSlsTblTIRDetailservices().iterator();

				while (itr.hasNext()) {

					tirDetail = (SlsTblTIRDetail) itr.next();
					tirDetail.setBlIsService(true);
					tirDetail.setBlIsSP(false);
					if (!(tirDetail.getNumQuantity() != null
							&& tirDetail.getNumQuantity().doubleValue() > 0))
					{
						tirDetail.setNumQuantity(new BigDecimal(1));
					}
					if (tirDetail.getNumQuantity() != null
							&& tirDetail.getNumQuantity().doubleValue() > 0) {
						if (!(tirDetail.getCfgTblProductDesign() != null
								&& tirDetail.getCfgTblProductDesign().getSerProductDesignId() > 0)) {
							tirDetail.setCfgTblProductDesign(null);
						}
						tirDetail.setBlIsDeleted(false);
//					tirDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						tirDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						tirDetail.setSlsTblTIR(SlsTblTIR);
//					ProTblProductionSummary proTblProductionSummary=

//					tirDetail.setNumUnitWt(proTblProductionSummary.getNumUnitWt());
						entityManager.persist(tirDetail);
					}

				}
			}
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteTIR(List<String> TIRsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serTIRId : TIRsId) {
				SlsTblTIR TIR = entityManager.find(SlsTblTIR.class, Integer.parseInt(serTIRId));
				if (TIR != null) {
					TIR.setBlIsDeleted(true);

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
	public String deleteTIRDetail(List<String> TIRsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serTIRDetailId : TIRsId) {
				SlsTblTIRDetail TIR = entityManager.find(SlsTblTIRDetail.class, Integer.parseInt(serTIRDetailId));
				if (TIR != null) {
					TIR.setBlIsDeleted(true);

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
	public String updateTIR(SlsTblTIR slsTblTIR) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			if(slsTblTIR.getTxtStatus3() !=null && slsTblTIR.getTxtStatus3().equalsIgnoreCase("Approved"))
				slsTblTIR.setDteApproveddate3(new Timestamp(UtilDateAndTime.getCurrentDate().getTime()));
			else if(slsTblTIR.getTxtStatus2() !=null && slsTblTIR.getTxtStatus2().equalsIgnoreCase("Approved"))
				slsTblTIR.setDteApproveddate2(new Timestamp(UtilDateAndTime.getCurrentDate().getTime()));
			else if(slsTblTIR.getTxtStatus1() != null && slsTblTIR.getTxtStatus1().equalsIgnoreCase("Approved"))
				slsTblTIR.setDteApproveddate1(new Timestamp(UtilDateAndTime.getCurrentDate().getTime()));
				
			slsTblTIR.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblTIR.setDteModifieddate(commonService.getCurrentTimeStamp_new());

			entityManager.merge(slsTblTIR);
			SlsTblTIRDetail tirDetail;

			boolean status = true;
//			if(slsTblTIR.getTxtStatus()!=null && slsTblTIR.getTxtStatus().equalsIgnoreCase("Approve"))
//			{
//				List<SlsTblTIRDetail> lstTIRDetail=searchTIRDetail(slsTblTIR.getSerTIRId());
//				if(slsTblTIR.getBlnIsApproved()!=null && slsTblTIR.getBlnIsApproved())
//				{
//					slsTblTIR.setSlsTblTIRDetails(lstTIRDetail);
//					/*Iterator<SlsTblTIRDetail> itr = lstTIRDetail.iterator();
//					while (itr.hasNext())
//					{
//						tirDetail = (SlsTblTIRDetail) itr.next();
//						if(tirDetail.getNumStockAvailabe().doubleValue() < tirDetail.getNumQuantity().doubleValue())
//						{
//							status=false;
//						}
//					}*/
//				}
//				
//			}

//			else 
			
			if(slsTblTIR.getSlsTblTIRDetail()!=null && slsTblTIR.getSlsTblTIRDetail().size()>0)
			
			{
		List<SlsTblTIRDetail> lstDetails =searchTIRDetail(slsTblTIR.getSerTIRId());
			
			List<SlsTblTIRDetail> lstDetailsDB = slsTblTIR.getSlsTblTIRDetail();
			
			if(lstDetails != null && lstDetails.size() > 0)
			{
				for(SlsTblTIRDetail dto : lstDetails)
				{
					boolean objectExists=false;
					
					Optional<SlsTblTIRDetail> existingCar =  (lstDetailsDB.stream()
					        .filter(c -> c.getSerTIRDetailId() != null && c.getSerTIRDetailId().equals(dto.getSerTIRDetailId()))
					        .findFirst());
					
					if (existingCar.isPresent())
						System.out.println(" exist---------"+dto.getSerTIRDetailId()+"---");
					else
					{
						System.out.println("not exist---------"+dto.getSerTIRDetailId()+"---");
						
						SlsTblTIRDetail detail = getTIRDetailByPK(dto.getSerTIRDetailId());
						detail.setBlIsDeleted(true);
						entityManager.merge(detail);
						entityManager.flush();
					}
				}
				
				}
			}

			{
				if (slsTblTIR.getSlsTblTIRDetail() != null) {
					Iterator<SlsTblTIRDetail> itr = slsTblTIR.getSlsTblTIRDetail().iterator();

					while (itr.hasNext()) {
						tirDetail = (SlsTblTIRDetail) itr.next();
						if (tirDetail.getSerTIRDetailId() != null
								&& tirDetail.getSerTIRDetailId() > 0) {
							if (tirDetail.getNumQuantity() != null
									&& tirDetail.getNumQuantity().doubleValue() > 0) {
								tirDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							} else {
								tirDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
								tirDetail.setBlIsDeleted(true);
							}

							entityManager.merge(tirDetail);
						} else {
							if (tirDetail.getNumQuantity() != null
									&& tirDetail.getNumQuantity().doubleValue() > 0) {
								tirDetail.setBlIsDeleted(false);
								tirDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
								tirDetail.setSlsTblTIR(slsTblTIR);

								entityManager.persist(tirDetail);
							}
						}
					}
				}
				
				
				if (slsTblTIR.getSlsTblTIRDetailservices() != null) {
					Iterator<SlsTblTIRDetail> itr = slsTblTIR.getSlsTblTIRDetailservices().iterator();

					while (itr.hasNext()) {
						tirDetail = (SlsTblTIRDetail) itr.next();
						if (tirDetail.getSerTIRDetailId() != null
								&& tirDetail.getSerTIRDetailId() > 0) {
							if (tirDetail.getNumQuantity() != null
									&& tirDetail.getNumQuantity().doubleValue() > 0) {
								tirDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							} else {
								tirDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
								tirDetail.setBlIsDeleted(true);
							}

							entityManager.merge(tirDetail);
						} else {
							if (tirDetail.getNumQuantity() != null
									&& tirDetail.getNumQuantity().doubleValue() > 0) {
								tirDetail.setBlIsDeleted(false);
								tirDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
								tirDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
								tirDetail.setSlsTblTIR(slsTblTIR);

								entityManager.persist(tirDetail);
							}
						}
					}
				}
			}
			if (status) {
				entityManager.getTransaction().commit();
				entityManager.close();

				if (slsTblTIR.getTxtStatus() != null
						&& slsTblTIR.getTxtStatus().equalsIgnoreCase("Approve"))
					setLedgerandStockEnteriesOnIssuance(slsTblTIR);
				return "Success";
			} else {
				entityManager.close();
				return "QNA";
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateTIRNo(String type) {
		// int TIRNo;
		String TIRType = type;
		// String TIRCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (TIRType.equals("1"))
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
 
		 {
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String) entityManager.
					 * createQuery("select MAX(txtTIRCode) from SlsTblTIR ")
					 * .getSingleResult();
					 */
					String zoneCode = (String) entityManager
							.createQuery(
									"SELECT b.txtTIRNo FROM SlsTblTIR b   ORDER BY b.serTIRId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "TIR-00";
					}
					ord_no1 = Integer.valueOf(zoneCode.substring(4));

					ord_no1 = ord_no1 + 1;
					String code = "TIR-001";
					if (ord_no1 < 10)
						code = "TIR-00" + ord_no1;
					else if (ord_no1 > 9 && ord_no1 < 100)
						code = "TIR-0" + ord_no1;
					else
						code = "TIR-" + ord_no1;
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

	public String getTIRById(String TIRId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIR where txtTIRCode='" + TIRId + "'";

			List<SlsTblTIR> TIR = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (TIR.size() > 0) {
				return String.valueOf(TIR.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public SlsTblTIR getTIRByPK(Integer TIRId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIR where serTIRId=" + TIRId + "";

			SlsTblTIR TIR = (SlsTblTIR)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			if (TIR!=null) {
				return TIR;
			}
			return null;

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	
	public SlsTblTIRDetail getTIRDetailByPK(Integer id) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIRDetail where serTIRDetailId=" + id + "";

			SlsTblTIRDetail TIRD = (SlsTblTIRDetail)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			if (TIRD!=null) {
				return TIRD;
			}
			return null;

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			return null;
		}
	}

	DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public List<SlsTblTIR> searchTIR(SlsTblTIR TIR) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
//		 and TIR.txtStatus in ( 'Created','Completed')
		
		
		
		String query = "select  new com.bezkoder.spring.login.admin.bll.dto.TIRDTO(TIR.slsTblSoVehicleDetail.txtRegistrationNo,TIR.serTIRId,TIR.dteDate,TIR.txtTIRNo,TIR.cfgTblDealer.txtCustomerName,TIR.slsTblSoVehicleDetail.cfgTblModel.txtModelName,\r\n" +
				"				TIR.slsTblSoVehicleDetail.txtChassisNo,TIR.slsTblSoVehicleDetail.txtEngineNo,TIR.cfgTblProduct.cfgTblProductCategory.txtProductCategoryName,\r\n" + 
				"				TIR.slsTblSoVehicleDetail.cfgTblColor.txtColorName,\r\n" + 
				"				TIR.dteTimeOut,TIR.txtStatus1,TIR.txtStatus2,TIR.txtStatus3,txtLevel,"
				+ "				TIR.dteApproveddate1,TIR.dteApproveddate2,TIR.dteApproveddate3)  from SlsTblTIR TIR where 1=1 ";
		if (TIR.getTxtTIRNo() != null) {
			query += " and upper(TIR.txtTIRNo) like" + " upper('" + TIR.getTxtTIRNo() + "%')"
					+ " ";
		}

		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			query += " and TIR.serGroupId = " + user.getSerGroupId() + " ";

		}

		if (TIR.getSerTIRId() != null) {
			query += " and TIR.serTIRId =" + " " + TIR.getSerTIRId() + "" + "  ";
		}
		
		if (TIR.getNumLevel() != null && TIR.getNumLevel() > 0) {
			if(TIR.getNumLevel() == 1)
			query += " and  ( TIR.numLevel  = " + TIR.getNumLevel() + " or TIR.numLevel  is null )"
					+ " ";
			else
				query += " and  ( TIR.numLevel  = " + TIR.getNumLevel() + "  )"
						+ " ";
		}

//		if (TIR.getTxtStatus() != null && TIR.getTxtStatus().trim().length() > 0) {
//			query += " and TIR.txtStatus = 'Approved'  ";
//		}

//		if (TIR.getDte_date_from() != null && TIR.getDte_date_from().trim().length() > 0) {
//			try {
//
//				query += " and TIR.dteDate >= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(TIR.getDte_date_from()))
//						+ "'";
//			} catch (ParseException e) {
//				e.printStackTrace();
//			}
//		}
//
//		if (TIR.getDte_date_to() != null && TIR.getDte_date_to().trim().length() > 0) {
//			try {
//				query += " and TIR.dteDate <= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(TIR.getDte_date_to()))
//						+ "'";
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

//		if (TIR.getSlsTblSaleOrder()!= null && TIR.getSlsTblSaleOrder().getSerSaleOrderId() !=null) {
//			query += " and TIR.slsTblSaleOrder.serSaleOrderId =" + " " + TIR.getSlsTblSaleOrder().getSerSaleOrderId() + "" + "  ";
//		}
		

		if (TIR.getDte_date_from() != null && TIR.getDte_date_from().trim().length() > 0) {
			try {

				query += " and TIR.dteDate >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(TIR.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (TIR.getDte_date_to() != null && TIR.getDte_date_to().trim().length() > 0) {
			try {
				query += " and TIR.dteDate <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(TIR.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if (cfgTblUser.getCfgTblCustomer() != null) {
			if (cfgTblUser.getCfgTblCustomer().getBlIsDealer() != null
					&& cfgTblUser.getCfgTblCustomer().getBlIsDealer()) {
				// query+=" and SaleOrder.cfgTblDealer.serCustomerId ="+"
				// "+cfgTblUser.getCfgTblCustomer().getSerCustomerId()+""+" ";

				query += " and TIR.cfgTblDealer.serCustomerId in"
						+ " (select serCustomerId from CfgTblCustomer customer  where customer.serCustomerId="
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ "  or customer.cfgTblGroupCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ " or customer.cfgTblCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + ")";
			} else {
				query += " and TIR.cfgTblDealer.serCustomerId =" + " "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";

			}
		}
		
		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			query += " and TIR.serGroupId = " + user.getSerGroupId() + " ";

		}

		query += " order by TIR.serTIRId  DESC";
		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblTIR> cust = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return cust;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblTIRDetail> searchTIRDetail(int TIRId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (TIRId > 0)
			query = "from SlsTblTIRDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblTIR.serTIRId= "
					+ TIRId;

		else
			return null;

		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblTIRDetail> lstDC = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		SlsTblTIRDetail tirDetail;
		Iterator<SlsTblTIRDetail> itr = lstDC.iterator();

		while (itr.hasNext()) {

			tirDetail = (SlsTblTIRDetail) itr.next();
			tirDetail.setNumStockAvailabe(new BigDecimal(0));
			double available_qty = 0;

			try {
				if (!(tirDetail.getCfgTblProductDesign() != null
						&& tirDetail.getCfgTblProductDesign().getSerProductDesignId() != null)) {
					CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
					cfgTblProductDesign.setSerProductDesignId(0);
					tirDetail.setCfgTblProductDesign(cfgTblProductDesign);
				}

				if (!(tirDetail.getCfgTblProductQuality() != null
						&& tirDetail.getCfgTblProductDesign().getSerProductDesignId() != null)) {
					CfgTblProductQuality cfgTblProductQuality = new CfgTblProductQuality();
					cfgTblProductQuality.setSerProductQualityId(0);
					tirDetail.setCfgTblProductQuality(cfgTblProductQuality);
				}

				available_qty = 0;/*
									 * proTblProductionDetailDAO.getProductAvailableQty(
									 * tirDetail.getCfgTblProduct().getSerProductId(),
									 * tirDetail.getCfgTblProductDesign().getSerProductDesignId(),
									 * tirDetail.getCfgTblProductQuality().getSerProductQualityId());
									 */

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (available_qty > 0)
				tirDetail.setNumStockAvailabe(new BigDecimal(available_qty));
		}
		return lstDC;

	}

	public void setLedgerandStockEnteriesOnIssuance(SlsTblTIR slsTblTIR) {

	}

	@Override
	public String AssignGatePassNumber(SlsTblTIR slsTblTIR) {
		EntityManager entityManager = getEntityManager();
		try {
			String GP_No = generateTIRNo("GP");
			entityManager.getTransaction().begin();
			slsTblTIR.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblTIR.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//			slsTblTIR.setTxtGatePassNo(GP_No);
			entityManager.merge(slsTblTIR);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

//	String addNewTIR(DCDTO slsTblTIR);
	@Override
	public String addNewTIR(DCDTO dto) {

		if (dto.isInvoice()) {
			if (!(dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)) {
				return "Invoice Number is empty";
			} else if (!(dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)) {
				return "DO Number is empty";
			} else if (!(dto.getDteInvDate() != null)) {
				return "Invoice Date is empty";
			}

			List<SlsTblTIR> lstTIR = getTIRListById(dto.getTxtDONo());
			if (!(lstTIR != null && lstTIR.size() > 0)) {
				return "DC Not found";
			}

			SlsTblTIR SlsTblTIR = lstTIR.get(0);
			EntityManager entityManager = getEntityManager();
			try {

//				SlsTblTIR.setTxtINVNo(dto.getTxtInvoiceNo());
//				SlsTblTIR.setNumInvoiceQuantity(new BigDecimal(dto.getInvQuantity()));
//				
//				SlsTblTIR.setTxtInvoiceStatus("Created");
				if (dto.getDteInvDate() != null)
					SlsTblTIR.setDteInvoiceDate(dto.getDteInvDate());
				entityManager.getTransaction().begin();
				SlsTblTIR.setBlIsDeleted(false);
//				SlsTblTIR.setDteInvcreateddate(commonService.getCurrentTimeStamp_new());

				entityManager.merge(SlsTblTIR);
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(SlsTblTIR);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		} else {
			if (!(dto.getTxtSaleOrderNo() != null && dto.getTxtSaleOrderNo().trim().length() > 0)) {
				return "Sale Order Number is empty";
			} else if (!(dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)) {
				return "DO Number is empty";
			} else if (!(dto.getDteDate() != null)) {
				return "DO Date is empty";
			}

			List<SlsTblSaleOrder> lstOrder = slsTblSaleOrderDAO.getSaleOrderBySapId(dto.getTxtSaleOrderNo());
			if (!(lstOrder != null && lstOrder.size() > 0)) {
				return "Sale Order Not found";
			}

			SlsTblSaleOrder slsTblSaleOrder = lstOrder.get(0);
			EntityManager entityManager = getEntityManager();
			try {

				SlsTblTIR SlsTblTIR = new SlsTblTIR();
//				SlsTblTIR.setSlsTblSaleOrder(slsTblSaleOrder);
				if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
					SlsTblTIR.setTxtTIRNo(dto.getTxtDONo());
//				if( dto.getQuantity() >0)
//				SlsTblTIR.setNumQuantity(new BigDecimal(dto.getQuantity()));

//				if( dto.getTareWeight() >0)
//				SlsTblTIR.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//				
//				if( dto.getGrossWeight() >0)
//				SlsTblTIR.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//				
//				if( dto.getNetWeight() >0)
//				SlsTblTIR.setNumNetWeight(new BigDecimal(dto.getNetWeight()));

//				if(dto.getTxtStatus() !=null && dto.getTxtStatus().trim().length()>0)
//				{
//					SlsTblTIR.setTxtStatus(dto.getTxtStatus().trim());
//				}
//				else
				SlsTblTIR.setTxtStatus("Created");
				if (dto.getDteDate() != null)
					SlsTblTIR.setDteDate(dto.getDteDate());
				entityManager.getTransaction().begin();
				SlsTblTIR.setBlIsDeleted(false);
				SlsTblTIR.setDteCreateddate(commonService.getCurrentTimeStamp_new());

				entityManager.persist(SlsTblTIR);

//				SlsTblSaleOrder slsTblTIR=SlsTblTIR.getSlsTblSaleOrder();
//				slsTblTIR.setBlnIsCompleted(true);
//				if(slsTblTIR.getTxtTIRNo()!=null && slsTblTIR.getTxtTIRNo().trim().length() >0 )
//						slsTblTIR.setTxtTIRNo(slsTblTIR.getTxtTIRNo()+","+SlsTblTIR.getTxtTIRNo());
//				else
//					slsTblTIR.setTxtTIRNo(SlsTblTIR.getTxtTIRNo());
//				slsTblTIR.setDteDate(SlsTblTIR.getDteDate());
//				entityManager.merge(slsTblTIR);

				entityManager.getTransaction().commit();
				entityManager.close();
				// setLedgerandStockEnteriesOnIssuance(SlsTblTIR);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
	}

	@Override
	public String updateTIR(DCDTO dto) {

		if (dto.isInvoice()) {
			if (dto.getTxtStatus().equalsIgnoreCase("Completed") || dto.getTxtStatus().equalsIgnoreCase("Cancelled")
					|| dto.getTxtStatus().equalsIgnoreCase("Cancel")) {

//				||
//				dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//				|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//				|| dto.getTxtStatus().equalsIgnoreCase("Deleted")

				List<SlsTblTIR> lstDetail = new ArrayList();
				if (dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)
					lstDetail = getTIRListByInvoiceId(dto.getTxtInvoiceNo());
				else
					return "Invoice Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "Invoice Not Found";
				}
				
				
				
				

				SlsTblTIR slsTblTIR = lstDetail.get(0);
				/*
				 * if (slsTblTIR.getTxtStatus() != null &&
				 * slsTblTIR.getTxtStatus().trim().length() > 0) { if
				 * (slsTblTIR.getTxtInvoiceStatus().equalsIgnoreCase("Approved")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblTIR.getTxtInvoiceStatus().equalsIgnoreCase("Approve")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblTIR.getTxtInvoiceStatus().equalsIgnoreCase("Rejected")) return
				 * "Invoice Already Rejected"; else if
				 * (slsTblTIR.getTxtInvoiceStatus().equalsIgnoreCase("Cancel")) return
				 * "Invoice Already Cancel"; else if
				 * (slsTblTIR.getTxtInvoiceStatus().equalsIgnoreCase("Deleted")) return
				 * "Invoice Already Deleted"; }
				 */

				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();

//					slsTblTIR.setDteInvModifieddate(commonService.getCurrentTimeStamp_new());
//					if( dto.getTxtStatus().equalsIgnoreCase("Cancelled") ||
//					dto.getTxtStatus().equalsIgnoreCase("Cancel") )
//					{
//						slsTblTIR.setTxtInvoiceStatus(null);
//						slsTblTIR.setTxtINVNo(null);
//						slsTblTIR.setDteInvoiceDate(null);
//					}
//					else
//					slsTblTIR.setTxtInvoiceStatus(dto.getTxtStatus());

					entityManager.merge(slsTblTIR);

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
		} else {
			if (!dto.isUpdate())
				if (dto.getTxtStatus() != null && dto.getTxtStatus().trim().length() > 0) {
				} else {
					return "Status is empty";
				}

//			if (dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//					|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//					|| dto.getTxtStatus().equalsIgnoreCase("Deleted")) 
			if (!dto.isUpdate())
				if (dto.getTxtStatus().equalsIgnoreCase("Completed") || dto.getTxtStatus().equalsIgnoreCase("Deleted")
						|| dto.getTxtStatus().equalsIgnoreCase("Reversed")) {

				} else {
					return "Status is not Valid";
				}

			List<SlsTblTIR> lstDetail = new ArrayList();
			if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
				lstDetail = getTIRListById(dto.getTxtDONo());
			else
				return "DO Number is empty";

			if (!(lstDetail != null && lstDetail.size() > 0)) {
				return "DO Not Found";
			}
			SlsTblTIR slsTblTIR = lstDetail.get(0);

			/*
			 * if (slsTblTIR.getTxtStatus() != null &&
			 * slsTblTIR.getTxtStatus().trim().length() > 0) { if
			 * (slsTblTIR.getTxtStatus().equalsIgnoreCase("Approved")) return
			 * "DO Already Approved"; else if
			 * (slsTblTIR.getTxtStatus().equalsIgnoreCase("Approve")) return
			 * "DO Already Approved"; else if
			 * (slsTblTIR.getTxtStatus().equalsIgnoreCase("Rejected")) return
			 * "DO Already Rejected"; else if
			 * (slsTblTIR.getTxtStatus().equalsIgnoreCase("Cancel")) return
			 * "DO Already Cancel"; else if
			 * (slsTblTIR.getTxtStatus().equalsIgnoreCase("Deleted")) return
			 * "DO Already Deleted"; }
			 */
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblTIR.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//					if(dto.isUpdate())
//					{

//					if( dto.getQuantity() >0)
//						slsTblTIR.setNumQuantity(new BigDecimal(dto.getQuantity()));
//					
//					if( dto.getTareWeight() >0)
//						slsTblTIR.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//					
//					if( dto.getGrossWeight() >0)
//						slsTblTIR.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//					
//					if( dto.getNetWeight() >0)
//						slsTblTIR.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					
////						slsTblTIR.setNumQuantity(new BigDecimal(dto.getQuantity()));
////						slsTblTIR.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
////						slsTblTIR.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
////						slsTblTIR.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					}

				if (dto.getTxtStatus() != null && dto.getTxtStatus().trim().length() > 1)
					slsTblTIR.setTxtStatus(dto.getTxtStatus());

//					if(dto.getTxtInvStatus()!=null && dto.getTxtInvStatus().trim().length() >0)
//					   slsTblTIR.setTxtInvoiceStatus(dto.getTxtInvStatus());
//					
//					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() >0)
//						   slsTblTIR.setTxtINVNo(dto.getTxtInvoiceNo());

				if (dto.getDteInvDate() != null)
					slsTblTIR.setDteInvoiceDate(dto.getDteInvDate());

				entityManager.merge(slsTblTIR);

				entityManager.getTransaction().commit();
				entityManager.close();
				return "DO Updated successfully.";

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}

		}
	}

	public List<SlsTblTIR> getTIRListById(String TIRId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIR where txtTIRCode='" + TIRId + "'";

			List<SlsTblTIR> TIR = entityManager.createQuery(query).getResultList();

			return TIR;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public List<SlsTblTIR> getTIRListByInvoiceId(String InvoiceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblTIR where txtINVNo='" + InvoiceId + "'";

			List<SlsTblTIR> TIR = entityManager.createQuery(query).getResultList();

			return TIR;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	@Override
	public SlsTblTIR getPrevious(String vehicle) {

		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		{

			try {
				entityManager.getTransaction().begin();

				SlsTblTIR slsTblTIR = (SlsTblTIR) entityManager
						.createQuery(
								"SELECT b FROM SlsTblTIR b where b.slsTblSoVehicleDetail.serSoVehicleDetailId = "
										+ vehicle + "  ORDER BY b.serTIRId DESC")
						.setMaxResults(1).getSingleResult();

				if (slsTblTIR != null)
					return slsTblTIR;
				else
					return new SlsTblTIR();
			} catch (Exception e) {
				e.printStackTrace();

			}
			SlsTblTIR sls=new SlsTblTIR();
			sls.setNumCurrentMillage(new BigDecimal(0));
			return sls;

		}
	}
	
	@Override
	public String uploadTIRDocument(SlsTblTIRDocument tirDocument) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		SlsTblTIR tir = entityManager.find(SlsTblTIR.class,
				tirDocument.getSlsTblTIR().getSerTIRId());
		if (tir == null) {
			return "Failure";
		}
		tirDocument.setSlsTblTIR(tir);
		entityManager.persist(tirDocument);
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
	}

	@Override
	public byte[] downloadDocument(int documentId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		SlsTblTIRDocument paymentDocument = entityManager.find(SlsTblTIRDocument.class, documentId);
		entityManager.getTransaction().commit();
		entityManager.close();
		return paymentDocument.getDocumentFile();
	}

	@Override
	public String removeCandidateDocument(int documentId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		SlsTblTIRDocument paymentDocument = entityManager.find(SlsTblTIRDocument.class, documentId);
		entityManager.remove(paymentDocument);
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
	}

	@Override
	public List<SlsTblTIRDocument> getSOPaymentDocumentList(int id) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblTIRDocument> rcsCandidateDocumnets = entityManager
				.createQuery("FROM SlsTblTIRDocument ff where ff.slsTblTIR.serTIRId=" + id)
				.getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();

		return rcsCandidateDocumnets;
	}
	
	
	
	@Override
	public byte[] getTIRPicture(String tirid,String id)
	{
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    SlsTblTIR tir = entityManager.find(SlsTblTIR.class, Integer.parseInt(tirid));
	    entityManager.getTransaction().commit();
	    entityManager.close();
	    if(tir!=null)
	    {
	    	if(id.equalsIgnoreCase("1"))
    		{
	    		if(tir.getPic1()!=null)
	 	    	    return tir.getPic1();
    		}
	    	else if(id.equalsIgnoreCase("2"))
    		{
	    		if(tir.getPic2()!=null)
	 	    	    return tir.getPic2();
    		}
	    	else if(id.equalsIgnoreCase("3"))
    		{
	    		if(tir.getPic3()!=null)
	 	    	    return tir.getPic3();
    		}
	    	
	    		
	    	
	 	    }
	    
	 
	    	return null;
	}
	
	
}
