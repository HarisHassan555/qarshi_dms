package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.bezkoder.spring.login.repository.UserRepository;
import javax.persistence.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bezkoder.spring.login.admin.bll.dto.NavigationMenuRoles;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.security.CustomUsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CommonService implements ICommonService {

	private boolean syncEmployeeCompleted;
	// Map<String, List<Visit>> confirmRejectOrNewVisits = new HashMap<String,
	// List<Visit>>();

	private List<NavigationMenuRoles> navigationMenuRoles;
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	/*
	 * @Autowired
	 * PfServerProperties serverProperties;
	 */

	private RestTemplate restTemplate;
	private ObjectMapper mapper;

	@Autowired
	private UserRepository userRepository;

	public RestTemplate getRestTemplate() {
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
		}
		return restTemplate;
	}

	public ObjectMapper getMapper() {
		if (mapper == null) {
			mapper = new ObjectMapper();
		}
		return mapper;
	}

	/*
	 * public int getCurrentLoggedInUser(String userName) {
	 * if (userName != null) {
	 * CfgTblUser user = userRepository.findByTxtUserName(userName);
	 * return user.getSerUserId();
	 * }else{
	 * return -1;
	 * }
	 * 
	 * }
	 */

	public int getCurrentLoggedInUser() {

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		if (attributes != null) {
			Integer currentUserId = (Integer) attributes.getRequest().getAttribute("currentUserId");
			return currentUserId != null ? currentUserId : -1;
		}

		return -1;
	}

	/*
	 * public int getCurrentLoggedInUser() {
	 * // TODO Auto-generated method stub
	 * 
	 * if(SecurityContextHolder.getContext().getAuthentication()!=null &&
	 * SecurityContextHolder.getContext().getAuthentication() instanceof
	 * CustomUsernamePasswordAuthenticationToken){
	 * return ((CustomUsernamePasswordAuthenticationToken)
	 * SecurityContextHolder.getContext().getAuthentication()).getVoId();
	 * }
	 * return -1;
	 * // return 1;
	 * }
	 */

	public boolean getIsPasswordChange(int id) {
		// TODO Auto-generated method stub

		// if(SecurityContextHolder.getContext().getAuthentication()!=null &&
		// SecurityContextHolder.getContext().getAuthentication() instanceof
		// CustomUsernamePasswordAuthenticationToken){
		// return ((CustomUsernamePasswordAuthenticationToken)
		// SecurityContextHolder.getContext().getAuthentication()).isPasswordChange();
		// }

		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblUser Users = (CfgTblUser) entityManager.createQuery("FROM CfgTblUser where serUserId = " + id)
				.getSingleResult();
		entityManager.getTransaction().commit();
		entityManager.close();

		return Users.getBlIsPasswordChang();
	}

	public CfgTblUser getCurrentUser(int id) {

		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblUser Users = (CfgTblUser) entityManager.createQuery("FROM CfgTblUser where serUserId = " + id)
				.getSingleResult();
		entityManager.getTransaction().commit();
		entityManager.close();

		return Users;
	}

	public DateFormat getDateFormater() {
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
		return df;
	}

	@SuppressWarnings("unchecked")
	public String getCurrentUserRole() {
		Collection<SimpleGrantedAuthority> authorities = null;
		try {
			authorities = (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication()
					.getAuthorities();
		} catch (Exception ex) {
		}
		if (authorities != null) {
			String roles = "";
			for (SimpleGrantedAuthority auth : authorities) {
				roles += auth.getAuthority() + ",";
			}
			roles = roles.substring(0, roles.length() - 1);
			return roles;
		} else {
			return "ROLE_MANAGER";
		}

	}

	public boolean isSyncEmployeeCompleted() {
		return syncEmployeeCompleted;
	}

	public void setSyncEmployeeCompleted(boolean syncEmployeeCompleted) {
		this.syncEmployeeCompleted = syncEmployeeCompleted;
	}

	public List<NavigationMenuRoles> getNavigationMenuRoles() {
		if (navigationMenuRoles == null) {
			populateNavigationMenus();
		}
		return navigationMenuRoles;
	}

	public void removeNavigationMenuRoles() {
		this.navigationMenuRoles = null;
	};

	@SuppressWarnings("unchecked")
	private void populateNavigationMenus() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblMenu> menuList = entityManager.createQuery(
				"FROM CfgTblMenu").getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();
		navigationMenuRoles = new ArrayList<NavigationMenuRoles>();

		try {
			for (CfgTblMenu menu : menuList) {
				NavigationMenuRoles nav = new NavigationMenuRoles();
				nav.setMenuName(menu.getTxtMenuName());
				nav.setMenuIcon(menu.getTxtMenuIcons());
				nav.setMenuRoles("");

				System.out.println("menu.getCfgTblSubMenus().size()-----:" + menu.getCfgTblSubMenus().size());
				if (menu.getCfgTblSubMenus().size() > 0) {
					for (CfgTblSubMenu subMenu : menu.getCfgTblSubMenus()) {

						System.out.println("----------:" + nav.getSubMenuRoles());
						if (!nav.getSubMenuRoles().containsKey(subMenu)) {
							nav.getSubMenuRoles().put(subMenu, "");
						}
						// subMenu.getCfgTblSubMenuRoles().iterator()

						for (CfgTblRole role : subMenu.getCfgTblRole()) {
							String subRoles = nav.getSubMenuRoles().get(subMenu);
							subRoles = subRoles + ",ROLE_" + role.getTxtRoleName().toUpperCase();
							nav.getSubMenuRoles().put(subMenu, subRoles);
						}
						subMenu.setCfgTblMenu(null);
						nav.setMenuRoles(nav.getMenuRoles() + nav.getSubMenuRoles().get(subMenu));
					}
				}
				navigationMenuRoles.add(nav);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	public int getCurrentUserRegionId() {
		if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
				.getAuthentication() instanceof CustomUsernamePasswordAuthenticationToken) {
			return ((CustomUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
					.getFkRegionId();
		}
		return -1;
	}

	public Boolean isCurrentUserRegionHead() {
		if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
				.getAuthentication().getPrincipal() instanceof CustomUsernamePasswordAuthenticationToken) {
			return ((CustomUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal()).getRegionHead();
		}
		return false;
	}

	public String getRegionBasedQuery(String tableColumnName, Boolean startAnd, Boolean endAnd) {
		String regionQuery = "";
		if (!isCurrentUserRegionHead()) {
			regionQuery += ((startAnd) ? " and " : " ") + tableColumnName + " = " + getCurrentUserRegionId() + ""
					+ ((endAnd) ? " and" : " ");
		}
		return regionQuery;
	}

	public int getCurrentUserVoId() {
		if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
				.getAuthentication() instanceof CustomUsernamePasswordAuthenticationToken) {
			return ((CustomUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
					.getVoId();
		}
		return -1;
	}

	@Override
	public String getCurrentTimeStamp() {
		return new Timestamp(new Date().getTime()).toString();
	}

	@Override
	public Timestamp getCurrentTimeStamp_new() {
		return new Timestamp(new Date().getTime());
	}

	public String getCurrentUserName() {
		if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
				.getAuthentication() instanceof CustomUsernamePasswordAuthenticationToken) {
			return ((CustomUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
					.getName();
		}
		return "";
	}

	public List<NavigationMenuRoles> getNavigationMenuRolesNew() {
		if (navigationMenuRoles == null) {
			populateNavigationMenus();
		}
		return navigationMenuRoles;
	}

	public List<String> getUserAddressesByRoles(List<String> roleNames) {
		EntityManager entityManager = getEntityManager();
		List<String> userAddresses = new ArrayList<>();

		try {
			entityManager.getTransaction().begin();

			userAddresses = entityManager.createQuery(
					"SELECT u.txtAddress FROM CfgTblUser u WHERE u.cfgTblRole.txtRoleName IN :roleNames AND u.txtAddress IS NOT NULL")
					.setParameter("roleNames", roleNames)
					.getResultList();

			entityManager.getTransaction().commit();
		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			e.printStackTrace(); // Replace with proper logging
		} finally {
			entityManager.close();
		}

		return userAddresses;
	}

	public List<String> getAddressesBasedOnRoleHierarchy(CfgTblUser currentUser) {
		Map<String, List<String>> roleHierarchy = new HashMap<>();
		roleHierarchy.put("MARKETING", Arrays.asList("PROCUREMENT", "PROCUREMENT_HEAD"));
		roleHierarchy.put("MARKETING_HEAD", Arrays.asList("PROCUREMENT", "PROCUREMENT_HEAD"));
		roleHierarchy.put("PROCUREMENT", Arrays.asList("TAX", "TAX_HEAD"));
		roleHierarchy.put("PROCUREMENT_HEAD", Arrays.asList("TAX", "TAX_HEAD"));
		roleHierarchy.put("TAX", Arrays.asList("FINANCE", "FINANCE_HEAD"));
		roleHierarchy.put("TAX_HEAD", Arrays.asList("FINANCE", "FINANCE_HEAD"));
		roleHierarchy.put("FINANCE", Arrays.asList("AUDIT", "AUDIT_HEAD"));
		roleHierarchy.put("FINANCE_HEAD", Arrays.asList("AUDIT", "AUDIT_HEAD"));
		roleHierarchy.put("AUDIT", Arrays.asList("PAYMENT", "PAYMENT_HEAD"));
		roleHierarchy.put("AUDIT_HEAD", Arrays.asList("PAYMENT", "PAYMENT_HEAD"));
		roleHierarchy.put("PAYMENT", new ArrayList<>()); // End of hierarchy
		String currentRole = currentUser.getCfgTblRole().getTxtRoleName();
		List<String> nextRoles = roleHierarchy.getOrDefault(currentRole, new ArrayList<>());
		return this.getUserAddressesByRoles(nextRoles);
	}

}
