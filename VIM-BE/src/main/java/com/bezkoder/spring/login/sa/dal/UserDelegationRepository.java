package com.bezkoder.spring.login.sa.dal;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblUserDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDelegationRepository extends JpaRepository<CfgTblUserDelegation, Integer> {

    @Query("SELECT d FROM CfgTblUserDelegation d " +
            "WHERE (d.blIsDeleted IS NULL OR d.blIsDeleted = false) " +
            "ORDER BY d.dteCreatedDate DESC, d.serDelegationId DESC")
    List<CfgTblUserDelegation> findAllNotDeleted();

    @Query("SELECT d FROM CfgTblUserDelegation d " +
            "WHERE d.serDelegationId = :id AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false)")
    Optional<CfgTblUserDelegation> findActiveById(Integer id);

    @Query("SELECT d FROM CfgTblUserDelegation d " +
            "WHERE d.serDelegateUserId = :delegateUserId " +
            "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false) " +
            "AND UPPER(COALESCE(d.txtStatus, '')) <> 'REVOKED' " +
            "ORDER BY d.dteValidFrom DESC, d.serDelegationId DESC")
    List<CfgTblUserDelegation> findDelegationsForDelegate(@Param("delegateUserId") Integer delegateUserId);
}
