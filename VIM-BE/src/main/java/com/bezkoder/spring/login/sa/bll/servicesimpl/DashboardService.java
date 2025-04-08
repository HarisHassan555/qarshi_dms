package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.SaleOrderStatusDTO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private SlsTblSaleOrderRepository saleOrderRepository;


    public Map<String, Object> getDashboardData(Date startDate, Date endDate) {
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("approvedCount", getApprovedCount(startDate, endDate));
        dashboardData.put("completedCount", getCompletedCount(startDate, endDate));
        dashboardData.put("cancelledCount", getCancelledCount(startDate, endDate));
        dashboardData.put("onHoldCount", getOnHoldCount(startDate, endDate));
        dashboardData.put("pendingCount", getPendingCount(startDate, endDate));
        dashboardData.put("totalSalesAmount", getTotalAmountInPeriod(startDate, endDate));
        dashboardData.put("averageOrderValue", getAverageOrderValue(startDate, endDate));
        dashboardData.put("salesStatusDistribution", getSalesStatusDistribution(startDate, endDate));
        dashboardData.put("salesCountByProduct", getSalesCountByProduct(startDate, endDate));
        dashboardData.put("transactionDifference", calculateTransactionDifference(startDate, endDate));
        dashboardData.put("top6Transaction", getLastSixTransactions(startDate, endDate));
        return dashboardData;
    }

    public long getApprovedCount(Date startDate, Date endDate) {
        return saleOrderRepository.countApproved(startDate, endDate);
    }

    public long getCompletedCount(Date startDate, Date endDate) {
        return saleOrderRepository.countCompleted(startDate, endDate);
    }

    public long getCancelledCount(Date startDate, Date endDate) {
        return saleOrderRepository.countCancelled(startDate, endDate);
    }

    public long getOnHoldCount(Date startDate, Date endDate) {
        return saleOrderRepository.countOnHold(startDate, endDate);
    }

    public long getPendingCount(Date startDate, Date endDate) {
        return saleOrderRepository.countPending(startDate, endDate);
    }

    public BigDecimal getTotalAmountInPeriod(Date startDate, Date endDate) {
        return saleOrderRepository.getTotalAmountInPeriod(startDate, endDate);
    }

    public BigDecimal getAverageOrderValue(Date startDate, Date endDate) {
        return saleOrderRepository.getAverageOrderValue(startDate, endDate);
    }

    public List<Object[]> getSalesStatusDistribution(Date startDate, Date endDate) {
        return saleOrderRepository.getSalesStatusDistribution(startDate, endDate);
    }

    public List<Object[]> getSalesCountByProduct(Date startDate, Date endDate) {
        return saleOrderRepository.getSalesCountByProduct(startDate, endDate);
    }

    public BigDecimal calculateTransactionDifference(Date startDate, Date endDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        // Last week date range
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date startDateLastWeek = calendar.getTime();
        Date endDateLastWeek = endDate;

        // Previous week date range
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date startDatePreviousWeek = calendar.getTime();
        Date endDatePreviousWeek = startDateLastWeek;

        // Get total amounts for both periods
        BigDecimal totalLastWeek = saleOrderRepository.getTotalAmountInPeriod(startDateLastWeek, endDateLastWeek);
        BigDecimal totalPreviousWeek = saleOrderRepository.getTotalAmountInPeriod(startDatePreviousWeek, endDatePreviousWeek);

        if (totalPreviousWeek != null && totalPreviousWeek.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal difference = totalLastWeek.subtract(totalPreviousWeek);
            return difference.multiply(BigDecimal.valueOf(100)).divide(totalPreviousWeek, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }

    /*public long getApprovedCount(Date startDate, Date endDate) {
        return saleOrderRepository.countApproved(startDate, endDate);
    }

    public long getCompletedCount(Date startDate, Date endDate) {
        return saleOrderRepository.countCompleted(startDate, endDate);
    }

    public long getCancelledCount(Date startDate, Date endDate) {
        return saleOrderRepository.countCancelled(startDate, endDate);
    }

    public long getOnHoldCount(Date startDate, Date endDate) {
        return saleOrderRepository.countOnHold(startDate, endDate);
    }

    public long getPendingCount(Date startDate, Date endDate) {
        return saleOrderRepository.countPending(startDate, endDate);
    }

    public BigDecimal calculateTransactionDifference(Date startDate, Date endDate) {


        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date startDateLastWeek = calendar.getTime();
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        Date endDateLastWeek = endDate;


        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date startDatePreviousWeek = calendar.getTime();
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        Date endDatePreviousWeek = startDateLastWeek;

        // Get total amounts for both periods
        BigDecimal totalLastWeek = saleOrderRepository.getTotalAmountInPeriod(startDateLastWeek, endDateLastWeek);
        BigDecimal totalPreviousWeek = saleOrderRepository.getTotalAmountInPeriod(startDatePreviousWeek, endDatePreviousWeek);

        if (totalPreviousWeek != null && totalPreviousWeek.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal difference = totalLastWeek.subtract(totalPreviousWeek);
            return difference.multiply(BigDecimal.valueOf(100)).divide(totalPreviousWeek, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }*/

    public List<SaleOrderStatusDTO> getLastSixTransactions(Date startDate, Date endDate) {
        List<SaleOrderStatusDTO> transactions =  saleOrderRepository.findTop6Transactions(startDate,endDate);
        return transactions;
        /*.stream()
                .sorted((o1, o2) -> o2.getDteDate().compareTo(o1.getDteDate())) // Sort by transaction date descending
                .limit(1) // Limit to the most recent
                .collect(Collectors.toList());*/
    }


    public List<SlsTblSaleOrder> getPendingSaleOrders(Date startDate, Date endDate) {
        List<String> statuses = Arrays.asList("Pending", "InProgress");
        return saleOrderRepository.findPendingOrders(startDate, endDate);
    }

    public List<SlsTblSaleOrder> getHoldSaleOrders(Date startDate, Date endDate) {
       // List<String> statuses = Arrays.asList("Hold", "hold");
        return saleOrderRepository.findOnHoldTransactions(startDate, endDate);
    }

    public List<SlsTblSaleOrder> getApprovedSaleOrders(Date startDate, Date endDate) {
        // List<String> statuses = Arrays.asList("Hold", "hold");
        return saleOrderRepository.findApprovedTransactions(startDate, endDate);
    }

    public List<SlsTblSaleOrder> getCancelledSaleOrders(Date startDate, Date endDate) {
        // List<String> statuses = Arrays.asList("Hold", "hold");
        return saleOrderRepository.fetchCancelledTransaction(startDate, endDate);
    }
}
