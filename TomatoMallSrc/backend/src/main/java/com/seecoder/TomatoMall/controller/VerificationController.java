package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.enums.VerificationStatus;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.service.VerificationService;
import com.seecoder.TomatoMall.vo.AccountVO;
import com.seecoder.TomatoMall.vo.PageResponseVO;
import com.seecoder.TomatoMall.vo.Response;
import com.seecoder.TomatoMall.vo.VerificationVO;
import com.sun.istack.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {
    @Autowired
    private VerificationService verificationService;

    @Autowired
    private AccountService accountService;

    @PostMapping("/apply")
    public Response<String> apply(@RequestBody VerificationApplyDTO request) {
        AccountVO accountVO = accountService.getInformation();
        verificationService.apply(
                accountVO.getId(), request.getVerifiedName(), request.reasonText, request.proofImgs
        );
        return Response.buildSuccess("申请成功");
    }

    @PostMapping("/review")
    public Response<String> review(@RequestBody VerificationReviewDTO request) {
        AccountVO accountVO = accountService.getInformation();
        if (!accountVO.getRole().equals("admin")) {
            throw new TomatoMallException("只有管理员身份才可以review");
        }
        verificationService.review(
                request.getAppId(),
                request.getPass(),
                accountVO.getId(),
                request.getRejectReason()
        );
        return Response.buildSuccess("review成功");
    }


    @GetMapping("/listall")
    public Response<PageResponseVO<VerificationVO>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        AccountVO current = accountService.getInformation();
        if (!"admin".equals(current.getRole())) {
            throw new TomatoMallException("需要管理员权限");
        }

        Page<VerificationVO> voPage =
                verificationService.getAllVerification(page, size);
        return Response.buildSuccess(new PageResponseVO<>(voPage));
    }

    @GetMapping("/list")
    public Response<PageResponseVO<VerificationVO>> listByStatus(
            @RequestParam(defaultValue = "PENDING") VerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        AccountVO current = accountService.getInformation();
        if (!"admin".equals(current.getRole())) {
            throw new TomatoMallException("需要管理员权限");
        }

        Page<VerificationVO> voPage =
                verificationService.getVerificationByStatus(status, page, size);
        return Response.buildSuccess(new PageResponseVO<>(voPage));
    }

    @GetMapping("/mine")
    public Response<PageResponseVO<VerificationVO>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        AccountVO current = accountService.getInformation();
        Page<VerificationVO> voPage =
                verificationService.getVerificationByAccountId(current.getId(), page, size);
        return Response.buildSuccess(new PageResponseVO<>(voPage));
    }

    @Data
    public static class VerificationApplyDTO {
        private String verifiedName;
        private String reasonText;
        private List<String> proofImgs;
    }

    @Data
    public static class VerificationReviewDTO {
        private Boolean pass;
        private String rejectReason;
        private Integer appId;
    }
}
