package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.math.BigDecimal;
import java.security.Key;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;
import javax.persistence.*;;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;


@Service
public class LoginDAOImpl implements LoginDAO{

	@Autowired
    private EntityManagerFactory entityManagerFactory;
	private String key = "Pathfinder987654"; 
	private static final Logger log = LoggerFactory.getLogger(LoginDAOImpl.class);
	
	@Autowired
	private ICommonService commonService;
	


	
	private EntityManager getEntityManager(){
		return entityManagerFactory.createEntityManager();
	}
	@Override
	public CfgTblUser userLogin(CfgTblUser CfgTblUser) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
	    List<CfgTblUser> user = null;
	    List<CfgTblUser> user_block = null;
	    try{
	    	byte[]   encrypted = Base64.encode(CfgTblUser.getTxtPassword().getBytes());
	    	
//	    	System.out.println("encrypted----------"+encrypted);
	    	user = entityManager.createQuery(
	    		"FROM CfgTblUser where txtUserName = "+"'"+CfgTblUser.getTxtUserName()+
	    		"'"+" and txtPassword = "+"'"+ new String(encrypted)+"' "+
	    		" and (blnStatus = true or blnStatus is null)").getResultList();
	  
	    
	    
			if (!(user != null && user.size() > 0)) {
				user_block = entityManager.createQuery("FROM CfgTblUser where txtUserName = " + "'"
						+ CfgTblUser.getTxtUserName() + "' and (blnStatus = true or blnStatus is null)").getResultList();
				if (user_block != null && user_block.size() > 0) {
					CfgTblUser user_edit = (CfgTblUser) user_block.get(0);
					if(user_edit.getNumAttempt() == null)
						user_edit.setNumAttempt(new BigDecimal(0));
					user_edit.setNumAttempt(new BigDecimal(user_edit.getNumAttempt().doubleValue() + 1));
					if (user_edit.getCfgTblPasswordPolicy().getNumAttempt().doubleValue() <= user_edit.getNumAttempt()
							.doubleValue())
						user_edit.setBlnStatus(false);

					entityManager.merge(user_edit);

				}

			}
			else
			{
				CfgTblUser user_edit = (CfgTblUser) user.get(0);
				if(user_edit.getNumAttempt() == null)
					user_edit.setNumAttempt(new BigDecimal(0));
				user_edit.setNumAttempt(new BigDecimal(0));
				

				entityManager.merge(user_edit);
			}
			
			entityManager.getTransaction().commit();
	    
	    }catch(Exception ex){
	    	log.error(ex.getMessage(),ex);
	    }
	    entityManager.close();
	 if(user !=null && user.size()>0)
		 return user.get(0);
	 else return null;
	}
	@Override
	public List<String> getUserRoles(String username) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    List<String> role = entityManager.createQuery(
	    		"select u.txtrole from CfgTblUser u where u.txtUserName = "+"'"+username+
	    		"'"+"").getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 return role;
	}
	
	@SuppressWarnings("null")
	@Override
	public String userPasswordUpdate(int id,String NewPassword,String oldPassword) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String status="Failure";
	  //  Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
	  //  byte[] oldPasswordByte=Base64.decode(oldPassword.getBytes());
	    
	    id=commonService.getCurrentUserVoId();
    CfgTblUser CfgTblUserObject = entityManager.find(CfgTblUser.class, (id));
   // String currentPassword=new String(oldPasswordByte);
    byte[]   encryptedoldPassword = Base64.encode(oldPassword.getBytes());
    System.out.println("karwa dy print"+new String(encryptedoldPassword));
   // byte[]   encrypted = Base64.encode(CfgTblUser.getPassword().getBytes());
    if(new String(encryptedoldPassword).equals(CfgTblUserObject.getTxtPassword())){
    if(CfgTblUserObject !=null && CfgTblUserObject.getSerUserId()>0){
	    try{
	       byte[]   encrypted = Base64.encode(NewPassword.getBytes());
	       CfgTblUserObject.setTxtPassword(new String(encrypted));
	       entityManager.merge(CfgTblUserObject);
	       entityManager.getTransaction().commit();
	       return status="Success";
	       
	    }
	    
	    catch(Exception ex){
	    	log.error(ex.getMessage(),ex);
	    }
	    entityManager.close();
		status="Failure";

	}
 }else{
	entityManager.close();
	status="CPNM";
	}
	return status;
    
}
	
	
	
	@Override
	public CfgTblUser getUserInformation(int appUserId) {
		EntityManager entityManager = null;
		CfgTblUser user = null;

		try {
			entityManager = getEntityManager();
			entityManager.getTransaction().begin();

			user = (CfgTblUser) entityManager.createQuery(
							"FROM CfgTblUser u WHERE u.serUserId = :userId", CfgTblUser.class)
					.setParameter("userId", appUserId)
					.getSingleResult();

			entityManager.getTransaction().commit();
		} catch (Exception e) {
			if (entityManager != null && entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			e.printStackTrace();
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}

		return user;
	}





	public String userPasswordUpdatebyAdmin(int id,String NewPassword) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String status="Failure";
	  //  Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
	  //  byte[] oldPasswordByte=Base64.decode(oldPassword.getBytes());
	    
//	    id=commonService.getCurrentUserVoId();
    CfgTblUser CfgTblUserObject = entityManager.find(CfgTblUser.class, (id));

//    if(new String(encryptedoldPassword).equals(CfgTblUserObject.getTxtPassword()))
    
    {
    if(CfgTblUserObject !=null && CfgTblUserObject.getSerUserId()>0){
	    try{
	       byte[]   encrypted = Base64.encode(NewPassword.getBytes());
	       CfgTblUserObject.setTxtPassword(new String(encrypted));
	       entityManager.merge(CfgTblUserObject);
	       entityManager.getTransaction().commit();
	       return status="Success";
	       
	    }
	    
	    catch(Exception ex){
	    	log.error(ex.getMessage(),ex);
	    }
	    entityManager.close();
		status="Failure";

	}
 }
	return status;
    
}

}
