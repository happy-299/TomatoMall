package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.po.Account;
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
    public Response<AccountVO> getAccount(@PathVariable String username) {
        System.out.println("getAccount");
        return Response.buildSuccess(accountService.getInformation());
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<Boolean> createUser(@RequestBody AccountVO accountVO) {
        System.out.println("createUser");
        return Response.buildSuccess(accountService.register(accountVO));
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<Boolean> updateUser(@RequestBody AccountVO accountVO) {
        System.out.println(accountVO.getUsername());
        System.out.println(accountVO.getName());
        System.out.println(accountVO.getPhone());
        System.out.println(accountVO.getAddress());
        System.out.println(accountVO.getEmail());
        System.out.println(accountVO.getAvatar());
        System.out.println(accountVO.getRole());
        return Response.buildSuccess(accountService.updateInformation(accountVO));
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        System.out.println("login");
        return Response.buildSuccess(accountService.login(username, password));
    }
}
