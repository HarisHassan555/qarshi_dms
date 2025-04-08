package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.persistence.*;;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.bll.dto.ZhamSdFromDmsCustmrCreate;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblClaimDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaim;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaimDetail;


@Repository
public class SlsTblClaimDAO implements ISlsTblClaimDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	@Autowired
	private LoginDAO loginDao;



	private static final Logger log = LoggerFactory.getLogger(SlsTblClaimDAO.class);

	public SlsTblClaimDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaim> getAllClaim() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblClaim> Claims = entityManager.createQuery("FROM SlsTblClaim where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Claims;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaim> getActiveClaim() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblClaim> Claims = entityManager
				.createQuery("FROM SlsTblClaim where blnStatus=1 and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Claims;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaim> getClaimByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblClaim where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblClaim> Claims = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Claims;
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
	public String deleteClaim(List<String> ClaimsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serClaimId : ClaimsId) {
				SlsTblClaim Claim = entityManager.find(SlsTblClaim.class, Integer.parseInt(serClaimId));
				if (Claim != null) {
					Claim.setBlIsDeleted(true);

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
	public String updateClaim(SlsTblClaim slsTblClaim) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblClaim.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblClaim.setDteModifieddate(commonService.getCurrentTimeStamp_new());

			entityManager.merge(slsTblClaim);
			SlsTblClaimDetail claimDetail;

			boolean status = true;
//			if(slsTblClaim.getTxtStatus()!=null && slsTblClaim.getTxtStatus().equalsIgnoreCase("Approve"))
//			{
//				List<SlsTblClaimDetail> lstClaimDetail=searchClaimDetail(slsTblClaim.getSerClaimId());
//				if(slsTblClaim.getBlnIsApproved()!=null && slsTblClaim.getBlnIsApproved())
//				{
//					slsTblClaim.setSlsTblClaimDetails(lstClaimDetail);
//					/*Iterator<SlsTblClaimDetail> itr = lstClaimDetail.iterator();
//					while (itr.hasNext())
//					{
//						claimDetail = (SlsTblClaimDetail) itr.next();
//						if(claimDetail.getNumStockAvailabe().doubleValue() < claimDetail.getNumQuantity().doubleValue())
//						{
//							status=false;
//						}
//					}*/
//				}
//				
//			}

//			else 

			{
				if (slsTblClaim.getSlsTblClaimDetail() != null) {
					Iterator<SlsTblClaimDetail> itr = slsTblClaim.getSlsTblClaimDetail().iterator();

					while (itr.hasNext()) {
						claimDetail = (SlsTblClaimDetail) itr.next();
						if (claimDetail.getSerClaimDetailId() != null
								&& claimDetail.getSerClaimDetailId() > 0) {
							if (claimDetail.getNumAmount() != null
									&& claimDetail.getNumAmount().doubleValue() > 0) {
								claimDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								claimDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							} else {
								claimDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
								claimDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
								claimDetail.setBlIsDeleted(true);
							}

							entityManager.merge(claimDetail);
						} else {
							if (claimDetail.getNumAmount() != null
									&& claimDetail.getNumAmount().doubleValue() > 0) {
								claimDetail.setBlIsDeleted(false);
								claimDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
								claimDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
								claimDetail.setSlsTblClaim(slsTblClaim);

								entityManager.persist(claimDetail);
							}
						}
					}
				}
			}
			if (status) {
				entityManager.getTransaction().commit();
				entityManager.close();

				if (slsTblClaim.getTxtStatus() != null
						&& slsTblClaim.getTxtStatus().equalsIgnoreCase("Approve"))
					setLedgerandStockEnteriesOnIssuance(slsTblClaim);
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
	public String generateClaimNo(String type) {
		// int ClaimNo;
		String ClaimType = type;
		// String ClaimCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
//		if (ClaimType.equals("1"))
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if (user.getSerGroupId() != null && user.getSerGroupId() == 2) {
			try {

				if (ClaimType.equals("GP")) {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String)
					 * entityManager.createQuery("select MAX(txtGatePassNo) from SlsTblClaim ")
					 * .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtGatePassNo FROM SlsTblClaim b where serGroupId= "
									+ user.getSerGroupId() + "  ORDER BY b.serClaimId DESC")
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
					 * createQuery("select MAX(txtClaimCode) from SlsTblClaim where serGroupId= "
					 * +user.getSerGroupId()) .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtClaimNo FROM SlsTblClaim b  where serGroupId= "
									+ user.getSerGroupId() + " ORDER BY b.serClaimId DESC")
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
			if (ClaimType.equals("GP")) {
				try {
					entityManager.getTransaction().begin();

					/*
					 * String zoneCode = (String)
					 * entityManager.createQuery("select MAX(txtGatePassNo) from SlsTblClaim ")
					 * .getSingleResult();
					 */

					String zoneCode = (String) entityManager
							.createQuery("SELECT b.txtClaimNo FROM SlsTblClaim b where serGroupId= "
									+ user.getSerGroupId() + "  ORDER BY b.serClaimId DESC")
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
					 * createQuery("select MAX(txtClaimCode) from SlsTblClaim ")
					 * .getSingleResult();
					 */
					String zoneCode = (String) entityManager
							.createQuery(
									"SELECT b.txtClaimNo FROM SlsTblClaim b   ORDER BY b.serClaimId DESC")
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

	public String getClaimById(String ClaimId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblClaim where txtClaimCode='" + ClaimId + "'";

			List<SlsTblClaim> Claim = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Claim.size() > 0) {
				return String.valueOf(Claim.size());
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
	public List<SlsTblClaim> searchClaim(SlsTblClaim Claim) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
//		and Claim.txtStatus in ( 'Created','Completed')
		String query = "from SlsTblClaim Claim where 1=1  ";
		if (Claim.getTxtClaimNo() != null) {
			query += " and upper(Claim.txtClaimNo) like" + " upper('" + Claim.getTxtClaimNo() + "%')"
					+ " ";
		}

		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			query += " and Claim.serGroupId = " + user.getSerGroupId() + " ";

		}

		if (Claim.getSerClaimId() != null) {
			query += " and Claim.serClaimId =" + " " + Claim.getSerClaimId() + "" + "  ";
		}

		if (Claim.getTxtStatus() != null && Claim.getTxtStatus().trim().length() > 0) {
			query += " and Claim.txtStatus = 'Approved'  ";
		}
		
		

//		if (Claim.getDte_date_from() != null && Claim.getDte_date_from().trim().length() > 0) {
//			try {
//
//				query += " and Claim.dteDate >= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(Claim.getDte_date_from()))
//						+ "'";
//			} catch (ParseException e) {
//				e.printStackTrace();
//			}
//		}
//
//		if (Claim.getDte_date_to() != null && Claim.getDte_date_to().trim().length() > 0) {
//			try {
//				query += " and Claim.dteDate <= '" + DATE_FORMATDB.format(DATE_FORMAT.parse(Claim.getDte_date_to()))
//						+ "'";
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

//		if (Claim.getSlsTblClaim()!= null && Claim.getSlsTblClaim().getSerClaimId() !=null) {
//			query += " and Claim.slsTblClaim.serClaimId =" + " " + Claim.getSlsTblClaim().getSerClaimId() + "" + "  ";
//		}
		
		
		if (Claim.getNumLevel() != null && Claim.getNumLevel() > 0) {
			if(Claim.getNumLevel() == 1)
			query += " and  ( Claim.numLevel  = " + Claim.getNumLevel() + " or Claim.numLevel  is null )"
					+ " ";
			else
				query += " and  ( Claim.numLevel  = " + Claim.getNumLevel() + "  )"
						+ " ";
		}

		query += " order by Claim.serClaimId  DESC";
		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblClaim> cust = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return cust;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaimDetail> searchClaimDetail(int ClaimId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (ClaimId > 0)
			query = "from SlsTblClaimDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblClaim.serClaimId= "
					+ ClaimId;

		else
			return null;

		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblClaimDetail> lstDC = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		SlsTblClaimDetail claimDetail;
		Iterator<SlsTblClaimDetail> itr = lstDC.iterator();

		while (itr.hasNext()) {

			claimDetail = (SlsTblClaimDetail) itr.next();
//			claimDetail.setNumStockAvailabe(new BigDecimal(0));
			double available_qty = 0;

			try {


				available_qty = 0;/*
									 * proTblProductionDetailDAO.getProductAvailableQty(
									 * claimDetail.getCfgTblProduct().getSerProductId(),
									 * claimDetail.getCfgTblProductDesign().getSerProductDesignId(),
									 * claimDetail.getCfgTblProductQuality().getSerProductQualityId());
									 */

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return lstDC;

	}

	public void setLedgerandStockEnteriesOnIssuance(SlsTblClaim slsTblClaim) {

	}

	@Override
	public String AssignGatePassNumber(SlsTblClaim slsTblClaim) {
		EntityManager entityManager = getEntityManager();
		try {
			String GP_No = generateClaimNo("GP");
			entityManager.getTransaction().begin();
			slsTblClaim.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblClaim.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//			slsTblClaim.setTxtGatePassNo(GP_No);
			entityManager.merge(slsTblClaim);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}



	@Override
	public String updateClaim(DCDTO dto) {

		if (dto.isInvoice()) {
			if (dto.getTxtStatus().equalsIgnoreCase("Completed") || dto.getTxtStatus().equalsIgnoreCase("Cancelled")
					|| dto.getTxtStatus().equalsIgnoreCase("Cancel")) {

//				||
//				dto.getTxtStatus().equalsIgnoreCase("Approved") || dto.getTxtStatus().equalsIgnoreCase("Rejected")
//				|| dto.getTxtStatus().equalsIgnoreCase("Cancel")
//				|| dto.getTxtStatus().equalsIgnoreCase("Deleted")

				List<SlsTblClaim> lstDetail = new ArrayList();
				if (dto.getTxtInvoiceNo() != null && dto.getTxtInvoiceNo().trim().length() > 0)
					lstDetail = getClaimListByInvoiceId(dto.getTxtInvoiceNo());
				else
					return "Invoice Number is empty";

				if (!(lstDetail != null && lstDetail.size() > 0)) {
					return "Invoice Not Found";
				}

				SlsTblClaim slsTblClaim = lstDetail.get(0);
				/*
				 * if (slsTblClaim.getTxtStatus() != null &&
				 * slsTblClaim.getTxtStatus().trim().length() > 0) { if
				 * (slsTblClaim.getTxtInvoiceStatus().equalsIgnoreCase("Approved")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblClaim.getTxtInvoiceStatus().equalsIgnoreCase("Approve")) return
				 * "Invoice Already Approved"; else if
				 * (slsTblClaim.getTxtInvoiceStatus().equalsIgnoreCase("Rejected")) return
				 * "Invoice Already Rejected"; else if
				 * (slsTblClaim.getTxtInvoiceStatus().equalsIgnoreCase("Cancel")) return
				 * "Invoice Already Cancel"; else if
				 * (slsTblClaim.getTxtInvoiceStatus().equalsIgnoreCase("Deleted")) return
				 * "Invoice Already Deleted"; }
				 */

				EntityManager entityManager = getEntityManager();
				try {
					entityManager.getTransaction().begin();

//					slsTblClaim.setDteInvModifieddate(commonService.getCurrentTimeStamp_new());
//					if( dto.getTxtStatus().equalsIgnoreCase("Cancelled") ||
//					dto.getTxtStatus().equalsIgnoreCase("Cancel") )
//					{
//						slsTblClaim.setTxtInvoiceStatus(null);
//						slsTblClaim.setTxtINVNo(null);
//						slsTblClaim.setDteInvoiceDate(null);
//					}
//					else
//					slsTblClaim.setTxtInvoiceStatus(dto.getTxtStatus());

					entityManager.merge(slsTblClaim);

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

			List<SlsTblClaim> lstDetail = new ArrayList();
			if (dto.getTxtDONo() != null && dto.getTxtDONo().trim().length() > 0)
				lstDetail = getClaimListById(dto.getTxtDONo());
			else
				return "DO Number is empty";

			if (!(lstDetail != null && lstDetail.size() > 0)) {
				return "DO Not Found";
			}
			SlsTblClaim slsTblClaim = lstDetail.get(0);

			/*
			 * if (slsTblClaim.getTxtStatus() != null &&
			 * slsTblClaim.getTxtStatus().trim().length() > 0) { if
			 * (slsTblClaim.getTxtStatus().equalsIgnoreCase("Approved")) return
			 * "DO Already Approved"; else if
			 * (slsTblClaim.getTxtStatus().equalsIgnoreCase("Approve")) return
			 * "DO Already Approved"; else if
			 * (slsTblClaim.getTxtStatus().equalsIgnoreCase("Rejected")) return
			 * "DO Already Rejected"; else if
			 * (slsTblClaim.getTxtStatus().equalsIgnoreCase("Cancel")) return
			 * "DO Already Cancel"; else if
			 * (slsTblClaim.getTxtStatus().equalsIgnoreCase("Deleted")) return
			 * "DO Already Deleted"; }
			 */
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblClaim.setDteModifieddate(commonService.getCurrentTimeStamp_new());

//					if(dto.isUpdate())
//					{

//					if( dto.getQuantity() >0)
//						slsTblClaim.setNumQuantity(new BigDecimal(dto.getQuantity()));
//					
//					if( dto.getTareWeight() >0)
//						slsTblClaim.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
//					
//					if( dto.getGrossWeight() >0)
//						slsTblClaim.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
//					
//					if( dto.getNetWeight() >0)
//						slsTblClaim.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					
////						slsTblClaim.setNumQuantity(new BigDecimal(dto.getQuantity()));
////						slsTblClaim.setNumTareWeight(new BigDecimal(dto.getTareWeight()));
////						slsTblClaim.setNumGrossWeight(new BigDecimal(dto.getGrossWeight()));
////						slsTblClaim.setNumNetWeight(new BigDecimal(dto.getNetWeight()));
//					}

				if (dto.getTxtStatus() != null && dto.getTxtStatus().trim().length() > 1)
					slsTblClaim.setTxtStatus(dto.getTxtStatus());

//					if(dto.getTxtInvStatus()!=null && dto.getTxtInvStatus().trim().length() >0)
//					   slsTblClaim.setTxtInvoiceStatus(dto.getTxtInvStatus());
//					
//					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() >0)
//						   slsTblClaim.setTxtINVNo(dto.getTxtInvoiceNo());

				if (dto.getDteInvDate() != null)
					slsTblClaim.setDteInvoiceDate(dto.getDteInvDate());

				entityManager.merge(slsTblClaim);

				entityManager.getTransaction().commit();
				entityManager.close();
				return "DO Updated successfully.";

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}

		}
	}

	public List<SlsTblClaim> getClaimListById(String ClaimId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblClaim where txtClaimCode='" + ClaimId + "'";

			List<SlsTblClaim> Claim = entityManager.createQuery(query).getResultList();

			return Claim;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	public List<SlsTblClaim> getClaimListByInvoiceId(String InvoiceId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblClaim where txtINVNo='" + InvoiceId + "'";

			List<SlsTblClaim> Claim = entityManager.createQuery(query).getResultList();

			return Claim;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public SlsTblClaim getPrevious(String vehicle) {

		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		{

			try {
				entityManager.getTransaction().begin();

				SlsTblClaim slsTblClaim = (SlsTblClaim) entityManager
						.createQuery(
								"SELECT b FROM SlsTblClaim b where b.slsTblSoVehicleDetail.serSoVehicleDetailId = "
										+ vehicle + "  ORDER BY b.serClaimId DESC")
						.setMaxResults(1).getSingleResult();

				if (slsTblClaim != null)
					return slsTblClaim;
				else
					return new SlsTblClaim();
			} catch (Exception e) {
				e.printStackTrace();

			}
			SlsTblClaim sls=new SlsTblClaim();
			
			return sls;

		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaim> getClaimForPostServiceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblClaim> Claims = entityManager.createQuery("FROM SlsTblClaim where blIsDeleted=FALSE and (blIsPostServiceFollowUp = false or blIsPostServiceFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Claims;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblClaim> getClaimForMaintinanceFU() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblClaim> Claims = entityManager.createQuery("FROM SlsTblClaim where blIsDeleted=FALSE and (blIsPostServiceFollowUp = false or blIsPostServiceFollowUp is null)")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Claims;
	}
	
	@Override
	public String addNewClaim(SlsTblClaim slsTblClaim) {
		EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				slsTblClaim.setBlIsDeleted(false);
				slsTblClaim.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblClaim.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
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
					details = soDetail;
		
						soDetail.setBlIsDeleted(false);
						soDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
						soDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
						soDetail.setSlsTblClaim(slsTblClaim);

						entityManager.persist(soDetail);
						
				}
				
				
				
				entityManager.getTransaction().commit();
				entityManager.close();
//				setLedgerandStockEnteriesOnIssuance(SlsTblClaim);
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}

	@Override
	public String addNewClaim(DCDTO slsTblClaim) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createClaimfromClaim(List<String> ClaimsId) {
		// TODO Auto-generated method stub
		return null;
	}
	

	


	 private  String createSOAPRequest(CfgTblCustomer dto) throws Exception {

	    	log.info("inside-------createSOAPRequest");
	  	
	  	ZhamSdFromDmsCustmrCreate customer=new ZhamSdFromDmsCustmrCreate();
	  	 MessageFactory messageFactory = MessageFactory.newInstance();
	  	 String serverURI = "urn:sap-com:document:sap:soap:functions:mc-style";
	       final SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
	       String      authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
	       MimeHeaders hd            = soapMessage.getMimeHeaders();
	       hd.addHeader("Authorization", "Basic " + authorization);
	       SOAPPart soapPart = soapMessage.getSOAPPart();
	       SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
//	       soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:soap:functions:mc-style");
	       soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:rfc:functions");
	       SOAPBody soapBody = soapEnvelope.getBody();
	       JAXBContext jaxbContext = JAXBContext.newInstance(ZhamSdFromDmsCustmrCreate.class);
	       Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	       // output pretty printed
	       jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	       jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
	       
	       if(dto.getCfgTblCity()!=null)
	       customer.setCITY(dto.getCfgTblCity().getTxtCityName());
	       else
	    	   customer.setCITY(" ");
	       
	       
	       if(dto.getTxtEmailAddress() != null && dto.getTxtEmailAddress().trim().length() >0)
	       customer.setEMAIL(dto.getTxtEmailAddress());
	       else
	    	   customer.setEMAIL(" ");
	       if(dto.getCfgTblCustomerCategory()!=null)
	       customer.setACCOUNTGRP(dto.getCfgTblCustomerCategory().getTxtCustomerCategoryCode());
	       else
	    	   if(dto.getCfgTblCustomer() !=null && dto.getCfgTblCustomer().getCfgTblCustomerCategory()!=null)
	    	   {
	    		   customer.setACCOUNTGRP(dto.getCfgTblCustomer().getCfgTblCustomerCategory().getTxtCustomerCategoryCode());
	    	   }
	    	   else
	       customer.setACCOUNTGRP("ZC03");
	       
	       if(dto.getTxtDisplayAddress()!=null && dto.getTxtDisplayAddress().trim().length() >0)
	       customer.setADDRESS(dto.getTxtDisplayAddress());
	       else
	    	   customer.setADDRESS("");
	       
	       
//	       if(dto.getTxtMobileNo()!=null && dto.getTxtMobileNo().trim().length() >0)
//	    	   customer.setCellnumber(dto.getTxtMobileNo());
//	           else
//	        	   customer.setCellnumber(" ");
	       
	       if(dto.getTxtCnicNo()!=null && dto.getTxtCnicNo().trim().length() >0)
	    	   customer.setCNIC(dto.getTxtCnicNo());
	           else
	        	   customer.setCNIC(" ");

	    
	       customer.setCOMMERCIAL(dto.getBlnCommercial()!=null &&  dto.getBlnCommercial() ? "X" : "");
	       customer.setCUSTOMERCODE("00"+dto.getTxtCustomerCode());
	       
	       if(dto.getTxtFTN()!=null && dto.getTxtFTN().trim().length() > 0)
	           customer.setFTN(dto.getTxtFTN());
	        else
	     	   customer.setFTN(" ");
	        
	       if(dto.getTxtCustomerName()!=null && dto.getTxtCustomerName().trim().length() >0)
	       customer.setFIRSTNAME(dto.getTxtCustomerName());
	       else
	    	   customer.setFIRSTNAME(" ");
	       
	       if(dto.getTxtNtnNo()!=null && dto.getTxtNtnNo().trim().length() > 0)
	    	   customer.setNTN(dto.getTxtNtnNo());
	       else
	    	   customer.setNTN(" ");
	       customer.setLASTNAME(" ");
	       customer.setPASSENGER(dto.getBlnPassanger()!=null &&  dto.getBlnPassanger() ? "X" : "");
	       customer.setFILER(dto.getBlnIsFiler()!=null &&  dto.getBlnIsFiler() ? "X" : "");
	       
	       if(dto.getTxtPhoneNo()!=null && dto.getTxtPhoneNo().trim().length() > 0)
	        customer.setPHONENUMBER(dto.getTxtPhoneNo());
	       else
	    	   customer.setPHONENUMBER(" ");
	       
	       if(dto.getTxtPhoneNo()!=null && dto.getTxtPhoneNo().trim().length() > 0)
	          customer.setCELLNUMBER(dto.getTxtPhoneNo());
	       else
	    	   customer.setCELLNUMBER(" ");
	       
	       
	       if(dto.getTxtProvince()!=null && dto.getTxtProvince().trim().length() > 0)
	           customer.setPROVINCE(dto.getTxtProvince());
	        else
	     	   customer.setPROVINCE(" ");
	       

	       customer.setREGION("SD");
	       if(dto.getTxtSTR()!=null &&  dto.getTxtSTR().trim().length() > 0)
	       customer.setSTRN(dto.getTxtSTR());
	       else
	    	   customer.setSTRN(" ");
	       
	       customer.setRECONCILIATION_AGL("900000101");
//	       jaxbMarshaller.marshal(customer, System.err);
	       
	       
	       
	       log.info("customer-------"+customer.toString());
	       jaxbMarshaller.marshal(customer, soapBody);
	       soapMessage.saveChanges();
	       soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
	       soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
	       soapMessage.writeTo(System.out);

	      hd.addHeader("SOAPAction", serverURI + "ReadProjects");

	      // Save the message
	      soapMessage.saveChanges();

	      // Check the input
	      System.out.println("Request SOAP Message = ");
	      soapMessage.writeTo(System.out);
	      System.out.println();
		  ByteArrayOutputStream out = new ByteArrayOutputStream();
	      String strMsg =soapMessageToString(soapMessage);
	      log.info("strMsg-----"+strMsg);
	      
	      return strMsg;
	  }
	    
	    
	    public static String soapMessageToString(SOAPMessage message) 
	    {
	        String result = null;

	        if (message != null) 
	        {
	            ByteArrayOutputStream baos = null;
	            try 
	            {
	                baos = new ByteArrayOutputStream();
	                message.writeTo(baos); 
	                result = baos.toString();
	            } 
	            catch (Exception e) 
	            {
	            } 
	            finally 
	            {
	                if (baos != null) 
	                {
	                    try 
	                    {
	                        baos.close();
	                    } 
	                    catch (IOException ioe) 
	                    {
	                    }
	                }
	            }
	        }
	        return result;
	    }  
	    
	    
	  boolean  SaveCustommerInSAP(CfgTblCustomer dto)
	    {
		  
		  try {
			log.info("inside -----SaveCustommerInSAP----:"+dto.getTxtCustomerCode());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  try {

			     String msg = createSOAPRequest(dto);
			     String username="aizaz.k";
			     String password="S@pabap123";
			     
//			     String username="DMIS_USER";
//			     String password="Abc@1234567";
			     
			     //String url = "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/wsdl/flv_10002A111AD1/bndg_url/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create?sap-client=110";
//			     String url = "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create";
//			     String url = "http://vhgdiqs4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/100/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create";
			                 
			     String url = ServerConfiguration.service_customer;
			     log.info("URL---customer--:"+url);
			     URL obj = new URL(url);
			 HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			 con.setRequestMethod("PUT");
			 con.setRequestProperty("Content-Type","application/soap+xml");
//			 con.setRequestProperty("Cookie","sap-usercontext=sap-client=110");
			 con.setRequestProperty("Cookie",ServerConfiguration.usercontext);
			 con.setAllowUserInteraction(true);
			 String userpass = ServerConfiguration.Password + ":" + ServerConfiguration.uname;
			 String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
			 con.setRequestProperty ("Authorization", basicAuth);
			 log.info("msg--------:"+msg);
			 String xml = msg;
			 con.setDoOutput(true);
			 
				
			 
			 
			 DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			 wr.writeBytes(xml);
			 wr.flush();
			 wr.close();
			 String responseStatus = con.getResponseMessage();
			 log.info("responseStatus--------:"+responseStatus);
			 System.out.println(responseStatus);
			 dto.setTxtXMSent(msg);
			 dto.setTxtReturnMsg(responseStatus);
//				updateCustomer(dto);
			 if(responseStatus.equalsIgnoreCase("Internal Server Error"))
			 {
				
				 log.info("------here is internal server error --------:");
				 dto.setTxtReturnMsg(responseStatus);
//					updateCustomer(dto);
				 return false;
			 }
			 
			 BufferedReader in = new BufferedReader(new InputStreamReader(
			 con.getInputStream()));
			 String inputLine;
			 StringBuffer response = new StringBuffer();
			 
			 while ((inputLine = in.readLine()) != null) {
			 response.append(inputLine);
			 }
			 
			
			 in.close();
			 log.info("response--------:"+response.toString());
			 System.out.println("response:" + response.toString());
			 
			 dto.setTxtXMReceive(response.toString());
//			 updateCustomer(dto);

				try {

					StringReader reader = new StringReader(response.toString());
					XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

					XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

					while (xmlStreamReader.hasNext()) {
						// Get integer value of current event.
						int xmlEvent = xmlStreamReader.next();

						// Process start element.
						if (xmlEvent == XMLStreamConstants.START_ELEMENT) {
							System.out.println("Start Element: " + xmlStreamReader.getLocalName());

							if (xmlStreamReader.getLocalName().equalsIgnoreCase("MSG")) {
//								System.out.print("" + xmlStreamReader.getElementText());
								
								String ret_msg= xmlStreamReader.getElementText();
								
								if(ret_msg.equalsIgnoreCase("Success"))
								{
									
								dto.setBlIsPOSTEDToSAP(true);
//								updateCustomer(dto);
								}
								else
								{
									dto.setTxtErrorMsgFromSap(ret_msg);
									dto.setBlIsPOSTEDToSAP(false);
																
//									updateCustomer(dto);
								}
								
							}

						}

						// Process end element.
						if (xmlEvent == XMLStreamConstants.END_ELEMENT) {
							System.out.println("End Element: " + xmlStreamReader.getLocalName());
						}
					}

				} catch (Exception e) {
					 log.info(e.getMessage());
					 System.out.println(e);
					e.printStackTrace();
				}
			 
			 } catch (Exception e) {
				 log.info(e.getMessage());
			 System.out.println(e);
			 e.printStackTrace();
			 return false;
			 }
	    	return true;
	    }
	
	

}
