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

    private String role;

    private String telephone;

    private String email;

    private String location;

    private Date createTime;

    private Boolean isVerified;

    private Integer followerCount;

    private Integer followingCount;


//    private RoleEnum role;


    public Account toPO() {
        Account account = new Account();
        account.setId(this.id);
        account.setUsername(this.username);
        account.setPassword(this.password);
        account.setName(this.name);
        account.setAvatar(this.avatar);
        account.setTelephone(this.telephone);
        account.setEmail(this.email);
        account.setLocation(this.location);
        account.setCreateTime(this.createTime);
        account.setRole(this.role);
        account.setIsVerified(this.isVerified);
        account.setFollowerCount(this.followerCount);
        account.setFollowingCount(this.followingCount);
        return account;
    }
}

