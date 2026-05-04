package com.brcsrc.yaws.twofa.recovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
    List<RecoveryCode> findByUserId(Long userId);
    List<RecoveryCode> findByUserIdAndDisabledFalseAndUsedAtIsNull(Long userId);
    void deleteByUserId(Long userId);
}
