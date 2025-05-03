package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.vo.AccountVO;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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
    @PostMapping("/{userId}/follow")
    public Response<String> follow(
            @PathVariable Integer userId, @RequestBody Integer targetId
    ) {
        accountService.followUser(userId, targetId);
        return Response.buildSuccess("关注成功");
    }


    @DeleteMapping("/{userId}/follow")
    public Response<String> unfollow(
            @PathVariable Integer userId, @RequestBody Integer targetId
    ) {
        accountService.unfollowUser(userId, targetId);
        return Response.buildSuccess("取消关注成功");
    }

    @GetMapping("/{userId}/following")
    public Response<List<AccountVO>> getFollowing(@PathVariable Integer userId) {
        List<AccountVO> followingList = accountService.getFollowingList(userId);
        return Response.buildSuccess(followingList);
    }

    @GetMapping("/{userId}/follower")
    public Response<List<AccountVO>> getFollower(@PathVariable Integer userId) {
        List<AccountVO> followerList = accountService.getFollowerList(userId);
        return Response.buildSuccess(followerList);
    }
}
