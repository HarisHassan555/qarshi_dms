package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.bezkoder.spring.login.admin.bll.servicesimpl.SoapClientService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.UserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.sa.bll.dto.*;
import com.bezkoder.spring.login.sa.dal.entities.*;

import com.bezkoder.spring.login.ses.ZKFEINVOICESESWEBSERVICE;
import org.apache.http.conn.ConnectTimeoutException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblDealDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.util.UtilDateAndTime;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

@Repository
@Transactional
public class SlsTblSaleOrderDAO implements ISlsTblSaleOrderDAO {

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

    @Autowired
    private ISlsTblDealDAO slsTblDealDAO;

    @Autowired
    @Lazy
    private SoapClientService soapClientService;


    @Autowired
    private ISlsTblDealDAO citTableDealDAO;

    @Autowired
    private UserService userService;


    @Autowired
    private AuditSlsTblSaleOrderRepository auditSlsTblSaleOrderRepository;

    private static final Logger log = LoggerFactory.getLogger(SlsTblSaleOrderDAO.class);

    public SlsTblSaleOrderDAO() {
        // TODO Auto-generated constructor stub
    }

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSaleOrder> getAllSaleOrder() {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        /*
         * List<SlsTblSaleOrder> SaleOrders =
         * entityManager.createQuery("FROM SlsTblSaleOrder where blIsDeleted=FALSE")
         * .getResultList();
         */

        List<SlsTblSaleOrder> SaleOrders = new ArrayList();
        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

        if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
            SaleOrders = entityManager.createQuery(
                            " FROM SlsTblSaleOrder where   serGroupId= " + user.getSerGroupId() + " and blIsDeleted=FALSE ")
                    .getResultList();
        } else
            SaleOrders = entityManager.createQuery(
//					" FROM SlsTblSaleOrder where blIsDeleted=FALSE and dteDate > '2022-07-01'  and (numTotal-numAmountReceived) > 0 and numLevel <=3 order by serSaleOrderId DESC")
                            " FROM SlsTblSaleOrder where blIsDeleted=FALSE and dteDate > '2023-07-31'  and numLevel <=5  order by serSaleOrderId DESC")
                    .getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return SaleOrders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSaleOrder> getActiveSaleOrder() {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        /*
         * List<SlsTblSaleOrder> SaleOrders = entityManager
         * .createQuery("FROM SlsTblSaleOrder where blnStatus=1 and blIsDeleted=FALSE").
         * getResultList();
         */

        List<SlsTblSaleOrder> SaleOrders = new ArrayList();
        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

        if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
            SaleOrders = entityManager.createQuery(" FROM SlsTblSaleOrder where blnStatus=1  and  serGroupId= "
                    + user.getSerGroupId() + " and blIsDeleted=FALSE ").getResultList();
        } else
            SaleOrders = entityManager.createQuery(" FROM SlsTblSaleOrder where blnStatus=1  and blIsDeleted=FALSE ")
                    .getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return SaleOrders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSaleOrder> getSaleOrderByProperty(String property, String value, String mode, String oldValue) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSaleOrder where " + property + "='" + value + "'";
            if (mode.equals("Edit")) {
                query += " and serBranchId <> " + oldValue;
            }
            List<SlsTblSaleOrder> SaleOrders = entityManager.createQuery(query).getResultList();

            entityManager.close();
            return SaleOrders;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    String pattern = "yyyy-MM-dd";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    @Override
    public String addNewSaleOrderWithPaymentDocument(SlsTblSaleOrder SlsTblSaleOrder, SOPaymentDocument PD) {

        String msg = addNewSaleOrder(SlsTblSaleOrder);
         /* if (PD.getDocumentFile() != null) { SlsTblSoPayments slsTblSoPayments = new
          SlsTblSoPayments(); slsTblSoPayments.setSlsTblSaleOrder(SlsTblSaleOrder);
          List<SlsTblSoPayments> lstPayments =
          searchSaleOrderPayments(slsTblSoPayments); if (lstPayments != null &&
          lstPayments.size() > 0) { PD.setSlsTblSaleOrder(lstPayments.get(0));
          uploadPaymentDocument(PD); } }*/
        return msg;
    }

    /*
     * String date = simpleDateFormat.format(new Date()); System.out.println(date);
     */
    @Override
    public String addNewSaleOrder(SlsTblSaleOrder SlsTblSaleOrder) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            // SlsTblSaleOrder.setBlnStatus(true);
            SlsTblSaleOrder.setBlIsDeleted(false);
            SlsTblSaleOrder.setNumLevel(1);
            SlsTblSaleOrder.setTxtLevel("First");
            SlsTblSaleOrder.setTxtStatus1("Pending");
            SlsTblSaleOrder.setTxtStatus2("Pending");

            if (SlsTblSaleOrder.getTxtpOrderType() != null && SlsTblSaleOrder.getTxtpOrderType().trim().equalsIgnoreCase("Spare parts")) {
                SlsTblSaleOrder.setBlIsSales(false);
            } else {
                SlsTblSaleOrder.setBlIsSales(true);
            }


            if (!(SlsTblSaleOrder.getBlIsGAL() != null && SlsTblSaleOrder.getBlIsGAL())) {
                SlsTblSaleOrder.setTxtType("DF");
            } else {
                SlsTblSaleOrder.setTxtType("GAL");
            }

            SlsTblSaleOrder.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
            Date currentDate = new Date();
            SlsTblSaleOrder.setDteCreateddate(this.getTimestampWithZeroTime(currentDate));
            SlsTblSaleOrder.setSerGroupId(
                    this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());

            try {
                if (!(SlsTblSaleOrder.getSerGroupId() != null && SlsTblSaleOrder.getSerGroupId() > 0)) {
                    CfgTblCustomer cust = cfgTblCustomerDAO.getCustomerByPK("471");
                    if (cust != null && cust.getSerGroupId() != null && cust.getSerGroupId() > 0) {
                        SlsTblSaleOrder.setSerGroupId(cust.getSerGroupId());
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // SlsTblSaleOrder.setTxtStatus("Approved");

            if (SlsTblSaleOrder.getCfgTblDealer() != null) {
                if (SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee() != null) {
                    HrTblEmployee hrTblEmployee = new HrTblEmployee();
                    hrTblEmployee
                            .setSerEmployeeId(SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
                    hrTblEmployee.setTxtEmployeeName(
                            SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeName());
                    hrTblEmployee.setTxtEmployeeCode(
                            SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getTxtEmployeeCode());
                    hrTblEmployee.setTxtEmail(SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getTxtEmail());
                    hrTblEmployee.setTxtMobileNo(SlsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getTxtMobileNo());
                    SlsTblSaleOrder.setHrTblEmployee(hrTblEmployee);
                }

                if (SlsTblSaleOrder.getCfgTblDealer().getBlnIsExport() != null
                        && SlsTblSaleOrder.getCfgTblDealer().getBlnIsExport() == true) {
                    CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
                    cfgTblDocumentType.setSerDocumentTypeId(3);
                    cfgTblDocumentType.setTxtCode("ZEXP");
                    SlsTblSaleOrder.setCfgTblDocumentType(cfgTblDocumentType);

                    CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();

                    if (SlsTblSaleOrder.getCfgTblCustomer() != null
                            && SlsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                        cfgTblDistributionChannel.setSerDistributionChannelId(4);
                        cfgTblDistributionChannel.setTxtCode("40");
                    } else {
                        cfgTblDistributionChannel.setSerDistributionChannelId(3);
                        cfgTblDistributionChannel.setTxtCode("30");
                    }

                    SlsTblSaleOrder.setCfgTblDistributionChannel(cfgTblDistributionChannel);

                } else {

                    CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();

                    cfgTblDocumentType.setSerDocumentTypeId(2);
                    cfgTblDocumentType.setTxtCode("ZLOC");
                    SlsTblSaleOrder.setCfgTblDocumentType(cfgTblDocumentType);
                    CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();

                    if (SlsTblSaleOrder.getCfgTblCustomer() != null
                            && SlsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                        cfgTblDistributionChannel.setSerDistributionChannelId(1);
                        cfgTblDistributionChannel.setTxtCode("10");
                    } else {
                        cfgTblDistributionChannel.setSerDistributionChannelId(2);
                        cfgTblDistributionChannel.setTxtCode("20");
                    }

                    SlsTblSaleOrder.setCfgTblDistributionChannel(cfgTblDistributionChannel);

                }

                if (SlsTblSaleOrder.getCfgTblDealer().getTxtDivision() != null)
                    if (SlsTblSaleOrder.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Agri")) {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(2);
                        cfgTblDivision.setTxtCode("20");
                        cfgTblDivision.setTxtName("Agri");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    } else if (SlsTblSaleOrder.getCfgTblDealer().getTxtDivision().equalsIgnoreCase("Labsa")) {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(3);
                        cfgTblDivision.setTxtCode("30");

                        cfgTblDivision.setTxtName("Labsa");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    } else {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(1);
                        cfgTblDivision.setTxtCode("10");
                        cfgTblDivision.setTxtName("Chemical");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    }

                if (SlsTblSaleOrder.getCfgTblDealer().getCfgTblIncoTerm() != null) {
                    SlsTblSaleOrder.setCfgTblIncoTerm(SlsTblSaleOrder.getCfgTblDealer().getCfgTblIncoTerm());
                }

            } else if (SlsTblSaleOrder.getCfgTblCustomer() != null) {

                if (SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee() != null) {
                    HrTblEmployee hrTblEmployee = new HrTblEmployee();
                    hrTblEmployee.setSerEmployeeId(
                            SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
                    hrTblEmployee.setTxtEmployeeName(
                            SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeName());
                    hrTblEmployee.setTxtEmployeeCode(
                            SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getTxtEmployeeCode());
                    hrTblEmployee.setTxtEmail(SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getTxtEmail());
                    hrTblEmployee
                            .setTxtMobileNo(SlsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getTxtMobileNo());
                    SlsTblSaleOrder.setHrTblEmployee(hrTblEmployee);
                }

                CfgTblDistributionChannel cfgTblDistributionChannel = new CfgTblDistributionChannel();
                if (SlsTblSaleOrder.getCfgTblCustomer().getBlnIsExport() != null
                        && SlsTblSaleOrder.getCfgTblCustomer().getBlnIsExport() == true) {
                    CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();
                    cfgTblDocumentType.setSerDocumentTypeId(3);
                    cfgTblDocumentType.setTxtCode("ZEXP");
                    SlsTblSaleOrder.setCfgTblDocumentType(cfgTblDocumentType);
                    cfgTblDistributionChannel.setSerDistributionChannelId(3);
                    cfgTblDistributionChannel.setTxtCode("30");
                } else {
                    CfgTblDocumentType cfgTblDocumentType = new CfgTblDocumentType();

                    cfgTblDocumentType.setSerDocumentTypeId(2);
                    cfgTblDocumentType.setTxtCode("ZLOC");
                    SlsTblSaleOrder.setCfgTblDocumentType(cfgTblDocumentType);
                    cfgTblDistributionChannel.setSerDistributionChannelId(2);
                    cfgTblDistributionChannel.setTxtCode("20");
                }
                SlsTblSaleOrder.setCfgTblDistributionChannel(cfgTblDistributionChannel);
                if (SlsTblSaleOrder.getCfgTblCustomer().getTxtDivision() != null)
                    if (SlsTblSaleOrder.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Agri")) {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(2);
                        cfgTblDivision.setTxtCode("20");
                        cfgTblDivision.setTxtName("Agri");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    } else if (SlsTblSaleOrder.getCfgTblCustomer().getTxtDivision().equalsIgnoreCase("Labsa")) {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(3);
                        cfgTblDivision.setTxtCode("30");
                        cfgTblDivision.setTxtName("Labsa");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    } else {
                        CfgTblDivision cfgTblDivision = new CfgTblDivision();
                        cfgTblDivision.setSerDivisionId(1);
                        cfgTblDivision.setTxtCode("10");
                        cfgTblDivision.setTxtName("Chemical");
                        SlsTblSaleOrder.setCfgTblDivision(cfgTblDivision);
                    }

                if (SlsTblSaleOrder.getCfgTblCustomer().getCfgTblIncoTerm() != null) {
                    SlsTblSaleOrder.setCfgTblIncoTerm(SlsTblSaleOrder.getCfgTblCustomer().getCfgTblIncoTerm());
                }

            }

            CfgTblSalesOrganization cfgTblSalesOrganization = new CfgTblSalesOrganization();
           // cfgTblSalesOrganization.setSerSalesOrganizationId(null);
           // cfgTblSalesOrganization.setTxtCode("1000");
            SlsTblSaleOrder.setCfgTblSalesOrganization(null);
            String order_no = SlsTblSaleOrder.getTxtSaleOrderNo();   // generateSaleOrderNoFORGNL(SlsTblSaleOrder);
            if (order_no != null && order_no.trim().length() > 0)
                SlsTblSaleOrder.setTxtSaleOrderNo(order_no);
            entityManager.persist(SlsTblSaleOrder);

            SlsTblSoDetail soDetail;
            Iterator<SlsTblSoDetail> itr = SlsTblSaleOrder.getSlsTblSoDetails().iterator();
            SlsTblSoDetail details = new SlsTblSoDetail();
            CfgTblProduct product = new CfgTblProduct();
            double order_Qty = 0;
            while (itr.hasNext()) {

                soDetail = (SlsTblSoDetail) itr.next();
                details = soDetail;
                if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                    /*
                     * if(soDetail.getCfgTblProductDesign()!=null &&
                     * soDetail.getCfgTblProductDesign().getSerProductDesignId()==0) {
                     * soDetail.setCfgTblProductDesign(null); }
                     */
                    SlsTblSaleOrder.setCfgTblProduct(soDetail.getCfgTblProduct());
                    SlsTblSaleOrder.setNumQuantity(soDetail.getNumQuantity());
//					entityManager.merge(SlsTblSaleOrder);

                    /*CfgTblProductDesign cfgTblProductDesign = new CfgTblProductDesign();
                    cfgTblProductDesign.setSerProductDesignId(0);*/
                    soDetail.setCfgTblProductDesign(null);
                    soDetail.setBlIsDeleted(false);
                    soDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
                    soDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
                    BigDecimal numQuantity = soDetail.getNumQuantity();  // Assuming this returns a BigDecimal
                    double quantityAsDouble = numQuantity.doubleValue();
                    BigDecimal numItemPrice = soDetail.getNumItemPrice();  // Assuming this returns a BigDecimal
                    double numItemPriceAsDouble = numItemPrice.doubleValue();
                    double result = calculatePercentage(numItemPriceAsDouble,quantityAsDouble);
                    BigDecimal storedBalance = new BigDecimal(String.valueOf(soDetail.getNumBalance()));
                    BigDecimal  currentBalance = new BigDecimal(String.valueOf(result));
                    BigDecimal updatedBalance = storedBalance.subtract(currentBalance);

                    // Output the result
                    System.out.println("Updated Balance: " + updatedBalance);
                    soDetail.setNumBalance(updatedBalance);
                    /*soDetail.setSapLineItem(soDetail.getSapLineItem());*/
                    soDetail.setSlsTblSaleOrder(SlsTblSaleOrder);

                    entityManager.persist(soDetail);

                    SlsTblSaleItemSchedule slsTblSaleItemSchedule;

                    if (soDetail.getSlsTblSaleItemSchedule() != null) {
                        Iterator<SlsTblSaleItemSchedule> itr_sch = soDetail.getSlsTblSaleItemSchedule().iterator();

                        while (itr_sch.hasNext()) {

                            slsTblSaleItemSchedule = (SlsTblSaleItemSchedule) itr_sch.next();
                            if (slsTblSaleItemSchedule.getNumQuantity() != null
                                    && slsTblSaleItemSchedule.getNumQuantity().doubleValue() > 0) {
                                slsTblSaleItemSchedule.setSlsTblSoDetail(soDetail);
                                entityManager.persist(slsTblSaleItemSchedule);
                            }
                        }

                    } else {
                        slsTblSaleItemSchedule = new SlsTblSaleItemSchedule();
                        slsTblSaleItemSchedule.setNumQuantity(soDetail.getNumQuantity());
                        slsTblSaleItemSchedule.setDteDate(commonService.getCurrentTimeStamp_new());
                        if (slsTblSaleItemSchedule.getNumQuantity() != null
                                && slsTblSaleItemSchedule.getNumQuantity().doubleValue() > 0) {
                            slsTblSaleItemSchedule.setSlsTblSoDetail(soDetail);
                            entityManager.persist(slsTblSaleItemSchedule);
                        }
                    }
                }
            }

//			try {
//				if(SlsTblSaleOrder.getTxtSapNo()!=null && SlsTblSaleOrder.getTxtSapNo().trim().length() >0)
//				{
//
//				}
//				else
//				{
//				String Sap_no =  addSaleOrderTOSAP(SlsTblSaleOrder);
//
//				if(Sap_no.equalsIgnoreCase("10M"))
//				{
////					SlsTblSaleOrder.setTxtDescription("Ship-to-party is unregistered in FBR, Monthly Sales limit of customer exceeded more than 10M.");
//					SlsTblSaleOrder.setTxtDescription("10M Error");
//					entityManager.merge(SlsTblSaleOrder);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMail(SlsTblSaleOrder,true);
//					return "10M";
//				}
//				else if(Sap_no.equalsIgnoreCase("90D"))
//				{
////					SlsTblSaleOrder.setTxtDescription("Ship-to-party invoice is still open from more than 90 days.");
//					SlsTblSaleOrder.setTxtDescription("90 Days Error");
//					entityManager.merge(SlsTblSaleOrder);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMail(SlsTblSaleOrder,false);
//					return "90D";
//				}
//				else if(Sap_no.equalsIgnoreCase("NTN"))
//				{
////					SlsTblSaleOrder.setTxtDescription("Ship-to-party is unregistered in FBR, Monthly Sales limit of customer exceeded more than 10M.");
//					SlsTblSaleOrder.setTxtDescription("NTN Error");
//					entityManager.merge(SlsTblSaleOrder);
//					entityManager.getTransaction().commit();
//					entityManager.close();
//					if(ServerConfiguration.send_mail)
//						sendSaleOrdErrorinMailForCNIC(SlsTblSaleOrder,true);
//
//
//					return "NTN";
//				}
//				else	SlsTblSaleOrder.setTxtSapNo(Sap_no);
//
//
//				if(ServerConfiguration.send_mail)
//					sendSaleOrderinMail( SlsTblSaleOrder, details);
//				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//

            entityManager.getTransaction().commit();
            entityManager.getTransaction().begin();
//			if (checkISPriceChange(SlsTblSaleOrder)) {
//				SlsTblSaleOrder.setTxtStatus3("Pending");
//
//			} else
//				SlsTblSaleOrder.setTxtStatus3("NA");
            SlsTblSaleOrder.setTxtStatus3("Pending");
            entityManager.merge(SlsTblSaleOrder);
            entityManager.getTransaction().commit();

            entityManager.close();
            if (SlsTblSaleOrder.getNumPaymentReceived() != null
                    && SlsTblSaleOrder.getNumPaymentReceived().doubleValue() > 0)
                addSOAdvancePayment(SlsTblSaleOrder);

            if (SlsTblSaleOrder.getSlsTblDeal() != null && SlsTblSaleOrder.getSlsTblDeal().getSerDealId() > 0) {
                UpdateDealDetails(SlsTblSaleOrder);
                UpdateDealMaster(SlsTblSaleOrder);
            }

            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }

    public Timestamp getTimestampWithZeroTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Set the time components to zero
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Timestamp(calendar.getTimeInMillis());
    }

    public static double calculatePercentage(double value, double percentage) {
        return value * percentage;
    }

   /* public static void main(String[] args) {
        double value = 4920.00;
        double percentage = 0.98;

        double result = calculatePercentage(value, percentage);
        System.out.println("The result is: " + result);
    }*/

    public void UpdateDealDetails(SlsTblSaleOrder slsTblSaleOrder) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        SlsTblSoDetail soDetail;
        Iterator<SlsTblSoDetail> itr = slsTblSaleOrder.getSlsTblSoDetails().iterator();
        SlsTblSoDetail details = new SlsTblSoDetail();
        CfgTblProduct product = new CfgTblProduct();
        double order_Qty = 0;
        while (itr.hasNext()) {

            soDetail = (SlsTblSoDetail) itr.next();
            if (soDetail.getSlsTblDealDetails() != null && soDetail.getSlsTblDealDetails().getSerDealDetailId() > 0) {
                SlsTblDealDetails detail = slsTblDealDAO
                        .getDealDetailByPK(soDetail.getSlsTblDealDetails().getSerDealDetailId());
                if (detail != null && detail.getNumBalance().doubleValue() > 0
                        && soDetail.getNumQuantity().doubleValue() > 0) {
                    if ((detail.getNumBalance().doubleValue() - (soDetail.getNumQuantity().doubleValue() * soDetail.getNumItemPrice().doubleValue())) > 0)
                       /* detail.setNumBalance(new BigDecimal(
                                detail.getNumBalance().doubleValue() - soDetail.getNumQuantity().doubleValue()));*/
                        detail.setNumBalance(soDetail.getNumBalance());
                    else
                        detail.setNumBalance(BigDecimal.ZERO);

                    entityManager.merge(detail);
                }
            }
        }
        entityManager.getTransaction().commit();

        entityManager.close();
    }

    public void UpdateDealMaster(SlsTblSaleOrder slsTblSaleOrder) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        if (slsTblSaleOrder.getSlsTblDeal() != null && slsTblSaleOrder.getSlsTblDeal().getSerDealId() > 0) {
            List<SlsTblDealDetails> lstDetails = slsTblDealDAO
                    .searchDealDetail(slsTblSaleOrder.getSlsTblDeal().getSerDealId());

            if (lstDetails != null && lstDetails.size() > 0) {
                boolean isFinal = true;
                Iterator<SlsTblDealDetails> itrDeal = lstDetails.iterator();
                SlsTblDealDetails dealDetails = new SlsTblDealDetails();

                while (itrDeal.hasNext()) {

                    dealDetails = (SlsTblDealDetails) itrDeal.next();
                    if (dealDetails.getNumBalance().doubleValue() > 0) {
                        isFinal = false;
                        break;
                    }

                }
                if (isFinal) {
                    SlsTblDeal deal = slsTblDealDAO.getSOByPK(slsTblSaleOrder.getSlsTblDeal().getSerDealId());
                    deal.setBlnIsCompleted(true);
                    entityManager.merge(deal);
                    entityManager.getTransaction().commit();
                }
            }
        }
        entityManager.close();
    }

    public void sendSaleOrderinMail(SlsTblSaleOrder slsTblSaleOrder, SlsTblSoDetail soDetail) {

    }

    public void addSOAdvancePayment(SlsTblSaleOrder slsTblSaleOrder) {
        log.info("addSOAdvancePayment -----------------------inside sale order Advance Payment-----");
        SlsTblSoPayments soPayments = new SlsTblSoPayments();
        soPayments.setDteDate(slsTblSaleOrder.getDteDate());
        soPayments.setNumPaymentReceived(slsTblSaleOrder.getNumPaymentReceived());
        soPayments.setTxtSlipNo(slsTblSaleOrder.getTxtSlipNo());
        soPayments.setTxtPaymentMethod(slsTblSaleOrder.getTxtPaymentMethod());
        soPayments.setTxtChequeNo(slsTblSaleOrder.getTxtChequeNo());
        soPayments.setSlsTblSaleOrder(slsTblSaleOrder);
        soPayments.setBlIsDealerAdjustment(slsTblSaleOrder.getBlIsDealerAdjustment());
        soPayments.setTxtPaymentTerm(slsTblSaleOrder.getTxtPaymentTerm());
        soPayments.setBlIsAdvance(true);
        try {
            log.info("addSOAdvancePayment -----------------------inside sale order Advance Payment---\nSo-----"
                    + slsTblSaleOrder.getTxtSaleOrderNo() + "\n----PaymentReceived:"
                    + slsTblSaleOrder.getNumPaymentReceived() + "\n--date:" + soPayments.getDteDate() + "----txtSlipNo:"
                    + slsTblSaleOrder.getTxtSlipNo());
        } catch (Exception e) {
            // TODO: handle exception
        }
        addNewSaleOrderPayment(soPayments);
    }

    private String addSaleOrderTOSAP(SlsTblSaleOrder slsTblSaleOrder) throws SOAPException {

        Header header = new Header();
        ZhamSdFromDmsCustmrCreate customer = new ZhamSdFromDmsCustmrCreate();
        try {
            // create an instance of `JAXBContext`
            // JAXBContext context = JAXBContext.newInstance(Header.class);

            try {
                MessageFactory messageFactory = MessageFactory.newInstance();
                SOAPMessage soapMessage = messageFactory.createMessage();
                SOAPPart soapPart = soapMessage.getSOAPPart();
                SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
                SOAPBody soapBody = soapEnvelope.getBody();
                JAXBContext jaxbContext = JAXBContext.newInstance(ZhamSdFromDmsCustmrCreate.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                // jaxbMarshaller.setProperty(CharacterEscapeHandler.class.getName(), new
                // CustomCharacterEscapeHandler());
//                jaxbMarshaller.setProperty("com.sun.xml.internal.bind.characterEscapeHandler",
//                        new CharacterEscapeHandler() {
//                            public void escape(char[] ch, int start, int length, boolean isAttVal, Writer writer)
//                                    throws IOException {
//                                writer.write(ch, start, length);
//                            }
//                        });
                // output pretty printed
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                jaxbMarshaller.marshal(customer, System.err);
                jaxbMarshaller.marshal(customer, soapBody);
                soapMessage.saveChanges();
                soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
                soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
                soapMessage.writeTo(System.out);
                System.out.println(soapMessage.toString());
            } catch (JAXBException e) {
                e.printStackTrace();
            }

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

            header.setDOC_TYPE(slsTblSaleOrder.getCfgTblDocumentType().getTxtCode());
            if (slsTblSaleOrder.getTxtPONo() != null && slsTblSaleOrder.getTxtPONo().trim().length() > 0)
                header.setPURCH_NO(slsTblSaleOrder.getTxtPONo());
            else
                header.setPURCH_NO("N/A");

            if (slsTblSaleOrder.getDtePODate() != null) {
                String strDate = DATE_FORMATSAP.format(slsTblSaleOrder.getDtePODate());
                header.setPURCH_DATE(strDate);
            }

            // header.setPURCH_DATE(DATE_FORMATSAP.format(DATE_FORMAT.parse(DATE_FORMATDB.format("yyyy-dd-mm",
            // slsTblSaleOrder.getDtePODate())))+"");

            if (slsTblSaleOrder.getCfgTblDistributionChannel() != null)
                header.setDISTR_CHAN(slsTblSaleOrder.getCfgTblDistributionChannel().getTxtCode());

            if (slsTblSaleOrder.getCfgTblSalesOrganization() != null)
                header.setSALES_ORG(slsTblSaleOrder.getCfgTblSalesOrganization().getTxtCode());

            if (slsTblSaleOrder.getCfgTblPaymentTerm() != null)
                header.setPMNDISTR_CHANTTRMS(slsTblSaleOrder.getCfgTblPaymentTerm().getTxtCode());

            if (slsTblSaleOrder.getCfgTblDivision() != null)
                header.setDIVISION(slsTblSaleOrder.getCfgTblDivision().getTxtCode());

            if (slsTblSaleOrder.getCfgTblIncoTerm() != null) {

                CfgTblIncoTerm cfgTblIncoTerms = cfgTblIncoTermsDAO
                        .getIncoTermByPK(slsTblSaleOrder.getCfgTblIncoTerm().getSerIncoTermsId());
                // header.setIncoterm1(slsTblSaleOrder.getCfgTblIncoTerm().getTxtCode());
                // header.setIncoterm2(slsTblSaleOrder.getCfgTblIncoTerm().getTxtName());

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

            SlsTblSoDetail detailDTO = new SlsTblSoDetail();
            SlsTblSoDetail soDetail = new SlsTblSoDetail();
            Iterator<SlsTblSoDetail> itr = slsTblSaleOrder.getSlsTblSoDetails().iterator();
            boolean check = true;
            while (itr.hasNext()) {

                soDetail = (SlsTblSoDetail) itr.next();
                if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                    detailDTO = soDetail;
                    if (check) {
                        Item item = new Item();
                        item.setItem_no("1");
                        item.setMaterial(soDetail.getCfgTblProduct().getTxtProductCode());

                        item.setReq_qty(soDetail.getNumQuantity().toString());

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
            if (slsTblSaleOrder.getCfgTblDealer() != null && slsTblSaleOrder.getCfgTblDealer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());
                partner.setPartn_numb(dto.getTxtCustomerCode());

            } else if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());
                partner.setPartn_numb(dto.getTxtCustomerCode());

            }

            // partner.setPartn_numb(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerCode());
            // partner.setPartn_numb("0000100028");

            lstPartner.add(partner);

            Partner partner2 = new Partner();
            partner2.setPartn_role("WE");
            // partner2.setPartn_numb("0000101799");
            if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());
                partner2.setPartn_numb(dto.getTxtCustomerCode());

            } else if (slsTblSaleOrder.getCfgTblDealer() != null
                    && slsTblSaleOrder.getCfgTblDealer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());
                partner2.setPartn_numb(dto.getTxtCustomerCode());

            }

            lstPartner.add(partner2);

            Partner partner3 = new Partner();
            partner3.setPartn_role("ZM");
            // partner3.setPartn_numb("00000502");
            if (slsTblSaleOrder.getCfgTblDealer() != null
                    && slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee() != null
                    && slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId() > 0) {

                HrTblEmployee dto = hrEmployeeDAO
                        .getEmployeeByPK(slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
                partner3.setPartn_numb(dto.getTxtEmployeeCode());
                lstPartner.add(partner3);
            } else if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId() > 0) {
                HrTblEmployee dto = hrEmployeeDAO
                        .getEmployeeByPK(slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
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
            String schedule = schedule_par.trim().length() > 3 ? "&sche_str=" + schedule_par : "";

            System.out.println("URL-----:" + "sap-client=800&head_str=" + header_par + "&item_str =" + item_par
                    + "&part_str =" + partner_par + schedule);
            String urlParameters = "sap-client=800&head_str=" + header_par + "&item_str=" + item_par + "&part_str="
                    + partner_par + schedule;
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            String request = ServerConfiguration.ip_servre + "/ZSO_CREATE_SRV";
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
                for (int c; (c = in.read()) >= 0; ) {
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
            } else if (conn.getResponseCode() == 200) {
                System.out.println("200----------");
            } else if (conn.getResponseCode() == 400) {
                System.out.println("500----------");
            }

            if (conn.getResponseCode() != 200 && conn.getResponseCode() != 400) {
                // sendSaleOrderErrorinMail(slsTblSaleOrder,soDetail);
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

    public void sendSaleOrderErrorinMail(SlsTblSaleOrder slsTblSaleOrder, SlsTblSoDetail soDetail) {

    }

    @Override
    public String deleteSaleOrder(List<String> SaleOrdersId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            for (String serSaleOrderId : SaleOrdersId) {
                SlsTblSaleOrder SaleOrder = entityManager.find(SlsTblSaleOrder.class, Integer.parseInt(serSaleOrderId));
                if (SaleOrder != null) {
                    SaleOrder.setBlIsDeleted(true);

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
    public String deleteSaleOrder(List<String> SaleOrdersId, String Remarks) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            for (String serSaleOrderId : SaleOrdersId) {
                SlsTblSaleOrder SaleOrder = entityManager.find(SlsTblSaleOrder.class, Integer.parseInt(serSaleOrderId));
                if (SaleOrder != null) {
                    SaleOrder.setBlIsDeleted(true);
                    SaleOrder.setTxtReasonforCancel(Remarks);
                    SaleOrder.setTxtStatus("Cancel");
                    SaleOrder.setTxtStatus1("Cancel");
                    SaleOrder.setTxtStatus2("Cancel");
                    SaleOrder.setTxtStatus3("Cancel");

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
    public String updateSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
        String status = slsTblSaleOrder.getTxtStatus();
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();  // Start transaction

            String remarks = slsTblSaleOrder.getTxtReasonforCancel() != null ? slsTblSaleOrder.getTxtReasonforCancel().trim() : "";

            // Fetch the current sale order from DB
            if (slsTblSaleOrder.getSerSaleOrderId() != null) {
                SlsTblSaleOrder savedOrder = getSaleOrderByPK(slsTblSaleOrder.getSerSaleOrderId());

                // Append the new reason for cancel
                if (savedOrder.getTxtReasonforCancel() != null && !savedOrder.getTxtReasonforCancel().isEmpty()) {
                    slsTblSaleOrder.setTxtReasonforCancel(/*savedOrder.getTxtReasonforCancel() + " - " +*/ remarks);
                } else {
                    slsTblSaleOrder.setTxtReasonforCancel(remarks);
                }
            }

            // Set modification details
            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());

            // Set product and quantity from the sale order details
            if (slsTblSaleOrder.getSlsTblSoDetails() != null && !slsTblSaleOrder.getSlsTblSoDetails().isEmpty()) {
                slsTblSaleOrder.setCfgTblProduct(slsTblSaleOrder.getSlsTblSoDetails().get(0).getCfgTblProduct());
                slsTblSaleOrder.setNumQuantity(slsTblSaleOrder.getSlsTblSoDetails().get(0).getNumQuantity());
            }

            // Handle customer entity reference
            if (!(slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0)) {
                slsTblSaleOrder.setCfgTblCustomer(null);  // Set customer to null if invalid
            }

            // Handle sale order status
            if (status != null && status.equalsIgnoreCase("Hold")) {
                switch (slsTblSaleOrder.getNumLevel()) {
                    case 1:
                        slsTblSaleOrder.setTxtStatus1("Hold");
                        slsTblSaleOrder.setSerApprovedbyName1(cfgTblUser.getTxtUserName());
                        break;
                    case 2:
                        slsTblSaleOrder.setTxtStatus2("Hold");
                        slsTblSaleOrder.setSerApprovedbyName2(cfgTblUser.getTxtUserName());
                        break;
                    case 3:
                        slsTblSaleOrder.setTxtStatus3("Hold");
                        slsTblSaleOrder.setSerApprovedbyName3(cfgTblUser.getTxtUserName());
                        break;
                    case 4:
                        slsTblSaleOrder.setTxtStatus4("Hold");
                        slsTblSaleOrder.setSerApprovedbyName4(cfgTblUser.getTxtUserName());
                        break;
                    case 5:
                        slsTblSaleOrder.setTxtStatus5("Hold");
                        slsTblSaleOrder.setSerApprovedbyName5(cfgTblUser.getTxtUserName());
                        break;
                    case 6:
                        slsTblSaleOrder.setTxtStatus6("Hold");
                        slsTblSaleOrder.setSerApprovedbyName6(cfgTblUser.getTxtUserName());
                        break;
                }

            } else if (slsTblSaleOrder.getTxtStatus() != null
                    && slsTblSaleOrder.getTxtStatus().equalsIgnoreCase("Cancel")) {
                if (slsTblSaleOrder.getNumLevel() > 2) {
                    // CncelSOInSAP(slsTblSaleOrder);  // Placeholder for SAP logic
                }
            }
            entityManager.merge(slsTblSaleOrder);  // Merge sale order
            if ("cancel".equals(slsTblSaleOrder.getTxtStatus1()) ||
                    "cancel".equals(slsTblSaleOrder.getTxtStatus2()) ||
                    "cancel".equals(slsTblSaleOrder.getTxtStatus3()) ||
                    "cancel".equals(slsTblSaleOrder.getTxtStatus4()) ||
                    "cancel".equals(slsTblSaleOrder.getTxtStatus5()) ||
                    "cancel".equals(slsTblSaleOrder.getTxtStatus6())) {
                this.updateCancelledOrderDetails(slsTblSaleOrder.getSlsTblDeal().getSerDealId(),slsTblSaleOrder.getSerSaleOrderId());
            }
            // Entry Hit Audit Trail Table
            AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
            auditSlsTblSaleOrder.setSerSaleOrderId(slsTblSaleOrder.getSerSaleOrderId());
            auditSlsTblSaleOrder.setTxtRemarks(remarks);
            switch (slsTblSaleOrder.getNumLevel()) {
                case 1:
                    auditSlsTblSaleOrder.setTxtDepartment("MARKETING");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(2));
                    break;
                case 2:
                    auditSlsTblSaleOrder.setTxtDepartment("PROCUREMENT");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(3));
                    break;
                case 3:
                    auditSlsTblSaleOrder.setTxtDepartment("TAX");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(4));
                    break;
                case 4:
                    auditSlsTblSaleOrder.setTxtDepartment("FINANCE");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(5));
                    break;
                case 5:
                    auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(6));
                    break;
                case 6:
                    auditSlsTblSaleOrder.setTxtDepartment("PAYMENT");
                    auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtoriginalStatus());
                    auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                    auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(6));


                    break;
                default:
                    auditSlsTblSaleOrder.setTxtDepartment("Unknown Department");
                    break;
            }

            /* auditSlsTblSaleOrder.setTxtNextLevel("2");*/
            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
            auditSlsTblSaleOrder.setBlIsVendor(slsTblSaleOrder.getBlIsVendor());
          //  CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
            auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
            updateAuditDetail(auditSlsTblSaleOrder);

            // Handle attached profile picture
            if (slsTblSaleOrder.getProfile_pic() != null) {
                addSaleOrderAttachmentTOSAP(slsTblSaleOrder);
            }

            // Process sale order details
            if (slsTblSaleOrder.getSlsTblSoDetails() != null && !slsTblSaleOrder.getSlsTblSoDetails().isEmpty()) {
                for (SlsTblSoDetail soDetail : slsTblSaleOrder.getSlsTblSoDetails()) {
                    // Handle product design nullification
                    if (soDetail.getCfgTblProductDesign() != null
                            && soDetail.getCfgTblProductDesign().getSerProductDesignId() == 0) {
                        soDetail.setCfgTblProductDesign(null);
                    }

                    // Merge or persist sale order details
                    if (soDetail.getSerSoDetailId() != null && soDetail.getSerSoDetailId() > 0) {
                        if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                            soDetail.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
                            soDetail.setDteModifieddate(commonService.getCurrentTimeStamp_new());
                            entityManager.merge(soDetail);
                        } else {
                            soDetail.setBlIsDeleted(true);
                            entityManager.merge(soDetail);
                        }
                    } else {
                        if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                            soDetail.setBlIsDeleted(false);
                            soDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
                            soDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());
                            soDetail.setSlsTblSaleOrder(slsTblSaleOrder);
                            entityManager.persist(soDetail);
                        }
                    }

                    // Process sale item schedule if available
                    if (soDetail.getSlsTblSaleItemSchedule() != null) {
                        // Delete previous schedules
                        for (SlsTblSaleItemSchedule schedule : searchSaleOrderDetailSchedule(soDetail.getSerSoDetailId(), false)) {
                            schedule.setBlIsDeleted(true);
                            entityManager.merge(schedule);
                        }

                        // Add new schedules
                        for (SlsTblSaleItemSchedule schedule : soDetail.getSlsTblSaleItemSchedule()) {
                            if (schedule.getNumQuantity() != null && schedule.getNumQuantity().doubleValue() > 0) {
                                schedule.setSlsTblSoDetail(soDetail);
                                entityManager.persist(schedule);
                            }
                        }
                    }
                }
            }

            entityManager.getTransaction().commit();  // Commit the transaction
            entityManager.close();  // Close EntityManager

            return "Success";
        } catch (Exception e) {
            entityManager.getTransaction().rollback();  // Rollback the transaction in case of error
            log.error("Error while updating Sale Order: ", e);  // Log the error
            return e.getMessage();  // Return error message
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();  // Ensure EntityManager is closed
            }
        }
    }


    public void updateCancelledOrderDetails(Integer salesOrderId,Integer saleId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        try {

            List<SlsTblDealDetails> dealsDetails = entityManager.createQuery(
                            "SELECT d FROM SlsTblDealDetails d WHERE d.slsTblDeal.serDealId = :ser_deal_id")
                    .setParameter("ser_deal_id", salesOrderId)
                    .getResultList();

            for (SlsTblDealDetails detail : dealsDetails) {

                BigDecimal cancelNumAmount = getTotalAmountForDetails(detail,saleId);
               // BigDecimal newBalance = detail.getNumItemPrice().multiply(detail.getNumQuantity());
                /*if (detail.getNumBalance() != null) {
                    newBalance = newBalance.add(detail.getNumBalance());
                }
                detail.setNumBalance(newBalance);

                entityManager.merge(detail);*/
                BigDecimal newBalance = (detail.getNumBalance() != null) ? detail.getNumBalance() : BigDecimal.ZERO;
                newBalance = newBalance.add(cancelNumAmount);
                detail.setNumBalance(newBalance);
                entityManager.merge(detail);
            }
            // commit the database
            entityManager.getTransaction().commit();

        } catch (Exception e) {

            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            e.printStackTrace();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }



    public BigDecimal getTotalAmountForDetails(SlsTblDealDetails detail,Integer saleOrderId) {
        EntityManager entityManager = getEntityManager();
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {

            List<SlsTblSoDetail> orderDetails = entityManager.createQuery(
                            "SELECT d FROM SlsTblSoDetail d " +
                                    "WHERE d.slsTblDealDetails.serDealDetailId = :ser_detail_id AND d.slsTblSaleOrder.serSaleOrderId  = :ser_sale_order_id")
                    .setParameter("ser_detail_id", detail.getSerDealDetailId())
                    .setParameter("ser_sale_order_id", saleOrderId)
                    .getResultList();

            if (orderDetails.size() > 1) {
                for (SlsTblSoDetail soDetail : orderDetails) {
                    totalAmount = totalAmount.add(soDetail.getNumAmount());
                }
            }

            else if (orderDetails.size() == 1) {
                totalAmount = orderDetails.get(0).getNumAmount();
            }

            else {
                totalAmount = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            e.printStackTrace();
            totalAmount = BigDecimal.ZERO;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        return totalAmount;
    }



    private String addSaleOrderAttachmentTOSAP(SlsTblSaleOrder slsTblSaleOrder) {

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
             * String type=slsTblSaleOrder.getTxtImageType();
             *
             * String[] words = type.split("/"); if(words!=null && words.length>0)
             * attachment.setFtype(words[1]); else attachment.setFtype("JPG");
             */

            String type = slsTblSaleOrder.getTxtImageName();

            String[] words = type.split("\\.", 0);
            if (words != null && words.length > 0)
                attachment.setFtype(words[1]);
            else
                attachment.setFtype("JPG");

            slsTblSaleOrder.setTxtImageType(attachment.getFtype());

            attachment.setFname(slsTblSaleOrder.getTxtImageName());

            // attachment.setFname("check");

            attachment.setSo(slsTblSaleOrder.getTxtSapNo());

            // attachment.setSo("1000056505");

            // Encoder encoder = Base64.getUrlEncoder();
            // String originalinput =
            // "https://stackabuse.com/tag/java/";Base64.getEncoder().encodeToString(file.getBytes())
            String encodedUrl = Base64.getEncoder().encodeToString(slsTblSaleOrder.getProfile_pic());
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
            String request = ServerConfiguration.ip_servre + "/zsd_so_gos";

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
                for (int c; (c = in.read()) >= 0; ) {
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
    public String generateSaleOrderNo(String type) {
        // int SaleOrderNo;
        String SaleOrderType = type;
        // String SaleOrderCode="";
        int ord_no = 0;
        int ord_no1 = 0;
        EntityManager entityManager = getEntityManager();

        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        {
            try {
                entityManager.getTransaction().begin();

                /*
                 * String zoneCode = (String) entityManager.
                 * createQuery("select MAX(txtSaleOrderNo) from SlsTblSaleOrder where serGroupId=1"
                 * ) .getSingleResult();
                 */
                String zoneCode = "";

//				if(user !=null 	&& user.getTxtrole().equalsIgnoreCase("SM"))
//				if (type != null && type.trim().length() == 3)
                if (user != null && user.getTxtUserName().length() == 3) {
                    String usr = user.getTxtUserName().trim();

                    try {
                        zoneCode = (String) entityManager
                                .createQuery("SELECT b.txtSaleOrderNo FROM SlsTblSaleOrder b where b.txtSaleOrderNo like '"
                                        + usr + "%'  ORDER BY b.serSaleOrderId DESC")
                                .setMaxResults(1).getSingleResult();
                    } catch (NoResultException ez) {
                        // TODO Auto-generated catch block
                        zoneCode = usr + "-000";
                        ez.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = usr + "-";
                    }
                    ord_no1 = Integer.valueOf(zoneCode.substring(4));

                    ord_no1 = ord_no1 + 1;
                    String code = usr + "-001";
                    if (ord_no1 < 10)
                        code = usr + "-00" + ord_no1;
                    else if (ord_no > 9 && ord_no1 < 100)
                        code = usr + "-0" + ord_no1;
                    else
                        code = usr + "-" + ord_no1;
                    return code;
                } else if (user != null && user.getTxtUserName().indexOf(".parts") > 0 && user.getTxtUserName().length() == 9) {
                    String usr = user.getTxtUserName().trim().substring(0, 3);
                    usr = usr + "P";
                    try {
                        zoneCode = (String) entityManager
                                .createQuery("SELECT b.txtSaleOrderNo FROM SlsTblSaleOrder b where b.txtSaleOrderNo like '"
                                        + usr + "%'  ORDER BY b.serSaleOrderId DESC")
                                .setMaxResults(1).getSingleResult();
                    } catch (NoResultException ez) {
                        // TODO Auto-generated catch block
                        zoneCode = usr + "-000";
                        ez.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = usr + "-";
                    }
                    ord_no1 = Integer.valueOf(zoneCode.substring(5));

                    ord_no1 = ord_no1 + 1;
                    String code = usr + "-001";
                    if (ord_no1 < 10)
                        code = usr + "-00" + ord_no1;
                    else if (ord_no > 9 && ord_no1 < 100)
                        code = usr + "-0" + ord_no1;
                    else
                        code = usr + "-" + ord_no1;
                    return code;
                } else {
                    zoneCode = (String) entityManager.createQuery(
                                    "SELECT b.txtSaleOrderNo FROM SlsTblSaleOrder b where b.txtSaleOrderNo like 'PSO%'  ORDER BY b.serSaleOrderId DESC")
                            .setMaxResults(1).getSingleResult();

//				String zoneCode = (String) entityManager
//						.createQuery("SELECT b.txtSaleOrderNo FROM SlsTblSaleOrder b where b.serGroupId= "
//								+ user.getSerGroupId() + " ORDER BY b.serSaleOrderId DESC")
//						.setMaxResults(1).getSingleResult();

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = "PSO-";
                    }
                    ord_no1 = Integer.valueOf(zoneCode.substring(4));

                    ord_no1 = ord_no1 + 1;
                    String code = "PSO-001";
                    if (ord_no1 < 10)
                        code = "PSO-00" + ord_no1;
                    else if (ord_no > 9 && ord_no1 < 100)
                        code = "PSO-0" + ord_no1;
                    else
                        code = "PSO-" + ord_no1;
                    return code;
                }
            } catch (NoResultException erz) {

//				if(user !=null 	&& user.getTxtrole().equalsIgnoreCase("SM"))
                if (type != null && type.trim().length() == 3) {
                    String usr = type;
//					if(user.getTxtUserName().length() > 3)
//					{
//						usr = "IRM";
//					}else
//						usr=user.getTxtUserName();
                    return usr + "-001";
                }

                return "PSO-001";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    public static boolean isNullOrEmpty(String myString) {
        return myString == null || "".equals(myString);
    }

    public String getSaleOrderById(String SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSaleOrder where txtSaleOrderNo='" + SaleOrderId + "'";

            List<SlsTblSaleOrder> SaleOrder = entityManager.createQuery(query).getResultList();

            entityManager.close();
            if (SaleOrder.size() > 0) {
                return String.valueOf(SaleOrder.size());
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
    public List<SlsTblSaleOrder> searchSaleOrder(SlsTblSaleOrder SaleOrder) {
        // Initialize the entity manager and result list
        EntityManager entityManager = getEntityManager();
        List<SlsTblSaleOrder> resultList = new ArrayList<>();

        try {
            // Start transaction manually
            entityManager.getTransaction().begin();

            StringBuilder query = new StringBuilder("from SlsTblSaleOrder SaleOrder where 1=1 ");

            // Sale Order No
            if (SaleOrder.getTxtSaleOrderNo() != null) {
                query.append(" and upper(SaleOrder.txtSaleOrderNo) like upper('")
                        .append(SaleOrder.getTxtSaleOrderNo()).append("%') ");
            }

            // Level
            if (SaleOrder.getNumLevel() != null && SaleOrder.getNumLevel() > 0) {
                query.append(" and SaleOrder.numLevel = ").append(SaleOrder.getNumLevel()).append(" ");
            }

            // Dealer
            if (SaleOrder.getTxtDealer() != null && SaleOrder.getTxtDealer().trim().length() > 2) {
                query.append(" and upper(SaleOrder.cfgTblDealer.txtCustomerName) like upper('")
                        .append(SaleOrder.getTxtDealer()).append("') ");
            }

            // Customer
            if (SaleOrder.getTxtCustomer() != null && SaleOrder.getTxtCustomer().trim().length() > 2) {
                query.append(" and upper(SaleOrder.cfgTblCustomer.txtCustomerName) like upper('")
                        .append(SaleOrder.getTxtCustomer()).append("') ");
            }

            // Product
            if (SaleOrder.getTxtProduct() != null && SaleOrder.getTxtProduct().trim().length() > 2) {
                query.append(" and upper(SaleOrder.cfgTblProduct.txtProductName) like upper('")
                        .append(SaleOrder.getTxtProduct()).append("') ");
            }

            // SAP No
            if (SaleOrder.getTxtSapNo() != null && SaleOrder.getTxtSapNo().trim().length() > 0) {
                query.append(" and upper(SaleOrder.txtSapNo) like upper('")
                        .append(SaleOrder.getTxtSapNo()).append("') ");
            }

            // User Group Filtering
            CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
            if (user != null) {
                if (user.getTxtUserName().equalsIgnoreCase("umair")) {
                    query.append(" and (SaleOrder.serGroupId = ").append(user.getSerGroupId())
                            .append(" or SaleOrder.cfgTblProduct.serProductId in (904,905,906,907,908,909)) ");
                } else if (user.getSerGroupId() != null && user.getSerGroupId() > 0) {
                    query.append(" and SaleOrder.serGroupId = ").append(user.getSerGroupId()).append(" ");
                }
            }

            // Status Filtering
            if (SaleOrder.getTxtStatus5() != null && SaleOrder.getNumLevel() >= 5) {
                query.append(" and SaleOrder.txtStatus5 = 'APPROVED' ");
            }

            // Complimentary Orders
            if (SaleOrder.getBlIsComplementry() != null && SaleOrder.getBlIsComplementry()) {
                query.append(" and SaleOrder.txtStatus = 'APPROVED' and SaleOrder.serCreatedUserId > 0 ");
            }

            // Issued Orders
            if (SaleOrder.getBlnIsIssued() != null && SaleOrder.getBlnIsIssued()) {
                query.append(" and SaleOrder.blnIsIssued = true ");
            }

            // Product ID Filtering
            if (SaleOrder.getCfgTblProduct() != null && SaleOrder.getCfgTblProduct().getSerProductId() != null
                    && SaleOrder.getCfgTblProduct().getSerProductId() > 0) {
                query.append(" and SaleOrder.cfgTblProduct.serProductId = ")
                        .append(SaleOrder.getCfgTblProduct().getSerProductId()).append(" ");
            }

            // Sale Order ID Filtering
            if (SaleOrder.getSerSaleOrderId() != null) {
                query.append(" and SaleOrder.serSaleOrderId = ")
                        .append(SaleOrder.getSerSaleOrderId()).append(" ");
            }

            // Date Range Filtering
            if (SaleOrder.getDte_date_from() != null && SaleOrder.getDte_date_from().trim().length() > 0) {
                try {
                    Date fromDate = DATE_FORMAT.parse(SaleOrder.getDte_date_from());
                    query.append(" and SaleOrder.dteCreateddate >= '")
                            .append(DATE_FORMATDB.format(fromDate)).append("' ");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if (SaleOrder.getDte_date_to() != null && SaleOrder.getDte_date_to().trim().length() > 0) {
                try {
                    Date toDate = DATE_FORMAT.parse(SaleOrder.getDte_date_to());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(toDate);
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);

                    query.append(" and SaleOrder.dteCreateddate <= '")
                            .append(DATE_FORMATDB.format(calendar.getTime())).append("' ");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            // Finalize query and log
            query.append(" order by SaleOrder.serSaleOrderId DESC");
            log.info("Query is ---" + query.toString());

            // Execute query and get the results
            resultList = entityManager.createQuery(query.toString()).getResultList();

            // Commit the transaction
            entityManager.getTransaction().commit();

        } catch (Exception e) {
            // In case of any error, log it and roll back the transaction
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            // Close the EntityManager to avoid connection leak
            entityManager.close();
        }

        return resultList;
    }

    /*public List<SlsTblSaleOrder> searchSaleOrder(SlsTblSaleOrder SaleOrder) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        StringBuilder query = new StringBuilder("from SlsTblSaleOrder SaleOrder where 1=1 ");

        // Sale Order No
        if (SaleOrder.getTxtSaleOrderNo() != null) {
            query.append(" and upper(SaleOrder.txtSaleOrderNo) like upper('")
                    .append(SaleOrder.getTxtSaleOrderNo()).append("%') ");
        }

        // Level
        if (SaleOrder.getNumLevel() != null && SaleOrder.getNumLevel() > 0) {
            query.append(" and SaleOrder.numLevel = ").append(SaleOrder.getNumLevel()).append(" ");
        }

        // Dealer
        if (SaleOrder.getTxtDealer() != null && SaleOrder.getTxtDealer().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblDealer.txtCustomerName) like upper('")
                    .append(SaleOrder.getTxtDealer()).append("') ");
        }

        // Customer
        if (SaleOrder.getTxtCustomer() != null && SaleOrder.getTxtCustomer().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblCustomer.txtCustomerName) like upper('")
                    .append(SaleOrder.getTxtCustomer()).append("') ");
        }

        // Product
        if (SaleOrder.getTxtProduct() != null && SaleOrder.getTxtProduct().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblProduct.txtProductName) like upper('")
                    .append(SaleOrder.getTxtProduct()).append("') ");
        }

        // SAP No
        if (SaleOrder.getTxtSapNo() != null && SaleOrder.getTxtSapNo().trim().length() > 0) {
            query.append(" and upper(SaleOrder.txtSapNo) like upper('")
                    .append(SaleOrder.getTxtSapNo()).append("') ");
        }

        // User Group Filtering
        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        if (user != null) {
            if (user.getTxtUserName().equalsIgnoreCase("umair")) {
                query.append(" and (SaleOrder.serGroupId = ").append(user.getSerGroupId())
                        .append(" or SaleOrder.cfgTblProduct.serProductId in (904,905,906,907,908,909)) ");
            } else if (user.getSerGroupId() != null && user.getSerGroupId() > 0) {
                query.append(" and SaleOrder.serGroupId = ").append(user.getSerGroupId()).append(" ");
            }
        }

        // Status Filtering
        if (SaleOrder.getTxtStatus5() != null && SaleOrder.getNumLevel() >= 5) {
            query.append(" and SaleOrder.txtStatus5 = 'APPROVED' ");
        }

        // Complimentary Orders
        if (SaleOrder.getBlIsComplementry() != null && SaleOrder.getBlIsComplementry()) {
            query.append(" and SaleOrder.txtStatus = 'APPROVED' and SaleOrder.serCreatedUserId > 0 ");
        }

        // Issued Orders
        if (SaleOrder.getBlnIsIssued() != null && SaleOrder.getBlnIsIssued()) {
            query.append(" and SaleOrder.blnIsIssued = true ");
        }

        // Product ID Filtering
        if (SaleOrder.getCfgTblProduct() != null && SaleOrder.getCfgTblProduct().getSerProductId() != null
                && SaleOrder.getCfgTblProduct().getSerProductId() > 0) {
            query.append(" and SaleOrder.cfgTblProduct.serProductId = ")
                    .append(SaleOrder.getCfgTblProduct().getSerProductId()).append(" ");
        }

        // Sale Order ID Filtering
        if (SaleOrder.getSerSaleOrderId() != null) {
            query.append(" and SaleOrder.serSaleOrderId = ")
                    .append(SaleOrder.getSerSaleOrderId()).append(" ");
        }

        // Date Range Filtering
        if (SaleOrder.getDte_date_from() != null && SaleOrder.getDte_date_from().trim().length() > 0) {
           *//* try {
                Date toDate = DATE_FORMAT.parse(SaleOrder.getDte_date_to());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(toDate);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);

                query.append(" and SaleOrder.dteCreateddate >= '")
                        .append(DATE_FORMATDB.format(calendar.getTime())).append("' ");
            } catch (ParseException e) {
                e.printStackTrace();
            }*//*
            try {
                Date fromDate = DATE_FORMAT.parse(SaleOrder.getDte_date_from());
                query.append(" and SaleOrder.dteCreateddate >= '")
                        .append(DATE_FORMATDB.format(fromDate)).append("' ");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (SaleOrder.getDte_date_to() != null && SaleOrder.getDte_date_to().trim().length() > 0) {
            try {
                Date toDate = DATE_FORMAT.parse(SaleOrder.getDte_date_to());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(toDate);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);

                query.append(" and SaleOrder.dteCreateddate <= '")
                        .append(DATE_FORMATDB.format(calendar.getTime())).append("' ");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Customer Filtering based on User
        *//*CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        if (cfgTblUser.getCfgTblCustomer() != null) {
            if (cfgTblUser.getCfgTblCustomer().getBlIsDealer() != null && cfgTblUser.getCfgTblCustomer().getBlIsDealer()) {
                query.append(" and SaleOrder.slsTblDeal.cfgTblCustomer.serCustomerId in (")
                        .append("select customer.serCustomerId from CfgTblCustomer customer ")
                        .append("join SaleOrder ON SaleOrder.slsTblDeal.serDealId = slsdealid.serDealId ")
                        .append("join slsTblDeal ON slsTblDeal.serDealId = slsdealid.serDealId ")
                        .append("where customer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId())
                        .append(" or customer.cfgTblGroupCustomer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId())
                        .append(") ");
            } else {
                query.append(" and SaleOrder.cfgTblCustomer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId()).append(" ");
            }
        }*//*

        query.append(" order by SaleOrder.serSaleOrderId DESC");

        log.info("Query is ---" + query.toString());

        List<SlsTblSaleOrder> resultList = entityManager.createQuery(query.toString()).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return resultList;
    }*/



    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        List<SlsTblSoDetail> lstSOD = null;

        if (SaleOrderId > 0) {
            String query = "SELECT ff FROM SlsTblSoDetail ff " +
                    "LEFT JOIN FETCH ff.slsTblSaleItemSchedule " +
                    "WHERE (ff.blIsDeleted IS NULL OR ff.blIsDeleted = FALSE) " +
                    "AND ff.slsTblSaleOrder.serSaleOrderId = :saleOrderId";

            // Log the query for debugging purposes
            log.info("Query is ---" + query);
            System.out.println("query ----: " + query);

            // Create the query and set the parameter
            lstSOD = entityManager.createQuery(query, SlsTblSoDetail.class)
                    .setParameter("saleOrderId", SaleOrderId)
                    .getResultList();
        } else {
            return null;
        }

        entityManager.getTransaction().commit();
        entityManager.close();

        return lstSOD;
    }
   /* public List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "";
        if (SaleOrderId > 0)
            query = "from SlsTblSoDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblSaleOrder.serSaleOrderId= "
                    + SaleOrderId;

        else
            return null;

        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<SlsTblSoDetail> lstSOD = entityManager.createQuery(subQuery).getResultList();
        entityManager.getTransaction().commit();
        entityManager.close();
        return lstSOD;
    }*/


    public List<SlsTblSoDetail> searchSaleOrderDetailwithitemid(int SaleOrderId, int itemId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "";
        if (SaleOrderId > 0 && itemId > 0)
            query = "from SlsTblSoDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblSaleOrder.serSaleOrderId= "
                    + SaleOrderId + "  and ff.cfgTblProduct.serProductId = " + itemId;

        else
            return null;

        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<SlsTblSoDetail> lstSOD = entityManager.createQuery(subQuery).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return lstSOD;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSaleItemSchedule> searchSaleOrderDetailSchedule(int sodetailid, boolean isforSO) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "";
        if (sodetailid > 0)
            if (isforSO)
                query = "from SlsTblSaleItemSchedule ff  where ( ff.blIsDeleted is null or ff.blIsDeleted = false ) and ff.slsTblSoDetail.slsTblSaleOrder.serSaleOrderId= "
                        + sodetailid;
            else
                query = "from SlsTblSaleItemSchedule ff  where ( ff.blIsDeleted is null or ff.blIsDeleted = false ) and ff.slsTblSoDetail.serSoDetailId= "
                        + sodetailid;
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
        SlsTblSaleOrder so = entityManager.find(SlsTblSaleOrder.class, Integer.parseInt(id));
        entityManager.getTransaction().commit();
        entityManager.close();
        if (so != null) {

            if (so.getProfile_pic() != null)
                return so.getProfile_pic();

        }

        return null;
    }

    public String updateSaleOrderfromSAP(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside------updateSaleOrderfromSAP-----" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
//		String query = "FROM SlsTblSaleOrder where txtSapNo='" + slsTblSaleOrder.getTxtSapNo() + "'";
        String query = "FROM SlsTblSaleOrder where txtSaleOrderNo='" + slsTblSaleOrder.getTxtSaleOrderNo() + "'";

        List<SlsTblSaleOrder> lstSaleOrder = entityManager.createQuery(query).getResultList();
        entityManager.getTransaction().commit();
        entityManager.close();
        if (lstSaleOrder != null && lstSaleOrder.size() > 0) {
            SlsTblSaleOrder dto = (SlsTblSaleOrder) lstSaleOrder.get(0);
            if (dto != null) {


                if (slsTblSaleOrder.getTxtChassisNo() != null && slsTblSaleOrder.getTxtChassisNo().trim().length() > 0)
                    dto.setTxtChassisNo(slsTblSaleOrder.getTxtChassisNo());

                if (slsTblSaleOrder.getTxtEngineNo() != null && slsTblSaleOrder.getTxtEngineNo().trim().length() > 0)
                    dto.setTxtEngineNo(slsTblSaleOrder.getTxtEngineNo());

                if (slsTblSaleOrder.getTxtRegistrationNo() != null && slsTblSaleOrder.getTxtRegistrationNo().trim().length() > 0)
                    dto.setTxtRegistrationNo(slsTblSaleOrder.getTxtRegistrationNo());

                if (slsTblSaleOrder.getTxtDeliveryPartNo() != null && slsTblSaleOrder.getTxtDeliveryPartNo().trim().length() > 0)
                    dto.setTxtDeliveryPartNo(slsTblSaleOrder.getTxtDeliveryPartNo());

                if (slsTblSaleOrder.getDteDeliveryDate() != null)
                    dto.setDteDeliveryDateActual(slsTblSaleOrder.getDteDeliveryDate());

                if (slsTblSaleOrder.getNumQtyActual() != null)
                    dto.setNumQtyActual(slsTblSaleOrder.getNumQtyActual());

                if (slsTblSaleOrder.getTxtDCNo() != null && slsTblSaleOrder.getTxtDCNo().trim().length() > 0)
                    dto.setTxtDCNo(slsTblSaleOrder.getTxtDCNo());

                if (slsTblSaleOrder.getTxtInvoiceNo() != null && slsTblSaleOrder.getTxtInvoiceNo().trim().length() > 0)
                    dto.setTxtInvoiceNo(slsTblSaleOrder.getTxtInvoiceNo());


                // slsTblSaleOrder.setSerSaleOrderId(dto.getSerSaleOrderId());

//				if(slsTblSaleOrder.getTxtMachineIp()!=null && slsTblSaleOrder.getTxtMachineIp().equalsIgnoreCase("netAmount"))
//				{
//					if(slsTblSaleOrder.getNumNetAmount()!=null && slsTblSaleOrder.getNumNetAmount().doubleValue() >=0)
//					{
//						dto.setNumNetAmount(slsTblSaleOrder.getNumNetAmount());
//
//					}
//
//					if (slsTblSaleOrder.getNumPrice() !=null && slsTblSaleOrder.getNumPrice().doubleValue() > 0) {
//
//						dto.setNumPrice(slsTblSaleOrder.getNumPrice());
//
//					}
//
//					return updateSaleOrderSAP(dto);
//				}
//				else
                {
//				if (slsTblSaleOrder.getBlnIsIncoTerm() !=null && slsTblSaleOrder.getBlnIsIncoTerm()) {
//					dto.setBlnIsIncoTerm(slsTblSaleOrder.getBlnIsIncoTerm());
//				}
//
//				if (slsTblSaleOrder.getTxtStatus() !=null && slsTblSaleOrder.getTxtStatus().trim().length() > 0) {
//					dto.setTxtStatus(slsTblSaleOrder.getTxtStatus().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtDCStatus() !=null && slsTblSaleOrder.getTxtDCStatus().trim().length() > 0) {
//					dto.setTxtDCStatus(slsTblSaleOrder.getTxtDCStatus().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtInvoiceStatus() !=null &&  slsTblSaleOrder.getTxtInvoiceStatus().trim().length() > 0) {
//					dto.setTxtInvoiceStatus(slsTblSaleOrder.getTxtInvoiceStatus().trim());
//				}
//
//				if (slsTblSaleOrder.getDteRSMApproval() !=null &&  slsTblSaleOrder.getDteRSMApproval().toString().length() > 0) {
//					dto.setDteRSMApproval(slsTblSaleOrder.getDteRSMApproval());
//				}
//
//				if (slsTblSaleOrder.getDteFinalApproval() !=null &&  slsTblSaleOrder.getDteFinalApproval().toString().length() > 0) {
//					dto.setDteFinalApproval(slsTblSaleOrder.getDteFinalApproval());
//				}
//
//				if (slsTblSaleOrder.getTxtOrderapprovalDate() !=null && slsTblSaleOrder.getTxtOrderapprovalDate().trim().length() > 0) {
//					dto.setTxtOrderapprovalDate(slsTblSaleOrder.getTxtOrderapprovalDate().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtDCDate() !=null && slsTblSaleOrder.getTxtDCDate().trim().length() > 0) {
//					if(dto.getTxtDCDate()!=null && dto.getTxtDCDate().trim().length()>0)
//						dto.setTxtDCDate(dto.getTxtDCDate()+","+ slsTblSaleOrder.getTxtDCDate().trim());
//						else
//							dto.setTxtDCDate(slsTblSaleOrder.getTxtDCDate().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtInvoiceDate() !=null && slsTblSaleOrder.getTxtInvoiceDate().trim().length() > 0) {
//
//					if(dto.getTxtInvoiceDate()!=null && dto.getTxtInvoiceDate().trim().length()>0)
//						dto.setTxtInvoiceDate(dto.getTxtInvoiceDate()+","+ slsTblSaleOrder.getTxtInvoiceDate().trim());
//						else
//							dto.setTxtInvoiceDate(slsTblSaleOrder.getTxtInvoiceDate().trim());
//				}
//
//
//				if (slsTblSaleOrder.getTxtDCNo() !=null && slsTblSaleOrder.getTxtDCNo().trim().length() > 0) {
//					if(dto.getTxtDCNo()!=null && dto.getTxtDCNo().trim().length()>0)
//					dto.setTxtDCNo(dto.getTxtDCNo()+","+ slsTblSaleOrder.getTxtDCNo().trim());
//					else
//						dto.setTxtDCNo(slsTblSaleOrder.getTxtDCNo().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtInvoiceNo()!=null && slsTblSaleOrder.getTxtInvoiceNo().trim().length() > 0) {
//					if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length()>0)
//					dto.setTxtInvoiceNo(dto.getTxtInvoiceNo()+","+ slsTblSaleOrder.getTxtInvoiceNo().trim());
//					else
//						dto.setTxtInvoiceNo(slsTblSaleOrder.getTxtInvoiceNo().trim());
//				}
//
//				if (slsTblSaleOrder.getTxtDCQty() !=null && slsTblSaleOrder.getTxtDCQty().trim().length() > 0) {
//					if(dto.getTxtDCQty()!=null && dto.getTxtDCQty().trim().length()>0)
//					dto.setTxtDCQty(dto.getTxtDCQty()+","+ slsTblSaleOrder.getTxtDCQty().trim());
//					else
//						dto.setTxtDCQty(slsTblSaleOrder.getTxtDCQty().trim());
//				}
//
//				if (slsTblSaleOrder.getNumPrice() !=null && slsTblSaleOrder.getNumPrice().doubleValue() > 0) {
//
//					dto.setNumPrice(slsTblSaleOrder.getNumPrice());
//
//				}
                    if (slsTblSaleOrder.getTxtDCStatus() != null && slsTblSaleOrder.getTxtDCStatus().trim().length() > 0
                            && slsTblSaleOrder.getTxtDCStatus().trim().equalsIgnoreCase("Approved")) {
                        dto.setTxtDCStatus("Approved");
                        dto.setDtePODate(commonService.getCurrentTimeStamp_new());
                        dto.setTxtDCDate(UtilDateAndTime.dateToStringddmmyyyy(commonService.getCurrentTimeStamp_new()));
                    } else if (slsTblSaleOrder.getTxtInvoiceStatus() != null
                            && slsTblSaleOrder.getTxtInvoiceStatus().trim().length() > 0
                            && slsTblSaleOrder.getTxtInvoiceStatus().trim().equalsIgnoreCase("Approved")) {
                        dto.setTxtInvoiceStatus("Approved");
                        dto.setDteRSMApproval(commonService.getCurrentTimeStamp_new());
                        dto.setTxtInvoiceDate(
                                UtilDateAndTime.dateToStringddmmyyyy(commonService.getCurrentTimeStamp_new()));
                    }


//					SlsTblSoVehicleDetail  vehicle=new SlsTblSoVehicleDetail();
//					vehicle.setCfgTblDealer(dto.getCfgTblDealerOne());
//					vehicle.setCfgTblCustomer(dto.getCfgTblDealer());
//					vehicle.setCfgTblProduct(dto.getCfgTblProduct());
//					vehicle.setDteDeliveryDate(dteDeliveryDate);

                    if (slsTblSaleOrder.getSlsTblSoDetails() != null && slsTblSaleOrder.getSlsTblSoDetails().size() > 0) {
                        dto.setSlsTblSoDetails(slsTblSaleOrder.getSlsTblSoDetails());
                        updateSaleOrderDetailSAP(dto);
                    }

                    return updateSaleOrderSAP(dto);
                }
            } else
                return "02-Sale Order Not Found";

        } else
            return "02-Sale Order Not Found";

        // return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String updateSaleOrderSAP(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

         //   AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
            List<SlsTblSoDetail> slsTblSoDetailList = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());

            boolean hasSuccessResponse = false;

            if (slsTblSoDetailList != null) {
                for (SlsTblSoDetail detail : slsTblSoDetailList) {
                    if (detail != null && "S".equals(detail.getTxtSoapReturnType())) {
                        hasSuccessResponse = true;
                    }
                }
            }

            if (hasSuccessResponse) {
                slsTblSaleOrder.setTxtSoapReturnType("S");
                slsTblSaleOrder.setTxtSoapResponseMsg("SES Created Successfully");
                slsTblSaleOrder.setTxtLevel("SECOND");
                slsTblSaleOrder.setNumLevel(2);
                slsTblSaleOrder.setTxtStatus1("APPROVED");
            //    auditSlsTblSaleOrder.setTxtStatus("APPROVED");
            } else {
                slsTblSaleOrder.setTxtSoapReturnType("E");
                slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
           //     auditSlsTblSaleOrder.setTxtStatus("Pending");
            }

            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());

          /*  auditSlsTblSaleOrder.setSerSaleOrderId(slsTblSaleOrder.getSerSaleOrderId());
            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtMarketingRemarks());
            auditSlsTblSaleOrder.setTxtDepartment("Marketing");
            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
            auditSlsTblSaleOrder.setTxtNextLevel("2");
            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentUserVoId());
            auditSlsTblSaleOrder.setApprovedNAME(commonService.getCurrentUserName());
            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());*/

            entityManager.merge(slsTblSaleOrder);
        //    entityManager.persist(auditSlsTblSaleOrder);
            entityManager.getTransaction().commit();
            entityManager.close();

            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return "Failure";
        }
        /*try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<SlsTblSoDetail> slsTblSoDetailList = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());
            List<SlsTblSoDetail> soDetailsListObj =
                    (slsTblSoDetailList != null ? slsTblSoDetailList.stream()
                            .filter(detail -> detail != null && "E".equals(detail.getTxtSoapReturnType()))
                            .collect(Collectors.toList()) : new ArrayList<>());

            if (slsTblSoDetailList != null) {
                for (SlsTblSoDetail detail : slsTblSoDetailList) {
                    if (detail == null ||
                            detail.getTxtSoapReturnType() == null ||
                            detail.getTxtSoapReturnType().isEmpty() ||
                            detail.getTxtSoapReturnType().equals("E")) {

                        // Update detail with 'E'
                        detail.setTxtSoapReturnType("E");
                        // Persist changes
                        entityManager.merge(detail);
                    }
                }
            }
            if(soDetailsListObj.size() > 0){
                slsTblSaleOrder.setTxtSoapReturnType("E");
                slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
            }else{
               *//* slsTblSaleOrder.setTxtReturnMsg("S");*//*
                slsTblSaleOrder.setTxtSoapReturnType("S");
                slsTblSaleOrder.setTxtSoapResponseMsg("SES Created Successfully");
                *//*slsTblSaleOrder.setTxtSoapReturnType(slsTblSaleOrder.getTxtSoapReturnType());
                slsTblSaleOrder.setTxtSoapResponseMsg(slsTblSaleOrder.getTxtSoapResponseMsg());*//*
            }
            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(slsTblSaleOrder);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }*/
    }

    @Override
    public String updateSaleOrderSAPPending(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            List<SlsTblSoDetail> slsTblSoDetailList = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());
            /*List<SlsTblSoDetail> soDetailsListObj =
                    (slsTblSoDetailList != null ? slsTblSoDetailList.stream()
                            .filter(detail -> detail != null && "E".equals(detail.getTxtSoapReturnType()))
                            .collect(Collectors.toList()) : new ArrayList<>());*/

            if (slsTblSoDetailList != null) {
                for (SlsTblSoDetail detail : slsTblSoDetailList) {
                    if (detail == null) {

                        // Update detail with 'E'
                        slsTblSaleOrder.setTxtSoapReturnType("E");
                        slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
                        // Persist changes
                        entityManager.merge(detail);
                    }
                }
            }
            /*if(soDetailsListObj.size() > 0){
                slsTblSaleOrder.setTxtSoapReturnType("E");
                slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
            }else{
                *//* slsTblSaleOrder.setTxtReturnMsg("S");*//*
                slsTblSaleOrder.setTxtSoapReturnType("S");
                slsTblSaleOrder.setTxtSoapResponseMsg("SES Created Successfully");
                *//*slsTblSaleOrder.setTxtSoapReturnType(slsTblSaleOrder.getTxtSoapReturnType());
                slsTblSaleOrder.setTxtSoapResponseMsg(slsTblSaleOrder.getTxtSoapResponseMsg());*//*
            }*/
            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(slsTblSaleOrder);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }


    @Override
    public String updateSaleOrderSAPRepost(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
            List<SlsTblSoDetail> slsTblSoDetailList = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());

            boolean hasSuccessResponse = false;

            if (slsTblSoDetailList != null) {
                for (SlsTblSoDetail detail : slsTblSoDetailList) {
                    if (detail != null && "S".equals(detail.getTxtSoapReturnType())) {
                        hasSuccessResponse = true;
                    }
                }
            }

            if (hasSuccessResponse) {
                slsTblSaleOrder.setTxtSoapReturnType("S");
                slsTblSaleOrder.setTxtSoapResponseMsg("SES Created Successfully");
                slsTblSaleOrder.setTxtLevel("SECOND");
                slsTblSaleOrder.setNumLevel(2);
                slsTblSaleOrder.setTxtStatus1("APPROVED");
                auditSlsTblSaleOrder.setTxtStatus("APPROVED");
            } else {
                slsTblSaleOrder.setTxtSoapReturnType("E");
                slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
                auditSlsTblSaleOrder.setTxtStatus("Pending");
            }

            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());

            auditSlsTblSaleOrder.setSerSaleOrderId(slsTblSaleOrder.getSerSaleOrderId());
            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtMarketingRemarks());
            auditSlsTblSaleOrder.setTxtDepartment("Marketing");
            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
            auditSlsTblSaleOrder.setTxtNextLevel("2");
            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentUserVoId());
            auditSlsTblSaleOrder.setApprovedNAME(commonService.getCurrentUserName());
            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());

            entityManager.merge(slsTblSaleOrder);
            entityManager.persist(auditSlsTblSaleOrder);
            entityManager.getTransaction().commit();
            entityManager.close();

            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return "Failure";
        }
    }

    /* public String updateSaleOrderSAPRepost(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
            entityManager.getTransaction().begin();
            List<SlsTblSoDetail> slsTblSoDetailList = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());
            List<SlsTblSoDetail> soDetailsListObj =
                    (slsTblSoDetailList != null ? slsTblSoDetailList.stream()
                            .filter(detail -> detail != null && "E".equals(detail.getTxtSoapReturnType()))
                            .collect(Collectors.toList()) : new ArrayList<>());

            if (slsTblSoDetailList != null) {
                for (SlsTblSoDetail detail : slsTblSoDetailList) {
                    if (detail == null ||
                            detail.getTxtSoapReturnType() == null ||
                            detail.getTxtSoapReturnType().isEmpty() ||
                            detail.getTxtSoapReturnType().equals("E")) {

                        // Update detail with 'E'
                        detail.setTxtSoapReturnType("E");
                        auditSlsTblSaleOrder.setTxtStatus("Pending");
                        // Persist changes
                        entityManager.merge(detail);
                    }
                }
            }
            if(soDetailsListObj.size() > 0){
                slsTblSaleOrder.setTxtSoapReturnType("E");
                slsTblSaleOrder.setTxtSoapResponseMsg("Response from SAP integration not available at this time. Please try again later or contact support.");
            }else{
                *//* slsTblSaleOrder.setTxtReturnMsg("S");*//*
                slsTblSaleOrder.setTxtSoapReturnType("S");
                slsTblSaleOrder.setTxtSoapResponseMsg("SES Created Successfully");
                slsTblSaleOrder.setTxtLevel("SECOND");
                slsTblSaleOrder.setNumLevel(2);
                slsTblSaleOrder.setTxtStatus1("APPROVED");
                slsTblSaleOrder.setTxtMarketingRemarks(slsTblSaleOrder.getTxtMarketingRemarks());
                auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                *//*slsTblSaleOrder.setTxtSoapReturnType(slsTblSaleOrder.getTxtSoapReturnType());
                slsTblSaleOrder.setTxtSoapResponseMsg(slsTblSaleOrder.getTxtSoapResponseMsg());*//*
            }
            slsTblSaleOrder.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSaleOrder.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            auditSlsTblSaleOrder.setSerSaleOrderId(slsTblSaleOrder.getSerSaleOrderId());
            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtMarketingRemarks());
            auditSlsTblSaleOrder.setTxtDepartment("Marketing");
            *//*  auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtStatus3());*//*
            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(2));
                      *//* auditSlsTblSaleOrder.setTxtLevel("3");
                       auditSlsTblSaleOrder.setTxtNextLevel("4");*//*
            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentUserVoId());
            auditSlsTblSaleOrder.setApprovedNAME(commonService.getCurrentUserName());
            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(slsTblSaleOrder);
            entityManager.persist(auditSlsTblSaleOrder);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }
*/
    public String updateSaleOrderDetailSAP(SlsTblSaleOrder slsTblSaleOrder) {
        try {
            log.info("inside-----updateSaleOrderSAP----:" + slsTblSaleOrder.getTxtSaleOrderNo());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            if (slsTblSaleOrder.getSlsTblSoDetails() != null && slsTblSaleOrder.getSlsTblSoDetails().size() > 0) {
                for (SlsTblSoDetail detail : slsTblSaleOrder.getSlsTblSoDetails()) {
                    SlsTblSoDetail detail2;
                    List<SlsTblSoDetail> lstSOD = searchSaleOrderDetailwithitemid(slsTblSaleOrder.getSerSaleOrderId(), detail.getCfgTblProduct().getSerProductId());
                    if (lstSOD != null && lstSOD.size() > 0) {
                        detail2 = lstSOD.get(0);
                        detail2.setNumIssueQty(detail.getNumIssueQty());
                        detail2.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
                        detail2.setDteModifieddate(commonService.getCurrentTimeStamp_new());
                        entityManager.merge(detail2);
                    }


                }
            }


            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }

    private String UpdateSaleOrderTOSAP(SlsTblSaleOrder slsTblSaleOrder) {

        try {

            if (!(slsTblSaleOrder.getTxtSapNo() != null && slsTblSaleOrder.getTxtSapNo().trim().length() > 0)) {
                return "NA";
            }

            if (slsTblSaleOrder.getSerSaleOrderId() != null && slsTblSaleOrder.getSerSaleOrderId() > 0) {
                slsTblSaleOrder = getSOByPK(slsTblSaleOrder.getSerSaleOrderId());

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

            header.setSALES_ORD(slsTblSaleOrder.getTxtSapNo());

            header.setDOC_TYPE(slsTblSaleOrder.getCfgTblDocumentType().getTxtCode());
            if (slsTblSaleOrder.getTxtPONo() != null && slsTblSaleOrder.getTxtPONo().trim().length() > 0)
                header.setPURCH_NO(slsTblSaleOrder.getTxtPONo());
            else
                header.setPURCH_NO("N/A");

            if (slsTblSaleOrder.getDtePODate() != null) {
                String strDate = DATE_FORMATSAP.format(slsTblSaleOrder.getDtePODate());
                header.setPURCH_DATE(strDate);
            }

            // header.setPURCH_DATE(DATE_FORMATSAP.format(DATE_FORMAT.parse(DATE_FORMATDB.format("yyyy-dd-mm",
            // slsTblSaleOrder.getDtePODate())))+"");

            if (slsTblSaleOrder.getCfgTblDistributionChannel() != null)
                header.setDISTR_CHAN(slsTblSaleOrder.getCfgTblDistributionChannel().getTxtCode());

            if (slsTblSaleOrder.getCfgTblSalesOrganization() != null)
                header.setSALES_ORG(slsTblSaleOrder.getCfgTblSalesOrganization().getTxtCode());

            if (slsTblSaleOrder.getCfgTblPaymentTerm() != null)
                header.setPMNDISTR_CHANTTRMS(slsTblSaleOrder.getCfgTblPaymentTerm().getTxtCode());

            if (slsTblSaleOrder.getCfgTblDivision() != null)
                header.setDIVISION(slsTblSaleOrder.getCfgTblDivision().getTxtCode());

            if (slsTblSaleOrder.getCfgTblIncoTerm() != null) {

                CfgTblIncoTerm cfgTblIncoTerms = cfgTblIncoTermsDAO
                        .getIncoTermByPK(slsTblSaleOrder.getCfgTblIncoTerm().getSerIncoTermsId());
                // header.setIncoterm1(slsTblSaleOrder.getCfgTblIncoTerm().getTxtCode());
                // header.setIncoterm2(slsTblSaleOrder.getCfgTblIncoTerm().getTxtName());

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

            SlsTblSoDetail detailDTO = new SlsTblSoDetail();
            SlsTblSoDetail soDetail = new SlsTblSoDetail();

            List lstDetails = searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());
            Iterator<SlsTblSoDetail> itr = lstDetails.iterator();
            boolean check = true;
            while (itr.hasNext()) {

                soDetail = (SlsTblSoDetail) itr.next();
                if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                    detailDTO = soDetail;
                    if (check) {
                        Item item = new Item();
                        item.setItem_no("1");
                        item.setMaterial(soDetail.getCfgTblProduct().getTxtProductCode());

                        item.setReq_qty(soDetail.getNumQuantity().toString());

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
            if (slsTblSaleOrder.getCfgTblDealer() != null && slsTblSaleOrder.getCfgTblDealer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());
                partner.setPartn_numb(dto.getTxtCustomerCode());

            } else if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());
                partner.setPartn_numb(dto.getTxtCustomerCode());

            }

            // partner.setPartn_numb(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerCode());
            // partner.setPartn_numb("0000100028");

            lstPartner.add(partner);

            Partner partner2 = new Partner();
            partner2.setPartn_role("WE");
            // partner2.setPartn_numb("0000101799");
            if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());
                partner2.setPartn_numb(dto.getTxtCustomerCode());

            } else if (slsTblSaleOrder.getCfgTblDealer() != null
                    && slsTblSaleOrder.getCfgTblDealer().getSerCustomerId() > 0) {
                CfgTblCustomer dto = cfgTblCustomerDAO
                        .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());
                partner2.setPartn_numb(dto.getTxtCustomerCode());

            }

            lstPartner.add(partner2);

            Partner partner3 = new Partner();
            partner3.setPartn_role("ZM");
            // partner3.setPartn_numb("00000502");
            if (slsTblSaleOrder.getCfgTblDealer() != null
                    && slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee() != null
                    && slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId() > 0) {

                HrTblEmployee dto = hrEmployeeDAO
                        .getEmployeeByPK(slsTblSaleOrder.getCfgTblDealer().getHrTblEmployee().getSerEmployeeId());
                partner3.setPartn_numb(dto.getTxtEmployeeCode());
                lstPartner.add(partner3);
            } else if (slsTblSaleOrder.getCfgTblCustomer() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee() != null
                    && slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId() > 0) {
                HrTblEmployee dto = hrEmployeeDAO
                        .getEmployeeByPK(slsTblSaleOrder.getCfgTblCustomer().getHrTblEmployee().getSerEmployeeId());
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
            List lstSch = searchSaleOrderDetailSchedule(detailDTO.getSerSoDetailId(), false);
            if (lstSch != null) {
                schedule_par = "";
                Iterator<SlsTblSaleItemSchedule> itr_sch = lstSch.iterator();
                Schedule schedule;
                while (itr_sch.hasNext()) {

                    slsTblSaleItemSchedule = (SlsTblSaleItemSchedule) itr_sch.next();
                    if (slsTblSaleItemSchedule.getNumQuantity() != null) {

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

            String schedule = schedule_par.trim().length() > 3 ? "&sche_str=" + schedule_par : "";

            System.out.println("URL-----:" + "sap-client=800&head_str=" + header_par + "&item_str =" + item_par
                    + "&part_str =" + partner_par + schedule);
            String urlParameters = "sap-client=800&head_str=" + header_par + "&item_str=" + item_par + "&part_str="
                    + partner_par + schedule;
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            String request = ServerConfiguration.ip_servre + "/ZSO_UPDATE_SERV";
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
                for (int c; (c = in.read()) >= 0; ) {
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
            } else if (conn.getResponseCode() == 200) {
                System.out.println("200----------");
            } else if (conn.getResponseCode() == 400) {
                System.out.println("500----------");
            }

            if (conn.getResponseCode() != 200 && conn.getResponseCode() != 400) {
                // sendSaleOrderErrorinMail(slsTblSaleOrder,soDetail);
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

    public SlsTblSaleOrder getSOByPK(int SOId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSaleOrder where serSaleOrderId=" + SOId;

            SlsTblSaleOrder so = (SlsTblSaleOrder) entityManager.createQuery(query).getSingleResult();

            entityManager.close();

            return so;

        } catch (NoResultException e) {
            log.error("serSaleOrderId not found");
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void DeleteSaleOrderCustomer(SlsTblSaleOrder order) {
        if (order.getSerSaleOrderId() != null && order.getSerSaleOrderId() > 0) {

            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();

            List<SlsTblSaleOrder> SaleOrders = new ArrayList();
            CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

            entityManager.createQuery(" update SlsTblSaleOrder s set s.cfgTblCustomer=null where s.serSaleOrderId = "
                    + order.getSerSaleOrderId()).getResultList();

            entityManager.getTransaction().commit();
            entityManager.close();

        }
    }

    public List<SlsTblSaleOrder> getSaleOrderBySapId(String SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSaleOrder where txtSapNo='" + SaleOrderId + "'";

            List<SlsTblSaleOrder> SaleOrders = entityManager.createQuery(query).getResultList();

            entityManager.close();
            if (SaleOrders.size() > 0) {
                return SaleOrders;
            }

            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public void sendSaleOrdErrorinMail(SlsTblSaleOrder slsTblSaleOrder, boolean is10M) {

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

        if (slsTblSaleOrder.getCfgTblDealer() != null)
            cfgTblCustomer = cfgTblCustomerDAO
                    .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());

        else if (slsTblSaleOrder.getCfgTblCustomer() != null)
            cfgTblCustomer = cfgTblCustomerDAO
                    .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());

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
            if (is10M) {
//				sb.append(" Dear Patron,CXYZ  is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");
                sb.append("");
                if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());

                sb.append(" is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");
            } else {
//				Dear Patron, CXYZ invoice is still open from more than 90 days, Kindly contact with your RSM. Warm Regards ICL

                sb.append("  ");
                if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());

                sb.append(
                        " invoice is still open from more than 90 days, Kindly contact with your RSM. Warm Regards ICL ");
            }

            StringBuffer sb_mail = new StringBuffer();
            if (is10M) {
//				sb.append(" Dear Patron,CXYZ  is unregistered in FBR, Monthly Sales limit exceeded more than 10M. Warm Regards ICL ");

                if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());

                sb_mail.append("  is unable to process the online order of ");

                if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
                sb_mail.append("  because customer is unregistered in FBR \r\n"
                        + "			and its monthly sales limit has been exceeded more than 10 M. please contact with dealer for further help. ");
            } else {
                if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());

                sb_mail.append(" is unable to process the online order of ");

                if (slsTblSaleOrder.getCfgTblCustomer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
                else if (slsTblSaleOrder.getCfgTblDealer() != null)
                    sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
                sb_mail.append(
                        "  because customer invoice is still open from 90 days. please contact with dealer for further help.. ");
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
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress
                    .parse(slsTblSaleOrder.getHrTblEmployee().getTxtEmail() + ",support@ittehadchemicals.com"));
//			mimeMessage.setRecipients(Message.RecipientType.TO,
//					InternetAddress.parse("support@ittehadchemicals.com"));
            // mimeMessage.setRecipients(Message.RecipientType.TO,
            // InternetAddress.parse(slsTblSaleOrder.getHrTblEmployee().getTxtEmail()));
            mimeMessage.setSubject("Online Sale Order");
            Transport.send(mimeMessage);

            System.out.println("Done");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendSaleOrdErrorinMailForCNIC(SlsTblSaleOrder slsTblSaleOrder, boolean isCNIC) {

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

        if (slsTblSaleOrder.getCfgTblDealer() != null)
            cfgTblCustomer = cfgTblCustomerDAO
                    .getCustomerByPK(slsTblSaleOrder.getCfgTblDealer().getSerCustomerId().toString());

        else if (slsTblSaleOrder.getCfgTblCustomer() != null)
            cfgTblCustomer = cfgTblCustomerDAO
                    .getCustomerByPK(slsTblSaleOrder.getCfgTblCustomer().getSerCustomerId().toString());

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
            sb.append("");
            if (slsTblSaleOrder.getCfgTblCustomer() != null)
                sb.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
            else if (slsTblSaleOrder.getCfgTblDealer() != null)
                sb.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());

            sb.append(
                    " NTN/CNIC Number is missing, Please Provide NTN/CNIC Number to ICL Sales Department. Warm Regards ICL");

            StringBuffer sb_mail = new StringBuffer();

            if (slsTblSaleOrder.getCfgTblDealer() != null)
                sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
            else if (slsTblSaleOrder.getCfgTblCustomer() != null)
                sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());

            sb_mail.append("  is unable to process the online order of ");

            if (slsTblSaleOrder.getCfgTblCustomer() != null)
                sb_mail.append(slsTblSaleOrder.getCfgTblCustomer().getTxtCustomerName());
            else if (slsTblSaleOrder.getCfgTblDealer() != null)
                sb_mail.append(slsTblSaleOrder.getCfgTblDealer().getTxtCustomerName());
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
//					InternetAddress.parse(slsTblSaleOrder.getHrTblEmployee().getTxtEmail()+",support@ittehadchemicals.com"));
            mimeMessage.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(slsTblSaleOrder.getHrTblEmployee().getTxtEmail()));

            mimeMessage.setSubject("Online Sale Order");
            Transport.send(mimeMessage);

            System.out.println("Done");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String updateSaleOrder(List<String> lstOrders, Date delivery) {
        List<SlsTblSaleOrder> lst = new ArrayList();

        for (String i : lstOrders) {
            SlsTblSaleOrder order = getSOByPK(Integer.parseInt(i));
            lst.add(order);
        }
        String msg = "Success";// ApproveSaleOrderTOSAP(lst);
        if (msg.equalsIgnoreCase("Success")) {
            for (SlsTblSaleOrder order : lst) {

                order = getSOByPK(order.getSerSaleOrderId());
                {
                    SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
                    try {
                        order.setTxtOrderapprovalDate(outPutFormat.format(new Date().getTime()));
//								return outPutFormat.format(new Date().getTime());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (delivery != null)
                    order.setDteDeliveryDate(delivery);
                order.setTxtStatus("APPROVED");
                updateSaleOrderSAP(order);
            }

        }

        return msg;
//			Success
    }

    private String ApproveSaleOrderTOSAP(List<SlsTblSaleOrder> lstSlsTblSaleOrder) {

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
            SlsTblSaleOrder order;
            List<Header> lstSchedule = new ArrayList();
            if (lstSlsTblSaleOrder != null) {
                Iterator<SlsTblSaleOrder> itr_sch = lstSlsTblSaleOrder.iterator();
                Header header;
                while (itr_sch.hasNext()) {

                    order = (SlsTblSaleOrder) itr_sch.next();

                    header = new Header();

                    header.setSALES_ORD(order.getTxtSapNo());

                    lstSchedule.add(header);

                }
                itab.setLstHeader(lstSchedule);

                marshaller.marshal(itab, swSchedule);
                schedule_par = swSchedule.toString();

            }

            String schedule = schedule_par.trim().length() > 3 ? "&so_str=" + schedule_par : "";

            System.out.println("URL-----:" + "sap-client=800" + schedule);
            String urlParameters = "sap-client=800" + schedule;
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            String request = ServerConfiguration.ip_servre + "/zsd_so_app";
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
                for (int c; (c = in.read()) >= 0; ) {
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
                return "Success";
            } else {
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
        List<HrTblEmployee> Employees = entityManager
                .createQuery("FROM HrTblEmployee e where e.cfgTblArea.serAreaId =" + areaid + " and blIsDeleted=FALSE")
                .getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return Employees;
    }

    public String updateSaleOrderPayment(SlsTblSoPayments slsTblSoPayments) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            slsTblSoPayments.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblSoPayments.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(slsTblSoPayments);

            entityManager.getTransaction().commit();
            return "Success";

        } catch (Exception e) {
            log.error("Error updating payment: " + e.getMessage(), e);

            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback(); // Ensure rollback on failure
            }

            return "Failure";
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close(); // Ensures cleanup
            }
        }
    }


    SHIntegeration integeration = new SHIntegeration();
    @Autowired
    private SlsTblSaleOrderRepository slsTblSaleOrderRepository;


    @Override
    public String addNewSaleOrderPayment(SlsTblSoPayments slsTblSoPayments) {
        log.info("---inside --------- addNewSaleOrderPayment");
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            // SlsTblSaleOrder.setBlnStatus(true);
            slsTblSoPayments.setBlIsDeleted(false);

            slsTblSoPayments.setTxtStatus("Pending");
            slsTblSoPayments.setNumLevel(1);
            slsTblSoPayments.setTxtLevel("First");
            slsTblSoPayments.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
            slsTblSoPayments.setDteCreateddate(commonService.getCurrentTimeStamp_new());

            if (slsTblSoPayments.getDteDate() == null)
                slsTblSoPayments.setDteDate(new Date());

            slsTblSoPayments.setSerGroupId(
                    this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());


            String msg = "";
            try {
                msg = integeration.CreatePaymentInSHERP(slsTblSoPayments);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            slsTblSoPayments.setTxtReturnMsg(msg);
            if (msg.indexOf("SUCCESS") > 0 || msg.indexOf("Success") > 0) {
                slsTblSoPayments.setBlIsPOSTEDToSAP(true);

            }


            entityManager.persist(slsTblSoPayments);

            if (slsTblSoPayments.getSlsTblSaleOrder() != null) {
                SlsTblSaleOrder slsTblSaleOrder = getSaleOrderByPK(
                        slsTblSoPayments.getSlsTblSaleOrder().getSerSaleOrderId());
//				if (slsTblSaleOrder != null) {
//					if (slsTblSaleOrder.getNumAmountReceived() != null) {
//						if (slsTblSoPayments.getNumPaymentReceived() != null)
//							slsTblSaleOrder.setNumAmountReceived(
//									new BigDecimal(slsTblSaleOrder.getNumAmountReceived().doubleValue()
//											+ slsTblSoPayments.getNumPaymentReceived().doubleValue()));
//					} else
//						slsTblSaleOrder.setNumAmountReceived(slsTblSoPayments.getNumPaymentReceived());
//
//					if (slsTblSaleOrder.getNumNetAmount() != null)
//						slsTblSaleOrder
//								.setNumRemainingBalance(new BigDecimal(slsTblSaleOrder.getNumNetAmount().doubleValue()
//										- slsTblSaleOrder.getNumAmountReceived().doubleValue()));
//
//					entityManager.merge(slsTblSaleOrder);
//				}

                if (!(slsTblSoPayments.getNumNetPayment() != null
                        && slsTblSoPayments.getNumNetPayment().doubleValue() > 0)) {
                    slsTblSoPayments.setNumNetPayment(slsTblSoPayments.getNumPaymentReceived());
                }

                if (slsTblSaleOrder != null) {
                    if (slsTblSaleOrder.getNumAmountReceived() != null) {
                        if (slsTblSoPayments.getNumNetPayment() != null)
                            slsTblSaleOrder.setNumAmountReceived(
                                    new BigDecimal(slsTblSaleOrder.getNumAmountReceived().doubleValue()
                                            + slsTblSoPayments.getNumNetPayment().doubleValue()));
                    } else
                        slsTblSaleOrder.setNumAmountReceived(slsTblSoPayments.getNumNetPayment());

                    if (slsTblSaleOrder.getNumNetAmount() != null)
                        slsTblSaleOrder.setNumRemainingBalance(
                                new BigDecimal(slsTblSaleOrder.getNumNetAmount().doubleValue()));

                    entityManager.merge(slsTblSaleOrder);
                }
                entityManager.getTransaction().commit();

                entityManager.close();
                try {
                    log.info("---saving  --------- SavePaymentInSAP" + slsTblSoPayments.getBlIsAdvance());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
//				if(slsTblSoPayments.getBlIsAdvance()!=null && !(slsTblSoPayments.getBlIsAdvance()))

//				if (slsTblSaleOrder.getNumLevel() != null && slsTblSaleOrder.getNumLevel() > 2)
//					SavePaymentInSAP(slsTblSoPayments, slsTblSaleOrder);
            } else if (slsTblSoPayments.getSlsTblDeal() != null) {
                SlsTblDeal slsTblDeal = slsTblDealDAO.getSOByPK(slsTblSoPayments.getSlsTblDeal().getSerDealId());
                if (slsTblDeal != null) {
                    if (slsTblDeal.getNumAmountReceived() != null) {
                        if (slsTblSoPayments.getNumPaymentReceived() != null)
                            slsTblDeal
                                    .setNumAmountReceived(new BigDecimal(slsTblDeal.getNumAmountReceived().doubleValue()
                                            + slsTblSoPayments.getNumPaymentReceived().doubleValue()));
                    } else
                        slsTblDeal.setNumAmountReceived(slsTblSoPayments.getNumPaymentReceived());

                    if (slsTblDeal.getNumNetAmount() != null)
                        slsTblDeal.setNumRemainingBalance(new BigDecimal(slsTblDeal.getNumNetAmount().doubleValue()
                                - slsTblDeal.getNumAmountReceived().doubleValue()));

                    entityManager.merge(slsTblDeal);
                }
                entityManager.getTransaction().commit();

                entityManager.close();

//				SaveDealPaymentInSAP(slsTblSoPayments, slsTblDeal);

            }

            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSoPayments> getAllSaleOrderPayments() {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        /*
         * List<SlsTblSaleOrder> SaleOrders =
         * entityManager.createQuery("FROM SlsTblSaleOrder where blIsDeleted=FALSE")
         * .getResultList();
         */

        List<SlsTblSoPayments> SaleOrders = new ArrayList();
        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

        if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
            SaleOrders = entityManager.createQuery(
                            " FROM SlsTblSoPayments where   serGroupId= " + user.getSerGroupId() + " and blIsDeleted=FALSE ")
                    .getResultList();
        } else
            SaleOrders = entityManager.createQuery(" FROM SlsTblSoPayments where blIsDeleted=FALSE  ").getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return SaleOrders;
    }

    @Override
    public List<SlsTblSoPayments> searchSaleOrderPayments(SlsTblSoPayments slsTblSoPayments) {

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "from SlsTblSoPayments SaleOrder where 1=1 and (SaleOrder.blIsDeleted = false or SaleOrder.blIsDeleted is null ) ";
        if (slsTblSoPayments.getSlsTblSaleOrder() != null) {
            if (slsTblSoPayments.getSlsTblSaleOrder().getTxtSaleOrderNo() != null) {
                query += " and upper(SaleOrder.slsTblSaleOrder.txtSaleOrderNo) like" + " upper('"
                        + slsTblSoPayments.getSlsTblSaleOrder().getTxtSaleOrderNo() + "')" + " ";
            }
        } else if (slsTblSoPayments.getSlsTblDeal() != null) {
            if (slsTblSoPayments.getSlsTblDeal().getTxtDealNo() != null) {
                query += " and upper(SaleOrder.slsTblDeal.txtDealNo) like" + " upper('"
                        + slsTblSoPayments.getSlsTblDeal().getTxtDealNo() + "')" + " ";
            }
        }

        query += " order by SaleOrder.serSoPaymentId  DESC";
        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<SlsTblSoPayments> cust = entityManager.createQuery(subQuery).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return cust;
    }


    @Override
    public List<SlsTblSoPayments> searchSaleOrderPayments(ReportDTO dto) {

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "from SlsTblSoPayments SaleOrder where 1=1 and (SaleOrder.blIsDeleted = false or SaleOrder.blIsDeleted is null ) ";
        if (dto.getTxt_so_no() != null) {
            {
                query += " and upper(SaleOrder.slsTblSaleOrder.txtSaleOrderNo) like" + " upper('"
                        + dto.getTxt_so_no() + "')" + " ";
            }
        }

//		if (slsTblSoPayments.getSlsTblDeal() != null) {
//			if (slsTblSoPayments.getSlsTblDeal().getTxtDealNo() != null) {
//				query += " and upper(SaleOrder.slsTblDeal.txtDealNo) like" + " upper('"
//						+ slsTblSoPayments.getSlsTblDeal().getTxtDealNo() + "')" + " ";
//			}
//		}

        if (dto.getNumLevel() > 0) {

            query += " and SaleOrder.numLevel  = " + dto.getNumLevel() + ""
                    + " ";
        }

        if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
            try {

                query += " and SaleOrder.dteDate >= '"
                        + DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
            try {
                query += " and SaleOrder.dteDate <= '"
                        + DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
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

                query += " and SaleOrder.slsTblSaleOrder.cfgTblDealerOne.serCustomerId in"
                        + " (select serCustomerId from CfgTblCustomer customer  where customer.serCustomerId="
                        + cfgTblUser.getCfgTblCustomer().getSerCustomerId()
                        + "  or customer.slsTblSaleOrder.cfgTblGroupCustomer.serCustomerId=   "
                        + cfgTblUser.getCfgTblCustomer().getSerCustomerId()
                        + " or customer.slsTblSaleOrder.cfgTblCustomer.serCustomerId=   "
                        + cfgTblUser.getCfgTblCustomer().getSerCustomerId() + ")";
            } else {
                query += " and SaleOrder.slsTblSaleOrder.cfgTblCustomer.serCustomerId =" + " "
                        + cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";

            }
        }

        query += " order by SaleOrder.serSoPaymentId  DESC";
        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<SlsTblSoPayments> cust = entityManager.createQuery(subQuery).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return cust;
    }

    @Override
    public SlsTblSaleOrder getSaleOrderByPK(int id) {
        EntityManager entityManager = getEntityManager();
        SlsTblSaleOrder order = null;

        try {

            TypedQuery<SlsTblSaleOrder> query = entityManager.createQuery(
                    "SELECT s FROM SlsTblSaleOrder s LEFT JOIN FETCH s.slsTblSoDetails WHERE s.serSaleOrderId = :id",
                    SlsTblSaleOrder.class);
            query.setParameter("id", id);

            order = query.getSingleResult();
        } catch (NoResultException e) {
            log.error("Sale order not found for ID: " + id);
        } catch (Exception e) {
            log.error("Error fetching sale order: " + e.getMessage(), e);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close(); // Ensures resource cleanup
            }
        }

        return order;
    }


    @Override
    public String uploadPaymentDocument(SOPaymentDocument paymentDocument) {
        try {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();
            SlsTblSaleOrder slsTblSaleOrder = entityManager.find(SlsTblSaleOrder.class,
                    paymentDocument.getSlsTblSaleOrder().getSerSaleOrderId());
            if (slsTblSaleOrder == null) {
                return "Failure";
            }
            paymentDocument.setSlsTblSaleOrder(slsTblSaleOrder);
            paymentDocument.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
            paymentDocument.setDteCreateddate(commonService.getCurrentTimeStamp_new());
            CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
            paymentDocument.setCreatedUserName(cfgTblUser.getTxtUserName());
            entityManager.persist(paymentDocument);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "Failure";
    }

    @Override
    public byte[] downloadDocument(int documentId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        SOPaymentDocument paymentDocument = entityManager.find(SOPaymentDocument.class, documentId);
        entityManager.getTransaction().commit();
        entityManager.close();
        return paymentDocument.getDocumentFile();
    }

    @Override
    public String removeCandidateDocument(int documentId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        SOPaymentDocument paymentDocument = entityManager.find(SOPaymentDocument.class, documentId);
        entityManager.remove(paymentDocument);
        entityManager.getTransaction().commit();
        entityManager.close();
        return "Success";
    }

    @Override
    public List<SOPaymentDocument> getSOPaymentDocumentList(int id) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
//		List<SOPaymentDocument> rcsCandidateDocumnets = entityManager
//				.createQuery("FROM SOPaymentDocument ff where ff.slsTblSoPayments.serSoPaymentId=" + id)
//				.getResultList();

        List<SOPaymentDocument> rcsCandidateDocumnets = entityManager
                .createQuery("FROM SOPaymentDocument ff where ff.slsTblSaleOrder.serSaleOrderId=" + id)
                .getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        if (rcsCandidateDocumnets != null && rcsCandidateDocumnets.size() > 0) {
            for (SOPaymentDocument paymnet : rcsCandidateDocumnets) {
                paymnet.setDocumentFile(null);
            }
        }
        return rcsCandidateDocumnets;
    }


    private void setupApprovedOrder(SlsTblSaleOrder order, SODTO dto) {
        order.setTxtLevel("SECOND");
        order.setNumLevel(2);
        order.setTxtStatus1("APPROVED");
        order.setTxtMarketingRemarks(dto.getTxtMarketingRemarks());
    }

    private void setupPendingOrder(SlsTblSaleOrder order, SODTO dto) {
        order.setTxtLevel("First");
        order.setNumLevel(1);
        order.setTxtStatus1("Pending");
        order.setTxtMarketingRemarks(dto.getTxtMarketingRemarks());
    }

    private void setApprovalDetails(SlsTblSaleOrder order) {
        order.setSerApprovedbyId1(commonService.getCurrentLoggedInUser());
        order.setDteApproveddate1(commonService.getCurrentTimeStamp_new());
        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
        order.setSerApprovedbyName1(cfgTblUser.getTxtUserName());
    }


    private String processSaleOrderDetails(SlsTblSaleOrder order, String msg, boolean marketingHead) throws Exception {

        if (!marketingHead) {
            return "Failure";
        }

        if (order == null || order.getSlsTblDeal() == null || order.getSlsTblSoDetails() == null) {
            return "Failure";
        }

        SlsTblDeal parentDeal = order.getSlsTblDeal();
        List<SlsTblSoDetail> soDetailsList = order.getSlsTblSoDetails();

        if (soDetailsList.isEmpty()) {
            return "Failure";
        }

        for (SlsTblSoDetail soDetail : soDetailsList) {
            ZKFEINVOICESESWEBSERVICE zkfEInvoiceSesWebService = createInvoiceWebService(parentDeal, soDetail);
            SlsTblSaleOrder responseDeal = null;

            /*try {*/
                SOAPRequestResponseWrapper response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
                List<SlsTblSaleOrder> slsTblSaleOrderList = soapClientService.readSOAPResponseSES(response, order);

                if (!slsTblSaleOrderList.isEmpty()) {
                    responseDeal = slsTblSaleOrderList.get(0);
                    msg = handleSoapResponse(soDetail, responseDeal, order, msg);
                } else {
                    handleSoapError(soDetail, responseDeal);
                    msg = "Failure";
                }
            /*}*/ /*catch (SOAPException e) {
                System.err.println("SOAPException occurred: " + e.getMessage());
                handleSoapError(soDetail, responseDeal);
                msg = "Failure";
            } catch (Exception e) {
                System.err.println("Exception occurred: " + e.getMessage());
                handleSoapError(soDetail, responseDeal);
                msg = "Failure";
            }*/

            updateSoDetail(soDetail);
        }

        return msg;
    }

    /*private String processSaleOrderDetails(SlsTblSaleOrder order, String msg, boolean marketingHead) {
        // Check if marketingHead is false and skip processing
        if (!marketingHead) {
            return "Failure";
        }

        SlsTblDeal parentDeal = order.getSlsTblDeal();
        List<SlsTblSoDetail> soDetailsList = order.getSlsTblSoDetails();

        for (SlsTblSoDetail soDetail : soDetailsList) {
            ZKFEINVOICESESWEBSERVICE zkfEInvoiceSesWebService = createInvoiceWebService(parentDeal, soDetail);
            SlsTblSaleOrder responseDeal = new SlsTblSaleOrder();
            try {
                SOAPRequestResponseWrapper response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
                List<SlsTblSaleOrder> slsTblSaleOrderList = soapClientService.readSOAPResponseSES(response, order);
                if (!slsTblSaleOrderList.isEmpty()) {
                    responseDeal = slsTblSaleOrderList.get(0);
                    msg = handleSoapResponse(soDetail, responseDeal, order, msg);
                } else {
                    handleSoapError(soDetail, responseDeal);
                    msg = "Failure";
                }
            } catch (SOAPException e) {
                handleSoapError(soDetail, responseDeal);
                msg = "Failure";
            } catch (Exception e) {
                handleSoapError(soDetail, responseDeal);
                msg = "Failure";
            }
            updateSoDetail(soDetail);
        }
        return msg;
    }*/
    /*private String processSaleOrderDetails(SlsTblSaleOrder order, String msg, boolean marketingHead) {
        SlsTblDeal parentDeal = order.getSlsTblDeal();
        List<SlsTblSoDetail> soDetailsList = order.getSlsTblSoDetails();

        for (SlsTblSoDetail soDetail : soDetailsList) {
            if(marketingHead){
            ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = createInvoiceWebService(parentDeal, soDetail);
            SlsTblSaleOrder responseDeal = new SlsTblSaleOrder();
            try {
                    SOAPRequestResponseWrapper response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
                    List<SlsTblSaleOrder> slsTblSaleOrderList = soapClientService.readSOAPResponseSES(response, order);
                    if (!slsTblSaleOrderList.isEmpty()) {
                        responseDeal = slsTblSaleOrderList.get(0);
                        msg = handleSoapResponse(soDetail, responseDeal, order, msg);
                    } else {
                        handleSoapError(soDetail, responseDeal);
                        msg = "Failure";
                    }
                } catch (SOAPException e) {
                    handleSoapError(soDetail, responseDeal);
                    msg = "Failure";
                } catch (Exception e) {
                    handleSoapError(soDetail, responseDeal);
                    msg = "Failure";
                }
                updateSoDetail(soDetail);
            }
        }
        return msg;
    }*/

    private ZKFEINVOICESESWEBSERVICE createInvoiceWebService(SlsTblDeal parentDeal, SlsTblSoDetail soDetail) {
        ZKFEINVOICESESWEBSERVICE zkfEInvoiceSesWebService = new ZKFEINVOICESESWEBSERVICE();
        zkfEInvoiceSesWebService.setEBELN(parentDeal.getTxtSapNo());
        zkfEInvoiceSesWebService.setEBELP(soDetail.getSapLineItem());
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).toString();
        zkfEInvoiceSesWebService.setDOCDATE(formattedDate);
        zkfEInvoiceSesWebService.setPOSTDATE(formattedDate);
        if(soDetail.getNumAmount() == null || soDetail.getNumAmount().compareTo(BigDecimal.ZERO) == 0){
           // soDetail.setNumQuantity(BigDecimal.ZERO);
            zkfEInvoiceSesWebService.setQUANTITY(BigDecimal.ZERO);
        }else{
            zkfEInvoiceSesWebService.setQUANTITY(soDetail.getNumQuantity());
        }
        return zkfEInvoiceSesWebService;
    }

    private String handleSoapResponse(SlsTblSoDetail soDetail, SlsTblSaleOrder responseDeal, SlsTblSaleOrder order, String msg) {
        String soapReturnType = responseDeal.getTxtSoapReturnType();

        if (soapReturnType == null || soapReturnType.isEmpty() || "E".equals(soapReturnType)) {
            order.setTxtSoapReturnType("E");
            order.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
            msg = "Failure";
        } else {
            order.setTxtSoapReturnType(soapReturnType);
            order.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
           // updateSaleOrderSAP(order);
            msg = "Success";
        }

        soDetail.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType() != null ? responseDeal.getTxtSoapReturnType() : "S");
        soDetail.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());

        return msg;
    }
    /*private String handleSoapResponse(SlsTblSoDetail soDetail, SlsTblSaleOrder responseDeal, SlsTblSaleOrder order, String msg) {
        String soapReturnType = soDetail.getTxtSoapReturnType();
        SlsTblSaleOrder slsTblSaleOrder1 = order;

        if ("E".equals(soapReturnType) || soapReturnType == null || soapReturnType.isEmpty()) {
            slsTblSaleOrder1.setTxtSoapReturnType("E");
            slsTblSaleOrder1.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
            msg = "Failure";
        } else {
            slsTblSaleOrder1.setTxtSoapReturnType(soapReturnType);
            slsTblSaleOrder1.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
           *//* slsTblSaleOrderRepository.save(slsTblSaleOrder1);*//*
            updateSaleOrderSAP(slsTblSaleOrder1);
        }

        soDetail.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
        soDetail.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
        return msg; // Return the updated message
    }*/

    private void handleSoapError(SlsTblSoDetail soDetail, SlsTblSaleOrder responseDeal) {
        soDetail.setTxtSoapReturnType("E");
        soDetail.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg() != null ? responseDeal.getTxtSoapResponseMsg() : "Response from SAP integration not available at this time. Please try again later or contact support.");
    }



    @Transactional(rollbackOn = {SOAPException.class, RuntimeException.class})
    @Override
    public String updateSaleOrder(List<String> lstOrders, Date delivery, int Level,SODTO dto) throws Exception {
        try {
            log.info("---------updateSaleOrder-----" + lstOrders.size() + "delivery---" + delivery + "--level---:"
                    + Level);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        List<SlsTblSaleOrder> lst = new ArrayList();
        for (String i : lstOrders) {
            SlsTblSaleOrder order = getSOByPK(Integer.parseInt(i));
            lst.add(order);
        }
        String msg = "Success";// ApproveSaleOrderTOSAP(lst);
        if (msg.equalsIgnoreCase("Success")) {
            for (SlsTblSaleOrder order : lst) {

                order = getSOByPK(order.getSerSaleOrderId());
                {
                    SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
                    try {
                        order.setTxtOrderapprovalDate(outPutFormat.format(new Date().getTime()));
//								return outPutFormat.format(new Date().getTime());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (order.getTxtpOrderType() != null && order.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {
                    log.info("updating Spare parts Order at level 2--------");
                    order.setDteDeliveryDate(new Date());
                    order.setTxtStatus2("APPROVED");
                    order.setTxtStatus1("APPROVED");
                    order.setSerApprovedbyId1(commonService.getCurrentLoggedInUser());
                    order.setDteApproveddate1(commonService.getCurrentTimeStamp_new());
                    order.setSerApprovedbyId2(commonService.getCurrentLoggedInUser());
                    order.setDteApproveddate2(commonService.getCurrentTimeStamp_new());
                    order.setSerApprovedbyId3(commonService.getCurrentLoggedInUser());
                    order.setDteApproveddate3(commonService.getCurrentTimeStamp_new());
                    order.setNumLevel(4);
                    order.setTxtLevel("APPROVED");
                    order.setTxtStatus("APPROVED");
                    String msg2 = "";
                    try {
                        List<SlsTblSoDetail> lstDetail = searchSaleOrderDetail(order.getSerSaleOrderId());
                        msg = integeration.CreateSOInSHERP(order, lstDetail);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                    order.setTxtReturnMsg(msg);
                    if (msg2.indexOf("Success") > 0) {
                        order.setBlIsPOSTEDToSAP(true);

                    }
                    updateSaleOrderSAP(order);

                    log.info("after--------SaveSOInSAP");
                } else {
                    boolean isPricechange = false;
                    if (Level == 1) {
                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        // Level 1 processing
                        if (isUserInRole("MARKETING_HEAD")) {
                            setupApprovedOrder(order, dto);
                            auditSlsTblSaleOrder.setTxtLevel("1");
                            auditSlsTblSaleOrder.setTxtNextLevel("2");
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtMarketingRemarks());
                            order.setTxtStatus("InProgress");
                            order.setSerApprovedbyId1(commonService.getCurrentLoggedInUser());
                            order.setDteApproveddate1(commonService.getCurrentTimeStamp_new());
                            order.setSerApprovedbyName1(cfgTblUser.getTxtUserName());

                        } else {
                            order.setSerApprovedbyId1(commonService.getCurrentLoggedInUser());
                            order.setDteApproveddate1(commonService.getCurrentTimeStamp_new());
                            order.setSerApprovedbyName1(cfgTblUser.getTxtUserName());
                            setupPendingOrder(order, dto);
                            auditSlsTblSaleOrder.setTxtLevel("1");
                            auditSlsTblSaleOrder.setTxtNextLevel("1");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtMarketingRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }
                        log.info("Updating order at level 1.");
                        if (delivery != null) {
                            order.setDteDeliveryDate(delivery);
                        }
                        msg = processSaleOrderDetails(order, msg,isUserInRole("MARKETING_HEAD"));
                        if(msg.equals("Success")){
                            setApprovalDetails(order);
                            updateSaleOrderSAP(order);
                            log.info("After updating sale order SAP.");
                            auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            auditSlsTblSaleOrder.setTxtDepartment("Marketing");
                            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                            updateAuditDetail(auditSlsTblSaleOrder);
                        }else{
                            setupPendingOrder(order, dto);
                            updateSaleOrderSAPPending(order);
                            auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                            auditSlsTblSaleOrder.setTxtDepartment("Marketing");
                            auditSlsTblSaleOrder.setTxtLevel("1");
                            auditSlsTblSaleOrder.setTxtNextLevel("1");
                            auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                            updateAuditDetail(auditSlsTblSaleOrder);
                        }


                    } else if (Level == 2) {
                        log.info("updating order at level 2--------");
                        if (delivery != null)
                            order.setDteDeliveryDate(delivery);


                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        if (isUserInRole("PROCUREMENT_HEAD")) {
                            order.setTxtLevel("THIRD");
                            order.setNumLevel(3);
                            auditSlsTblSaleOrder.setTxtLevel("2");
                            auditSlsTblSaleOrder.setTxtNextLevel("3");
                            order.setTxtStatus2("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtProcurementRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");

                            if ("Send Back".equals(order.getTxtStatus3()) ||
                                    order.getTxtStatus3() == null ||
                                    order.getTxtStatus3().isEmpty()) {
                                order.setTxtStatus3("Pending");
                            }
                        } else {

                            order.setTxtLevel("SECOND");
                            order.setNumLevel(2);
                            order.setTxtStatus2("Pending");
                            auditSlsTblSaleOrder.setTxtLevel("2");
                            auditSlsTblSaleOrder.setTxtNextLevel("2");
                            order.setTxtProcurementRemarks(dto.getTxtProcurementRemarks());
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtProcurementRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }


                      //  order.setTxtStatus2("APPROVED");
                        order.setTxtStatus("InProgress");
                      //  order.setTxtProcurementRemarks(dto.getTxtProcurementRemarks());
                        order.setSerApprovedbyId2(commonService.getCurrentLoggedInUser());
                        order.setDteApproveddate2(commonService.getCurrentTimeStamp_new());

                        if (isPricechange) {
                            log.info("updating order at level 2--------price change");
                           // order.setTxtLevel("THIRD");
                           // order.setNumLevel(3);
                        } else {
                            log.info("updating order at level 2--------Price not changed");
                       //     order.setNumLevel(4);
                       //     order.setTxtLevel("APPROVED");
                       //     order.setTxtStatus("APPROVED");
                        }

                        String msg2 = "";
                        try {
                            List<SlsTblSoDetail> lstDetail = searchSaleOrderDetail(order.getSerSaleOrderId());
//						msg = integeration.CreateSOInSHERP(order,lstDetail);
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        }
//					 order.setTxtReturnMsg(msg);
                        if (msg2.indexOf("Success") > 0) {
                            order.setBlIsPOSTEDToSAP(true);

                        }
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        order.setSerApprovedbyName2(cfgTblUser.getTxtUserName());
                        order.setTxtSapInvoiceNo(dto.getTxtSapInvoiceNo());
                        updateSaleOrderSAP(order);
                    //    AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                    //    auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                    //    auditSlsTblSaleOrder.setTxtLevel("2");
                    //    auditSlsTblSaleOrder.setTxtNextLevel("3");
                    //    auditSlsTblSaleOrder.setTxtRemarks(order.getTxtProcurementRemarks());
                        auditSlsTblSaleOrder.setTxtDepartment("PROCUREMENT");
                        auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                      //  CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                        //auditSlsTblSaleOrderRepository.save(auditSlsTblSaleOrder);
                        updateAuditDetail(auditSlsTblSaleOrder);
                    } else if (Level == 3) {
                        log.info("updating order at level 3--------");
                        if (delivery != null)
                            order.setDteDeliveryDate(delivery);

                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        if (isUserInRole("TAX_HEAD")) {
                            order.setTxtLevel("FOURTH");
                            order.setNumLevel(4);
                            auditSlsTblSaleOrder.setTxtLevel("3");
                            auditSlsTblSaleOrder.setTxtNextLevel("4");
                            order.setTxtStatus3("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtTaxRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            if ("Send Back".equals(order.getTxtStatus4()) ||
                                    order.getTxtStatus4() == null ||
                                    order.getTxtStatus4().isEmpty()) {

                                order.setTxtStatus4("Pending");
                            }

                        } else {

                            order.setTxtLevel("THIRD");
                            order.setNumLevel(3);
                            order.setTxtStatus3("Pending");
                            auditSlsTblSaleOrder.setTxtLevel("3");
                            auditSlsTblSaleOrder.setTxtNextLevel("3");
                            order.setTxtTaxRemarks(dto.getTxtTaxRemarks());
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtTaxRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }

                     //   order.setTxtStatus3("APPROVED");
                        order.setTxtStatus("InProgress");
                        order.setSerApprovedbyId3(commonService.getCurrentLoggedInUser());
                        order.setDteApproveddate3(commonService.getCurrentTimeStamp_new());
                        /*CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());*/
                     //   order.setNumLevel(4);
                     //   order.setTxtLevel("FOURTH");
                     //   order.setTxtTaxRemarks(dto.getTxtTaxRemarks());
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        order.setSerApprovedbyName3(cfgTblUser.getTxtUserName());
                        updateSaleOrderSAP(order);

                    //    AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                     //   auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                   //     auditSlsTblSaleOrder.setTxtLevel("3");
                   //     auditSlsTblSaleOrder.setTxtNextLevel("4");
                   //     auditSlsTblSaleOrder.setTxtRemarks(order.getTxtTaxRemarks());
                        auditSlsTblSaleOrder.setTxtDepartment("TAX");
                        auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                       // auditSlsTblSaleOrderRepository.save(auditSlsTblSaleOrder);
                        auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                   //     CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                        updateAuditDetail(auditSlsTblSaleOrder);

                    } else if (Level == 4) {
                        log.info("updating order at level 3--------");
                        if (delivery != null)
                            order.setDteDeliveryDate(delivery);


                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        if (isUserInRole("FINANCE_HEAD")) {
                            order.setTxtLevel("FIFTH");
                            order.setNumLevel(5);
                            auditSlsTblSaleOrder.setTxtLevel("4");
                            auditSlsTblSaleOrder.setTxtNextLevel("5");
                            order.setTxtStatus4("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtFinanceRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");

                            if ("Send Back".equals(order.getTxtStatus5()) ||
                                    order.getTxtStatus5() == null ||
                                    order.getTxtStatus5().isEmpty()) {

                                order.setTxtStatus5("Pending");
                            }
                        } else {

                            order.setTxtLevel("FOURTH");
                            order.setNumLevel(4);
                            order.setTxtStatus4("Pending");
                            auditSlsTblSaleOrder.setTxtLevel("4");
                            auditSlsTblSaleOrder.setTxtNextLevel("4");
                            order.setTxtFinanceRemarks(dto.getTxtFinanceRemarks());
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtFinanceRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }


                   //     order.setTxtStatus4("APPROVED");
                        order.setTxtStatus("InProgress");
                        order.setSerApprovedbyId4(commonService.getCurrentLoggedInUser());
                        order.setDteApproveddate4(commonService.getCurrentTimeStamp_new());
                 //       order.setNumLevel(5);
                 //       order.setTxtLevel("FIFTH");
                    //    order.setTxtFinanceRemarks(dto.getTxtFinanceRemarks());
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        order.setSerApprovedbyName4(cfgTblUser.getTxtUserName());
                        updateSaleOrderSAP(order);
                  //      AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                    //    auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                  //      auditSlsTblSaleOrder.setTxtLevel("4");
                  //      auditSlsTblSaleOrder.setTxtNextLevel("5");
                    //    auditSlsTblSaleOrder.setTxtRemarks(order.getTxtFinanceRemarks());
                        auditSlsTblSaleOrder.setTxtDepartment("Finance");
                        auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                      //  CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                        /*auditSlsTblSaleOrderRepository.save(auditSlsTblSaleOrder);*/
                        updateAuditDetail(auditSlsTblSaleOrder);

                    } else if (Level == 5) {
                        log.info("updating order at level 3--------");
                        if (delivery != null)
                            order.setDteDeliveryDate(delivery);


                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        if (isUserInRole("AUDIT_HEAD")) {
                            order.setTxtLevel("SIXTH");
                            order.setNumLevel(6);
                            auditSlsTblSaleOrder.setTxtLevel("5");
                            auditSlsTblSaleOrder.setTxtNextLevel("6");
                            order.setTxtStatus5("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            if ("Send Back".equals(order.getTxtStatus6()) ||
                                    order.getTxtStatus6() == null ||
                                    order.getTxtStatus6().isEmpty()) {

                                order.setTxtStatus6("Pending");
                            }
                          //  order.setTxtStatus5("APPROVED");
                        } else {

                            order.setTxtLevel("FIFTH");
                            order.setNumLevel(5);
                            order.setTxtStatus5("Pending");
                            auditSlsTblSaleOrder.setTxtLevel("5");
                            auditSlsTblSaleOrder.setTxtNextLevel("5");
                            order.setTxtAuditRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }

                  //      order.setTxtStatus5("APPROVED");
                        order.setTxtStatus("InProgress");
                        order.setSerApprovedbyId5(commonService.getCurrentLoggedInUser());
                        order.setDteApproveddate5(commonService.getCurrentTimeStamp_new());
                 //       order.setNumLevel(6);
                  //      order.setTxtLevel("APPROVED");
                  //      order.setTxtStatus("APPROVED");
                  //      order.setTxtAuditRemarks(dto.getTxtAuditRemarks());
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        order.setSerApprovedbyName5(cfgTblUser.getTxtUserName());
                        updateSaleOrderSAP(order);
                    //    AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                   //     auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                  //      auditSlsTblSaleOrder.setTxtLevel("5");
                  //      auditSlsTblSaleOrder.setTxtNextLevel("6");
                  //      auditSlsTblSaleOrder.setTxtRemarks(order.getTxtAuditRemarks());
                        auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                        auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                   //     CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                       /* auditSlsTblSaleOrderRepository.save(auditSlsTblSaleOrder);*/
                        updateAuditDetail(auditSlsTblSaleOrder);
                        /*SlsTblDeal parentDeal = order.getSlsTblDeal();
                        List<SlsTblSoDetail> soDetailsList = order.getSlsTblSoDetails();

                        for (SlsTblSoDetail soDetail : soDetailsList) {
                           *//* ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = new ZkfEInvoiceSesWebService();
                            zkfEInvoiceSesWebService.setEbeln(order.getSlsTblDeal().getTxtSapNo());
                            zkfEInvoiceSesWebService.setEbelp(soDetail.getSapLineItem());
                            zkfEInvoiceSesWebService.setDocDate("2024-09-05"); // for testing
                            zkfEInvoiceSesWebService.setPostDate("2024-09-05");// for testing
                            zkfEInvoiceSesWebService.setQuantity(soDetail.getNumQuantity());*//*
                            ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = new ZkfEInvoiceSesWebService();
                            zkfEInvoiceSesWebService.setEbeln(parentDeal.getTxtSapNo());  // From parent deal
                            zkfEInvoiceSesWebService.setEbelp(soDetail.getSapLineItem());
                            zkfEInvoiceSesWebService.setDocDate("2024-09-05"); // For testing
                            zkfEInvoiceSesWebService.setPostDate("2024-09-05"); // For testing
                            zkfEInvoiceSesWebService.setQuantity(soDetail.getNumQuantity());
                            SOAPMessage response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
                            List<SlsTblSaleOrder> slsTblSaleOrder = soapClientService.readSOAPResponseSES(response, order);
                            // We assume that we get only one parent deal in the response
                            if (!slsTblSaleOrder.isEmpty()) {
                                SlsTblSaleOrder responseDeal = slsTblSaleOrder.get(0);
                                SlsTblSaleOrder slsTblSaleOrder1 =new SlsTblSaleOrder();
                                slsTblSaleOrder1 = order;
                              //  parentDeal.setSerDealId(responseDeal.getSerDealId());
                              //  parentDeal.setTxtDealNo(responseDeal.getTxtDealNo());
                              //  parentDeal.setBlIsDeleted(false);
                                slsTblSaleOrder1.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                                slsTblSaleOrder1.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                               // updateDealSAP(parentDeal);
                                updateSaleOrderSAP(slsTblSaleOrder1);
                                soDetail.setSerSoDetailId(soDetail.getSerSoDetailId());
                                soDetail.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                                soDetail.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                                updateSoDetail(soDetail);
                            }
                            *//*for (SlsTblDeal deal : slsTblDeals) {

                                try {
                                    deal.setSerDealId(order.getSlsTblDeal().getSerDealId());
                                    deal.setTxtDealNo(order.getSlsTblDeal().getTxtSapNo());
                                    deal.setBlIsDeleted(false);
                                    updateDealSAP(deal);
                                } catch (Exception e) {

                                    System.err.println("Error while saving deal: " + deal.getTxtDealNo());
                                    e.printStackTrace();
                                }
                            }*//*
                        }*/

                    } else if (Level == 6) {
                        log.info("updating order at level 3--------");
                        if (delivery != null)
                            order.setDteDeliveryDate(delivery);


                        AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        if (isUserInRole("PAYMENT_HEAD")) {
                            order.setTxtLevel("SIXTH");
                            order.setNumLevel(6);
                            auditSlsTblSaleOrder.setTxtLevel("5");
                            auditSlsTblSaleOrder.setTxtNextLevel("5");
                            order.setTxtStatus6("APPROVED");
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            order.setTxtStatus("APPROVED");
                        } else {

                            order.setTxtLevel("SIXTH");
                            order.setNumLevel(6);
                            order.setTxtStatus6("Pending");
                            auditSlsTblSaleOrder.setTxtLevel("6");
                            auditSlsTblSaleOrder.setTxtNextLevel("6");
                            order.setTxtAuditRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtAuditRemarks());
                            auditSlsTblSaleOrder.setTxtStatus("Pending");
                        }

                        //      order.setTxtStatus5("APPROVED");
                        //     order.setTxtStatus("InProgress");
                        order.setSerApprovedbyId6(commonService.getCurrentLoggedInUser());
                      //  order.setDteApproveddate6(commonService.getCurrentTimeStamp_new());
                        //       order.setNumLevel(6);
                        //      order.setTxtLevel("APPROVED");
                        //      order.setTxtStatus("APPROVED");
                        //      order.setTxtAuditRemarks(dto.getTxtAuditRemarks());
                        CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        order.setSerApprovedbyName6(cfgTblUser.getTxtUserName());
                        updateSaleOrderSAP(order);
                        //    AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                        auditSlsTblSaleOrder.setSerSaleOrderId(order.getSerSaleOrderId());
                        //     auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                        //      auditSlsTblSaleOrder.setTxtLevel("5");
                        //      auditSlsTblSaleOrder.setTxtNextLevel("6");
                        //      auditSlsTblSaleOrder.setTxtRemarks(order.getTxtAuditRemarks());
                        auditSlsTblSaleOrder.setTxtDepartment("PAYMENT");
                        auditSlsTblSaleOrder.setSerApprovedbyId(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                    //    CfgTblUser cfgTblUser = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
                        auditSlsTblSaleOrder.setApprovedNAME(cfgTblUser.getTxtUserName());
                        /* auditSlsTblSaleOrderRepository.save(auditSlsTblSaleOrder);*/
                        updateAuditDetail(auditSlsTblSaleOrder);
                        /*SlsTblDeal parentDeal = order.getSlsTblDeal();
                        List<SlsTblSoDetail> soDetailsList = order.getSlsTblSoDetails();

                        for (SlsTblSoDetail soDetail : soDetailsList) {
                           *//* ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = new ZkfEInvoiceSesWebService();
                            zkfEInvoiceSesWebService.setEbeln(order.getSlsTblDeal().getTxtSapNo());
                            zkfEInvoiceSesWebService.setEbelp(soDetail.getSapLineItem());
                            zkfEInvoiceSesWebService.setDocDate("2024-09-05"); // for testing
                            zkfEInvoiceSesWebService.setPostDate("2024-09-05");// for testing
                            zkfEInvoiceSesWebService.setQuantity(soDetail.getNumQuantity());*//*
                            ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = new ZkfEInvoiceSesWebService();
                            zkfEInvoiceSesWebService.setEbeln(parentDeal.getTxtSapNo());  // From parent deal
                            zkfEInvoiceSesWebService.setEbelp(soDetail.getSapLineItem());
                            zkfEInvoiceSesWebService.setDocDate("2024-09-05"); // For testing
                            zkfEInvoiceSesWebService.setPostDate("2024-09-05"); // For testing
                            zkfEInvoiceSesWebService.setQuantity(soDetail.getNumQuantity());
                            SOAPMessage response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
                            List<SlsTblSaleOrder> slsTblSaleOrder = soapClientService.readSOAPResponseSES(response, order);
                            // We assume that we get only one parent deal in the response
                            if (!slsTblSaleOrder.isEmpty()) {
                                SlsTblSaleOrder responseDeal = slsTblSaleOrder.get(0);
                                SlsTblSaleOrder slsTblSaleOrder1 =new SlsTblSaleOrder();
                                slsTblSaleOrder1 = order;
                              //  parentDeal.setSerDealId(responseDeal.getSerDealId());
                              //  parentDeal.setTxtDealNo(responseDeal.getTxtDealNo());
                              //  parentDeal.setBlIsDeleted(false);
                                slsTblSaleOrder1.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                                slsTblSaleOrder1.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                               // updateDealSAP(parentDeal);
                                updateSaleOrderSAP(slsTblSaleOrder1);
                                soDetail.setSerSoDetailId(soDetail.getSerSoDetailId());
                                soDetail.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                                soDetail.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                                updateSoDetail(soDetail);
                            }
                            *//*for (SlsTblDeal deal : slsTblDeals) {

                                try {
                                    deal.setSerDealId(order.getSlsTblDeal().getSerDealId());
                                    deal.setTxtDealNo(order.getSlsTblDeal().getTxtSapNo());
                                    deal.setBlIsDeleted(false);
                                    updateDealSAP(deal);
                                } catch (Exception e) {

                                    System.err.println("Error while saving deal: " + deal.getTxtDealNo());
                                    e.printStackTrace();
                                }
                            }*//*
                        }*/

                    }
                }
            }

        }

        return msg;
//			Success
    }

    public String updateDealSAP(SlsTblDeal slsTblDeal) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            slsTblDeal.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            slsTblDeal.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(slsTblDeal);

            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }


    public String updateSoDetail(SlsTblSoDetail soDetail) {

        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
          //  SlsTblSoDetail existingDetail = entityManager.find(SlsTblSoDetail.class,soDetail.getSerSoDetailId());

            if (soDetail != null) {
                entityManager.merge(soDetail);
            } else {
                // Save new detail (if it doesn't already exist)
                entityManager.persist(soDetail);
            }
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
           // System.out.println("Detail updated/saved successfully: " + soDetail.getSapLineItem());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }

    public boolean checkISPriceChange(SlsTblSaleOrder order) {
//		log.info("inside-------checkISPriceChange");
//		EntityManager entityManager = getEntityManager();
//		entityManager.getTransaction().begin();
//		/*
//		 * List<SlsTblSaleOrder> SaleOrders =
//		 * entityManager.createQuery("FROM SlsTblSaleOrder where blIsDeleted=FALSE")
//		 * .getResultList();
//		 */
//
//		List<SlsTblSoDetail> lstSODetail = new ArrayList();
//
//		lstSODetail = entityManager
//				.createQuery(" FROM SlsTblSoDetail d where d.blIsDeleted=FALSE and d.slsTblSaleOrder.serSaleOrderId = "
//						+ order.getSerSaleOrderId())
//				.getResultList();
//
//		entityManager.getTransaction().commit();
//		entityManager.close();
//		if (lstSODetail != null && lstSODetail.size() > 0) {
//			for (SlsTblSoDetail dtoDetial : lstSODetail) {
//				if(dtoDetial.getCfgTblProduct().getNumSalePrice() == null)
//					dtoDetial.getCfgTblProduct().setNumSalePrice(BigDecimal.ZERO);
//				if (dtoDetial.getNumItemPrice().doubleValue() != dtoDetial.getCfgTblProduct().getNumSalePrice()
//						.doubleValue()) {
//					return true;
//				}
//			}
//		}
//		return false;
        return true;
    }

    private String createSOAPRequest(SlsTblSaleOrder dto) throws Exception {
        log.info("inside--------createSOAPRequest");
        MessageFactory messageFactory = MessageFactory.newInstance();
        String serverURI = "urn:sap-com:document:sap:rfc:functions";
        final SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        String authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
        MimeHeaders hd = soapMessage.getMimeHeaders();
        hd.addHeader("Authorization", "Basic " + authorization);
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
        soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:rfc:functions");
        SOAPBody soapBody = soapEnvelope.getBody();
        JAXBContext jaxbContext = JAXBContext.newInstance(ITAB.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        ITAB master = new ITAB();


        if (dto.getSlsTblDeal() != null && dto.getSlsTblDeal().getTxtDealNo() != null && dto.getSlsTblDeal().getTxtDealNo().trim().length() > 0)
            master.setASSIGNMENT_NUMBER(dto.getSlsTblDeal().getTxtDealNo());
        else
            master.setASSIGNMENT_NUMBER("NONDEALORDER");


        master.setBookingdate(dto.getDteDate().toString());
        if (dto.getCfgTblDealerOne() != null) {
            if (dto.getCfgTblDealerOne().getTxtCustomerCode().trim().length() == 10) {
                master.setCustomercode(dto.getCfgTblDealer().getTxtCustomerCode());
                master.setDealercode(dto.getCfgTblDealerOne().getTxtCustomerCode());
            } else {
                if (dto.getCfgTblDealer() != null)
                    master.setCustomercode("00" + dto.getCfgTblDealer().getTxtCustomerCode());
                else
                    master.setCustomercode("00" + dto.getCfgTblDealerOne().getTxtCustomerCode());
                master.setDealercode("00" + dto.getCfgTblDealerOne().getTxtCustomerCode());
            }

        } else {
            if (dto.getCfgTblDealer().getTxtCustomerCode().trim().length() == 8) {
                master.setCustomercode(dto.getCfgTblDealer().getTxtCustomerCode());
                master.setDealercode(dto.getCfgTblDealer().getTxtCustomerCode());
            } else {

                master.setCustomercode("00" + dto.getCfgTblDealer().getTxtCustomerCode());
                master.setDealercode("00" + dto.getCfgTblDealer().getTxtCustomerCode());
            }
        }

        if (dto.getDteDeliveryDate() == null)
            dto.setDteDeliveryDate(new Date());
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(dto.getDteDeliveryDate());
        System.out.println(date);
        master.setDeliverydate(date);

        if (dto.getCfgTblCustomer() != null) {
            if (dto.getCfgTblCustomer().getTxtCustomerCode().trim().length() == 10) {
                master.setEnduser(dto.getCfgTblCustomer().getTxtCustomerCode());
            } else {
                master.setEnduser("00" + dto.getCfgTblCustomer().getTxtCustomerCode());
            }
        } else
            master.setEnduser(master.getDealercode());

        master.setInvoicetext("Invoice");

        if (!(dto.getBlIsGAL() != null && dto.getBlIsGAL())) {
            if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Commercial")) {
                master.setOrdertype("Z3O5");
            } else if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {
                master.setOrdertype("Z3O2");
            } else if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Fabrication")) {
                master.setOrdertype("Z3O2");
            } else {
                master.setOrdertype("Z3O1");
            }
        } else {
            if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Commercial")) {
                master.setOrdertype("Z2O5");
            } else if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {
                master.setOrdertype("Z2O2");
            } else if (dto.getTxtpOrderType() != null && dto.getTxtpOrderType().equalsIgnoreCase("Fabrication")) {
                master.setOrdertype("Z2O2");
            } else {
                master.setOrdertype("Z2O1");
            }
        }
        master.setPsonumber(dto.getTxtSaleOrderNo());

//		if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("PickUp-Other-than-carr")) {
//			master.setPricegroup("02");
//		} else if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("Truck-Carriage")) {
//			master.setPricegroup("03");
//		} else if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("Truck-Other-than-carr")) {
//			master.setPricegroup("04");
//		} else if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("Buses-Pvt")) {
//			master.setPricegroup("05");
//		} else if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("Buses-Pub")) {
//			master.setPricegroup("06");
//		} else if (dto.getTxtPriceGroup() != null && dto.getTxtPriceGroup().equalsIgnoreCase("Exempt")) {
//			master.setPricegroup("07");
//		} else {
//			master.setPricegroup("01");
//		}

        if (dto.getTxtDescription() != null && dto.getTxtDescription().trim().length() > 0) {
            master.setRemarks(dto.getTxtDescription());
        } else
            master.setRemarks(" ");

        List lstPartner = new ArrayList();

        Header header = new Header();

        SlsTblSoDetail detailDTO = new SlsTblSoDetail();
        SlsTblSoDetail soDetail = new SlsTblSoDetail();
        Iterator<SlsTblSoDetail> itr = dto.getSlsTblSoDetails().iterator();
        boolean check = true;
        int i = 0;
        while (itr.hasNext()) {

            soDetail = (SlsTblSoDetail) itr.next();
            if (soDetail.getNumQuantity() != null && soDetail.getNumQuantity().doubleValue() > 0) {
                detailDTO = soDetail;

                if (check) {
                    i++;
                    Partner detail = new Partner();
                    int value = i * 10;
                    detail.setLineitem("0000" + (value + ""));

                    if (soDetail.getTxtColour() != null && soDetail.getTxtColour().trim().length() > 0)
                        master.setColor(soDetail.getTxtColour());
                    else
                        master.setColor(" ");

                        detail.setColor(" ");

                    if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts"))
                        detail.setMaterialcode(soDetail.getCfgTblProduct().getTxtProductCode());
                    else if (soDetail.getCfgTblProduct().getTxtProductCode().length() == 18)
                        detail.setMaterialcode(soDetail.getCfgTblProduct().getTxtProductCode());
                    else
                        detail.setMaterialcode("000000000" + soDetail.getCfgTblProduct().getTxtProductCode());


                    if (soDetail.getNumItemPrice() != null && soDetail.getNumItemPrice().doubleValue() > 0) {
                        detail.setZpr(soDetail.getNumItemPrice().toString());
                        if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {

                        } else if (!(master.getOrdertype().equalsIgnoreCase("Z2O5")))
                            detail.setZait(soDetail.getNumItemPrice().toString());
                    } else {
                        detail.setZpr("0");
                        if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {

                        } else if (!(master.getOrdertype().equalsIgnoreCase("Z2O5")))
                            detail.setZait("0");
                    }


                    if (soDetail.getNumEngineCC() != null && soDetail.getNumEngineCC().trim().length() > 0) {
                        if (soDetail.getNumEngineCC().indexOf("2500") >= 0
                                || soDetail.getNumEngineCC().equalsIgnoreCase("2500"))
                            detail.setMvgr3("001");
                        else
                            detail.setMvgr3("002");
                    } else {
                        detail.setMvgr3("002");
                    }


                    if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts")) {

                    } else if (master.getOrdertype().equalsIgnoreCase("Z2O1")) {
                        if (dto.getNumFreight() != null && dto.getNumFreight().doubleValue() > 0) {
                            detail.setZfni((dto.getNumFreight().doubleValue() / (dto.getSlsTblSoDetails().size())) + "");
                        } else
                            detail.setZfni("0");
                    }

                    detail.setQuantity(soDetail.getNumQuantity().toString());

                    lstPartner.add(detail);

                }

            }
        }

        header.setLstPartner(lstPartner);

        try {
            log.info("-------header----" + header.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        master.setHeader(header);
//		       jaxbMarshaller.marshal(customer, System.err);
        jaxbMarshaller.marshal(master, soapBody);
        soapMessage.saveChanges();
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
        soapMessage.writeTo(System.out);

        hd.addHeader("SOAPAction", serverURI + "ReadProjects");

        // Save the message
        soapMessage.saveChanges();
        log.info("Request SOAP Message----" + System.out);
        // Check the input
        System.out.println("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String strMsg = soapMessageToString(soapMessage);

        return strMsg;
    }

    public static String soapMessageToString(SOAPMessage message) {
        log.info("inside------soapMessageToString");
        String result = null;

        if (message != null) {
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                message.writeTo(baos);
                result = baos.toString();
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            } finally {
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        }
        return result;
    }

    void SaveSOInSAP(SlsTblSaleOrder dto) {

        try {
            try {
                log.info("inside--------SaveSOInSAP" + dto.getTxtSaleOrderNo());
            } catch (Exception e) {
                // TODO: handle exception
            }
            String msg = createSOAPRequest(dto);
            String username = "aizaz.k";
            String password = "S@pabap123";

            // String url =
            // "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/wsdl/flv_10002A111AD1/bndg_url/sap/bc/srt/rfc/sap/zham_sd_from_dms_custmr_create/110/zham_sd_from_dms_custmr_create/zham_sd_from_dms_custmr_create?sap-client=110";
//			String url = "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_so/110/zham_sd_from_dms_create_so/zham_sd_from_dms_create_so";
//			String url = "http://vhgdiqs4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_so/100/zham_sd_from_dms_create_so/zham_sd_from_dms_create_so";
            String url = ServerConfiguration.service_so;
            log.info("url--service_so--" + url);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/soap+xml");
//			con.setRequestProperty("Cookie", "sap-usercontext=sap-client=110");
//			con.setRequestProperty("Cookie", "sap-usercontext=sap-client=100");
            con.setRequestProperty("Cookie", ServerConfiguration.usercontext);
            con.setAllowUserInteraction(true);
            // String userpass = username + ":" + password;
            // String basicAuth = "Basic " + new
            // String(Base64.getEncoder().encode(userpass.getBytes()));
            // con.setRequestProperty ("Authorization", basicAuth);

            con.setAllowUserInteraction(true);
            String userpass = ServerConfiguration.Password + ":" + ServerConfiguration.uname;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            con.setRequestProperty("Authorization", basicAuth);
            log.info("msg----" + msg);
            String xml = msg;
            con.setDoOutput(true);
            log.info("1111111----");
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            log.info("2222222---");
            String responseStatus = con.getResponseMessage();

            log.info("33333---" + responseStatus);
            dto.setTxtReturnMsg(responseStatus);
            dto.setTxtXMSent(xml);
            updateSaleOrderSAP(dto);
            if (responseStatus.equalsIgnoreCase("Internal Server Error")) {

                log.info("------here is internal server error --------:");
                if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts"))
                    dto.setNumLevel(1);
                else
                    dto.setNumLevel(2);
                dto.setTxtErrorMsgFromSap("Internal Server Error, Some Field Size is Greator so unable to hit SAP.");
                dto.setBlIsPOSTEDToSAP(false);
                updateSaleOrderSAP(dto);
                return;
            }

            log.info("responseStatus----" + responseStatus);
            System.out.println(responseStatus);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println("response:" + response.toString());
            log.info("response----" + response.toString());
            dto.setTxtXMReceive(response.toString());
            updateSaleOrderSAP(dto);

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

                            String ret_msg = xmlStreamReader.getElementText();

                            if (ret_msg.equalsIgnoreCase("Success")) {
                                dto.setBlnIsApproved(true);
                                dto.setBlIsPOSTEDToSAP(true);
                                updateSaleOrderSAP(dto);
                            } else {
                                dto.setTxtErrorMsgFromSap(ret_msg);
                                dto.setBlIsPOSTEDToSAP(false);
                                dto.setTxtStatus("InProgress");
                                if (dto.getTxtpOrderType().equalsIgnoreCase("Spare parts"))
                                    dto.setNumLevel(1);
                                else
                                    dto.setNumLevel(2);
                                updateSaleOrderSAP(dto);
                            }

                        }

                    }

                    // Process end element.
                    if (xmlEvent == XMLStreamConstants.END_ELEMENT) {

                        System.out.println("End Element: " + xmlStreamReader.getLocalName());
                    }
                }

            } catch (Exception e) {
                log.info("End Element:---11111-" + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            log.info("End Element:---2222222-" + e.getMessage());
            System.out.println(e);
            e.printStackTrace();
        }

    }

    private static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void SavePaymentInSAP(SlsTblSoPayments dto, SlsTblSaleOrder slsTblSaleOrder) {

        if (dto.getTxtPaymentTerm() != null && dto.getTxtPaymentTerm().equalsIgnoreCase("Adjustment"))
            return;
        log.info("---inside ------SavePaymentInSAP" + slsTblSaleOrder.getTxtSaleOrderNo());
        try {

            String msg = createPaymentAPRequest(dto, slsTblSaleOrder);
            String username = "aizaz.k";
            String password = "S@pabap123";

            // String url =
            // "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_dpr/110/zham_sd_from_dms_create_dpr/zham_sd_from_dms_create_dpr";

            // String url =
            // "http://vhgdiqs4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_dpr/100/zham_sd_from_dms_create_dpr/zham_sd_from_dms_create_dpr";
            String url = ServerConfiguration.service_payment;
            log.info("url----" + url);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/soap+xml");
            // con.setRequestProperty("Cookie", "sap-usercontext=sap-client=110");
            // con.setRequestProperty("Cookie", "sap-usercontext=sap-client=100");
            con.setRequestProperty("Cookie", ServerConfiguration.usercontext);
            con.setAllowUserInteraction(true);
            // String userpass = username + ":" + password;
            // String basicAuth = "Basic " + new
            // String(Base64.getEncoder().encode(userpass.getBytes()));
            // con.setRequestProperty ("Authorization", basicAuth);

            con.setAllowUserInteraction(true);
            String userpass = ServerConfiguration.uname + ":" + ServerConfiguration.Password;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            con.setRequestProperty("Authorization", basicAuth);
            log.info("xml----" + msg);
            String xml = msg;
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            String responseStatus = con.getResponseMessage();
            dto.setTxtReturnMsg(responseStatus);
            dto.setTxtXMSent(xml);
            log.info("responseStatus----" + responseStatus);
            System.out.println(responseStatus);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            log.info("response----" + response.toString());
            System.out.println("response:" + response.toString());
            dto.setTxtXMReceive(response.toString());
            updateSaleOrderPayment(dto);

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

                            String ret_msg = xmlStreamReader.getElementText();

                            if (ret_msg.equalsIgnoreCase("Success")) {

                                dto.setBlIsPOSTEDToSAP(true);
                                updateSaleOrderPayment(dto);
                            } else {
                                dto.setTxtErrorMsgFromSap(ret_msg);
                                dto.setBlIsPOSTEDToSAP(false);

                                updateSaleOrderPayment(dto);
                            }

                        }

                    }

                    // Process end element.
                    if (xmlEvent == XMLStreamConstants.END_ELEMENT) {
                        System.out.println("End Element: " + xmlStreamReader.getLocalName());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (ConnectTimeoutException e) {
            System.out.println(e);
            log.error("ConnectTimeoutException----" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }

    private String createPaymentAPRequest(SlsTblSoPayments dto, SlsTblSaleOrder slsTblSaleOrder) throws Exception {
        log.info("---inside ------createPaymentAPRequest" + slsTblSaleOrder.getTxtSaleOrderNo());
        MessageFactory messageFactory = MessageFactory.newInstance();
        String serverURI = "urn:sap-com:document:sap:rfc:functions";
        final SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        String authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
        MimeHeaders hd = soapMessage.getMimeHeaders();
        hd.addHeader("Authorization", "Basic " + authorization);
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
        soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:rfc:functions");
        SOAPBody soapBody = soapEnvelope.getBody();
        JAXBContext jaxbContext = JAXBContext.newInstance(Payment.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        Payment master = new Payment();

        master.setCompanyCode("2000");

        if (dto.getTxtPaymentMethod() != null && dto.getTxtPaymentMethod().trim().length() > 0)
            master.setDepositbank(dto.getTxtPaymentMethod());
        else
            master.setDepositbank(" ");

        if (dto.getTxtSlipNo() != null && dto.getTxtSlipNo().trim().length() > 0)
            master.setInstrumentno(dto.getTxtSlipNo());
        else
            master.setInstrumentno(" ");

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(dto.getDteDate());
        System.out.println(date);
        if (date == null) {
            master.setInstrumentdate(" ");
            master.setInstrumentdate(" ");
        } else {
            master.setInstrumentdate(date);
            master.setDueondate(date);
        }

        master.setPayment(dto.getNumPaymentReceived().toString());

        if (slsTblSaleOrder.getTxtSaleOrderNo() != null && slsTblSaleOrder.getTxtSaleOrderNo().trim().length() > 0)

            master.setPsono(slsTblSaleOrder.getTxtSaleOrderNo());
        else
            master.setPsono(" ");

        List lstPartner = new ArrayList();

        try {
            log.info("Payment---:" + master.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//				       jaxbMarshaller.marshal(customer, System.err);
        jaxbMarshaller.marshal(master, soapBody);
        soapMessage.saveChanges();
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
        soapMessage.writeTo(System.out);

        hd.addHeader("SOAPAction", serverURI + "ReadProjects");

        // Save the message
        soapMessage.saveChanges();
        log.info("Request SOAP Message:--" + System.out);
        // Check the input
        System.out.println("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String strMsg = soapMessageToString(soapMessage);
        log.info("strMsg:--" + System.out);
        return strMsg;
    }

    //////////////////////////////////////////////////////////////////////////////////// Cancel
    //////////////////////////////////////////////////////////////////////////////////// Sale
    //////////////////////////////////////////////////////////////////////////////////// ORder////////////////////////////////////////
    private String createCancelSOAPRequest(SlsTblSaleOrder slsTblSaleOrder) throws Exception {
        log.info("---inside ------createPaymentAPRequest" + slsTblSaleOrder.getTxtSaleOrderNo());
        MessageFactory messageFactory = MessageFactory.newInstance();
        String serverURI = "urn:sap-com:document:sap:rfc:functions";
        final SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        String authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
        MimeHeaders hd = soapMessage.getMimeHeaders();
        hd.addHeader("Authorization", "Basic " + authorization);
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
        soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:rfc:functions");
        SOAPBody soapBody = soapEnvelope.getBody();
        JAXBContext jaxbContext = JAXBContext.newInstance(CancelSO.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        CancelSO master = new CancelSO();

//		if (slsTblSaleOrder.getTxtCancelReason() != null && slsTblSaleOrder.getTxtCancelReason().trim().length() > 0)
//			master.setRejectionreason(slsTblSaleOrder.getTxtCancelReason());
//		else
//			master.setRejectionreason(" ");

        if (slsTblSaleOrder.getTxtSaleOrderNo() != null && slsTblSaleOrder.getTxtSaleOrderNo().trim().length() > 0)

            master.setPsonumber(slsTblSaleOrder.getTxtSaleOrderNo());
        else
            master.setPsonumber(" ");

        List<CancelDetailInner> lstPartner = new ArrayList();
        CancelDetail dto2 = new CancelDetail();
        CancelDetailInner dto = new CancelDetailInner();
        dto.setPOSNR("10");
        dto.setABGRU("10");
        lstPartner.add(dto);
        dto2.setLstDetail(lstPartner);
        master.setCancelDetail(dto2);

        try {
            log.info("Payment---:" + master.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//				       jaxbMarshaller.marshal(customer, System.err);
        jaxbMarshaller.marshal(master, soapBody);
        soapMessage.saveChanges();
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
        soapMessage.writeTo(System.out);

        hd.addHeader("SOAPAction", serverURI + "ReadProjects");

        // Save the message
        soapMessage.saveChanges();
        log.info("Request SOAP Message:--" + System.out);
        // Check the input
        System.out.println("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String strMsg = soapMessageToString(soapMessage);
        log.info("strMsg:--" + System.out);
        return strMsg;
    }

    void CncelSOInSAP(SlsTblSaleOrder dto) {

        try {
            try {
                log.info("inside--------SaveSOInSAP" + dto.getTxtSaleOrderNo());
            } catch (Exception e) {
                // TODO: handle exception
            }
            String msg = createCancelSOAPRequest(dto);
            String username = "aizaz.k";
            String password = "S@pabap123";

            String url = ServerConfiguration.service_cancel_so;
            log.info("url--service_so--" + url);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/soap+xml");
//			con.setRequestProperty("Cookie", "sap-usercontext=sap-client=110");
//			con.setRequestProperty("Cookie", "sap-usercontext=sap-client=100");
            con.setRequestProperty("Cookie", ServerConfiguration.usercontext);
            con.setAllowUserInteraction(true);
            // String userpass = username + ":" + password;
            // String basicAuth = "Basic " + new
            // String(Base64.getEncoder().encode(userpass.getBytes()));
            // con.setRequestProperty ("Authorization", basicAuth);

            con.setAllowUserInteraction(true);
            String userpass = ServerConfiguration.Password + ":" + ServerConfiguration.uname;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            con.setRequestProperty("Authorization", basicAuth);
            log.info("msg----" + msg);
            String xml = msg;
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            String responseStatus = con.getResponseMessage();
            dto.setTxtReturnMsg(responseStatus);
            dto.setTxtXMSent(xml);
            log.info("responseStatus----" + responseStatus);
            System.out.println(responseStatus);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println("response:" + response.toString());
            log.info("response----" + response.toString());
            dto.setTxtXMReceive(response.toString());
            updateSaleOrderSAP(dto);

//				 StringReader sr = new StringReader(response.toString());
//				 JAXBContext jaxbContext = JAXBContext.newInstance(EndResult.class);
//				 Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//				 EndResult loginResult = (EndResult) unmarshaller.unmarshal(sr);
//
//
//				 System.out.println("response:" + loginResult.toString());

        } catch (ConnectTimeoutException e) {
            System.out.println(e);
            log.error("ConnectTimeoutException----" + e.getMessage());
            e.printStackTrace();

        } catch (Exception e) {
            log.error("Exception----" + e.getMessage());
            e.printStackTrace();
        }

    }

    //	@Override
    public String addNewSaleOrderToolDetail(List<SlsTblToolDetail> lstSlsTblToolDetail) {
        if (lstSlsTblToolDetail != null && lstSlsTblToolDetail.size() > 0) {
            for (SlsTblToolDetail slsTblToolDetail : lstSlsTblToolDetail) {
                log.info("---inside --------- addNewSaleOrder Tools");
                EntityManager entityManager = getEntityManager();
                try {
                    entityManager.getTransaction().begin();
                    // SlsTblSaleOrder.setBlnStatus(true);
                    slsTblToolDetail.setBlIsDeleted(false);
                    slsTblToolDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
                    slsTblToolDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());

                    entityManager.persist(slsTblToolDetail);

                    if (slsTblToolDetail.getSlsTblSaleOrder() != null) {
                        SlsTblSaleOrder slsTblSaleOrder = getSaleOrderByPK(
                                slsTblToolDetail.getSlsTblSaleOrder().getSerSaleOrderId());
                        slsTblToolDetail.setNumQuantity(new BigDecimal(1.0));

                        entityManager.getTransaction().commit();

                        entityManager.close();

//					if(slsTblToolDetail.getBlIsAdvance()!=null && !(slsTblToolDetail.getBlIsAdvance()))

                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return "Failure";
                }
            }
            return "Success";
        } else
            return "Failure";

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void SaveDealPaymentInSAP(SlsTblSoPayments dto, SlsTblDeal slsTblDeal) {

        if (dto.getTxtPaymentTerm() != null && dto.getTxtPaymentTerm().equalsIgnoreCase("Adjustment"))
            return;
        log.info("---inside ------SavePaymentInSAP" + slsTblDeal.getTxtDealNo());
        try {

            String msg = createPaymentDealRequest(dto, slsTblDeal);
            String username = "aizaz.k";
            String password = "S@pabap123";

            // String url =
            // "http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_dpr/110/zham_sd_from_dms_create_dpr/zham_sd_from_dms_create_dpr";

            // String url =
            // "http://vhgdiqs4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zham_sd_from_dms_create_dpr/100/zham_sd_from_dms_create_dpr/zham_sd_from_dms_create_dpr";
            String url = ServerConfiguration.service_payment;
            log.info("url----" + url);
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("PUT");
            con.setRequestProperty("Content-Type", "application/soap+xml");
            // con.setRequestProperty("Cookie", "sap-usercontext=sap-client=110");
            // con.setRequestProperty("Cookie", "sap-usercontext=sap-client=100");
            con.setRequestProperty("Cookie", ServerConfiguration.usercontext);
            con.setAllowUserInteraction(true);
            // String userpass = username + ":" + password;
            // String basicAuth = "Basic " + new
            // String(Base64.getEncoder().encode(userpass.getBytes()));
            // con.setRequestProperty ("Authorization", basicAuth);

            con.setAllowUserInteraction(true);
            String userpass = ServerConfiguration.uname + ":" + ServerConfiguration.Password;
            String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
            con.setRequestProperty("Authorization", basicAuth);
            log.info("xml----" + msg);
            String xml = msg;
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            String responseStatus = con.getResponseMessage();
            dto.setTxtReturnMsg(responseStatus);
            dto.setTxtXMSent(xml);
            log.info("responseStatus----" + responseStatus);
            System.out.println(responseStatus);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            log.info("response----" + response.toString());
            System.out.println("response:" + response.toString());
            dto.setTxtXMReceive(response.toString());
            updateSaleOrderPayment(dto);

            // StringReader sr = new StringReader(response.toString());
            // JAXBContext jaxbContext = JAXBContext.newInstance(EndResult.class);
            // Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // EndResult loginResult = (EndResult) unmarshaller.unmarshal(sr);
            //
            //
            // System.out.println("response:" + loginResult.toString());

        } catch (ConnectTimeoutException e) {
            System.out.println(e);
            log.error("ConnectTimeoutException----" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }

    private String createPaymentDealRequest(SlsTblSoPayments dto, SlsTblDeal slsTblDeal) throws Exception {
        log.info("---inside ------createPaymentDealRequest" + slsTblDeal.getTxtDealNo());
        MessageFactory messageFactory = MessageFactory.newInstance();
        String serverURI = "urn:sap-com:document:sap:rfc:functions";
        final SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        String authorization = Base64Coder.encodeString("aizaz.k:S@pabap123");
        MimeHeaders hd = soapMessage.getMimeHeaders();
        hd.addHeader("Authorization", "Basic " + authorization);
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
        soapEnvelope.addNamespaceDeclaration("urn", "urn:sap-com:document:sap:rfc:functions");
        SOAPBody soapBody = soapEnvelope.getBody();
        JAXBContext jaxbContext = JAXBContext.newInstance(Payment.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        Payment master = new Payment();

        if (dto.getTxtPaymentMethod() != null && dto.getTxtPaymentMethod().trim().length() > 0)
            master.setDepositbank(dto.getTxtPaymentMethod());
        else
            master.setDepositbank(" ");

        if (dto.getTxtSlipNo() != null && dto.getTxtSlipNo().trim().length() > 0)
            master.setInstrumentno(dto.getTxtSlipNo());
        else
            master.setInstrumentno(" ");

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(dto.getDteDate());
        System.out.println(date);
        if (date == null) {
            master.setInstrumentdate(" ");
            master.setInstrumentdate(" ");
        } else {
            master.setInstrumentdate(date);
            master.setDueondate(date);
        }

        master.setPayment(dto.getNumPaymentReceived().toString());

        if (slsTblDeal.getTxtDealNo() != null && slsTblDeal.getTxtDealNo().trim().length() > 0)

            master.setPsono(slsTblDeal.getTxtDealNo());
        else
            master.setPsono(" ");

        if (slsTblDeal.getCfgTblDealer() != null
                && slsTblDeal.getCfgTblDealer().getTxtCustomerCode().trim().length() > 0) {
            if (slsTblDeal.getCfgTblDealer().getTxtCustomerCode().trim().length() == 10) {
                master.setCustomer(slsTblDeal.getCfgTblDealer().getTxtCustomerCode());
            } else {
                master.setCustomer("0000" + slsTblDeal.getCfgTblDealer().getTxtCustomerCode());
            }
        }

        List lstPartner = new ArrayList();

        try {
            log.info("Payment---:" + master.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//				       jaxbMarshaller.marshal(customer, System.err);
        jaxbMarshaller.marshal(master, soapBody);
        soapMessage.saveChanges();
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");
        soapMessage.writeTo(System.out);

        hd.addHeader("SOAPAction", serverURI + "ReadProjects");

        // Save the message
        soapMessage.saveChanges();
        log.info("Request SOAP Message:--" + System.out);
        // Check the input
        System.out.println("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String strMsg = soapMessageToString(soapMessage);
        log.info("strMsg:--" + System.out);
        return strMsg;
    }


    @Override
    public String deletePayment(List<String> paymentIds) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            for (String serPaymentId : paymentIds) {
                SlsTblSoPayments soPayment = entityManager.find(SlsTblSoPayments.class, Integer.parseInt(serPaymentId));
                if (soPayment != null) {
                    if (soPayment.getSlsTblSaleOrder().getNumLevel() > 2) {
                        return "POSTED";


                    } else
                        soPayment.setBlIsDeleted(true);

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
    public String addToolsDetail(List<SlsTblToolDetail> lstSlsTblToolDetail) {

        if (lstSlsTblToolDetail != null && lstSlsTblToolDetail.size() > 0) {
            for (SlsTblToolDetail slsTblToolDetail : lstSlsTblToolDetail) {
                log.info("---inside --------- addNewSaleOrder Tools");
                EntityManager entityManager = getEntityManager();
                try {
                    entityManager.getTransaction().begin();
                    // SlsTblSaleOrder.setBlnStatus(true);
                    slsTblToolDetail.setBlIsDeleted(false);
                    slsTblToolDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
                    slsTblToolDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());

                    entityManager.persist(slsTblToolDetail);

                    if (slsTblToolDetail.getSlsTblSaleOrder() != null) {
                        SlsTblSaleOrder slsTblSaleOrder = getSaleOrderByPK(
                                slsTblToolDetail.getSlsTblSaleOrder().getSerSaleOrderId());


                        entityManager.getTransaction().commit();

                        entityManager.close();
                        slsTblSaleOrder.setBlnIsIssued(true);
                        updateSaleOrderSAP(slsTblSaleOrder);

//					if(slsTblToolDetail.getBlIsAdvance()!=null && !(slsTblToolDetail.getBlIsAdvance()))

                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return "Failure";
                }
            }
            return "Success";
        } else
            return "Failure";
    }


    @Override
    public String updateToolsDetail(List<SlsTblToolDetail> lstSlsTblToolDetail) {
        SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
        if (lstSlsTblToolDetail != null && lstSlsTblToolDetail.size() > 0) {
            for (SlsTblToolDetail slsTblToolDetail : lstSlsTblToolDetail) {
                log.info("---inside --------- addNewSaleOrder Tools");
                EntityManager entityManager = getEntityManager();
                try {
                    entityManager.getTransaction().begin();
                    // SlsTblSaleOrder.setBlnStatus(true);
                    slsTblToolDetail.setBlIsDeleted(false);
                    slsTblToolDetail.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
                    slsTblToolDetail.setDteCreateddate(commonService.getCurrentTimeStamp_new());

                    entityManager.merge(slsTblToolDetail);

                    if (slsTblToolDetail.getSlsTblSaleOrder() != null) {
                        slsTblSaleOrder = getSaleOrderByPK(
                                slsTblToolDetail.getSlsTblSaleOrder().getSerSaleOrderId());


                        entityManager.getTransaction().commit();

                        entityManager.close();
                        slsTblSaleOrder.setBlnIsReceived(true);


//					if(slsTblToolDetail.getBlIsAdvance()!=null && !(slsTblToolDetail.getBlIsAdvance()))

                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return "Failure";
                }
            }

            updateSaleOrderSAP(slsTblSaleOrder);
            return "Success";
        } else
            return "Failure";
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblToolDetail> searchSaleOrderToolDetail(int SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "";
        if (SaleOrderId > 0)
            query = "from SlsTblToolDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblSaleOrder.serSaleOrderId= "
                    + SaleOrderId;

        else
            return null;

        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<SlsTblToolDetail> lstSOtoolDetails = entityManager.createQuery(subQuery).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return lstSOtoolDetails;
    }


    @Override
    public String generateSaleOrderNoFORGNL(SlsTblSaleOrder so) {
        // int SaleOrderNo;
        String SaleOrderType = so.getTxtpOrderType();
        // String SaleOrderCode="";
        int ord_no = 0;
        int ord_no1 = 0;
        EntityManager entityManager = getEntityManager();

        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        {
            try {
                entityManager.getTransaction().begin();

                /*
                 * String zoneCode = (String) entityManager.
                 * createQuery("select MAX(txtSaleOrderNo) from SlsTblSaleOrder where serGroupId=1"
                 * ) .getSingleResult();
                 */
                String zoneCode = "";
                String zoneCode2 = "0";
//				if(user !=null 	&& user.getTxtrole().equalsIgnoreCase("SM"))
//				if (type != null && type.trim().length() == 3)
                List<String> lstUsers = new ArrayList();
                lstUsers.add("CSF");
                lstUsers.add("CIS");
                lstUsers.add("CMN");
                lstUsers.add("CGT");
                lstUsers.add("CCY");
                lstUsers.add("CGB");
                lstUsers.add("CLP");
                lstUsers.add("CCL");
                lstUsers.add("CID");
                lstUsers.add("GAL");

                if (user != null && lstUsers.indexOf(user.getTxtUserName()) > 0) {
                    String usr = user.getTxtUserName().trim();

//					   and len(b.txtSaleOrderNo)==6 length(b.txtSaleOrderNo)

                    try {
                        zoneCode2 = (String) entityManager
                                .createQuery("SELECT max(substring(b.txtSaleOrderNo,4,9))  FROM SlsTblSaleOrder b where b.txtSaleOrderNo like '"
                                        + usr + "%' and length(b.txtSaleOrderNo) = 9  ")
                                .setMaxResults(1).getSingleResult();

                        System.out.println("-----" + zoneCode2);
                    } catch (NoResultException ez) {
                        // TODO Auto-generated catch block
                        zoneCode = usr + "000000";
                        ez.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = usr + "";
                    }
//					ord_no1 = Integer.valueOf(zoneCode.substring(4));
                    try {
                        ord_no1 = Integer.valueOf(zoneCode2);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    ord_no1 = ord_no1 + 1;
                    String code = usr + "001";
                    if (ord_no1 < 10)
                        code = usr + "00000" + ord_no1;
                    else if (ord_no1 > 9 && ord_no1 < 100)
                        code = usr + "0000" + ord_no1;
                    else if (ord_no1 > 99 && ord_no1 < 1000)
                        code = usr + "000" + ord_no1;
                    else if (ord_no1 > 999 && ord_no1 < 10000)
                        code = usr + "00" + ord_no1;
                    else if (ord_no1 > 9999 && ord_no1 < 100000)
                        code = usr + "0" + ord_no1;
                    else
                        code = usr + "" + ord_no1;
                    return code;
                } else if (user != null && user.getTxtUserName().indexOf(".parts") > 0 && user.getTxtUserName().length() == 9) {
                    String usr = user.getTxtUserName().trim().substring(0, 3);
                    usr = usr + "P";
                    try {
                        zoneCode = (String) entityManager
                                .createQuery("SELECT b.txtSaleOrderNo FROM SlsTblSaleOrder b where b.txtSaleOrderNo like '"
                                        + usr + "%'  ORDER BY b.serSaleOrderId DESC")
                                .setMaxResults(1).getSingleResult();
                    } catch (NoResultException ez) {
                        // TODO Auto-generated catch block
                        zoneCode = usr + "-000";
                        ez.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = usr + "-";
                    }
                    ord_no1 = Integer.valueOf(zoneCode.substring(5));

                    ord_no1 = ord_no1 + 1;
                    String code = usr + "-001";
                    if (ord_no1 < 10)
                        code = usr + "-00" + ord_no1;
                    else if (ord_no > 9 && ord_no1 < 100)
                        code = usr + "-0" + ord_no1;
                    else
                        code = usr + "-" + ord_no1;
                    return code;
                } else {
//					000008001
                    String usr = "000";

//					   and len(b.txtSaleOrderNo)==6 length(b.txtSaleOrderNo)

                    try {
                        zoneCode2 = (String) entityManager
                                .createQuery("SELECT max(substring(b.txtSaleOrderNo,4,9))  FROM SlsTblSaleOrder b where b.txtSaleOrderNo like '"
                                        + 000 + "%' and length(b.txtSaleOrderNo) = 9  ")
                                .setMaxResults(1).getSingleResult();

                        System.out.println("-----" + zoneCode2);
                    } catch (NoResultException ez) {
                        // TODO Auto-generated catch block
                        zoneCode = usr + "000000";
                        ez.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (isNullOrEmpty(zoneCode)) {

                        zoneCode = usr + "";
                    }
//					ord_no1 = Integer.valueOf(zoneCode.substring(4));
                    try {
                        ord_no1 = Integer.valueOf(zoneCode2);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    ord_no1 = ord_no1 + 1;
                    String code = usr + "001";
                    if (ord_no1 < 10)
                        code = usr + "00000" + ord_no1;
                    else if (ord_no1 > 9 && ord_no1 < 100)
                        code = usr + "0000" + ord_no1;
                    else if (ord_no1 > 99 && ord_no1 < 1000)
                        code = usr + "000" + ord_no1;
                    else if (ord_no1 > 999 && ord_no1 < 10000)
                        code = usr + "00" + ord_no1;
                    else if (ord_no1 > 9999 && ord_no1 < 100000)
                        code = usr + "0" + ord_no1;
                    else
                        code = usr + "" + ord_no1;
                    return code;
                }


            } catch (NoResultException erz) {

//				if(user !=null 	&& user.getTxtrole().equalsIgnoreCase("SM"))
//				if (type != null && type.trim().length() == 3) {
//					String usr = type;
////					if(user.getTxtUserName().length() > 3)
////					{
////						usr = "IRM";
////					}else
////						usr=user.getTxtUserName();
//					return usr + "-001";
//				}

                return "000-001";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSoPayments> CheckPaymentDuplicationChequeNo(SlsTblSoPayments payment) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSoPayments where txtChequeNo='" + payment.getTxtChequeNo() + "'";

            List<SlsTblSoPayments> payments = entityManager.createQuery(query).getResultList();

            entityManager.close();
            if (payments.size() > 0) {
                return payments;
            }

            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SlsTblSoPayments> CheckPaymentDuplicationtxtSlipNo(SlsTblSoPayments payment) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSoPayments where txtSlipNo='" + payment.getTxtSlipNo() + "'";

            List<SlsTblSoPayments> payments = entityManager.createQuery(query).getResultList();

            entityManager.close();
            if (payments.size() > 0) {
                return payments;
            }

            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    @Override
    public String updateSaleOrderPayment(List<String> lstOrderPayments, int Level) {
        try {
            log.info("---------updateSaleOrderPayment-----" + lstOrderPayments.size() + "--level---:"
                    + Level);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        List<SlsTblSoPayments> lst = new ArrayList();
        for (String i : lstOrderPayments) {
            SlsTblSoPayments order = getSOPaymentsByPK(Integer.parseInt(i));
            lst.add(order);
        }
        String msg = "Success";// ApproveSaleOrderTOSAP(lst);
        if (msg.equalsIgnoreCase("Success")) {
            for (SlsTblSoPayments order : lst) {

//				order = getSOByPK(order.getSerSaleOrderId());
                {
                    SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
                    try {
                        order.setTxtOrderapprovalDate(outPutFormat.format(new Date().getTime()));
//								return outPutFormat.format(new Date().getTime());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                boolean isPricechange = false;
                if (Level == 1) {
                    log.info("updating order at level 1--------");

                    order.setTxtStatus1("APPROVED");
                    order.setTxtStatus("InProgress");
                    order.setSerApprovedbyId1(commonService.getCurrentLoggedInUser());
                    order.setDteApproveddate1(commonService.getCurrentTimeStamp_new());
                    order.setTxtLevel("SECOND");
                    order.setNumLevel(2);
                    log.info("before--------updateSaleOrderPaymnetSAP");
                    String msg2 = "";
                    try {
//						 List<SlsTblSoDetail> lstDetail = searchSaleOrderDetail(order.getSerSaleOrderId());
//						msg = integeration.CreateSOInSHERP(order,lstDetail);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                    order.setTxtReturnMsg(msg);
                    if (msg2.indexOf("Success") > 0) {
//						order.setBlIsPOSTEDToSAP(true);

                    }

                    updateSaleOrderPaymentNew(order);


                } else if (Level == 2) {
                    log.info("updating order at level 2--------");

                    order.setTxtStatus2("APPROVED");
                    order.setTxtStatus("InProgress");
                    order.setSerApprovedbyId2(commonService.getCurrentLoggedInUser());
                    order.setDteApproveddate2(commonService.getCurrentTimeStamp_new());

                    {
                        log.info("updating order at level 2--------Price not changed");
                        order.setNumLevel(4);
                        order.setTxtLevel("APPROVED");
                        order.setTxtStatus("APPROVED");
                    }

                    String msg2 = "";
                    try {
//						 List<SlsTblSoDetail> lstDetail = searchSaleOrderDetail(order.getSerSaleOrderId());
//						msg = integeration.CreateSOInSHERP(order,lstDetail);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
//					 order.setTxtReturnMsg(msg);
                    if (msg2.indexOf("Success") > 0) {
                        order.setBlIsPOSTEDToSAP(true);

                    }
//					updateSaleOrderSAP(order);

//					SaveSOInSAP(order);
                    log.info("after--------SaveSOInSAP");
                    SlsTblSoPayments slsTblSoPayments = new SlsTblSoPayments();
                    log.info("before--------SavePaymentInSAP");
//					slsTblSoPayments.setSlsTblSaleOrder(order);


                    updateSaleOrderPaymentNew(order);
//					log.info("after--------SaveSOInSAP");
//					SlsTblSoPayments slsTblSoPayments = new SlsTblSoPayments();
//					log.info("before--------SavePaymentInSAP");
//					slsTblSoPayments.setSlsTblSaleOrder(order);
//
//
//					List<SlsTblSoPayments> lstPayments = searchSaleOrderPayments(slsTblSoPayments);
//					try {
//						log.info("lstPayments.size()-----------------:"+lstPayments.size());
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						log.error("payment is null-------"+msg);
//						e.printStackTrace();
//					}
//					if (lstPayments != null && lstPayments.size() > 0) {
////								if(lstPayments.get(0).getTxtPaymentTerm() !=null)
////								if(lstPayments.get(0).getTxtPaymentTerm() !=null && !(lstPayments.get(0).getTxtPaymentTerm().equalsIgnoreCase("Adjustment")))
////									SavePaymentInSAP(lstPayments.get(0), order);
////								else
//
////						for( SlsTblSoPayments soPayment : lstPayments)
////						{
////							if(soPayment.getNumPaymentReceived() != null && soPayment.getNumPaymentReceived().doubleValue() > 0)
////							SavePaymentInSAP(soPayment, order);
////						}
//						SavePaymentInSAP(lstPayments.get(0), order);
//						log.info("after--------SavePaymentInSAP");
//					}

                }

            }

        }

        return msg;
//			Success
    }


    public SlsTblSoPayments getSOPaymentsByPK(int SOPaymentId) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            String query = "FROM SlsTblSoPayments where  serSoPaymentId=" + SOPaymentId;

            SlsTblSoPayments so = (SlsTblSoPayments) entityManager.createQuery(query).getSingleResult();

            entityManager.close();

            return so;

        } catch (NoResultException e) {
            log.error("serSaleOrderId not found");
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }


    public String updateSaleOrderPaymentNew(SlsTblSoPayments soPaymnets) {
        try {
            log.info("inside-----updateSaleOrderPaymentNew----:" + soPaymnets.getTxtSlipNo());
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            soPaymnets.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            soPaymnets.setDteModifieddate(commonService.getCurrentTimeStamp_new());
            entityManager.merge(soPaymnets);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }


    @Override
    public String addNewSaleOrderWithPaymentDocuments(SlsTblSaleOrder slsTblSaleOrder, List<SOPaymentDocument> paymentDocuments) {
        String msg = addNewSaleOrder(slsTblSaleOrder);
        for (SOPaymentDocument pd : paymentDocuments) {
            if (pd.getDocumentFile() != null) {
                pd.setSlsTblSaleOrder(slsTblSaleOrder);
                uploadPaymentDocumentWithSale(pd);
            }
        }
        return msg;
    }

    @Override
    public String uploadPaymentDocumentWithSale(SOPaymentDocument paymentDocument) {
        try {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();
            SlsTblSaleOrder slsTblSaleOrder = entityManager.find(SlsTblSaleOrder.class,
                    paymentDocument.getSlsTblSaleOrder().getSerSaleOrderId());
            if (slsTblSaleOrder == null) {
                return "Failure";
            }
            paymentDocument.setSlsTblSaleOrder(slsTblSaleOrder);

            paymentDocument.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
            paymentDocument.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
            paymentDocument.setDteCreateddate(commonService.getCurrentTimeStamp_new());
            CfgTblUser user = commonService.getCurrentUser(paymentDocument.getSerCreatedUserId());
            paymentDocument.setCreatedUserName(user.getTxtUserName());
            entityManager.persist(paymentDocument);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "Failure";
    }


    public String updateAuditDetail(AuditSlsTblSaleOrder soDetail) {

        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            //  SlsTblSoDetail existingDetail = entityManager.find(SlsTblSoDetail.class,soDetail.getSerSoDetailId());
            entityManager.persist(soDetail);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
            // System.out.println("Detail updated/saved successfully: " + soDetail.getSapLineItem());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Failure";
        }
    }



    @Override
    public List<AuditSlsTblSaleOrder> searchSaleOrderAudit(int SaleOrderId) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        String query = "";
        if (SaleOrderId > 0)
            query = "from AuditSlsTblSaleOrder ff  where ff.serSaleOrderId= "
                    + SaleOrderId + "ORDER BY ff.dteChangedDate DESC";

        else
            return null;

        log.info("Query is ---" + query.substring(0, query.length()));

        System.out.println("query ----:" + query.substring(0, query.length()));
        String subQuery = query.substring(0, query.length());
        List<AuditSlsTblSaleOrder> lstSOD = entityManager.createQuery(subQuery).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();
        return lstSOD;
    }


    private boolean isUserInRole(String roleName) {
        int currentUserId = commonService.getCurrentLoggedInUser();
        CfgTblUser cfgTblUser = commonService.getCurrentUser(currentUserId);

        if (cfgTblUser != null) {
            Hibernate.initialize(cfgTblUser.getCfgTblUserRoles());
            CfgTblRole userRole = cfgTblUser.getCfgTblRole();
            String txtRoleName = userRole.getTxtRoleName();
            if (txtRoleName.equals(roleName)) {
                return true;
            }
        }
       return false;
    }



    @Override
    @Transactional
    public String updateSaleOrderPayment(ServiceDTO serviceDTO) throws Exception {

        SlsTblSaleOrder slsTblSaleOrder = this.getSaleOrderByPK(Integer.parseInt(serviceDTO.getSaleOrderNo()));

        if (slsTblSaleOrder == null) {
            log.error("Sale Order not found for SaleOrderNo: {}", serviceDTO.getSaleOrderNo());
            return "Sale Order Not Found";
        }

        String msg = "Success";

        try {
            SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            slsTblSaleOrder.setTxtOrderapprovalDate(outPutFormat.format(new Date()));
        } catch (Exception e) {
            log.error("Error formatting order approval date: {}", e.getMessage(), e);
            return "Error formatting date";
        }

        List<SlsTblSoDetail> soDetailsList = slsTblSaleOrder.getSlsTblSoDetails();
        if (soDetailsList == null || soDetailsList.isEmpty()) {
            log.error("No Sale Order Details found for Sale Order: {}", serviceDTO.getSaleOrderNo());
            return "Error: No Sale Order Details Found";
        }



        Optional<SlsTblSoDetail> soDetailsOpt = soDetailsList.stream()
                .filter(detail -> detail.getSerSoDetailId() == Integer.parseInt(serviceDTO.getSaleOrderDetailId()))
                .findFirst();

        if (!soDetailsOpt.isPresent()) {
            log.error("No matching SlsTblSoDetail found for SaleOrderDetailId: {}", serviceDTO.getSaleOrderDetailId());
            return "Error: Sale Order Detail Not Found";
        }

        SlsTblSoDetail soDetailsListObj = soDetailsOpt.get();
        ZKFEINVOICESESWEBSERVICE zkfEInvoiceSesWebService = new ZKFEINVOICESESWEBSERVICE();

        // Ensure parent deal exists
        if (slsTblSaleOrder.getSlsTblDeal() != null) {
            zkfEInvoiceSesWebService.setEBELN(slsTblSaleOrder.getSlsTblDeal().getTxtDealNo());
        } else {
            log.error("SlsTblDeal is null for SaleOrder: {}", slsTblSaleOrder.getSerSaleOrderId());
            return "Error: SlsTblDeal is null";
        }

        zkfEInvoiceSesWebService.setEBELP(soDetailsListObj.getSapLineItem());

        // Handle null Posted Date
        Date postedDate = serviceDTO.getPostedDate();
        if (postedDate == null) {
            log.error("Posted date is null for Sale Order: {}", serviceDTO.getSaleOrderNo());
            return "Error: Posted Date is required";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(postedDate);
        zkfEInvoiceSesWebService.setDOCDATE(formattedDate);
        zkfEInvoiceSesWebService.setPOSTDATE(formattedDate);
        zkfEInvoiceSesWebService.setQUANTITY(soDetailsListObj.getNumQuantity());

        try {
            SOAPRequestResponseWrapper response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
            List<SlsTblSaleOrder> slsTblSaleOrderNew = soapClientService.readSOAPResponseSES(response, slsTblSaleOrder);

            if (!slsTblSaleOrderNew.isEmpty()) {
                SlsTblSaleOrder responseDeal = slsTblSaleOrderNew.get(0);
                soDetailsListObj.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                soDetailsListObj.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                updateSoDetail(soDetailsListObj);
                msg = updateSaleOrderSAPRepost(slsTblSaleOrder);
            } else {
                log.error("SOAP response did not return any updated Sale Order.");
                return "Error: No response from SOAP service";
            }
        } catch (Exception e) {
            log.error("SOAP request failed: {}", e.getMessage(), e);
            return "Error: SOAP request failed";
        }

        return msg;
    }


    /*public String updateSaleOrderPayment(ServiceDTO serviceDTO) throws Exception {

        SlsTblSaleOrder   slsTblSaleOrder =  this.getSaleOrderByPK(Integer.parseInt(serviceDTO.getSaleOrderNo()));
        String msg = "Success";
       if (msg.equalsIgnoreCase("Success")) {
       //     for (SlsTblSoPayments order : lst) {
                SimpleDateFormat outPutFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
               try {
                    slsTblSaleOrder.setTxtOrderapprovalDate(outPutFormat.format(new Date()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
           List<SlsTblSoDetail> soDetailsList = slsTblSaleOrder.getSlsTblSoDetails();
           SlsTblSoDetail soDetailsListObj = soDetailsList.stream()
                   .filter(detail -> detail.getSerSoDetailId() == Integer.parseInt(serviceDTO.getSaleOrderDetailId()))
                   .findFirst().get();


           if(soDetailsListObj!=null){
               ZKFEINVOICESESWEBSERVICE zkfEInvoiceSesWebService = new ZKFEINVOICESESWEBSERVICE();
               zkfEInvoiceSesWebService.setEBELN(slsTblSaleOrder.getSlsTblDeal().getTxtDealNo());  // From parent deal
               zkfEInvoiceSesWebService.setEBELP(soDetailsListObj.getSapLineItem());
               Date postedDate = serviceDTO.getPostedDate();
               SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
               String formattedDate = sdf.format(postedDate);
               zkfEInvoiceSesWebService.setDOCDATE(formattedDate);
               zkfEInvoiceSesWebService.setPOSTDATE(formattedDate);
               zkfEInvoiceSesWebService.setQUANTITY(soDetailsListObj.getNumQuantity());
               SOAPRequestResponseWrapper response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
               List<SlsTblSaleOrder> slsTblSaleOrderNew = soapClientService.readSOAPResponseSES(response, slsTblSaleOrder);

               if(slsTblSaleOrderNew.size() > 0){
                   SlsTblSaleOrder responseDeal = slsTblSaleOrderNew.get(0);
                   soDetailsListObj.setSerSoDetailId(soDetailsListObj.getSerSoDetailId());
                   soDetailsListObj.setTxtSoapReturnType(responseDeal.getTxtSoapReturnType());
                   soDetailsListObj.setTxtSoapResponseMsg(responseDeal.getTxtSoapResponseMsg());
                   updateSoDetail(soDetailsListObj);
                   msg = updateSaleOrderSAPRepost(slsTblSaleOrder);

               }

           }
       }
        return msg;
    }*/


    @Override
    public List<SlsTblSaleOrder> searchSaleOrderByDepartment(SlsTblSaleOrder SaleOrder) {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        StringBuilder query = new StringBuilder("from SlsTblSaleOrder SaleOrder where 1=1 ");

        // Sale Order No
        if (SaleOrder.getTxtSaleOrderNo() != null) {
            query.append(" and upper(SaleOrder.txtSaleOrderNo) like upper('")
                    .append(SaleOrder.getTxtSaleOrderNo()).append("%') ");
        }

        // Level
        if (SaleOrder.getNumLevel() != null && SaleOrder.getNumLevel() > 0) {
            query.append(" and SaleOrder.numLevel = ").append(SaleOrder.getNumLevel()).append(" ");
        }

        // Dealer
        if (SaleOrder.getTxtDealer() != null && SaleOrder.getTxtDealer().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblDealer.txtCustomerName) like upper('")
                    .append(SaleOrder.getTxtDealer()).append("') ");
        }

        // Customer
        if (SaleOrder.getTxtCustomer() != null && SaleOrder.getTxtCustomer().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblCustomer.txtCustomerName) like upper('")
                    .append(SaleOrder.getTxtCustomer()).append("') ");
        }

        // Product
        if (SaleOrder.getTxtProduct() != null && SaleOrder.getTxtProduct().trim().length() > 2) {
            query.append(" and upper(SaleOrder.cfgTblProduct.txtProductName) like upper('")
                    .append(SaleOrder.getTxtProduct()).append("') ");
        }

        // SAP No
        if (SaleOrder.getTxtSapNo() != null && SaleOrder.getTxtSapNo().trim().length() > 0) {
            query.append(" and upper(SaleOrder.txtSapNo) like upper('")
                    .append(SaleOrder.getTxtSapNo()).append("') ");
        }

        // User Group Filtering
        CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        if (user != null) {
            if (user.getTxtUserName().equalsIgnoreCase("umair")) {
                query.append(" and (SaleOrder.serGroupId = ").append(user.getSerGroupId())
                        .append(" or SaleOrder.cfgTblProduct.serProductId in (904,905,906,907,908,909)) ");
            } else if (user.getSerGroupId() != null && user.getSerGroupId() > 0) {
                query.append(" and SaleOrder.serGroupId = ").append(user.getSerGroupId()).append(" ");
            }
        }

       /* // Status Filtering
        if (SaleOrder.getTxtStatus5() != null && SaleOrder.getNumLevel() >= 5) {
            query.append(" and SaleOrder.txtStatus5 = 'APPROVED' ");
        }
*/
        // Complimentary Orders
        if (SaleOrder.getBlIsComplementry() != null && SaleOrder.getBlIsComplementry()) {
            query.append(" and SaleOrder.txtStatus = 'APPROVED' and SaleOrder.serCreatedUserId > 0 ");
        }

        // Issued Orders
        if (SaleOrder.getBlnIsIssued() != null && SaleOrder.getBlnIsIssued()) {
            query.append(" and SaleOrder.blnIsIssued = true ");
        }

        // Product ID Filtering
        if (SaleOrder.getCfgTblProduct() != null && SaleOrder.getCfgTblProduct().getSerProductId() != null
                && SaleOrder.getCfgTblProduct().getSerProductId() > 0) {
            query.append(" and SaleOrder.cfgTblProduct.serProductId = ")
                    .append(SaleOrder.getCfgTblProduct().getSerProductId()).append(" ");
        }

        // Sale Order ID Filtering
        if (SaleOrder.getSerSaleOrderId() != null) {
            query.append(" and SaleOrder.serSaleOrderId = ")
                    .append(SaleOrder.getSerSaleOrderId()).append(" ");
        }

        // Date Range Filtering
        if (SaleOrder.getDte_date_from() != null && SaleOrder.getDte_date_from().trim().length() > 0) {
            try {
                Date fromDate = DATE_FORMAT.parse(SaleOrder.getDte_date_from());
                query.append(" and SaleOrder.dteCreateddate >= '")
                        .append(DATE_FORMATDB.format(fromDate)).append("' ");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (SaleOrder.getDte_date_to() != null && SaleOrder.getDte_date_to().trim().length() > 0) {
            try {
                Date toDate = DATE_FORMAT.parse(SaleOrder.getDte_date_to());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(toDate);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);

                query.append(" and SaleOrder.dteCreateddate <= '")
                        .append(DATE_FORMATDB.format(calendar.getTime())).append("' ");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Customer Filtering based on User
        CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
        if (cfgTblUser.getCfgTblCustomer() != null) {
            if (cfgTblUser.getCfgTblCustomer().getBlIsDealer() != null && cfgTblUser.getCfgTblCustomer().getBlIsDealer()) {
                query.append(" and SaleOrder.cfgTblDealerOne.serCustomerId in (")
                        .append("select serCustomerId from CfgTblCustomer customer where customer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId())
                        .append(" or customer.cfgTblGroupCustomer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId())
                        .append(") ");
            } else {
                query.append(" and SaleOrder.cfgTblCustomer.serCustomerId = ")
                        .append(cfgTblUser.getCfgTblCustomer().getSerCustomerId()).append(" ");
            }
        }

        query.append(" order by SaleOrder.serSaleOrderId DESC");

        log.info("Query is ---" + query.toString());

        List<SlsTblSaleOrder> resultList = entityManager.createQuery(query.toString()).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return resultList;
    }



    @Override
    public String updateSaleOrderRemarks(List<SlsTblSaleOrder> saleOrders) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            log.info("Starting bulk update of sale order remarks.");

            int batchSize = 50;
            int count = 0;

            for (SlsTblSaleOrder slsTblSaleOrder : saleOrders) {
                log.info("Processing sale order: " + slsTblSaleOrder.getTxtSapInvoiceNo());

                String query = "FROM SlsTblSaleOrder ff WHERE ff.txtSapInvoiceNo = :txtSapInvoiceNo";
                SlsTblSaleOrder updateSaleOrder = (SlsTblSaleOrder) entityManager.createQuery(query)
                        .setParameter("txtSapInvoiceNo", slsTblSaleOrder.getTxtSapInvoiceNo())
                        .getSingleResult();

                if (slsTblSaleOrder.getNumLevel() == 3) {

                    if (slsTblSaleOrder.getTxtTaxRemarks() != null && slsTblSaleOrder.getTxtTaxRemarks().toLowerCase().contains("Note: Rejected") || slsTblSaleOrder.getTxtTaxRemarks().toLowerCase().contains("Note:Rejected")){
                        updateSaleOrder.setTxtStatus3("Pending");
                        updateSaleOrder.setTxtLevel("SECOND");
                        updateSaleOrder.setNumLevel(2);
                        updateSaleOrder.setTxtStatus2("Pending");
                    }else {
                        updateSaleOrder.setTxtTaxRemarks(slsTblSaleOrder.getTxtTaxRemarks());
                        updateSaleOrder.setTxtLevel("FOURTH");
                        updateSaleOrder.setNumLevel(4);
                        updateSaleOrder.setTxtStatus3("APPROVED");
                        updateSaleOrder.setSerApprovedbyId3(getUserIdIfRoleExists("TAX_HEAD").getSerUserId());
                        updateSaleOrder.setSerApprovedbyName3(getUserIdIfRoleExists("TAX_HEAD").getTxtUserName());
                    }
                } else {
                   /* if (slsTblSaleOrder.getTxtFinanceRemarks() != null && !slsTblSaleOrder.getTxtFinanceRemarks().trim().isEmpty()) {*/
                    if (slsTblSaleOrder.getTxtAuditRemarks() != null && slsTblSaleOrder.getTxtAuditRemarks().equals("Note:Reject") || slsTblSaleOrder.getTxtAuditRemarks().contains("Note: Reject")){
                        updateSaleOrder.setTxtLevel("FOURTH");
                        updateSaleOrder.setNumLevel(4);
                        updateSaleOrder.setTxtStatus4("Pending");
                        updateSaleOrder.setTxtStatus5("Pending");
                    } else {
                     updateSaleOrder.setTxtFinanceRemarks("SAP AUTO APPROVAL");
                     /* updateSaleOrder.setTxtLevel("FOURTH");
                     updateSaleOrder.setNumLevel(4);*/
                     updateSaleOrder.setTxtStatus4("APPROVED");
                     updateSaleOrder.setSerApprovedbyId4(getUserIdIfRoleExists("FINANCE_HEAD").getSerUserId());
                     updateSaleOrder.setSerApprovedbyName4(getUserIdIfRoleExists("FINANCE_HEAD").getTxtUserName());
                     updateSaleOrder.setTxtAuditRemarks(slsTblSaleOrder.getTxtAuditRemarks());
                    // updateSaleOrder.setTxtTaxRemarks(slsTblSaleOrder.getTxtTaxRemarks());
                     updateSaleOrder.setTxtLevel("SIXTH");
                     updateSaleOrder.setNumLevel(6);
                     updateSaleOrder.setTxtStatus5("APPROVED");
                     updateSaleOrder.setTxtStatus6("Pending");
                     updateSaleOrder.setSerApprovedbyId5(getUserIdIfRoleExists("AUDIT_HEAD").getSerUserId());
                     updateSaleOrder.setSerApprovedbyName5(getUserIdIfRoleExists("AUDIT_HEAD").getTxtUserName());
                    }
                }
                entityManager.merge(updateSaleOrder);
                count++;

                // Flush and clear the context every batchSize
                if (count % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }

                AuditSlsTblSaleOrder auditSlsTblSaleOrder = new AuditSlsTblSaleOrder();
                auditSlsTblSaleOrder.setSerSaleOrderId(updateSaleOrder.getSerSaleOrderId());
                /*auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtTaxRemarks());*/
                switch (slsTblSaleOrder.getNumLevel()) {
                    case 3:
                        if (slsTblSaleOrder.getTxtTaxRemarks() != null && slsTblSaleOrder.getTxtTaxRemarks().toLowerCase().contains("Note: Rejected") || slsTblSaleOrder.getTxtTaxRemarks().contains("Note:Rejected")){
                            auditSlsTblSaleOrder.setTxtDepartment("TAX");
                            /*  auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtStatus3());*/
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(3));
                            auditSlsTblSaleOrder.setTxtLevel("2");
                            auditSlsTblSaleOrder.setTxtNextLevel("3");
                            auditSlsTblSaleOrder.setTxtStatus("Send Back");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("TAX_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("TAX_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtTaxRemarks());
                        }else {
                            auditSlsTblSaleOrder.setTxtDepartment("TAX");
                            /*  auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtStatus3());*/
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(4));
                            auditSlsTblSaleOrder.setTxtLevel("3");
                            auditSlsTblSaleOrder.setTxtNextLevel("4");
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("TAX_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("TAX_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtTaxRemarks());
                        }
                        break;
                    case 5:
                        if (slsTblSaleOrder.getTxtAuditRemarks() != null && slsTblSaleOrder.getTxtAuditRemarks().equals("Note:Reject") || slsTblSaleOrder.getTxtAuditRemarks().contains("Note: Reject")){
                            auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                            /*  auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtStatus3());*/
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(3));
                            auditSlsTblSaleOrder.setTxtLevel("4");
                            auditSlsTblSaleOrder.setTxtNextLevel("5");
                            auditSlsTblSaleOrder.setTxtStatus("Send Back");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("AUDIT_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("AUDIT_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtAuditRemarks());
                        }else {
                            auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(6));
                            auditSlsTblSaleOrder.setTxtLevel("5");
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("AUDIT_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("AUDIT_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtAuditRemarks());
                            break;
                        }
                    case 4:
                        if (slsTblSaleOrder.getTxtAuditRemarks() != null && slsTblSaleOrder.getTxtAuditRemarks().equals("Note:Reject") || slsTblSaleOrder.getTxtAuditRemarks().contains("Note: Reject")){
                            auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                            /*  auditSlsTblSaleOrder.setTxtStatus(slsTblSaleOrder.getTxtStatus3());*/
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                           /* auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(3));*/
                            auditSlsTblSaleOrder.setTxtLevel("4");
                            auditSlsTblSaleOrder.setTxtNextLevel("5");
                            auditSlsTblSaleOrder.setTxtStatus("Send Back");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("AUDIT_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("AUDIT_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtAuditRemarks());
                            break;
                        }/*else {
                            auditSlsTblSaleOrder.setTxtDepartment("AUDIT");
                            auditSlsTblSaleOrder.setTxtLevel(String.valueOf(slsTblSaleOrder.getNumLevel()));
                            auditSlsTblSaleOrder.setTxtNextLevel(String.valueOf(6));
                            auditSlsTblSaleOrder.setTxtLevel("5");
                            auditSlsTblSaleOrder.setTxtStatus("APPROVED");
                            auditSlsTblSaleOrder.setSerApprovedbyId(getUserIdIfRoleExists("AUDIT_HEAD").getSerUserId());
                            auditSlsTblSaleOrder.setApprovedNAME(getUserIdIfRoleExists("AUDIT_HEAD").getTxtUserName());
                            auditSlsTblSaleOrder.setDteChangedDate(commonService.getCurrentTimeStamp_new());
                            auditSlsTblSaleOrder.setTxtRemarks(slsTblSaleOrder.getTxtAuditRemarks());
                            break;
                        }    */
                    default:
                        auditSlsTblSaleOrder.setTxtDepartment("Unknown Department");
                        break;
                }
                updateAuditDetail(auditSlsTblSaleOrder);
            }

            entityManager.flush();
            entityManager.clear();

            entityManager.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            log.error("Error during bulk update: ", e);
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return "Failure";
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }


    private CfgTblUser getUserIdIfRoleExists(String roleName) {
        List<CfgTblUser> users = userService.getActiveUser();

        for (CfgTblUser user : users) {
            Hibernate.initialize(user.getCfgTblUserRoles());

            CfgTblRole userRole = user.getCfgTblRole();
            if (userRole != null) {
                String txtRoleName = userRole.getTxtRoleName();
                if (txtRoleName.equals(roleName)) {
                    return user;
                }
            }
        }

        return null; // Return null if no user with the specified role is found
    }


    /*private String getUserNameIfRoleExists(String roleName) {
        List<CfgTblUser> users = userService.getActiveUser(); // Assuming this method returns a list of all users

        for (CfgTblUser user : users) {
            // Initialize user roles to ensure they're loaded
            Hibernate.initialize(user.getCfgTblUserRoles());

            // Check if the user has a role
            CfgTblRole userRole = user.getCfgTblRole();
            if (userRole != null) {
                String txtRoleName = userRole.getTxtRoleName();
                if (txtRoleName.equals(roleName)) {
                    return user.getTxtUserName(); // Assuming there's a method to get the user ID
                }
            }
        }

        return null; // Return null if no user with the specified role is found
    }*/
/*


    public String addNewDeals(List<SlsTblDeal> deals) {
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


    /*if (isUserInRole("TAX_HEAD")) {
        order.setTxtLevel("FOURTH");
        order.setNumLevel(4);
        auditSlsTblSaleOrder.setTxtLevel("3");
        auditSlsTblSaleOrder.setTxtNextLevel("4");
        order.setTxtStatus3("APPROVED");
        auditSlsTblSaleOrder.setTxtRemarks(dto.getTxtTaxRemarks());
        auditSlsTblSaleOrder.setTxtStatus("APPROVED");
        if ("Send Back".equals(order.getTxtStatus4()) ||
                order.getTxtStatus4() == null ||
                order.getTxtStatus4().isEmpty()) {

            order.setTxtStatus4("Pending");
        }

    }*/
}