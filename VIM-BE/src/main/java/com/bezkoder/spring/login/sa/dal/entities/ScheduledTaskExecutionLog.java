package com.bezkoder.spring.login.sa.dal.entities;

import org.joda.time.DateTime;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_task_execution_log")
public class ScheduledTaskExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_time", nullable = false)
    private String executionTime;

    @Column(name = "output_date", nullable = false)
    private String outputDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Constructors
    public ScheduledTaskExecutionLog() {}

    public ScheduledTaskExecutionLog(String executionTime, String outputDate, String status, String errorMessage) {
        this.executionTime = executionTime;
        this.outputDate = outputDate;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public String getOutputDate() {
        return outputDate;
    }

    public void setOutputDate(String outputDate) {
        this.outputDate = outputDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ScheduledTaskExecutionLog{" +
                "id=" + id +
                ", executionTime=" + executionTime +
                ", outputDate='" + outputDate + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}