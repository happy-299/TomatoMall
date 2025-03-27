package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.vo.AccountVO;
import com.seecoder.TomatoMall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Resource
    AccountService accountService;

    /**
     * 获取用户详情
     */
    @GetMapping("/{username}")
    public Response<AccountVO> getAccount(
            @PathVariable Integer username
    ) {
        return Response.buildSuccess(accountService.getInformation());
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<Boolean> createUser(@RequestBody AccountVO accountVO) {
        return Response.buildSuccess(accountService.register(accountVO));
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<Boolean> updateUser(@RequestBody AccountVO accountVO) {
        return Response.buildSuccess(accountService.updateInformation(accountVO));
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return Response.buildSuccess(accountService.login(username, password));
    }
}
