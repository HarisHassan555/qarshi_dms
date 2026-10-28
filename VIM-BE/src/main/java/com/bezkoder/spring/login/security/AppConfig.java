package com.bezkoder.spring.login.security;

import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@EnableScheduling
@Configuration
@ComponentScan(basePackages = {"com.bezkoder.spring.login", "com.bezkoder.spring.login.admin","com.bezkoder.spring.login.admin.security"})
public class AppConfig {

    /*@Bean
    public SoapClientService soapClientService(Environment env) {
        SoapClientService soapClientService = new SoapClientService();
        *//*soapClientService.setSoapEndpointUrl(env.getProperty("soap.endpoint.url"));
        soapClientService.setUsername(env.getProperty("soap.username"));
        soapClientService.setPassword(env.getProperty("soap.password"));*//*
        return soapClientService;
    }*/


    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("velocity-scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        return new UndertowServletWebServerFactory();
    }
}
