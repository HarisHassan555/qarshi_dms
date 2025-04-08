package com.bezkoder.spring.login.sa.dal.entities;

import com.bezkoder.spring.login.sa.bll.dto.SaleOrderStatusDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Repository
public interface SlsTblSaleOrderRepository extends JpaRepository<SlsTblSaleOrder, Integer> {

    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'APPROVED' " +
            "OR s.txtStatus1 = 'APPROVED' " +
            "OR s.txtStatus2 = 'APPROVED' " +
            "OR s.txtStatus3 = 'APPROVED' " +
            "OR s.txtStatus4 = 'APPROVED' " +
            "OR s.txtStatus5 = 'APPROVED' " +
            "OR s.txtStatus6 = 'APPROVED')")
    long countApproved(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    /*@Query("SELECT COUNT(s) FROM SlsTblSaleOrder s WHERE s.txtStatus = 'APPROVED' AND s.blIsDeleted = FALSE AND s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate")
    long countApproved(@Param("startDate") Date startDate, @Param("endDate") Date endDate);*/

    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'APPROVED' " +
            "OR s.txtStatus2 = 'APPROVED' " +
            "OR s.txtStatus3 = 'APPROVED' " +
            "OR s.txtStatus4 = 'APPROVED' " +
            "OR s.txtStatus5 = 'APPROVED' " +
            "OR s.txtStatus6 = 'APPROVED')")
    long countCompleted(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'cancel' " +
            "OR s.txtStatus1 = 'cancel' " +
            "OR s.txtStatus2 = 'cancel' " +
            "OR s.txtStatus3 = 'cancel' " +
            "OR s.txtStatus4 = 'cancel' " +
            "OR s.txtStatus5 = 'cancel' " +
            "OR s.txtStatus6 = 'cancel')")
    long countCancelled(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'Hold' " +
            "OR s.txtStatus1 = 'Hold' " +
            "OR s.txtStatus2 = 'Hold' " +
            "OR s.txtStatus3 = 'Hold' " +
            "OR s.txtStatus4 = 'Hold' " +
            "OR s.txtStatus5 = 'Hold' " +
            "OR s.txtStatus6 = 'Hold')")
    long countOnHold(@Param("startDate") Date startDate, @Param("endDate") Date endDate);


    @Query("SELECT new com.bezkoder.spring.login.sa.bll.dto.SaleOrderStatusDTO(s, "
            + "CASE WHEN SUM(sd.numBalance) > 0 THEN 'In Process' ELSE 'Completed' END) "
            + "FROM SlsTblSaleOrder s "
            + "LEFT JOIN SlsTblSoDetail sd ON sd.slsTblSaleOrder.serSaleOrderId = s.serSaleOrderId "
            + "WHERE s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate "
            + "GROUP BY s "
            + "ORDER BY s.dteCreateddate DESC")
    List<SaleOrderStatusDTO> findTop6Transactions(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'Pending' " +
            "OR s.txtStatus = 'InProgress' " +
            "OR s.txtStatus1 = 'Pending' " +
            "OR s.txtStatus1 = 'InProgress' " +
            "OR s.txtStatus2 = 'Pending' " +
            "OR s.txtStatus2 = 'InProgress' " +
            "OR s.txtStatus3 = 'Pending' " +
            "OR s.txtStatus3 = 'InProgress' " +
            "OR s.txtStatus4 = 'Pending' " +
            "OR s.txtStatus4 = 'InProgress' " +
            "OR s.txtStatus5 = 'Pending' " +
            "OR s.txtStatus5 = 'InProgress' " +
            "OR s.txtStatus6 = 'Pending' " +
            "OR s.txtStatus6 = 'InProgress')")
    long countPending(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // total sales amount
    @Query("SELECT SUM(s.numAmount) FROM SlsTblSaleOrder s WHERE s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate AND s.blIsDeleted = FALSE")
    BigDecimal getTotalAmountInPeriod(@Param("startDate") Date startDate, @Param("endDate") Date endDate);


    @Query("SELECT COALESCE(s.cfgTblProduct.txtProductName, 'Others'), COUNT(s) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE AND s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate " +
            "GROUP BY s.cfgTblProduct.txtProductName ORDER BY COUNT(s) DESC")
    List<Object[]> getSalesCountByProduct(@Param("startDate") Date startDate, @Param("endDate") Date endDate);


    @Query("SELECT COALESCE(CASE " +
            "WHEN s.txtStatus = 'APPROVED' THEN 'Approved' " +
            "WHEN s.txtStatus = 'cancel' THEN 'Cancelled' " +
            "WHEN s.txtStatus = 'Hold' THEN 'On Hold' " +
            "WHEN s.txtStatus = 'Pending' THEN 'Pending' " +
            "WHEN s.txtStatus = 'InProgress' THEN 'In Progress' " +
            "WHEN s.txtStatus = 'SomeOtherStatus' THEN 'Some Other Status' " +
            "ELSE 'Others' END, 'Others'), COUNT(s) " +
            "FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate " +
            "GROUP BY COALESCE(CASE " +
            "WHEN s.txtStatus = 'APPROVED' THEN 'Approved' " +
            "WHEN s.txtStatus = 'cancel' THEN 'Cancelled' " +
            "WHEN s.txtStatus = 'Hold' THEN 'On Hold' " +
            "WHEN s.txtStatus = 'Pending' THEN 'Pending' " +
            "WHEN s.txtStatus = 'InProgress' THEN 'In Progress' " +
            "WHEN s.txtStatus = 'SomeOtherStatus' THEN 'Some Other Status' " +
            "ELSE 'Others' END, 'Others')")
    List<Object[]> getSalesStatusDistribution(@Param("startDate") Date startDate, @Param("endDate") Date endDate);


    @Query("SELECT AVG(s.numAmount) FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE AND s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate")
    BigDecimal getAverageOrderValue(@Param("startDate") Date startDate, @Param("endDate") Date endDate);



    @Query("SELECT COUNT(s) FROM SlsTblSaleOrder s WHERE s.dteCreateddate >= :startDate AND s.dteCreateddate < :endDate")
    long countTotalSales(@Param("startDate") Date startDate, @Param("endDate") Date endDate);


   /* @Query("SELECT s FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus IN :statuses " +
            "OR s.txtStatus1 IN :statuses " +
            "OR s.txtStatus2 IN :statuses " +
            "OR s.txtStatus3 IN :statuses " +
            "OR s.txtStatus4 IN :statuses " +
            "OR s.txtStatus5 IN :statuses " +
            "OR s.txtStatus6 IN :statuses)")
    List<SlsTblSaleOrder> findByStatus(@Param("startDate") Date startDate,
                                       @Param("endDate") Date endDate,
                                       @Param("statuses") List<String> statuses);*/


    @Query("SELECT s FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'Pending' " +
            "OR s.txtStatus = 'InProgress' " +
            "OR s.txtStatus1 = 'Pending' " +
            "OR s.txtStatus1 = 'InProgress' " +
            "OR s.txtStatus2 = 'Pending' " +
            "OR s.txtStatus2 = 'InProgress' " +
            "OR s.txtStatus3 = 'Pending' " +
            "OR s.txtStatus3 = 'InProgress' " +
            "OR s.txtStatus4 = 'Pending' " +
            "OR s.txtStatus4 = 'InProgress' " +
            "OR s.txtStatus5 = 'Pending' " +
            "OR s.txtStatus5 = 'InProgress' " +
            "OR s.txtStatus6 = 'Pending' " +
            "OR s.txtStatus6 = 'InProgress')")
    List<SlsTblSaleOrder> findPendingOrders(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);


    @Query("SELECT s FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'Hold' " +
            "OR s.txtStatus1 = 'Hold' " +
            "OR s.txtStatus2 = 'Hold' " +
            "OR s.txtStatus3 = 'Hold' " +
            "OR s.txtStatus4 = 'Hold' " +
            "OR s.txtStatus5 = 'Hold' " +
            "OR s.txtStatus6 = 'Hold')")
    List<SlsTblSaleOrder> findOnHoldTransactions(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);


    @Query("SELECT s FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'APPROVED' " +
            "OR s.txtStatus2 = 'APPROVED' " +
            "OR s.txtStatus3 = 'APPROVED' " +
            "OR s.txtStatus4 = 'APPROVED' " +
            "OR s.txtStatus5 = 'APPROVED' " +
            "OR s.txtStatus6 = 'APPROVED')")
    List<SlsTblSaleOrder> findApprovedTransactions(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);


    @Query("SELECT s FROM SlsTblSaleOrder s " +
            "WHERE s.blIsDeleted = FALSE " +
            "AND s.dteCreateddate >= :startDate " +
            "AND s.dteCreateddate < :endDate " +
            "AND (s.txtStatus = 'cancel' " +
            "OR s.txtStatus1 = 'cancel' " +
            "OR s.txtStatus2 = 'cancel' " +
            "OR s.txtStatus3 = 'cancel' " +
            "OR s.txtStatus4 = 'cancel' " +
            "OR s.txtStatus5 = 'cancel' " +
            "OR s.txtStatus6 = 'cancel')")
    List<SlsTblSaleOrder> fetchCancelledTransaction(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}