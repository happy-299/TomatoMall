package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.VerificationApplication;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.repository.VerificationRepository;
import com.seecoder.TomatoMall.service.VerificationService;
import com.seecoder.TomatoMall.vo.VerificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationServiceImpl implements VerificationService {

    @Autowired
    private VerificationRepository verRepo;

    @Autowired
    private AccountRepository accountRepo;

    @Override
    @Transactional
    public void apply(Integer accountId, String reason, List<String> imgUrls) {
        Account acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new TomatoMallException("账号不存在"));

        if (verRepo.existsByAccountIdAndStatus(accountId, VerificationStatus.PENDING)) {
            throw new TomatoMallException("已有待审核申请");
        }

        VerificationApplication app = new VerificationApplication();
        app.setAccount(acc);
        app.setReasonText(reason);
        app.setProofImgs(imgUrls);
        verRepo.save(app);
    }

    @Override
    @Transactional
    public void review(
            Integer appId, boolean pass, Integer adminId,
            String rejectReason) {
        VerificationApplication app = verRepo.findById(appId)
                .orElseThrow(() -> new TomatoMallException("申请不存在"));
        if (app.getStatus() != VerificationStatus.PENDING) {
            throw new TomatoMallException("申请已处理");
        }

        Account admin = accountRepo.findById(adminId)
                .orElseThrow(() -> new TomatoMallException("管理员不存在"));
        app.setReviewer(admin);
        app.setReviewTime(LocalDateTime.now());
        app.setStatus(pass ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED);

        if (pass) {
            Account acc = app.getAccount();
            acc.setIsVerified(true);
            accountRepo.save(acc);
        } else {
            app.setRejectReason(rejectReason);
        }

        verRepo.save(app);
    }

    @Override
    public Page<VerificationVO> getAllVerification(int page, int size) {
        Page<VerificationApplication> pageData =
                verRepo.findAll(PageRequest.of(page, size));
        return pageData.map(VerificationApplication::toVO);
    }

    @Override
    public Page<VerificationVO> getVerificationByStatus(
            VerificationStatus status, int page, int size) {

        Page<VerificationApplication> pageData =
                verRepo.findByStatus(status, PageRequest.of(page, size, Sort.by("createTime").descending()));

        return pageData.map(VerificationApplication::toVO);
    }

    @Override
    public Page<VerificationVO> getVerificationByAccountId(
            Integer accountId, int page, int size) {

        Page<VerificationApplication> pageData =
                verRepo.findByAccountId(accountId, PageRequest.of(page, size, Sort.by("createTime").descending()));

        return pageData.map(VerificationApplication::toVO);
    }
}
