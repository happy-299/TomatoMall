package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.AccountVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Account
{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;


    // 用户名
    @Basic
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Basic
    @Column(name = "password", nullable = false)
    private String password;

    // 用户真实姓名
    @Basic
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Basic
    @Column(name = "avatar")
    private String avatar;

    @Basic
    @Column(name = "role")
    private String role;


    // 这里后端不作校验，由前端来校验
    @Basic
    @Column(name = "telephone", length = 11)
    private String telephone;

    @Basic
    @Column(name = "email", length = 100)
    private String email;

    @Basic
    @Column(name = "location")
    private String location;

    // 以下为作业需求中不存在的字段

    @Basic
    @Column(name = "create_time")
    private Date createTime;

    // 大v认证
    @Basic
    private Boolean isVerified = false;

    // 粉丝数量
    @Basic
    private Integer followerCount = 0;

    // 关注数量
    @Basic
    private Integer followingCount = 0;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SearchHistory> searchHistories = new ArrayList<>();

//    @Basic
//    @Column(name = "role")
//    @Enumerated(EnumType.STRING)
//    private RoleEnum role;

    public AccountVO toVO()
    {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(this.id);
        accountVO.setUsername(this.username);
        accountVO.setPassword(this.password);
        accountVO.setName(this.name);
        accountVO.setAvatar(this.avatar);
        accountVO.setTelephone(this.telephone);
        accountVO.setEmail(this.email);
        accountVO.setLocation(this.location);
        accountVO.setCreateTime(this.createTime);
        accountVO.setRole(this.role);
        accountVO.setIsVerified(this.isVerified);
        accountVO.setFollowerCount(this.followerCount);
        accountVO.setFollowingCount(this.followingCount);
        return accountVO;
    }

}
