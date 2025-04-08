package com.bezkoder.spring.login.sa.bll.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponse {

    private long approvedCount;
    private long cancelledCount;
    private long onHoldCount;
    private long completedCount;

    private long pendingCount;

    private long previousApprovedCount;
    private long previousCancelledCount;
    private long previousOnHoldCount;

    private long previousCompletedCount;

    private long previousPendingCount;
    public long getPreviousApprovedCount() {
        return previousApprovedCount;
    }

    public void setPreviousApprovedCount(long previousApprovedCount) {
        this.previousApprovedCount = previousApprovedCount;
    }

    public long getPreviousCancelledCount() {
        return previousCancelledCount;
    }

    public void setPreviousCancelledCount(long previousCancelledCount) {
        this.previousCancelledCount = previousCancelledCount;
    }

    public long getPreviousOnHoldCount() {
        return previousOnHoldCount;
    }

    public void setPreviousOnHoldCount(long previousOnHoldCount) {
        this.previousOnHoldCount = previousOnHoldCount;
    }

    public long getPreviousCompletedCount() {
        return previousCompletedCount;
    }

    public void setPreviousCompletedCount(long previousCompletedCount) {
        this.previousCompletedCount = previousCompletedCount;
    }

    public long getPreviousPendingCount() {
        return previousPendingCount;
    }

    public void setPreviousPendingCount(long previousPendingCount) {
        this.previousPendingCount = previousPendingCount;
    }



    private BigDecimal transactionDifference;

    public DashboardResponse(long approvedCount, long cancelledCount, long onHoldCount, long completedCount, BigDecimal transactionDifference, List<SaleOrderStatusDTO> lastSixTransactions, BigDecimal percentageDifferenceApproved, BigDecimal percentageDifferenceCancelled, BigDecimal percentageDifferenceOnHold, BigDecimal percentageDifferenceCompleted,long pendingCount,BigDecimal percentageDifferencePending,long previousApprovedCount,long previousCancelledCount,long previousOnHoldCount,long previousCompletedCount,long previousPendingCount) {
        this.approvedCount = approvedCount;
        this.cancelledCount = cancelledCount;
        this.onHoldCount = onHoldCount;
        this.completedCount = completedCount;
        this.transactionDifference = transactionDifference;
        this.lastSixTransactions = lastSixTransactions;
        this.percentageDifferenceApproved = percentageDifferenceApproved;
        this.percentageDifferenceCancelled = percentageDifferenceCancelled;
        this.percentageDifferenceOnHold = percentageDifferenceOnHold;
        this.percentageDifferenceCompleted = percentageDifferenceCompleted;
        this.pendingCount = pendingCount;
        this.percentageDifferencePending= percentageDifferencePending;
        this.previousApprovedCount = previousApprovedCount;
        this.previousCancelledCount=previousCancelledCount;
        this.previousOnHoldCount=previousOnHoldCount;
        this.previousCompletedCount=previousCompletedCount;
        this.previousPendingCount=previousPendingCount;
    }

    private List<SaleOrderStatusDTO> lastSixTransactions;
    private BigDecimal percentageDifferenceApproved;
    private BigDecimal percentageDifferenceCancelled;
    private BigDecimal percentageDifferenceOnHold;
    private BigDecimal percentageDifferenceCompleted;

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public BigDecimal getPercentageDifferencePending() {
        return percentageDifferencePending;
    }

    public void setPercentageDifferencePending(BigDecimal percentageDifferencePending) {
        this.percentageDifferencePending = percentageDifferencePending;
    }

    private BigDecimal percentageDifferencePending;
    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public long getOnHoldCount() {
        return onHoldCount;
    }

    public void setOnHoldCount(long onHoldCount) {
        this.onHoldCount = onHoldCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public BigDecimal getTransactionDifference() {
        return transactionDifference;
    }

    public void setTransactionDifference(BigDecimal transactionDifference) {
        this.transactionDifference = transactionDifference;
    }

    public List<SaleOrderStatusDTO> getLastSixTransactions() {
        return lastSixTransactions;
    }

    public void setLastSixTransactions(List<SaleOrderStatusDTO> lastSixTransactions) {
        this.lastSixTransactions = lastSixTransactions;
    }

    public BigDecimal getPercentageDifferenceApproved() {
        return percentageDifferenceApproved;
    }

    public void setPercentageDifferenceApproved(BigDecimal percentageDifferenceApproved) {
        this.percentageDifferenceApproved = percentageDifferenceApproved;
    }

    public BigDecimal getPercentageDifferenceCancelled() {
        return percentageDifferenceCancelled;
    }

    public void setPercentageDifferenceCancelled(BigDecimal percentageDifferenceCancelled) {
        this.percentageDifferenceCancelled = percentageDifferenceCancelled;
    }

    public BigDecimal getPercentageDifferenceOnHold() {
        return percentageDifferenceOnHold;
    }

    public void setPercentageDifferenceOnHold(BigDecimal percentageDifferenceOnHold) {
        this.percentageDifferenceOnHold = percentageDifferenceOnHold;
    }

    public BigDecimal getPercentageDifferenceCompleted() {
        return percentageDifferenceCompleted;
    }

    public void setPercentageDifferenceCompleted(BigDecimal percentageDifferenceCompleted) {
        this.percentageDifferenceCompleted = percentageDifferenceCompleted;
    }
}