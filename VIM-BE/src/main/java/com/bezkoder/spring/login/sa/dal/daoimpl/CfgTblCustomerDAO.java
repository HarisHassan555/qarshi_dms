package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

import javax.persistence.*;;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.xmlbeans.impl.soap.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.dto.ZhamSdFromDmsCustmrCreate;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomerDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.daoimpl.CfgTblUserDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblPasswordPolicy;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

@Repository
public class CfgTblCustomerDAO implements ICfgTblCustomerDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;
	
	 @Autowired
	  private LoginDAO loginDao;
	 
	 @Autowired
	  private CfgTblUserDAO userDAO  ;
	 

	private static final Logger log = LoggerFactory.getLogger(CfgTblCustomerDAO.class);

	public CfgTblCustomerDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getAllCustomer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCustomer> Customers=new ArrayList();
		//CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			 Customers = entityManager
//						.createQuery("FROM CfgTblCustomer where  serGroupId= "+user.getSerGroupId()+"  and ( blIsDealer=FALSE or blIsDealer is null ) and blIsDeleted=FALSE").getResultList();
//		}
//		else
		/*if(user !=null 	&& user.getCfgTblCustomer()!=null &&  user.getCfgTblCustomer().getSerCustomerId() >0)
		{
			 Customers = entityManager
						.createQuery("FROM CfgTblCustomer where  cfgTblCustomer.serCustomerId= "+user.getCfgTblCustomer().getSerCustomerId()+"  and ( blIsDealer=FALSE or blIsDealer is null ) and blIsDeleted=FALSE").getResultList();
		}
		else*/
			Customers = entityManager
				.createQuery("FROM CfgTblCustomer where  blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getActiveCustomer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
//		CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
		List<CfgTblCustomer> Customers=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//{
//	 Customers = entityManager
//				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and serGroupId= "+user.getSerGroupId()+" and ( blIsDealer=FALSE or blIsDealer is null )  and blIsDeleted=FALSE").getResultList();
//}
//else
	Customers = entityManager
				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and ( blIsDealer=FALSE or blIsDealer is null ) and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getCustomerWODealer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
//		CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
		List<CfgTblCustomer> Customers=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//{
//	 Customers = entityManager
//				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and serGroupId= "+user.getSerGroupId()+" and ( blIsDealer=FALSE or blIsDealer is null ) and  cfgTblCustomer is null and blIsDeleted=FALSE").getResultList();
//}
//else
	Customers = entityManager
				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and ( blIsDealer=FALSE or blIsDealer is null ) and  cfgTblCustomer is null and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getCustomerByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomer where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblCustomer> Customers = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Customers;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	SHIntegeration integeration=new SHIntegeration();
	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	/*
	 * String date = simpleDateFormat.format(new Date());
	 * System.out.println(date);
	 */
	@Override
	public String addNewCustomer(CfgTblCustomer CfgTblCustomer) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCustomer.setBlnStatus(true);
			CfgTblCustomer.setBlIsDeleted(false);
			CfgTblCustomer.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblCustomer.setDteCreateddate(commonService.getCurrentTimeStamp_new());
		//	CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			String msg="";
			 try {
				 if(CfgTblCustomer.getCfgTblCustomer()!=null)
				 {
					 CfgTblCustomer dealer =this.getCustomerByPK(CfgTblCustomer.getCfgTblCustomer().getSerCustomerId().toString());
					 msg = integeration.CreateCustomerInSHERP(CfgTblCustomer,dealer);
				 }
				 else
				msg = integeration.CreateCustomerInSHERP(CfgTblCustomer,null);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			CfgTblCustomer.setTxtReturnMsg(msg);
			if(msg.indexOf("Success") >0)
			{
				CfgTblCustomer.setBlIsPOSTEDToSAP(true);
				
			}

			entityManager.persist(CfgTblCustomer);
			entityManager.getTransaction().commit();
			entityManager.close();
			
			
//			CfgTblCustomer=this.getCustomerByPK(CfgTblCustomer.getSerCustomerId().toString());
			
			
//			
//			boolean status = false;//SaveCustommerInSAP(CfgTblCustomer);
			
			CfgTblUser cfgTblUser = new CfgTblUser();
			if (CfgTblCustomer.getTxtUserName() !=null && CfgTblCustomer.getTxtUserName().length() >0) {
				
				if (CfgTblCustomer.getBlIsGroup() == null || !(CfgTblCustomer.getBlIsGroup())) {
					cfgTblUser.setCfgTblCustomer(CfgTblCustomer);
					CfgTblPasswordPolicy cfgTblPasswordPolicy = new CfgTblPasswordPolicy();
					cfgTblPasswordPolicy.setSerPasswordPolicyId(1);
					cfgTblUser.setTxtAddress(CfgTblCustomer.getTxtEmailAddress());
					cfgTblUser.setTxtAddress(CfgTblCustomer.getTxtEmailAddress());
					cfgTblUser.setBlIsActive(true);
					cfgTblUser.setBlIsDeleted(false);
					cfgTblUser.setBlIsPasswordChang(true);
					cfgTblUser.setNumAttempt(new BigDecimal(0));
					cfgTblUser.setBlnStatus(true);
					//			cfgTblUser.setTxtUserName(CfgTblCustomer.getTxtEmailAddress());
					cfgTblUser.setTxtUserName(CfgTblCustomer.getTxtUserName());
					cfgTblUser.setCfgTblPasswordPolicy(cfgTblPasswordPolicy);
					
					if (CfgTblCustomer.getTxtDivision() != null && CfgTblCustomer.getTxtDivision().equalsIgnoreCase("Labsa"))
						CfgTblCustomer.setBlIsLabsa(true);
					
					if (CfgTblCustomer.getBlIsDealer() != null && CfgTblCustomer.getBlIsDealer())
						cfgTblUser.setTxtrole("DEALER");
					else
						cfgTblUser.setTxtrole("CUSTOMER");
					userDAO.addNewUser(cfgTblUser);
				} else {
					////////////set group status in user and so we can select from in customer for that delaer
					cfgTblUser.setCfgTblCustomer(CfgTblCustomer.getCfgTblGroupCustomer());
					List lstUser = userDAO.searchUser(cfgTblUser);
					if (lstUser != null && lstUser.size() > 0) {
						cfgTblUser = (CfgTblUser) lstUser.get(0);
						if (cfgTblUser.getBlIsGroupCustomer() == null || !cfgTblUser.getBlIsGroupCustomer()) {
							cfgTblUser.setBlIsGroupCustomer(true);
							userDAO.updateUser(cfgTblUser);
						}

					}
				} 
			} else {
				////////////set group status in user and so we can select from in customer for that delaer
				cfgTblUser.setCfgTblCustomer(CfgTblCustomer.getCfgTblGroupCustomer());
				List lstUser = userDAO.searchUser(cfgTblUser);
				if (lstUser != null && lstUser.size() > 0) {
					cfgTblUser = (CfgTblUser) lstUser.get(0);
					if (cfgTblUser.getBlIsGroupCustomer() == null || !cfgTblUser.getBlIsGroupCustomer()) {
						cfgTblUser.setBlIsGroupCustomer(true);
						userDAO.updateUser(cfgTblUser);
					}

				}
			} 
//			if(!status)
//			{
//				return "NSuccess";
//			}
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteCustomer(List<String> CustomersId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serCustomerId : CustomersId) {
				CfgTblCustomer Customer = entityManager.find(CfgTblCustomer.class, Integer.parseInt(serCustomerId));
				if (Customer != null) {
					Customer.setBlIsDeleted(true);

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
	public String updateCustomer(CfgTblCustomer CfgTblCustomer) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblCustomer);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateCustomerNo(String type) {
		
		
		    
		// int CustomerNo;
		String CustomerType = type;
		// String CustomerCode="";
		Double ord_no = 0.0;
		Double ord_no1 = 0.0;
		EntityManager entityManager = getEntityManager();
		if (CustomerType.equalsIgnoreCase("6")) {
			try {
				entityManager.getTransaction().begin();

//				String zoneCode = (String) entityManager.createQuery("select MAX( txtCustomerCode ) from CfgTblCustomer c where c.cfgTblCustomerCategory.serCustomerCategoryId=6")
//						.getSingleResult();
				
				String zoneCode = (String) entityManager.createQuery("select txtCustomerCode from CfgTblCustomer c  where c.serCustomerId= (select max(serCustomerId) from CfgTblCustomer d where max(serCustomerId) < 1000000 )")
						.getSingleResult();
				
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "100001";
				}
				ord_no = Double.valueOf(zoneCode);

				ord_no = ord_no + 1;
				
				return ord_no+"";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "100001";
		} 
		else if (CustomerType.equalsIgnoreCase("7")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX( txtCustomerCode ) from CfgTblCustomer c where c.cfgTblCustomerCategory.serCustomerCategoryId=7")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "200001";
				}
				ord_no = Double.valueOf(zoneCode);

				ord_no = ord_no + 1;
				
				return ord_no+"";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "200001";
		} 
		
		else if (CustomerType.equalsIgnoreCase("8")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX( txtCustomerCode ) from CfgTblCustomer c where c.cfgTblCustomerCategory.serCustomerCategoryId=8")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "300001";
				}
				ord_no = Double.valueOf(zoneCode);

				ord_no = ord_no + 1;
				
				return ord_no+"";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "300001";
		} 
		
		else	if (CustomerType.equalsIgnoreCase("9")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX( txtCustomerCode ) from CfgTblCustomer c where c.cfgTblCustomerCategory.serCustomerCategoryId=9")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "4100001";
				}
				ord_no = Double.valueOf(zoneCode);

				ord_no = ord_no + 1;
				
				return ord_no+"";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "400001";
		} 
		
		
		
		else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX( txtCustomerCode ) from CfgTblCustomer c where c.cfgTblCustomerCategory.serCustomerCategoryId=6")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "100001";
				}
				ord_no = Double.valueOf(zoneCode);

				ord_no = ord_no + 1;
				
				return ord_no+"";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "100001";
		}
	}
	
	public HttpHeaders getHttpHeaders() {
		HttpHeaders header = new HttpHeaders();
		header.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
		header.setContentType(MediaType.APPLICATION_XML);
		header.set("UserName", "");
		header.set("Password", "");
//		header.set(CommonConstant.LANG_ID, String.valueOf(getLanguageId()));
//		header.set(CommonConstant.SESSION, getSession());
		// header.set(CommonConstant.LANG_ORIENTATION, getLanguageOrientation());
		return header;
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getCustomerById(String CustomerId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomer where txtCustomerCode='" + CustomerId + "'";

			List<CfgTblCustomer> Customer = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Customer.size() > 0) {
				return String.valueOf(Customer.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	public CfgTblCustomer getCustomerBycode(String CustomerId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomer where txtCustomerCode='" + CustomerId + "'";

			List<CfgTblCustomer> Customer = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Customer.size() > 0) {
				return Customer.get(0);
			}
			return null;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	public CfgTblCustomer getCustomerByPK(String CustomerId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblCustomer where serCustomerId=" + CustomerId + "";

			List<CfgTblCustomer> Customer = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Customer.size() > 0) {
				return  (CfgTblCustomer)Customer.get(0);
			}
			return new CfgTblCustomer();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblCustomer> searchCustomer(CfgTblCustomer Customer) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblCustomer Customer where 1=1 ";
	    if(Customer.getTxtCustomerCode() != null)
	    {
	    	query+=" and upper(Customer.txtCustomerCode) like"+" upper('"+Customer.getTxtCustomerCode()+"%')"+" ";
	    }
	    if(Customer.getTxtCustomerName() !=null){
	    	query+=" and upper(Customer.txtCustomerName) like"+" upper('"+Customer.getTxtCustomerName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Customer.getTxtEmail() !=null){
	    	query+=" and upper(Customer.txtEmail) like"+" upper('"+Customer.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Customer.getSerCustomerId() !=null){
	    	query+=" and Customer.serCustomerId ="+" "+Customer.getSerCustomerId()+""+"  ";
	    }
	  
	    query+=" order by Customer.serCustomerId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblCustomer> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getAllDealer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCustomer> Customers=new ArrayList();
	//	CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			 Customers = entityManager
//						.createQuery("FROM CfgTblCustomer where  serGroupId= "+user.getSerGroupId()+" and blIsDealer=TRUE  and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Customers = entityManager
				.createQuery("FROM CfgTblCustomer where blIsDealer=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getAllMainDealer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblCustomer> Customers=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			 Customers = entityManager
//						.createQuery("FROM CfgTblCustomer where  serGroupId= "+user.getSerGroupId()+" and cfgTblCustomerCategory.txtCustomerCategoryCode = 'ZC04'  and blIsDealer=TRUE  and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Customers = entityManager
				.createQuery("FROM CfgTblCustomer where    blIsDealer=TRUE  and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getActiveDealer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
//		CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
		List<CfgTblCustomer> Customers=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//{
//	 Customers = entityManager
//				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and serGroupId= "+user.getSerGroupId()+" and  blIsDealer=TRUE and blIsDeleted=FALSE").getResultList();
//}
//else
	Customers = entityManager
				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE and  blIsDealer=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblCustomer> getgroupActiveCustomer() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
//		CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  
		List<CfgTblCustomer> Customers=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
if(user !=null 	&& user.getBlIsGroupCustomer()!= null && user.getBlIsGroupCustomer())
{
	 Customers = entityManager
				.createQuery("FROM CfgTblCustomer where blnStatus=TRUE  and ( blIsDealer=FALSE or blIsDealer is null )  and blIsDeleted=FALSE "
						+ " and cfgTblCustomer.serCustomerId in  ( select cust.serCustomerId  FROM CfgTblCustomer cust where "  
				        + "cust.serCustomerId="+user.getCfgTblCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +user.getCfgTblCustomer().getSerCustomerId() 
						 
						+ " ) ").getResultList();
}

		entityManager.getTransaction().commit();
		entityManager.close();

		return Customers;
	}
	
	
	@Override
	public CfgTblCustomer addNewCustomerFromSapOrder(CfgTblCustomer CfgTblCustomer) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCustomer.setBlnStatus(true);
			CfgTblCustomer.setBlIsDeleted(false);
			CfgTblCustomer.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblCustomer.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  

			entityManager.persist(CfgTblCustomer);
			entityManager.getTransaction().commit();
			entityManager.close();
		
	
			return CfgTblCustomer;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new CfgTblCustomer();
		}
	}
	
	
	
	
    private  String createSOAPRequest(CfgTblCustomer dto) throws Exception {

    	log.info("inside-------createSOAPRequest");
  	
  	   ZhamSdFromDmsCustmrCreate customer=new ZhamSdFromDmsCustmrCreate();
  	   MessageFactory messageFactory = MessageFactory.newInstance();
  	   String serverURI = "urn:sap-com:document:sap:soap:functions:mc-style";
       final SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
       String      authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
       MimeHeaders hd            = soapMessage.getMimeHeaders();
       hd.addHeader("Authorization", "Basic " + authorization);
       SOAPPart soapPart = soapMessage.getSOAPPart();
       SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
//       soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:soap:functions:mc-style");
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
       
       
//       if(dto.getTxtMobileNo()!=null && dto.getTxtMobileNo().trim().length() >0)
//    	   customer.setCellnumber(dto.getTxtMobileNo());
//           else
//        	   customer.setCellnumber(" ");
       
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
//       jaxbMarshaller.marshal(customer, System.err);
       
       
       
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
		     
//		     String username="DMIS_USER";
//		     String password="Abc@1234567";
		     
		     //String url = "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/wsdl/flv_10002A111AD1/bndg_url/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create?sap-client=110";
//		     String url = "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create";
//		     String url = "http://vhgdiqs4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/100/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create";
		                 
		     String url = ServerConfiguration.service_customer;
		     log.info("URL---customer--:"+url);
		     URL obj = new URL(url);
		 HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 con.setRequestMethod("PUT");
		 con.setRequestProperty("Content-Type","application/soap+xml");
//		 con.setRequestProperty("Cookie","sap-usercontext=sap-client=110");
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
			updateCustomer(dto);
		 if(responseStatus.equalsIgnoreCase("Internal Server Error"))
		 {
			
			 log.info("------here is internal server error --------:");
			 dto.setTxtReturnMsg(responseStatus);
				updateCustomer(dto);
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
		 updateCustomer(dto);

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
//							System.out.print("" + xmlStreamReader.getElementText());
							
							String ret_msg= xmlStreamReader.getElementText();
							
							if(ret_msg.equalsIgnoreCase("Success"))
							{
								
							dto.setBlIsPOSTEDToSAP(true);
							updateCustomer(dto);
							}
							else
							{
								dto.setTxtErrorMsgFromSap(ret_msg);
								dto.setBlIsPOSTEDToSAP(false);
															
								updateCustomer(dto);
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
  
  
	@Override
	public String updateAndrePostCustomerInSAP(CfgTblCustomer CfgTblCustomer) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblCustomer);
			entityManager.getTransaction().commit();
			entityManager.close();
			log.info("-------------------REPOSTING CUSTOMER-------------------------");
//			CfgTblCustomer=this.getCustomerByPK(CfgTblCustomer.getSerCustomerId().toString());
//			boolean status = SaveCustommerInSAP(CfgTblCustomer);
//			if(!status)
//			{
//				return "NSuccess";
//			}
			log.info("-------------------END OF REPOSTING CUSTOMER-------------------------");
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}
	
	
	
	@Override
	public CfgTblCustomer addNewCustomerRC(CfgTblCustomer CfgTblCustomer) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblCustomer.setBlnStatus(true);
			CfgTblCustomer.setBlIsDeleted(false);
			CfgTblCustomer.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblCustomer.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			CfgTblCustomer.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());  

			entityManager.persist(CfgTblCustomer);
			entityManager.getTransaction().commit();
			entityManager.close();
			CfgTblCustomer=this.getCustomerByPK(CfgTblCustomer.getSerCustomerId().toString());
		

		
		
			return CfgTblCustomer;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	

}
