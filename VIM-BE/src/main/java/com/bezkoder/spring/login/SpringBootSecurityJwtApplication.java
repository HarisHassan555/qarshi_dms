package com.bezkoder.spring.login;

import io.undertow.server.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.bezkoder.spring.login.admin","com.bezkoder.spring.login.sa","com.bezkoder.spring.login.security","com.bezkoder.spring.login.controllers","com.bezkoder.spring.login.admin.dal.daoimpl","com.bezkoder.spring.login.sa.bll"})
public class SpringBootSecurityJwtApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
    SpringApplication.run(SpringBootSecurityJwtApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SpringBootSecurityJwtApplication.class);
	}

	@PreDestroy
	public void cleanup() {
		LocaleContextHolder.resetLocaleContext();
	}

	/*private static final ThreadLocal<Session> threadLocalSession = new ThreadLocal<>();

	@PreDestroy
	public static void clearThreadLocalSession() {
		threadLocalSession.remove();
	}*/


	/*@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@PreDestroy
	public void destroy() {
		taskExecutor.shutdown();
		System.out.println("ThreadPoolExecutor has been shut down.");
	}*/

	/*@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				System.out.println(beanName);
			}
		};
	}*/


	/*@PostConstruct
	public void init() {
		try {
			Path path = Paths.get("./temp/uploads");
			if (!Files.exists(path)) {
				Files.createDirectories(path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

}
