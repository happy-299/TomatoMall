package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import com.seecoder.TomatoMall.vo.VerificationVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VerificationService {
    void apply(Integer accountId, String reason, List<String> imgUrls);
    void review(Integer appId, boolean pass, Integer adminId, String rejectReason);
    Page<VerificationVO> getAllVerification(int page, int size);
    Page<VerificationVO> getVerificationByStatus(VerificationStatus status, int page, int size);
    Page<VerificationVO> getVerificationByAccountId(Integer accountId, int page, int size);
}
