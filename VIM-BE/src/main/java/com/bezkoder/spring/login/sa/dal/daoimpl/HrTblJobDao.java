package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.sa.dal.dao.IHrTblJobDao;
import com.bezkoder.spring.login.sa.dal.entities.HrTblJob;

@Repository
public class HrTblJobDao implements IHrTblJobDao {

	@Autowired
	private EntityManagerFactory entityManagerFactory;
	
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(HrTblJobDao.class);
	
	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<HrTblJob> getAllJobs() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblJob> listresult =  entityManager.createQuery("From HrTblJob").getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();
 		return listresult;
	}

	@Override
	public HrTblJob addNewJob(HrTblJob job) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		entityManager.persist(job);
		entityManager.getTransaction().commit();
		entityManager.close();
		return job;
	}

	@Override
	public String deleteJob(List<HrTblJob> listJobs) {
		EntityManager entityManager = getEntityManager();
 		entityManager.getTransaction().begin();
		for(HrTblJob job : listJobs) {
		 	   entityManager.remove(job);
		}
        entityManager.getTransaction().commit(); 		
	    entityManager.close();
	    return "success";
	}

	@Override
	public HrTblJob updateJob(HrTblJob job) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		entityManager.merge(job);
		entityManager.getTransaction().commit();
		entityManager.close();
		return job;
	}

	@Override
	public HrTblJob getJobById(HrTblJob job) {
 		EntityManager entityManager = getEntityManager();
 		entityManager.getTransaction().begin();
 		entityManager.createQuery("FROM HrTblJob where serJobId = "  + job.getSerJobId());
 		entityManager.getTransaction().commit();
 		entityManager.close();
 		return job;
	}

}
