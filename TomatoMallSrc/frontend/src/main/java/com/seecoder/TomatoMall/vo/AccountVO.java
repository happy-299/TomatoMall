package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.enums.RoleEnum;
import com.seecoder.TomatoMall.po.Account;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class AccountVO {
    private Integer id;

    private String username;

    private String password;

    private String name;

    private String avatar;

    private String phone;

    private String email;

    private String address;

    private Date createTime;

    private RoleEnum role;


    public Account toPO(){
        Account account=new Account();
        account.setId(this.id);
        account.setUsername(this.username);
        account.setPassword(this.password);
        account.setName(this.name);
        account.setAvatar(this.avatar);
        account.setPhone(this.phone);
        account.setEmail(this.email);
        account.setAddress(this.address);
        account.setCreateTime(this.createTime);
        account.setRole(this.role);
        return account;
    }
}

