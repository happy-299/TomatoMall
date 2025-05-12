package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.NoteVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Note
{
    //Id 标题 内容 作者 价格（0免费，或者大于零的整数，单位为番茄币）
// 付费的用户列表（作者默认已付费） 创建时间 浏览量 点赞数
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    private String title;

    @Basic
    @Column(length = 2000)
    private String content;

    @Basic
    private String img;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Account creator;

    @Basic
    private Integer price = 0;

    @ManyToMany
    @JoinTable(
            name = "note_payer", // 自动生成中间表
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "payer_id")
    )
    private List<Account> payers = new ArrayList<>();

    @Basic
    private LocalDateTime createTime;

    @Basic
    private Integer viewCnt = 0;

    @Basic
    private Integer likeCnt = 0;

    @ManyToMany
    @JoinTable(
            name = "note_liker", // 自动生成中间表
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "liker_id")
    )
    private List<Account> likers = new ArrayList<>();

    public NoteVO toVO()
    {
        return NoteVO.builder()
                .id(id)
                .title(title)
                .content(content)
                .img(img)
                .price(price)
                .creatorId(creator.getId())
                .createTime(createTime)
                .likeCnt(likeCnt)
//                .payersId(payers.stream()
//                        .map(Account::getId)
//                        .collect(Collectors.toList()))
                .viewCnt(viewCnt)
                .build();
    }


}
