package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.IPatientDAO;
import com.bezkoder.spring.login.admin.dal.entities.Patient;

@Repository
public class PatientDAO implements IPatientDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(PatientDAO.class);

	public PatientDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Patient> getAllPatient() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<Patient> Patients = entityManager.createQuery("FROM Patient  ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Patients;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Patient> getActivePatient() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<Patient> Patients = entityManager
				.createQuery("FROM Patient ").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Patients;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Patient> getPatientByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Patient where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serPatientId <> " + oldValue;
			}
			List<Patient> Patients = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Patients;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	/*
	 * String date = simpleDateFormat.format(new Date());
	 * System.out.println(date);
	 */
	@Override
	public String addNewPatient(Patient Patient) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
//			Patient.setCreatedBy(commonService.getCurrentLoggedInUser());
//			Patient.setCreatedAt(commonService.getCurrentTimeStamp_new());
			
			
			
//			Patient.setIsActive(true);
//			last_sync_message character varying(1000) NOT NULL,
//			  last_sync_time timestamp without time zone,
//			  mrn character varying(100) NOT NULL,
//			  accountid bigint,
//			  ehrid bigint NOT NULL,
//			  sync_statusid bigint NOT NULL,
			Patient.setLastSyncMessage("");
			Patient.setSyncStatusid(new Long(1));
		//	Patient.setMrn("");
		//	Patient.setSyncStatusid(0);
			entityManager.persist(Patient);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deletePatient(List<String> PatientsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serPatientId : PatientsId) {
				Patient Patient = entityManager.find(Patient.class, Integer.parseInt(serPatientId));
//				if (Patient != null) {
//					Patient.setBlIsDeleted(true);
//
//				}
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
	public String updatePatient(Patient Patient) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(Patient);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generatePatientNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getPatientById(String PatientId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM Patient where txtPatientCode='" + PatientId + "'";

			List<Patient> Patient = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Patient.size() > 0) {
				return String.valueOf(Patient.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<Patient> searchPatient(Patient Patient) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from Patient Patient where 1=1 ";
	  
	  
	  
	    
	    /*if(Patient.getTxtEmail() !=null){
	    	query+=" and upper(Patient.txtEmail) like"+" upper('"+Patient.getTxtEmail()+"')"+"  ";
	    }*/
	    
	   
	  
	    query+=" order by Patient.serPatientId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<Patient> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
