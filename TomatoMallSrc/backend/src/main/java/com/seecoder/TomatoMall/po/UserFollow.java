package com.seecoder.TomatoMall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class UserFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    private Integer followerId;

    @Basic
    private Integer followedId;

    @Basic
    private LocalDateTime createTime = LocalDateTime.now();

}
