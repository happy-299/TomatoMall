package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.ReviewVO;
import com.seecoder.TomatoMall.vo.SearchHistoryVO;
import lombok.*;

import javax.persistence.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
public class SearchHistory
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String keyword;      // 搜索关键词

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;          // 关联用户


    public SearchHistoryVO toVO()
    {
        return SearchHistoryVO.builder().id(this.id).keyword(this.keyword).build();
    }
}