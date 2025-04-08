package com.bezkoder.spring.login.admin.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
/***
 * 
 * @author Muhammad Khalil
 *
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
		RequestDispatcher rd= (RequestDispatcher) request.getRequestDispatcher("/error?errorMessage=Access Denied!");
		try {
			rd.forward((ServletRequest) request, (ServletResponse) response);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}
}
