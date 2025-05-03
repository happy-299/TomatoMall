package com.seecoder.TomatoMall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class UserFollow {
    @Id
    private Integer id;

    @Basic
    private Integer followerId;

    @Basic
    private Integer followedId;

    @Basic
    private LocalDateTime createTime;

}
