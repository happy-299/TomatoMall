package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.vo.AccountVO;
import com.seecoder.TomatoMall.vo.PartAccountVO;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountService accountService;

    /**
     * 获取用户详情
     */
    @GetMapping("/{username}")
    public Response<AccountVO> getAccount(
            @PathVariable String username
    ) {
        return Response.buildSuccess(accountService.getInformation());
    }

    @GetMapping("/{userid}")
    public Response<PartAccountVO> getAccountById(
            @PathVariable Integer userid
    ) {
        return Response.buildSuccess(accountService.getInformationPart(userid));
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<String> createUser(@RequestBody AccountVO accountVO) {
        if (accountService.register(accountVO)) {
            return Response.buildSuccess("注册成功");
        } else {
            return Response.buildFailure("注册失败", "401");
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<Boolean> updateUser(@RequestBody AccountVO accountVO) {
        return Response.buildSuccess(accountService.updateInformation(accountVO));
    }

    @Data // Lombok 自动生成 getter/setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestBody LoginRequest loginRequest) {
//        System.out.println("user" + loginRequest.username);
        return Response.buildSuccess(accountService.login(
                loginRequest.username, loginRequest.password));
    }

    /*
     * ==================================
     *       follow related
     * ==================================
     */
    @GetMapping("isfollowed")
    public Response<Boolean> isFollowed(@RequestParam Integer targetUserId) {
        AccountVO accountVO = accountService.getInformation();
        return Response.buildSuccess(accountService.isFollowed(accountVO.getId(),targetUserId));
    }


    @PostMapping("/follow")
    public Response<String> follow(@RequestBody Integer targetId) {
        AccountVO accountVO = accountService.getInformation();
        accountService.followUser(accountVO.getId(), targetId);
        return Response.buildSuccess("关注成功");
    }


    @DeleteMapping("/follow")
    public Response<String> unfollow(@RequestBody Integer targetId) {
        AccountVO accountVO = accountService.getInformation();
        accountService.unfollowUser(accountVO.getId(), targetId);
        return Response.buildSuccess("取消关注成功");
    }

    @GetMapping("/following")
    public Response<List<PartAccountVO>> getFollowing() {
        AccountVO accountVO = accountService.getInformation();
        List<AccountVO> followingList = accountService.getFollowingList(accountVO.getId());
        List<PartAccountVO> part =  followingList.stream().map(vo -> vo.toPart()).collect(Collectors.toList());
        return Response.buildSuccess(part);
    }

    @GetMapping("/follower")
    public Response<List<PartAccountVO>> getFollower() {
        AccountVO accountVO = accountService.getInformation();
        List<AccountVO> followerList = accountService.getFollowerList(accountVO.getId());
        List<PartAccountVO> part =  followerList.stream().map(vo -> vo.toPart()).collect(Collectors.toList());
        return Response.buildSuccess(part);
    }

    @Data // Lombok 自动生成 getter/setter
    public static class VerifyRequest {
        private String verifiedName;
    }


    @PostMapping("/verified")
    public Response<List<PartAccountVO>> getVerified(@RequestBody VerifyRequest verifiedReq) {
        List<AccountVO> accountVOList =  accountService.getUserByVerifiedName(verifiedReq.getVerifiedName());
        List<PartAccountVO> part =  accountVOList.stream().map(vo -> vo.toPart()).collect(Collectors.toList());
        return Response.buildSuccess(part);
    }
}
