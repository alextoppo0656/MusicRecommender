package com.musicrec.repository;

import com.musicrec.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.lastSeen < :cutoffTime")
    int deleteOldSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
}