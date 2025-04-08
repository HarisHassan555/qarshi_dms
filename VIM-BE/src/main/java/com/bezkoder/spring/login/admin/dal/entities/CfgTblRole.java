package com.bezkoder.spring.login.admin.dal.entities;

import com.bezkoder.spring.login.sa.dal.entities.Permission;
import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;


/**
 * The persistent class for the cfg_tbl_role database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_role")
@NamedQuery(name="CfgTblRole.findAll", query="SELECT c FROM CfgTblRole c")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "serRoleId")
public class CfgTblRole implements Serializable {
	private static final long serialVersionUID = 1L;

//	@Id
//	@GeneratedValue(strategy=GenerationType.AUTO)
//	@Column(name="ser_role_id")
//	private Integer serRoleId;
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_Role_ser_role_id_seq")
	@SequenceGenerator(name = "cfg_tbl_Role_ser_role_id_seq", sequenceName = "cfg_tbl_role_ser_role_id_seq",initialValue = 01, allocationSize = 1)
	@Column(name="ser_role_id")
	private Integer serRoleId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	@Column(name="txt_role_code")
	private String txtRoleCode;

	@Column(name="txt_role_name")
	private String txtRoleName;

	//bi-directional many-to-one association to CfgTblSubMenuRole
	@OneToMany(mappedBy="cfgTblRole")
	@JsonIgnore
	private List<CfgTblSubMenuRole> cfgTblSubMenuRoles;

	//bi-directional many-to-one association to CfgTblUserRole
	@OneToMany(mappedBy="cfgTblRole")
	@JsonIgnore
	private List<CfgTblUserRole> cfgTblUserRoles;


	/*@ManyToMany*/
	/*@JoinTable(
			name = "role_permission",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id")
	)*/
	@ManyToMany(mappedBy = "roles")
	@JsonIgnore
	private List<Permission> permissions;

	public CfgTblRole() {
	}

	public Integer getSerRoleId() {
		return this.serRoleId;
	}

	public void setSerRoleId(Integer serRoleId) {
		this.serRoleId = serRoleId;
	}

	public Boolean getBlIsActive() {
		return this.blIsActive;
	}

	public void setBlIsActive(Boolean blIsActive) {
		this.blIsActive = blIsActive;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public Timestamp getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Timestamp dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Timestamp getDteModifiedDate() {
		return this.dteModifiedDate;
	}

	public void setDteModifiedDate(Timestamp dteModifiedDate) {
		this.dteModifiedDate = dteModifiedDate;
	}

	public Integer getSerCreatedUser() {
		return this.serCreatedUser;
	}

	public void setSerCreatedUser(Integer serCreatedUser) {
		this.serCreatedUser = serCreatedUser;
	}

	public Integer getSerModifiedUser() {
		return this.serModifiedUser;
	}

	public void setSerModifiedUser(Integer serModifiedUser) {
		this.serModifiedUser = serModifiedUser;
	}

	public String getTxtRoleCode() {
		return this.txtRoleCode;
	}

	public void setTxtRoleCode(String txtRoleCode) {
		this.txtRoleCode = txtRoleCode;
	}

	public String getTxtRoleName() {
		return this.txtRoleName;
	}

	public void setTxtRoleName(String txtRoleName) {
		this.txtRoleName = txtRoleName;
	}

	public List<CfgTblSubMenuRole> getCfgTblSubMenuRoles() {
		return this.cfgTblSubMenuRoles;
	}

	public void setCfgTblSubMenuRoles(List<CfgTblSubMenuRole> cfgTblSubMenuRoles) {
		this.cfgTblSubMenuRoles = cfgTblSubMenuRoles;
	}

	public CfgTblSubMenuRole addCfgTblSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole) {
		getCfgTblSubMenuRoles().add(cfgTblSubMenuRole);
		cfgTblSubMenuRole.setCfgTblRole(this);

		return cfgTblSubMenuRole;
	}

	public CfgTblSubMenuRole removeCfgTblSubMenuRole(CfgTblSubMenuRole cfgTblSubMenuRole) {
		getCfgTblSubMenuRoles().remove(cfgTblSubMenuRole);
		cfgTblSubMenuRole.setCfgTblRole(null);

		return cfgTblSubMenuRole;
	}

	public List<CfgTblUserRole> getCfgTblUserRoles() {
		return this.cfgTblUserRoles;
	}

	public void setCfgTblUserRoles(List<CfgTblUserRole> cfgTblUserRoles) {
		this.cfgTblUserRoles = cfgTblUserRoles;
	}

	public CfgTblUserRole addCfgTblUserRole(CfgTblUserRole cfgTblUserRole) {
		getCfgTblUserRoles().add(cfgTblUserRole);
		cfgTblUserRole.setCfgTblRole(this);

		return cfgTblUserRole;
	}

	public CfgTblUserRole removeCfgTblUserRole(CfgTblUserRole cfgTblUserRole) {
		getCfgTblUserRoles().remove(cfgTblUserRole);
		cfgTblUserRole.setCfgTblRole(null);

		return cfgTblUserRole;
	}
	
	
	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;


	public List<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<Permission> permissions) {
		this.permissions = permissions;
	}

}