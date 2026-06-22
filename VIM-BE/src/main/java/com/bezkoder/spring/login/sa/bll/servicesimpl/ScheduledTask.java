package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.SoapClientService;
import com.bezkoder.spring.login.sa.bll.dto.SOAPRequestResponseWrapper;
import com.bezkoder.spring.login.sa.dal.entities.ScheduledTaskExecutionLog;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.workflow.Ziwfr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.annotation.PreDestroy;
import javax.persistence.*;;
import javax.xml.soap.SOAPMessage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


@Component
@CrossOrigin( origins = "*" )
public class ScheduledTask {


    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private ScheduledFuture<?> futureTask;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
    private final SoapClientService soapClientService;

    public ScheduledTask(SoapClientService soapClientService) {
        this.soapClientService = soapClientService;
    }



    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }


    // SAP service-order import — scheduler disabled (manual trigger via SoapController /sendPost still available)
    public void executeTaskAtSpecificTime() {

        ScheduledTaskExecutionLog scheduledTaskExecutionLog = new ScheduledTaskExecutionLog();

        try {

            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate date = LocalDate.parse(now.format(inputFormatter), inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String outputDate = date.format(outputFormatter);
            SOAPRequestResponseWrapper soapResponse = soapClientService.sendSOAPRequest(outputDate);
            soapClientService.readSOAPResponse(soapResponse);
            logger.info("Scheduled task executed successfully at: {}", now);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setOutputDate(outputDate);
            scheduledTaskExecutionLog.setStatus("Scheduled task executed successfully");
            scheduledTaskExecutionLog.setErrorMessage(null);

        } catch (Exception e) {
            logger.error("Error occurred while executing scheduled task at: {}", LocalDateTime.now(), e);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setStatus("Failure");
            scheduledTaskExecutionLog.setErrorMessage(e.getMessage());
            scheduledTaskExecutionLog.setOutputDate(null);

        }

        saveExecutionTaskLog(scheduledTaskExecutionLog);


        // Gracefully shutdown the task after execution
        cleanupTask();
    }

    private void cleanupTask() {
        if (futureTask != null && !futureTask.isCancelled()) {
            futureTask.cancel(true);
            logger.info("Scheduled task was canceled gracefully.");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (taskScheduler != null) {
            taskScheduler.shutdown();
            logger.info("TaskScheduler gracefully shut down.");
        }
    }

    public String saveExecutionTaskLog(ScheduledTaskExecutionLog scheduledTaskExecutionLog) {
        try {
            logger.info("Schedule log");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        EntityManager entityManager = getEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(scheduledTaskExecutionLog);
            entityManager.getTransaction().commit();
            entityManager.close();
            return "Success";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "Failure";
        }
    }


    public List<ScheduledTaskExecutionLog> getAllExecutionLogs() {
        EntityManager entityManager = getEntityManager();
        List<ScheduledTaskExecutionLog> logs = new ArrayList<>();
        try {
            logger.info("Fetching all scheduled task execution logs");
            logs = entityManager.createQuery("SELECT log FROM ScheduledTaskExecutionLog log").getResultList();
        } catch (Exception e) {
            logger.error("Error fetching logs: {}", e.getMessage(), e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return logs;
    }


    public List<Ziwfr> getAllProcurement() {
        EntityManager entityManager = getEntityManager();
        List<SlsTblSaleOrder> slsTblSaleOrderArrayList = new ArrayList<>();
        try {
            logger.info("Fetching all scheduled task execution logs");
           /* slsTblSaleOrderArrayList = entityManager.createQuery("SELECT log FROM SlsTblSaleOrder log").getResultList();*/
            slsTblSaleOrderArrayList = entityManager.createQuery(
                            "SELECT order FROM SlsTblSaleOrder order WHERE order.numLevel = :txtLevel AND order.txtStatus2 = :textStatus AND order.txtStatus3 = :textStatus3"
                    )
                    .setParameter("txtLevel", 2)
                    .setParameter("textStatus", "Approved")
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
    }


    // SAP workflow remarks — scheduler disabled
    public String executeWorkFlowTaskAtSpecificTime() {


        try {
            List<Ziwfr> ziwfrList = getAllProcurement();
            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);

            // Loop through each Ziwfr item and send SOAP requests individually
            for (Ziwfr ziwfr : ziwfrList) {
                try {
                    logger.info("Sending SOAP request for item: {}", ziwfr);
                    SOAPRequestResponseWrapper soapResponse = soapClientService.sendSOAPRequestRemarks(Collections.singletonList(ziwfr));
                    soapClientService.readSOAPResponseRemarks(soapResponse,ziwfr);
                    logger.info("SOAP request for item {} executed successfully.", ziwfr);
                } catch (Exception e) {
                    logger.error("Error occurred while processing item: {} at: {}", ziwfr, LocalDateTime.now(), e);
                    // Optionally log error per item, or continue to next item
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

       /* try {
            List<Ziwfr> ziwfrList =  getAllProcurement();
            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);
            SOAPMessage soapResponse = soapClientService.sendSOAPRequestRemarks(ziwfrList);
            soapClientService.readSOAPResponseRemarks(soapResponse);
            logger.info("Scheduled task executed successfully at: {}", now);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
           *//* scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setOutputDate(outputDate);
            scheduledTaskExecutionLog.setStatus("Scheduled task executed successfully");
            scheduledTaskExecutionLog.setErrorMessage(null);*//*

        } catch (Exception e) {
            logger.error("Error occurred while executing scheduled task at: {}", LocalDateTime.now(), e);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            *//*scheduledTaskExecutionLog.setExecutionTime(formattedDateTime);
            scheduledTaskExecutionLog.setStatus("Failure");
            scheduledTaskExecutionLog.setErrorMessage(e.getMessage());
            scheduledTaskExecutionLog.setOutputDate(null);*//*

        }*/

       /* saveExecutionTaskLog(scheduledTaskExecutionLog);*/
    }




    /*@Scheduled(fixedRate = 1800000) // 1800000 ms = 30 minutes
    public void executeTaskEveryHalfHour() {
        try {

            LocalDateTime now = LocalDateTime.now();
            logger.info("Starting scheduled task at: {}", now);

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate date = LocalDate.parse(now.format(inputFormatter), inputFormatter);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String outputDate = date.format(outputFormatter);

            SOAPMessage soapResponse = soapClientService.sendSOAPRequest(outputDate);

            soapClientService.readSOAPResponse(soapResponse);

            logger.info("Scheduled task executed successfully at: {}", now);

        } catch (Exception e) {

            logger.error("Error occurred while executing scheduled task at: {}", LocalDateTime.now(), e);
        }
    }*/
}
