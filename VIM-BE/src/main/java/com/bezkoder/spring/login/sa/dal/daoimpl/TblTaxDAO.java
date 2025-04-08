package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;

import com.bezkoder.spring.login.sa.dal.dao.ITblTaxDAO;
import com.bezkoder.spring.login.sa.dal.entities.TblTax;

@Repository
public class TblTaxDAO implements ITblTaxDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(TblTaxDAO.class);

	public TblTaxDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TblTax> getAllTax() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<TblTax> tax = entityManager.createQuery("FROM TblTax where blIsDeleted=FALSE ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return tax;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TblTax> getActiveTax() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<TblTax> tax = entityManager
				.createQuery("FROM TblTax where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
		

		return tax;
	}

	@Override
	public String addNewTax(TblTax tax) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			tax.setBlnStatus(true);
			tax.setBlIsDeleted(false);
			
			entityManager.persist(tax);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteTax(List<String> id) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serTaxId : id) {
				TblTax tax = entityManager.find(TblTax.class, Integer.parseInt(serTaxId));
				if (tax != null) {
					tax.setBlIsDeleted(true);

				}
			}
			entityManager.getTransaction().commit();
			entityManager.close();

		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}
		return "Success";
	}

	@Override
	public String updateTax(TblTax tax) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(tax);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

}
