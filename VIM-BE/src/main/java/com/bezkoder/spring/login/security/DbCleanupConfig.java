package com.bezkoder.spring.login.security;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

@Component
public class DbCleanupConfig {

    @PreDestroy
    public void cleanup() {
        System.out.println("Cleaning up MySQL JDBC resources...");
        AbandonedConnectionCleanupThread.checkedShutdown();
        System.out.println("✅ MySQL Abandoned Connection Cleanup Thread shut down successfully.");
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                System.out.println("✅ Deregistered JDBC driver: " + driver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
