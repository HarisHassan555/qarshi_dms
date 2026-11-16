package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CfgTblEmailDeliveryLogRepository extends JpaRepository<CfgTblEmailDeliveryLog, Long> {

    @Query("SELECT e FROM CfgTblEmailDeliveryLog e WHERE e.blIsDeleted = false "
            + "AND (:status IS NULL OR :status = '' OR e.txtStatus = :status) "
            + "AND (:triggerType IS NULL OR :triggerType = '' OR e.txtTriggerType = :triggerType) "
            + "AND (:recipient IS NULL OR :recipient = '' OR LOWER(e.txtRecipientEmail) LIKE LOWER(CONCAT('%', :recipient, '%'))) "
            + "AND (:applicationId IS NULL OR e.serApplicationId = :applicationId) "
            + "ORDER BY e.dteSentAt DESC")
    Page<CfgTblEmailDeliveryLog> search(
            @Param("status") String status,
            @Param("triggerType") String triggerType,
            @Param("recipient") String recipient,
            @Param("applicationId") Integer applicationId,
            Pageable pageable);

    Optional<CfgTblEmailDeliveryLog> findBySerEmailDeliveryLogIdAndBlIsDeletedFalse(Long id);
}