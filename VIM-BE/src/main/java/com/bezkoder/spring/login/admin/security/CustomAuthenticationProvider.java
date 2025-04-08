package com.bezkoder.spring.login.admin.security;

import com.bezkoder.spring.login.admin.bll.servicesimpl.SetupService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@EnableWebSecurity
@CrossOrigin(origins = "*")
public class CustomAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	SetupService setupService ;
	/*@Autowired
    CustomSuccessHandler customSuccessHandler;*/
	
	private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

	public Authentication authenticate(Authentication authentication) {

		CfgTblUser appUser = new CfgTblUser();
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();
		appUser.setTxtUserName(username);
		appUser.setTxtPassword(password);
		CfgTblUser result = setupService.userLogin(appUser);
		if (result == null) {
			throw new UsernameNotFoundException("User not found");
		}
		//if(result.getBlIsPasswordChang())
		
		{
			   System.out.println(result.getBlIsPasswordChang());
			   
			 //  response.sendRedirect("/WEB-INF/jsp/failure.jsp");
			   
			   /*HttpSecurity http.csrf().disable()
			      .authorizeRequests().anyRequest().authenticated().and()
			      .formLogin()
			        .loginPage("/login")
			        .successHandler(customSuccessHandler)
			        .permitAll().and()
			      .logout()
			        .permitAll().and().exceptionHandling().accessDeniedHandler(new CustomAccessDeniedHandler());*/
			   

		}
		List<String> roles = new ArrayList<String>();

		try {

			List<String> userRoles = setupService.getUserRoles(username);

			log.debug("******************* After Fetching User Roles *******************************");
			for (String role : userRoles) {
				// log.debug("********************"+role.getRoleName()+"*******************");
				roles.add(role);
				System.out.println("user Role for talha " + role);
			}
		} catch (Exception ex) {
			log.error(
					"************************** Unable to Load Users's Roles*********************\n" + ex.getMessage(),
					ex);
		}
		try {
			log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> User Id : " + result.getSerUserId()
					+ " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			return new CustomUsernamePasswordAuthenticationToken(result.getSerUserId(), username, password,
					result.getTxtUserName(), null,result.getBlIsPasswordChang(),buildUserAuthority(roles));
		} catch (Exception ex) {
			throw new UsernameNotFoundException("User not found");
		}
	}

	private List<GrantedAuthority> buildUserAuthority(List<String> userRoles) {
		log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>" + Arrays.toString(userRoles.toArray())
				+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		List<GrantedAuthority> setAuths = new ArrayList<GrantedAuthority>();
		// Build user's authorities
		for (String userRole : userRoles) {
			setAuths.add(new SimpleGrantedAuthority("ROLE_" + userRole));
		}
		return setAuths;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
