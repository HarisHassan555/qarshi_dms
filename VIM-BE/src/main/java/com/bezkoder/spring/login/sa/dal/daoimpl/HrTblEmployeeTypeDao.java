package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.*;;

import com.bezkoder.spring.login.sa.dal.dao.IHrTblEmployeeTypeDao;
import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployeeType;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;


@Repository
public class HrTblEmployeeTypeDao implements  IHrTblEmployeeTypeDao{

	@Autowired
	private EntityManagerFactory entityManagerFactory;
	private static final Logger log = LoggerFactory.getLogger(HrTblEmployeeTypeDao.class);
	
	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}
	
	@Override
	public List<HrTblEmployeeType> getAllEmployeeTypes() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblEmployeeType> listAll = entityManager.createQuery("FROM  HrTblEmployeeType ").getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();
		return listAll;
	}

	@Override
	public HrTblEmployeeType addNewEmployeeType(HrTblEmployeeType hrTblEmployeeType) {
		try {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(hrTblEmployeeType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return hrTblEmployeeType;
		}catch(Exception ex) {
			log.error(ex.getMessage());
			return null;
		}		
	}

	@Override
	public String deleteEmployeeType(List<HrTblEmployeeType> listEmployeeTypes) {
		try {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			 for(HrTblEmployeeType employeeType: listEmployeeTypes) {
				 entityManager.remove(employeeType);
			 }
			return "success";
		}catch(Exception ex) {
			log.error(ex.getMessage());
			return null;
		}	
	}

	@Override
	public HrTblEmployeeType updateEmployeeType(HrTblEmployeeType hrTblEmployeeType) {
		try {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(hrTblEmployeeType);
			entityManager.getTransaction().commit();
			entityManager.close();
			return hrTblEmployeeType;
		}catch(Exception ex) {
			log.error(ex.getMessage());
			return null;
		}		
	}

	@Override
	public HrTblEmployeeType getEmployeeTypeById(HrTblEmployeeType hrTblEmployeeType) {
		try {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			HrTblEmployeeType result = (HrTblEmployeeType) 
					entityManager.createQuery("FROM HrTblEmployeeType where serEmployeeId = " + 
							hrTblEmployeeType.getTxtEmployeeType()).getSingleResult();
			return result;
		}catch(Exception ex) {
			log.error(ex.getMessage());
			return null;
		}	
	}
 
}
