package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.dto.DashboardResponse;
import com.bezkoder.spring.login.sa.bll.dto.SaleOrderStatusDTO;
import com.bezkoder.spring.login.sa.bll.servicesimpl.DashboardService;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@CrossOrigin( origins = "*" )
@RestController
//@RequestMapping("/api/auth")
public class AdminDashBoardController {

    @Autowired
    private DashboardService dashboardService;


    @GetMapping("/admin-dashboard")
    public Map<String, Object> getDashboardData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {

        // Call the service method to get the dashboard data
        return dashboardService.getDashboardData(startDate, endDate);
    }


    @GetMapping("/transaction-details")
    public List<SlsTblSaleOrder>getTransactionalDetailData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        return dashboardService.getPendingSaleOrders(startDate, endDate);
    }


    @GetMapping("/hold-transaction")
    public List<SlsTblSaleOrder>getHoldTransactionData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        return dashboardService.getHoldSaleOrders(startDate, endDate);
    }

    @GetMapping("/approved-transaction")
    public List<SlsTblSaleOrder>getApprovedTransactionData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        return dashboardService.getApprovedSaleOrders(startDate, endDate);
    }

    @GetMapping("/cancel-transaction")
    public List<SlsTblSaleOrder>getCancelTransactionData(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {
        return dashboardService.getCancelledSaleOrders(startDate, endDate);
    }

    @GetMapping("/user-dashboard")
    public DashboardResponse getDashboardDataByUser(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate) {

        // Calculate previous week's dates
        Calendar calendar = Calendar.getInstance();

        // For previous start date
        calendar.setTime(startDate);
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date previousStartDate = calendar.getTime();

        // For previous end date
        calendar.setTime(endDate);
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date previousEndDate = calendar.getTime();

        // Fetch counts for the current and previous week
        long approvedCount = dashboardService.getApprovedCount(startDate, endDate);
        long previousApprovedCount = dashboardService.getApprovedCount(previousStartDate, previousEndDate);

        long cancelledCount = dashboardService.getCancelledCount(startDate, endDate);
        long previousCancelledCount = dashboardService.getCancelledCount(previousStartDate, previousEndDate);

        long onHoldCount = dashboardService.getOnHoldCount(startDate, endDate);
        long previousOnHoldCount = dashboardService.getOnHoldCount(previousStartDate, previousEndDate);

        long completedCount = dashboardService.getCompletedCount(startDate, endDate);
        long previousCompletedCount = dashboardService.getCompletedCount(previousStartDate, previousEndDate);

        long pendingCount = dashboardService.getPendingCount(startDate, endDate);
        long previousPendingCount = dashboardService.getPendingCount(previousStartDate, previousEndDate);

        // Calculate transaction difference
        BigDecimal transactionDifference = dashboardService.calculateTransactionDifference(startDate, endDate);

        // Calculate percentage differences
        BigDecimal percentageDifferenceApproved = calculatePercentageDifference(approvedCount, previousApprovedCount);
        BigDecimal percentageDifferenceCancelled = calculatePercentageDifference(cancelledCount, previousCancelledCount);
        BigDecimal percentageDifferenceOnHold = calculatePercentageDifference(onHoldCount, previousOnHoldCount);
        BigDecimal percentageDifferenceCompleted = calculatePercentageDifference(completedCount, previousCompletedCount);
        BigDecimal percentageDifferencePending = calculatePercentageDifference(pendingCount, previousPendingCount);

        // Fetch last six transactions
        List<SaleOrderStatusDTO> lastSixTransactions = dashboardService.getLastSixTransactions(startDate, endDate);

        // Construct and return the DashboardResponse
        return new DashboardResponse(
                approvedCount,
                cancelledCount,
                onHoldCount,
                completedCount,
                transactionDifference,
                lastSixTransactions,
                percentageDifferenceApproved,
                percentageDifferenceCancelled,
                percentageDifferenceOnHold,
                percentageDifferenceCompleted,
                pendingCount,
                percentageDifferencePending,
                previousApprovedCount,
                previousCancelledCount,previousOnHoldCount,previousCompletedCount,previousPendingCount
        );
    }


    private BigDecimal calculatePercentageDifference(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(currentCount - previousCount)
                .divide(BigDecimal.valueOf(previousCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

}



