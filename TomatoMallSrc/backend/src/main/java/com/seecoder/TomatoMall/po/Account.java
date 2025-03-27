package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.enums.RoleEnum;
import com.seecoder.TomatoMall.vo.AccountVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Account {
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


    // 这里后端不作校验，由前端来校验
    @Basic
    @Column(name = "phone", length = 11)
    private String phone;

    @Basic
    @Column(name = "email", length = 100)
    private String email;

    @Basic
    @Column(name = "address")
    private String address;

    // 以下为作业需求中不存在的字段

    @Basic
    @Column(name = "create_time")
    private Date createTime;

    @Basic
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    public AccountVO toVO() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(this.id);
        accountVO.setUsername(this.username);
        accountVO.setPassword(this.password);
        accountVO.setName(this.name);
        accountVO.setAvatar(this.avatar);
        accountVO.setPhone(this.phone);
        accountVO.setEmail(this.email);
        accountVO.setAddress(this.address);
        accountVO.setCreateTime(this.createTime);
        accountVO.setRole(this.role);
        return accountVO;
    }

}
