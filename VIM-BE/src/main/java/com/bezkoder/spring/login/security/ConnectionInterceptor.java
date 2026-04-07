package com.bezkoder.spring.login.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ConnectionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ConnectionInterceptor.class);

    @Before("execution(* com.bezkoder.spring.login.sa.dal.daoimpl.*.*(..))")
    public void beforeExecution(JoinPoint joinPoint) {
        log.trace("Entering DAO method: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @After("execution(* com.bezkoder.spring.login.sa.dal.daoimpl.*.*(..))")
    public void afterExecution(JoinPoint joinPoint) {
        log.trace("Leaving DAO method: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }
}
