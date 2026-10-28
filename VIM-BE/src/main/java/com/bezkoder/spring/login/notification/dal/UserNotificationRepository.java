package com.bezkoder.spring.login.notification.dal;

import com.bezkoder.spring.login.notification.dal.entities.CfgTblUserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<CfgTblUserNotification, Long> {

    @Query("SELECT n FROM CfgTblUserNotification n WHERE n.serUserNotificationId = :id "
            + "AND n.serRecipientUserId = :userId "
            + "AND (n.blIsDeleted IS NULL OR n.blIsDeleted = false)")
    Optional<CfgTblUserNotification> findActiveForRecipient(@Param("id") Long id, @Param("userId") Integer userId);

    @Query("SELECT n FROM CfgTblUserNotification n WHERE n.serRecipientUserId = :userId "
            + "AND (n.blIsDeleted IS NULL OR n.blIsDeleted = false) "
            + "AND (:unreadOnly = false OR n.blnRead = false OR n.blnRead IS NULL) "
            + "ORDER BY n.dteCreatedDate DESC, n.serUserNotificationId DESC")
    List<CfgTblUserNotification> findForRecipient(@Param("userId") Integer userId,
            @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

    @Query("SELECT COUNT(n) FROM CfgTblUserNotification n WHERE n.serRecipientUserId = :userId "
            + "AND (n.blIsDeleted IS NULL OR n.blIsDeleted = false) "
            + "AND (n.blnRead = false OR n.blnRead IS NULL)")
    long countUnreadForRecipient(@Param("userId") Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CfgTblUserNotification n SET n.blnRead = true, n.dteReadDate = :readAt, n.dteModifiedDate = :readAt "
            + "WHERE n.serRecipientUserId = :userId "
            + "AND (n.blIsDeleted IS NULL OR n.blIsDeleted = false) "
            + "AND (n.blnRead = false OR n.blnRead IS NULL)")
    int markAllReadForRecipient(@Param("userId") Integer userId, @Param("readAt") Timestamp readAt);
}
