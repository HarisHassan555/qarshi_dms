
package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.SoapClientService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.UserService;
import com.bezkoder.spring.login.sa.bll.dto.SOAPRequestResponseWrapper;
import com.bezkoder.spring.login.sa.bll.servicesimpl.ScheduledTask;
import com.bezkoder.spring.login.sa.dal.entities.AuditSlsTblSes;
import com.bezkoder.spring.login.sa.dal.entities.ScheduledTaskExecutionLog;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.workflow.Ziwfr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@CrossOrigin( origins = "*" )
public class SoapController {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private SoapClientService soapClientService;

    @Autowired
    private UserService userService;


    @Autowired
    private ICommonService commonService;

    @Autowired
    private ScheduledTask scheduledTask;

    private static final Logger logger = LoggerFactory.getLogger(SoapController.class);
    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /*@PostMapping("/sendRequest")
    public String sendSoapRequest() {
        try {
            SOAPMessage response = soapClientService.sendSOAPRequest("2021-01-01");
            soapClientService.readSOAPResponse(response);
            return "Success";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failure";
        }
    }*/


    @GetMapping("/allLogs")
    public List<ScheduledTaskExecutionLog> getAllExecutionLog(HttpServletRequest request, HttpServletResponse response) {
        List<ScheduledTaskExecutionLog> scheduledTaskExecutionLogList = new ArrayList<>();
        try {

            scheduledTaskExecutionLogList = scheduledTask.getAllExecutionLogs();
             return scheduledTaskExecutionLogList;

        } catch (Exception e) {
            e.printStackTrace();
            return scheduledTaskExecutionLogList;
        }
    }


    @RequestMapping(value = "/sendPost", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String executeTaskAtSpecificTime(@RequestBody Map<String, String> request) {

        ScheduledTaskExecutionLog scheduledTaskExecutionLog = new ScheduledTaskExecutionLog();
        try {
            LocalDateTime now = LocalDateTime.now();

            // Extract the date from the request map
            String erDate = request.get("erDate");
            SOAPRequestResponseWrapper soapResponse = soapClientService.sendSOAPRequest(erDate);
            soapClientService.readSOAPResponse(soapResponse);

            // Log success
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setOutputDate(erDate);
            scheduledTaskExecutionLog.setStatus("Scheduled task executed successfully");
            scheduledTaskExecutionLog.setErrorMessage(null);
            saveExecutionTaskLog(scheduledTaskExecutionLog);

            return "success";
        } catch (Exception e) {
            // Log failure
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setStatus("Failure");
            scheduledTaskExecutionLog.setErrorMessage(e.getMessage());
            scheduledTaskExecutionLog.setOutputDate(null);
            saveExecutionTaskLog(scheduledTaskExecutionLog);
            return "failure";
        }
    }



    public String saveExecutionTaskLog(ScheduledTaskExecutionLog scheduledTaskExecutionLog) {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(scheduledTaskExecutionLog);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
          //  logger.error(e.getMessage(), e);
            return "Failure";
        }
    }


    public List<Ziwfr> getAllProcurement() {
        EntityManager entityManager = getEntityManager();
        List<SlsTblSaleOrder> slsTblSaleOrderArrayList = new ArrayList<>();
        try {
            logger.info("Fetching all procurement records with specified statuses");
            slsTblSaleOrderArrayList = entityManager.createQuery(
                             "SELECT saleOrder FROM SlsTblSaleOrder saleOrder " +
                                    "WHERE saleOrder.numLevel  IN (:txtLevels) " +
                                    "AND saleOrder.txtStatus2 = :txtStatus2 " +
                                    "AND (saleOrder.txtStatus3 = :txtStatusPending OR saleOrder.txtStatus3 = :txtStatusApproved) " +
                                    "AND (saleOrder.txtStatus4 = :txtStatusPending " +
                                    "OR saleOrder.txtStatus4 = :txtStatusApproved " +
                                    "OR saleOrder.txtStatus4 IS NULL " +
                                    "OR saleOrder.txtStatus4 = '') " +
                                    "AND saleOrder.txtSapInvoiceNo IS NOT NULL " +
                                    "AND (saleOrder.txtStatus5 = :txtStatusPending " +
                                    "OR saleOrder.txtStatus5 IS NULL " +
                                    "OR saleOrder.txtStatus5 = '')"
                    )
                    .setParameter("txtLevels", Arrays.asList(3, 4, 5))
                    .setParameter("txtStatus2", "APPROVED")
                    .setParameter("txtStatusPending", "Pending")
                    .setParameter("txtStatusApproved", "APPROVED")
                    .getResultList();

        } catch (Exception e) {
            logger.error("Error fetching procurement records: {}", e.getMessage(), e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }

        // Prepare the list of Ziwfr objects to return

        List<Ziwfr> ZkfInvoiceWorkflowRemarksList = new ArrayList<>();
        for (SlsTblSaleOrder order : slsTblSaleOrderArrayList) {
            Ziwfr remarksList = new Ziwfr();
            remarksList.setBelnr(order.getTxtSapInvoiceNo());  // Assuming txtSapInvoiceNo is a valid field
            ZkfInvoiceWorkflowRemarksList.add(remarksList);
        }

        return ZkfInvoiceWorkflowRemarksList;
    }
    /*public List<Ziwfr> getAllProcurement() {
        EntityManager entityManager = getEntityManager();
        List<SlsTblSaleOrder> slsTblSaleOrderArrayList = new ArrayList<>();
        try {
            logger.info("Fetching all scheduled task execution logs");
            *//* slsTblSaleOrderArrayList = entityManager.createQuery("SELECT log FROM SlsTblSaleOrder log").getResultList();*//*
            slsTblSaleOrderArrayList = entityManager.createQuery(
                            "SELECT order FROM SlsTblSaleOrder order WHERE order.numLevel = :txtLevel AND order.txtStatus2 = :txtStatus2 AND order.txtStatus3 = :textStatus3"
                    )
                    .setParameter("txtLevel", 2)
                    .setParameter("txtStatus2", "APPROVED")
                    .setParameter("textStatus3", "Pending")
                    .getResultList();
        } catch (Exception e) {
            logger.error("Error fetching logs: {}", e.getMessage(), e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
        Ziwfr remarksList = new Ziwfr();
        List<Ziwfr> ZkfInvoiceWorkflowRemarksList = new ArrayList<>();
        for (SlsTblSaleOrder order : slsTblSaleOrderArrayList) {
            remarksList.setBelnr(order.getTxtSapInvoiceNo());
            ZkfInvoiceWorkflowRemarksList.add(remarksList);
        }
        return ZkfInvoiceWorkflowRemarksList;
    }*/


    @RequestMapping(value="/workFlowSap",method= RequestMethod.GET, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String  workFlowSap() {

        try {
            List<Ziwfr> ziwfrList = getAllProcurement();
            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);
            for (Ziwfr ziwfr : ziwfrList) {
                try {
                    logger.info("Sending SOAP request for item: {}", ziwfr);
                    SOAPRequestResponseWrapper soapResponse = soapClientService.sendSOAPRequestRemarks(Collections.singletonList(ziwfr));
                    soapClientService.readSOAPResponseRemarks(soapResponse,ziwfr);
                    logger.info("SOAP request for item {} executed successfully.", ziwfr);
                } catch (Exception e) {
                    logger.error("Error occurred while processing item: {} at: {}", ziwfr, LocalDateTime.now(), e);

                }
            }

            logger.info("Scheduled task executed successfully at: {}", now);

            // Format and return success message
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            return "Success";

        } catch (Exception e) {
            logger.error("Error occurred while executing scheduled task at: {}", LocalDateTime.now(), e);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            return "Failure";
        }
        /*try {
            List<Ziwfr> ziwfrList =  getAllProcurement();
            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);
            SOAPMessage soapResponse = soapClientService.sendSOAPRequestRemarks(ziwfrList);
            soapClientService.readSOAPResponseRemarks(soapResponse);
            logger.info("Scheduled task executed successfully at: {}", now);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            return "Success";
        } catch (Exception e) {
            logger.error("Error occurred while executing scheduled task at: {}", LocalDateTime.now(), e);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            *//*scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setStatus("Failure");
            scheduledTaskExecutionLog.setErrorMessage(e.getMessage());
            scheduledTaskExecutionLog.setOutputDate(null);*//*

        }*/
      //  return "Failure";
        /* saveExecutionTaskLog(scheduledTaskExecutionLog);*/
    }

    @RequestMapping(value="/sesLog", method=RequestMethod.GET, headers="Accept=application/json", consumes=MediaType.APPLICATION_JSON_VALUE)
    public List<AuditSlsTblSes> getRequestResponseLog() {
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();

            String query = "FROM AuditSlsTblSes";
            List<AuditSlsTblSes> auditSlsTblSes = entityManager.createQuery(query).getResultList();

            // Commit transaction if everything goes well
            entityManager.getTransaction().commit();

            return auditSlsTblSes;
        } catch (NoResultException e) {
            logger.error("Log not found");
            return null;
        } catch (Exception e) {
            // Rollback transaction in case of an error
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            logger.error(e.getMessage(), e);
            return null;
        } finally {
            // Ensure the EntityManager is always closed
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }


}
    /*@GetMapping("/sendPost")
    public String sendPostRequest() {
        try {
                ZkfEInvoiceSesWebService zkfEInvoiceSesWebService = new ZkfEInvoiceSesWebService();
                zkfEInvoiceSesWebService.setEbeln("4400021469");
                zkfEInvoiceSesWebService.setEbelp("00010");
                zkfEInvoiceSesWebService.setDocDate("2024-09-05");
                zkfEInvoiceSesWebService.setPostDate("2024-09-05");
                zkfEInvoiceSesWebService.setQuantity(new BigDecimal(1));
                SOAPMessage response = soapClientService.sendSOAPRequestSES(zkfEInvoiceSesWebService);
               // soapClientService.readSOAPResponseSES(response);
            //  EmailService emailService = new EmailService();
            //  CfgTblUser user = userService.getAdminOfCurrentUserRole(commonService.getCurrentLoggedInUser());

*//*List<String> recipients = Arrays.asList(user.getTxtAddress());

            emailService.sendEmail(
                    recipients,
                    "Test Subject",
                    "This is a test email."
            );*//**//*

            return "SOAP Request Sent and Response Received!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

}


*/