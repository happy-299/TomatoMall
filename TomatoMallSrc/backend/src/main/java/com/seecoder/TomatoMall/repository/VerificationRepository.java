package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import com.seecoder.TomatoMall.po.VerificationApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<VerificationApplication, Integer> {
    Optional<VerificationApplication> findByAccountId(Integer accountId);

    Page<VerificationApplication> findByStatus(VerificationStatus status, Pageable pageable);

    Page<VerificationApplication> findByAccountId(Integer accountId, Pageable pageable);

    boolean existsByAccountIdAndStatus(Integer accountId, VerificationStatus status);
}
