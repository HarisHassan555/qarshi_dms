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

import com.bezkoder.spring.login.sa.dal.entities.CfgTblArea;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblRegion;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblZone;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;


@Repository
public class CfgTblTerritoryDAO implements com.bezkoder.spring.login.sa.dal.dao.ICfgTerritoryDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblTerritoryDAO.class);

	public CfgTblTerritoryDAO() {
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	
	@Override
	public String addNewRegion(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		int isFoundCode=checkRegionByCode(cfgTblRegion);
		int isFoundName=checkRegionByName(cfgTblRegion);
		if(isFoundCode==0 && isFoundName==0){
		entityManager.getTransaction().begin();
		cfgTblRegion.setBl_Status(new Byte("1"));
		cfgTblRegion.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
	    cfgTblRegion.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));

		entityManager.persist(cfgTblRegion);
		
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else
		{
			return "error";
		}
	}
	
	public static String setDateParsing(String date) throws ParseException {

	    //this format date we want
	    DateFormat mSDF = new SimpleDateFormat("mm/dd/yyyy"); 

	    //this format date actully present
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
	public List<CfgTblRegion> getAllRegion() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();

		List<CfgTblRegion> region = entityManager.createQuery("FROM CfgTblRegion where bl_Status=1").getResultList();

		entityManager.close();

		return region;
	}
	


	

	@Override
	public String deleteRegion(List<String> regionId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String regId : regionId) {
				CfgTblRegion region = entityManager.find(CfgTblRegion.class, Integer.parseInt(regId));
				if (region != null) {
					region.setBl_Status(new Byte("0"));
					region.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
				    region.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));

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
	public String updateRegion(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();		
		int isFoundCode = checkRegionByCodeEdit(cfgTblRegion);
		int isFoundName = checkRegionByNameEdit(cfgTblRegion);
		if(isFoundCode==0 && isFoundName==0){
		cfgTblRegion.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
		cfgTblRegion.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
		entityManager.merge(cfgTblRegion);
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else{
			return "error";
		}
	}

	
	public int  checkRegionByCode(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> region = entityManager.createQuery("FROM CfgTblRegion where txtRegionCode='"+cfgTblRegion.getTxtRegionCode()+"'").getResultList();
        entityManager.close();

        if(region.size()>0){
        	return region.size();
        }return 0;
		
	}
	public int  checkRegionByName(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> region = entityManager.createQuery("FROM CfgTblRegion where txtRegionName='"+cfgTblRegion.getTxtRegionName()+"'").getResultList();
        entityManager.close();
        if(region.size()>0){
        	return region.size();
        }return 0;
		
	}
	
	
	public int  checkZoneByCode(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone> zone = entityManager.createQuery("FROM CfgTblZone where txtZoneCode='"+cfgTblZone.getTxtZoneCode()+"'").getResultList();
        entityManager.close();

        if(zone.size()>0){
        	return zone.size();
        }return 0;
		
	}
	public int  checkAreaByName(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblArea> area = entityManager.createQuery("FROM CfgTblArea where txtAreaName='"+cfgTblArea.getTxtAreaName()+"'").getResultList();
        entityManager.close();
        if(area.size()>0){
        	return area.size();
        }return 0;
		
	}
	
	public int  checkAreaByCode(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblArea> area = entityManager.createQuery("FROM CfgTblArea where txtAreaCode='"+cfgTblArea.getTxtAreaCode()+"'").getResultList();
        entityManager.close();

        if(area.size()>0){
        	return area.size();
        }return 0;
		
	}
	public int  checkZoneByName(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone> zone = entityManager.createQuery("FROM CfgTblZone where txtZoneName='"+cfgTblZone.getTxtZoneName()+"'").getResultList();
        entityManager.close();
        if(zone.size()>0){
        	return zone.size();
        }return 0;
		
	}
	
	
	public int  checkZoneByCodeEdit(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> zone = entityManager.createQuery("FROM CfgTblZone where txtZoneCode='"+cfgTblZone.getTxtZoneCode()+"'"+" and serZoneId <>"+cfgTblZone.getSerZoneId()).getResultList();
        entityManager.close();

        if(zone.size()>0){
        	return zone.size();
        }return 0;
		
	}
	
	public int  checkZoneByNameEdit(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> zone = entityManager.createQuery("FROM CfgTblZone where txtZoneName='"+cfgTblZone.getTxtZoneName()+"'"+" and serZoneId <>"+cfgTblZone.getSerZoneId()).getResultList();
        entityManager.close();
        if(zone.size()>0){
        	return zone.size();
        }return 0;
		
	}
	
	
	public int  checkRegionByCodeEdit(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> region = entityManager.createQuery("FROM CfgTblRegion where txtRegionCode='"+cfgTblRegion.getTxtRegionCode()+"'"+" and serRegionId <>"+cfgTblRegion.getSerRegionId()).getResultList();
        entityManager.close();

        if(region.size()>0){
        	return region.size();
        }return 0;
		
	}
	public int  checkRegionByNameEdit(CfgTblRegion cfgTblRegion) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> region = entityManager.createQuery("FROM CfgTblRegion where txtRegionName='"+cfgTblRegion.getTxtRegionName()+"'"+" and serRegionId <>"+cfgTblRegion.getSerRegionId()).getResultList();
        entityManager.close();
        if(region.size()>0){
        	return region.size();
        }return 0;
		
	}
	
	public int  checkAreaByCodeEdit(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblRegion> area = entityManager.createQuery("FROM CfgTblArea where txtAreaCode='"+cfgTblArea.getTxtAreaCode()+"'"+" and serAreaId <>"+cfgTblArea.getSerAreaId()).getResultList();
        entityManager.close();

        if(area.size()>0){
        	return area.size();
        }return 0;
		
	}
	public int  checkAreaByNameEdit(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblArea> area = entityManager.createQuery("FROM CfgTblArea where txtAreaName='"+cfgTblArea.getTxtAreaName()+"'"+" and serAreaId <>"+cfgTblArea.getSerAreaId()).getResultList();
        entityManager.close();
        if(area.size()>0){
        	return area.size();
        }return 0;
		
	}
	
	
	@Override
	public String generateRegionCode() {
		int ord_no=0;
	
		EntityManager entityManager = getEntityManager();
		try{
			entityManager.getTransaction().begin();
	
			String  regionCode = (String) entityManager.createQuery("select MAX(txtRegionCode) from CfgTblRegion ").getSingleResult();
			 if(isNullOrEmpty(regionCode)){
					
				 regionCode="RG-00";
				}
			ord_no=  Integer.valueOf(regionCode.substring(3));
			
			ord_no=ord_no+1;
			String code ="RG-1";
			if(ord_no <10)
				code="RG-00"+ord_no;
			else if (ord_no >9 && ord_no <100)
				code="RG-0"+ord_no;
			else 
				code="RG-"+ord_no;
			return code;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String addNewZone(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode=checkZoneByCode(cfgTblZone);
		int isFoundName=checkZoneByName(cfgTblZone);
		if(isFoundCode==0 && isFoundName==0){
		cfgTblZone.setBl_Status(new Byte("1"));
		cfgTblZone.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
		cfgTblZone.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));
		CfgTblRegion  region= getRegionId(cfgTblZone.getCfgTblRegion().getTxtRegionCode());
		cfgTblZone.setCfgTblRegion(region);
		entityManager.persist(cfgTblZone);		
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else{
			return "error";
		}
		
	}
	
	@Override
	public String updateZone(CfgTblZone cfgTblZone) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode = checkZoneByCodeEdit(cfgTblZone);
		int isFoundName = checkZoneByNameEdit(cfgTblZone);
		if(isFoundCode==0 && isFoundName==0){
		/*CfgTblRegion region = entityManager.find(CfgTblRegion.class, Integer.parseInt(cfgTblZone.getCfgTblRegion().getTxtRegionCode()));
		cfgTblZone.setCfgTblRegion(region);*/
		cfgTblZone.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
		cfgTblZone.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
		entityManager.merge(cfgTblZone);
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else{
			return "error";
		}

	}
	
	@Override
	public String deleteZone(List<String> zonesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serZoneId : zonesId) {
				CfgTblZone zone = entityManager.find(CfgTblZone.class, Integer.parseInt(serZoneId));
				if (zone != null) {
					zone.setBl_Status(new Byte("0"));
					zone.setBlIsDeleted(new Byte("1"));
					zone.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
					zone.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));

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
	public String generateZoneCode() {
		int ord_no=0;
	
		EntityManager entityManager = getEntityManager();
		try{
			entityManager.getTransaction().begin();
	
			String  zoneCode = (String) entityManager.createQuery("select MAX(txtZoneCode) from CfgTblZone").getSingleResult();
			 if(isNullOrEmpty(zoneCode)){
					
				 zoneCode="ZC-00";
				}
			ord_no=  Integer.valueOf(zoneCode.substring(3));
			
			ord_no=ord_no+1;
			String code ="ZC-1";
			if(ord_no <10)
				code="ZC-00"+ord_no;
			else if (ord_no >9 && ord_no <100)
				code="ZC-0"+ord_no;
			else 
				code="ZC-"+ord_no;
			return code;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return "";
	
	
	}

	@Override
	public List<CfgTblZone> getAllZone() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblZone> zone = entityManager.createQuery("FROM CfgTblZone where blIsDeleted=0").getResultList();
		entityManager.close();
		return zone;
	}
	
	
	public CfgTblRegion  getRegionId(String  txtRegionId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblRegion region = (CfgTblRegion) entityManager.createQuery("FROM CfgTblRegion where txtRegionCode="+"'"+txtRegionId+"'").getSingleResult();
        entityManager.close();
        return region;
		
	}
	
	public CfgTblZone  getZoneId(String  txtZoneId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		CfgTblZone zone = (CfgTblZone) entityManager.createQuery("FROM CfgTblZone where txtZoneCode='"+txtZoneId+"'").getSingleResult();
        entityManager.close();
        return zone;
		
	}
	
	@SuppressWarnings("null")
	@Override
	public String generateAreaCode() {
		int ord_no=0;
	
		EntityManager entityManager = getEntityManager();
		try{
			entityManager.getTransaction().begin();
	
			String  areaCode = (String) entityManager.createQuery("select MAX(txtAreaCode) from CfgTblArea").getSingleResult();
			 if(isNullOrEmpty(areaCode)){
					
				 areaCode="AC-00";
				}
			ord_no=  Integer.valueOf(areaCode.substring(3));
			ord_no=ord_no+1;
			String code ="AC-1";
			if(ord_no <10)
				code="AC-00"+ord_no;
			else if (ord_no >9 && ord_no <100)
				code="AC-0"+ord_no;
			else 
				code="AC-"+ord_no;
			return code;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return "";
	
	
	}
// Area Setup 
	@Override
	public String addNewArea(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode=checkAreaByCode(cfgTblArea);
		int isFoundName=checkAreaByName(cfgTblArea);
		if(isFoundCode==0 && isFoundName==0){
		cfgTblArea.setBl_Status(new Byte("1"));
		
		cfgTblArea.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
		cfgTblArea.setDteCreatedDate(parseDate(commonService.getCurrentTimeStamp()));
	//	CfgTblZone  zone= getZoneId(cfgTblArea.getCfgTblZone().getTxtZoneCode());
		CfgTblZone zone = entityManager.find(CfgTblZone.class, (cfgTblArea.getCfgTblZone().getSerZoneId()));
		cfgTblArea.setCfgTblZone(zone);
		entityManager.persist(cfgTblArea);
		cfgTblArea.setBlIsbranch(cfgTblArea.getBlIsbranch());
		if(cfgTblArea.getBlIsbranch()==1){
			cfgTblArea.setSer_parent_id(cfgTblArea.getSer_parent_id());
		}else{
			cfgTblArea.setSer_parent_id("");
		}

		cfgTblArea.setBlIsbranch(cfgTblArea.getBlIsbranch());
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else{
			return "error";
		}

		
	}
	
	@Override
	public String updateArea(CfgTblArea cfgTblArea) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		int isFoundCode = checkAreaByCodeEdit(cfgTblArea);
		int isFoundName = checkAreaByNameEdit(cfgTblArea);
		if(isFoundCode==0 && isFoundName==0){
		/*CfgTblZone zone = entityManager.find(CfgTblZone.class, Integer.parseInt(cfgTblArea.getCfgTblZone().getTxtZoneCode()));
		cfgTblArea.setCfgTblZone(zone);*/
		cfgTblArea.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
		cfgTblArea.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));
		entityManager.merge(cfgTblArea);
		cfgTblArea.setBlIsbranch(cfgTblArea.getBlIsbranch());
		if(cfgTblArea.getBlIsbranch()==1){
			cfgTblArea.setSer_parent_id(cfgTblArea.getSer_parent_id());
		}else{
			//cfgTblArea.setSer_parent_id(0);
		}
		entityManager.getTransaction().commit();
		entityManager.close();
		return "Success";
		}else{
			return "error";
		}

	}
	
	@Override
	public String deleteArea(List<String> areaId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String arId : areaId) {
				CfgTblArea area = entityManager.find(CfgTblArea.class, Integer.parseInt(arId));
				if (area != null) {
					area.setBl_Status(new Byte("0"));
					area.setBlIsDeleted(new Byte("1"));
					area.setSerModifiedUserId(commonService.getCurrentLoggedInUser());
					area.setDteModifiedDate(parseDate(commonService.getCurrentTimeStamp()));

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

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblArea> getAllArea() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblArea> area = entityManager.createQuery("FROM CfgTblArea where bl_Status=1").getResultList();
		entityManager.close();
		return area;
	}
	
	
	
	
	
	@Override
	public String generateItemCode() {
	
		int ord_no=0;
		
		EntityManager entityManager = getEntityManager();
		try{
			entityManager.getTransaction().begin();
	
			String  itemCode = (String) entityManager.createQuery("select MAX(txtItemCode) from CitItemSetup").getSingleResult();
            if(isNullOrEmpty(itemCode)){
				
            	itemCode="IC-00";
			}
			ord_no=  Integer.valueOf(itemCode.substring(3));
			
			ord_no=ord_no+1;
			String code ="IC-1";
			if(ord_no <10)
				code="IC-00"+ord_no;
			else if (ord_no >9 && ord_no <100)
				code="IC-0"+ord_no;
			else 
				code="IC-"+ord_no;
			return code;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return "";
	}
	
	
	

	    public static boolean isNullOrEmpty(String myString)
	    {
	         return myString == null || "".equals(myString);
	    }

		@Override
		public List<CfgTblArea> getAllAreaBranches() {
			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			List<CfgTblArea> area = entityManager.createQuery("FROM CfgTblArea where bl_Status=1 and blIsbranch=1").getResultList();
			entityManager.close();
			return area;
		}

	

}

