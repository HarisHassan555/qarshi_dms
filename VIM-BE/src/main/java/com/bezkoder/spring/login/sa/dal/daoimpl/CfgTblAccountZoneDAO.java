package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
 import javax.persistence.*;;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone2;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone3;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone1;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;

@Repository
public class CfgTblAccountZoneDAO implements com.bezkoder.spring.login.sa.dal.dao.ICfgTblAccountZoneDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblAccountZoneDAO.class);

	public CfgTblAccountZoneDAO() {
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@Override
	public String addNewZone1(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		int isFoundCode = checkZone1ByCode(cfgTblZone1);
		int isFoundName = checkZone1ByName(cfgTblZone1);
		if (isFoundCode == 0 && isFoundName == 0) {
			entityManager.getTransaction().begin();
			cfgTblZone1.setBl_Status(new Byte("1"));
			cfgTblZone1.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			cfgTblZone1.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));

			entityManager.persist(cfgTblZone1);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} else {
			return "error";
		}
	}

	public static String setDateParsing(String date) throws ParseException {

		// this format date we want
		DateFormat mSDF = new SimpleDateFormat("mm/dd/yyyy");

		// this format date actully present
		SimpleDateFormat formatter = new SimpleDateFormat("mm/dd/yyyy");
		return mSDF.format(formatter.parse(date));
	}

	public static Date parseDate(String stringToParse) {
		Date date = null;
		try {
			date = new SimpleDateFormat("mm-dd-yyyy").parse(stringToParse);
			System.out.println(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblZone1> getAllZone1() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();

		List<CfgTblZone1> zone1 = entityManager.createQuery("FROM CfgTblZone1 where bl_Status=1").getResultList();

		entityManager.close();

		return zone1;
	}

	@Override
	public String deleteZone1(List<String> zone1Id) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String regId : zone1Id) {
				CfgTblZone1 zone1 = entityManager.find(CfgTblZone1.class, Integer.parseInt(regId));
				if (zone1 != null) {
					zone1.setBl_Status(new Byte("0"));
					zone1.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
					zone1.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));

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
	public String updateZone1(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode = checkZone1ByCodeEdit(cfgTblZone1);
		int isFoundName = checkZone1ByNameEdit(cfgTblZone1);
		if (isFoundCode == 0 && isFoundName == 0) {
			cfgTblZone1.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			cfgTblZone1.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
			entityManager.merge(cfgTblZone1);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} else {
			return "error";
		}
	}

	public int checkZone1ByCode(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone1> zone1 = entityManager
				.createQuery("FROM CfgTblZone1 where txtZone1Code='" + cfgTblZone1.getTxtZone1Code() + "'")
				.getResultList();
		entityManager.close();

		if (zone1.size() > 0) {
			return zone1.size();
		}
		return 0;

	}

	public int checkZone1ByName(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone1> zone1 = entityManager
				.createQuery("FROM CfgTblZone1 where txtZone1Name='" + cfgTblZone1.getTxtZone1Name() + "'")
				.getResultList();
		entityManager.close();
		if (zone1.size() > 0) {
			return zone1.size();
		}
		return 0;

	}

	public int checkZone2ByCode(CfgTblZone2 cfgTblZone2) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone2> zone2 = entityManager
				.createQuery("FROM CfgTblZone2 where txtZone2Code='" + cfgTblZone2.getTxtZone2Code() + "'")
				.getResultList();
		entityManager.close();

		if (zone2.size() > 0) {
			return zone2.size();
		}
		return 0;

	}
	/*
	 * public int checkZone2ByName(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone2> zone23 =
	 * entityManager.createQuery("FROM CfgTblZone2 where txtZone2Name='"+cfgTblZone2
	 * .getTxtZone2Name()+"'").getResultList(); entityManager.close();
	 * if(zone23.size()>0){ return zone23.size(); }return 0;
	 * 
	 * }
	 */

	/*
	 * public int checkZone2ByCode(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone2> zone23 =
	 * entityManager.createQuery("FROM CfgTblZone2 where txtZone2Code='"+cfgTblZone2
	 * .getTxtZone2Code()+"'").getResultList(); entityManager.close();
	 * 
	 * if(zone23.size()>0){ return zone23.size(); }return 0;
	 * 
	 * } public int checkZone2ByName(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone2> zone2 =
	 * entityManager.createQuery("FROM CfgTblZone2 where txtZone2Name='"+cfgTblZone2
	 * .getTxtZone2Name()+"'").getResultList(); entityManager.close();
	 * if(zone2.size()>0){ return zone2.size(); }return 0;
	 * 
	 * }
	 */

	/*
	 * public int checkZone2ByCodeEdit(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone1> zone2 =
	 * entityManager.createQuery("FROM CfgTblZone2 where txtZone2Code='"+cfgTblZone2
	 * .getTxtZone2Code()+"'"+" and serZone2Id <>"+cfgTblZone2.getSerZone2Id()).
	 * getResultList(); entityManager.close();
	 * 
	 * if(zone2.size()>0){ return zone2.size(); }return 0;
	 * 
	 * }
	 * 
	 * public int checkZone2ByNameEdit(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone1> zone2 =
	 * entityManager.createQuery("FROM CfgTblZone2 where txtZone2Name='"+cfgTblZone2
	 * .getTxtZone2Name()+"'"+" and serZone2Id <>"+cfgTblZone2.getSerZone2Id()).
	 * getResultList(); entityManager.close(); if(zone2.size()>0){ return
	 * zone2.size(); }return 0;
	 * 
	 * }
	 */

	public int checkZone1ByCodeEdit(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone1> zone1 = entityManager.createQuery("FROM CfgTblZone1 where txtZone1Code='"
				+ cfgTblZone1.getTxtZone1Code() + "'" + " and serZone1Id <>" + cfgTblZone1.getSerZone1Id())
				.getResultList();
		entityManager.close();

		if (zone1.size() > 0) {
			return zone1.size();
		}
		return 0;

	}

	public int checkZone1ByNameEdit(CfgTblZone1 cfgTblZone1) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone1> zone1 = entityManager.createQuery("FROM CfgTblZone1 where txtZone1Name='"
				+ cfgTblZone1.getTxtZone1Name() + "'" + " and serZone1Id <>" + cfgTblZone1.getSerZone1Id())
				.getResultList();
		entityManager.close();
		if (zone1.size() > 0) {
			return zone1.size();
		}
		return 0;

	}

	public int checkZone2ByCodeEdit(CfgTblZone2 cfgTblZone2) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone1> zone23 = entityManager.createQuery("FROM CfgTblZone2 where txtZone2Code='"
				+ cfgTblZone2.getTxtZone2Code() + "'" + " and serZone2Id <>" + cfgTblZone2.getSerZone2Id())
				.getResultList();
		entityManager.close();

		if (zone23.size() > 0) {
			return zone23.size();
		}
		return 0;

	}

	public int checkZone2ByNameEdit(CfgTblZone2 cfgTblZone2) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone2> zone23 = entityManager.createQuery("FROM CfgTblZone2 where txtZone2Name='"
				+ cfgTblZone2.getTxtZone2Name() + "'" + " and serZone2Id <>" + cfgTblZone2.getSerZone2Id())
				.getResultList();
		entityManager.close();
		if (zone23.size() > 0) {
			return zone23.size();
		}
		return 0;

	}

	@Override
	public String generateZone1Code() {
		int ord_no = 0;

		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			String zone1Code = (String) entityManager.createQuery("select MAX(txtZone1Code) from CfgTblZone1 ")
					.getSingleResult();
			if (isNullOrEmpty(zone1Code)) {

				zone1Code = "RG-00";
			}
			ord_no = Integer.valueOf(zone1Code.substring(3));

			ord_no = ord_no + 1;
			String code = "RG-1";
			if (ord_no < 10)
				code = "RG-00" + ord_no;
			else if (ord_no > 9 && ord_no < 100)
				code = "RG-0" + ord_no;
			else
				code = "RG-" + ord_no;
			return code;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/*
	 * @Override public String addNewZone2(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * int isFoundCode = checkZone2ByCode(cfgTblZone2); int isFoundName =
	 * checkZone2ByName(cfgTblZone2); if (isFoundCode == 0 && isFoundName == 0) {
	 * cfgTblZone2.setBl_Status(new Byte("1"));
	 * cfgTblZone2.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
	 * cfgTblZone2.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()))
	 * ; CfgTblZone1 zone1 =
	 * getZone1Id(cfgTblZone2.getCfgTblZone1().getTxtZone1Code());
	 * cfgTblZone2.setCfgTblZone1(zone1); entityManager.persist(cfgTblZone2);
	 * entityManager.getTransaction().commit(); entityManager.close(); return
	 * "Success"; } else { return "error"; }
	 * 
	 * }
	 */
	@Override
	public String updateZone2(CfgTblZone2 cfgTblZone2) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode = checkZone2ByCodeEdit(cfgTblZone2);
		int isFoundName = checkZone2ByNameEdit(cfgTblZone2);
		if (isFoundCode == 0 && isFoundName == 0) {
			/*
			 * CfgTblZone1 zone1 = entityManager.find(CfgTblZone1.class,
			 * Integer.parseInt(cfgTblZone2.getCfgTblZone1().getTxtZone1Code()));
			 * cfgTblZone2.setCfgTblZone1(zone1);
			 */
			cfgTblZone2.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
			cfgTblZone2.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
			entityManager.merge(cfgTblZone2);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} else {
			return "error";
		}

	}

	@Override
	public String deleteZone2(List<String> zone2sId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serZone2Id : zone2sId) {
				CfgTblZone2 zone2 = entityManager.find(CfgTblZone2.class, Integer.parseInt(serZone2Id));
				if (zone2 != null) {
				//	zone2.setBl_Status(new Byte("0"));
				//	zone2.setBlIsDeleted(new Byte("1"));
					zone2.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
					zone2.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));

				}
			}
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";

		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}

	}



	@Override
	public List<CfgTblZone2> getAllZone2() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone2> zone2 = entityManager.createQuery("FROM CfgTblZone2 where blIsDeleted=0").getResultList();
		entityManager.close();
		return zone2;
	}

	public CfgTblZone1 getZone1Id(String txtZone1Id) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblZone1 zone1 = (CfgTblZone1) entityManager
				.createQuery("FROM CfgTblZone1 where txtZone1Code=" + "'" + txtZone1Id + "'").getSingleResult();
		entityManager.close();
		return zone1;

	}

	public CfgTblZone2 getZone2Id(String txtZone2Id) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblZone2 zone2 = (CfgTblZone2) entityManager
				.createQuery("FROM CfgTblZone2 where txtZone2Code='" + txtZone2Id + "'").getSingleResult();
		entityManager.close();
		return zone2;

	}

	@SuppressWarnings("null")
	@Override
	public String generateZone2Code() {
		int ord_no = 0;

		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			String zone23Code = (String) entityManager.createQuery("select MAX(txtZone2Code) from CfgTblZone2")
					.getSingleResult();
			if (isNullOrEmpty(zone23Code)) {

				zone23Code = "AC-00";
			}
			ord_no = Integer.valueOf(zone23Code.substring(3));
			ord_no = ord_no + 1;
			String code = "AC-1";
			if (ord_no < 10)
				code = "AC-00" + ord_no;
			else if (ord_no > 9 && ord_no < 100)
				code = "AC-0" + ord_no;
			else
				code = "AC-" + ord_no;
			return code;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";

	}
// Zone2 Setup 
	/*
	 * @Override public String addNewZone2(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * int isFoundCode=checkZone2ByCode(cfgTblZone2); int
	 * isFoundName=checkZone2ByName(cfgTblZone2); if(isFoundCode==0 &&
	 * isFoundName==0){ cfgTblZone2.setBl_Status(new Byte("1"));
	 * 
	 * cfgTblZone2.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
	 * cfgTblZone2.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()))
	 * ; // CfgTblZone2 zone2=
	 * getZone2Id(cfgTblZone2.getCfgTblZone2().getTxtZone2Code()); CfgTblZone2 zone2
	 * = entityManager.find(CfgTblZone2.class,
	 * (cfgTblZone2.getCfgTblZone2().getSerZone2Id()));
	 * cfgTblZone2.setCfgTblZone2(zone2); entityManager.persist(cfgTblZone2);
	 * cfgTblZone2.setBlIsbranch(cfgTblZone2.getBlIsbranch());
	 * if(cfgTblZone2.getBlIsbranch()==1){
	 * cfgTblZone2.setSer_parent_id(cfgTblZone2.getSer_parent_id()); }else{
	 * cfgTblZone2.setSer_parent_id(""); }
	 * 
	 * cfgTblZone2.setBlIsbranch(cfgTblZone2.getBlIsbranch());
	 * entityManager.getTransaction().commit(); entityManager.close(); return
	 * "Success"; }else{ return "error"; }
	 * 
	 * 
	 * }
	 */

	/*
	 * @Override public String updateZone2(CfgTblZone2 cfgTblZone2) { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * int isFoundCode = checkZone2ByCodeEdit(cfgTblZone2); int isFoundName =
	 * checkZone2ByNameEdit(cfgTblZone2); if(isFoundCode==0 && isFoundName==0){
	 * CfgTblZone2 zone2 = entityManager.find(CfgTblZone2.class,
	 * Integer.parseInt(cfgTblZone2.getCfgTblZone2().getTxtZone2Code()));
	 * cfgTblZone2.setCfgTblZone2(zone2);
	 * cfgTblZone2.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
	 * cfgTblZone2.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp())
	 * ); entityManager.merge(cfgTblZone2);
	 * cfgTblZone2.setBlIsbranch(cfgTblZone2.getBlIsbranch());
	 * if(cfgTblZone2.getBlIsbranch()==1){
	 * cfgTblZone2.setSer_parent_id(cfgTblZone2.getSer_parent_id()); }else{
	 * //cfgTblZone2.setSer_parent_id(0); } entityManager.getTransaction().commit();
	 * entityManager.close(); return "Success"; }else{ return "error"; }
	 * 
	 * }
	 * 
	 * @Override public String deleteZone2(List<String> zone23Id) { EntityManager
	 * entityManager = getEntityManager(); try {
	 * entityManager.getTransaction().begin(); for (String arId : zone23Id) {
	 * CfgTblZone2 zone23 = entityManager.find(CfgTblZone2.class,
	 * Integer.parseInt(arId)); if (zone23 != null) { zone23.setBl_Status(new
	 * Byte("0")); zone23.setBlIsDeleted(new Byte("1"));
	 * zone23.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
	 * zone23.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
	 * 
	 * } } entityManager.getTransaction().commit(); entityManager.close();
	 * 
	 * } catch (Exception ex) { log.error(ex.getMessage(), ex); return "Failure"; }
	 * return "Success"; }
	 */

	/*
	 * @SuppressWarnings("unchecked")
	 * 
	 * @Override public List<CfgTblZone2> getAllZone2() { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone2> zone23 =
	 * entityManager.createQuery("FROM CfgTblZone2 where bl_Status=1").getResultList
	 * (); entityManager.close(); return zone23; }
	 */

	@Override
	public String generateItemCode() {

		int ord_no = 0;

		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();

			String itemCode = (String) entityManager.createQuery("select MAX(txtItemCode) from CitItemSetup")
					.getSingleResult();
			if (isNullOrEmpty(itemCode)) {

				itemCode = "IC-00";
			}
			ord_no = Integer.valueOf(itemCode.substring(3));

			ord_no = ord_no + 1;
			String code = "IC-1";
			if (ord_no < 10)
				code = "IC-00" + ord_no;
			else if (ord_no > 9 && ord_no < 100)
				code = "IC-0" + ord_no;
			else
				code = "IC-" + ord_no;
			return code;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	@Override
	public String addNewZone2(CfgTblZone2 cfgTblZone2) {
		EntityManager entityManager = getEntityManager();
		int isFoundCode = checkZone2ByCode(cfgTblZone2);
//		int isFoundName = checkZone2ByName(cfgTblZone2);
		if (isFoundCode == 0 ) {
			entityManager.getTransaction().begin();
			/*cfgTblZone2.setBl_Status(new Byte("1"));*/
			cfgTblZone2.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			cfgTblZone2.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));

			entityManager.persist(cfgTblZone2);

			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} else {
			return "error";
		}
	}

	@Override
	public String generateZone3Code() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addNewZone3(CfgTblZone3 cfgTblZone3) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*cfgTblZone3.setBl_Status(new Byte("1"));*/
		cfgTblZone3.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
		cfgTblZone3.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));

		entityManager.persist(cfgTblZone3);

		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
	}

	@Override
	public String deleteZone3(List<String> zone3Id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String updateZone3(CfgTblZone3 cfgTblZone3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<CfgTblZone3> getAllZone3() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();

		List<CfgTblZone3> zone1 = entityManager.createQuery("FROM CfgTblZone3 where bl_Status=1").getResultList();

		entityManager.close();

		return zone1;
		
	}

	@Override
	public List<CfgTblZone3> getAllZone3Branches() {
		return null;
	}

	/*
	 * @Override public List<CfgTblZone2> getAllZone2Branches() { EntityManager
	 * entityManager = getEntityManager(); entityManager.getTransaction().begin();
	 * List<CfgTblZone2> zone23 = entityManager.
	 * createQuery("FROM CfgTblZone2 where bl_Status=1 and blIsbranch=1")
	 * .getResultList(); entityManager.close(); return zone23; }
	 */

}
