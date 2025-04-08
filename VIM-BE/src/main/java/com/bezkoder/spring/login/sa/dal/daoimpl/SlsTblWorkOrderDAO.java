package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.Date;
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

import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductDesign;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductQuality;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaim;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaimDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrder;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrderDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;


@Repository
public class SlsTblWorkOrderDAO implements ISlsTblWorkOrderDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	@Autowired
	private LoginDAO loginDao;

	@Autowired
	private ISlsTblSaleOrderDAO slsTblSaleOrderDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblWorkOrderDAO.class);

	public SlsTblWorkOrderDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrder> getAllWorkOrder() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblWorkOrder> WorkOrders = entityManager.createQuery("FROM SlsTblWorkOrder where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrder> getActiveWorkOrder() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblWorkOrder> WorkOrders = entityManager
				.createQuery("FROM SlsTblWorkOrder where blnStatus=1 and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrder> getWorkOrderByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblWorkOrder where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblWorkOrder> WorkOrders = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return WorkOrders;
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
	public String addNewWorkOrder(SlsTblWorkOrder SlsTblWorkOrder) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
//			SlsTblWorkOrder.setBlnStatus(true);
			SlsTblWorkOrder.setBlIsDeleted(false);
			SlsTblWorkOrder.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblWorkOrder.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			
			try {
				SlsTblWorkOrder.setSerGroupId(
						this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle exception
			}
			if (SlsTblWorkOrder.getSlsTblSoVehicleDetail() != null) {

				SlsTblWorkOrder.setCfgTblCustomer(SlsTblWorkOrder.getSlsTblSoVehicleDetail().getCfgTblCustomer());
				SlsTblWorkOrder.setCfgTblProduct(SlsTblWorkOrder.getSlsTblSoVehicleDetail().getCfgTblProduct());

				SlsTblWorkOrder.setCfgTblModel(SlsTblWorkOrder.getSlsTblSoVehicleDetail().getCfgTblModel());
				SlsTblWorkOrder.setCfgTblColor(SlsTblWorkOrder.getSlsTblSoVehicleDetail().getCfgTblColor());
			}

			entityManager.persist(SlsTblWorkOrder);

			if (SlsTblWorkOrder.getSlsTblWorkOrderDetail() != null
					&& SlsTblWorkOrder.getSlsTblWorkOrderDetail().size() > 0) {
				SlsTblWorkOrderDetail workOrderDetail;
				Iterator<SlsTblWorkOrderDetail> itr = SlsTblWorkOrder.getSlsTblWorkOrderDetail().iterator();

				while (itr.hasNext()) {

					workOrderDetail = (SlsTblWorkOrderDetail) itr.next();
					workOrderDetail.setBlIsService(false);
					workOrderDetail.setBlIsSP(true);
					if (workOrderDetail.getNumQuantity() != null
							&& workOrderDetail.getNumQuantity().doubleValue() > 0) {
						if (!(workOrderDetail.getCfgTblProductDesign() != null
								&& workOrderDetail.getCfgTblProductDesign().getSerProductDesignId() > 0)) {
							workOrderDetail.setCfgTblProductDesign(null);
						}
						workOrderDetail.setBlIsDeleted(false);
//					workOrderDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						workOrderDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						workOrderDetail.setSlsTblWorkOrder(SlsTblWorkOrder);
//					ProTblProductionSummary proTblProductionSummary=

//					workOrderDetail.setNumUnitWt(proTblProductionSummary.getNumUnitWt());
						entityManager.persist(workOrderDetail);
					}

				}
			}

			if (SlsTblWorkOrder.getSlsTblWorkOrderDetailservices() != null
					&& SlsTblWorkOrder.getSlsTblWorkOrderDetail().size() > 0) {
				SlsTblWorkOrderDetail workOrderDetail;
				Iterator<SlsTblWorkOrderDetail> itr = SlsTblWorkOrder.getSlsTblWorkOrderDetailservices().iterator();

				while (itr.hasNext()) {

					workOrderDetail = (SlsTblWorkOrderDetail) itr.next();
					workOrderDetail.setBlIsService(true);
					workOrderDetail.setBlIsSP(false);
					if (!(workOrderDetail.getNumQuantity() != null
							&& workOrderDetail.getNumQuantity().doubleValue() > 0)) {
						workOrderDetail.setNumQuantity(new BigDecimal(1));
					}
					if (workOrderDetail.getNumQuantity() != null
							&& workOrderDetail.getNumQuantity().doubleValue() > 0) {
						if (!(workOrderDetail.getCfgTblProductDesign() != null
								&& workOrderDetail.getCfgTblProductDesign().getSerProductDesignId() > 0)) {
							workOrderDetail.setCfgTblProductDesign(null);
						}
						workOrderDetail.setBlIsDeleted(false);
//					workOrderDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						workOrderDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						workOrderDetail.setSlsTblWorkOrder(SlsTblWorkOrder);
//					ProTblProductionSummary proTblProductionSummary=

//					workOrderDetail.setNumUnitWt(proTblProductionSummary.getNumUnitWt());
						entityManager.persist(workOrderDetail);
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
	public String deleteWorkOrder(List<String> WorkOrdersId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serWorkOrderId : WorkOrdersId) {
				SlsTblWorkOrder WorkOrder = entityManager.find(SlsTblWorkOrder.class, Integer.parseInt(serWorkOrderId));
				if (WorkOrder != null) {
					WorkOrder.setBlIsDeleted(true);

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
	public String updateWorkOrder(SlsTblWorkOrder slsTblWorkOrder) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblWorkOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblWorkOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());

			entityManager.merge(slsTblWorkOrder);
			SlsTblWorkOrderDetail workOrderDetail;

			boolean status = true;
//			if(slsTblWorkOrder.getTxtStatus()!=null && slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Approve"))
//			{
//				List<SlsTblWorkOrderDetail> lstWorkOrderDetail=searchWorkOrderDetail(slsTblWorkOrder.getSerWorkOrderId());
//				if(slsTblWorkOrder.getBlnIsApproved()!=null && slsTblWorkOrder.getBlnIsApproved())
//				{
//					slsTblWorkOrder.setSlsTblWorkOrderDetails(lstWorkOrderDetail);
//					/*Iterator<SlsTblWorkOrderDetail> itr = lstWorkOrderDetail.iterator();
//					while (itr.hasNext())
//					{
//						workOrderDetail = (SlsTblWorkOrderDetail) itr.next();
//						if(workOrderDetail.getNumStockAvailabe().doubleValue() < workOrderDetail.getNumQuantity().doubleValue())
//						{
//							status=false;
//						}
//					}*/
//				}
//				
//			}

//			else 

			{
				if (slsTblWorkOrder.getSlsTblWorkOrderDetail() != null) {
					Iterator<SlsTblWorkOrderDetail> itr = slsTblWorkOrder.getSlsTblWorkOrderDetail().iterator();

					while (itr.hasNext()) {
						workOrderDetail = (SlsTblWorkOrderDetail) itr.next();
						if (workOrderDetail.getSerWorkOrderDetailId() != null
								&& workOrderDetail.getSerWorkOrderDetailId() > 0) {
							if (workOrderDetail.getNumQuantity() != null
									&& workOrderDetail.getNumQuantity().doubleValue() > 0) {
								workOrderDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								workOrderDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							} else {
								workOrderDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								workOrderDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
								workOrderDetail.setBlIsDeleted(true);
							}

							entityManager.merge(workOrderDetail);
						} else {
							if (workOrderDetail.getNumQuantity() != null
									&& workOrderDetail.getNumQuantity().doubleValue() > 0) {
								workOrderDetail.setBlIsDeleted(false);
								workOrderDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
								workOrderDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
								workOrderDetail.setSlsTblWorkOrder(slsTblWorkOrder);

								entityManager.persist(workOrderDetail);
							}
						}
					}
				}
			}
			if (status) {
				entityManager.getTransaction().commit();
				entityManager.close();

				if (slsTblWorkOrder.getTxtStatus() != null
						&& slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Approve"))
					setLedgerandStockEnteriesOnIssuance(slsTblWorkOrder);
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
	public String generateWorkOrderNo(String type) {
		// int WorkOrderNo;
		String WorkOrderType = type;
		// String WorkOrderCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (WorkOrderType.equals("1"))
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if (user.getSerGroupId() != null && user.getSerGroupId() == 2) {
			try {

				if (WorkOrderType.equals("GP")) {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String)
					 * entityManager.createQuery("select MAX(txtGatePassNo) from SlsTblWorkOrder ")
					 * .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtGatePassNo FROM SlsTblWorkOrder b where serGroupId= "
									+ user.getSerGroupId() + "  ORDER BY b.serWorkOrderId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "JC-00";
					}
					ord_no = Integer.valueOf(zoneCode.substring(3));

					ord_no = ord_no + 1;
					String code = "JC-1";
					if (ord_no < 10)
						code = "JC-00" + ord_no;
					else if (ord_no > 9 && ord_no < 100)
						code = "JC-0" + ord_no;
					else
						code = "JC-" + ord_no;
					return code;
				} else {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String) entityManager.
					 * createQuery("select MAX(txtWorkOrderCode) from SlsTblWorkOrder where serGroupId= "
					 * +user.getSerGroupId()) .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtWorkOrderNo FROM SlsTblWorkOrder b  where serGroupId= "
									+ user.getSerGroupId() + " ORDER BY b.serWorkOrderId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "JC-00";
					}
					ord_no = Integer.valueOf(zoneCode.substring(7));

					ord_no = ord_no + 1;
					String code = "JC-1";
					if (ord_no < 10)
						code = "JC-00" + ord_no;
					else if (ord_no > 9 && ord_no < 100)
						code = "JC-0" + ord_no;
					else
						code = "JC-" + ord_no;
					return code;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			if (WorkOrderType.equals("GP")) {
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String)
					 * entityManager.createQuery("select MAX(txtGatePassNo) from SlsTblWorkOrder ")
					 * .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtWorkOrderNo FROM SlsTblWorkOrder b where serGroupId= "
									+ user.getSerGroupId() + "  ORDER BY b.serWorkOrderId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "JC-00";
					}
					ord_no = Integer.valueOf(zoneCode.substring(3));

					ord_no = ord_no + 1;
					String code = "JC-1";
					if (ord_no < 10)
						code = "JC-00" + ord_no;
					else if (ord_no > 9 && ord_no < 100)
						code = "JC-0" + ord_no;
					else
						code = "JC-" + ord_no;
					return code;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return "";
			} else {
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String) entityManager.
					 * createQuery("select MAX(txtWorkOrderCode) from SlsTblWorkOrder ")
					 * .getSingleResult();
					 */
					String zoneCode = (String) entityManager
							.createQuery(
									"SELECT b.txtWorkOrderNo FROM SlsTblWorkOrder b   ORDER BY b.serWorkOrderId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "JC-00";
					}
					ord_no1 = Integer.valueOf(zoneCode.substring(3));

					ord_no1 = ord_no1 + 1;
					String code = "JC-1";
					if (ord_no1 < 10)
						code = "JC-00" + ord_no1;
					else if (ord_no1 > 9 && ord_no1 < 100)
						code = "JC-0" + ord_no1;
					else
						code = "JC-" + ord_no1;
					return code;
				} catch (Exception e) {
					e.printStackTrace();

				}
				return "JC-001";
			}
		}
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getWorkOrderById(String WorkOrderId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblWorkOrder where txtWorkOrderNo='" + WorkOrderId + "'";

			List<SlsTblWorkOrder> WorkOrder = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (WorkOrder.size() > 0) {
				return String.valueOf(WorkOrder.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	public SlsTblWorkOrder getWorkOrderByCode(String WorkOrderId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblWorkOrder where txtWorkOrderNo='" + WorkOrderId + "'";

			List<SlsTblWorkOrder> WorkOrder = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (WorkOrder.size() > 0) {
				return WorkOrder.get(0);
			}
			return null;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public List<SlsTblWorkOrder> searchWorkOrder(SlsTblWorkOrder WorkOrder) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
//		and WorkOrder.txtStatus in ( 'Created','Completed')
		String query = "from SlsTblWorkOrder WorkOrder where 1=1  ";
		if (WorkOrder.getTxtWorkOrderNo() != null) {
			query += " and upper(WorkOrder.txtWorkOrderNo) like" + " upper('" + WorkOrder.getTxtWorkOrderNo() + "%')"
					+ " ";
		}

		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			query += " and WorkOrder.serGroupId = " + user.getSerGroupId() + " ";

		}

		if (WorkOrder.getSerWorkOrderId() != null) {
			query += " and WorkOrder.serWorkOrderId =" + " " + WorkOrder.getSerWorkOrderId() + "" + "  ";
		}

		if (WorkOrder.getTxtStatus() != null && WorkOrder.getTxtStatus().trim().length() > 0) {
			query += " and WorkOrder.txtStatus = 'Approved'  ";
		}

		if (WorkOrder.getDte_date_from() != null && WorkOrder.getDte_date_from().trim().length() > 0) {
			try {

				query += " and WorkOrder.dteDate >= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(WorkOrder.getDte_date_from()))
						+ "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (WorkOrder.getDte_date_to() != null && WorkOrder.getDte_date_to().trim().length() > 0) {
			try {
				query += " and WorkOrder.dteDate <= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(WorkOrder.getDte_date_to()))
						+ "'";
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

				query += " and WorkOrder.cfgTblDealer.serCustomerId in"
						+ " (select serCustomerId from CfgTblCustomer customer  where customer.serCustomerId="
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ "  or customer.cfgTblGroupCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ " or customer.cfgTblCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + ")";
			} else {
				query += " and WorkOrder.cfgTblDealer.serCustomerId =" + " "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";

			}
		}
//		if (WorkOrder.getSlsTblSaleOrder()!= null && WorkOrder.getSlsTblSaleOrder().getSerSaleOrderId() !=null) {
//			query += " and WorkOrder.slsTblSaleOrder.serSaleOrderId =" + " " + WorkOrder.getSlsTblSaleOrder().getSerSaleOrderId() + "" + "  ";
//		}

		query += " order by WorkOrder.serWorkOrderId  DESC";
		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblWorkOrder> cust = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return cust;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrderDetail> searchWorkOrderDetail(int WorkOrderId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (WorkOrderId > 0)
			query = "from SlsTblWorkOrderDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblWorkOrder.serWorkOrderId= "
					+ WorkOrderId;

		else
			return null;

		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblWorkOrderDetail> lstDC = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		SlsTblWorkOrderDetail workOrderDetail;
		Iterator<SlsTblWorkOrderDetail> itr = lstDC.iterator();

		while (itr.hasNext()) {

			workOrderDetail = (SlsTblWorkOrderDetail) itr.next();
			workOrderDetail.setNumStockAvailabe(new BigDecimal(0));
			double available_qty = 0;

			try {
				if (!(workOrderDetail.getCfgTblProductDesign() != null
						&& workOrderDetail.getCfgTblProductDesign().getSerProductDesignId() != null)) {
					CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
					cfgTblProductDesign.setSerProductDesignId(0);
					workOrderDetail.setCfgTblProductDesign(cfgTblProductDesign);
				}

				if (!(workOrderDetail.getCfgTblProductQuality() != null
						&& workOrderDetail.getCfgTblProductDesign().getSerProductDesignId() != null)) {
					CfgTblProductQuality cfgTblProductQuality = new CfgTblProductQuality();
					cfgTblProductQuality.setSerProductQualityId(0);
					workOrderDetail.setCfgTblProductQuality(cfgTblProductQuality);
				}

				available_qty = 0;/*
									 * proTblProductionDetailDAO.getProductAvailableQty(
									 * workOrderDetail.getCfgTblProduct().getSerProductId(),
									 * workOrderDetail.getCfgTblProductDesign().getSerProductDesignId(),
									 * workOrderDetail.getCfgTblProductQuality().getSerProductQualityId());
									 */

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (available_qty > 0)
				workOrderDetail.setNumStockAvailabe(new BigDecimal(available_qty));
		}
		return lstDC;

	}

	public void setLedgerandStockEnteriesOnIssuance(SlsTblWorkOrder slsTblWorkOrder) {

	}

	@Override
	public String AssignGatePassNumber(SlsTblWorkOrder slsTblWorkOrder) {
		EntityManager entityManager = getEntityManager();
		try {
			String GP_No = generateWorkOrderNo("GP");
			entityManager.getTransaction().begin();
			slsTblWorkOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblWorkOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//			slsTblWorkOrder.setTxtGatePassNo(GP_No);
			entityManager.merge(slsTblWorkOrder);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

//	String addNewWorkOrder(DCDTO slsTblWorkOrder);
	@Override
	public String addNewWorkOrder(DCDTO dto) {

		if (dto.isInvoice()) {
			if (!(dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)) {
				return "Invoice Number is empty";
			} else if (!(dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)) {
				return "DO Number is empty";
			} else if (!(dto.getDteInvDate() != null)) {
				return "Invoice Date is empty";
			}

			List<SlsTblWorkOrder> lstWorkOrder = getWorkOrderListById(dto.getTxtDONo());
			if (!(lstWorkOrder != null && lstWorkOrder.size() > 0)) {
				return "DC Not found";
			}

			SlsTblWorkOrder SlsTblWorkOrder = lstWorkOrder.get(0);
			EntityManager entityManager = getEntityManager();
			try {

//				SlsTblWorkOrder.setTxtINVNo(dto.getTxtInvoiceNo());
//				SlsTblWorkOrder.setNumInvoiceQuantity(new BigDecimal(dto.getInvQuantity()));
//				
//				SlsTblWorkOrder.setTxtInvoiceStatus("Created");
				if (dto.getDteInvDate() != null)
					SlsTblWorkOrder.setDteInvoiceDate(dto.getDteInvDate());
				entityManager.getTransaction().begin();
				SlsTblWorkOrder.setBlIsDeleted(false);
//				SlsTblWorkOrder.setDteInvcreateddate(commonService.getCurrentTimeStamp_new());

				entityManager.merge(SlsTblWorkOrder);
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(SlsTblWorkOrder);
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

				SlsTblWorkOrder SlsTblWorkOrder = new SlsTblWorkOrder();
//				SlsTblWorkOrder.setSlsTblSaleOrder(slsTblSaleOrder);
				if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
					SlsTblWorkOrder.setTxtWorkOrderNo(dto.getTxtDONo());
//				if( dto.getQuantity() >0)
//				SlsTblWorkOrder.setNumQuantity(new BigDecimal(dto.getQuantity()));

//				if( dto.getTareWeight() >0)
//				SlsTblWorkOrder.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//				
//				if( dto.getGrossWeight() >0)
//				SlsTblWorkOrder.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//				
//				if( dto.getNetWeight() >0)
//				SlsTblWorkOrder.setNumNetWeight(new BigDecimal(dto.getNetWeight()));

//				if(dto.getTxtStatus() !=null && dto.getTxtStatus().trim().length()>0)
//				{
//					SlsTblWorkOrder.setTxtStatus(dto.getTxtStatus().trim());
//				}
//				else
				SlsTblWorkOrder.setTxtStatus("Created");
				if (dto.getDteDate() != null)
					SlsTblWorkOrder.setDteDate(dto.getDteDate());
				entityManager.getTransaction().begin();
				SlsTblWorkOrder.setBlIsDeleted(false);
				SlsTblWorkOrder.setDteCreateddate(commonService.getCurrentTimeStamp_new());

				entityManager.persist(SlsTblWorkOrder);

//				SlsTblSaleOrder slsTblWorkOrder=SlsTblWorkOrder.getSlsTblSaleOrder();
//				slsTblWorkOrder.setBlnIsCompleted(true);
//				if(slsTblWorkOrder.getTxtWorkOrderNo()!=null && slsTblWorkOrder.getTxtWorkOrderNo().trim().length() >0 )
//						slsTblWorkOrder.setTxtWorkOrderNo(slsTblWorkOrder.getTxtWorkOrderNo()+","+SlsTblWorkOrder.getTxtWorkOrderNo());
//				else
//					slsTblWorkOrder.setTxtWorkOrderNo(SlsTblWorkOrder.getTxtWorkOrderNo());
//				slsTblWorkOrder.setDteDate(SlsTblWorkOrder.getDteDate());
//				entityManager.merge(slsTblWorkOrder);

				entityManager.getTransaction().commit();
				entityManager.close();
				// setLedgerandStockEnteriesOnIssuance(SlsTblWorkOrder);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
	}

	@Override
	public String updateWorkOrder(DCDTO dto) {

		if (dto.isInvoice()) {
			if (dto.getTxtStatus().equalsIgnoreCase("Completed") || dto.getTxtStatus().equalsIgnoreCase("Cancelled")
					|| dto.getTxtStatus().equalsIgnoreCase("Cancel")) {

//				||
//				dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//				|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//				|| dto.getTxtStatus().equalsIgnoreCase("Deleted")

				List<SlsTblWorkOrder> lstDetail = new ArrayList();
				if (dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)
					lstDetail = getWorkOrderListByInvoiceId(dto.getTxtInvoiceNo());
				else
					return "Invoice Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "Invoice Not Found";
				}

				SlsTblWorkOrder slsTblWorkOrder = lstDetail.get(0);
				/*
				 * if (slsTblWorkOrder.getTxtStatus() != null &&
				 * slsTblWorkOrder.getTxtStatus().trim().length() > 0) { if
				 * (slsTblWorkOrder.getTxtInvoiceStatus().equalsIgnoreCase("Approved")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblWorkOrder.getTxtInvoiceStatus().equalsIgnoreCase("Approve")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblWorkOrder.getTxtInvoiceStatus().equalsIgnoreCase("Rejected")) return
				 * "Invoice Already Rejected"; else if
				 * (slsTblWorkOrder.getTxtInvoiceStatus().equalsIgnoreCase("Cancel")) return
				 * "Invoice Already Cancel"; else if
				 * (slsTblWorkOrder.getTxtInvoiceStatus().equalsIgnoreCase("Deleted")) return
				 * "Invoice Already Deleted"; }
				 */

				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();

//					slsTblWorkOrder.setDteInvModifieddate(commonService.getCurrentTimeStamp_new());
//					if( dto.getTxtStatus().equalsIgnoreCase("Cancelled") ||
//					dto.getTxtStatus().equalsIgnoreCase("Cancel") )
//					{
//						slsTblWorkOrder.setTxtInvoiceStatus(null);
//						slsTblWorkOrder.setTxtINVNo(null);
//						slsTblWorkOrder.setDteInvoiceDate(null);
//					}
//					else
//					slsTblWorkOrder.setTxtInvoiceStatus(dto.getTxtStatus());

					entityManager.merge(slsTblWorkOrder);

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

			List<SlsTblWorkOrder> lstDetail = new ArrayList();
			if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
				lstDetail = getWorkOrderListById(dto.getTxtDONo());
			else
				return "DO Number is empty";

			if (!(lstDetail != null && lstDetail.size() > 0)) {
				return "DO Not Found";
			}
			SlsTblWorkOrder slsTblWorkOrder = lstDetail.get(0);

			/*
			 * if (slsTblWorkOrder.getTxtStatus() != null &&
			 * slsTblWorkOrder.getTxtStatus().trim().length() > 0) { if
			 * (slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Approved")) return
			 * "DO Already Approved"; else if
			 * (slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Approve")) return
			 * "DO Already Approved"; else if
			 * (slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Rejected")) return
			 * "DO Already Rejected"; else if
			 * (slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Cancel")) return
			 * "DO Already Cancel"; else if
			 * (slsTblWorkOrder.getTxtStatus().equalsIgnoreCase("Deleted")) return
			 * "DO Already Deleted"; }
			 */
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblWorkOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//					if(dto.isUpdate())
//					{

//					if( dto.getQuantity() >0)
//						slsTblWorkOrder.setNumQuantity(new BigDecimal(dto.getQuantity()));
//					
//					if( dto.getTareWeight() >0)
//						slsTblWorkOrder.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//					
//					if( dto.getGrossWeight() >0)
//						slsTblWorkOrder.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//					
//					if( dto.getNetWeight() >0)
//						slsTblWorkOrder.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					
////						slsTblWorkOrder.setNumQuantity(new BigDecimal(dto.getQuantity()));
////						slsTblWorkOrder.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
////						slsTblWorkOrder.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
////						slsTblWorkOrder.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					}

				if (dto.getTxtStatus() != null && dto.getTxtStatus().trim().length() > 1)
					slsTblWorkOrder.setTxtStatus(dto.getTxtStatus());

//					if(dto.getTxtInvStatus()!=null && dto.getTxtInvStatus().trim().length() >0)
//					   slsTblWorkOrder.setTxtInvoiceStatus(dto.getTxtInvStatus());
//					
//					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() >0)
//						   slsTblWorkOrder.setTxtINVNo(dto.getTxtInvoiceNo());

				if (dto.getDteInvDate() != null)
					slsTblWorkOrder.setDteInvoiceDate(dto.getDteInvDate());

				entityManager.merge(slsTblWorkOrder);

				entityManager.getTransaction().commit();
				entityManager.close();
				return "DO Updated successfully.";

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}

		}
	}

	public List<SlsTblWorkOrder> getWorkOrderListById(String WorkOrderId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblWorkOrder where txtWorkOrderCode='" + WorkOrderId + "'";

			List<SlsTblWorkOrder> WorkOrder = entityManager.createQuery(query).getResultList();

			return WorkOrder;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public List<SlsTblWorkOrder> getWorkOrderListByInvoiceId(String InvoiceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblWorkOrder where txtINVNo='" + InvoiceId + "'";

			List<SlsTblWorkOrder> WorkOrder = entityManager.createQuery(query).getResultList();

			return WorkOrder;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public SlsTblWorkOrder getPrevious(String vehicle) {

		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		{

			try {
				entityManager.getTransaction().begin();

				SlsTblWorkOrder slsTblWorkOrder = (SlsTblWorkOrder) entityManager
						.createQuery(
								"SELECT b FROM SlsTblWorkOrder b where b.slsTblSoVehicleDetail.serSoVehicleDetailId = "
										+ vehicle + "  ORDER BY b.serWorkOrderId DESC")
						.setMaxResults(1).getSingleResult();

				if (slsTblWorkOrder != null)
					return slsTblWorkOrder;
				else
					return new SlsTblWorkOrder();
			} catch (Exception e) {
				e.printStackTrace();

			}
			SlsTblWorkOrder sls=new SlsTblWorkOrder();
			sls.setNumCurrentMillage(new BigDecimal(0));
			return sls;

		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrder> getWorkOrderForPostServiceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblWorkOrder> WorkOrders = entityManager.createQuery("FROM SlsTblWorkOrder where blIsDeleted=FALSE and (blIsPostServiceFollowUp = false or blIsPostServiceFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblWorkOrder> getWorkOrderForMaintinanceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblWorkOrder> WorkOrders = entityManager.createQuery("FROM SlsTblWorkOrder where blIsDeleted=FALSE and (blIsPostServiceFollowUp = false or blIsPostServiceFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return WorkOrders;
	}
	
	@Override
	public SlsTblClaim addNewClaim(SlsTblClaim slsTblClaim) {
		EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblClaim.setBlIsDeleted(false);
				slsTblClaim.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblClaim.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				
				slsTblClaim.setTxtClaimNo(generateClaimNo("SP"));
				
				CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
				if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
					slsTblClaim.setSerGroupId(user.getSerGroupId());
				}
				
				entityManager.persist(slsTblClaim);
				
				SlsTblClaimDetail soDetail;
				Iterator<SlsTblClaimDetail> itr = slsTblClaim.getSlsTblClaimDetail().iterator();
				SlsTblClaimDetail details ;
				CfgTblProduct product = new CfgTblProduct();
				double order_Qty = 0;
				while (itr.hasNext()) {

					soDetail = (SlsTblClaimDetail) itr.next();
					
		
						soDetail.setBlIsDeleted(false);
						soDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						soDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						soDetail.setSlsTblClaim(slsTblClaim);

						entityManager.persist(soDetail);
						
				}
				
				
				
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(SlsTblWorkOrder);
				return slsTblClaim;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return slsTblClaim;
			}
			
		}
	

	

	public SlsTblClaim addNewClaimSerivce(SlsTblClaim slsTblClaim) {
		EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblClaim.setBlIsDeleted(false);
				slsTblClaim.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblClaim.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				
				slsTblClaim.setTxtClaimNo(generateClaimNo("Service"));
				slsTblClaim.setBlnIsService(true);
				CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
				if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
					slsTblClaim.setSerGroupId(user.getSerGroupId());
				}
				
				entityManager.persist(slsTblClaim);
				
				SlsTblClaimDetail soDetail;
				Iterator<SlsTblClaimDetail> itr = slsTblClaim.getSlsTblClaimDetail().iterator();
				SlsTblClaimDetail details = new SlsTblClaimDetail();
				CfgTblProduct product = new CfgTblProduct();
				double order_Qty = 0;
				while (itr.hasNext()) {

					soDetail = (SlsTblClaimDetail) itr.next();
					details = soDetail;
		
						soDetail.setBlIsDeleted(false);
						soDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						soDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						soDetail.setSlsTblClaim(slsTblClaim);

						entityManager.persist(soDetail);
						
				}
				
				
				
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(SlsTblWorkOrder);
				return slsTblClaim;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return slsTblClaim;
			}
			
		}
	
	@Override
	public String createClaimfromWorkOrder(List<String> WorkOrdersId,String Type) {
		SlsTblClaim retclaim=new SlsTblClaim();
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			List<SlsTblClaimDetail> lstClaim =new ArrayList();
			SlsTblClaim claim= new SlsTblClaim();
			double Total=0;
			double CLAIM_Total=0;
			double Tax_Total=0;
			
			double Total_Service=0;
			double CLAIM_Total_service=0;
			double Tax_Total_Service=0;
			
			for (String serWorkOrderId : WorkOrdersId) {
	    
				SlsTblWorkOrder WorkOrder = entityManager.find(SlsTblWorkOrder.class, Integer.parseInt(serWorkOrderId));
				List<SlsTblWorkOrderDetail> lstDetail = searchWorkOrderDetail(Integer.parseInt(serWorkOrderId));
				double ST=0.0;
				double ST_Service=0.0;
				if (lstDetail != null && lstDetail.size() > 0) {
					SlsTblClaimDetail detail = new SlsTblClaimDetail();
					for (SlsTblWorkOrderDetail dto : lstDetail) {
//						if (Type.equalsIgnoreCase("SP")) {
//							if (dto.getBlIsService())
//								continue;
//						} else {
//							if (dto.getBlIsSP())
//								continue;
//						}
						if (dto != null) {
							double qty = 1;
							double price = 1;
							if (dto.getNumQuantity() != null && dto.getNumQuantity().doubleValue() > 0) {
								qty = dto.getNumQuantity().doubleValue();
							}

							if (dto.getNumPricePerPiece() != null && dto.getNumPricePerPiece().doubleValue() > 0) {
								price = dto.getNumPricePerPiece().doubleValue();
							}
							if (dto.getBlIsSP() != null && dto.getBlIsSP()) {

								Total = Total + (qty * price);
							} else {
								Total_Service = Total_Service + (qty * price);
							}
						}

						detail = new SlsTblClaimDetail();
						detail.setCfgTblDealer(dto.getSlsTblWorkOrder().getCfgTblDealer());
						detail.setCfgTblCustomer(dto.getSlsTblWorkOrder().getCfgTblCustomer());
						detail.setBlIsSP(true);
						detail.setNumAmount(new BigDecimal(Total));
						detail.setNumServiceAmount(new BigDecimal(Total_Service));
						detail.setSlsTblSoVehicleDetail(dto.getSlsTblWorkOrder().getSlsTblSoVehicleDetail());
						detail.setCfgTblProduct(dto.getCfgTblProduct());
						detail.setTxtReason(dto.getSlsTblWorkOrder().getTxtRemarks());
						detail.setSlsTblWorkOrder(dto.getSlsTblWorkOrder());
						
						

					}
					CLAIM_Total = CLAIM_Total + Total;
					CLAIM_Total_service = CLAIM_Total_service + Total_Service;
					lstClaim.add(detail);
					if (lstClaim != null && lstClaim.size() > 0)
						claim.setCfgTblDealer(lstClaim.get(0).getCfgTblDealer());
					claim.setDteDate(new Date());

					
						ST = (Total * 17) / 100;
						
					
						if (claim.getCfgTblDealer() != null && claim.getCfgTblDealer().getTxtProvince() != null) {
							if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("Punjab"))
								ST_Service = (Total_Service * 5) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("sindh"))
								ST_Service = (Total_Service * 17) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("KPK"))
								ST_Service = (Total_Service * 17) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("Balochistan"))
								ST_Service = (Total_Service * 17) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("GB"))
								ST_Service = (Total_Service * 17) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("AJK"))
								ST_Service = (Total_Service * 17) / 100;
							else if (claim.getCfgTblDealer() != null
									&& claim.getCfgTblDealer().getTxtProvince().equalsIgnoreCase("Fedral"))
								ST_Service = (Total_Service * 17) / 100;
							else
								ST_Service = (Total_Service * 17) / 100;
						} else
							ST_Service = (Total_Service * 17) / 100;

					

				}
				Tax_Total =Tax_Total+ST;
				Tax_Total_Service =Tax_Total_Service+ST_Service;
			}
			claim.setNumAmountBeforeTax(new BigDecimal(CLAIM_Total));
			claim.setNumSalesTax(new BigDecimal(Tax_Total));
			
			claim.setNumServiceAmountBeforeTax(new BigDecimal(CLAIM_Total_service));
			claim.setNumServiceTax(new BigDecimal(Tax_Total_Service));
			
			
			claim.setNumAmountAfterTax(new BigDecimal(CLAIM_Total+Tax_Total+CLAIM_Total_service+Tax_Total_Service));
			
			claim.setBlnIsService(false);
			claim.setSlsTblClaimDetail(lstClaim);
			
				retclaim = addNewClaim(claim);
			
			
			
			
			entityManager.getTransaction().commit();
			entityManager.close();
			
//			if(Type.equalsIgnoreCase("SP"))
			UpdateWorkOrderFromClaim(WorkOrdersId,true,retclaim);
//			else
//				UpdateWorkOrderFromClaim(WorkOrdersId,false,retclaim);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}
		return "Success";
	}
	
	
	public String UpdateWorkOrderFromClaim(List<String> WorkOrdersId,boolean isSP,SlsTblClaim retclaim) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serWorkOrderId : WorkOrdersId) {
							
							
							SlsTblWorkOrder WorkOrder = entityManager.find(SlsTblWorkOrder.class, Integer.parseInt(serWorkOrderId));
							if(isSP)
							{
								WorkOrder.setBlIsSPCreated(true);
								WorkOrder.setTxtClaimNo(retclaim.getTxtClaimNo());
								WorkOrder.setSerClaimId(retclaim.getSerClaimId());
							}
							else
							{
								WorkOrder.setTxtClaimNoService(retclaim.getTxtClaimNo());
								WorkOrder.setSerClaimIdService(retclaim.getSerClaimId());
								WorkOrder.setBlIsServiceCreated(true);
							}
							entityManager.merge(WorkOrder);
			}
			
			entityManager.getTransaction().commit();
			entityManager.close();
			
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}
		return "Success";
	}
	
	
	

	public String generateClaimNo(String type) {
		// int WorkOrderNo;
		String WorkOrderType = type;
		// String WorkOrderCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (WorkOrderType.equals("1"))
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if (type.equalsIgnoreCase("SP")) {
		

			 
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String) entityManager.
					 * createQuery("select MAX(txtWorkOrderCode) from SlsTblWorkOrder ")
					 * .getSingleResult();
					 */
					String zoneCode = (String) entityManager
							.createQuery(
									"SELECT b.txtClaimNo FROM SlsTblClaim b   ORDER BY b.serClaimId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "CLSP-00";
					}
					ord_no1 = Integer.valueOf(zoneCode.substring(5));

					ord_no1 = ord_no1 + 1;
					String code = "CLSP-1";
					if (ord_no1 < 10)
						code = "CLSP-00" + ord_no1;
					else if (ord_no1 > 9 && ord_no1 < 100)
						code = "CLSP-0" + ord_no1;
					else
						code = "CLSP-" + ord_no1;
					return code;
				} catch (Exception e) {
					e.printStackTrace();

				}
				return "CLSP-001";
			}
		else
		{
					 
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String) entityManager.
					 * createQuery("select MAX(txtWorkOrderCode) from SlsTblWorkOrder ")
					 * .getSingleResult();
					 */
					String zoneCode = (String) entityManager
							.createQuery(
									"SELECT b.txtWorkOrderNo FROM SlsTblWorkOrder b   ORDER BY b.serWorkOrderId DESC")
							.setMaxResults(1).getSingleResult();

					if (isNullOrEmpty(zoneCode)) {

						zoneCode = "CL-00";
					}
					ord_no1 = Integer.valueOf(zoneCode.substring(3));

					ord_no1 = ord_no1 + 1;
					String code = "CL-1";
					if (ord_no1 < 10)
						code = "CL-00" + ord_no1;
					else if (ord_no1 > 9 && ord_no1 < 100)
						code = "CL-0" + ord_no1;
					else
						code = "CL-" + ord_no1;
					return code;
				} catch (Exception e) {
					e.printStackTrace();

				}
				return "CL-001";
			
		}
		
	}
	
	
	
	
	public List<SlsTblWorkOrder> getAllDealerJCS() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
//		List<CitTableBookingDetail> bDetails = entityManager.createQuery("select count(B.serBookingDetailId) as Total, date_format(B.dteBookingDate, '%d-%m-%y'), r.txtRegionName FROM CitTableBookingDetail B JOIN CitTableBranch Br ON B.citTableBranch1.serBranchId = Br.serBranchId JOIN CitAreaSetup A ON Br.citAreaSetup.serAreaId = A.serAreaId join CitZoneSetup z on A.citZoneSetup.serZoneId=z.serZoneId join CitRegionSetup r on z.citRegionSetup.serRegionId=r.serRegionId WHERE MONTH(B.dteBookingDate) = MONTH(CURRENT_DATE) group by B.dteBookingDate,r.txtRegionName having r.txtRegionName='Central'").getResultList();
		List<SlsTblWorkOrder> bDetails = entityManager.createQuery("select count(B.serWorkOrderId) as Total,  dealer.txtCustomerName FROM SlsTblWorkOrder B JOIN CfgTblCustomer dealer ON B.cfgTblDealer.serCustomerId =dealer.serCustomerId  group by dealer.txtCustomerName ").getResultList();

		entityManager.close();

		return bDetails;
	}
	

}
