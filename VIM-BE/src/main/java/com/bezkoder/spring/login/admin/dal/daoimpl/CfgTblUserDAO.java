package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;

import javax.management.relation.Role;
import javax.persistence.*;;


import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblUserDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordHistory;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import org.springframework.security.crypto.codec.Base64;

@Repository
public class CfgTblUserDAO implements ICfgTblUserDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	@Autowired
	private EmailService emailService;


	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ICfgTblRoleDAO cfgTblRoleDAO;

	@Value("${mail.smtp.username:}")
	private String smtpUsername;

	@Value("${mail.smtp.password:}")
	private String smtpPassword;

	private static final Logger log = LoggerFactory.getLogger(CfgTblUserDAO.class);

	  private static SecureRandom random = new SecureRandom();

	    private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
	    private static final String NUMERIC = "0123456789";
	    private static final String SPECIAL_CHARS = "!@#$%^&*_=+-/";

		private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
		private static final int PASSWORD_LENGTH = 12; // You can change this to your desired length

	
	public CfgTblUserDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblUser> getAllUser() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		/*List<CfgTblUser> Users = entityManager.createQuery("FROM CfgTblUser where blIsDeleted=FALSE or blIsDeleted is null ")
				.getResultList();*/
		List<CfgTblUser> users = entityManager
				.createQuery("SELECT user FROM CfgTblUser user " +
						"LEFT JOIN FETCH user.cfgTblRole role " +
						"WHERE user.blIsDeleted = FALSE OR user.blIsDeleted IS NULL", CfgTblUser.class)
				.getResultList();

		for (CfgTblUser user : users) {
			System.out.println("User: " + user.getTxtUserName());
			if (user.getCfgTblRole() != null) {
				System.out.println("Role: " + user.getCfgTblRole().getTxtRoleName());
			} else {
				System.out.println("Role: No role assigned");
			}
		}
		/*Hibernate.initialize(users.getCfgTblManager());*/
		entityManager.getTransaction().commit();
		entityManager.close();

		return users;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblUser> getActiveUser() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblUser> Users = entityManager
				.createQuery("FROM CfgTblUser where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Users;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblUser> getUserByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblUser where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serUserId <> " + oldValue;
			}
			List<CfgTblUser> Users = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Users;
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
	public String addNewUser(CfgTblUser CfgTblUser) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			//CfgTblUser.setBlnStatus(true);
			CfgTblUser.setBlIsDeleted(false);
			CfgTblUser.setBlIsActive(true);
			CfgTblUser.setBlIsPasswordChang(true);

			String plaintText = generateRandomPassword(PASSWORD_LENGTH);
			CfgTblUser.setTxtPassword(passwordEncoder.encode(plaintText));

			CfgTblCustomer cfgTblCustomerObj = null;
			if (CfgTblUser.getCfgTblRole() != null) {
				List<CfgTblRole> roles = cfgTblRoleDAO.getAllRole();
				Optional<CfgTblRole> roleOptional = roles.stream()
						.filter(role -> role.getSerRoleId() != null
								&& CfgTblUser.getCfgTblRole() != null
								&& CfgTblUser.getCfgTblRole().getSerRoleId() != null
								&& role.getSerRoleId().equals(CfgTblUser.getCfgTblRole().getSerRoleId()))
						.findFirst();

				if (roleOptional.isPresent()) {
					CfgTblUser.setTxtrole(roleOptional.get().getTxtRoleName());
				}
			}

			CfgTblRole cfgTblRole = new CfgTblRole();
			cfgTblRole.setSerRoleId(CfgTblUser.getCfgTblRole().getSerRoleId());
			CfgTblUser.setCfgTblRole(cfgTblRole);

			if (CfgTblUser.getCfgTblManager() != null && CfgTblUser.getCfgTblManager().getSerUserId() > 0) {
				cfgTblCustomerObj = entityManager.find(CfgTblCustomer.class, CfgTblUser.getCfgTblManager().getSerUserId());
			} else if (CfgTblUser.getCfgTblCustomer() != null) {
				cfgTblCustomerObj = entityManager.find(CfgTblCustomer.class, CfgTblUser.getCfgTblCustomer().getSerCustomerId());
			}

			CfgTblUser.setCfgTblCustomer(cfgTblCustomerObj);

			entityManager.persist(CfgTblUser);
			entityManager.getTransaction().commit();

			emailService.sendPassordinMail(CfgTblUser.getTxtAddress(), CfgTblUser.getTxtUserName(), plaintText);

			return "{\"status\":\"Success\"}";

		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			log.error("Error adding user: ", e);
			return "{\"status\":\"Failure\"}";

		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}


	public static String generateRandomPassword(int length) {
		SecureRandom random = new SecureRandom();
		StringBuilder password = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int index = random.nextInt(CHARACTERS.length());
			password.append(CHARACTERS.charAt(index));
		}

		return password.toString();
	}

	@Override
	public String deleteUser(List<String> UsersId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serUserId : UsersId) {
				CfgTblUser User = entityManager.find(CfgTblUser.class, Integer.parseInt(serUserId));
				if (User != null) {
					User.setBlIsDeleted(true);

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
	public String updateUser(CfgTblUser CfgTblUser) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			if (CfgTblUser == null || CfgTblUser.getSerUserId() == null) {
				entityManager.getTransaction().rollback();
				return "Failure";
			}

			CfgTblUser existingUser = entityManager.find(CfgTblUser.class, CfgTblUser.getSerUserId());
			if (existingUser == null) {
				entityManager.getTransaction().rollback();
				return "Failure";
			}

			// Preserve existing values for any field not provided by the client to avoid accidental data loss.
			if (CfgTblUser.getBlIsActive() == null) CfgTblUser.setBlIsActive(existingUser.getBlIsActive());
			if (CfgTblUser.getBlnStatus() == null) CfgTblUser.setBlnStatus(existingUser.getBlnStatus());
			if (CfgTblUser.getDteCreatedDate() == null) CfgTblUser.setDteCreatedDate(existingUser.getDteCreatedDate());
			if (CfgTblUser.getDteModifiedDate() == null) CfgTblUser.setDteModifiedDate(existingUser.getDteModifiedDate());
			if (CfgTblUser.getNumSubMenuOrder() == null) CfgTblUser.setNumSubMenuOrder(existingUser.getNumSubMenuOrder());
			if (CfgTblUser.getSerCreatedUser() == null) CfgTblUser.setSerCreatedUser(existingUser.getSerCreatedUser());
			if (CfgTblUser.getSerModifiedUser() == null) CfgTblUser.setSerModifiedUser(existingUser.getSerModifiedUser());
			if (isBlank(CfgTblUser.getTxtAddress())) CfgTblUser.setTxtAddress(existingUser.getTxtAddress());
			if (isBlank(CfgTblUser.getTxtCnic())) CfgTblUser.setTxtCnic(existingUser.getTxtCnic());
			if (isBlank(CfgTblUser.getTxtContactNo())) CfgTblUser.setTxtContactNo(existingUser.getTxtContactNo());
			if (isBlank(CfgTblUser.getTxtPassword())) CfgTblUser.setTxtPassword(existingUser.getTxtPassword());
			if (isBlank(CfgTblUser.getTxtUserName())) CfgTblUser.setTxtUserName(existingUser.getTxtUserName());
			if (isBlank(CfgTblUser.getTxtrole())) CfgTblUser.setTxtrole(existingUser.getTxtrole());
			if (CfgTblUser.getCfgTblUserRoles() == null) CfgTblUser.setCfgTblUserRoles(existingUser.getCfgTblUserRoles());
			if (CfgTblUser.getSerGroupId() == null) CfgTblUser.setSerGroupId(existingUser.getSerGroupId());
			if (CfgTblUser.getBlIsDeleted() == null) CfgTblUser.setBlIsDeleted(existingUser.getBlIsDeleted());
			if (CfgTblUser.getCfgTblRole() == null) CfgTblUser.setCfgTblRole(existingUser.getCfgTblRole());
			if (CfgTblUser.getBlIsPasswordChang() == null) CfgTblUser.setBlIsPasswordChang(existingUser.getBlIsPasswordChang());
			if (CfgTblUser.getCfgTblManager() == null) CfgTblUser.setCfgTblManager(existingUser.getCfgTblManager());
			if (CfgTblUser.getCfgTblPasswordPolicy() == null) CfgTblUser.setCfgTblPasswordPolicy(existingUser.getCfgTblPasswordPolicy());
			if (CfgTblUser.getDteExpiryDate() == null) CfgTblUser.setDteExpiryDate(existingUser.getDteExpiryDate());
			if (CfgTblUser.getNumAttempt() == null) CfgTblUser.setNumAttempt(existingUser.getNumAttempt());
			if (CfgTblUser.getCfgTblCustomer() == null) CfgTblUser.setCfgTblCustomer(existingUser.getCfgTblCustomer());
			if (CfgTblUser.getBlIsGroupCustomer() == null) CfgTblUser.setBlIsGroupCustomer(existingUser.getBlIsGroupCustomer());
			if (CfgTblUser.getHrTblDepartment() == null) CfgTblUser.setHrTblDepartment(existingUser.getHrTblDepartment());
			if (isBlank(CfgTblUser.getTxtSignaturePath())) CfgTblUser.setTxtSignaturePath(existingUser.getTxtSignaturePath());
			if (isBlank(CfgTblUser.getTxtDepartmentName())) CfgTblUser.setTxtDepartmentName(existingUser.getTxtDepartmentName());
			if (isBlank(CfgTblUser.getTxtDesignation())) CfgTblUser.setTxtDesignation(existingUser.getTxtDesignation());

			if (CfgTblUser.getCfgTblRole() != null && isBlank(CfgTblUser.getTxtrole())) {
				CfgTblUser.setTxtrole(CfgTblUser.getCfgTblRole().getTxtRoleName());
			}

			entityManager.merge(CfgTblUser);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	@Override
	public String generateUserNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getUserById(String UserId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblUser where txtUserCode='" + UserId + "'";

			List<CfgTblUser> User = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (User.size() > 0) {
				return String.valueOf(User.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public CfgTblUser getUserEntityById(Integer userId) {
		if (userId == null || userId <= 0) {
			return null;
		}
		EntityManager entityManager = getEntityManager();
		try {
			List<CfgTblUser> users = entityManager.createQuery(
							"SELECT user FROM CfgTblUser user " +
									"LEFT JOIN FETCH user.cfgTblRole role " +
									"WHERE user.serUserId = :userId " +
									"AND (user.blIsDeleted = FALSE OR user.blIsDeleted IS NULL)",
							CfgTblUser.class)
					.setParameter("userId", userId)
					.setMaxResults(1)
					.getResultList();
			return users.isEmpty() ? null : users.get(0);
		} catch (Exception e) {
			log.error("Error loading user by id: {}", userId, e);
			return null;
		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}
	
	@Override
	public List<CfgTblUser> searchUser(CfgTblUser User) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblUser User where 1=1 ";
	   
	    if(User.getTxtUserName() !=null){
	    	query+=" and upper(User.txtUserName) like"+" upper('"+User.getTxtUserName()+"%')"+"  ";
	    }
	    
	  
	    
	    if(User.getTxtAddress() !=null){
	    	query+=" and upper(User.txtAddress) like"+" upper('"+User.getTxtAddress()+"')"+"  ";
	    }
	    
	    if(User.getSerUserId() !=null){
	    	query+=" and User.serUserId ="+" "+User.getSerUserId()+""+"  ";
	    }
	    
	    if(User.getCfgTblCustomer()!=null && User.getCfgTblCustomer().getSerCustomerId() !=null){
	    	query+=" and User.cfgTblCustomer.serCustomerId ="+" "+User.getCfgTblCustomer().getSerCustomerId()+""+"  ";
	    }
	    
	    
	  
	    query+=" order by User.serUserId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblUser> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	
	
	
	
	public List<CfgTblUser> CheckUserDuplicationForUpdate(CfgTblUser User) {
		EntityManager entityManager = getEntityManager();
    entityManager.getTransaction().begin();
    String query = "from CfgTblUser User where 1=1 ";
   
    if(User.getTxtUserName() !=null){
    	query+=" and upper(User.txtUserName) like"+" upper('"+User.getTxtUserName()+"%')"+"  ";
    }
    
  
    
    if(User.getTxtAddress() !=null){
    	query+=" and upper(User.txtAddress) like"+" upper('"+User.getTxtAddress()+"')"+"  ";
    }
    
    if(User.getSerUserId() !=null){
    	query+=" and User.serUserId !="+" "+User.getSerUserId()+""+"  ";
    }
    
    
  
    query+=" order by User.serUserId  DESC";
    log.info("Query is ---"+query.substring(0, query.length()));
    
    System.out.println("query ----:"+query.substring(0, query.length()));
    String subQuery = query.substring(0, query.length());
    List<CfgTblUser> cust = entityManager.createQuery(
    		subQuery).getResultList();
    
    entityManager.getTransaction().commit();
    entityManager.close();
 
    return cust;
}
	
	public void sendPassordinMail(String mail,String user_name,String pass)
	{
		 Properties props = new Properties();
		 props.put("mail.smtp.auth", "true");
		 props.put("mail.smtp.starttls.enable", true);
		 props.put("mail.smtp.host", "smtp.gmail.com");
		 props.put("mail.smtp.port", 587);
//			props.put("mail.smtp.port", 465);
			
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");



//			 ConnectDB db=new ConnectDB();
//			 ResultSet rs= null;
			Session session = Session.getInstance(props,
			  new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(smtpUsername, smtpPassword);
				}
			  });

			try {
				Multipart multipart = new MimeMultipart();
		         
		            
		  				
	         Message mimeMessage = new MimeMessage(session);

		      MimeBodyPart messageBodyPart = new MimeBodyPart();
		      messageBodyPart.setContent("Your account User Name  is "+user_name+" and Password is "+pass, "text/html");
		       
		       
		      // code to add attachment...will be revealed later
		      MimeBodyPart attachPart = new MimeBodyPart();
		      multipart.addBodyPart(messageBodyPart);
		      mimeMessage.setContent(multipart);
		      
		     mimeMessage.setFrom(new InternetAddress("support@ittehadchemicals.com"));
//		      mimeMessage.setFrom(new InternetAddress("mimeMessage.setFrom(new InternetAddress(\"iclportal5@gmail.com\"));"));
				mimeMessage.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(mail));
//				
				mimeMessage.setSubject("Account Password");
				Transport.send(mimeMessage);

				System.out.println("Done");

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}
	
	
	 public static String generatePassword(int len, String dic) {
			String result = "";
			for (int i = 0; i < len; i++) {
			    int index = random.nextInt(dic.length());
			    result += dic.charAt(index);
			}
			return result;
		    }
	 
	 
		@SuppressWarnings("null")
		@Override
		public String userPasswordUpdate(int id, String NewPassword, String oldPassword) {
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				CfgTblUser CfgTblUserObject = entityManager.find(CfgTblUser.class, id);
				if (CfgTblUserObject == null) {
					entityManager.getTransaction().rollback();
					return "Failure";
				}

				if (NewPassword == null || NewPassword.trim().isEmpty()) {
					entityManager.getTransaction().rollback();
					return "Failure";
				}

				if (oldPassword != null && !oldPassword.trim().isEmpty()) {
					String existingPasswordHash = CfgTblUserObject.getTxtPassword();
					if (existingPasswordHash == null || !passwordEncoder.matches(oldPassword, existingPasswordHash)) {
						entityManager.getTransaction().rollback();
						return "CPNM";
					}
				}

				CfgTblUserObject.setTxtPassword(passwordEncoder.encode(NewPassword));
				entityManager.merge(CfgTblUserObject);
				entityManager.getTransaction().commit();
				emailService.sendPassordinMail(CfgTblUserObject.getTxtAddress(), CfgTblUserObject.getTxtUserName(),
						NewPassword);
				return "Success";
			} catch (Exception ex) {
				if (entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
				log.error(ex.getMessage(), ex);
				return "Failure";
			} finally {
				if (entityManager != null && entityManager.isOpen()) {
					entityManager.close();
				}
			}
		}
		
		
		public List<CfgTblPasswordHistory> getAllPasswordHistory(int userid) {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin(); 
			List<CfgTblPasswordHistory> lstPH = entityManager.createQuery("FROM CfgTblPasswordHistory ph where ph.serUserId = "+userid+" Order by ph.serUserId DESC").getResultList();
			
          /*  if(lstPH!=null && lstPH.size() >0)
            {
            	CfgTblPasswordHistory dtoPH;
            	for (int i=0;i <lstPH.size();i++)
            	 {
            		dtoPH=(CfgTblPasswordHistory)lstPH.get(i);
            	 }
            }*/
		   
				

			entityManager.getTransaction().commit();
			entityManager.close();

			return lstPH;
		}
		
		
	
		public String addNewPasswordHistory(CfgTblPasswordHistory cfgTblPasswordHistory) {
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				cfgTblPasswordHistory.setBlnStatus(true);
				cfgTblPasswordHistory.setBlIsDeleted(false);
				cfgTblPasswordHistory.setBlIsActive(true);
				cfgTblPasswordHistory.setDteDate(this.commonService.getCurrentTimeStamp_new());
				cfgTblPasswordHistory.setDteCreatedDate(this.commonService.getCurrentTimeStamp_new());
	
				  
				entityManager.persist(cfgTblPasswordHistory);
				entityManager.getTransaction().commit();
				entityManager.close();
				
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
		
		
		@Override
		public String ForgetPassword(CfgTblUser cfgTblUser) {
			EntityManager entityManager = getEntityManager();
			try {
				
				List lstUsers=searchUser(cfgTblUser);
				if(lstUsers!=null && lstUsers.size()==1)
				{
					entityManager.getTransaction().begin();
					CfgTblUser user=(CfgTblUser)lstUsers.get(0);
					user.setBlIsActive(true);
					user.setBlIsPasswordChang(true);
					user.setBlnStatus(true);
					String NewPassword = generatePassword(8, ALPHA_CAPS + ALPHA + SPECIAL_CHARS + NUMERIC);
					  byte[]   encrypted = Base64.encode(NewPassword.getBytes());
					/*String plaintText = generateRandomPassword(PASSWORD_LENGTH);*/
					//user.setTxtPassword(passwordEncoder.encode(plaintText));
					  user.setTxtPassword(new String(encrypted));
					  entityManager.merge(user);
						entityManager.getTransaction().commit();
						entityManager.close();
					   // emailService.sendPassordinMail(user.getTxtAddress(),user.getTxtUserName(),plaintText);
						//sendPassordinMail(user.getTxtAddress(),user.getTxtUserName(),NewPassword);
						return "Success";
				}
				else
				{
					return "NM";
				}
				
			
			
				
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		}
		
		
		@SuppressWarnings("unchecked")
		@Override
		public List<CfgTblUser> getCustomerActiveUser() {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			List<CfgTblUser> Users = entityManager
					.createQuery("FROM CfgTblUser where blnStatus=TRUE and (blIsDeleted=FALSE or blIsDeleted is null)  and cfgTblCustomer is not null").getResultList();

			entityManager.getTransaction().commit();
			entityManager.close();

			return Users;
		}


	public CfgTblUser getAdminOfCurrentUserRole(Integer userId) {
		EntityManager entityManager = getEntityManager();
		CfgTblUser adminUser = null;

		try {

			// Get roles associated with the user
			List<String> roles = entityManager.createQuery(
							"SELECT r.txtRoleName FROM CfgTblUser u JOIN u.cfgTblRole r WHERE u.serUserId = :userId")
					.setParameter("userId", userId)
					.getResultList();

			if (roles.isEmpty()) {
				throw new NoResultException("No roles found for user with ID: " + userId);
			}

			String currentRole = roles.get(0);

			if ("admin".equalsIgnoreCase(currentRole)) {
				return entityManager.find(CfgTblUser.class, userId);
			} else {
				adminUser = (CfgTblUser) entityManager.createQuery(
								"SELECT u FROM CfgTblUser u JOIN u.cfgTblRole r " +
										"WHERE r.txtRoleName = :roleName AND u.serUserId = :userId  And  u.cfgTblManager IS NULL")
						.setParameter("roleName", currentRole)
						.setParameter("userId", userId)
						.getSingleResult();

				return adminUser;
			}
		} catch (NoResultException e) {
			System.err.println("No admin found for the role: " + e.getMessage());
		} catch (Exception e) {
			// Handle other exceptions
			e.printStackTrace();
		} finally {
			entityManager.close();
		}

		return adminUser;
	}

	/*public CfgTblUser getAdminOfCurrentUserRole(Integer userId) {
		EntityManager entityManager = getEntityManager();
		CfgTblUser adminUser = null;

		try {
			entityManager.getTransaction().begin();
			String currentRole = (String) entityManager.createQuery(
							"SELECT r.txtRoleName FROM CfgTblUser u JOIN u.cfgTblRole r WHERE u.serUserId = :userId")
					.setParameter("userId", userId)
					.getSingleResult();

			if ("admin".equalsIgnoreCase(currentRole)) {

				return entityManager.find(CfgTblUser.class, userId);
			} else {

				 adminUser = (CfgTblUser) entityManager.createQuery(
								"SELECT u FROM CfgTblUser u JOIN u.cfgTblRole r " +
										"WHERE r.txtRoleName = :roleName AND u.cfgTblManager IS NULL")
						.setParameter("roleName", currentRole)
						.getSingleResult();

				return adminUser;
			}

		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			e.printStackTrace();
		} finally {
			entityManager.close();
		}

		return adminUser;
	}*/


}
