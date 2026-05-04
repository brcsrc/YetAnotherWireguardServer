package com.brcsrc.yaws.twofa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PreAuthSessionRepository extends JpaRepository<PreAuthSession, String> {
    @Modifying
    @Query("delete from PreAuthSession p where p.expiresAt < :now")
    int purgeExpiredSessions(@Param("now") LocalDateTime now);
}
