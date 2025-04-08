package com.bezkoder.spring.login.security;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Aspect
@Component
public class ConnectionInterceptor {

    @Autowired
    private DataSource dataSource;

    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    @Before("execution(* com.bezkoder.spring.login.sa.dal.daoimpl.*.*(..))")
    public void beforeExecution() throws SQLException {
        Connection connection = dataSource.getConnection();
        connectionHolder.set(connection);
        System.out.println("✅ Opened Connection: " + connection);
    }

    @After("execution(* com.bezkoder.spring.login.sa.dal.daoimpl.*.*(..))")
    public void afterExecution() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✅ Closed Connection: " + connection);
            } catch (SQLException e) {
                System.err.println("Error while closing connection: " + e.getMessage());
            } finally {
                connectionHolder.remove();
            }
        }
    }
}
