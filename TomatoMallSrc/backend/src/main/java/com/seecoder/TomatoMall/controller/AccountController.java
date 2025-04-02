package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.vo.AccountVO;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
}
