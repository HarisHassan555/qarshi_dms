package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.*;;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.bezkoder.spring.login.sa.dal.entities.*;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.sa.bll.dto.ITAB;
import com.bezkoder.spring.login.sa.bll.dto.Attachment;
import com.bezkoder.spring.login.sa.bll.dto.Header;
import com.bezkoder.spring.login.sa.bll.dto.Item;
import com.bezkoder.spring.login.sa.bll.dto.Partner;
import com.bezkoder.spring.login.sa.bll.dto.Schedule;
import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblDealDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

@Repository
public class SlsTblDealDAO implements ISlsTblDealDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	@Autowired
	private LoginDAO loginDao;

	@Autowired
	private CfgTblCustomerDAO cfgTblCustomerDAO;

	@Autowired
	private HrTblEmployeeDAO hrEmployeeDAO;

	@Autowired
	private CfgTblIncoTermsDAO cfgTblIncoTermsDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblDealDAO.class);

	public SlsTblDealDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblDeal> getAllDeal() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*
		 * List<SlsTblDeal> Deals =
		 * entityManager.createQuery("FROM SlsTblDeal where blIsDeleted=FALSE")
		 * .getResultList();
		 */

		List<SlsTblDeal> Deals = new ArrayList();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			Deals = entityManager.createQuery(
					" FROM SlsTblDeal where   serGroupId= " + user.getSerGroupId() + " and blIsDeleted=FALSE ")
					.getResultList();
		} else
			Deals = entityManager.createQuery(" FROM SlsTblDeal where blIsDeleted=FALSE  ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Deals;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblDeal> getActiveDeal() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*
		 * List<SlsTblDeal> Deals = entityManager
		 * .createQuery("FROM SlsTblDeal where blnStatus=1 and blIsDeleted=FALSE").
		 * getResultList();
		 */

		List<SlsTblDeal> Deals = new ArrayList();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			Deals = entityManager.createQuery(" FROM SlsTblDeal where  d (blnDealCompletionStatus =false or blnDealCompletionStatus is null ) and  serGroupId= "
					+ user.getSerGroupId() + " and blIsDeleted=FALSE ").getResultList();
		} else
			Deals = entityManager.createQuery(" FROM SlsTblDeal where (blnDealCompletionStatus =false or blnDealCompletionStatus is null ) and blIsDeleted=FALSE ")
					.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Deals;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblDeal> getDealByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDeal where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblDeal> Deals = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Deals;
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
	public String addNewDeal(SlsTblDeal SlsTblDeal) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			// SlsTblDeal.setBlnStatus(true);
			SlsTblDeal.setBlIsDeleted(false);
			SlsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			SlsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			SlsTblDeal.setSerGroupId(
					this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			// SlsTblDeal.setTxtStatus("Approved");

			if (SlsTblDeal.getCfgTblDealer() != null) {
				if (SlsTblDeal.getCfgTblDealer().getHrTblEmployee() != null) {
					HrTblEmployee hrTblEmployee = new HrTblEmployee();
					hrTblEmployee
							.setSerEmployeeId(SlsTblDeal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
					hrTblEmployee.setTxtEmployeeName(
							SlsTblDeal.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeName());
					hrTblEmployee.setTxtEmployeeCode(
							SlsTblDeal.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeCode());
					hrTblEmployee.setTxtEmail(SlsTblDeal.getCfgTblDealer().getHrTblEmployee().getTxtEmail());
					hrTblEmployee.setTxtMobileNo(SlsTblDeal.getCfgTblDealer().getHrTblEmployee().getTxtMobileNo());
					SlsTblDeal.setHrTblEmployee(hrTblEmployee);
				}

				if (SlsTblDeal.getCfgTblDealer().getBlnIsExport() != null
						&& SlsTblDeal.getCfgTblDealer().getBlnIsExport() == true) {
					CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
					cfgTblDocumentType.setSerDocumentTypeId(3);
					cfgTblDocumentType.setTxtCode("ZEXP");
					SlsTblDeal.setCfgTblDocumentType(cfgTblDocumentType);

					CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();

					if (SlsTblDeal.getCfgTblCustomer() != null
							&& SlsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
						cfgTblDistributionChannel.setSerDistributionChannelId(4);
						cfgTblDistributionChannel.setTxtCode("40");
					} else {
						cfgTblDistributionChannel.setSerDistributionChannelId(3);
						cfgTblDistributionChannel.setTxtCode("30");
					}

					SlsTblDeal.setCfgTblDistributionChannel(cfgTblDistributionChannel);

				} else {

					CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();

					cfgTblDocumentType.setSerDocumentTypeId(2);
					cfgTblDocumentType.setTxtCode("ZLOC");
					SlsTblDeal.setCfgTblDocumentType(cfgTblDocumentType);
					CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();

					if (SlsTblDeal.getCfgTblCustomer() != null
							&& SlsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
						cfgTblDistributionChannel.setSerDistributionChannelId(1);
						cfgTblDistributionChannel.setTxtCode("10");
					} else {
						cfgTblDistributionChannel.setSerDistributionChannelId(2);
						cfgTblDistributionChannel.setTxtCode("20");
					}

					SlsTblDeal.setCfgTblDistributionChannel(cfgTblDistributionChannel);

				}

if(SlsTblDeal.getCfgTblDealer().getTxtDivision() !=null)
				if (SlsTblDeal.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Agri")) {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(2);
					cfgTblDivision.setTxtCode("20");
					cfgTblDivision.setTxtName("Agri");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				} else if (SlsTblDeal.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Labsa")) {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(3);
					cfgTblDivision.setTxtCode("30");

					cfgTblDivision.setTxtName("Labsa");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				}

				else {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(1);
					cfgTblDivision.setTxtCode("10");
					cfgTblDivision.setTxtName("Chemical");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				}

				if (SlsTblDeal.getCfgTblDealer().getCfgTblIncoTerm() != null) {
					SlsTblDeal.setCfgTblIncoTerm(SlsTblDeal.getCfgTblDealer().getCfgTblIncoTerm());
				}

			}

			else if (SlsTblDeal.getCfgTblCustomer() != null) {

				if (SlsTblDeal.getCfgTblCustomer().getHrTblEmployee() != null) {
					HrTblEmployee hrTblEmployee = new HrTblEmployee();
					hrTblEmployee.setSerEmployeeId(
							SlsTblDeal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
					hrTblEmployee.setTxtEmployeeName(
							SlsTblDeal.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeName());
					hrTblEmployee.setTxtEmployeeCode(
							SlsTblDeal.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeCode());
					hrTblEmployee.setTxtEmail(SlsTblDeal.getCfgTblCustomer().getHrTblEmployee().getTxtEmail());
					hrTblEmployee
							.setTxtMobileNo(SlsTblDeal.getCfgTblCustomer().getHrTblEmployee().getTxtMobileNo());
					SlsTblDeal.setHrTblEmployee(hrTblEmployee);
				}

				CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();
				if (SlsTblDeal.getCfgTblCustomer().getBlnIsExport() != null
						&& SlsTblDeal.getCfgTblCustomer().getBlnIsExport() == true) {
					CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
					cfgTblDocumentType.setSerDocumentTypeId(3);
					cfgTblDocumentType.setTxtCode("ZEXP");
					SlsTblDeal.setCfgTblDocumentType(cfgTblDocumentType);
					cfgTblDistributionChannel.setSerDistributionChannelId(3);
					cfgTblDistributionChannel.setTxtCode("30");
				} else {
					CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();

					cfgTblDocumentType.setSerDocumentTypeId(2);
					cfgTblDocumentType.setTxtCode("ZLOC");
					SlsTblDeal.setCfgTblDocumentType(cfgTblDocumentType);
					cfgTblDistributionChannel.setSerDistributionChannelId(2);
					cfgTblDistributionChannel.setTxtCode("20");
				}
				SlsTblDeal.setCfgTblDistributionChannel(cfgTblDistributionChannel);
				if (SlsTblDeal.getCfgTblCustomer().getTxtDivision() != null)
				if (SlsTblDeal.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Agri")) {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(2);
					cfgTblDivision.setTxtCode("20");
					cfgTblDivision.setTxtName("Agri");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				} else if (SlsTblDeal.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Labsa")) {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(3);
					cfgTblDivision.setTxtCode("30");
					cfgTblDivision.setTxtName("Labsa");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				} else {
					CfgTblDivision cfgTblDivision = new CfgTblDivision();
					cfgTblDivision.setSerDivisionId(1);
					cfgTblDivision.setTxtCode("10");
					cfgTblDivision.setTxtName("Chemical");
					SlsTblDeal.setCfgTblDivision(cfgTblDivision);
				}

				if (SlsTblDeal.getCfgTblCustomer().getCfgTblIncoTerm() != null) {
					SlsTblDeal.setCfgTblIncoTerm(SlsTblDeal.getCfgTblCustomer().getCfgTblIncoTerm());
				}

			}

			CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
			cfgTblSalesOrganization.setSerSalesOrganizationId(1);
			cfgTblSalesOrganization.setTxtCode("1000");

			SlsTblDeal.setCfgTblSalesOrganization(cfgTblSalesOrganization);
			SlsTblDeal.setBlnIsCompleted(false);
			entityManager.persist(SlsTblDeal);

			SlsTblDealDetails dealDetail;
			Iterator<SlsTblDealDetails> itr = SlsTblDeal.getSlsTblDealDetails().iterator();
			SlsTblDealDetails details = new SlsTblDealDetails();
			CfgTblProduct product=new CfgTblProduct();
			double order_Qty=0;
			while (itr.hasNext()) {

				dealDetail = (SlsTblDealDetails) itr.next();
				details = dealDetail;
				if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
					/*
					 * if(dealDetail.getCfgTblProductDesign()!=null &&
					 * dealDetail.getCfgTblProductDesign().getSerProductDesignId()==0) {
					 * dealDetail.setCfgTblProductDesign(null); }
					 */
					SlsTblDeal.setCfgTblProduct(dealDetail.getCfgTblProduct());
					SlsTblDeal.setNumQuantity(dealDetail.getNumQuantity());
//					entityManager.merge(SlsTblDeal);
					
					CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
					cfgTblProductDesign.setSerProductDesignId(0);
					dealDetail.setCfgTblProductDesign(cfgTblProductDesign);
					dealDetail.setBlIsDeleted(false);
					if(dealDetail.getNumItemPrice()!=null)
					{
					dealDetail.setNumBalance(dealDetail.getNumQuantity().multiply(dealDetail.getNumItemPrice()));
					dealDetail.setNumTotalPrice(dealDetail.getNumBalance());
					}
					dealDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
					dealDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
					dealDetail.setSlsTblDeal(SlsTblDeal);
					
					entityManager.persist(dealDetail);

					
				}
			}

//			try {
//				if(SlsTblDeal.getTxtSapNo()!=null && SlsTblDeal.getTxtSapNo().trim().length() >0)
//				{
//					
//				}
//				else
//				{
//				String Sap_no =  addDealTOSAP(SlsTblDeal);    
//				
//				if(Sap_no.equalsIgnoreCase("10M"))
//				{
////					SlsTblDeal.setTxtDescription("Ship-to-party is unregistered in FBR, Monthly Sales limit of customer exceeded more than 10M.");
//					SlsTblDeal.setTxtDescription("10M Error");
//					entityManager.merge(SlsTblDeal);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMail(SlsTblDeal,true);
//					return "10M";
//				}
//				else if(Sap_no.equalsIgnoreCase("90D"))
//				{
////					SlsTblDeal.setTxtDescription("Ship-to-party invoice is still open from more than 90 days.");
//					SlsTblDeal.setTxtDescription("90 Days Error");
//					entityManager.merge(SlsTblDeal);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMail(SlsTblDeal,false);
//					return "90D";
//				}
//				else if(Sap_no.equalsIgnoreCase("NTN"))
//				{
////					SlsTblDeal.setTxtDescription("Ship-to-party is unregistered in FBR, Monthly Sales limit of customer exceeded more than 10M.");
//					SlsTblDeal.setTxtDescription("NTN Error");
//					entityManager.merge(SlsTblDeal);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMailForCNIC(SlsTblDeal,true);
//
//
//					return "NTN";
//				}
//				else	SlsTblDeal.setTxtSapNo(Sap_no);
//				
//				
//				if(ServerConfiguration.send_mail)
//					sendDealinMail( SlsTblDeal, details);
//				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

//			entityManager.merge(SlsTblDeal);

			entityManager.getTransaction().commit();

			entityManager.close();
		

			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	public void sendDealinMail(SlsTblDeal slsTblDeal, SlsTblDealDetails dealDetail) {

		// EmailValidator validator = EmailValidator.getInstance();

		SMSSender sms = new SMSSender();

		String toAddress = "mkhalilawan@gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", true);
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", 587);
//		props.put("mail.smtp.port", 465);
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		CfgTblCustomer cfgTblCustomer = new CfgTblCustomer();

		if (slsTblDeal.getCfgTblDealer() != null)
		{
			if(slsTblDeal.getCfgTblDealer().getBlIsGroup() !=null &&  slsTblDeal.getCfgTblDealer().getBlIsGroup())
			{
				cfgTblCustomer =slsTblDeal.getCfgTblDealer().getCfgTblGroupCustomer();
			}
			else
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());
		}

		else if (slsTblDeal.getCfgTblCustomer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());

		// ConnectDB db=new ConnectDB();
		// ResultSet rs= null;
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("support@ittehadchemicals.com", "ss*65300");
				// return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com",
				// "ICL*@12345");

				// return new PasswordAuthentication("iclportal5@gmail.com", "abc123456@");

			}
		});

		try {

			// Dear Patron,
			//
			//
			//
			// Sales Order 1000067632 of Caustic Soda Flakes , QTY 10.000 TN has been
			// created W.R.T sold to party name to ship to party name.
			//
			// Thank you for your order.

			// Regards
			Multipart multipart = new MimeMultipart();
			Message mimeMessage = new MimeMessage(session);
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			StringBuffer sb = new StringBuffer();
			sb.append("");
			sb.append("Sale Order " + slsTblDeal.getTxtSapNo());
			sb.append(" of " + dealDetail.getCfgTblProduct().getTxtProductName());
			sb.append(", QTY " + dealDetail.getNumQuantity() + " TN has been created online W.R.T ");
			if (slsTblDeal.getCfgTblDealer() != null)
				sb.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() + " to ");

			if (slsTblDeal.getCfgTblCustomer() != null)
				sb.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());

			sb.append(". Thank you for your order.");

			messageBodyPart.setContent(
					"Dear Patron, <br/> " + sb.toString() + "<br/> Regards <br/> Ittehad Chemicals Limited <br/>"
							+ "39-Empress Road,Lahore.<br/>" + "Ext: 132" + " ",
					"text/html");

			MimeBodyPart attachPart = new MimeBodyPart();
			multipart.addBodyPart(messageBodyPart);
			mimeMessage.setContent(multipart);

			mimeMessage.setFrom(new InternetAddress("support@ittehadchemicals.com"));
			// mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new
			// InternetAddress(\"iclportal5@gmail.com\"));"));

			sms.sendSms(cfgTblCustomer.getTxtPhoneNo(), "Dear Patron, " + sb.toString() + " Regards ICL");
			mimeMessage.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(cfgTblCustomer.getTxtEmailAddress()));
			// mimeMessage.setRecipients(Message.RecipientType.TO,
			// InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()));
			mimeMessage.setSubject("Online Sale Order");
			Transport.send(mimeMessage);

			System.out.println("Done");

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Multipart multipart = new MimeMultipart();
			Message mimeMessage = new MimeMessage(session);
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			StringBuffer sb = new StringBuffer();
			sb.append("");
			sb.append("Sale Order " + slsTblDeal.getTxtSapNo());
			sb.append(" of " + dealDetail.getCfgTblProduct().getTxtProductName());
			sb.append(", QTY " + dealDetail.getNumQuantity() + " TN has been created online W.R.T ");
			if (slsTblDeal.getCfgTblDealer() != null)
				sb.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() + " to ");

			if (slsTblDeal.getCfgTblCustomer() != null)
				sb.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());

			sb.append(". Please Update price and incoterms as per requirement.");

			messageBodyPart.setContent("Dear RSM, <br/> " + sb.toString()
					+ "<br/> Regards <br/> Ittehad Chemicals Limited <br/>" + "Sales Portal <br/>" + " ", "text/html");

			MimeBodyPart attachPart = new MimeBodyPart();
			multipart.addBodyPart(messageBodyPart);
			mimeMessage.setContent(multipart);

			mimeMessage.setFrom(new InternetAddress("support@ittehadchemicals.com"));
			// mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new
			// InternetAddress("iclportal5@gmail.com"));
			// mimeMessage.setRecipients(Message.RecipientType.TO,
			// InternetAddress.parse(cfgTblCustomer.getTxtEmailAddress()));
if(slsTblDeal.getHrTblEmployee() !=null && slsTblDeal.getHrTblEmployee().getTxtMobileNo()!=null )
			sms.sendSms(slsTblDeal.getHrTblEmployee().getTxtMobileNo(),
					"Dear RSM, " + sb.toString() + " Regards ICL");
			
			if(slsTblDeal.getCfgTblDealer()!=null && slsTblDeal.getCfgTblDealer().getCfgTblArea()!=null)
			{
				List<HrTblEmployee> lstEmployee=	getAreaEmployees(slsTblDeal.getCfgTblDealer().getCfgTblArea().getSerAreaId());
				int count=0;
				StringBuffer emails=new StringBuffer();
				for(HrTblEmployee emp:lstEmployee)
				{
					if(count == 0)
					{
						emails.append(emp.getTxtEmail());
					}
					else
					{
						
						emails.append(","+emp.getTxtEmail());
					}
					count++;
						
				}
				System.out.println("emails---"+emails.toString());
				mimeMessage.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse(emails.toString()));
				mimeMessage.setSubject("Online Sale Order");
				Transport.send(mimeMessage);
			}
			else
			{
			mimeMessage.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()));
			mimeMessage.setSubject("Online Sale Order");
			Transport.send(mimeMessage);
			}
			System.out.println("Done");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String addDealTOSAP(SlsTblDeal slsTblDeal) {

		try {
			// create an instance of `JAXBContext`
			// JAXBContext context = JAXBContext.newInstance(Header.class);

			JAXBContext context = JAXBContext.newInstance(ITAB.class);

			// create an instance of `Marshaller`
			Marshaller marshaller = context.createMarshaller();

			// enable pretty-print XML output
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// write XML to `StringWriter`
			StringWriter sw = new StringWriter();

			StringWriter sw1 = new StringWriter();

			StringWriter sw2 = new StringWriter();

			StringWriter swSchedule = new StringWriter();

			String header_par = "";

			String partner_par = "";

			String item_par = "";

			String schedule_par = "";

			// create `Book` object
			// Book book = new Book(17L, "Head First Java", "ISBN-45565-45",
			// new Author(5L, "Bert", "Bates"));

			ITAB itab = new ITAB();

			Header header = new Header();

			// <DOC_TYPE>ZLOC</DOC_TYPE>
			// <PURCH_NO>123456</PURCH_NO>
			// <PURCH_DATE>20200522</PURCH_DATE>
			// <DISTR_CHAN>10</DISTR_CHAN>
			// <SALES_ORG>1000</SALES_ORG>
			// <DIVISION>10</DIVISION>
			// <PMNDISTR_CHANTTRMS>Z000</PMNDISTR_CHANTTRMS>

			// header.setDOC_TYPE("ZLOC");
			// header.setPURCH_NO("123456");
			// header.setPURCH_DATE("20200722");
			// header.setDISTR_CHAN("10");
			// header.setSALES_ORG("1000");
			// header.setPMNDISTR_CHANTTRMS("Z000");
			// header.setDIVISION("10");
			// itab.setHeader(header);

			header.setDOC_TYPE(slsTblDeal.getCfgTblDocumentType().getTxtCode());
			if (slsTblDeal.getTxtPONo() != null && slsTblDeal.getTxtPONo().trim().length() > 0)
				header.setPURCH_NO(slsTblDeal.getTxtPONo());
			else
				header.setPURCH_NO("N/A");

			if (slsTblDeal.getDtePODate() != null) {
				String strDate = DATE_FORMATSAP.format(slsTblDeal.getDtePODate());
				header.setPURCH_DATE(strDate);
			}

			// header.setPURCH_DATE(DATE_FORMATSAP.format(DATE_FORMAT.parse(DATE_FORMATDB.format("yyyy-dd-mm",
			// slsTblDeal.getDtePODate())))+"");

			if (slsTblDeal.getCfgTblDistributionChannel() != null)
				header.setDISTR_CHAN(slsTblDeal.getCfgTblDistributionChannel().getTxtCode());

			if (slsTblDeal.getCfgTblSalesOrganization() != null)
				header.setSALES_ORG(slsTblDeal.getCfgTblSalesOrganization().getTxtCode());

			if (slsTblDeal.getCfgTblPaymentTerm() != null)
				header.setPMNDISTR_CHANTTRMS(slsTblDeal.getCfgTblPaymentTerm().getTxtCode());

			if (slsTblDeal.getCfgTblDivision() != null)
				header.setDIVISION(slsTblDeal.getCfgTblDivision().getTxtCode());

			if (slsTblDeal.getCfgTblIncoTerm() != null) {

				CfgTblIncoTerm cfgTblIncoTerms = cfgTblIncoTermsDAO
						.getIncoTermByPK(slsTblDeal.getCfgTblIncoTerm().getSerIncoTermsId());
				// header.setIncoterm1(slsTblDeal.getCfgTblIncoTerm().getTxtCode());
				// header.setIncoterm2(slsTblDeal.getCfgTblIncoTerm().getTxtName());

				header.setIncoterm1(cfgTblIncoTerms.getTxtCode());
				header.setIncoterm2(cfgTblIncoTerms.getTxtName());
			}
			itab.setHeader(header);

			// convert book object to XML
			// marshaller.marshal(book, sw);
			marshaller.marshal(itab, sw);

			// print the XML
			// System.out.println(sw.toString());
			header_par = sw.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			ITAB itab_item = new ITAB();

			// Item item=new Item();
			// item.setMaterial("000000000000032000");
			// item.setReq_qty("230");
			//
			// itab_item.setItem(item);

			SlsTblDealDetails detailDTO = new SlsTblDealDetails();
			SlsTblDealDetails dealDetail = new SlsTblDealDetails();
			Iterator<SlsTblDealDetails> itr = slsTblDeal.getSlsTblDealDetails().iterator();
			boolean check = true;
			while (itr.hasNext()) {

				dealDetail = (SlsTblDealDetails) itr.next();
				if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
					detailDTO = dealDetail;
					if (check) {
						Item item = new Item();
						item.setItem_no("1");
						item.setMaterial(dealDetail.getCfgTblProduct().getTxtProductCode());

						item.setReq_qty(dealDetail.getNumQuantity().toString());

						// <ITM_NUMBER>000010</ITM_NUMBER>
						itab_item.setItem(item);
						check = false;
					}

				}
			}

			marshaller.marshal(itab_item, sw1);
			item_par = sw1.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			ITAB itab_partner = new ITAB();

			List lstPartner = new ArrayList();
			Partner partner = new Partner();
			partner.setPartn_role("AG");
			if (slsTblDeal.getCfgTblDealer() != null && slsTblDeal.getCfgTblDealer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());
				partner.setPartn_numb(dto.getTxtCustomerCode());

			} else if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());
				partner.setPartn_numb(dto.getTxtCustomerCode());

			}

			// partner.setPartn_numb(slsTblDeal.getCfgTblDealer().getTxtCustomerCode());
			// partner.setPartn_numb("0000100028");

			lstPartner.add(partner);

			Partner partner2 = new Partner();
			partner2.setPartn_role("WE");
			// partner2.setPartn_numb("0000101799");
			if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());
				partner2.setPartn_numb(dto.getTxtCustomerCode());

			} else if (slsTblDeal.getCfgTblDealer() != null
					&& slsTblDeal.getCfgTblDealer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());
				partner2.setPartn_numb(dto.getTxtCustomerCode());

			}

			lstPartner.add(partner2);

			Partner partner3 = new Partner();
			partner3.setPartn_role("ZM");
			// partner3.setPartn_numb("00000502");
			if (slsTblDeal.getCfgTblDealer() != null
					&& slsTblDeal.getCfgTblDealer().getHrTblEmployee() != null
					&& slsTblDeal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId() > 0) {

				HrTblEmployee dto = hrEmployeeDAO
						.getEmployeeByPK(slsTblDeal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
				partner3.setPartn_numb(dto.getTxtEmployeeCode());
				lstPartner.add(partner3);
			} else if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getHrTblEmployee() != null
					&& slsTblDeal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId() > 0)

			{
				HrTblEmployee dto = hrEmployeeDAO
						.getEmployeeByPK(slsTblDeal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
				partner3.setPartn_numb(dto.getTxtEmployeeCode());
				lstPartner.add(partner3);
			}

			itab_partner.setLstPartner(lstPartner);

			marshaller.marshal(itab_partner, sw2);

			System.out.println(sw2.toString());
			partner_par = sw2.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			ITAB itab_Schedule = new ITAB();
			SlsTblSaleItemSchedule slsTblSaleItemSchedule;
			List<Schedule> lstSchedule = new ArrayList();
			if (detailDTO.getSlsTblSaleItemSchedule() != null) {
				Iterator<SlsTblSaleItemSchedule> itr_sch = detailDTO.getSlsTblSaleItemSchedule().iterator();
				Schedule schedule;
				while (itr_sch.hasNext()) {

					slsTblSaleItemSchedule = (SlsTblSaleItemSchedule) itr_sch.next();
					if (slsTblSaleItemSchedule.getNumQuantity() != null
							&& slsTblSaleItemSchedule.getNumQuantity().doubleValue() > 0) {

						schedule = new Schedule();

						// schedule4.setDate("20201025");

						schedule.setDate(DATE_FORMATSAP.format(slsTblSaleItemSchedule.getDteDate()));
						schedule.setQuantity(slsTblSaleItemSchedule.getNumQuantity() + "");

						lstSchedule.add(schedule);

					}
				}
				itab_Schedule.setLstSchedule(lstSchedule);

				marshaller.marshal(itab_Schedule, swSchedule);
				schedule_par = swSchedule.toString();

			}

			// System.out.println("URL-----:"+"http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par+"&ST4="+schedule_par);

			// URL url = new URL(
			// "http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par);
			// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//
			// conn.setRequestMethod("POST");
			//
			//
			// conn.setRequestProperty("Accept", "application/text");
			String schedule=schedule_par.trim().length()>3 ?"&sche_str=" + schedule_par :"";

			System.out.println("URL-----:" + "sap-client=800&head_str=" + header_par + "&item_str =" + item_par
					+ "&part_str =" + partner_par + schedule);
			String urlParameters = "sap-client=800&head_str=" + header_par + "&item_str=" + item_par + "&part_str="
					+ partner_par +schedule ;
			byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
			int postDataLength = postData.length;
    		 String request = ServerConfiguration.ip_servre+"/ZSO_CREATE_SRV";
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();

			conn.setDoOutput(true);
			conn.getOutputStream().write(postData);

			Reader in;
			try {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				for (int c; (c = in.read()) >= 0;) {
					ab.append((char) c);
				}
			} catch (Exception e) {
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}

			//
			System.out.println("sale order number is-------------:" + ab.toString());
			if (conn.getResponseCode() == 230) {
				System.out.println("230----------");
				return "NTN";
			} else if (conn.getResponseCode() == 220) {
				System.out.println("220----------");
				return "10M";
			} else if (conn.getResponseCode() == 210) {
				System.out.println("210----------");
				return "90D";
			}
			else if (conn.getResponseCode() == 200) {
				System.out.println("200----------");
			} else if (conn.getResponseCode() == 400) {
				System.out.println("500----------");
			}

			if (conn.getResponseCode() != 200 && conn.getResponseCode() != 400) {
				// sendDealErrorinMail(slsTblDeal,dealDetail);
				conn.disconnect();
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			} else {
				conn.disconnect();
				return ab.toString();
			}

		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			return "";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		// return "";
	}

	public void sendDealErrorinMail(SlsTblDeal slsTblDeal, SlsTblDealDetails dealDetail) {

		String toAddress = "mkhalilawan@gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", true);
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", 587);
//		props.put("mail.smtp.port", 465);
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		CfgTblCustomer cfgTblCustomer = new CfgTblCustomer();

		if (slsTblDeal.getCfgTblDealer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());

		else if (slsTblDeal.getCfgTblCustomer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());

		// ConnectDB db=new ConnectDB();
		// ResultSet rs= null;
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				// return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com",
				// "ICL*@12345");

				return new PasswordAuthentication("iclportal5@gmail.com", "abc123456@");

			}
		});

		try {
			Multipart multipart = new MimeMultipart();

			Message mimeMessage = new MimeMessage(session);

			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(
					"Order No.  " + slsTblDeal.getTxtDealNo() + slsTblDeal.getCfgTblDealer() != null
							? " <br/> Dealer " + slsTblDeal.getCfgTblDealer().getTxtCustomerName()
							: "" + slsTblDeal.getCfgTblCustomer() != null
									? " <br/> Customer " + slsTblDeal.getCfgTblCustomer().getTxtCustomerName()
									: "" + "<br/> Material " + dealDetail.getCfgTblProduct().getTxtProductName()
											+ "<br/> Quantity " + dealDetail.getNumApprovedQuantity() + " ",
					"text/html");

			// code to add attachment...will be revealed later
			MimeBodyPart attachPart = new MimeBodyPart();
			multipart.addBodyPart(messageBodyPart);
			mimeMessage.setContent(multipart);

			mimeMessage.setFrom(new InternetAddress("iclportal5@gmail.com"));
			// mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new
			// InternetAddress(\"iclportal5@gmail.com\"));"));
			mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse("mkhalilawan@gmail.com"));
			//
			mimeMessage.setSubject("Customer Feedback");
			Transport.send(mimeMessage);

			System.out.println("Done");

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public String deleteDeal(List<String> DealsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDealId : DealsId) {
				SlsTblDeal Deal = entityManager.find(SlsTblDeal.class, Integer.parseInt(serDealId));
				if (Deal != null) {
					Deal.setBlIsDeleted(true);

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

	@Transactional
	@Override
	public String updateDeal(SlsTblDeal slsTblDeal) {
		
		
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblDeal.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblDeal.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			if (slsTblDeal.getSlsTblDealDetails() != null && slsTblDeal.getSlsTblDealDetails().size() > 0) {
				slsTblDeal.setCfgTblProduct(slsTblDeal.getSlsTblDealDetails().get(0).getCfgTblProduct());
				slsTblDeal.setNumQuantity(slsTblDeal.getSlsTblDealDetails().get(0).getNumQuantity());
			}
		
			if(!(slsTblDeal.getCfgTblCustomer()!=null && slsTblDeal.getCfgTblCustomer().getSerCustomerId()!=null && slsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0))
			{
				slsTblDeal.setCfgTblCustomer(null);
			}
			entityManager.merge(slsTblDeal);
		
			if (slsTblDeal.getProfile_pic() != null)
				addDealAttachmentTOSAP(slsTblDeal);
			SlsTblDealDetails dealDetail;

			if (slsTblDeal.getSlsTblDealDetails() != null && slsTblDeal.getSlsTblDealDetails().size() > 0) {

				Iterator<SlsTblDealDetails> itr = slsTblDeal.getSlsTblDealDetails().iterator();

				while (itr.hasNext()) {
					dealDetail = (SlsTblDealDetails) itr.next();

					if (dealDetail.getCfgTblProductDesign() != null
							&& dealDetail.getCfgTblProductDesign().getSerProductDesignId() == 0) {
						dealDetail.setCfgTblProductDesign(null);
					}
					if (dealDetail.getSerDealDetailId() != null && dealDetail.getSerDealDetailId() > 0) {
						if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
							dealDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
							dealDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
						} else {
							dealDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
							dealDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
							dealDetail.setBlIsDeleted(true);
						}

						entityManager.merge(dealDetail);
					} else {
						if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
							dealDetail.setBlIsDeleted(false);
							dealDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
							dealDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
							dealDetail.setSlsTblDeal(slsTblDeal);

							entityManager.persist(dealDetail);
						}
					}
					
	
				}

			}
			entityManager.getTransaction().commit();
			entityManager.close();
//			if(!(slsTblDeal.getBlnFromSAP()))
//			{
//				String msg=  UpdateDealTOSAP(slsTblDeal);
//			if(msg.equalsIgnoreCase("10M") || msg.equalsIgnoreCase("90D") || msg.equalsIgnoreCase("NTN")) 
//			throw new RuntimeException(msg);
//			}




			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	private String addDealAttachmentTOSAP(SlsTblDeal slsTblDeal) {

		try {
			// create an instance of `JAXBContext`
			// JAXBContext context = JAXBContext.newInstance(Header.class);

			JAXBContext context = JAXBContext.newInstance(ITAB.class);

			// create an instance of `Marshaller`
			Marshaller marshaller = context.createMarshaller();

			// enable pretty-print XML output
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// write XML to `StringWriter`
			StringWriter sw = new StringWriter();

			String header_par = "";

			ITAB itab = new ITAB();

			Attachment attachment = new Attachment();

			/*
			 * String type=slsTblDeal.getTxtImageType();
			 * 
			 * String[] words = type.split("/"); if(words!=null && words.length>0)
			 * attachment.setFtype(words[1]); else attachment.setFtype("JPG");
			 */

			String type = slsTblDeal.getTxtImageName();

			String[] words = type.split("\\.", 0);
			if (words != null && words.length > 0)
				attachment.setFtype(words[1]);
			else
				attachment.setFtype("JPG");

			slsTblDeal.setTxtImageType(attachment.getFtype());

			attachment.setFname(slsTblDeal.getTxtImageName());

			// attachment.setFname("check");

			attachment.setSo(slsTblDeal.getTxtSapNo());

			// attachment.setSo("1000056505");

			// Encoder encoder = Base64.getUrlEncoder();
			// String originalinput =
			// "https://stackabuse.com/tag/java/";Base64.getEncoder().encodeToString(file.getBytes())
			String encodedUrl = Base64.getEncoder().encodeToString(slsTblDeal.getProfile_pic());
			attachment.setImmage(encodedUrl);
			itab.setAttachment(attachment);
			marshaller.marshal(itab, sw);

			// print the XML
			// System.out.println(sw.toString());
			header_par = sw.toString();

			// System.out.println("URL-----:"+"http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par+"&ST4="+schedule_par);

			// URL url = new URL(
			// "http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par);
			// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//
			// conn.setRequestMethod("POST");
			//
			//
			// conn.setRequestProperty("Accept", "application/text");

			// System.out.println("URL-----:"+"sap-client=800&ST1="+header_par);
			String urlParameters = "sap-client=800&" + header_par;
			byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
			int postDataLength = postData.length;
    		 String request = ServerConfiguration.ip_servre+"/zsd_so_gos";

			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));

			// DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
			// writer.writeBytes(hashmap.toString());

			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();

			conn.setDoOutput(true);
			conn.getOutputStream().write(postData);

			Reader in;
			try {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				for (int c; (c = in.read()) >= 0;) {
					ab.append((char) c);
				}
			} catch (Exception e) {
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}

			//
			System.out.println("sale order number is-------------:" + ab.toString());

			if (conn.getResponseCode() == 200) {
				System.out.println("200----------");
			} else if (conn.getResponseCode() == 400) {
				System.out.println("500----------");
			}

			if (conn.getResponseCode() != 200 && conn.getResponseCode() != 400) {
				conn.disconnect();
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			} else {
				conn.disconnect();
				return ab.toString();
			}

		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			return "";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		// return "";
	}

	@Override
	public String generateDealNo(String type) {
		// int DealNo;
		String DealType = type;
		// String DealCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();

		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		{
			try {
				entityManager.getTransaction().begin();

				/*
				 * String zoneCode = (String) entityManager.
				 * createQuery("select MAX(txtDealNo) from SlsTblDeal where serGroupId=1"
				 * ) .getSingleResult();
				 */

				String zoneCode = (String) entityManager
						.createQuery("SELECT b.txtDealNo FROM SlsTblDeal b where b.serGroupId= "
								+ user.getSerGroupId() + " ORDER BY b.serDealId DESC")
						.setMaxResults(1).getSingleResult();

				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "DL-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(3));

				ord_no1 = ord_no1 + 1;
				String code = "DL-1";
				if (ord_no1 < 10)
					code = "DL-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "DL-0" + ord_no1;
				else
					code = "DL-" + ord_no1;
				entityManager.close();
				return code;
			} catch (NoResultException erz) {
				return "DL-001";
			}

			catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getDealById(String DealId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDeal where txtDealNo='" + DealId + "'";

			List<SlsTblDeal> Deal = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Deal.size() > 0) {
				return String.valueOf(Deal.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}




	DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat DATE_FORMATSAP = new SimpleDateFormat("yyyyMMdd");

	@Override
	/*public List<SlsTblDeal> searchDeal(SlsTblDeal Deal) {
		EntityManager entityManager = getEntityManager();
		List<SlsTblDeal> cust = new ArrayList<>();

		try {
			entityManager.getTransaction().begin();

			StringBuilder queryStr = new StringBuilder("SELECT d FROM SlsTblDeal d WHERE 1=1 ");
			Map<String, Object> params = new HashMap<>();

			if (Deal.getTxtDealNo() != null) {
				queryStr.append(" AND UPPER(d.txtDealNo) LIKE UPPER(:dealNo)");
				params.put("dealNo", Deal.getTxtDealNo() + "%");
			}

			if (Deal.getTxtDealer() != null && Deal.getTxtDealer().trim().length() > 2) {
				queryStr.append(" AND UPPER(d.cfgTblDealer.txtCustomerName) LIKE UPPER(:dealer)");
				params.put("dealer", Deal.getTxtDealer());
			}

			if (Deal.getTxtCustomer() != null && Deal.getTxtCustomer().trim().length() > 2) {
				queryStr.append(" AND UPPER(d.cfgTblCustomer.txtCustomerName) LIKE UPPER(:customer)");
				params.put("customer", Deal.getTxtCustomer());
			}

			if (Deal.getTxtProduct() != null && Deal.getTxtProduct().trim().length() > 2) {
				queryStr.append(" AND UPPER(d.cfgTblProduct.txtProductName) LIKE UPPER(:product)");
				params.put("product", Deal.getTxtProduct());
			}

			if (Deal.getTxtSapNo() != null && Deal.getTxtSapNo().trim().length() > 0) {
				queryStr.append(" AND UPPER(d.txtSapNo) LIKE UPPER(:sapNo)");
				params.put("sapNo", Deal.getTxtSapNo());
			}

			CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
			if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
				queryStr.append(" AND d.serGroupId = :serGroupId");
				params.put("serGroupId", user.getSerGroupId());
			}

			if (Deal.getBlIsComplementry() != null && Deal.getBlIsComplementry()) {
				queryStr.append(" AND d.txtStatus = 'APPROVED' AND d.serCreatedUserId > 0");
			}

			if (Deal.getCfgTblProduct() != null && Deal.getCfgTblProduct().getSerProductId() != null) {
				queryStr.append(" AND d.cfgTblProduct.serProductId = :productId");
				params.put("productId", Deal.getCfgTblProduct().getSerProductId());
			}

			if (Deal.getSerDealId() != null) {
				queryStr.append(" AND d.serDealId = :dealId");
				params.put("dealId", Deal.getSerDealId());
			}

			if (Deal.getTxtStatus() != null && Deal.getTxtStatus().trim().length() > 0) {
				queryStr.append(" AND (d.txtStatus IS NULL OR d.txtStatus = '')");
				queryStr.append(" AND d.blnIsIncoTerm = TRUE");
			}

			//SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (Deal.getDte_date_from() != null && Deal.getDte_date_from().trim().length() > 0) {
				try {
					SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
					Date fromDate = DATE_FORMAT.parse(Deal.getDte_date_from());
					queryStr.append(" AND d.dteCreateddate >= :fromDate");
					params.put("fromDate", fromDate);
				} catch (ParseException e) {
					System.err.println("Invalid date format for 'from' date: " + e.getMessage());
				}
			}

			if (Deal.getDte_date_to() != null && Deal.getDte_date_to().trim().length() > 0) {
				try {
					SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
					Date toDate = DATE_FORMAT.parse(Deal.getDte_date_to());
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(toDate);
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					calendar.set(Calendar.MILLISECOND, 999);

					queryStr.append(" AND d.dteCreateddate <= :toDate");
					params.put("toDate", calendar.getTime());
				} catch (ParseException e) {
					System.err.println("Invalid date format for 'to' date: " + e.getMessage());
				}
			}

			CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
			if (cfgTblUser.getCfgTblCustomer() != null) {
				if (Boolean.TRUE.equals(cfgTblUser.getCfgTblCustomer().getBlIsDealer())) {
					queryStr.append(" AND d.cfgTblDealer.serCustomerId IN ")
							.append("(SELECT c.serCustomerId FROM CfgTblCustomer c WHERE c.serCustomerId = :customerId ")
							.append("OR c.cfgTblGroupCustomer.serCustomerId = :customerId ")
							.append("OR c.cfgTblCustomer.serCustomerId = :customerId)");
					params.put("customerId", cfgTblUser.getCfgTblCustomer().getSerCustomerId());
				} else {
					queryStr.append(" AND d.cfgTblCustomer.serCustomerId = :customerId");
					params.put("customerId", cfgTblUser.getCfgTblCustomer().getSerCustomerId());
				}
			}

			queryStr.append(" ORDER BY d.serDealId DESC");

			Query query = entityManager.createQuery(queryStr.toString());
			for (Map.Entry<String, Object> param : params.entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			cust = query.getResultList();
			entityManager.getTransaction().commit();

		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			e.printStackTrace();
		} finally {
			if (entityManager.isOpen()) {
				entityManager.close();
			}
		}

		return cust;
	}*/

	public List<SlsTblDeal> searchDeal(SlsTblDeal Deal) {

		SimpleDateFormat DATE_FORMATDBTO = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "from SlsTblDeal Deal where 1=1 ";
		if (Deal.getTxtDealNo() != null) {
			query += " and upper(Deal.txtDealNo) like" + " upper('" + Deal.getTxtDealNo() + "%')"
					+ " ";
		}
		
		
		if (Deal.getTxtDealer() != null && Deal.getTxtDealer().trim().length() > 2) {
			query += " and upper(Deal.cfgTblDealer.txtCustomerName) like" + " upper('" + Deal.getTxtDealer() + "')"
					+ " ";
		}
		
		
		if (Deal.getTxtCustomer() != null && Deal.getTxtCustomer().trim().length() > 2) {
			query += " and upper(Deal.cfgTblCustomer.txtCustomerName) like" + " upper('" + Deal.getTxtCustomer() + "')"
					+ " ";
		}
		
		if (Deal.getTxtProduct() != null && Deal.getTxtProduct().trim().length() > 2) {
			query += " and upper(Deal.cfgTblProduct.txtProductName) like" + " upper('" + Deal.getTxtProduct() + "')"
					+ " ";
		}
		
		
		if (Deal.getTxtSapNo() != null && Deal.getTxtSapNo().trim().length() >0) {
			query += " and upper(Deal.txtSapNo) like" + " upper('" + Deal.getTxtSapNo() + "')"
					+ " ";
		}

		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			query += " and Deal.serGroupId = " + user.getSerGroupId() + " ";

		}
		
		
	if(Deal.getBlIsComplementry() !=null && Deal.getBlIsComplementry())
	{
		query += " and Deal.txtStatus = 'APPROVED' and Deal.serCreatedUserId > 0 "
				+ " ";
	}

	if (Deal.getCfgTblProduct() != null && Deal.getCfgTblProduct().getSerProductId() != null  && Deal.getCfgTblProduct().getSerProductId() > 0) {
		query += " and Deal.cfgTblProduct.serProductId =" + " " + Deal.getCfgTblProduct().getSerProductId() + "" + "  ";
	}

		if (Deal.getSerDealId() != null) {
			query += " and Deal.serDealId =" + " " + Deal.getSerDealId() + "" + "  ";
		}

		if (Deal.getTxtStatus() != null && Deal.getTxtStatus().trim().length() > 0) {
			query += " and ( Deal.txtStatus is null or Deal.txtStatus = '' ) ";
			query += " and  Deal.blnIsIncoTerm  = true ";
		}

		if (Deal.getDte_date_from() != null && Deal.getDte_date_from().trim().length() > 0) {
			try {
				Date fromDate = DATE_FORMAT.parse(Deal.getDte_date_from());
				String formattedFromDate = DATE_FORMATDB.format(fromDate);
				query += " AND Deal.dteCreateddate >= '" + formattedFromDate + "'";
			} catch (ParseException e) {
				System.err.println("Invalid date format for 'from' date: " + e.getMessage());
			}
		}

		if (Deal.getDte_date_to() != null && Deal.getDte_date_to().trim().length() > 0) {
			try {

				Date toDate = DATE_FORMAT.parse(Deal.getDte_date_to());
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(toDate);
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);

				String formattedToDate = DATE_FORMATDBTO.format(calendar.getTime());

				System.out.println("Formatted To Date: " + formattedToDate);
			} catch (ParseException e) {
				System.err.println("Invalid date format for 'to' date: " + e.getMessage());
			}
		}

		CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if (cfgTblUser.getCfgTblCustomer() != null) {
			if (cfgTblUser.getCfgTblCustomer().getBlIsDealer() != null
					&& cfgTblUser.getCfgTblCustomer().getBlIsDealer()) {
				query += " and Deal.cfgTblDealer.serCustomerId in"
						+ " (select serCustomerId from CfgTblCustomer customer  where customer.serCustomerId="
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ "  or customer.cfgTblGroupCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
						+ " or customer.cfgTblCustomer.serCustomerId=   "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + ")";
			} else {
				query += " and Deal.cfgTblCustomer.serCustomerId =" + " "
						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";

			}
		}

		query += " order by Deal.serDealId  DESC";
		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblDeal> cust = entityManager.createQuery(subQuery).getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();

		return cust;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblDealDetails> searchDealDetail(int dealId) {
		if (dealId <= 0) {
			return null;
		}

		EntityManager entityManager = null;
		List<SlsTblDealDetails> lstSOD = new ArrayList<>();

		try {
			entityManager = getEntityManager();
			entityManager.getTransaction().begin();

			String query = "FROM SlsTblDealDetails ff WHERE (blIsDeleted IS NULL OR blIsDeleted = FALSE) AND ff.slsTblDeal.serDealId = :dealId";

			lstSOD = entityManager.createQuery(query, SlsTblDealDetails.class)
					.setParameter("dealId", dealId)
					.getResultList();

			// Explicitly initialize the associated SlsTblDeal if needed
			for (SlsTblDealDetails dealDetails : lstSOD) {
				SlsTblDeal slsTblDeal = getDealInfoById(String.valueOf(dealDetails.getSlsTblDeal().getSerDealId()));
				if (slsTblDeal != null) {
					Hibernate.initialize(slsTblDeal);
					dealDetails.setSlsTblDeal(slsTblDeal);
				}
			}

			entityManager.getTransaction().commit();
		} catch (Exception e) {
			if (entityManager != null && entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			e.printStackTrace();
		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
		return lstSOD;
	}

	/*public List<SlsTblDealDetails> searchDealDetail(int DealId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (DealId > 0)
			query = "from SlsTblDealDetails ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblDeal.serDealId= "
					+ DealId;

		else
			return null;

		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblDealDetails> lstSOD = entityManager.createQuery(subQuery).getResultList();
		// Iterate over the list and load the associated SlsTblDeal
		for (SlsTblDealDetails dealDetails : lstSOD) {
			// Use entityManager to initialize the association if it is lazy-loaded
			SlsTblDeal slsTblDeal = getDealInfoById(String.valueOf(dealDetails.getSlsTblDeal().getSerDealId()));
			if (slsTblDeal != null) {
				Hibernate.initialize(slsTblDeal); // Explicitly load the deal
				lstSOD.get(lstSOD.indexOf(dealDetails)).setSlsTblDeal(slsTblDeal);
			}
		}
		entityManager.getTransaction().commit();
		entityManager.close();
		*//*
		 * SlsTblDealDetails dealDetail; Iterator<SlsTblDealDetails> itr = lstSOD.iterator();
		 * 
		 * while (itr.hasNext()) {
		 * 
		 * dealDetail = (SlsTblDealDetails) itr.next(); dealDetail.setNumStockAvailabe(new
		 * BigDecimal(0)); double available_qty = 0;
		 * 
		 * try { if (!(dealDetail.getCfgTblProductDesign() != null &&
		 * dealDetail.getCfgTblProductDesign().getSerProductDesignId()!=null) ) {
		 * CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
		 * cfgTblProductDesign.setSerProductDesignId(0);
		 * dealDetail.setCfgTblProductDesign(cfgTblProductDesign); }
		 * 
		 * if (!(dealDetail.getCfgTblProductQuality() != null &&
		 * dealDetail.getCfgTblProductDesign().getSerProductDesignId()!=null)) {
		 * CfgTblProductQuality cfgTblProductQuality = new CfgTblProductQuality();
		 * cfgTblProductQuality.setSerProductQualityId(0);
		 * dealDetail.setCfgTblProductQuality(cfgTblProductQuality); }
		 * 
		 * available_qty = 0;
		 * 
		 * } catch (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } if(available_qty>0) dealDetail.setNumStockAvailabe(new
		 * BigDecimal(available_qty)); }
		 *//*
		return lstSOD;
	}*/

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblSaleItemSchedule> searchDealDetailSchedule(int sodetailid,boolean isforSO) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (sodetailid > 0)
			if(isforSO)
			query = "from SlsTblSaleItemSchedule ff  where ( ff.blIsDeleted is null or ff.blIsDeleted = false ) and ff.slsTblSoDetail.slsTblDeal.serDealId= " + sodetailid;
			else 
				query = "from SlsTblSaleItemSchedule ff  where ( ff.blIsDeleted is null or ff.blIsDeleted = false ) and ff.slsTblSoDetail.serSoDetailId= " + sodetailid;
		else
			return null;

		log.info("Query is ---" + query.substring(0, query.length()));

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblSaleItemSchedule> lstSOD = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return lstSOD;
	}

	
	

	@Override
	public byte[] getSOPicture(String id) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		SlsTblDeal so = entityManager.find(SlsTblDeal.class, Integer.parseInt(id));
		entityManager.getTransaction().commit();
		entityManager.close();
		if (so != null) {

			if (so.getProfile_pic() != null)
				return so.getProfile_pic();

		}

		return null;
	}

	public String updateDealfromSAP(SlsTblDeal slsTblDeal) {

		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "FROM SlsTblDeal where txtSapNo='" + slsTblDeal.getTxtSapNo() + "'";

		List<SlsTblDeal> lstDeal = entityManager.createQuery(query).getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();
		if (lstDeal != null && lstDeal.size() > 0) {
			SlsTblDeal dto = (SlsTblDeal) lstDeal.get(0);
			if (dto != null) {
				// slsTblDeal.setSerDealId(dto.getSerDealId());
				
				if(slsTblDeal.getTxtMachineIp()!=null && slsTblDeal.getTxtMachineIp().equalsIgnoreCase("netAmount"))
				{
					if(slsTblDeal.getNumNetAmount()!=null && slsTblDeal.getNumNetAmount().doubleValue() >=0)
					{
						dto.setNumNetAmount(slsTblDeal.getNumNetAmount());
						
					}
					
					if (slsTblDeal.getNumPrice() !=null && slsTblDeal.getNumPrice().doubleValue() > 0) {
						
						dto.setNumPrice(slsTblDeal.getNumPrice());
						
					}
					
					return updateDealSAP(dto);
				}
				else
				{
				if (slsTblDeal.getBlnIsIncoTerm() !=null && slsTblDeal.getBlnIsIncoTerm()) {
					dto.setBlnIsIncoTerm(slsTblDeal.getBlnIsIncoTerm());
				}
				
				if (slsTblDeal.getTxtStatus() !=null && slsTblDeal.getTxtStatus().trim().length() > 0) {
					dto.setTxtStatus(slsTblDeal.getTxtStatus().trim());
				}
				
				if (slsTblDeal.getTxtDCStatus() !=null && slsTblDeal.getTxtDCStatus().trim().length() > 0) {
					dto.setTxtDCStatus(slsTblDeal.getTxtDCStatus().trim());
				}

				if (slsTblDeal.getTxtInvoiceStatus() !=null &&  slsTblDeal.getTxtInvoiceStatus().trim().length() > 0) {
					dto.setTxtInvoiceStatus(slsTblDeal.getTxtInvoiceStatus().trim());
				}
				
				if (slsTblDeal.getDteRSMApproval() !=null &&  slsTblDeal.getDteRSMApproval().toString().length() > 0) {
					dto.setDteRSMApproval(slsTblDeal.getDteRSMApproval());
				}
				
				if (slsTblDeal.getDteFinalApproval() !=null &&  slsTblDeal.getDteFinalApproval().toString().length() > 0) {
					dto.setDteFinalApproval(slsTblDeal.getDteFinalApproval());
				}
				
				if (slsTblDeal.getTxtOrderapprovalDate() !=null && slsTblDeal.getTxtOrderapprovalDate().trim().length() > 0) {
					dto.setTxtOrderapprovalDate(slsTblDeal.getTxtOrderapprovalDate().trim());
				}
				
				if (slsTblDeal.getTxtDCDate() !=null && slsTblDeal.getTxtDCDate().trim().length() > 0) {
					if(dto.getTxtDCDate()!=null && dto.getTxtDCDate().trim().length()>0)
						dto.setTxtDCDate(dto.getTxtDCDate()+","+ slsTblDeal.getTxtDCDate().trim());
						else
							dto.setTxtDCDate(slsTblDeal.getTxtDCDate().trim());
				}
				
				if (slsTblDeal.getTxtInvoiceDate() !=null && slsTblDeal.getTxtInvoiceDate().trim().length() > 0) {
					
					if(dto.getTxtInvoiceDate()!=null && dto.getTxtInvoiceDate().trim().length()>0)
						dto.setTxtInvoiceDate(dto.getTxtInvoiceDate()+","+ slsTblDeal.getTxtInvoiceDate().trim());
						else
							dto.setTxtInvoiceDate(slsTblDeal.getTxtInvoiceDate().trim());
				}
				
				
				if (slsTblDeal.getTxtDCNo() !=null && slsTblDeal.getTxtDCNo().trim().length() > 0) {
					if(dto.getTxtDCNo()!=null && dto.getTxtDCNo().trim().length()>0)
					dto.setTxtDCNo(dto.getTxtDCNo()+","+ slsTblDeal.getTxtDCNo().trim());
					else
						dto.setTxtDCNo(slsTblDeal.getTxtDCNo().trim());
				}
				
				if (slsTblDeal.getTxtInvoiceNo()!=null && slsTblDeal.getTxtInvoiceNo().trim().length() > 0) {
					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length()>0)
					dto.setTxtInvoiceNo(dto.getTxtInvoiceNo()+","+ slsTblDeal.getTxtInvoiceNo().trim());
					else
						dto.setTxtInvoiceNo(slsTblDeal.getTxtInvoiceNo().trim());
				}
				
				if (slsTblDeal.getTxtDCQty() !=null && slsTblDeal.getTxtDCQty().trim().length() > 0) {
					if(dto.getTxtDCQty()!=null && dto.getTxtDCQty().trim().length()>0)
					dto.setTxtDCQty(dto.getTxtDCQty()+","+ slsTblDeal.getTxtDCQty().trim());
					else
						dto.setTxtDCQty(slsTblDeal.getTxtDCQty().trim());
				}
				
				if (slsTblDeal.getNumPrice() !=null && slsTblDeal.getNumPrice().doubleValue() > 0) {
					
					dto.setNumPrice(slsTblDeal.getNumPrice());
					
				}
				

				return updateDealSAP(dto);
				}
			} else
				return "02-Sale Orer Not Found";

		} else
			return "02-Sale Orer Not Found";

		// return null;
	}

	public String updateDealSAP(SlsTblDeal slsTblDeal) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblDeal.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblDeal.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			slsTblDeal.setTxtStatus("close");
			entityManager.merge(slsTblDeal);

			String dealno = slsTblDeal.getTxtDealNo();
			SlsTblSaleOrder saleOrder = getSaleOrderByDealNo(dealno);
			if (saleOrder != null) {

				String remarks = "Close deal " + dealno;
				updateAuditDetail(saleOrder, remarks);
			} else {
				log.error("SaleOrder not found for DealNo: " + dealno);
				return "Failure";
			}

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}


	public String updateAuditDetail(SlsTblSaleOrder saleOrder, String remarks) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			// Create the audit record for SaleOrder
			AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
			auditSlsTblSaleOrder.setSerSaleOrderId(saleOrder.getSerSaleOrderId());
			auditSlsTblSaleOrder.setTxtRemarks(remarks);

			// Persist the audit details
			entityManager.persist(auditSlsTblSaleOrder);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	/*public String updateAuditDetail(AuditSlsTblSaleOrder soDetail) {

		EntityManager entityManager = getEntityManager();
		try {

			entityManager.getTransaction().begin();
			AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
			auditSlsTblSaleOrder.setSerSaleOrderId(slsTblSaleOrder.getSerSaleOrderId());
			auditSlsTblSaleOrder.setTxtRemarks(remarks);
			entityManager.persist(soDetail);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}*/


	private SlsTblSaleOrder getSaleOrderByDealNo(String dealno) {
		EntityManager entityManager = getEntityManager();
		Integer dealValue= Integer.parseInt(dealno);
		try {
			return (SlsTblSaleOrder) entityManager.createQuery("SELECT s FROM SlsTblSaleOrder s WHERE s.slsTblDeal.serDealId = :dealValue")
					.setParameter("dealValue", dealValue)
					.getSingleResult();
		} catch (NoResultException e) {
			log.error("SaleOrder not found for dealValue: " + dealValue, e);
			return null;
		}
	}
	
	private String UpdateDealTOSAP(SlsTblDeal slsTblDeal) {

		try {
			
			if (!(slsTblDeal.getTxtSapNo()!=null && slsTblDeal.getTxtSapNo().trim().length() >0))
			{
				return "NA";
			}
			
			
			if(slsTblDeal.getSerDealId()!=null && slsTblDeal.getSerDealId()>0)
			{
				slsTblDeal=getSOByPK(slsTblDeal.getSerDealId());
						
			}
			// create an instance of `JAXBContext`
			// JAXBContext context = JAXBContext.newInstance(Header.class);

			JAXBContext context = JAXBContext.newInstance(ITAB.class);

			// create an instance of `Marshaller`
			Marshaller marshaller = context.createMarshaller();

			// enable pretty-print XML output
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// write XML to `StringWriter`
			StringWriter sw = new StringWriter();

			StringWriter sw1 = new StringWriter();

			StringWriter sw2 = new StringWriter();

			StringWriter swSchedule = new StringWriter();

			String header_par = "";

			String partner_par = "";

			String item_par = "";

			String schedule_par = "";

			// create `Book` object
			// Book book = new Book(17L, "Head First Java", "ISBN-45565-45",
			// new Author(5L, "Bert", "Bates"));

			ITAB itab = new ITAB();

			Header header = new Header();

			// <DOC_TYPE>ZLOC</DOC_TYPE>
			// <PURCH_NO>123456</PURCH_NO>
			// <PURCH_DATE>20200522</PURCH_DATE>
			// <DISTR_CHAN>10</DISTR_CHAN>
			// <SALES_ORG>1000</SALES_ORG>
			// <DIVISION>10</DIVISION>
			// <PMNDISTR_CHANTTRMS>Z000</PMNDISTR_CHANTTRMS>

			// header.setDOC_TYPE("ZLOC");
			// header.setPURCH_NO("123456");
			// header.setPURCH_DATE("20200722");
			// header.setDISTR_CHAN("10");
			// header.setSALES_ORG("1000");
			// header.setPMNDISTR_CHANTTRMS("Z000");
			// header.setDIVISION("10");
			// itab.setHeader(header);
			
			header.setSALES_ORD(slsTblDeal.getTxtSapNo());
			
			header.setDOC_TYPE(slsTblDeal.getCfgTblDocumentType().getTxtCode());
			if (slsTblDeal.getTxtPONo() != null && slsTblDeal.getTxtPONo().trim().length() > 0)
				header.setPURCH_NO(slsTblDeal.getTxtPONo());
			else
				header.setPURCH_NO("N/A");

			if (slsTblDeal.getDtePODate() != null) {
				String strDate = DATE_FORMATSAP.format(slsTblDeal.getDtePODate());
				header.setPURCH_DATE(strDate);
			}

			// header.setPURCH_DATE(DATE_FORMATSAP.format(DATE_FORMAT.parse(DATE_FORMATDB.format("yyyy-dd-mm",
			// slsTblDeal.getDtePODate())))+"");

			if (slsTblDeal.getCfgTblDistributionChannel() != null)
				header.setDISTR_CHAN(slsTblDeal.getCfgTblDistributionChannel().getTxtCode());

			if (slsTblDeal.getCfgTblSalesOrganization() != null)
				header.setSALES_ORG(slsTblDeal.getCfgTblSalesOrganization().getTxtCode());

			if (slsTblDeal.getCfgTblPaymentTerm() != null)
				header.setPMNDISTR_CHANTTRMS(slsTblDeal.getCfgTblPaymentTerm().getTxtCode());

			if (slsTblDeal.getCfgTblDivision() != null)
				header.setDIVISION(slsTblDeal.getCfgTblDivision().getTxtCode());

			if (slsTblDeal.getCfgTblIncoTerm() != null) {

				CfgTblIncoTerm cfgTblIncoTerms = cfgTblIncoTermsDAO
						.getIncoTermByPK(slsTblDeal.getCfgTblIncoTerm().getSerIncoTermsId());
				// header.setIncoterm1(slsTblDeal.getCfgTblIncoTerm().getTxtCode());
				// header.setIncoterm2(slsTblDeal.getCfgTblIncoTerm().getTxtName());

				header.setIncoterm1(cfgTblIncoTerms.getTxtCode());
				header.setIncoterm2(cfgTblIncoTerms.getTxtName());
			}
			itab.setHeader(header);

			// convert book object to XML
			// marshaller.marshal(book, sw);
			marshaller.marshal(itab, sw);

			// print the XML
			// System.out.println(sw.toString());
			header_par = sw.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			ITAB itab_item = new ITAB();

			// Item item=new Item();
			// item.setMaterial("000000000000032000");
			// item.setReq_qty("230");
			//
			// itab_item.setItem(item);

			SlsTblDealDetails detailDTO = new SlsTblDealDetails();
			SlsTblDealDetails dealDetail = new SlsTblDealDetails();
			
			List lstDetails=searchDealDetail(slsTblDeal.getSerDealId());
			Iterator<SlsTblDealDetails> itr = lstDetails.iterator();
			boolean check = true;
			while (itr.hasNext()) {

				dealDetail = (SlsTblDealDetails) itr.next();
				if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
					detailDTO = dealDetail;
					if (check) {
						Item item = new Item();
						item.setItem_no("1");
						item.setMaterial(dealDetail.getCfgTblProduct().getTxtProductCode());

						item.setReq_qty(dealDetail.getNumQuantity().toString());

						// <ITM_NUMBER>000010</ITM_NUMBER>
						itab_item.setItem(item);
						check = false;
					}

				}
			}

			marshaller.marshal(itab_item, sw1);
			item_par = sw1.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			ITAB itab_partner = new ITAB();

			List lstPartner = new ArrayList();
			Partner partner = new Partner();
			partner.setPartn_role("AG");
			if (slsTblDeal.getCfgTblDealer() != null && slsTblDeal.getCfgTblDealer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());
				partner.setPartn_numb(dto.getTxtCustomerCode());

			} else if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());
				partner.setPartn_numb(dto.getTxtCustomerCode());

			}

			// partner.setPartn_numb(slsTblDeal.getCfgTblDealer().getTxtCustomerCode());
			// partner.setPartn_numb("0000100028");

			lstPartner.add(partner);

			Partner partner2 = new Partner();
			partner2.setPartn_role("WE");
			// partner2.setPartn_numb("0000101799");
			if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());
				partner2.setPartn_numb(dto.getTxtCustomerCode());

			} else if (slsTblDeal.getCfgTblDealer() != null
					&& slsTblDeal.getCfgTblDealer().getSerCustomerId() > 0) {
				CfgTblCustomer dto = cfgTblCustomerDAO
						.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());
				partner2.setPartn_numb(dto.getTxtCustomerCode());

			}
		

			lstPartner.add(partner2);

			Partner partner3 = new Partner();
			partner3.setPartn_role("ZM");
			// partner3.setPartn_numb("00000502");
			if (slsTblDeal.getCfgTblDealer() != null
					&& slsTblDeal.getCfgTblDealer().getHrTblEmployee() != null
					&& slsTblDeal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId() > 0) {

				HrTblEmployee dto = hrEmployeeDAO
						.getEmployeeByPK(slsTblDeal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
				partner3.setPartn_numb(dto.getTxtEmployeeCode());
				lstPartner.add(partner3);
			} else if (slsTblDeal.getCfgTblCustomer() != null
					&& slsTblDeal.getCfgTblCustomer().getHrTblEmployee() != null
					&& slsTblDeal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId() > 0)

			{
				HrTblEmployee dto = hrEmployeeDAO
						.getEmployeeByPK(slsTblDeal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
				partner3.setPartn_numb(dto.getTxtEmployeeCode());
				lstPartner.add(partner3);
			}

			itab_partner.setLstPartner(lstPartner);

			marshaller.marshal(itab_partner, sw2);

			System.out.println(sw2.toString());
			partner_par = sw2.toString();

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			ITAB itab_Schedule = new ITAB();
			SlsTblSaleItemSchedule slsTblSaleItemSchedule;
			List<Schedule> lstSchedule = new ArrayList();
			List lstSch=searchDealDetailSchedule(detailDTO.getSerDealDetailId(),false);
		if (lstSch != null) {
			schedule_par="";
				Iterator<SlsTblSaleItemSchedule> itr_sch = lstSch.iterator();
				Schedule schedule;
				while (itr_sch.hasNext()) {

					slsTblSaleItemSchedule = (SlsTblSaleItemSchedule) itr_sch.next();
					if (slsTblSaleItemSchedule.getNumQuantity() != null)
					{

						schedule = new Schedule();

						// schedule4.setDate("20201025");

						schedule.setDate(DATE_FORMATSAP.format(slsTblSaleItemSchedule.getDteDate()));
						schedule.setQuantity(slsTblSaleItemSchedule.getNumQuantity() + "");

						lstSchedule.add(schedule);

					}
				}
				itab_Schedule.setLstSchedule(lstSchedule);

				marshaller.marshal(itab_Schedule, swSchedule);
				schedule_par = swSchedule.toString();

			}

			// System.out.println("URL-----:"+"http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par+"&ST4="+schedule_par);

			// URL url = new URL(
			// "http://110.39.189.4:8000/zrest_tst?sap-client=800&ST1="+header_par+"&ST2="+item_par+"&ST3="+partner_par);
			// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//
			// conn.setRequestMethod("POST");
			//
			//
			// conn.setRequestProperty("Accept", "application/text");
		
		
		String schedule=schedule_par.trim().length()>3 ?"&sche_str=" + schedule_par :"";

			System.out.println("URL-----:" + "sap-client=800&head_str=" + header_par + "&item_str =" + item_par
					+ "&part_str =" + partner_par + schedule);
			String urlParameters = "sap-client=800&head_str=" + header_par + "&item_str=" + item_par + "&part_str="
					+ partner_par + schedule;
			byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
			int postDataLength = postData.length;
			 String request = ServerConfiguration.ip_servre+"/ZSO_UPDATE_SERV";
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();
		
			conn.setDoOutput(true);
			conn.getOutputStream().write(postData);
			
			Reader in;
			try {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				for (int c; (c = in.read()) >= 0;) {
					ab.append((char) c);
				}
			} catch (Exception e) {
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}

			System.out.println("sale order number is-------------:" + ab.toString());
			
			if (conn.getResponseCode() == 230) {
				System.out.println("230----------");
				return "NTN";
			} else if (conn.getResponseCode() == 220) {
				System.out.println("220----------");
				return "10M";
			} else if (conn.getResponseCode() == 210) {
				System.out.println("210----------");
				return "90D";
			}
			else if (conn.getResponseCode() == 200) {
				System.out.println("200----------");
			} else if (conn.getResponseCode() == 400) {
				System.out.println("500----------");
			}

			if (conn.getResponseCode() != 200 && conn.getResponseCode() != 400) {
				// sendDealErrorinMail(slsTblDeal,dealDetail);
				conn.disconnect();
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			} else {
				conn.disconnect();
				return ab.toString();
			}

		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			return "";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		// return "";
	}
	
	
	public SlsTblDeal getSOByPK(int SOId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDeal where serDealId=" + SOId ;

			SlsTblDeal so = (SlsTblDeal)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			
			return so;

		}  
		catch (NoResultException e) {
			log.error("serDealId not found");
			return null;
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	public SlsTblDealDetails getDealDetailByPK(int detailId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDealDetails where serDealDetailId=" + detailId ;

			SlsTblDealDetails details = (SlsTblDealDetails)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			
			return details;

		}  
		catch (NoResultException e) {
			log.error("serDealId not found");
			return null;
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Transactional
	@SuppressWarnings("unchecked")
	public void DeleteDealCustomer(SlsTblDeal order) {
		if(order.getSerDealId()!=null && order.getSerDealId()>0)
		{
		
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
	

		List<SlsTblDeal> Deals = new ArrayList();
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

			entityManager.createQuery(" update SlsTblDeal s set s.cfgTblCustomer=null where s.serDealId = "+order.getSerDealId()).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		
		}
	}
	
	
	public List<SlsTblDeal> getDealBySapId(String DealId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDeal where txtSapNo='" + DealId + "'";

			List<SlsTblDeal> Deals = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Deals.size() > 0) {
				return Deals;
			}
	
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	public void sendSaleOrdErrorinMail(SlsTblDeal slsTblDeal,boolean is10M) {

		// EmailValidator validator = EmailValidator.getInstance();

		SMSSender sms = new SMSSender();

		String toAddress = "mkhalilawan@gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", true);
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", 587);
//		props.put("mail.smtp.port", 465);
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		CfgTblCustomer cfgTblCustomer = new CfgTblCustomer();

		if (slsTblDeal.getCfgTblDealer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());

		else if (slsTblDeal.getCfgTblCustomer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());

		// ConnectDB db=new ConnectDB();
		// ResultSet rs= null;
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("support@ittehadchemicals.com", "ss*65300");
				// return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com",
				// "ICL*@12345");

				// return new PasswordAuthentication("iclportal5@gmail.com", "abc123456@");

			}
		});

		try {

			Multipart multipart = new MimeMultipart();
			Message mimeMessage = new MimeMessage(session);
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			StringBuffer sb = new StringBuffer();
			if(is10M)
			{
//				sb.append(" Dear Patron,CXYZ  is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");
				sb.append("" );
				if (slsTblDeal.getCfgTblCustomer() != null)
					sb.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				else if (slsTblDeal.getCfgTblDealer() != null)
					sb.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
				
				sb.append(" is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");
			}
			else
			{
//				Dear Patron, CXYZ invoice is still open from more than 90 days, Kindly contact with your RSM. Warm Regards ICL
				
				sb.append("  " );
				if (slsTblDeal.getCfgTblCustomer() != null)
					sb.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				else if (slsTblDeal.getCfgTblDealer() != null)
					sb.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
				
				sb.append(" invoice is still open from more than 90 days, Kindly contact with your RSM. Warm Regards ICL ");
			}
			

			StringBuffer sb_mail = new StringBuffer();
			if(is10M)
			{
//				sb.append(" Dear Patron,CXYZ  is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");
				
			
			 if (slsTblDeal.getCfgTblDealer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
			 else 	if (slsTblDeal.getCfgTblCustomer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				
			 sb_mail.append("  is unable to process the online order of ");
				
			
			 if (slsTblDeal.getCfgTblCustomer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
			 else 	if (slsTblDeal.getCfgTblDealer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
			 sb_mail.append("  because customer is unregistered in FBR \r\n" + 
						"			and its monthly sales limit has been exceeded more than 10 M. please contact with dealer for further help. ");
			}
			else
			{
				 if (slsTblDeal.getCfgTblDealer() != null)
					 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
				 else 	if (slsTblDeal.getCfgTblCustomer() != null)
					 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
					
				 sb_mail.append(" is unable to process the online order of ");
					
				
				 if (slsTblDeal.getCfgTblCustomer() != null)
					 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				 else 	if (slsTblDeal.getCfgTblDealer() != null)
					 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
				 sb_mail.append("  because customer invoice is still open from 90 days. please contact with dealer for further help.. ");
			}

			messageBodyPart.setContent("Dear RSM, <br/> " + sb_mail.toString()
			+ "<br/> Regards <br/> Ittehad Chemicals Limited <br/>" + "Sales Portal <br/>" + " ", "text/html");

			MimeBodyPart attachPart = new MimeBodyPart();
			multipart.addBodyPart(messageBodyPart);
			mimeMessage.setContent(multipart);

			mimeMessage.setFrom(new InternetAddress("support@ittehadchemicals.com"));
			// mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new
			// InternetAddress(\"iclportal5@gmail.com\"));"));

			sms.sendSms(cfgTblCustomer.getTxtPhoneNo(), "Dear Patron, " + sb.toString() + "");
			mimeMessage.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()+",support@ittehadchemicals.com"));
//			mimeMessage.setRecipients(Message.RecipientType.TO,
//					InternetAddress.parse("support@ittehadchemicals.com"));
			// mimeMessage.setRecipients(Message.RecipientType.TO,
			// InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()));
			mimeMessage.setSubject("Online Sale Order");
			Transport.send(mimeMessage);

			System.out.println("Done");

		} catch (Exception e) {
			e.printStackTrace();
		}

	
	}
	
	
	
	public void sendSaleOrdErrorinMailForCNIC(SlsTblDeal slsTblDeal,boolean isCNIC) {

		// EmailValidator validator = EmailValidator.getInstance();

		SMSSender sms = new SMSSender();

		String toAddress = "mkhalilawan@gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", true);
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", 587);
//		props.put("mail.smtp.port", 465);
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		CfgTblCustomer cfgTblCustomer = new CfgTblCustomer();

		if (slsTblDeal.getCfgTblDealer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblDealer().getSerCustomerId().toString());

		else if (slsTblDeal.getCfgTblCustomer() != null)
			cfgTblCustomer = cfgTblCustomerDAO
					.getCustomerByPK(slsTblDeal.getCfgTblCustomer().getSerCustomerId().toString());

		// ConnectDB db=new ConnectDB();
		// ResultSet rs= null;
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("support@ittehadchemicals.com", "ss*65300");
				// return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com",
				// "ICL*@12345");

				// return new PasswordAuthentication("iclportal5@gmail.com", "abc123456@");

			}
		});

		try {

			Multipart multipart = new MimeMultipart();
			Message mimeMessage = new MimeMessage(session);
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			StringBuffer sb = new StringBuffer();
					sb.append("" );
				if (slsTblDeal.getCfgTblCustomer() != null)
					sb.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				else if (slsTblDeal.getCfgTblDealer() != null)
					sb.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
				
				sb.append(" NTN/CNIC Number is missing, Please Provide NTN/CNIC Number to ICL Sales Department. Warm Regards ICL");
			
			

			StringBuffer sb_mail = new StringBuffer();

			 if (slsTblDeal.getCfgTblDealer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
			 else 	if (slsTblDeal.getCfgTblCustomer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
				
			 sb_mail.append("  is unable to process the online order of ");
				
			
			 if (slsTblDeal.getCfgTblCustomer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblCustomer().getTxtCustomerName());
			 else 	if (slsTblDeal.getCfgTblDealer() != null)
				 sb_mail.append(slsTblDeal.getCfgTblDealer().getTxtCustomerName() );
			 sb_mail.append("  because his NTN/CNIC Number is missing, please contact with dealer for further help.");
			
			

			messageBodyPart.setContent("Dear RSM, <br/> " + sb_mail.toString()
			+ "<br/> Regards <br/> Ittehad Chemicals Limited <br/>" + "Sales Portal <br/>" + " ", "text/html");

			MimeBodyPart attachPart = new MimeBodyPart();
			multipart.addBodyPart(messageBodyPart);
			mimeMessage.setContent(multipart);

			mimeMessage.setFrom(new InternetAddress("support@ittehadchemicals.com"));
			// mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new
			// InternetAddress(\"iclportal5@gmail.com\"));"));

	
			
			sms.sendSms(cfgTblCustomer.getTxtPhoneNo(), "Dear Patron, " + sb.toString() + "");
//			mimeMessage.setRecipients(Message.RecipientType.TO,
//					InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()+",support@ittehadchemicals.com"));
			mimeMessage.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(slsTblDeal.getHrTblEmployee().getTxtEmail()));

			mimeMessage.setSubject("Online Sale Order");
			Transport.send(mimeMessage);

			System.out.println("Done");

		} catch (Exception e) {
			e.printStackTrace();
		}

	
	}
	
	@Override
	public String updateDeal(List<String> lstOrders) {
		 List<SlsTblDeal> lst=new ArrayList();
		
		for(String i :lstOrders) 
		{
			SlsTblDeal order = getSOByPK(Integer.parseInt(i));
			lst.add(order);
		}
			/*String msg= ApproveDealTOSAP(lst);
			if(msg.equalsIgnoreCase("Success"))
			{*/
				for(SlsTblDeal order :lst)
				{
					
					order = getSOByPK(order.getSerDealId());
					{
						   SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
							try {
								order.setTxtOrderapprovalDate(outPutFormat.format(new Date().getTime()));
//								return outPutFormat.format(new Date().getTime());
								
							} catch (Exception e) {
								e.printStackTrace();
							}
					}
					 
					/*order.setTxtStatus("close");*/

					updateDealSAP(order);
					resetNumbalance(order);
				}


		//	}
			return "Success";
			//return msg;
//			Success
	 }


	public void resetNumbalance(SlsTblDeal order) {
		// Check if the order has details
		if (order != null && order.getSlsTblDealDetails() != null) {
			for (SlsTblDealDetails detail : order.getSlsTblDealDetails()) {
				// Set numbalance to 0
				detail.setNumBalance(BigDecimal.valueOf(0));
				this.updateDealDetailsSAP(detail);
			}


			// Optional: Save the order or details back to the database if needed
			// orderService.save(order); // Uncomment this if you have a service to save the order
		} else {
			System.out.println("Order or details are null.");
		}
	}
	
	private String ApproveDealTOSAP(List<SlsTblDeal> lstSlsTblDeal) {

		try {
			JAXBContext context = JAXBContext.newInstance(ITAB.class);

			// create an instance of `Marshaller`
			Marshaller marshaller = context.createMarshaller();

			// enable pretty-print XML output
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// write XML to `StringWriter`
			
			StringWriter swSchedule = new StringWriter();

			String schedule_par = "";

			ITAB itab = new ITAB();
			SlsTblDeal order;
			List<Header> lstSchedule = new ArrayList();
			if (lstSlsTblDeal!= null) {
				Iterator<SlsTblDeal> itr_sch = lstSlsTblDeal.iterator();
				Header header;
				while (itr_sch.hasNext()) {

					order = (SlsTblDeal) itr_sch.next();
	
						header = new Header();
					
						header.setSALES_ORD(order.getTxtSapNo());

					
						lstSchedule.add(header);
					
				}
				itab.setLstHeader(lstSchedule);

				marshaller.marshal(itab, swSchedule);
				schedule_par = swSchedule.toString();

			}

			String schedule=schedule_par.trim().length()>3 ?"&so_str=" + schedule_par :"";

			System.out.println("URL-----:" + "sap-client=800"  + schedule);
			String urlParameters = "sap-client=800"  + schedule ;
			byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
			int postDataLength = postData.length;
    		 String request = ServerConfiguration.ip_servre+"/zsd_so_app";
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();

			conn.setDoOutput(true);
			conn.getOutputStream().write(postData);

			Reader in;
			try {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				for (int c; (c = in.read()) >= 0;) {
					ab.append((char) c);
				}
			} catch (Exception e) {
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}

			//
			System.out.println("sale order number is-------------:" + ab.toString());

			if (conn.getResponseCode() == 200) {
				System.out.println("200----------");
				conn.disconnect();
				return  "Success";
			}

			else {
				conn.disconnect();
				return "Sale Order Not Approved";
			}

		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			return "1.Sale Order Not Approved ";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "2.Sale Order Not Approved";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "3.Sale Order Not Approved";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "4.Sale Order Not Approved";
		}
		// return "";
	}


	public List<HrTblEmployee> getAreaEmployees(int areaid) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblEmployee> Employees = entityManager.createQuery("FROM HrTblEmployee e where e.cfgTblArea.serAreaId ="+areaid+" and blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Employees;
	}

	@Override
	public void sendSaleOrderinMail(SlsTblSaleOrder slsTblSaleOrder, SlsTblSoDetail soDetail) {
		// TODO Auto-generated method stub
		
	}


	public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		int batchSize = 50;
		int count = 0;

		try {
			entityManager.getTransaction().begin();

			for (SlsTblDeal slsTblDeal : deals) {
				if (isDuplicateDeal(slsTblDeal, entityManager)) {
					log.warn("Duplicate deal detected: " + slsTblDeal.getTxtSapNo());
					continue;
				}


				slsTblDeal.setBlIsDeleted(false);
				slsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				slsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblDeal.setSerGroupId(loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId()
				);


				if (slsTblDeal.getCfgTblDealer() != null) {
					processDealer(slsTblDeal);
				} else if (slsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(slsTblDeal);
				}


				slsTblDeal.setCfgTblSalesOrganization(null);
				slsTblDeal.setBlnIsCompleted(false);
				slsTblDeal.setCfgTblDistributionChannel(null);
				slsTblDeal.setCfgTblDocumentType(null);


				slsTblDeal = entityManager.merge(slsTblDeal);


				processDealDetails(slsTblDeal, entityManager);


				if (++count % batchSize == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}


			entityManager.flush();
			entityManager.clear();


			entityManager.getTransaction().commit();
			return "Success";
		} catch (PersistenceException pe) {
			log.error("Persistence error while adding new deals: ", pe);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return "Failure: " + pe.getMessage();
		} catch (Exception e) {
			log.error("General error while adding new deals: ", e);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return "Failure: " + e.getMessage();
		} finally {

			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}


	/*public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		int batchSize = 50;
		int count = 0;

		try {
			entityManager.getTransaction().begin();

			for (SlsTblDeal slsTblDeal : deals) {
				// Set necessary properties for the deal
				slsTblDeal.setBlIsDeleted(false);
				slsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				slsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblDeal.setSerGroupId(
						loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId()
				);

				// Process associated entities (dealer or customer)
				if (slsTblDeal.getCfgTblDealer() != null) {
					processDealer(slsTblDeal);
				} else if (slsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(slsTblDeal);
				}

				// Nullify unnecessary associations
				slsTblDeal.setCfgTblSalesOrganization(null);
				slsTblDeal.setBlnIsCompleted(false);
				slsTblDeal.setCfgTblDistributionChannel(null);
				slsTblDeal.setCfgTblDocumentType(null);

				// Merge the SlsTblDeal entity (attach it to the persistence context)
				slsTblDeal = entityManager.merge(slsTblDeal);

				// Now persist SlsTblDealDetails which will reference the persisted SlsTblDeal
				processDealDetails(slsTblDeal, entityManager);

				// Batch processing: Flush and clear EntityManager every batchSize records
				if (++count % batchSize == 0) {
					entityManager.flush();  // Push the data to the database
					entityManager.clear();  // Detach all entities from the context to prevent memory overload
				}
			}

			// Final flush and clear to commit all remaining data
			entityManager.flush();
			entityManager.clear();

			// Commit the transaction to finalize all changes
			entityManager.getTransaction().commit();
			return "Success";
		} catch (PersistenceException pe) {
			log.error("Persistence error while adding new deals: ", pe);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();  // Rollback in case of error
			}
			return "Failure: " + pe.getMessage();
		} catch (Exception e) {
			log.error("General error while adding new deals: ", e);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();  // Rollback in case of error
			}
			return "Failure: " + e.getMessage();
		} finally {
			// Ensure the EntityManager is closed properly to avoid leaks
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}*/


	private boolean isDuplicateDeal(SlsTblDeal deal, EntityManager entityManager) {
		if (deal.getTxtSapNo() == null) {
			return false;
		}

		String query = "SELECT COUNT(d) FROM SlsTblDeal d WHERE d.txtSapNo = :sapNo";
		Long count = entityManager.createQuery(query, Long.class)
				.setParameter("sapNo", deal.getTxtSapNo())
				.getSingleResult();
		return count > 0;
	}



	/*public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
		cfgTblSalesOrganization.setSerSalesOrganizationId(1);
		cfgTblSalesOrganization.setTxtCode("1000");

		try {
			entityManager.getTransaction().begin();

			int batchSize = 50;
			int count = 0;

			for (SlsTblDeal slsTblDeal : deals) {

				slsTblDeal.setBlIsDeleted(false);
				slsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				slsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblDeal.setSerGroupId(
						loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId()
				);


				if (slsTblDeal.getCfgTblDealer() != null) {
					processDealer(slsTblDeal);
				} else if (slsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(slsTblDeal);
				}


				slsTblDeal.setCfgTblSalesOrganization(null);
				slsTblDeal.setBlnIsCompleted(false);
				//if (slsTblDeal.getCfgTblDistributionChannel() == null) {
				slsTblDeal.setCfgTblDistributionChannel(null);
				slsTblDeal.setCfgTblDocumentType(null);
				slsTblDeal.setCfgTblSalesOrganization(null);
			//	}

				entityManager.persist(slsTblDeal);


				processDealDetails(slsTblDeal, entityManager);

				// Batch processing logic
				if (++count % batchSize == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}

			entityManager.flush();
			entityManager.clear();

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (PersistenceException pe) {
			log.error("Persistence error while adding new deals: ", pe);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}

			return "Failure: " + pe.getMessage();
		} catch (Exception e) {
			log.error("General error while adding new deals: ", e);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return "Failure: " + e.getMessage();
		} finally {
			// Safely close the EntityManager
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}*/


	/*public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
		cfgTblSalesOrganization.setSerSalesOrganizationId(1);
		cfgTblSalesOrganization.setTxtCode("1000");

		try {
			entityManager.getTransaction().begin();

			int batchSize = 50;
			int count = 0;

			for (SlsTblDeal slsTblDeal : deals) {
				slsTblDeal.setBlIsDeleted(false);
				slsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				slsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				slsTblDeal.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());

				if (slsTblDeal.getCfgTblDealer() != null) {
					processDealer(slsTblDeal);
				} else if (slsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(slsTblDeal);
				}

				slsTblDeal.setCfgTblSalesOrganization(cfgTblSalesOrganization);
				slsTblDeal.setBlnIsCompleted(false);
				entityManager.persist(slsTblDeal);
				processDealDetails(slsTblDeal, entityManager);
				if (++count % batchSize == 0) {
					entityManager.flush();
					entityManager.clear();
				}
			}

			entityManager.flush();
			entityManager.clear();

			entityManager.getTransaction().commit();
			return "Success";
		} catch (PersistenceException pe) {
			log.error("Persistence error adding new deals: ", pe);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return "Failure";
		} catch (Exception e) {
			log.error("General error adding new deals: ", e);
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return "Failure";
		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}*/

	/*public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			for (SlsTblDeal SlsTblDeal : deals) {
				SlsTblDeal.setBlIsDeleted(false);
				SlsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				SlsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				SlsTblDeal.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());

				// Set dealer or customer specific attributes
				if (SlsTblDeal.getCfgTblDealer() != null) {
					processDealer(SlsTblDeal);
				} else if (SlsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(SlsTblDeal);
				}

				// Set sales organization
				CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
				cfgTblSalesOrganization.setSerSalesOrganizationId(1);
				cfgTblSalesOrganization.setTxtCode("1000");
				SlsTblDeal.setCfgTblSalesOrganization(cfgTblSalesOrganization);
				SlsTblDeal.setBlnIsCompleted(false);
				entityManager.persist(SlsTblDeal);

				// Process deal details
				processDealDetails(SlsTblDeal, entityManager);
			}

			entityManager.getTransaction().commit();
			return "Success";
		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			log.error("Error adding new deals: ", e);
			return "Failure";
		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}*/


	/*public String addNewDeals(List<SlsTblDeal> deals) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			for (SlsTblDeal SlsTblDeal : deals) {

				SlsTblDeal.setBlIsDeleted(false);
				SlsTblDeal.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				SlsTblDeal.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				SlsTblDeal.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
				// SlsTblDeal.setTxtStatus("Approved"); // Uncomment if needed

				// Set dealer or customer specific attributes
				if (SlsTblDeal.getCfgTblDealer() != null) {
					processDealer(SlsTblDeal);
				} else if (SlsTblDeal.getCfgTblCustomer() != null) {
					processCustomer(SlsTblDeal);
				}

				// Set sales organization
				CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
				cfgTblSalesOrganization.setSerSalesOrganizationId(1);
				cfgTblSalesOrganization.setTxtCode("1000");
				SlsTblDeal.setCfgTblSalesOrganization(cfgTblSalesOrganization);
				SlsTblDeal.setBlnIsCompleted(false);
				entityManager.persist(SlsTblDeal);
				// Process deal details
				processDealDetails(SlsTblDeal,entityManager);

			}

			entityManager.getTransaction().commit();
			entityManager.close();

			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}*/

	private void processDealer(SlsTblDeal deal) {
		if (deal.getCfgTblDealer().getHrTblEmployee() != null) {
			HrTblEmployee hrTblEmployee = new HrTblEmployee();
			hrTblEmployee.setSerEmployeeId(deal.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
			hrTblEmployee.setTxtEmployeeName(deal.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeName());
			hrTblEmployee.setTxtEmployeeCode(deal.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeCode());
			hrTblEmployee.setTxtEmail(deal.getCfgTblDealer().getHrTblEmployee().getTxtEmail());
			hrTblEmployee.setTxtMobileNo(deal.getCfgTblDealer().getHrTblEmployee().getTxtMobileNo());
			deal.setHrTblEmployee(hrTblEmployee);
		}

		CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
		CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();
		if (deal.getCfgTblDealer().getBlnIsExport() != null && deal.getCfgTblDealer().getBlnIsExport()) {
			cfgTblDocumentType.setSerDocumentTypeId(3);
			cfgTblDocumentType.setTxtCode("ZEXP");
			cfgTblDistributionChannel.setSerDistributionChannelId(deal.getCfgTblCustomer() != null && deal.getCfgTblCustomer().getSerCustomerId() > 0 ? 4 : 3);
			cfgTblDistributionChannel.setTxtCode(deal.getCfgTblCustomer() != null && deal.getCfgTblCustomer().getSerCustomerId() > 0 ? "40" : "30");
		} else {
			cfgTblDocumentType.setSerDocumentTypeId(2);
			cfgTblDocumentType.setTxtCode("ZLOC");
			cfgTblDistributionChannel.setSerDistributionChannelId(deal.getCfgTblCustomer() != null && deal.getCfgTblCustomer().getSerCustomerId() > 0 ? 1 : 2);
			cfgTblDistributionChannel.setTxtCode(deal.getCfgTblCustomer() != null && deal.getCfgTblCustomer().getSerCustomerId() > 0 ? "10" : "20");
		}
		deal.setCfgTblDocumentType(cfgTblDocumentType);
		deal.setCfgTblDistributionChannel(cfgTblDistributionChannel);

		if (deal.getCfgTblDealer().getTxtDivision() != null) {
			CfgTblDivision cfgTblDivision = new CfgTblDivision();
			if (deal.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Agri")) {
				cfgTblDivision.setSerDivisionId(2);
				cfgTblDivision.setTxtCode("20");
				cfgTblDivision.setTxtName("Agri");
			} else if (deal.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Labsa")) {
				cfgTblDivision.setSerDivisionId(3);
				cfgTblDivision.setTxtCode("30");
				cfgTblDivision.setTxtName("Labsa");
			} else {
				cfgTblDivision.setSerDivisionId(1);
				cfgTblDivision.setTxtCode("10");
				cfgTblDivision.setTxtName("Chemical");
			}
			deal.setCfgTblDivision(cfgTblDivision);
		}

		if (deal.getCfgTblDealer().getCfgTblIncoTerm() != null) {
			deal.setCfgTblIncoTerm(deal.getCfgTblDealer().getCfgTblIncoTerm());
		}
	}


	private void processCustomer(SlsTblDeal deal) {
		if (deal.getCfgTblCustomer() != null) {
			HrTblEmployee employee = deal.getCfgTblCustomer().getHrTblEmployee();
			if (employee != null) {
				HrTblEmployee hrTblEmployee = new HrTblEmployee();
				hrTblEmployee.setSerEmployeeId(employee.getSerEmployeeId());
				hrTblEmployee.setTxtEmployeeName(employee.getTxtEmployeeName());
				hrTblEmployee.setTxtEmployeeCode(employee.getTxtEmployeeCode());
				hrTblEmployee.setTxtEmail(employee.getTxtEmail());
				hrTblEmployee.setTxtMobileNo(employee.getTxtMobileNo());
				deal.setHrTblEmployee(hrTblEmployee);
			}

			CfgTblDocumentType documentType = new CfgTblDocumentType();
			CfgTblDistributionChannel distributionChannel = new CfgTblDistributionChannel();
			if (Boolean.TRUE.equals(deal.getCfgTblCustomer().getBlnIsExport())) {
				documentType.setSerDocumentTypeId(3);
				documentType.setTxtCode("ZEXP");
				distributionChannel.setSerDistributionChannelId(3);
				distributionChannel.setTxtCode("30");
			} else {
				documentType.setSerDocumentTypeId(2);
				documentType.setTxtCode("ZLOC");
				distributionChannel.setSerDistributionChannelId(2);
				distributionChannel.setTxtCode("20");
			}
			deal.setCfgTblDocumentType(documentType);
			deal.setCfgTblDistributionChannel(distributionChannel);

			if (deal.getCfgTblCustomer().getTxtDivision() != null) {
				CfgTblDivision division = new CfgTblDivision();
				switch (deal.getCfgTblCustomer().getTxtDivision().toLowerCase()) {
					case "agri":
						division.setSerDivisionId(2);
						division.setTxtCode("20");
						division.setTxtName("Agri");
						break;
					case "labsa":
						division.setSerDivisionId(3);
						division.setTxtCode("30");
						division.setTxtName("Labsa");
						break;
					default:
						division.setSerDivisionId(1);
						division.setTxtCode("10");
						division.setTxtName("Chemical");
				}
				deal.setCfgTblDivision(division);
			}

			if (deal.getCfgTblCustomer().getCfgTblIncoTerm() != null) {
				deal.setCfgTblIncoTerm(deal.getCfgTblCustomer().getCfgTblIncoTerm());
			}
		}
	}


	/*private void processCustomer(SlsTblDeal deal) {
		if (deal.getCfgTblCustomer().getHrTblEmployee() != null) {
			HrTblEmployee hrTblEmployee = new HrTblEmployee();
			hrTblEmployee.setSerEmployeeId(deal.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
			hrTblEmployee.setTxtEmployeeName(deal.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeName());
			hrTblEmployee.setTxtEmployeeCode(deal.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeCode());
			hrTblEmployee.setTxtEmail(deal.getCfgTblCustomer().getHrTblEmployee().getTxtEmail());
			hrTblEmployee.setTxtMobileNo(deal.getCfgTblCustomer().getHrTblEmployee().getTxtMobileNo());
			deal.setHrTblEmployee(hrTblEmployee);
		}

		CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
		CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();
		if (deal.getCfgTblCustomer().getBlnIsExport() != null && deal.getCfgTblCustomer().getBlnIsExport()) {
			cfgTblDocumentType.setSerDocumentTypeId(3);
			cfgTblDocumentType.setTxtCode("ZEXP");
			cfgTblDistributionChannel.setSerDistributionChannelId(3);
			cfgTblDistributionChannel.setTxtCode("30");
		} else {
			cfgTblDocumentType.setSerDocumentTypeId(2);
			cfgTblDocumentType.setTxtCode("ZLOC");
			cfgTblDistributionChannel.setSerDistributionChannelId(2);
			cfgTblDistributionChannel.setTxtCode("20");
		}
		deal.setCfgTblDocumentType(cfgTblDocumentType);
		deal.setCfgTblDistributionChannel(cfgTblDistributionChannel);

		if (deal.getCfgTblCustomer().getTxtDivision() != null) {
			CfgTblDivision cfgTblDivision = new CfgTblDivision();
			if (deal.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Agri")) {
				cfgTblDivision.setSerDivisionId(2);
				cfgTblDivision.setTxtCode("20");
				cfgTblDivision.setTxtName("Agri");
			} else if (deal.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Labsa")) {
				cfgTblDivision.setSerDivisionId(3);
				cfgTblDivision.setTxtCode("30");
				cfgTblDivision.setTxtName("Labsa");
			} else {
				cfgTblDivision.setSerDivisionId(1);
				cfgTblDivision.setTxtCode("10");
				cfgTblDivision.setTxtName("Chemical");
			}
			deal.setCfgTblDivision(cfgTblDivision);
		}

		if (deal.getCfgTblCustomer().getCfgTblIncoTerm() != null) {
			deal.setCfgTblIncoTerm(deal.getCfgTblCustomer().getCfgTblIncoTerm());
		}
	}*/


	private void processDealDetails(SlsTblDeal deal, EntityManager entityManager) {
		if (deal == null || deal.getSlsTblDealDetails() == null || deal.getSlsTblDealDetails().isEmpty()) {
			return;  // No details to process
		}

		// Batch processing: Flush and clear the EntityManager periodically to prevent memory issues
		int batchSize = 50;  // Adjust based on your needs
		int count = 0;

		Iterator<SlsTblDealDetails> itr = deal.getSlsTblDealDetails().iterator();
		while (itr.hasNext()) {
			SlsTblDealDetails dealDetail = itr.next();

			if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
				// Set the associated product and quantity for the deal
				deal.setCfgTblProduct(dealDetail.getCfgTblProduct());
				deal.setNumQuantity(dealDetail.getNumQuantity());

				// Initialize other necessary fields
				dealDetail.setCfgTblProductDesign(null);  // Set to null or set appropriate value
				dealDetail.setBlIsDeleted(false);

				// Calculate prices if item price is available
				if (dealDetail.getNumItemPrice() != null) {
					dealDetail.setNumBalance(dealDetail.getNumQuantity().multiply(dealDetail.getNumItemPrice()));
					dealDetail.setNumTotalPrice(dealDetail.getNumBalance());
				}

				// Set the creation details for the deal detail
				dealDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				dealDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				dealDetail.setSlsTblDeal(deal);

				// Merge the deal detail if it's already managed by the persistence context
				entityManager.merge(dealDetail); // Use merge instead of persist to handle detached entities

				// Batch processing: Flush and clear periodically to manage memory
				if (++count % batchSize == 0) {
					entityManager.flush();  // Push changes to the database
					entityManager.clear();  // Detach all entities from the persistence context to free memory
				}
			}
		}

		// Final flush and clear to ensure all remaining records are persisted
		entityManager.flush();
		entityManager.clear();
	}

	/*private void processDealDetails(SlsTblDeal deal, EntityManager entityManager) {
		if (deal == null || deal.getSlsTblDealDetails() == null || deal.getSlsTblDealDetails().isEmpty()) {
			return;  // No details to process
		}

		// Batch processing: Flush and clear the EntityManager periodically to prevent memory issues
		int batchSize = 50;  // Adjust based on your needs
		int count = 0;

		Iterator<SlsTblDealDetails> itr = deal.getSlsTblDealDetails().iterator();
		while (itr.hasNext()) {
			SlsTblDealDetails dealDetail = itr.next();

			if (dealDetail.getNumQuantity() != null && dealDetail.getNumQuantity().doubleValue() > 0) {
				// Set the associated product and quantity for the deal
				deal.setCfgTblProduct(dealDetail.getCfgTblProduct());
				deal.setNumQuantity(dealDetail.getNumQuantity());

				// Initialize other necessary fields
				dealDetail.setCfgTblProductDesign(null);  // Set to null or set appropriate value
				dealDetail.setBlIsDeleted(false);

				// Calculate prices if item price is available
				if (dealDetail.getNumItemPrice() != null) {
					dealDetail.setNumBalance(dealDetail.getNumQuantity().multiply(dealDetail.getNumItemPrice()));
					dealDetail.setNumTotalPrice(dealDetail.getNumBalance());
				}

				// Set the creation details for the deal detail
				dealDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
				dealDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
				dealDetail.setSlsTblDeal(deal);

				// Persist the deal detail
				entityManager.persist(dealDetail);

				// Batch processing: Flush and clear periodically to manage memory
				if (++count % batchSize == 0) {
					entityManager.flush();  // Push changes to the database
					entityManager.clear();  // Detach all entities from the persistence context to free memory
				}
			}
		}

		// Final flush and clear to ensure all remaining records are persisted
		entityManager.flush();
		entityManager.clear();
	}*/





	public String updateDealDetailsSAP(SlsTblDealDetails slsTblDealDetail) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			slsTblDealDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			slsTblDealDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
			entityManager.merge(slsTblDealDetail);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	/*public String updateDealDetails(SlsTblDealDetails slsTblDealDetail) {


		}

		return msg;
//			Success
	}*/


	public SlsTblDeal getDealInfoById(String DealId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			String query = "FROM SlsTblDeal WHERE serDealId='" + DealId + "'";
			SlsTblDeal deal = (SlsTblDeal) entityManager.createQuery(query).getSingleResult();

			// Commit transaction if everything goes well
			entityManager.getTransaction().commit();

			return deal;

		} catch (Exception e) {
			// Rollback transaction in case of exception
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			log.error(e.getMessage(), e);
			return null;
		} finally {
			// Ensure entityManager is closed in the finally block
			if (entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}

	/*public SlsTblDeal getDealInfoById(String DealId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblDeal where serDealId='" + DealId + "'";
			SlsTblDeal Deal = (SlsTblDeal) entityManager.createQuery(query).getSingleResult();
			entityManager.close();
			*//*if (Deal.size() > 0) {
				return deal;
			}
			return "0";*//*
			return Deal;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}*/

}
