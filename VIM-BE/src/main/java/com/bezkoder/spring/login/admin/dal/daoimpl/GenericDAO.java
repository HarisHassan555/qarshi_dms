package com.bezkoder.spring.login.admin.dal.daoimpl;
/*package com.bezkoder.spring.login.hms.dal.daoimpl.common;

import javax.persistence.*;;

import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.login.hms.dal.dao.common.IGenericDAO;

*//**
 * @author WAQAR NAWAZ
 *
 *//*
@Service
public abstract class GenericDAO<K, E> implements IGenericDAO<K, E> {

	@Autowired
	@PersistenceUnit(unitName = "hms")
	private EntityManagerFactory entityManagerFactory;

	private static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("hms");

	private EntityManager entityManager = null;// entityManagerFactory.createEntityManager();

	*//**
	 * 
	 *//*
	public GenericDAO() {
		entityManager = entityManagerFactory.createEntityManager();

	}

	@Override
	@Transactional
	public void persist(E entity) {
		
		 * if (!this.getEntityManager().getTransaction().isActive()) {
		 * this.getEntityManager().getTransaction().begin(); }
		 
		startTransaction();
		this.getEntityManager().persist(entity);
		commitTransaction();
		
		 * this.getEntityManager().getTransaction().commit();
		 * this.getEntityManager().close();
		 

		// entityManager.flush();
		// entityManager.refresh(entity);
	}

	@Override
	public void persistWOT(E entity) {

		// this.entityManager.getTransaction().begin();
		this.getEntityManager().persist(entity);
		// this.entityManager.getTransaction().commit();
		// entityManager.flush();
		// entityManager.refresh(entity);
	}

	@Override
	@Transactional
	public void update(E entity) {

		
		 * if (!this.getEntityManager().getTransaction().isActive()) {
		 * this.getEntityManager().getTransaction().begin(); }
		 
		startTransaction();
		this.getEntityManager().merge(entity);
		commitTransaction();
		
		 * this.getEntityManager().getTransaction().commit();
		 * this.getEntityManager().close();
		 
		// entityManager.flush();
	}

	@Override
	@Transactional
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void remove(Class type, Object id) {

		
		 * if (!this.getEntityManager().getTransaction().isActive()) {
		 * this.getEntityManager().getTransaction().begin(); }
		 
		startTransaction();
		Object entity = this.getEntityManager().getReference(type, id);
		this.getEntityManager().remove(entity);
		commitTransaction();
		
		 * this.getEntityManager().getTransaction().commit();
		 * this.getEntityManager().close();
		 
		// entityManager.flush();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object findById(Class type, Object id) {

		return this.getEntityManager().find(type, id);
	}

	protected void refreshDB() {

		// clears the First level cache of EM
		this.getEntityManager().clear();
		// clears the Second level cache of EM
		this.getEntityManager().getEntityManagerFactory().getCache().evictAll();
	}

	protected EntityManager getEntityManager() {
		if (this.entityManager.isOpen()) {
			return this.entityManager;
		} else {
			this.entityManager = entityManagerFactory.createEntityManager();
			return this.entityManager;
		}
	}

	protected boolean startTransaction() {
		if (!this.getEntityManager().getTransaction().isActive()) {
			this.getEntityManager().getTransaction().begin();
			return true;
		}
		return false;
	}

	protected void commitTransaction() {
		if (this.getEntityManager().getTransaction().isActive()) {
			this.getEntityManager().getTransaction().commit();
		}
		if (this.getEntityManager().isOpen()) {
			this.getEntityManager().close();
		}
	}

	protected void close() {
		if (this.getEntityManager().isOpen()) {
			this.getEntityManager().close();
		}
	}

	protected void flush() {
		if (this.getEntityManager().isOpen()) {
			this.getEntityManager().flush();
		}
	}

	protected void rollback() {
		if (this.getEntityManager().getTransaction().isActive()) {
			this.getEntityManager().getTransaction().rollback();
		}
	}

	protected void clear() {
		if (this.getEntityManager().isOpen()) {
			getEntityManager().clear();
		}
	}
}
*/