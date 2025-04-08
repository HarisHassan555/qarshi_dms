package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.util.Properties;
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblCustomerFeedbackDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblCustomerFeedback;

@Repository
public class SlsTblCustomerFeedbackDAO implements ISlsTblCustomerFeedbackDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	 @Autowired
	  private LoginDAO loginDao;
	 
	 @Autowired
	  private CfgTblCustomerDAO cfgTblCustomerDAO;

	private static final Logger log = LoggerFactory.getLogger(SlsTblCustomerFeedbackDAO.class);

	public SlsTblCustomerFeedbackDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblCustomerFeedback> getAllCustomerFeedback() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<SlsTblCustomerFeedback> CustomerFeedbacks = entityManager.createQuery("FROM SlsTblCustomerFeedback where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return CustomerFeedbacks;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblCustomerFeedback> getActiveCustomerFeedback() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<SlsTblCustomerFeedback> CustomerFeedbacks = entityManager
				.createQuery("FROM SlsTblCustomerFeedback where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		

		return CustomerFeedbacks;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SlsTblCustomerFeedback> getCustomerFeedbackByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblCustomerFeedback where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<SlsTblCustomerFeedback> CustomerFeedbacks = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return CustomerFeedbacks;
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
	public String addNewCustomerFeedback(SlsTblCustomerFeedback SlsTblCustomerFeedback) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			
			SlsTblCustomerFeedback.setBlIsDeleted(false);
			
			entityManager.persist(SlsTblCustomerFeedback);
			entityManager.getTransaction().commit();
			entityManager.close();
			
			sendCustomerFeedbackinMail(SlsTblCustomerFeedback);
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteCustomerFeedback(List<String> CustomerFeedbacksId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serCustomerFeedbackId : CustomerFeedbacksId) {
				SlsTblCustomerFeedback CustomerFeedback = entityManager.find(SlsTblCustomerFeedback.class, Integer.parseInt(serCustomerFeedbackId));
				if (CustomerFeedback != null) {
					CustomerFeedback.setBlIsDeleted(true);

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
	public String updateCustomerFeedback(SlsTblCustomerFeedback SlsTblCustomerFeedback) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(SlsTblCustomerFeedback);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateCustomerFeedbackNo(String type) {
		// int CustomerFeedbackNo;
		String CustomerFeedbackType = type;
		// String CustomerFeedbackCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (CustomerFeedbackType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCustomerFeedbackCode) from SlsTblCustomerFeedback ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CITY-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "CITY-1";
				if (ord_no < 10)
					code = "CITY-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "CITY-0" + ord_no;
				else
					code = "CITY-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtCustomerFeedbackCode) from SlsTblCustomerFeedback ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "CITY-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(5));

				ord_no1 = ord_no1 + 1;
				String code = "CITY-1";
				if (ord_no1 < 10)
					code = "CITY-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "CITY-0" + ord_no1;
				else
					code = "CITY-" + ord_no1;
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

	public String getCustomerFeedbackById(String CustomerFeedbackId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM SlsTblCustomerFeedback where txtCustomerFeedbackCode='" + CustomerFeedbackId + "'";

			List<SlsTblCustomerFeedback> CustomerFeedback = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (CustomerFeedback.size() > 0) {
				return String.valueOf(CustomerFeedback.size());
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
	public List<SlsTblCustomerFeedback> searchCustomerFeedback(SlsTblCustomerFeedback CustomerFeedback) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from SlsTblCustomerFeedback CustomerFeedback where 1=1 ";
	   
	  
	    
	    /*if(CustomerFeedback.getTxtEmail() !=null){
	    	query+=" and upper(CustomerFeedback.txtEmail) like"+" upper('"+CustomerFeedback.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    CfgTblUser 	cfgTblUser=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		if(cfgTblUser.getCfgTblCustomer()!=null)
		{
			if(cfgTblUser.getCfgTblCustomer().getBlIsDealer())
			{
				query+=" and CustomerFeedback.cfgTblCustomer.serCustomerId ="+" "+cfgTblUser.getCfgTblCustomer().getSerCustomerId()+""+"  ";
			}
			else
			{
				query+=" and CustomerFeedback.cfgTblCustomer.serCustomerId ="+" "+cfgTblUser.getCfgTblCustomer().getSerCustomerId()+""+"  ";
				
			}
		}
	    
	    if (CustomerFeedback.getDte_date_from() != null && CustomerFeedback.getDte_date_from().trim().length() > 0) {
			try {

				query+=  " and CustomerFeedback.dteDate >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(CustomerFeedback.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (CustomerFeedback.getDte_date_to() != null && CustomerFeedback.getDte_date_to().trim().length() > 0) {
			try {
				query+= " and CustomerFeedback.dteDate <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(CustomerFeedback.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	  
	    query+=" order by CustomerFeedback.serCustommerFeedbackId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<SlsTblCustomerFeedback> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	public void sendCustomerFeedbackinMail(SlsTblCustomerFeedback SlsTblCustomerFeedback)
	{
		 
		String toAddress="mkhalilawan@gmail.com";
		 
		 Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", true);
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", 587);
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            CfgTblCustomer cfgTblCustomer=new CfgTblCustomer();

            if(SlsTblCustomerFeedback.getCfgTblCustomer()!=null)
            	cfgTblCustomer= cfgTblCustomerDAO.getCustomerByPK(SlsTblCustomerFeedback.getCfgTblCustomer().getSerCustomerId().toString());

//			 ConnectDB db=new ConnectDB();
//			 ResultSet rs= null;
			Session session = Session.getInstance(props,
			  new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
//					return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com", "ICL*@12345");
//					return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com", "ICL*@12345");
					
					return new PasswordAuthentication("dealerfeedback@ittehadchemicals.com", "ICL*@12345");
					
				}
			  });

			try {
				Multipart multipart = new MimeMultipart();
		         
		            
		  				
	         Message mimeMessage = new MimeMessage(session);

		      MimeBodyPart messageBodyPart = new MimeBodyPart();
		      messageBodyPart.setContent("This Feedback <br/> "+SlsTblCustomerFeedback.getTxtRemarks()+" <br/> From Dealer "+cfgTblCustomer.getTxtCustomerName()+" ", "text/html");
		       
		       
		      // code to add attachment...will be revealed later
		      MimeBodyPart attachPart = new MimeBodyPart();
		      multipart.addBodyPart(messageBodyPart);
		      mimeMessage.setContent(multipart);
		      
		     mimeMessage.setFrom(new InternetAddress("dealerfeedback@ittehadchemicals.com"));
//		      mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new InternetAddress(\"iclportal5@gmail.com\"));"));
				/*mimeMessage.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse("support@ittehadchemicals.com"));*/
		     
		     mimeMessage.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse("dealerfeedback@ittehadchemicals.com"));
						
//					InternetAddress.parse("mkhalilawan@gmail.com"));
//				
				mimeMessage.setSubject("Customer Feedback");
				Transport.send(mimeMessage);

				System.out.println("Done");

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
	}
	
}
